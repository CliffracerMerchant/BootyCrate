/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.Toolbar

/** A toolbar that has a bottom border and allows setting a gradient as a background and / or border.
 *
 *  TopActionBar acts as a Toolbar, except that a gradient (in the form of a
 *  Shader) can be set as the background or as the paint to use for its border.
 *  Setting a gradient background this way (as opposed to, e.g. a ShapeDrawable
 *  with a gradient fill) allows for more customization (e.g. a radial gradient
 *  with different x and y radii).
 *
 *  The border width is derived from the attr borderWidth. The background and
 *  border gradients can be set independently of each other through the prop-
 *  erties backgroundGradient and borderGradient. */
class TopActionBar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {
    val backgroundDrawable: GradientVectorDrawable
    val borderDrawable: GradientVectorDrawable

    var backgroundGradient: Shader? = null
        set(value) { field = value; backgroundDrawable.gradient = value }
    var borderGradient: Shader? = null
        set(value) { field = value; borderDrawable.gradient = value }

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.TopActionBar)
        val borderWidth = a.getDimension(R.styleable.TopActionBar_bottomBorderWidth, 0f)
        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.layout_height))
        val height = a.getDimensionPixelSize(0, 0)
        a.recycle()

        val width = context.resources.displayMetrics.widthPixels
        val backgroundPathData = "L $width,0 L $width,$height L 0,$height Z"
        backgroundDrawable = GradientVectorDrawable(width * 1f, height * 1f, backgroundPathData)

        val borderPathData = "M 0,${height - borderWidth / 2} L $width,${height - borderWidth / 2}"
        borderDrawable = GradientVectorDrawable(width * 1f, height * 1f, borderPathData)
        borderDrawable.style = Paint.Style.STROKE
        borderDrawable.strokeWidth = borderWidth

        backgroundDrawable.gradient = backgroundGradient
        borderDrawable.gradient = borderGradient
        background = LayerDrawable(arrayOf(backgroundDrawable, borderDrawable))
    }
}