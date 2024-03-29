/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser

/**
 * A vector drawable that strokes a path supplied in the constructor with a gradient background.
 *
 * GradientVectorDrawable is a custom drawable that draws a vector based on
 * the path data supplied to its constructor with a gradient background. The
 * gradient used is set through the public property gradient.
 *
 * While the same effect of having a VectorDrawable with a gradient background
 * is achievable using the Android API's native VectorDrawable, GradientVector-
 * Drawable makes it easier to achieve certain effects (such a global back-
 * ground gradient that "shows through" certain UI components) due to its use
 * of the paint.shader property.
 */
class GradientVectorDrawable(
    private val pathWidth: Float,
    private val pathHeight: Float,
    pathData: String
) : Drawable() {
    private val paint = Paint()
    private val originalPath: Path = PathParser.createPathFromPathData(pathData)
    protected var path = Path()
    protected val matrix = Matrix()

    var style: Paint.Style get() = paint.style
                           set(value) { paint.style = value }
    var strokeWidth get() = paint.strokeWidth
                    set(value) { paint.strokeWidth = value }
    var gradient: Shader? get() = paint.shader
                          set(value) { paint.shader = value }

    // For when pathSize is a square.
    constructor(pathSize: Float, pathData: String) : this(pathSize, pathSize, pathData)

    override fun onBoundsChange(bounds: Rect) {
        path.set(originalPath)
        matrix.setScale(bounds.width() / pathWidth,
                        bounds.height() / pathHeight)
        path.transform(matrix)
    }

    override fun setTint(tintColor: Int) { paint.color = tintColor }

    override fun draw(canvas: Canvas) {
        if (paint.style != Paint.Style.STROKE || paint.strokeWidth != 0f)
            canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }

    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }

    override fun getOpacity(): Int = when (paint.alpha) { 0    -> PixelFormat.TRANSPARENT
                                                          255  -> PixelFormat.OPAQUE
                                                          else -> PixelFormat.TRANSLUCENT }
}

/** A simple drawable that draws the path described by its property path. Both
 * the path and the paint used to draw the path (accessed through the property
 * paint) are fully public in order to allow for easy customization. */
class PathDrawable : Drawable() {
    var paint = Paint()
    val path = Path()

    override fun draw(canvas: Canvas) = canvas.drawPath(path, paint)

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }

    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }

    override fun getOpacity(): Int = when (paint.alpha) { 0    -> PixelFormat.TRANSPARENT
                                                          255  -> PixelFormat.OPAQUE
                                                          else -> PixelFormat.TRANSLUCENT }
}