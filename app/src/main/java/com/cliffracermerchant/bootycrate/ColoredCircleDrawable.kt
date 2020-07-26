package com.cliffracermerchant.bootycrate

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class ColoredCircleDrawable(size: Float, color: Int, outlineColor: Int): Drawable() {
    private val radius = size / 3.5f
    private val centerX = size / 2
    private val paint = Paint()
    private val outlinePaint = Paint()
    var color: Int
        get() = paint.color
        set(value) { paint.color = value; invalidateSelf() }

    init {
        this.color = color
        paint.style = Paint.Style.FILL
        outlinePaint.color = outlineColor
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeWidth = 3f
    }

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerX, radius,
                          if (color != 0) paint
                          else            outlinePaint)
    }

    override fun setAlpha(alpha: Int) { }
    override fun getOpacity(): Int = PixelFormat.OPAQUE
    override fun setColorFilter(colorFilter: ColorFilter?) { }
}