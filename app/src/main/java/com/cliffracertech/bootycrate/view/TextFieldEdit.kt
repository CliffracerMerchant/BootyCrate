/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.*

/**
 * A view to edit a single line text field.
 *
 * TextFieldEdit is an AppCompatEditText optimized for toggleable editing
 * of a single line. When isEditable is false, the TextFieldEdit will pre-
 * sent itself as a single line TextView. When isEditable is true,
 * TextFieldEdit will request focus when it is tapped and display a soft
 * input. If the user presses the done action on the soft input or if the
 * focus is changed, the changes to the text can be reacted to through the
 * property onTextChangedListener. If the proposed change would leave the
 * field empty, and the property canBeBlank is false, the value will
 * instead be reverted to its previous value. Note that if the canBeBlank
 * property is changed to false when the field is already empty, it will
 * not be enforced until the value is changed to a non-blank value.

 * When in editable mode, TextFieldEdit will underline itself to indicate
 * this to the user, and will set its minHeight to the value of the dimen-
 * sion R.dimen.text_field_edit_editable_min_height to ensure that its
 * touch target size is adequate.
 */
open class TextFieldEdit(context: Context, attrs: AttributeSet) :
    AppCompatEditText(context, attrs)
{
    var onTextChangedListener: ((String) -> Unit)? = null
    val isEditable get() = isFocusableInTouchMode
    var canBeBlank: Boolean

    protected var underlineAlpha = 0
    protected var lastValue: String? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TextFieldEdit)
        setEditable(a.getBoolean(R.styleable.TextFieldEdit_isEditable, false))
        canBeBlank = a.getBoolean(R.styleable.TextFieldEdit_canBeBlank, true)
        a.recycle()

        maxLines = 1
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
        ellipsize = TextUtils.TruncateAt.END
        paint.strokeWidth = resources.dpToPixels(1f)
    }

    private fun setTextPrivate(newText: String) {
        if (canBeBlank || newText.isNotBlank()) {
            setText(newText)
            onTextChangedListener?.invoke(text.toString())
        } else setText(lastValue ?: "")
    }

    override fun onEditorAction(actionCode: Int) {
        if (actionCode == EditorInfo.IME_ACTION_DONE) {
            clearFocus()
            SoftKeyboard.hide(this)
            setTextPrivate(text.toString())
        }
        super.onEditorAction(actionCode)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) setTextPrivate(text.toString())
    }

    fun setEditable(editable: Boolean) {
        if (editable) lastValue = text.toString()
        isFocusableInTouchMode = editable
        isFocusable = editable
        isClickable = editable
        /* Setting the input type here will prevent misspelling underlines from
         * being displayed when the TextFieldEdit is not in an editable state. */
        inputType = if (editable) InputType.TYPE_CLASS_TEXT
                    else          InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        minHeight = if (!editable) 0 else
            resources.getDimensionPixelSize(R.dimen.editable_text_field_min_height)
        underlineAlpha = if (editable) 255 else 0
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if (underlineAlpha == 0) return
        val y = baseline + resources.dpToPixels(2f)
        val paintOldAlpha = paint.alpha
        paint.alpha = underlineAlpha
        canvas?.drawLine(0f, y, width.toFloat(), y, paint)
        paint.alpha = paintOldAlpha
    }
}

/** A TextFieldEdit subclass that allows the toggling of a strike-through
 * effect, using the public function setStrikeThroughEnabled. */
open class StrikeThroughTextFieldEdit(context: Context, attrs: AttributeSet) :
    TextFieldEdit(context, attrs)
{
    protected val normalTextColor = currentTextColor
    protected var strikeThroughIsRtl = false
    protected var strikeThroughLength: Float? = null

    init {
        doAfterTextChanged { text ->
            if (strikeThroughLength != null)
                strikeThroughLength = paint.measureText(text, 0, text?.length ?: 0)
        }
    }

    fun setStrikeThroughEnabled(strikeThroughEnabled: Boolean) {
        strikeThroughIsRtl = !strikeThroughEnabled
        strikeThroughLength = if (!strikeThroughEnabled) null
                              else paint.measureText(text, 0, text?.length ?: 0)
        setTextColor(if (strikeThroughEnabled) currentHintTextColor
                     else                      normalTextColor)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val strikeThroughLength = this.strikeThroughLength ?: return
        val truncatedStrikeThroughLength = kotlin.math.min(strikeThroughLength, width.toFloat())
        val truncatedTextLength = kotlin.math.min(paint.measureText(text, 0, text?.length ?: 0),
                                                  width.toFloat())

        paint.strokeWidth = textSize / 12
        val begin = if (!strikeThroughIsRtl) 0f
                    else truncatedStrikeThroughLength
        val end = if (strikeThroughIsRtl) truncatedTextLength
                  else truncatedStrikeThroughLength
        val baselineOffset = paint.fontMetrics.ascent * 0.35f
        val y = baseline + baselineOffset
        canvas?.drawLine(begin, y, end, y, paint)
    }
}

/**
 * An extension of StrikeThroughTextFieldEdit that animates changes in editable state or the length of the strike through.
 *
 * AnimatedStrikeThroughTextFieldEdit has overloads of the functions setEditable
 * and setStrikeThroughEnabled that also take a boolean animate parameter,
 * which when true will animate changes in the editable state or the strike
 * through length. The animations will use the value of the property animatorConfig
 * for their durations and interpolators.
 */
