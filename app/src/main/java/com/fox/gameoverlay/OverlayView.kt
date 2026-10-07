package com.fox.gameoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager

class OverlayView(private val context: Context) {
    private var view: CustomOverlayView? = null
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isShowing = false

    fun show() {
        if (!isShowing) {
            view = CustomOverlayView(context)
            val params = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
            }
            windowManager.addView(view, params)
            isShowing = true
        }
    }

    fun hide() {
        if (isShowing && view != null) {
            windowManager.removeView(view)
            isShowing = false
        }
    }

    fun updateDetections(detections: List<DetectionResult>) {
        view?.updateDetections(detections)
    }

    data class DetectionResult(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val confidence: Float,
        val label: String
    )

    private inner class CustomOverlayView(context: Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        private val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = 30f
        }

        private var detections = listOf<DetectionResult>()

        fun updateDetections(newDetections: List<DetectionResult>) {
            detections = newDetections
            postInvalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            for (detection in detections) {
                canvas.drawRect(
                    detection.x,
                    detection.y,
                    detection.x + detection.width,
                    detection.y + detection.height,
                    paint
                )

                val label = "${detection.label} ${String.format("%.2f", detection.confidence)}"
                canvas.drawText(label, detection.x, detection.y - 10, textPaint)
            }
        }
    }
}
