/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Shader
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
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
    return if (pos in oldRange)
         pos + moveEndPos - moveStartPos
    else if (pos < oldRange.first && pos >= newRange.first)
        pos + moveCount
    else if (pos > oldRange.last && pos <= newRange.first)
        pos - moveCount
    else pos
}

fun View.centerX() = left + width / 2
fun View.centerY() = top + height / 2
fun Bitmap.getPixelAtCenter(view: View) = getPixel(view.centerX(), view.centerY())

internal object UtilsPrivate {
    val matrix = Matrix()
    val matrixValues = FloatArray(9)
}

/** Translate the shader by dx, dy; will not affect the Shader's scale.*/
fun Shader.translateBy(dx: Float, dy: Float) {
    getLocalMatrix(UtilsPrivate.matrix)
    UtilsPrivate.matrix.getValues(UtilsPrivate.matrixValues)
    UtilsPrivate.matrixValues[2] = dx
    UtilsPrivate.matrixValues[5] = dy
    UtilsPrivate.matrix.setValues(UtilsPrivate.matrixValues)
    setLocalMatrix(UtilsPrivate.matrix)
}

fun SearchView.setOnQueryTextChangeListener(listener: (String?) -> Boolean) {
    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?) = true
        override fun onQueryTextChange(newText: String?) = listener.invoke(newText)
    })
}

/** Return a InputMethodManager system service from the @param context. */
fun inputMethodManager(context: Context) =
    context.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager

/** Return @param context as a FragmentActivity. */
fun fragmentActivityFrom(context: Context) =
    try { context as FragmentActivity }
    catch(e: ClassCastException) {
        try { (context as ContextWrapper).baseContext as FragmentActivity }
        catch(e: ClassCastException) {
            throw ClassCastException("The provided context must be an instance of FragmentActivity")
        }
    }

fun View.setHeight(height: Int) { bottom = top + height }

