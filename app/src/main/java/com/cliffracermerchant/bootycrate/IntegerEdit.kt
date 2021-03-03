/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cliffracermerchant.bootycrate.databinding.IntegerEditBinding

/**
 * A compound view to edit an integer quantity.
 *
 * IntegerEdit is a compound view that combines an EditText displaying an
 * integer with two image buttons on its left and right to act as decrease and
 * increase buttons. IntegerEdit provides a publicly accessible LiveData mem-
 * ber to allow external entities to react to a change in its data.
 *
 * The EditText is by default not focusable in touch mode, but this can be
 * changed by calling the function setValueIsDirectlyEditable. If set to true,
 * the user can edit the value directly (rather than through the use of the
 * decrease / increase buttons). When in the editable state, the EditText will
 * underline the current value to indicate this to the user, and will, if neces-
 * sary, expand the width of the value to R.dimen.integer_edit_editable_value_-
 * min_width to make it a larger touch target.
 *
 * Unless the function setValueIsDirectlyEditable is called with a value of
 * false for the parameter animate, the IntegerEdit will start an animation
 * that uses the property animatorConfig for its duration and interpolator. In
 * case an external layout needs to know information about this animation to
 * combine it with others, setValueIsDirectlyEditable will return an AnimInfo
 * object that contains the animator and the width change that was animated, or
 * null if no animation occurred.
 */
class IntegerEdit(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    // this.value = this.value is not pointless due
    // to value's custom getter and setter
    private var _minValue = 0
    private var _maxValue = 0
    var minValue get() = _minValue
                 set(value) { _minValue = value; this.value = this.value }
    var maxValue get() = _maxValue
                 set(value) { _maxValue = value; this.value = this.value }
    var stepSize: Int

    private var _value get() = try { ui.valueEdit.text.toString().toInt() }
                               catch (e: Exception) { 0 }
                       set(value) { val newValue = value.coerceIn(minValue, maxValue)
                                    ui.valueEdit.setText(newValue.toString()) }
    var value get() = _value
              set(newValue) { _value = newValue
                              _liveData.value = value }

    val valueIsFocusable get() = ui.valueEdit.isFocusableInTouchMode

    private val _liveData = MutableLiveData(value)
    val liveData: LiveData<Int> get() = _liveData

    val ui = IntegerEditBinding.inflate(LayoutInflater.from(context), this)
    var animatorConfig = AnimatorConfig.translation

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.IntegerEdit)
        _value = a.getInt(R.styleable.IntegerEdit_initialValue, 0)
        _minValue = a.getInt(R.styleable.IntegerEdit_minValue, 0)
        _maxValue = a.getInt(R.styleable.IntegerEdit_maxValue, 99)
        stepSize = a.getInt(R.styleable.IntegerEdit_stepSize, 1)
        setValueIsFocusable(
            a.getBoolean(R.styleable.IntegerEdit_valueIsFocusable, false),
            animate = false)

        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textSize))
        ui.valueEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, a.getDimension(0, 0f))
        ui.valueEdit.paint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)//ui.valueEdit.textSize / 20
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

    fun initValue(newValue: Int) { _value = newValue }
    fun increment() = modifyValue(stepSize)
    fun decrement() = modifyValue(-stepSize)
    private fun modifyValue(stepSize: Int) { value += stepSize }

    /** Information about the internal animations played and
     * any width change when setValueIsFocusable is called. */
    data class AnimInfo(val animators: List<Animator>, val widthChange: Int)

    /** Set whether the value is focusable, and return an AnimInfo object if an animation was played. */
    fun setValueIsFocusable(focusable: Boolean, animate: Boolean = true): AnimInfo? {
        ui.valueEdit.isFocusableInTouchMode = focusable
        if (!focusable && ui.valueEdit.isFocused)
            ui.valueEdit.clearFocus()
        val underlineEndAlpha = if (focusable) 255 else 0

        val oldWidth = ui.valueEdit.width
        ui.valueEdit.minWidth = if (!focusable) 0 else
            resources.getDimensionPixelSize(R.dimen.integer_edit_editable_value_min_width)
        if (!animate || oldWidth == 0) {
            underlineAlpha = underlineEndAlpha
            return null
        }

        /* The following animations will make the IntegerEdit smoothly expand
         * to its new total width after the change in the value edit's min-
         * Width (layout transitions apparently do not handle this). */
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        ui.valueEdit.measure(wrapContentSpec, wrapContentSpec)
        val newWidth = ui.valueEdit.measuredWidth
        val widthChange = newWidth - oldWidth
        val underlineStartAlpha = if (focusable) 0 else 255

        val anims = listOf(
            valueAnimatorOfFloat(ui.valueEdit::setTranslationX, -widthChange / 2f, 0f, animatorConfig),
            valueAnimatorOfFloat(ui.increaseButton::setTranslationX, -widthChange.toFloat(), 0f, animatorConfig),
            valueAnimatorOfInt(::setUnderlineAlpha, underlineStartAlpha, underlineEndAlpha, animatorConfig))
        for (anim in anims) anim.start()
        return AnimInfo(anims, widthChange)
    }

    private var underlineAlpha = 0
    // So that the property can be used in a ObjectAnimator or one of the AnimatorUtils valueAnimator functions.
    private fun setUnderlineAlpha(value: Int) { underlineAlpha = value; invalidate() }

    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        val result = super.drawChild(canvas, child, drawingTime)
        if (child !== ui.valueEdit || underlineAlpha == 0) return result

        val y = ui.valueEdit.baseline + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                                  2f, resources.displayMetrics)
        val paintOldAlpha = ui.valueEdit.paint.alpha
        ui.valueEdit.paint.alpha = underlineAlpha
        val startX = ui.decreaseButton.x + ui.decreaseButton.width
        val endX = ui.increaseButton.x
        canvas?.drawLine(startX, y, endX, y, ui.valueEdit.paint)
        ui.valueEdit.paint.alpha = paintOldAlpha
        return true
    }
}
