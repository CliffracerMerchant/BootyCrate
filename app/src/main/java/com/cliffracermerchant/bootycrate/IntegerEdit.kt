/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
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
 *  Because a user might click on the provided decrease or increase buttons
 *  rapidly when intending to change the value by a large amount, and because
 *  external entities most likely don't want to deal with a rapid succession of
 *  intermediate states, IntegerEdit will only change its LiveData member after
 *  a change is made and no further change to the value is made within the
 *  valueChangedNotificationTimeout interval.
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
 *  - Int valueChangedNotificationTimeout = 1000: The delay in milliseconds
 *    after the last change to the value before the LiveData member will be
 *    updated.
 *  - Int stepSize = 1: The amount to decrease or increase the value by when
 *    the corresponding button is pressed.
 *  - Float textSize = 16.0f: The text display size for the central EditText
 *    member. When this is set, the scale of the decrease and increase buttons
 *    will be adjusted to match the new text size. */
class IntegerEdit(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    // currentValue = currentValue is not pointless due
    // to currentValue's custom getter and setter
    private var _minValue = 0
    var minValue get() = _minValue
                 set(value) { _minValue = value; currentValue = currentValue }
    private var _maxValue = 0
    var maxValue get() = _maxValue
                 set(value) { _maxValue = value; currentValue = currentValue }

    var valueChangedNotificationTimeout: Int
    var stepSize: Int

    private var _currentValue get() = try { valueEdit.text.toString().toInt() }
                                      catch (e: Exception) { 0 }
                              set(value) { valueEdit.setText(value.coerceIn(
                                            minValue, maxValue).toString()) }
    var currentValue get() = _currentValue
                     set(value) { if (value != _currentValue) updateLiveDataWithDelay()
                                  _currentValue = value }

    var isEditable = false
        set(editable) { field = editable
                        valueEdit.isFocusableInTouchMode = editable
                        if (!editable && valueEdit.isFocused) valueEdit.clearFocus()
                        invalidate() }

    private val _liveData = MutableLiveData(currentValue)
    val liveData: LiveData<Int> get() = _liveData

    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    private val _handler = Handler()

    init {
        LayoutInflater.from(context).inflate(R.layout.integer_edit_layout, this, true)
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.IntegerEdit)
        _currentValue = styledAttrs.getInt(R.styleable.IntegerEdit_initialValue, 0)
        _minValue = styledAttrs.getInt(R.styleable.IntegerEdit_minValue, 0)
        _maxValue = styledAttrs.getInt(R.styleable.IntegerEdit_maxValue, 99)
        valueChangedNotificationTimeout = styledAttrs.getInt(
            R.styleable.IntegerEdit_valueChangedNotificationTimeout, 1000)
        stepSize = styledAttrs.getInt(R.styleable.IntegerEdit_stepSize, 1)
        val textSize = styledAttrs.getDimension(R.styleable.IntegerEdit_textSize, 1f)
        valueEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        styledAttrs.recycle()

        decreaseButton.setOnClickListener { decrement() }
        increaseButton.setOnClickListener { increment() }
        valueEdit.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearFocus()
                imm?.hideSoftInputFromWindow(windowToken, 0)
                currentValue = currentValue // To enforce min/max value
                updateLiveDataWithDelay()
            }
            actionId == EditorInfo.IME_ACTION_DONE
        }
        valueEdit.setOnFocusChangeListener { _, focused ->
            if (!focused) {
                currentValue = currentValue // To enforce min/max value
                updateLiveDataWithDelay()
            }
        }
        setWillNotDraw(false)
        clipChildren = false
    }

    fun increment() = modifyValue(stepSize)
    fun decrement() = modifyValue(-stepSize)

    private fun updateLiveData() { _liveData.value = currentValue }

    private fun updateLiveDataWithDelay() {
        _handler.removeCallbacks(::updateLiveData)
        _handler.postDelayed(::updateLiveData, valueChangedNotificationTimeout.toLong())
    }

    private fun modifyValue(stepSize: Int) {
        val oldValue = currentValue
        currentValue += stepSize
        if (currentValue != oldValue) updateLiveDataWithDelay()
    }

    fun initCurrentValue(newValue: Int) { _currentValue = newValue }

    override fun onDraw(canvas: Canvas) {
        if (isEditable) valueEdit.paintFlags = valueEdit.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        else            valueEdit.paintFlags = valueEdit.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        super.onDraw(canvas)
    }
}
