/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat

/**
 * A button that acts as a checkbox with easily an customizable color.
 *
 * TintableCheckbox acts as a tinted checkbox that can be toggled between a
 * normal checkbox mode (where the checked state can be toggled) and a color
 * editing mode (where the checkbox will morph into a tinted circle, and a
 * click will open a color picker dialog). The mode is changed via the pro-
 * perty inColorEditMode, while the current color is changed through the
 * property color (or colorIndex if setting the color to an value of View-
 * ModelItem.Colors is preferred.
 */
class TintableCheckbox(context: Context, attrs: AttributeSet) :
    AppCompatImageButton(context, attrs)
{
    var inColorEditMode = false
        set(value) { field = value
                     refreshDrawableState() }
    private var _isChecked = false
    var isChecked get() = _isChecked
        set(checked) { _isChecked = checked
                       val newState = android.R.attr.state_checked * if (checked) 1 else -1
                       setImageState(intArrayOf(newState), true)
                       onCheckedChangedListener?.invoke(checked) }

    var onCheckedChangedListener: ((Boolean) -> Unit)? = null
    var onColorChangedListener: ((Int) -> Unit)? = null

    private var _color = 0
    var color get() = _color
              set(value) { setColor(value) }
    var colorIndex get() = ViewModelItem.Colors.indexOf(color)
                   set(value) = setColorIndex(value)

    init {
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.tintable_checkbox))
        val checkMarkDrawable = (drawable as LayerDrawable).getDrawable(1)
        checkMarkDrawable.setTint(ContextCompat.getColor(context, android.R.color.black))
        setOnClickListener {
            val activity = context as? MainActivity ?: return@setOnClickListener
            if (inColorEditMode)
                showColorPickerDialog(activity.supportFragmentManager,
                                      colorIndex, ::setColorIndex)
            else isChecked = !isChecked
        }
    }

    fun setColor(newColor: Int, animate: Boolean = false) {
        val checkboxBg = (drawable as LayerDrawable).getDrawable(0)
        if (!animate) checkboxBg.setTint(newColor)
        else valueAnimatorOfArgb(checkboxBg::setTint, color, newColor).start()
        _color = newColor
        onColorChangedListener?.invoke(newColor)
    }

    fun setColorIndex(colorIndex: Int, animate: Boolean = false) {
        val index = colorIndex.coerceIn(ViewModelItem.Colors.indices)
        setColor(ViewModelItem.Colors[index], animate)
    }

    /** Set the check state without calling the onCheckedChangeListener. */
    fun initIsChecked(isChecked: Boolean) {
        _isChecked = isChecked
        val newState = android.R.attr.state_checked * if (isChecked) 1 else -1
        setImageState(intArrayOf(newState), true)
    }

    // For some reason when the CheckboxAndColorEdit's visibility is set to gone
    // and later made visible again, the drawable state is not preserved correctly.
    // Setting inColorEditMode to false and then true again is a workaround.
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE && inColorEditMode) {
            inColorEditMode = false
            inColorEditMode = true
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        return if (inColorEditMode)
            super.onCreateDrawableState(extraSpace + 1).apply {
                mergeDrawableStates(this, intArrayOf(R.attr.state_edit_color))
            }
        else super.onCreateDrawableState(extraSpace)
    }
}