/*  Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatImageView
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
    private val iconSize: Int
    private val iconPathSize: Float
    private val borderPath = Path()
    private val borderPaint = Paint()
    private val bgPathData = context.getString(R.string.circle_path_data)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundGradientButton)
        iconPathData = a.getString(R.styleable.RoundGradientButton_iconPathData) ?: ""
        iconSize = a.getDimensionPixelSize(R.styleable.RoundGradientButton_iconSize, 0)
        iconPathSize = a.getFloat(R.styleable.RoundGradientButton_iconPathSize, 0f)
        a.recycle()

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f,
                                                            context.resources.displayMetrics)

        doOnNextLayout {
            val rect = Rect()
            getDrawingRect(rect)
            RectF(rect).apply {
                // The background circle is inset by half of the border thickness
                // to make sure it draws entirely inside the button's bounds.
                val inset = borderPaint.strokeWidth / 2
                inset(inset, inset)
                borderPath.arcTo(this, 270f, 180f, true)
                borderPath.arcTo(this, 90f, 180f, true)
            }
        }
    }

    fun setForegroundGradient(fgGradient: Shader) {
        setImageDrawable(GradientVectorDrawable(
            iconSize, iconPathSize, iconPathData, fgGradient))
        borderPaint.shader = fgGradient
    }

    fun setBackgroundGradient(bgGradient: Shader) = doOnNextLayout {
            background = GradientVectorDrawable(width, 2f, bgPathData, bgGradient)
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(borderPath, borderPaint)
    }
}