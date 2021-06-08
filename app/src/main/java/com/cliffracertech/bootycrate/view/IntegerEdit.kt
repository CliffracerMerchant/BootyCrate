/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.animation.Animator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.IntegerEditBinding
import com.cliffracertech.bootycrate.utils.*

/**
 * A compound view to edit an integer quantity.
 *
 * IntegerEdit is a compound view that combines an EditText displaying an
 * integer with two image buttons on its left and right to act as decrease and
 * increase buttons. The property onValueChangedListener can be set to invoke
 * an action whenever the value is changed.
 *
 * The EditText is by default not focusable in touch mode, but this can be
 * changed by setting the property valueIsFocusable. If set to true, the user
 * can edit the value directly with a keyboard. When in the editable state, the
 * EditText will underline the current value to indicate this to the user, and
 * will, if necessary, expand the width of the value to R.dimen.integer_edit_editable_value_min_width
 * to make it a larger touch target.
 */
open class IntegerEdit(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    // value = value is not pointless due to value's custom getter and setter
    var minValue = 0
        set(newMin) { field = newMin; value = value }
    var maxValue = 0
        set(newMax) { field = newMax; value = value }
    var stepSize: Int

    var onValueChangedListener: ((Int)->Unit)? = null
    var value get() = try { ui.valueEdit.text.toString().toInt() }
                      catch (e: Exception) { 0 }
              set(newValue) { val adjustedNewValue = newValue.coerceIn(minValue, maxValue)
                              ui.valueEdit.setText(adjustedNewValue.toString())
                              onValueChangedListener?.invoke(adjustedNewValue) }
    var valueIsFocusable get() = ui.valueEdit.isFocusableInTouchMode
        set(value) {
            ui.valueEdit.isFocusableInTouchMode = value
            if (!value && ui.valueEdit.isFocused)
                ui.valueEdit.clearFocus()
            underlineAlpha = if (value) 255 else 0
            ui.valueEdit.minWidth = if (!value) 0 else
                resources.getDimensionPixelSize(R.dimen.integer_edit_editable_value_min_width)
        }

    val ui = IntegerEditBinding.inflate(LayoutInflater.from(context), this)
    protected var underlineAlpha = 0

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.IntegerEdit)
        maxValue = a.getInt(R.styleable.IntegerEdit_maxValue, 999)
        minValue = a.getInt(R.styleable.IntegerEdit_minValue, 0)
        initValue(a.getInt(R.styleable.IntegerEdit_initialValue, 0))
        stepSize = a.getInt(R.styleable.IntegerEdit_stepSize, 1)
        valueIsFocusable = a.getBoolean(R.styleable.IntegerEdit_valueIsFocusable, false)

        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textSize))
        ui.valueEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, a.getDimension(0, 0f))
        ui.valueEdit.paint.strokeWidth = resources.dpToPixels(1f)
        a.recycle()

        ui.decreaseButton.setOnClickListener { decrement() }
        ui.increaseButton.setOnClickListener { increment() }
        ui.valueEdit.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearFocus()
                inputMethodManager(context)?.hideSoftInputFromWindow(windowToken, 0)
                value = value // To enforce min/max value
            }
            actionId == EditorInfo.IME_ACTION_DONE
        }
        ui.valueEdit.setOnFocusChangeListener { _, focused ->
            if (!focused) { value = value } // To enforce min/max value
        }
    }

    fun initValue(newValue: Int) {
        val adjustedNewValue = newValue.coerceIn(minValue, maxValue)
        ui.valueEdit.setText(adjustedNewValue.toString())
    }

    fun increment() = modifyValue(stepSize)
    fun decrement() = modifyValue(-stepSize)
    private fun modifyValue(stepSize: Int) { value += stepSize }

    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        val result = super.drawChild(canvas, child, drawingTime)
        if (child !== ui.valueEdit || underlineAlpha == 0) return result

        val y = ui.valueEdit.baseline + resources.dpToPixels(2f)
        val paintOldAlpha = ui.valueEdit.paint.alpha
        ui.valueEdit.paint.alpha = underlineAlpha
        val startX = ui.decreaseButton.x + ui.decreaseButton.width
        val endX = ui.increaseButton.x
        canvas?.drawLine(startX, y, endX, y, ui.valueEdit.paint)
        ui.valueEdit.paint.alpha = paintOldAlpha
        return true
    }
}

/** An extension of IntegerEdit that animates changes in the focusable state.
 *
 * AnimatedIntegerEdit provides the new function setValueIsFocusable, which
 * acts similarly to the property valueIsFocusable's setter, except that it
 * also animates the change in focusable state. The animations played will use
 * the duration and interpolators defined by the value of the property
 * animatorConfig if set, or the default duration and interpolators otherwise.
 */
class AnimatedIntegerEdit(context: Context, attrs: AttributeSet) : IntegerEdit(context, attrs) {
    var animatorConfig: AnimatorConfig? = null

    // So that the property can be used in one of the AnimatorUtils valueAnimator functions.
    private fun setUnderlineAlphaPrivate(value: Int) { underlineAlpha = value; invalidate() }

    /** Information about the internal animations played and
     * any width change when setValueIsFocusable is called. */
    data class AnimInfo(val animators: List<Animator>, val widthChange: Int)

    /** Set whether the value is focusable, and return an AnimInfo object if an animation was played.
     *
     * In case an external layout needs to know information about these animations
     * to combine it with others, setValueIsFocusable will return an AnimInfo
     * object that contains the animator and the width change that was animated,
     * or null if no animation occurred. The animations will be started unless
     * the parameter startAnimationsImmediately is set to false.
     */
    fun setValueIsFocusable(
        focusable: Boolean,
        animate: Boolean = true,
        startAnimationsImmediately: Boolean = true
    ): AnimInfo? {
        if (!animate) { valueIsFocusable = focusable; return null }

        ui.valueEdit.isFocusableInTouchMode = focusable
        if (!focusable && ui.valueEdit.isFocused)
            ui.valueEdit.clearFocus()
        val underlineEndAlpha = if (focusable) 255 else 0

        val oldWidth = ui.valueEdit.width
        ui.valueEdit.minWidth = if (!focusable) 0 else
            resources.getDimensionPixelSize(R.dimen.integer_edit_editable_value_min_width)

        /* The following animations will make the IntegerEdit smoothly expand
         * to its new total width after the change in the value edit's min-
         * Width (layout transitions apparently do not handle this). */
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        ui.valueEdit.measure(wrapContentSpec, wrapContentSpec)
        val newWidth = ui.valueEdit.measuredWidth
        val widthChange = newWidth - oldWidth
        val underlineStartAlpha = if (focusable) 0 else 255

        val anims = listOf(
            floatValueAnimator(ui.valueEdit::setTranslationX, -widthChange / 2f, 0f, animatorConfig),
            floatValueAnimator(ui.increaseButton::setTranslationX, -widthChange.toFloat(), 0f, animatorConfig),
            intValueAnimator(::setUnderlineAlphaPrivate, underlineStartAlpha, underlineEndAlpha, animatorConfig))
        if (startAnimationsImmediately)
            for (anim in anims) anim.start()
        else {
            // Set the view translationX properties to their starting value to prevent flickering
            ui.valueEdit.translationX = -widthChange / 2f
            ui.increaseButton.translationX = -widthChange.toFloat()
        }
        return AnimInfo(anims, widthChange)
    }
}
