/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Activity
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

/** Return a InputMethodManager system service from the @param context. */
fun inputMethodManager(context: Context) =
    context.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager

/** Return @param context as a FragmentActivity. */
fun Context.asFragmentActivity() =
    try { this as FragmentActivity }
    catch(e: ClassCastException) {
        try { (this as ContextWrapper).baseContext as FragmentActivity }
        catch(e: ClassCastException) {
            throw ClassCastException("The provided context must be an instance of FragmentActivity")
        }
    }

fun View.setHeight(height: Int) { bottom = top + height }

fun dpToPixels(dp: Float, resources: Resources) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

private val typedValue = TypedValue()
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