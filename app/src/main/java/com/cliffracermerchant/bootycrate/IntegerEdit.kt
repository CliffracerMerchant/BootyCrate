/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.android.synthetic.main.integer_edit_layout.view.*

/** A compound view to edit an integer quantity.
 *
 *  IntegerEdit is a compound view that combines an EditText displaying an
 *  integer with two image buttons on its left and right to act as decrease and
 *  increase buttons. IntegerEdit provides a publicly accessible LiveData mem-
 *  ber to allow external entities to react to a change in its data.
 *
 *  The EditText is by default not focusable in touch mode, but this can be
 *  changed by setting the member isEditable. If set to true, the user can edit
 *  the value directly (rather than through the use of the decrease / increase
 *  buttons). When in the editable state, the EditText will underline the cur-
 *  rent value to indicate this to the user.
 *
 *  XML Attributes:
 *  - Int initialValue = 0: The starting value
 *  - Int minValue = 0: The minimum value that the IntegerEdit can hold.
 *  - Int maxValue = 99: The maximum value that the IntegerEdit can hold.
 *  - Int stepSize = 1: The amount to decrease or increase the value by when
 *    the corresponding button is pressed.
 *  - valueIsDirectlyEditable: Boolean = false: Whether the user can input the
 *    value directly instead of through the decrease / increase buttons. */
class IntegerEdit(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    // this.value = this.value is not pointless due
    // to value's custom getter and setter
    private var _minValue = 0
    var minValue get() = _minValue
                 set(value) { _minValue = value; this.value = this.value }
    private var _maxValue = 0
    var maxValue get() = _maxValue
                 set(value) { _maxValue = value; this.value = this.value }

    var stepSize: Int

    private var _value get() = try { valueEdit.text.toString().toInt() }
                               catch (e: Exception) { 0 }
                       set(value) { val newValue = value.coerceIn(minValue, maxValue)
                                    valueEdit.setText(newValue.toString()) }
    var value get() = _value
              set(newValue) { _value = newValue
                              _liveData.value = value }

    var valueIsDirectlyEditable get() = valueEdit.isFocusableInTouchMode
                                set(editable) { setEditable(editable) }

    private val _liveData = MutableLiveData(value)
    val liveData: LiveData<Int> get() = _liveData

    private val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

    init {
        LayoutInflater.from(context).inflate(R.layout.integer_edit_layout, this, true)
        var a = context.obtainStyledAttributes(attrs, R.styleable.IntegerEdit)
        _value = a.getInt(R.styleable.IntegerEdit_initialValue, 0)
        _minValue = a.getInt(R.styleable.IntegerEdit_minValue, 0)
        _maxValue = a.getInt(R.styleable.IntegerEdit_maxValue, 99)
        stepSize = a.getInt(R.styleable.IntegerEdit_stepSize, 1)
        valueIsDirectlyEditable = a.getBoolean(R.styleable.IntegerEdit_valueIsDirectlyEditable, false)

        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textSize))
        // For some reason the text size appears larger than it should be;
        // the division by 2 is an approximation to correct this.
        valueEdit.textSize = a.getDimensionPixelSize(0, 0) / 2f
        a.recycle()

        decreaseButton.setOnClickListener { decrement() }
        increaseButton.setOnClickListener { increment() }
        valueEdit.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearFocus()
                imm?.hideSoftInputFromWindow(windowToken, 0)
                value = value // To enforce min/max value
            }
            actionId == EditorInfo.IME_ACTION_DONE
        }
        valueEdit.setOnFocusChangeListener { _, focused ->
            if (!focused) { value = value } // To enforce min/max value
        }
    }

    fun initValue(newValue: Int) { _value = newValue }
    fun increment() = modifyValue(stepSize)
    fun decrement() = modifyValue(-stepSize)
    private fun modifyValue(stepSize: Int) { value += stepSize }

    private fun setEditable(editable: Boolean) {
        valueEdit.isFocusableInTouchMode = editable
        if (!editable && valueEdit.isFocused)
            valueEdit.clearFocus()
        if (editable) valueEdit.paintFlags = valueEdit.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        else          valueEdit.paintFlags = valueEdit.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        invalidate()
    }
}
