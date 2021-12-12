/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.*
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
 *
 * When in or not in color edit mode the check box's contentDescription
 * attribute will be equal to the value of the properties editColorContentDescription
 * or checkBoxContentDescription, respectively. The XML attribute colorDescriptionsResId
 * must be set to reference a string array of at least equal length to the
 * colors array. The description of the current color will then be included
 * in TintableCheckbox accessibility description.
 *
 * TintableCheckbox assumes it is instantiated inside an instance of FragmentActivity
 * in order to show child DialogFragments. If this is not the case, the color picker
 * will not be shown when the TintableCheckbox is clicked while inColorEditMode is true.
 */
class TintableCheckbox(context: Context, attrs: AttributeSet) : AppCompatImageButton(context, attrs) {

    private var _isChecked = false
    var isChecked get() = _isChecked
        set(checked) = setIsCheckedPrivate(checked)

    val colors: IntArray
    val colorDescriptions: Array<String>
    val color get() = colors[_colorIndex]
    private var _colorIndex = 0
    var colorIndex get() = _colorIndex
        set(value) = setColorIndex(value)

    private var _inColorEditMode = false
    var inColorEditMode get() = _inColorEditMode
                        set(value) = setInColorEditMode(value)

    var onCheckedChangedListener: ((Boolean) -> Unit)? = null
    var onColorChangedListener: ((Int) -> Unit)? = null

    var checkBoxContentDescription: String? = null
    var editColorContentDescription: String? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TintableCheckbox)
        val colorsResId = a.getResourceIdOrThrow(R.styleable.TintableCheckbox_colorsResId)
        val colorDescriptionsResId = a.getResourceIdOrThrow(R.styleable.TintableCheckbox_colorDescriptionsResId)
        colors = getColors(context, colorsResId)
        colorDescriptions = getColorDescriptions(context, colorDescriptionsResId)
        if (colorDescriptions.size < colors.size) throw IllegalStateException(
            "The color descriptions array must be at least the same size as the colors array.")
        a.recycle()

        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.tintable_checkbox))
        val checkMarkDrawable = (drawable as LayerDrawable).getDrawable(1)
        checkMarkDrawable.setTint(Color.BLACK)
        setOnClickListener {
            if (inColorEditMode) {
                val activity = context as? FragmentActivity ?: return@setOnClickListener
                val colorPicker = ColorSheet().cornerRadius(0f)
                    .colorPicker(colors, color) { color: Int ->
                        val colorIndex = colors.indexOf(color)
                        setColorIndex(if (colorIndex != -1) colorIndex else 0)
                    }
                val transaction = activity.supportFragmentManager.beginTransaction()
                    .runOnCommit { updateColorPickerContentDescriptions(colorPicker) }
                colorPicker.show(transaction, "TintableCheckboxColorPicker")
            }
            else isChecked = !isChecked
        }

        ViewCompat.setAccessibilityDelegate(this, object: AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                val colorState = colorDescriptions[colorIndex]
                val checkedState = if (inColorEditMode) "" else context.getString(
                    if (isChecked) R.string.checkbox_checked_description
                    else           R.string.checkbox_unchecked_description)
                info.stateDescription = "$colorState, $checkedState"
                info.contentDescription = if (inColorEditMode) editColorContentDescription
                                          else                 checkBoxContentDescription
                info.className = ""
            }
        })
    }

    fun setInColorEditMode(inColorEditMode: Boolean, animate: Boolean = true) {
        _inColorEditMode = inColorEditMode
        refreshDrawableState()
        if (!animate) drawable.jumpToCurrentState()
    }

    /** Set the color index without calling the onColorChangedListener. */
    fun initColorIndex(colorIndex: Int) {
        _colorIndex = colorIndex.coerceIn(colors.indices)
        (drawable as LayerDrawable).getDrawable(0).setTint(color)
    }

    /** Set the color of the checkbox to the color defined by colors[colorIndex],
     * and call onColorChangedListener with the new color. */
    fun setColorIndex(colorIndex: Int, animate: Boolean = true) {
        if (colorIndex == _colorIndex) return
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

    private fun setIsCheckedPrivate(isChecked: Boolean) {
        _isChecked = isChecked
        val newState = android.R.attr.state_checked * if (isChecked) 1 else -1
        setImageState(intArrayOf(newState), true)
        onCheckedChangedListener?.invoke(isChecked)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray =
        if (!_inColorEditMode)
            super.onCreateDrawableState(extraSpace)
        else super.onCreateDrawableState(extraSpace + 1).apply {
            mergeDrawableStates(this, intArrayOf(R.attr.state_edit_color))
        }

    private fun updateColorPickerContentDescriptions(colorPicker: ColorSheet) =
        (colorPicker.view as? LinearLayout)?.apply {
            findViewById<ImageView>(R.id.colorSheetClose)?.contentDescription =
                context.getString(R.string.color_picker_close_description)

            val recyclerView = findViewById<RecyclerView>(R.id.colorSheetList)
            recyclerView.addOnChildAttachStateChangeListener(object: RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    val circle = view.findViewById<ImageView>(R.id.colorSelectedCircle)
                    val tint = circle.imageTintList?.defaultColor ?: 1
                    val index = colors.indexOf(tint)
                    val colorName = colorDescriptions[index]
                    circle.contentDescription =
                        context.getString(R.string.color_picker_option_description, colorName)
                }
                override fun onChildViewDetachedFromWindow(view: View) { }
            })
        }

    private companion object {
        val colorsCache = mutableMapOf<Int, IntArray>()
        val colorDescriptionsCache = mutableMapOf<Int, Array<String>>()

        /** Return the IntArray representing color values pointed to by the
         * parameter arrayResId. In order to prevent multiple copies of the
         * same array, getColors will automatically share references of the
         * same array between TintableCheckboxes that use the same colors */
        fun getColors(context: Context, arrayResId: Int) =
            colorsCache.getOrPut(arrayResId) { context.getIntArray(arrayResId) }

        /** Return the StringArray representing color descriptions pointed
         * to by the parameter arrayResId. In order to prevent multiple
         * copies of the same array, getColorDescriptions will automatically
         * share references of the same array between TintableCheckboxes
         * that use the same colors */
        fun getColorDescriptions(context: Context, arrayResId: Int) =
            colorDescriptionsCache.getOrPut(arrayResId) {
                context.resources.getStringArray(arrayResId)
            }
    }
}