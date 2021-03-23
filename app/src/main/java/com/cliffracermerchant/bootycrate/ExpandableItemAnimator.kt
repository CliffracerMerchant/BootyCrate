/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * A RecyclerView.ItemAnimator that animates the expanding and collapsing of items.
 *
 * ExpandableItemAnimator is a RecyclerView.ItemAnimator that provides an over-
 * ride of animateChange to animate the height changes of expanding or collaps-
 * ing items. It assumes that only one item can be expanded at a time, and
 * that a previously expanded item will be collapsed when a new one is expan-
 * ded. In order to play these animations correctly, it is necessary to call
 * the function notifyExpandedItemChanged with the adapter position of the
 * newly expanded item whenever it is changed, or null if the expanded item is
 * being collapsed. The currently expanded item can be queried via the prop-
 * erty expandedItemPos. The recycler view that uses ExpandableItemAnimator
 * must use item views that implement the ExpandableRecyclerViewItem interface.
 *
 * ExpandableItemAnimator also has an observer member that, when registered as
 * an adapter data observer for the adapter using the ExpandableItemAnimator
 * instance as its item animator, will automatically update the expanded item
 * position so that this doesn't need to be done manually. ExpandableItemAnim-
 * ator will attempt to register itself automatically, but if the parent recy-
 * cler view does not have an adapter when ExpandableItemAnimator is construc-
 * ted, this will have to be done manually.
 */
class ExpandableItemAnimator(
    private val recyclerView: RecyclerView,
    animatorConfig: AnimatorConfig,
    adapter: RecyclerView.Adapter<*>? = null
) : DefaultItemAnimator() {

    val expandedItemPos get() = _expandedItemPos
    private var _expandedItemPos: Int? = null
    private var collapsingItemPos: Int? = null

    private val pendingChangeAnimators = mutableListOf<Animator>()
    private val pendingRemoveAnimators = mutableListOf<ViewPropertyAnimator>()
    private val changingViews = mutableListOf<ExpandableRecyclerViewItem>()

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

    private var remainingScrollDistance = 0
    fun notifyExpandedItemChanged(newlyExpandedItemPos: Int?) {
        collapsingItemPos = expandedItemPos
        _expandedItemPos = newlyExpandedItemPos
        // The recyclerView's remaining scroll distance is sometimes needed during change
        // animations to determine whether the animated view needs to have an additional
        // translation animation performed. Unfortunately if this calculation is performed
        // at that time the value will reflect the remaining scroll distance after the
        // animations are finished, rather than before. This value is therefore calculated
        // here and stored in case it ends up being needed.
        remainingScrollDistance = recyclerView.computeVerticalScrollRange() -
                                  recyclerView.computeVerticalScrollOffset() -
                                  recyclerView.computeVerticalScrollExtent()
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

        // Animate the height change of the view
        val view = newHolder.itemView
        if (view !is ExpandableRecyclerViewItem)
            throw IllegalStateException("The item views used with ExpandableItemAnimator must " +
                                        "implement ExpandableItemAnimator.ExpandableRecyclerViewItem.")
        val pos = newHolder.adapterPosition

        // preInfo.top won't necessarily be the correct start value
        // if the view is on bottom and also needs to be translated.
        val start = view.top + preInfo.bottom - preInfo.top
        view.bottom = start
        pendingChangeAnimators.add(valueAnimatorOfInt(
            setter = view::setBottom, fromValue = start,
            toValue = postInfo.bottom, config = animatorConfig
        ).apply {
            doOnStart { dispatchChangeStarting(newHolder, true) }
            doOnEnd {
                dispatchChangeFinished(newHolder, true)
                if (pos == collapsingItemPos)
                    collapsingItemPos = null
            }
        })
        setupTranslationAnimIfNeeded(view, pos, heightChange)
        changingViews.add(view)
        return true
    }

    /**
     * Perform any needed translation animations on the view to prevent it from jumping.
     *
     * If another view is expanding as this one is collapsing and this is the view on
     * bottom, or if the recycler view is scrolled down all the way when this view is
     * being collapsed, the view must be translated by up to its height change to
     * prevent visual artifacts.
     */
    private fun setupTranslationAnimIfNeeded(view: View, pos: Int, heightChange: Int ) {
        val collapsingPos = collapsingItemPos
        val expandingPos = expandedItemPos
        val translationAmount = when {
            collapsingPos != null && expandingPos != null -> {
                val viewIsOnBottom = if (collapsingPos == pos) collapsingPos > expandingPos
                                     else                      expandingPos > collapsingPos
                if (viewIsOnBottom) heightChange.toFloat() else 0f
            }
            heightChange > 0 -> 0f
            else -> (heightChange + remainingScrollDistance).coerceAtMost(0).toFloat()
        }
        if (translationAmount != 0f) {
            view.translationY = translationAmount
            pendingChangeAnimators.add(valueAnimatorOfFloat(setter = view::setTranslationY,
                                                            fromValue = view.translationY,
                                                            toValue = 0f, config = animatorConfig))
        }
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder?): Boolean {
        val view = holder?.itemView ?: return false
        pendingRemoveAnimators.add(view.animate()
            .alpha(0f).withLayer()
            .applyConfig(animatorConfig)
            .withStartAction { dispatchRemoveStarting(holder) }
            .withEndAction {
                dispatchRemoveFinished(holder)
                recyclerView.layoutManager?.removeView(view)
            })
        return true
    }

    override fun runPendingAnimations() {
        super.runPendingAnimations()
        for (anim in pendingChangeAnimators) anim.start()
        for (anim in pendingRemoveAnimators) anim.start()
        for (view in changingViews) view.runPendingAnimations()
        pendingChangeAnimators.clear()
        pendingRemoveAnimators.clear()
        changingViews.clear()
    }

    /**
     * An interface for views that are used to represent expandable recycler view
     * items to describe what should change internally when expanded or collapsed.
     *
     * Implementing views should perform any necessary changes to child views
     * with a setExpanded implementation. Additionally, animations for these
     * changes should be prepared and stored if @param animate == true, and
     * then later played in an implementation of runPendingAnimations.
     */
    interface ExpandableRecyclerViewItem {
        fun expand() = setExpanded(true)
        fun collapse() = setExpanded(false)
        fun setExpanded(expanding: Boolean, animate: Boolean = true)
        fun runPendingAnimations()
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
}