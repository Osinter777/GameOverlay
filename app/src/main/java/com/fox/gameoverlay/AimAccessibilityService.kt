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

        fun aimAt(x: Float, y: Float) {
            val svc = instance ?: return
            val now = System.currentTimeMillis()
            val prefs = svc.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val cooldown = prefs.getLong("aimCooldown", 80L)
            if (now - lastAimTime < cooldown) return
            lastAimTime = now
            val dur = prefs.getLong("aimDuration", 40L).coerceIn(1L, 500L)
            try {
                val path = Path().apply { moveTo(x, y) }
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
