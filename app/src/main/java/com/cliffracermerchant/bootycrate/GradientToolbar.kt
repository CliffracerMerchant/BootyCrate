/*
 * Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.Toolbar
import androidx.core.view.doOnNextLayout

/** A toolbar that allows for setting a gradient as a background and / or border.
 *
 *  GradientToolbar acts as a Toolbar, except that a gradient (in the form of a
 *  Shader) can be set as the background or as the paint to use for its optional
 *  border. Setting a gradient background this way (as opposed to, e.g. a Shape-
 *  Drawable with a gradient fill) allows for more customization (e.g. a radial
 *  gradient with different x and y radii).
 *
 *  The optional border can be set to draw along the GradientToolbar's top edge,
 *  bottom edge, both edges, or neither through the public property borderMode.
 *  The border's thickness can be set using the public property borderThickness */
open class GradientToolbar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs)
{
    protected val backgroundPaint = Paint()
    protected val borderPaint = Paint()

    enum class BorderMode { Top, Bottom, TopBottom, None }
    var borderMode: BorderMode
    var borderThickness: Float

    init {
        setWillNotDraw(false)
        val a = context.obtainStyledAttributes(attrs, R.styleable.GradientToolbar)
        borderMode = BorderMode.values()[a.getInt(R.styleable.GradientToolbar_borderMode, 0)]
        borderThickness = a.getDimension(R.styleable.GradientToolbar_borderThickness, 0f)
        a.recycle()

        borderPaint.style = Paint.Style.FILL
        backgroundPaint.style = Paint.Style.FILL
    }

    fun setBackgroundGradient(gradient: Shader) { backgroundPaint.shader = gradient }
    fun setBorderGradient(gradient: Shader) { borderPaint.shader = gradient }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return
        canvas.drawRect(left * 1f, top * 1f, right * 1f, bottom * 1f, backgroundPaint)
        if (borderMode == BorderMode.Top || borderMode == BorderMode.TopBottom)
            canvas.drawRect(left * 1f, top * 1f, left + width * 1f,
                            top + borderThickness, borderPaint)
        if (borderMode == BorderMode.Bottom || borderMode == BorderMode.TopBottom)
            canvas.drawRect(left * 1f, bottom  - borderThickness,
                            left + width * 1f, bottom * 1f, borderPaint)
    }
}

class TopActionBar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {
    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TopActionBar)
        val borderWidth = a.getDimension(R.styleable.TopActionBar_bottomBorderWidth, 0f)
        a.recycle()
        doOnNextLayout {
            val backgroundPathData = "L $width,0 L $width,$height L 0,$height Z"
            val backgroundDrawable = GradientVectorDrawable(
                width, height, width * 1f, height * 1f, backgroundPathData)

            val borderPathData = "M 0,${height - borderWidth} L $width,${height - borderWidth} L $width,$height L 0,$height Z"
            val borderDrawable = GradientVectorDrawable(
                width, height, width * 1f, height * 1f, borderPathData)

            background = LayerDrawable(arrayOf(backgroundDrawable, borderDrawable))
        }
    }
    fun initGradients(backgroundGradient: Shader, borderGradient: Shader) = doOnNextLayout {
        (background as LayerDrawable).apply {
            (getDrawable(0) as GradientVectorDrawable).gradient = backgroundGradient
            (getDrawable(1) as GradientVectorDrawable).gradient = borderGradient
        }
    }
}