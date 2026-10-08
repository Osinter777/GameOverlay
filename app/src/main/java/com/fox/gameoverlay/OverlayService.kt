package com.fox.gameoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager

class OverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "game_overlay"
        private const val NOTIF_ID = 1002

        @Volatile private var instance: OverlayService? = null

        fun updateDetections(list: List<Detection>, screenW: Int, screenH: Int) {
            val svc = instance ?: return
            svc.view?.post {
                val v = svc.view ?: return@post
                v.setDetections(list, screenW, screenH)
                val prefs = svc.getSharedPreferences("settings", Context.MODE_PRIVATE)
                if (prefs.getBoolean("aimEnabled", false) && AimAccessibilityService.instance != null) {
                    val fov = prefs.getInt("fov", 300).toFloat()
                    val detection = v.closestDetection() ?: return@post
                    val r = v.screenRect(detection)
                    val cx = r.centerX()
                    val cy = r.centerY()
                    val dx = cx - v.width / 2f
                    val dy = cy - v.height / 2f
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist <= fov) {
                        AimAccessibilityService.swipeTo(cx, cy)
                    }
                }
            }
        }
    }

    private lateinit var wm: WindowManager
    private var view: OverlayView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val color = prefs.getInt("color", Color.RED)
        val thickness = prefs.getFloat("thickness", 3f)
        val alpha = prefs.getFloat("alpha", 1f)

        val v = OverlayView(this).apply { setStyle(color, thickness, alpha) }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        view = v
        wm.addView(v, p)
    }

    override fun onDestroy() {
        instance = null
        try { view?.let { wm.removeView(it) } } catch (_: Throwable) {}
        view = null
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Overlay", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("GameOverlay")
            .setContentText("Оверлей активен")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
}
