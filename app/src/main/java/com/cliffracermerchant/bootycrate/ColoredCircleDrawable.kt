/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

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

    var radius = size / 3.5f
        set(value) { field = value
            centerX = value * 3.5f/2f }
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