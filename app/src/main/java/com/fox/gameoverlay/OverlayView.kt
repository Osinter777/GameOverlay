package com.fox.gameoverlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

@SuppressLint("ViewConstructor")
class OverlayView(context: Context) : View(context) {

    private val boxes = ArrayList<Detection>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 0, 0, 0) }

    private var srcW = 1
    private var srcH = 1
    private var frame: RectF = RectF()
    private var lastWidth = 0
    private var lastHeight = 0

    fun setStyle(color: Int, thickness: Float, alpha: Float) {
        paint.color = color
        paint.strokeWidth = thickness
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        invalidate()
    }

    fun setDetections(list: List<Detection>, sw: Int, sh: Int) {
        boxes.clear()
        boxes.addAll(list)
        if (sw > 0) srcW = sw
        if (sh > 0) srcH = sh
        recalcFrame(lastWidth, lastHeight)
        invalidate()
    }

    fun screenRect(d: Detection): RectF {
        if (frame.width() <= 0f) return RectF()
        val scale = frame.width() / srcW
        return RectF(
            frame.left + d.rect.left * scale,
            frame.top + d.rect.top * scale,
            frame.left + d.rect.right * scale,
            frame.top + d.rect.bottom * scale
        )
    }

    fun closestDetection(): Detection? {
        if (boxes.isEmpty() || frame.width() <= 0f) return null
        val cx = frame.centerX()
        val cy = frame.centerY()
        var best: Detection? = null
        var bestDist = Float.MAX_VALUE
        for (d in boxes) {
            val r = screenRect(d)
            val dx = r.centerX() - cx
            val dy = r.centerY() - cy
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                bestDist = dist
                best = d
            }
        }
        return best
    }

    private fun recalcFrame(w: Int, h: Int) {
        if (w <= 0 || h <= 0 || srcW <= 0 || srcH <= 0) return
        val scale = minOf(w.toFloat() / srcW, h.toFloat() / srcH)
        val dx = (w - srcW * scale) / 2f
        val dy = (h - srcH * scale) / 2f
        frame = RectF(dx, dy, dx + srcW * scale, dy + srcH * scale)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        lastWidth = w
        lastHeight = h
        recalcFrame(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (boxes.isEmpty()) return
        if (frame.width() <= 0f) return
        val scale = frame.width() / srcW

        for (d in boxes) {
            val l = frame.left + d.rect.left * scale
            val t = frame.top + d.rect.top * scale
            val r = frame.left + d.rect.right * scale
            val b = frame.top + d.rect.bottom * scale
            canvas.drawRect(l, t, r, b, paint)

            val label = "${d.label} ${(d.score * 100).toInt()}%"
            val tw = textPaint.measureText(label)
            val th = textPaint.textSize
            canvas.drawRect(l, t - th - 8f, l + tw + 12f, t, bgPaint)
            canvas.drawText(label, l + 6f, t - 6f, textPaint)
        }
    }
}
