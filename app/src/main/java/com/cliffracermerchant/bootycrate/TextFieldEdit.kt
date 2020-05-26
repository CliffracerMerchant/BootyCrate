package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
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
    private val bounds = Rect()
    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    var editableHint: String?

    var isEditable: Boolean get() = isFocusableInTouchMode
                            set(editable) = setEditablePrivate(editable)

    init {
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.TextFieldEdit)
        isEditable = styledAttrs.getBoolean(R.styleable.TextFieldEdit_isEditable, false)
        editableHint = styledAttrs.getString(R.styleable.TextFieldEdit_editableHint)
        styledAttrs.recycle()
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorHint, typedValue, true)
        maxLines = 1
        //isSingleLine = true
        paint.strokeWidth = textSize / 18f // for underline drawing
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
        ellipsize = TextUtils.TruncateAt.END
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearFocus()
                imm?.hideSoftInputFromWindow(windowToken, 0)
                liveData.value = text.toString()
            }
            actionId == EditorInfo.IME_ACTION_DONE
        }
    }

    private fun setEditablePrivate(editable: Boolean) {
        isFocusableInTouchMode = editable
        inputType = if (editable) InputType.TYPE_CLASS_TEXT
                    else          InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        hint = if (editable) editableHint else null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isEditable) return
        val baseLine = getLineBounds(0, bounds)
        var string = text.toString()
        if (string.isEmpty()) {
            if (hint == null || hint.isEmpty()) return
            assert(hint.toString() == editableHint)
            string = editableHint ?: "editable field here"
        }
        val underlineWidth = paint.measureText(string, 0, string.length)
        canvas.drawLine(0f,             baseLine + 7f,
                        underlineWidth, baseLine + 7f, paint)
    }
}