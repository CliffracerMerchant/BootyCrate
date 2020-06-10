package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.MutableLiveData

//TODO: Don't force soft input to appear if hardware keyboard is present

/**     TextFieldEdit is an AppCompatEditText optimized for toggleable editing
 *  of a single line. When isEditable is false, the TextFieldEdit will present
 *  itself as a normal single line EditText. When isEditable is true, TextField-
 *  Edit will request focus when it is tapped and display a soft input. If the
 *  user presses the done action on the soft input, the TextFieldEdit's live-
 *  data member's value will be set. Changes to the displayed field can be list-
 *  ened to through this live data member.
 *      When in editable mode, TextFieldEdit will underline it's text to indi-
 *  cate this to the user. If the TextFieldEdit is empty, it will instead draw
 *  its underlined editableHint (like TextView.hint except that it is not dis-
 *  played when not editable). It will also toggle its input mode between
 *  TYPE_TEXT_FLAG_NO_SUGGESTIONS when not editable and TYPE_CLASS_TEXT when
 *  editable. This will prevent misspelling underlines from being displayed
 *  when the TextFieldEdit is not in an editable state. */
class TextFieldEdit(context: Context, attrs: AttributeSet?) :
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
        /*setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearFocus()
                imm?.hideSoftInputFromWindow(windowToken, 0)
                liveData.value = text.toString()
            }
            actionId == EditorInfo.IME_ACTION_DONE
        }*/
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
        inputType = if (editable) InputType.TYPE_CLASS_TEXT
                    else          InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        hint = if (editable) editableHint else null
        if (!editable && isFocused) clearFocus()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        paintFlags = if (isEditable) paintFlags or Paint.UNDERLINE_TEXT_FLAG
                     else paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        super.onDraw(canvas)
    }
}