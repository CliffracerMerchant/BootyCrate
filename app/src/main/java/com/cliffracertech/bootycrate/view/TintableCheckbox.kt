/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.content.Context
import android.graphics.drawable.AnimatedStateListDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.argbValueAnimator
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import com.cliffracertech.bootycrate.utils.getIntArray
import dev.sasikanth.colorsheet.ColorSheet

/**
 * A button that acts as a checkbox with an easily customizable color.
 *
 * TintableCheckbox acts as a tinted checkbox that can be toggled between a
 * normal checkbox mode (where the checked state can be toggled) and a color
 * editing mode (where the checkbox will morph into a tinted circle, and a
 * click will open a color picker dialog). The mode is changed via the property
 * inColorEditMode, or the function setInColorEditMode (which also allows
 * skipping the animation when the parameter animate == false).
 *
 * The current color is accessed through the property color. The color index
 * can be queried or changed through the property colorIndex. The colors that
 * can be chosen from are defined in the XML attribute colorsResId, and can be
 * accessed through the property colors. The property onColorChangedListener
 * can be set to invoke an action when the color is changed. The function
 * initColorIndex will set the color index without an animation and will not
 * call the onColorChangedListener.
 */
class TintableCheckbox(context: Context, attrs: AttributeSet) : AppCompatImageButton(context, attrs) {
    private var _inColorEditMode = false
    var inColorEditMode get() = _inColorEditMode
                        set(value) = setInColorEditMode(value)
    private var _isChecked = false
    var isChecked get() = _isChecked
        set(checked) { _isChecked = checked
                       val newState = android.R.attr.state_checked * if (checked) 1 else -1
                       setImageState(intArrayOf(newState), true)
                       onCheckedChangedListener?.invoke(checked) }

    var onCheckedChangedListener: ((Boolean) -> Unit)? = null
    var onColorChangedListener: ((Int) -> Unit)? = null

    val colors: IntArray
    val color get() = colors[_colorIndex]
    private var _colorIndex = 0
    var colorIndex get() = _colorIndex
                   set(value) = setColorIndex(value)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TintableCheckbox)
        val colorsResId = a.getResourceId(R.styleable.TintableCheckbox_colorsResId, 0)
        colors = context.getIntArray(colorsResId)
        a.recycle()

        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.tintable_checkbox))
        val checkMarkDrawable = (drawable as LayerDrawable).getDrawable(1)
        checkMarkDrawable.setTint(ContextCompat.getColor(context, android.R.color.black))
        setOnClickListener {
            if (inColorEditMode) {
                val activity = context.asFragmentActivity()
                ColorSheet().colorPicker(colors, color) { color: Int ->
                    val colorIndex = colors.indexOf(color)
                    setColorIndex(if (colorIndex != -1) colorIndex else 0)
                }.show(activity.supportFragmentManager)
            }
            else isChecked = !isChecked
        }
    }

    fun setInColorEditMode(inColorEditMode: Boolean, animate: Boolean = true) {
        _inColorEditMode = inColorEditMode
        refreshDrawableState()
        if (!animate) (drawable as? LayerDrawable)?.apply {
            (getDrawable(0) as? AnimatedStateListDrawable)?.jumpToCurrentState()
            (getDrawable(1) as? AnimatedStateListDrawable)?.jumpToCurrentState()
        }
    }

    /** Set the color index without calling the onColorChangedListener. */
    fun initColorIndex(colorIndex: Int) {
        _colorIndex = colorIndex.coerceIn(colors.indices)
        (drawable as LayerDrawable).getDrawable(0).setTint(color)
    }

    /** Set the color of the checkbox to the color defined by colors[colorIndex],
     * and call onColorChangedListener with the new color. */
    fun setColorIndex(colorIndex: Int, animate: Boolean = false) {
        val oldColor = color
        _colorIndex = colorIndex.coerceIn(colors.indices)
        val checkboxBg = (drawable as LayerDrawable).getDrawable(0)
        if (!animate) checkboxBg.setTint(color)
        else argbValueAnimator(checkboxBg::setTint, oldColor, color).start()
        onColorChangedListener?.invoke(color)
    }

    /** Set the check state without calling the onCheckedChangedListener. */
    fun initIsChecked(isChecked: Boolean) {
        _isChecked = isChecked
        val newState = android.R.attr.state_checked * if (isChecked) 1 else -1
        setImageState(intArrayOf(newState), true)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray =
        if (!_inColorEditMode)
            super.onCreateDrawableState(extraSpace)
        else super.onCreateDrawableState(extraSpace + 1).apply {
            mergeDrawableStates(this, intArrayOf(R.attr.state_edit_color))
        }
}