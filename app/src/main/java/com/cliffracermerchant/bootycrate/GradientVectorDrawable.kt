/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser

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
    private val width: Int,
    private val height: Int,
    pathWidth: Float,
    pathHeight: Float,
    pathData: String,
    gradient: Shader
) : Drawable() {
    private val paint = Paint()
    private var path = Path()

    // For when the size and pathSize are both squares.
    constructor(size: Int, pathSize: Float, pathData: String, gradient: Shader) :
        this(size, size, pathSize, pathSize, pathData, gradient)

    init {
        paint.style = Paint.Style.FILL
        paint.shader = gradient
        setPathData(pathData, pathWidth, pathHeight)
    }

    fun setGradient(gradient: Shader) { paint.shader = gradient }

    fun setPathData(pathData: String, pathWidth: Float, pathHeight: Float) {
        path = PathParser.createPathFromPathData(pathData)
        val matrix = Matrix()
        matrix.setScale(width / pathWidth, height / pathHeight)
        path.transform(matrix)
    }

    override fun draw(canvas: Canvas) = canvas.drawPath(path, paint)

    override fun getIntrinsicWidth() = width
    override fun getIntrinsicHeight() = height

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }

    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }

    override fun getOpacity(): Int = when (paint.alpha) { 0    -> PixelFormat.TRANSPARENT
                                                          255  -> PixelFormat.OPAQUE
                                                          else -> PixelFormat.TRANSLUCENT }
}
