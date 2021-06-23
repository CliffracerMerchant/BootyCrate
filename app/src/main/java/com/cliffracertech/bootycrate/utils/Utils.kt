/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentActivity

/**
 * For a given position in a range, return the position after
 * a move operation on one or more items is performed.
 * @param pos The before move position whose position after the move is desired.
 * @param moveStartPos The start position of the to-be-moved range before the move.
 * @param moveEndPos The start position of the to-be-moved range after the move.
 * @param moveCount The number of items being moved.
 * @return The new position of the pos parameter after the move.
 */
fun adjustPosInRangeAfterMove(pos: Int, moveStartPos: Int, moveEndPos: Int, moveCount: Int): Int {
    val oldRange = moveStartPos until moveStartPos + moveCount
    val newRange = moveEndPos until moveEndPos + moveCount
    return when { pos in oldRange ->
                      pos + moveEndPos - moveStartPos
                  pos < oldRange.first && pos >= newRange.first ->
                      pos + moveCount
                  pos > oldRange.last && pos <= newRange.first ->
                      pos - moveCount
                  else -> pos }
}

/** Return a NotificationManager system service from the context. */
fun notificationManager(context: Context) =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
/** Return an AlarmManager system service from the context. */
fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

/** Return @param context as a FragmentActivity. */
fun Context.asFragmentActivity() =
    try { this as FragmentActivity }
    catch(e: ClassCastException) {
        try { (this as ContextWrapper).baseContext as FragmentActivity }
        catch(e: ClassCastException) {
            throw ClassCastException("The provided context must be an instance of FragmentActivity")
        }
    }

/** An object that, once initialized by calling init with an instance of Context,
 * can be used to either hide, implicitly show, or explicitly show the soft input
 * given a view instance using the functions hide, showImplicitly, and showExplicitly. */
object SoftKeyboard {
    private lateinit var imm: InputMethodManager
    fun init(context: Context) {
        imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    fun hide(view: View) = imm.hideSoftInputFromWindow(view.windowToken, 0)
    fun showImplicitly(view: View) = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    fun showExplicitly(view: View) = imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
}

fun View.setHeight(height: Int) { bottom = top + height }

fun View.clearFocusAndHideSoftInput() {
    if (!isFocused) return
    clearFocus()
    SoftKeyboard.hide(this)
}

/** Return the provided dp amount in terms of pixels. */
fun Resources.dpToPixels(dp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)

/** Return the provided sp amount in terms of pixels. */
fun Resources.spToPixels(sp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics)

/** Return the provided dp amount in terms of pixels. */
fun Context.dpToPixels(dp: Float) = resources.dpToPixels(dp)
/** Return the provided sp amount in terms of pixels. */
fun Context.spToPixels(sp: Float) = resources.spToPixels(sp)

private val typedValue = TypedValue()
/** Resolve the current theme's value for the provided int attribute. */
fun Resources.Theme.resolveIntAttribute(attr: Int): Int {
    resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

/** Return the IntArray pointed to by @param arrayResId, resolving theme attributes if necessary. */
fun Context.getIntArray(arrayResId: Int): IntArray {
    val ta = resources.obtainTypedArray(arrayResId)
    val array = IntArray(ta.length()) {
        if (ta.peekValue(it).type == TypedValue.TYPE_ATTRIBUTE)
            theme.resolveIntAttribute(ta.peekValue(it).data)
        else ta.getColor(it, 0)
    }
    ta.recycle()
    return array
}