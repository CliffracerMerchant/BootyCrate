/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.*
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.animation.doOnEnd
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData

/** A view to edit a single line text field.
 *
 *  TextFieldEdit is an AppCompatEditText optimized for toggleable editing
 *  of a single line. When isEditable is false, the TextFieldEdit will present
 *  itself as a normal single line TextView. When isEditable is true, TextField-
 *  Edit will request focus when it is tapped and display a soft input. If the
 *  user presses the done action on the soft input or if the focus is changed,
 *  the TextFieldEdit's liveData member's value will be set. Changes to the dis-
 *  played field can be listened to through this live data member.
 *
 *  When in editable mode, TextFieldEdit will underline it's text to indicate
 *  this to the user. If the TextFieldEdit is empty, it will instead draw its
 *  underlined editableHint (like TextView.hint except that it is not displayed
 *  when not editable). If editable hint is null, the TextFieldEdit will use
 *  TextView's hint as normal (displayed all the time when empty). */
open class TextFieldEdit(context: Context, attrs: AttributeSet?) :
        AppCompatEditText(context, attrs) {
    val liveData = MutableLiveData<String>()
    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

    var editableHint: String?
    var isEditable: Boolean get() = isFocusableInTouchMode
                            set(editable) = setEditablePrivate(editable)

    init {
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.TextFieldEdit)
        isEditable = styledAttrs.getBoolean(R.styleable.TextFieldEdit_isEditable, false)
        editableHint = styledAttrs.getString(R.styleable.TextFieldEdit_editableHint)
        styledAttrs.recycle()
        maxLines = 1
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
        ellipsize = TextUtils.TruncateAt.END
    }

    override fun onEditorAction(actionCode: Int) {
        if (actionCode == EditorInfo.IME_ACTION_DONE) {
            clearFocus()
            imm?.hideSoftInputFromWindow(windowToken, 0)
            liveData.value = text.toString()
        }
        super.onEditorAction(actionCode)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) liveData.value = text.toString()
    }

    private fun setEditablePrivate(editable: Boolean) {
        isFocusableInTouchMode = editable
        /* Setting the input type here will prevent misspelling underlines from
         * being displayed when the TextFieldEdit is not in an editable state. */
        inputType = if (editable) InputType.TYPE_CLASS_TEXT
                    else          InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        if (editableHint != null) hint = if (editable) editableHint
                                         else          null
        if (!editable && isFocused) clearFocus()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        paintFlags = if (isEditable) paintFlags or Paint.UNDERLINE_TEXT_FLAG
                     else            paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        super.onDraw(canvas)
    }
}

/** A TextFieldEdit subclass that allows the toggling of a strike-through
 *  effect, optionally with an animation, using the public function setStrike-
 *  throughEnabled. */
class AnimatedStrikeThroughTextFieldEdit(context: Context, attrs: AttributeSet) :
        TextFieldEdit(context, attrs) {
    private var strikeThroughLength: Float? = null
    private var strikeThroughAnimationIsReversed = false
    private val normalTextColor = currentTextColor

    init {
        doAfterTextChanged { text ->
            if (strikeThroughLength != null)
                strikeThroughLength = paint.measureText(text, 0, text?.length ?: 0)
        }
    }

    fun setStrikeThroughEnabled(strikeThroughEnabled: Boolean, animate: Boolean = true) {
        strikeThroughAnimationIsReversed = !strikeThroughEnabled
        val fullLength = paint.measureText(text, 0, text?.length ?: 0)
        if (animate) {
            ObjectAnimator.ofArgb(this, "textColor", currentTextColor,
                                  if (strikeThroughEnabled) currentHintTextColor
                                  else                      normalTextColor).start()
            val anim = ValueAnimator.ofFloat(0f, fullLength)
            anim.addUpdateListener { strikeThroughLength = anim.animatedValue as Float
                                     invalidate() }
            if (!strikeThroughEnabled) anim.doOnEnd { strikeThroughLength = null }
            anim.start()
        } else {
            strikeThroughLength = if (strikeThroughEnabled) fullLength else null
            setTextColor(if (strikeThroughEnabled) currentHintTextColor
                         else                      normalTextColor)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val strikeThroughLength = this.strikeThroughLength ?: return

        val fullLength = paint.measureText(text, 0, text?.length ?: 0)
        paint.strokeWidth = textSize / 12
        val begin = if (!strikeThroughAnimationIsReversed) 0f
                    else strikeThroughLength
        val end = if (strikeThroughAnimationIsReversed) fullLength
                  else strikeThroughLength
        canvas.drawLine(begin, baseline / 1.4f, end, baseline / 1.4f, paint)
    }
}