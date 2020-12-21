/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.getDrawableOrThrow

/** A button that has a foreground tint that is separate from its background tint.
 *
 *  TintableForegroundButton is a button that accepts two drawables to draw
 *  itself, a backgroundDrawable and a foreground drawable. Both of these prop-
 *  erties must be set through XML. Thereafter, the background and foreground
 *  tints can be changed independently of one another. In addition to the fore-
 *  groundDrawble, the foregroundTint property will also be used as the default
 *  text color. */
class TintableForegroundButton(context: Context, attrs: AttributeSet) :
    AppCompatButton(context, attrs)
{
    val backgroundDrawable: Drawable get() = (background as LayerDrawable).getDrawable(0)
    val foregroundDrawable: Drawable get() = (background as LayerDrawable).getDrawable(1)
    var backgroundTint: Int = 0
        set(tint) { field = tint
                    backgroundDrawable.setTintList(ColorStateList.valueOf(tint)) }
    var foregroundTint: Int = 0
        set(tint) { field = tint
                    foregroundDrawable.setTintList(ColorStateList.valueOf(tint))
                    setTextColor(tint) }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TintableForegroundButton)
        val bgDrawable = a.getDrawableOrThrow(R.styleable.TintableForegroundButton_backgroundDrawable)
        val fgDrawable = a.getDrawableOrThrow(R.styleable.TintableForegroundButton_foregroundDrawable)
        background = LayerDrawable(arrayOf(bgDrawable, fgDrawable))
        backgroundTint = a.getColor(R.styleable.TintableForegroundButton_backgroundTint, 0)
        foregroundTint = a.getColor(R.styleable.TintableForegroundButton_foregroundTint, 0)
        a.recycle()
    }
}