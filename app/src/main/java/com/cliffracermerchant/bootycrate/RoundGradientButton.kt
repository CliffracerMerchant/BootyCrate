/*  Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout

/** A round button that draws its icon, border, and background with a gradient.
 *
 *  RoundGradientButton uses a foreground and background gradient that are
 *  passed to it via the function setGradients. The foreground gradient is used
 *  to draw its outline border and its vector icon (whose path data and path
 *  size are defined using the XML attributes iconPathData and iconPathSize).
 *  The background gradient is used for filling its round background. */
class RoundGradientButton(context: Context, attrs: AttributeSet) :
    AppCompatImageView(context, attrs)
{
    private val iconPathData: String
    private val iconPathSize: Float
    private val borderPath = Path()
    private val borderPaint = Paint()

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundGradientButton)
        iconPathData = a.getString(R.styleable.RoundGradientButton_iconPathData) ?: ""
        iconPathSize = a.getFloat(R.styleable.RoundGradientButton_iconPathSize, 0f)
        a.recycle()

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f,
                                                            context.resources.displayMetrics)
        borderPaint.color = ContextCompat.getColor(context, R.color.colorAccent)

        doOnNextLayout {
            val rect = Rect()
            getDrawingRect(rect)
            // The background circle is inset by half of the border thickness
            // to make sure it draws entirely inside the buttons bounds.
            val inset = (borderPaint.strokeWidth / 2f).toInt()
            rect.inset(inset, inset)
            borderPath.arcTo(rect.left * 1f, rect.top * 1f,
                             rect.right * 1f, rect.bottom * 1f,
                             270f, 180f, true)
            borderPath.arcTo(rect.left * 1f, rect.top * 1f,
                             rect.right * 1f, rect.bottom * 1f,
                             90f, 180f, true)
        }
    }

    fun setGradients(foregroundGradient: Gradient.Builder, backgroundGradient: Gradient.Builder) {
        if (width != 0)
            setGradientsPrivate(foregroundGradient, backgroundGradient)
        else doOnNextLayout {
            setGradientsPrivate(foregroundGradient, backgroundGradient) }
    }

    private fun setGradientsPrivate(foregroundGradient: Gradient.Builder,
                                    backgroundGradient: Gradient.Builder) {
        setImageDrawable(GradientVectorDrawable.forParent(
            1f, iconPathSize, iconPathData, foregroundGradient, this))
        // bgPathData describes a round path with a radius equal to the button's width
        val bgPathData = "M 1,0 A 1 1 0 1 1 1, 2 A 1 1 0 1 1 1, 0"
        val bgPathSize = 2f
        background = GradientVectorDrawable.forParent(
            width * 1f, bgPathSize, bgPathData, backgroundGradient, this)
        borderPaint.shader = Gradient.radialWithParentOffset(foregroundGradient, this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(borderPath, borderPaint)
    }
}