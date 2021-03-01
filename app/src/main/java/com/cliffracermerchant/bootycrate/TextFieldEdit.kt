/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
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
 * get size is adequate. It will also animate changes in its editable state
 * (unless the function setEditable is called with the parameter animate =
 * equal to false). The animations will use the property animatorConfig for
 * their durations and interpolators.
 */
open class TextFieldEdit(context: Context, attrs: AttributeSet?) :
    AppCompatEditText(context, attrs)
{
    val liveData = MutableLiveData<String>()
    var animatorConfig = AnimatorConfig.translation
    val isEditable get() = isFocusableInTouchMode
    var canBeEmpty: Boolean

    private var underlineAlpha = 0
    private var lastValue: String? = null
    private val inputMethodManager = inputMethodManager(context)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TextFieldEdit)
        setEditable(a.getBoolean(R.styleable.TextFieldEdit_isEditable, false), animate = false)
        canBeEmpty = a.getBoolean(R.styleable.TextFieldEdit_canBeBlank, true)
        a.recycle()

        maxLines = 1
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
        ellipsize = TextUtils.TruncateAt.END
        paint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f,
                                                      resources.displayMetrics)
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

    /**
     * Information about the internal animations played when setEditable is called.
     *
     * The internal translate animation will only take into account the view's change
     * in height to smoothly translate the text to its new location. If the view is
     * also moved on screen, then the animation's start and end values will need to
     * be adjusted by this amount using the function adjustTranslationStartEnd.
     */
    data class AnimInfo(
        val translateAnimator: ValueAnimator,
        val underlineAnimator: ValueAnimator,
        val heightChange: Int,
        private val startTranslationY: Float,
        private val endTranslationY: Float
    ) {
        fun adjustTranslationStartEnd(startAdjustment: Float, endAdjustment: Float) =
            translateAnimator.setFloatValues(startTranslationY + startAdjustment,
                                             endTranslationY + endAdjustment)
    }

    /** Set the editable state of the TextFieldEdit and return the AnimInfo
     * containing information about the internal animations set up during
     * the state change if @param animate == true, or null otherwise. */
    fun setEditable(editable: Boolean, animate: Boolean = true): AnimInfo? {
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
        val oldBaseline = baseline
        minHeight = if (!editable) 0 else
            resources.getDimensionPixelSize(R.dimen.text_field_edit_editable_min_height)
        val newUnderlineAlpha = if (editable) 255 else 0
        if (!animate) {
            underlineAlpha = newUnderlineAlpha
            return null
        }

        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(wrapContentSpec, wrapContentSpec)
        val baselineChange = baseline - oldBaseline
        val heightChange = measuredHeight - oldHeight
        val start = -baselineChange.toFloat()

        val translateAnimator = valueAnimatorOfFloat(
            setter = ::setTranslationY,
            fromValue = start, toValue = 0f,
            config = animatorConfig
        ).apply { start() }

        val underlineAnimator = valueAnimatorOfInt(
            setter = ::setUnderlineAlpha,
            fromValue = if (editable) 0 else 255,
            toValue = newUnderlineAlpha,
            config = if (editable) AnimatorConfig.fadeIn
                     else          AnimatorConfig.fadeOut
        ).apply { start() }

        return AnimInfo(translateAnimator, underlineAnimator, heightChange, start, 0f)
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if (underlineAlpha == 0) return
        val y = baseline + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                     2f, resources.displayMetrics)
        val paintOldAlpha = paint.alpha
        paint.alpha = underlineAlpha
        canvas?.drawLine(0f, y, width.toFloat(), y, paint)
        paint.alpha = paintOldAlpha
    }

    // So that the property can be used in a ObjectAnimator or one of the AnimatorUtils valueAnimator functions.
    private fun setUnderlineAlpha(value: Int) { underlineAlpha = value; invalidate() }
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
            valueAnimatorOfArgb(::setTextColor, currentTextColor, endColor, animatorConfig).start()
            valueAnimatorOfFloat(::setStrikeThroughLength, 0f, fullLength, animatorConfig).apply {
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
        val truncatedStrikeThroughLength = kotlin.math.min(strikeThroughLength, width.toFloat())
        val truncatedTextLength = kotlin.math.min(paint.measureText(text, 0, text?.length ?: 0),
                                                  width.toFloat())

        paint.strokeWidth = textSize / 12
        val begin = if (!strikeThroughAnimIsReversed) 0f
                    else truncatedStrikeThroughLength
        val end = if (strikeThroughAnimIsReversed) truncatedTextLength
                  else truncatedStrikeThroughLength
        val baselineOffset = paint.fontMetrics.ascent * 0.35f
        val y = baseline + baselineOffset
        canvas?.drawLine(begin, y, end, y, paint)
    }
}