package com.cliffracermerchant.stuffcrate

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.lifecycle.MutableLiveData
import com.cliffracermerchant.stuffcrate.R.*
import kotlinx.android.synthetic.main.integer_edit_layout.view.*

/**     IntegerEdit is a compound view that combines an EditText displaying an
 *  integer with two image buttons on its left and right to act as decrement
 *  and increment buttons. IntegerEdit provides a publicly accessible LiveData
 *  member to allow external entities to react to a change in its data.
 *      Because a user might click on the provided decrement or increment but-
 *  tons rapidly when intending to change the value by a large amount, and
 *  because external entities most likely don't want to deal with a rapid suc-
 *  cession of intermediate states, IntegerEdit will only change its publicly
 *  accessible LiveData member after a change is made AND no further change to
 *  the value is made within the valueChangedNotificationTimeout interval.
 *
 *  XML Attributes:
 *  - Int initialValue = 0: The starting value
 *  - Int minValue = 0: The minimum value that the IntegerEdit can hold.
 *  - Int maxValue = 99: The maximum value that the IntegerEdit can hold.
 *  - Int valueChangedNotificationTimeout = 1000: The delay in milliseconds
 *    after the last change to the value before the LiveData member will be
 *    updated.
 *  - Int stepSize = 1: The amount to decrement or increment the value by when
 *    the corresponding button is pressed.
 *  - Float textSize = 16.0f: The text display size for the central EditText
 *    member. When this is set, the scale of the decrement and increment but-
 *    tons will be adjusted to match the new text size. */
class IntegerEdit(context: Context?, attrs: AttributeSet?) :
        LinearLayout(context, attrs) {
    private val view = inflate(context, layout.integer_edit_layout, this)
    private val updateLiveData = Runnable { liveData.value = currentValue }

    var minValue: Int
    var maxValue: Int
    var valueChangedNotificationTimeout: Int
    var stepSize: Int
    var textSize: Float get() = valueEdit.textSize
                        set(value) { valueEdit.textSize = value
                                     decrementButton.scaleX = value / 32.0f
                                     decrementButton.scaleY = value / 32.0f
                                     incrementButton.scaleX = value / 32.0f
                                     incrementButton.scaleY = value / 32.0f }
    var currentValue: Int get() = try { valueEdit.text.toString().toInt() }
                                  catch (e: NumberFormatException) { 0 }
                          set(value) { view.valueEdit.setText(value.coerceIn(
                                                minValue, maxValue).toString()) }
    var editable: Boolean get() = valueEdit.isEnabled
                          set(editable) { valueEdit.isEnabled = editable }

    val liveData = MutableLiveData(currentValue)

    init {
        val styledAttrs = context?.obtainStyledAttributes(attrs, R.styleable.IntegerEdit)
        currentValue = styledAttrs?.getInt(R.styleable.IntegerEdit_initialValue, 0) ?: 0
        minValue = styledAttrs?.getInt(R.styleable.IntegerEdit_minValue, 0) ?: 0
        maxValue = styledAttrs?.getInt(R.styleable.IntegerEdit_maxValue, 99) ?: 99
        valueChangedNotificationTimeout = styledAttrs?.getInt(
            R.styleable.IntegerEdit_valueChangedNotificationTimeout, 1000) ?: 1000
        stepSize = styledAttrs?.getInt(R.styleable.IntegerEdit_stepSize, 1) ?: 1
        textSize = styledAttrs?.getFloat(R.styleable.IntegerEdit_textSize, 16.0f) ?: 16.0f
        styledAttrs?.recycle()

        decrementButton.setOnClickListener { modifyValue(-stepSize) }
        incrementButton.setOnClickListener { modifyValue(stepSize) }
        valueEdit.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                currentValue = currentValue // not pointless due to custom setter and getter
                handler.removeCallbacks(updateLiveData)
                handler.postDelayed(updateLiveData, valueChangedNotificationTimeout.toLong())
            }
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