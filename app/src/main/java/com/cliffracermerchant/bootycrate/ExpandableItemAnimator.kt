/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.Animator
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
 * erty expandedItemPos.
 *
 * ExpandableItemAnimator also has an observer member that, when registered as
 * an adapter data observer for the adapter using the ExpandableItemAnimator
 * instance as its item animator, will automatically update the expanded item
 * position so that this doesn't need to be done manually. ExpandableItemAnim-
 * ator will attempt to register itself automatically, but if the parent recy-
 * cler view does not have an adapter when ExpandableItemAnimator is construc-
 * ted this will have to be done manually.
 */
class ExpandableItemAnimator(
    private val recyclerView: RecyclerView,
    private val animatorConfig: AnimatorConfigs.Config = AnimatorConfigs.translation
) : DefaultItemAnimator() {

    val expandedItemPos get() = _expandedItemPos
    private var _expandedItemPos: Int? = null
    private var collapsingItemPos: Int? = null

    private val pendingChangeAnimators = mutableListOf<Animator>()
    private val pendingRemoveAnimators = mutableListOf<ViewPropertyAnimator>()
    private val changingViews = mutableListOf<ExpandableSelectableItemView<*>>()

    val observer = object: RecyclerView.AdapterDataObserver() {
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
    }

    init {
        recyclerView.adapter?.registerAdapterDataObserver(observer)
        addDuration = animatorConfig.duration
        changeDuration = animatorConfig.duration
        removeDuration = animatorConfig.duration
        moveDuration = animatorConfig.duration
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

        // If a view is being expanded or collapsed, oldHolder must be
        // equal to newHolder, and the heightChange must not be zero.
        if (oldHolder != newHolder) return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
        val heightChange = postInfo.bottom - postInfo.top - preInfo.bottom + preInfo.top
        if (heightChange == 0) return super.animateChange(oldHolder, newHolder, preInfo, postInfo)

        // Animate the height change of the view
        val view = newHolder.itemView
        val pos = newHolder.adapterPosition

        // preInfo.top won't necessarily be the correct start value
        // if the view is on bottom and also needs to be translated.
        val start = view.top + preInfo.bottom - preInfo.top
        valueAnimatorOfInt(view::setBottom, start, postInfo.bottom, animatorConfig).apply {
            doOnStart { dispatchChangeStarting(newHolder, true) }
            doOnEnd {
                dispatchChangeFinished(newHolder, true)
                if (pos == collapsingItemPos)
                    collapsingItemPos = null
            }
            pendingChangeAnimators.add(this)
            start(); pause()
        }

     /* If another view is expanding as this one is collapsing,
        the view on bottom must be translated by the same amount
        as its height change to prevent visual artifacts. */
        val collapsingPos = collapsingItemPos
        val expandingPos = expandedItemPos
        if (collapsingPos != null && expandingPos != null) {
            val viewIsOnBottom = if (collapsingPos == pos) collapsingPos > expandingPos
                                 else                      expandingPos > collapsingPos
            if (viewIsOnBottom) valueAnimatorOfFloat(
                setter = view::setTranslationY,
                fromValue = heightChange * 1f,
                toValue = 0f, config = animatorConfig
            ).apply {
                pendingChangeAnimators.add(this)
                start(); pause()
            }
        }

     /* The below cast to ExpandableSelectableItemView is not documented in the
        class documentation and undesirably couples ExpandableItemAnimator to the
        use of ExpandableSelectableItemView. An attempt was made to introduce a
        member called onExpandCollapseAnimationStartedListener that would be
        called with the view holder being expanded or collapsed whenever a change
        animation was prepared. Through this the containing recycler view could
        then call ExpandableSelectableItemView.runPendingAnimations so that the
        view's internal animations would sync with those of the item animator.
        Unfortunately the animations were not synced properly when this was done.
        For the time being, we'll just assume the use of ExpandableSelectableItem-
        View here since ExpandableItemAnimator is always used with ExpandableSel-
        ectableItemView in this project. */
        changingViews.add(view as ExpandableSelectableItemView<*>)
        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder?): Boolean {
        val view = holder?.itemView ?: return false
        pendingRemoveAnimators.add(view.animate()
            .alpha(0f).withLayer()
            .applyConfig(AnimatorConfigs.fadeOut)
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
    }
}