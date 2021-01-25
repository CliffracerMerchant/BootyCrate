/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.LayoutTransition
import android.view.View
import android.view.ViewGroup

fun delaylessLayoutTransition() = LayoutTransition().apply {
    setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
    setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
    setStartDelay(LayoutTransition.APPEARING, 0)
    setStartDelay(LayoutTransition.DISAPPEARING, 0)
    setStartDelay(LayoutTransition.CHANGING, 0)
}

fun LayoutTransition.doOnStart(onStart: (transition: LayoutTransition,
                                         container: ViewGroup, view: View,
                                         transitionType: Int) -> Unit = {_, _, _, _ -> }) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(transition: LayoutTransition, container: ViewGroup,
                                     view: View, transitionType: Int) {
            onStart(transition, container, view, transitionType)
        }
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) { }
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