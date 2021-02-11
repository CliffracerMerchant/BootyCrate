/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.SearchView

fun defaultLayoutTransition() = LayoutTransition().apply {
    setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
    setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
    setStartDelay(LayoutTransition.APPEARING, 0)
    setStartDelay(LayoutTransition.DISAPPEARING, 0)
    setStartDelay(LayoutTransition.CHANGING, 0)
    val interp = AccelerateDecelerateInterpolator()
    setInterpolator(LayoutTransition.CHANGE_APPEARING, interp)
    setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, interp)
    setInterpolator(LayoutTransition.APPEARING, interp)
    setInterpolator(LayoutTransition.DISAPPEARING, interp)
    setInterpolator(LayoutTransition.CHANGING, interp)
}

fun LayoutTransition.doOnStart(onStart: (transition: LayoutTransition,
                                         container: ViewGroup, view: View,
                                         transitionType: Int) -> Unit = {_, _, _, _ -> }) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) =
            onStart(a, b, c, d)
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) { }
    })
}

fun LayoutTransition.doOnEnd(onEnd: (transition: LayoutTransition,
                                     container: ViewGroup, view: View,
                                     transitionType: Int) -> Unit = {_, _, _, _ -> }) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) { }
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) =
            onEnd(a, b, c, d)
    })
}

/** For a given position in a range, return the position after
 *  a move operation on one or more items is performed.
 *  @param pos The before move position whose position after the move is desired.
 *  @param moveStartPos The start position of the to-be-moved range before the move.
 *  @param moveEndPos The start position of the to-be-moved range after the move.
 *  @param moveCount The number of items being moved.
 *  @return The new position of the pos parameter after the move. */
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

fun Bitmap.getPixelAtCenter(view: View) = getPixel(view.centerX(), view.centerY())
fun View.centerX() = left + width / 2
fun View.centerY() = top + height / 2

internal object UtilsPrivate {
    val matrix = Matrix()
    val matrixValues = FloatArray(9)
    val rect = Rect()
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

/** A SearchView that allows multiple onSearchClickListeners and onCloseListeners. */
class MultiListenerSearchView(context: Context, attrs: AttributeSet) : SearchView(context, attrs) {
    private val onOpenListeners = mutableListOf<OnClickListener>()
    private val onCloseListeners = mutableListOf<OnCloseListener>()

    init {
        super.setOnSearchClickListener { for (l in onOpenListeners) l.onClick(this) }
        super.setOnCloseListener { for (l in onCloseListeners) l.onClose(); false }
    }

    override fun setOnSearchClickListener(l: OnClickListener?) {
        if (l != null) onOpenListeners.add(l)
    }
    override fun setOnCloseListener(l: OnCloseListener) { onCloseListeners.add(l) }
}