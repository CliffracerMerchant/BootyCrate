package com.cliffracermerchant.bootycrate

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.lifecycle.MutableLiveData
import com.cliffracermerchant.bootycrate.R.*
import kotlinx.android.synthetic.main.integer_edit_layout.view.*

/**     IntegerEdit is a compound view that combines an EditText displaying an
 *  integer with two image buttons on its left and right to act as decrease and
 *  increase buttons. IntegerEdit provides a publicly accessible LiveData mem-
 *  ber to allow external entities to react to a change in its data.
 *      Because a user might click on the provided decrease or increase buttons
 *  rapidly when intending to change the value by a large amount, and because
 *  external entities most likely don't want to deal with a rapid succession of
 *  intermediate states, IntegerEdit will only change its publicly accessible
 *  LiveData member after a change is made AND no further change to the value
 *  is made within the valueChangedNotificationTimeout interval.
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
class IntegerEdit(context: Context?, attrs: AttributeSet?) :
        LinearLayout(context, attrs) {

    var minValue: Int
    var maxValue: Int
    var valueChangedNotificationTimeout: Int
    var stepSize: Int
    var textSize: Float get() = valueEdit.textSize
                        set(value) { valueEdit.textSize = value
                                     decreaseButton.scaleX = value / 32.0f
                                     decreaseButton.scaleY = value / 32.0f
                                     increaseButton.scaleX = value / 32.0f
                                     increaseButton.scaleY = value / 32.0f }
    var currentValue: Int get() = try { valueEdit.text.toString().toInt() }
                                  catch (e: Exception) { 0 }
                          set(value) { valueEdit.setText(value.coerceIn(
                                           minValue, maxValue).toString()) }
    var editable: Boolean get() = valueEdit.isClickable
                          set(editable) { valueEdit.isClickable = editable }

    val liveData = MutableLiveData(currentValue)
    private val updateLiveData = Runnable { liveData.value = currentValue }

    fun updateLiveDataFromEditor() {
        currentValue = currentValue // not pointless due to custom setter and getter
        handler.removeCallbacks(updateLiveData)
        handler.postDelayed(updateLiveData, valueChangedNotificationTimeout.toLong())
    }

    init {
        inflate(context, layout.integer_edit_layout, this)
        val styledAttrs = context?.obtainStyledAttributes(attrs, styleable.IntegerEdit)
        currentValue = styledAttrs?.getInt(styleable.IntegerEdit_initialValue, 0) ?: 0
        minValue = styledAttrs?.getInt(styleable.IntegerEdit_minValue, 0) ?: 0
        maxValue = styledAttrs?.getInt(styleable.IntegerEdit_maxValue, 99) ?: 99
        valueChangedNotificationTimeout = styledAttrs?.getInt(
            styleable.IntegerEdit_valueChangedNotificationTimeout, 1000) ?: 1000
        stepSize = styledAttrs?.getInt(styleable.IntegerEdit_stepSize, 1) ?: 1
        textSize = styledAttrs?.getFloat(styleable.IntegerEdit_textSize, 16.0f) ?: 16.0f
        styledAttrs?.recycle()

        decreaseButton.setOnClickListener { modifyValue(-stepSize) }
        increaseButton.setOnClickListener { modifyValue(stepSize) }
        valueEdit.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE)
                updateLiveDataFromEditor()
            actionId == EditorInfo.IME_ACTION_DONE
        }
    }

    private fun modifyValue(stepSize: Int) {
        val oldValue = currentValue
        currentValue += stepSize
        if (currentValue != oldValue) {
            handler.removeCallbacks(updateLiveData)
            handler.postDelayed(updateLiveData, valueChangedNotificationTimeout.toLong())
        }
    }
}