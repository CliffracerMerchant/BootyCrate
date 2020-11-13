/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/** A RecyclerView.ItemAnimator that animates the expanding and collapsing of items.
 *
 *  ExpandableItemAnimator is a RecyclerView.ItemAnimator that provides an over-
 *  ride of animateChange to animate the height changes of expanding or collaps-
 *  ing items. It assumes that only one item can be expanded at a time, and
 *  that a previously expanded item will be collapsed when a new one is expan-
 *  ded. In order to play these animations correctly, it is necessary to call
 *  the function notifyExpandedItemChanged with the adapter position of the
 *  newly expanded item whenever it is changed, or null if the expanded item is
 *  being collapsed. The currently expanded item can be queried via the prop-
 *  erty expandedItemPos.
 *
 *  ExpandableItemAnimator also has an observer member that, when registered as
 *  an adapter data observer for the adapter using the ExpandableItemAnimator
 *  instance as its item animator, will automatically update the expanded item
 *  position so that this doesn't need to be done manually.*/
class ExpandableItemAnimator : DefaultItemAnimator() {
    private val pendingAnimations = mutableListOf<Animator>()
    val expandedItemPos get() = _expandedItemPos
    private var _expandedItemPos: Int? = null
    private var collapsingItemPos: Int? = null

    val observer = object: RecyclerView.AdapterDataObserver() {
        private var initialized = false

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (!initialized) {
                initialized = true
                return
            }
            val expandingPos = expandedItemPos ?: return
            if (expandingPos >= positionStart)
                _expandedItemPos = expandingPos + itemCount
        }
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            val expandingPos = expandedItemPos ?: return
            _expandedItemPos = adjustPosInRangeAfterMove(expandingPos, fromPosition,
                                                         toPosition, itemCount)
        }
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            if (expandedItemPos in positionStart until positionStart + itemCount)
                _expandedItemPos = null
        }
    }

    fun notifyExpandedItemChanged(newlyExpandedItemPos: Int?) {
        collapsingItemPos = expandedItemPos
        _expandedItemPos = newlyExpandedItemPos
    }

    /* The DefaultItemAnimator animations start playing in runPendingAnimations, rather
     * than in their respective functions (e.g. animateChange). If this custom change
     * animation is started here in animateChange, there will be a small lag time in
     * between the custom change animation and the default animations due to the slight
     * delay before runPendingAnimations is called. But if the change animations are
     * started in runPendingAnimations, the view's final state will be briefly visible
     * before the expanding/collapsing animation is started, causing a flicker effect.
     * To prevent this, the expanding/collapsing animation and the possible translation
     * animation are started here in animateChange, but paused immediately. They are
     * then resumed in runPendingAnimations when the DefaultItemAnimator animations
     * start. */
    override fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder,
                               preInfo: ItemHolderInfo, postInfo: ItemHolderInfo): Boolean {
        // If a view is being expanded or collapsed, oldHolder must be
        // equal to newHolder, and the heightChange must not be zero.
        if (oldHolder != newHolder) return false
        val heightChange = postInfo.bottom - postInfo.top - preInfo.bottom + preInfo.top
        if (heightChange == 0) return false

        // Animate the height change of the view
        val view = newHolder.itemView
        val pos = newHolder.adapterPosition
        val startHeight = preInfo.bottom - preInfo.top

        val expandCollapseAnim = ValueAnimator.ofInt(0, heightChange)
        expandCollapseAnim.addUpdateListener {
            view.bottom = view.top + startHeight + it.animatedValue as Int
        }
        expandCollapseAnim.duration = moveDuration
        expandCollapseAnim.doOnStart {
            dispatchChangeStarting(newHolder, true)
            expandCollapseAnim.pause()
        }
        expandCollapseAnim.doOnEnd {
            dispatchChangeFinished(newHolder, true)
            if (pos == collapsingItemPos)
                collapsingItemPos = null
        }
        pendingAnimations.add(expandCollapseAnim)
        expandCollapseAnim.start()

        // If another view is expanding as this one is collapsing,
        // the view on bottom must be translated by the same amount
        // as its height change to prevent visual artifacts.
        val collapsingPos = collapsingItemPos
        val expandingPos = expandedItemPos
        if (collapsingPos != null && expandingPos != null) {
            val viewIsOnBottom = if (collapsingPos == pos) collapsingPos > expandingPos
            else                      expandingPos > collapsingPos
            if (viewIsOnBottom) {
                view.translationY = heightChange.toFloat()
                val translationAnim = ObjectAnimator.ofFloat(view, "translationY", 0f)
                translationAnim.duration = moveDuration
                translationAnim.doOnStart { translationAnim.pause() }
                pendingAnimations.add(translationAnim)
                translationAnim.start()
            }
        }
        return true
    }

    /* For some reason no removal animations are played with this item animator when
       no override for animateRemove is provided. To prevent this the default removal
       animation, a fade out, is duplicated here. */
    override fun animateRemove(holder: RecyclerView.ViewHolder?): Boolean {
        val view = holder?.itemView ?: return false
        view.animate().alpha(0f).setDuration(removeDuration).withLayer().
        withEndAction { dispatchChangeFinished(holder, true) }.
        withStartAction {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            dispatchChangeStarting(holder, true)
        }.start()
        return true
    }

    override fun runPendingAnimations() {
        super.runPendingAnimations()
        for (anim in pendingAnimations) anim.resume()
        pendingAnimations.clear()
    }
}