class AnimatedStrikeThroughTextFieldEdit(context: Context, attrs: AttributeSet) :
    StrikeThroughTextFieldEdit(context, attrs)
{
    var animatorConfig: AnimatorConfig? = null
    val hasStrikeThrough get() = strikeThroughLength ?: 0f > 0f

    /**
     * Information about the internal animations played when setEditable is called.
     *
     * The internal translate animation will only take into account the view's change
     * in baseline to smoothly translate the text to its new location. If the view's
     * top is also moved, then the animation's start and end values will need to be
     * adjusted by this amount using the function adjustTranslationStartEnd.
     */
    data class AnimInfo(
        val translateAnimator: ValueAnimator,
        val underlineAnimator: ValueAnimator?,
        private var startTranslationY: Float,
        private var endTranslationY: Float
    ) {
        fun adjustTranslationStartEnd(startAdjustment: Float, endAdjustment: Float) {
            startTranslationY += startAdjustment
            endTranslationY += endAdjustment
            translateAnimator.setFloatValues(startTranslationY, endTranslationY)
        }
    }

    /** A set of options describing how the underline alpha will be updated during a call to setExpanded. */
    enum class UnderlineAlphaUpdateMode {
        /** The underline alpha will be set immediately before the setExpanded
         *  animation occurs, useful if the view is currently not visible but
         *  will appear during the setExpanded animation. */
        BeforeAnimation,
        /** The underline alpha will be set immediately after the setExpanded
         *  animation finishes, useful it the view is currently visible but will
         *  not be by the end of the setExpanded animation. */
        AfterAnimation,
        /** The underline alpha will be animated from its current value to
         *  its final value alongside the setExpandedAnimation. */
        Animate
    }

    /** Set the editable state of the TextFieldEdit, and return the AnimInfo
     *  containing information about the internal animations set up during
     *  the state change if @param animate == true, or null otherwise. The
     *  value of the param underlineAlphaUpdateMode will determine how the
     *  underline alpha will be updated. */
    fun setEditable(
        editable: Boolean,
        animate: Boolean = true,
        underlineAlphaUpdateMode: UnderlineAlphaUpdateMode = UnderlineAlphaUpdateMode.Animate,
        startAnimationsImmediately: Boolean = true
    ): AnimInfo? {
        if (!animate) {
            super.setEditable(editable)
            return null
        }
        if (editable) lastValue = text.toString()
        isFocusableInTouchMode = editable
        inputType = if (editable) InputType.TYPE_CLASS_TEXT
                    else          InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        val oldBaseline = baseline
        minHeight = if (!editable) 0 else
            resources.getDimensionPixelSize(R.dimen.editable_text_field_min_height)
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(wrapContentSpec, wrapContentSpec)
        val baselineChange = baseline - oldBaseline
        val newUnderlineAlpha = if (editable) 255 else 0

        translationY -= baselineChange
        val translateAnimator = floatValueAnimator(::setTranslationY, translationY, 0f, animatorConfig)
        val underlineAnimator = when (underlineAlphaUpdateMode) {
            UnderlineAlphaUpdateMode.Animate ->
                intValueAnimator(::setUnderlineAlphaPrivate, underlineAlpha,
                                 newUnderlineAlpha, animatorConfig)
            UnderlineAlphaUpdateMode.BeforeAnimation -> {
                underlineAlpha = newUnderlineAlpha
                setLayerType(LAYER_TYPE_HARDWARE, null)
                translateAnimator.doOnEnd { setLayerType(LAYER_TYPE_NONE, null) }
                null
            } UnderlineAlphaUpdateMode.AfterAnimation -> {
                setLayerType(LAYER_TYPE_HARDWARE, null)
                translateAnimator.doOnEnd { setLayerType(LAYER_TYPE_NONE, null)
                                            underlineAlpha = newUnderlineAlpha }
                null
            }
        }
        if (startAnimationsImmediately) { translateAnimator.start()
                                          underlineAnimator?.start() }
        return AnimInfo(translateAnimator, underlineAnimator, translationY, 0f)
    }

    fun setStrikeThroughEnabled(strikeThroughEnabled: Boolean, animate: Boolean = true) {
        if (!animate) super.setStrikeThroughEnabled(strikeThroughEnabled)
        else {
            strikeThroughIsRtl = !strikeThroughEnabled
            val fullLength = paint.measureText(text, 0, text?.length ?: 0)
            val endColor = if (strikeThroughEnabled) currentHintTextColor
            else normalTextColor
            argbValueAnimator(::setTextColor, currentTextColor, endColor, animatorConfig).start()
            floatValueAnimator(::setStrikeThroughLength, 0f, fullLength, animatorConfig).apply {
                if (!strikeThroughEnabled) doOnEnd { strikeThroughLength = null }
            }.start()
        }
    }

    // So that the properties can be used in the AnimatorUtils valueAnimator functions.
    private fun setUnderlineAlphaPrivate(value: Int) { underlineAlpha = value; invalidate() }
    private fun setStrikeThroughLength(value: Float) { strikeThroughLength = value; invalidate() }
}