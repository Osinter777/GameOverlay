package com.fox.gameoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class FloatingMenuService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_menu"
        private const val NOTIF_ID = 2001
        private const val PREFS = "settings"

        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"

        private const val NEON = 0xFF39FF14.toInt()
        private const val BG_MENU = 0xCC101010.toInt()
        private const val BG_ICON = 0xCC0A0A0A.toInt()
    }

    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences

    private var iconView: View? = null
    private var iconParams: WindowManager.LayoutParams? = null
    private var menuView: View? = null
    private var menuParams: WindowManager.LayoutParams? = null

    private var menuOpen = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        addIcon()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeMenu()
        try { iconView?.let { wm.removeView(it) } } catch (_: Throwable) {}
        iconView = null
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "FloatingMenu", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("GameOverlay")
            .setContentText("Меню активно")
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setOngoing(true)
            .build()

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    private fun addIcon() {
        val size = dp(56)
        val tv = TextView(this).apply {
            text = "G"
            setTextColor(NEON)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(BG_ICON)
                setStroke(dp(2), NEON)
            }
        }

        val p = WindowManager.LayoutParams(
            size, size,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        tv.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = p.x; startY = p.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (kotlin.math.abs(dx) > dp(6) || kotlin.math.abs(dy) > dp(6)) moved = true
                    if (moved) {
                        p.x = startX + dx.toInt()
                        p.y = startY + dy.toInt()
                        try { wm.updateViewLayout(tv, p) } catch (_: Throwable) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleMenu()
                    true
                }
                else -> false
            }
        }

        iconView = tv
        iconParams = p
        wm.addView(tv, p)
    }

    private fun toggleMenu() {
        if (menuOpen) removeMenu() else addMenu()
    }

    private fun removeMenu() {
        menuOpen = false
        try { menuView?.let { wm.removeView(it) } } catch (_: Throwable) {}
        menuView = null
        menuParams = null
    }

    private fun addMenu() {
        if (menuOpen) return
        val v = buildMenuView()
        val p = WindowManager.LayoutParams(
            dp(240),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (iconParams?.x ?: dp(16)) + dp(64)
            y = iconParams?.y ?: dp(120)
        }

        menuView = v
        menuParams = p
        wm.addView(v, p)
        menuOpen = true
    }

    private fun buildMenuView(): View {
        val bg = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(BG_MENU)
            setStroke(dp(2), NEON)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
            background = bg
        }

        root.addView(title("GAME OVERLAY"))

        root.addView(toggleRow(
            label = "ESP",
            prefKey = "espEnabled",
            default = false
        ))

        root.addView(toggleRow(
            label = "AIM",
            prefKey = "aimEnabled",
            default = false
        ))

        root.addView(sliderRow(
            label = "FOV",
            prefKey = "fov",
            min = 100, max = 600, default = 300,
            suffix = "px"
        ))

        root.addView(sliderRow(
            label = "Aim Duration",
            prefKey = "aimDuration",
            min = 30, max = 300, default = 40,
            suffix = "ms",
            isLong = true
        ))

        root.addView(sliderRow(
            label = "Aim Cooldown",
            prefKey = "aimCooldown",
            min = 50, max = 300, default = 80,
            suffix = "ms",
            isLong = true
        ))

        return root
    }

    private fun title(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(NEON)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }

    private fun toggleRow(label: String, prefKey: String, default: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }

        val name = TextView(this).apply {
            text = label
            setTextColor(NEON)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }

        val state = TextView(this).apply {
            val cur = prefs.getBoolean(prefKey, default)
            text = if (cur) "ON" else "OFF"
            setTextColor(NEON)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
            setPadding(dp(8), 0, 0, 0)
        }

        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(name, lp)
        row.addView(state, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        row.isClickable = true
        row.setOnClickListener {
            val cur = prefs.getBoolean(prefKey, default)
            val next = !cur
            prefs.edit().putBoolean(prefKey, next).apply()
            state.text = if (next) "ON" else "OFF"
        }

        return row
    }

    private fun sliderRow(
        label: String,
        prefKey: String,
        min: Int,
        max: Int,
        default: Int,
        suffix: String,
        isLong: Boolean = false
    ): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val cur = if (isLong) prefs.getLong(prefKey, default.toLong()).toInt()
        else prefs.getInt(prefKey, default)

        val name = TextView(this).apply {
            text = label
            setTextColor(NEON)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }

        val value = TextView(this).apply {
            text = "$cur$suffix"
            setTextColor(NEON)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        }

        header.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(value, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = (cur - min).coerceIn(0, max - min)
            progressTintList = android.content.res.ColorStateList.valueOf(NEON)
            thumbTintList = android.content.res.ColorStateList.valueOf(NEON)
        }

        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val real = min + p
                value.text = "$real$suffix"
                val editor = prefs.edit()
                if (isLong) editor.putLong(prefKey, real.toLong())
                else editor.putInt(prefKey, real)
                editor.apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        wrap.addView(header)
        wrap.addView(bar)
        return wrap
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}
