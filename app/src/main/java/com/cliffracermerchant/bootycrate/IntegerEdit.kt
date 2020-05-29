package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.lifecycle.MutableLiveData
import com.cliffracermerchant.bootycrate.R
import kotlinx.android.synthetic.main.integer_edit_layout.view.*

//TODO: Don't force soft input to appear if hardware keyboard is present

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
 *      The EditText is by default not focusable in touch mode, but this can be
 *  changed by setting the member isEditable. If set to true, the user can edit
 *  the value directly (rather than through the user of the decrease / increase
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
class IntegerEdit(context: Context, attrs: AttributeSet?) :
        LinearLayout(context, attrs) {

    var minValue: Int
    var maxValue: Int
    var valueChangedNotificationTimeout: Int
    var stepSize: Int
    var textSize: Float get() = valueEdit.textSize
                        set(value) = setTextSizePrivate(value)
    var currentValue: Int get() = try { valueEdit.text.toString().toInt() }
                                  catch (e: Exception) { 0 }
                          set(value) { valueEdit.setText(value.coerceIn(
                                         minValue, maxValue).toString()) }
    var isEditable: Boolean get() = valueEdit.isFocusableInTouchMode
                            set(editable) { valueEdit.isFocusableInTouchMode = editable
                                            invalidate() }

    val liveData = MutableLiveData(currentValue)
    private val updateLiveData = Runnable { liveData.value = currentValue }
    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

    init {
        inflate(context, R.layout.integer_edit_layout, this)
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.IntegerEdit)
        currentValue = styledAttrs.getInt(R.styleable.IntegerEdit_initialValue, 0)
        minValue = styledAttrs.getInt(R.styleable.IntegerEdit_minValue, 0)
        maxValue = styledAttrs.getInt(R.styleable.IntegerEdit_maxValue, 99)
        valueChangedNotificationTimeout = styledAttrs.getInt(
            R.styleable.IntegerEdit_valueChangedNotificationTimeout, 1000)
        stepSize = styledAttrs.getInt(R.styleable.IntegerEdit_stepSize, 1)
        textSize = styledAttrs.getFloat(R.styleable.IntegerEdit_textSize, 16.0f)
        styledAttrs.recycle()

        decreaseButton.setOnClickListener { modifyValue(-stepSize) }
        increaseButton.setOnClickListener { modifyValue(stepSize) }
        valueEdit.setOnEditorActionListener{ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearFocus()
                imm?.hideSoftInputFromWindow(windowToken, 0)
                updateLiveDataFromEditor()
            }
            actionId == EditorInfo.IME_ACTION_DONE
        }
        setWillNotDraw(false)
        clipChildren = false
    }

    private fun updateLiveDataFromEditor() {
        currentValue = currentValue // not pointless due to custom setter and getter
        handler.removeCallbacks(updateLiveData)
        handler.postDelayed(updateLiveData, valueChangedNotificationTimeout.toLong())
    }

    private fun modifyValue(stepSize: Int) {
        val oldValue = currentValue
        currentValue += stepSize
        if (currentValue != oldValue) {
            handler.removeCallbacks(updateLiveData)
            handler.postDelayed(updateLiveData, valueChangedNotificationTimeout.toLong())
        }
    }

    private fun setTextSizePrivate(size: Float) {
        valueEdit.textSize = size
        valueEdit.setPadding((size / 3).toInt(), 0, (size / 3).toInt(), 0)
        decreaseButton.scaleX = size / 32
        decreaseButton.scaleY = size / 32
        increaseButton.scaleX = size / 32
        increaseButton.scaleY = size / 32
    }

    override fun onDraw(canvas: Canvas) {
        if (isEditable) valueEdit.paintFlags = valueEdit.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        else valueEdit.paintFlags = valueEdit.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        super.onDraw(canvas)
    }
}