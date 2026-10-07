package com.fox.gameoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_RESULT_CODE = "code"
        const val EXTRA_RESULT_DATA = "data"
        const val CHANNEL_ID = "game_overlay_capture"
        const val NOTIF_ID = 1001

        @Volatile var isRunning = false
            private set
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var detector: TFLiteHelper? = null

    private var width = 0
    private var height = 0
    private var density = 0
    private var targetFps = 30
    private var lastFrameTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val thread = HandlerThread("capture").also { it.start() }
        handlerThread = thread
        handler = Handler(thread.looper)
        detector = TFLiteHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundCompat()
                val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                @Suppress("DEPRECATION")
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (data != null) startCapture(code, data)
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("GameOverlay")
            .setContentText("Захват экрана активен")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_ID, "Capture", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        if (isRunning) return
        val wm = getSystemService(WindowManager::class.java)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        width = metrics.widthPixels
        height = metrics.heightPixels
        density = metrics.densityDpi

        val mpm = getSystemService(MediaProjectionManager::class.java)
        projection = mpm.getMediaProjection(resultCode, data)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        targetFps = prefs.getInt("fps", 30).coerceIn(1, 60)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = projection?.createVirtualDisplay(
            "capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val now = System.currentTimeMillis()
            val frameInterval = 1000L / targetFps
            if (now - lastFrameTime < frameInterval) {
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastFrameTime = now

            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                processImage(image)
            } catch (_: Throwable) {
            } finally {
                image.close()
            }
        }, handler)

        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                isRunning = false
                stopSelf()
            }
        }, handler)

        isRunning = true
    }

    private fun processImage(image: Image) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width

        val bmpWidth = width + rowPadding / pixelStride
        val bmp = Bitmap.createBitmap(bmpWidth, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(buffer)

        val cropped = Bitmap.createBitmap(bmp, 0, 0, width, height)
        if (cropped !== bmp) bmp.recycle()

        val detections = detector?.detect(cropped) ?: emptyList()
        cropped.recycle()

        OverlayService.updateDetections(detections, width, height)
    }

    override fun onDestroy() {
        isRunning = false
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        try { imageReader?.close() } catch (_: Throwable) {}
        try { projection?.stop() } catch (_: Throwable) {}
        handlerThread?.quitSafely()
        detector?.close()
        super.onDestroy()
    }
}