/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

/** A button with a vector background and an outline that are both tintable with gradients.
 *
 *  OutlinedGradientButton is a button that accepts two paths that define its
 *  vector background and outline, backgroundPathData and outlinePathData. Both
 *  of these paths' gradient shaders can be set independently of each other via
 *  the properties backgroundGradient and outlineGradient. In addition to the
 *  outline, the outlineGradient property will also be used as the shader for
 *  any text. In order for the path data to be interpreted correctly, the XML
 *  properties pathWidth and pathHeight must also be set in XML. The stroke
 *  width of the outline can be set through the XML property outlineStrokeWidth. */
@SuppressLint("ResourceType")
open class OutlinedGradientButton(context: Context, attrs: AttributeSet) :
    AppCompatButton(context, attrs)
{
    val backgroundDrawable get() = _backgroundDrawable
    val outlineDrawable get() = _outlineDrawable
    protected var _backgroundDrawable: GradientVectorDrawable
    protected var _outlineDrawable: GradientVectorDrawable

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.OutlinedGradientButton)
        val backgroundPathData = a.getString(R.styleable.OutlinedGradientButton_backgroundPathData) ?: ""
        val outlinePathData = a.getString(R.styleable.OutlinedGradientButton_outlinePathData) ?: ""
        val outlineStrokeWidth = a.getDimension(R.styleable.OutlinedGradientButton_outlineStrokeWidth, 0f)
        // Apparently if more than one attr is retrieved with a manual
        // IntArray, they must be in numerical id order to work.
        // Log.d("", "R.attr.pathWidth = ${R.attr.pathWidth}, R.attr.pathHeight=${R.attr.pathHeight}")
        a = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.pathHeight, R.attr.pathWidth))
        val pathHeight = a.getFloat(0, 0f)
        val pathWidth = a.getFloat(1, 0f)
        a.recycle()

        _backgroundDrawable = GradientVectorDrawable(pathWidth, pathHeight, backgroundPathData )
        _outlineDrawable = GradientVectorDrawable(pathWidth, pathHeight, outlinePathData)
        outlineDrawable.strokeWidth = outlineStrokeWidth
        outlineDrawable.style = Paint.Style.STROKE
        this.background = LayerDrawable(arrayOf(backgroundDrawable, outlineDrawable))
    }
}