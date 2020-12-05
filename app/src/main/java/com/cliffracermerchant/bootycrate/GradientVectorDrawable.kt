/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser
import androidx.core.graphics.withSave

/** A vector drawable that strokes a path supplied in the constructor with a gradient background.
 *
 *  GradientVectorDrawable is a custom drawable that draws a vector based on
 *  the path data supplied to its constructor with a gradient background. The
 *  gradient is passed to the GradientVectorDrawable in the form of a Gradient-
 *  Builder along with the coordinates of the drawable's parent so that the
 *  drawable can offset the gradient supplied by the parent's position.
 *
 *  While the same effect of having a VectorDrawable with a gradient background
 *  is achievable using the Android API's native VectorDrawable, GradientVector-
 *  Drawable is different in that it can take a gradient in global coordinates
 *  and offset it by the drawable's position to achieve an effect that the nat-
 *  ive VectorDrawable cannot.
 *
 *  NOTE: Due to the project's target API level of 21's inability to inflate
 *  custom drawables from XML, GradientVectorDrawable does not have an XML
 *  constructor. In case this is changed in the future, attributes for all of
 *  the required parameters (besides the gradient builder and the parent coor-
 *  dinates, which are unknowable before runtime and would have to be set using
 *  setGradient) are provided in res/attrs.xml. */
class GradientVectorDrawable(
    private val width: Float,
    private val height: Float,
    private val viewportWidth: Float,
    private val viewportHeight: Float,
    pathData: String,
    gradientBuilder: GradientBuilder,
    parentX: Float,
    parentY: Float
) : Drawable() {

    var pathData: String = ""
        set(value) { field = value; path = PathParser.createPathFromPathData(value) }
    private val paint = Paint()
    private var path = Path()

    // A secondary constructor in case the size and viewport size are squares.
    constructor(size: Float, viewPortSize: Float, pathData: String,
                gradientBuilder: GradientBuilder, parentX: Float, parentY: Float) :
        this(size, size, viewPortSize, viewPortSize, pathData, gradientBuilder, parentX, parentY)

    init {
        paint.strokeWidth = 0f
        paint.style = Paint.Style.FILL
        bounds = Rect(0, 0, width.toInt(), height.toInt())
        this.pathData = pathData
        setShader(gradientBuilder, parentX, parentY)
    }

    fun setShader(gradientBuilder: GradientBuilder, xOffset: Float, yOffset: Float) {
        val originalX1 = gradientBuilder.x1
        val originalY1 = gradientBuilder.y1
        paint.shader = gradientBuilder.setX1(originalX1 - xOffset).
                                       setY1(originalY1 - yOffset).
                                       buildRadialGradient()
        gradientBuilder.setX1(originalX1).setY1(originalY1)
    }

    override fun draw(canvas: Canvas) {
        val xScale = width / viewportWidth
        val yScale = height / viewportHeight
        canvas.withSave {
            scale(xScale, yScale)
            drawPath(path, paint)
        }
    }

    override fun getIntrinsicWidth(): Int = width.toInt()
    override fun getIntrinsicHeight(): Int = height.toInt()

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }

    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }

    override fun getOpacity(): Int = when (paint.alpha) { 0    -> PixelFormat.TRANSPARENT
                                                          255  -> PixelFormat.OPAQUE
                                                          else -> PixelFormat.TRANSLUCENT }
}