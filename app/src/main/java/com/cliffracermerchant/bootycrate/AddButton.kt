/*  Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.doOnNextLayout

/** A round add button that draws its icon, border, and background with a gradient.
 *
 *  AddButton uses a foreground and background gradient that are passed to it via
 *  the function initGradients. The foreground gradient is used to draw its outline
 *  border and its vector add icon. The background gradient is used for filling its
 *  round background. */
class AddButton(context: Context, attrs: AttributeSet) :
    AppCompatImageButton(context, attrs)
{
    init {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.borderWidth, typedValue, true)
        val borderWidth = TypedValue.complexToDimension(
            typedValue.data, context.resources.displayMetrics)
        doOnNextLayout {
            // The background circle is inset by half of the border thickness
            // to make sure it draws entirely inside the button's bounds.
            val radius = width / 2
            val circlePathData = "M $radius,0 A $radius,$radius 0 1 1 $radius,${2 * radius} " +
                                             "A $radius,$radius 0 1 1 $radius,0"
            background = GradientVectorDrawable(width, width * 1f, circlePathData)

            setPadding(0, 0, 0, 0)
            val inset = borderWidth / 2
            val insetRadius = radius - inset
            val insetCirclePathData = "M $radius,$inset A $insetRadius,$insetRadius 0 1 1 $radius,${height - inset} " +
                                                       "A $insetRadius,$insetRadius 0 1 1 $radius,$inset"

            // (56dp button size - 18dp target icon size) / 2 = 19dp padding
            val plusIconPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 19f,
                context.resources.displayMetrics)
            val plusPathData = "M $radius,$plusIconPadding L $radius,${width - plusIconPadding} " +
                               "M $plusIconPadding,$radius L ${width - plusIconPadding},$radius"

            setImageDrawable(GradientVectorDrawable(
                width, width * 1f,"$insetCirclePathData $plusPathData"
            ).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
            })
        }
    }
    fun initGradients(backgroundGradient: Shader, foregroundGradient: Shader) = doOnNextLayout {
        (background as GradientVectorDrawable).gradient = backgroundGradient
        (drawable as GradientVectorDrawable).gradient = foregroundGradient
    }
}