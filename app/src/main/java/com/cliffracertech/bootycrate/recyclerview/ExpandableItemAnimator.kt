/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.animation.Animator
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.utils.*

/**
 * A RecyclerView.ItemAnimator that animates the expanding and collapsing of items.
 *
 * ExpandableItemAnimator is a RecyclerView.ItemAnimator that provides an
 * override of animateChange to animate the height changes of expanding or
 * collapsing items. It assumes that only one item can be expanded at a
 * time, and that a previously expanded item will be collapsed when a new
 * one is expanded. In order to play these animations correctly, it is
 * necessary to necessary to call the function notifyExpandedItemChanged
 * with the adapter position of the newly expanded item whenever it is
 * changed, or null if the expanded item is being collapsed. The currently
 * expanded item can be queried via the property expandedItemPos. The
 * recycler view that uses ExpandableItemAnimator must use item views that
 * implement the ExpandableRecyclerViewItem interface.
 *
 * The property expandCollapseAnimationFinishedListener can be set to
 * listen for when expand collapse animations are finished. The boolean
 * parameter represents whether or not the view is expanding (false implies
 * that the view is collapsing instead), and the int parameter represents
 * the adapter position of the item.
 *
 * ExpandableItemAnimator can also create an observer that, when registered
 * as an adapter data observer for the adapter using the ExpandableItemAnimator
 * instance as its item animator, will automatically update the expanded
 * item position so that this doesn't need to be done manually by calling
 * notifyExpandedItemChanged. ExpandableItemAnimator will attempt to
 * register itself automatically, but if the parent recycler view does not
 * have an adapter when ExpandableItemAnimator is constructed, this will
 * have to be done manually.
 */
class ExpandableItemAnimator(
    private val recyclerView: RecyclerView,
    animatorConfig: AnimatorConfig,
    adapter: RecyclerView.Adapter<*>? = null
) : DefaultItemAnimator() {

    private var _expandedItemPos: Int? = null
    private var collapsingItemPos: Int? = null
    val expandedItemPos get() = _expandedItemPos
    private var _expandCollapseAnimationInProgress = false
    val expandCollapseAnimationInProgress get() = _expandCollapseAnimationInProgress
    private val pendingAnimators = mutableListOf<Animator>()
    private val pendingViewPropAnimators = mutableListOf<ViewPropertyAnimator>()
    private val changingViews = mutableListOf<ExpandableRecyclerViewItem>()
    var expandCollapseAnimationFinishedListener: ((Boolean, Int)-> Unit)? = null

    var animatorConfig = animatorConfig
        set(value) { field = value
                     addDuration = value.duration
                     changeDuration = value.duration
                     removeDuration = value.duration
                     moveDuration = value.duration }

    init {
        adapter?.let { registerAdapterDataObserver(it) }
        this.animatorConfig = animatorConfig
    }

    fun notifyExpandedItemChanged(newlyExpandedItemPos: Int?) {
        collapsingItemPos = expandedItemPos
        _expandedItemPos = newlyExpandedItemPos
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        preInfo: ItemHolderInfo,
        postInfo: ItemHolderInfo
    ): Boolean {
        // If a view is being expanded or collapsed, oldHolder must be
        // equal to newHolder, and the heightChange must not be zero.
        if (oldHolder != newHolder)
            return super.animateChange(oldHolder, newHolder, preInfo, postInfo)

        val heightChange = postInfo.bottom - postInfo.top - preInfo.bottom + preInfo.top
        if (heightChange == 0)
            return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
        val view = newHolder.itemView
        if (view !is ExpandableRecyclerViewItem) throw IllegalStateException(
            "The item views used with ExpandableItemAnimator must " +
            "implement ExpandableItemAnimator.ExpandableRecyclerViewItem.")

        val startHeight = preInfo.bottom - preInfo.top
        setupHeightChangeAnimation(newHolder, view, startHeight, heightChange)
        val topChange = postInfo.top - preInfo.top
        setupTopChangeAnimation(view, newHolder.adapterPosition, topChange, heightChange)
        changingViews.add(view)
        return true
    }

    private fun setupHeightChangeAnimation(holder: RecyclerView.ViewHolder,
                                           view: View, start: Int, change: Int) {
        view.setHeight(start)
        intValueAnimator(view::setHeight, start, start + change, animatorConfig).apply {
            doOnStart { dispatchChangeStarting(holder, true)
                        _expandCollapseAnimationInProgress = true }
            doOnEnd {
                dispatchChangeFinished(holder, true)
                // While _expandCollapseAnimationInProgress might be set to false before
                // the the second of a pair of expand collapse animations finishes, the
                // expand and collapse animations should be synced closely enough for
                // this to not matter.
                _expandCollapseAnimationInProgress = false
                if (holder.adapterPosition == collapsingItemPos)
                    collapsingItemPos = null
                expandCollapseAnimationFinishedListener?.invoke(change > 0, holder.adapterPosition)
            }
            pendingAnimators.add(this)
        }
    }

    private fun setupTopChangeAnimation(view: View, pos: Int, topChange: Int, heightChange: Int) {
        var translationAmount = 0
        if (topChange != 0)
            translationAmount = topChange
        else {
            val collapsingPos = collapsingItemPos
            val expandingPos = expandedItemPos
            if (collapsingPos != null && expandingPos != null) {
                val viewIsOnBottom = if (collapsingPos == pos) collapsingPos > expandingPos
                                     else                      expandingPos > collapsingPos
                if (viewIsOnBottom) translationAmount = -heightChange
            }
        }
        if (translationAmount != 0) {
            view.translationY = -translationAmount.toFloat()
            pendingViewPropAnimators.add(view.animate().translationY(0f).applyConfig(animatorConfig))
        }
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder?): Boolean {
        val view = holder?.itemView ?: return false
        view.animate().alpha(0f).withLayer()
                      .applyConfig(animatorConfig)
                      .withStartAction { dispatchRemoveStarting(holder) }
                      .withEndAction { dispatchRemoveFinished(holder) }
                      .start()
        return true
    }

    override fun runPendingAnimations() {
        super.runPendingAnimations()
        for (anim in pendingAnimators) anim.start()
        for (anim in pendingViewPropAnimators) anim.start()
        for (view in changingViews) view.runPendingAnimations()
        pendingAnimators.clear()
        pendingViewPropAnimators.clear()
        changingViews.clear()
    }

    fun registerAdapterDataObserver(adapter: RecyclerView.Adapter<*>) {
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            private var initialized = false

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (!initialized) initialized = true
                else {
                    val expandingPos = expandedItemPos ?: return
                    if (expandingPos >= positionStart)
                        _expandedItemPos = expandingPos + itemCount
                }
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
        })
    }

    /**
     * An interface for views that are used to represent expandable recycler view
     * items to describe what should change internally when expanded or collapsed.
     *
     * Implementing views should perform any necessary changes to child views
     * with a setExpanded implementation. Animations for these changes should
     * be prepared and stored if @param animate == true, and then later played
     * in an implementation of runPendingAnimations.
     */
    interface ExpandableRecyclerViewItem {
        fun expand() = setExpanded(true)
        fun collapse() = setExpanded(false)
        fun setExpanded(expanding: Boolean, animate: Boolean = true)
        fun runPendingAnimations()
    }
}