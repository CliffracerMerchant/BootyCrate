/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** A colored circle Drawable.
 *
 *  ColoredCircleDrawable is similar to a circular ShapeDrawable, except that its
 *  color is more easily changed, and it has an outline that will be drawn only if
 *  its color is 0 (i.e. transparent). */
class ColoredCircleDrawable(size: Float, color: Int, outlineColor: Int): Drawable() {
    private val paint = Paint()
    private val outlinePaint = Paint()
    private var centerX = size / 2

    var radius = size / 2f
        set(value) { field = value
            centerX = value * 3.5f/2f }
    var color: Int
        get() = paint.color
        set(value) {
            paint.color = value
            invalidateSelf()
        }

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

    fun setColor(newColor: Int, animate: Boolean = true) {
        if (!animate) color = newColor
        else ObjectAnimator.ofArgb(this, "color", color, newColor).start()
    }

    override fun setAlpha(alpha: Int) { }
    override fun getOpacity(): Int = PixelFormat.OPAQUE
    override fun setColorFilter(colorFilter: ColorFilter?) { }
}