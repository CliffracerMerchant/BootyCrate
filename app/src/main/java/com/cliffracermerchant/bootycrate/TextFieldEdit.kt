/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.animation.doOnEnd
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData

/**
 * A view to edit a single line text field.
 *
 * TextFieldEdit is an AppCompatEditText optimized for toggleable editing
 * of a single line. When isEditable is false, the TextFieldEdit will present
 * itself as a normal single line TextView. When isEditable is true, TextField-
 * Edit will request focus when it is tapped and display a soft input. If the
 * user presses the done action on the soft input or if the focus is changed,
 * the changes can be listened to through the member liveData. If the proposed
 * change would leave the field empty and the property canBeEmpty is false,
 * the value will instead be reverted to its previous value. Note that if the
 * canBeEmpty property is changed to false when the field is already empty,
 * it will not be enforced until the value is changed to a non-blank value.
 *
 * When in editable mode, TextFieldEdit will underline itself to indicate this
 * to the user, and will set its minHeight to the value of the dimension
 * R.dimen.text_field_edit_editable_min_height to ensure that its touch tar-
 * get size is adequate.
 */
open class TextFieldEdit(context: Context, attrs: AttributeSet?) :
    AppCompatEditText(context, attrs)
{
    val liveData = MutableLiveData<String>()

    var isEditable: Boolean get() = isFocusableInTouchMode
                            set(editable) = setEditable(editable, animate = true)

    var canBeEmpty: Boolean
    private var lastValue: String? = null
    private val interp = AccelerateDecelerateInterpolator()
    private val inputMethodManager = inputMethodManager(context)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TextFieldEdit)
        isEditable = a.getBoolean(R.styleable.TextFieldEdit_isEditable, false)
        canBeEmpty = a.getBoolean(R.styleable.TextFieldEdit_canBeBlank, true)
        a.recycle()

        maxLines = 1
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
        ellipsize = TextUtils.TruncateAt.END
    }

    override fun onEditorAction(actionCode: Int) {
        if (actionCode == EditorInfo.IME_ACTION_DONE) {
            clearFocus()
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
            liveData.value = text.toString()
        }
        super.onEditorAction(actionCode)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) liveData.value = text.toString()
    }

    fun setEditable(editable: Boolean, animate: Boolean = true) {
        if (!canBeEmpty)
            if (editable) lastValue = text.toString()
            else if (text.isNullOrBlank()) setText(lastValue)

        isFocusableInTouchMode = editable
        /* Setting the input type here will prevent misspelling underlines from
         * being displayed when the TextFieldEdit is not in an editable state. */
        inputType = if (editable) InputType.TYPE_CLASS_TEXT
                    else          InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        if (!editable && isFocused) clearFocus()

        val oldHeight = height
        minHeight = if (!editable) 0 else
            resources.getDimensionPixelSize(R.dimen.text_field_edit_editable_min_height)
        val endGravity = if (editable) Gravity.CENTER_VERTICAL else Gravity.START
        if (!animate || oldHeight == 0) {
            gravity = endGravity
            return
        }
        /* A text gravity of center_vertical is intended when the TextFieldEdit is expanded
         * for editing (its vertical gravity does not matter when it is not editable due to
         * its layout height being wrap_content and its minHeight being 0). But if the gra-
         * vity is center_vertical when the expansion animation is played, a parent layout
         * transition will not animate this, and the text will jump to its new position in
         * the resized view. While this can be counteracted with a translationY animation,
         * if the gravity is already center_vertical when the translationY animation is
         * started it can cause the text to be partially clipped. To work around this the
         * gravity is set to viewStart when the view is collapsed, and is only set to
         * center_vertical after the expansion animation is finished. */
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(wrapContentSpec, wrapContentSpec)
        val heightChange = measuredHeight - oldHeight
        translationY = if (editable) 0f else -heightChange / 2f
        animate().withLayer().setInterpolator(interp)
            .translationY(if (editable) heightChange / 2f else 0f)
            .withEndAction {
                if (editable) translationY = 0f
                gravity = endGravity
            }.start()
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if (!isEditable) return
        val y = baseline + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                     2f, resources.displayMetrics)
        canvas?.drawLine(0f, y, width.toFloat(), y, paint)
    }
}

/**
 * A TextFieldEdit subclass that allows the toggling of a strike-through
 * effect, optionally with an animation, using the public function setStrike-
 * throughEnabled.
 */
class AnimatedStrikeThroughTextFieldEdit(context: Context, attrs: AttributeSet) :
    TextFieldEdit(context, attrs)
{
    private val normalTextColor = currentTextColor
    private var strikeThroughAnimIsReversed = false
    private var strikeThroughLength: Float? = null
    // So that the setter can be passed to valueAnimatorOfFloat
    private fun setStrikeThroughLength(value: Float) {
        strikeThroughLength = value
        invalidate()
    }

    init {
        doAfterTextChanged { text ->
            if (strikeThroughLength != null)
                strikeThroughLength = paint.measureText(text, 0, text?.length ?: 0)
        }
    }

    fun setStrikeThroughEnabled(strikeThroughEnabled: Boolean, animate: Boolean = true) {
        strikeThroughAnimIsReversed = !strikeThroughEnabled
        val fullLength = paint.measureText(text, 0, text?.length ?: 0)
        if (animate) {
            val endColor = if (strikeThroughEnabled) currentHintTextColor
                           else                      normalTextColor
            valueAnimatorOfArgb(::setTextColor, currentTextColor, endColor).start()
            valueAnimatorOfFloat(::setStrikeThroughLength, 0f, fullLength).apply {
                if (!strikeThroughEnabled)
                    doOnEnd { strikeThroughLength = null }
            }.start()
        } else {
            strikeThroughLength = if (strikeThroughEnabled) fullLength else null
            setTextColor(if (strikeThroughEnabled) currentHintTextColor
                         else                      normalTextColor)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val strikeThroughLength = this.strikeThroughLength ?: return

        val fullLength = paint.measureText(text, 0, text?.length ?: 0)
        paint.strokeWidth = textSize / 12
        val begin = if (!strikeThroughAnimIsReversed) 0f
                    else strikeThroughLength
        val end = if (strikeThroughAnimIsReversed) fullLength
                  else strikeThroughLength
        val baselineOffset = paint.fontMetrics.ascent * 0.35f
        val y = baseline + baselineOffset
        canvas?.drawLine(begin, y, end, y, paint)
    }
}