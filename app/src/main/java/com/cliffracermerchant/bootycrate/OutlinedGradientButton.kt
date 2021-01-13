/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.view.doOnNextLayout

/** A button with a vector background and an outline that are both tintable with gradients.
 *
 *  OutlinedGradientButton is a button that accepts two paths that define its
 *  vector background and outline, backgroundPathData and outlinePathData. Both
 *  of these paths' gradient shaders can be set independently of each other via
 *  the properties backgroundGradient and outlineGradient. In addition to the
 *  outline, the foregroundGradient property will also be used as the shader
 *  for any text. In order for the path data to be interpreted correctly, the
 *  XML properties pathWidth and pathHeight must also be set in XML. The stroke
 *  width of the outline can be set through the XML property outlineStrokeWidth.
 *
 *  Note that due to its background being initialized only after it is laid out,
 *  trying to set backgroundGradient or outlineGradient before OutlinedGradient-
 *  Button is laid will not work. If the gradients need to be set during start-
 *  up before OutlinedGradient is laid out, use the function initGradients with
 *  the desired background gradient and outline gradient instead. */
@SuppressLint("ResourceType")
open class OutlinedGradientButton(context: Context, attrs: AttributeSet) :
    AppCompatButton(context, attrs)
{
    val backgroundDrawable get() = try { (background as? LayerDrawable)?.getDrawable(0) as? GradientVectorDrawable }
                                   catch(e: IndexOutOfBoundsException) { null }
    val outlineDrawable get() = try { (background as? LayerDrawable)?.getDrawable(1) as? GradientVectorDrawable }
                                catch(e: IndexOutOfBoundsException) { null }
    var backgroundGradient get() = backgroundDrawable?.gradient
                           set(value) { backgroundDrawable?.gradient = value  }
    var outlineGradient get() = outlineDrawable?.gradient
                        set(value) { outlineDrawable?.gradient = value
                                     paint.shader = value }


    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.OutlinedGradientButton)
        val backgroundPathData = a.getString(R.styleable.OutlinedGradientButton_backgroundPathData) ?: ""
        val outlinePathData = a.getString(R.styleable.OutlinedGradientButton_outlinePathData) ?: ""
        val outlineStrokeWidth = a.getDimension(R.styleable.OutlinedGradientButton_outlineStrokeWidth, 0f)
        // Apparently if attrs are retrieved with a manual IntArray, they must be in numerical id order to work.
        // Log.d("", "R.attr.pathWidth = ${R.attr.pathWidth}, R.attr.pathHeight=${R.attr.pathHeight}")
        a = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.pathHeight, R.attr.pathWidth))
        val pathHeight = a.getFloat(0, 0f)
        val pathWidth = a.getFloat(1, 0f)
        a.recycle()

        doOnNextLayout {
            val background = GradientVectorDrawable(width, height, pathWidth, pathHeight, backgroundPathData )
            val outline = GradientVectorDrawable(width, height, pathWidth, pathHeight, outlinePathData)
            outline.strokeWidth = outlineStrokeWidth
            outline.style = Paint.Style.STROKE
            this.background = LayerDrawable(arrayOf(background, outline))
        }
    }

    fun initGradients(backgroundGradient: Shader, outlineGradient: Shader) = doOnNextLayout {
        this.backgroundGradient = backgroundGradient
        this.outlineGradient = outlineGradient
    }
}