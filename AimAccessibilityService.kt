package com.fox.gameoverlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class AimAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: AimAccessibilityService? = null
            private set

        @Volatile private var lastAimTime = 0L

        fun swipeTo(x: Float, y: Float) {
            val svc = instance ?: return
            val now = System.currentTimeMillis()
            val prefs = svc.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val cooldown = prefs.getLong("aimCooldown", 150L)
            if (now - lastAimTime < cooldown) return
            lastAimTime = now
            val dur = prefs.getLong("aimDuration", 120L).coerceIn(30L, 500L)

            try {
                val screenW = svc.resources.displayMetrics.widthPixels.toFloat()
                val screenH = svc.resources.displayMetrics.heightPixels.toFloat()
                val startX = screenW / 2f
                val startY = screenH / 2f

                val path = Path().apply {
                    moveTo(startX, startY)
                    lineTo(x, y)
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0L, dur))
                    .build()
                svc.dispatchGesture(gesture, null, null)
            } catch (_: Throwable) {
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
