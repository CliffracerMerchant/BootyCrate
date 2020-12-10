/*
 * Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat

/** A button that acts as a checkbox with easily an customizable color.
 *
 *  ToggleableTintedCheckbox acts as a checkbox with a tintable (via the color
 *  property) background. When the property isEditable is set to false, the
 *  checkbox will change to a circle tinted the same color as the checkbox back-
 *  ground and its checkable state can not be changed until isEditable is
 *  changed back to true. */
class ToggleableTintedCheckbox(context: Context, attrs: AttributeSet) :
    AppCompatImageButton(context, attrs)
{
    var isEditable get() = isActivated
                   set(value) { isActivated = value }
    var onCheckedChangedListener: ((Boolean) -> Unit)? = null
    var isChecked = false
        set(value) { field = value
                     val newState = android.R.attr.state_checked * if (value) 1 else -1
                     setImageState(intArrayOf(newState), true)
                     onCheckedChangedListener?.invoke(value) }

    private var _color = 0
    var color get() = _color
        set(value) { val checkboxBackground = (drawable as LayerDrawable).getDrawable(0)
                     ObjectAnimator.ofArgb(checkboxBackground, "tint", color, value).apply {
                         if (value == 0)
                             doOnEnd { checkboxBackground.setTintList(null) }
                         start()
                     }
                     _color = value
        }

    init {
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.shopping_list_checkbox_icon))
        val checkMarkDrawable = (drawable as LayerDrawable).getDrawable(1)
        checkMarkDrawable.setTint(ContextCompat.getColor(context, android.R.color.black))
        setOnClickListener { if (isEditable) isChecked = !isChecked }
    }

    fun setColorWithoutAnimation(newColor: Int) {
        val checkboxBackground = (drawable as LayerDrawable).getDrawable(0)
        checkboxBackground.setTint(newColor)
        _color = newColor
    }
}