/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/** A RecyclerView utility that manages the expansion of a single RecyclerView item at a time.
 *
 *  RecyclerViewExpandedItem is intended to be incorporated into a Recycler-
 *  View via composition to help it manage the expanded or collapsed state of
 *  its items. To accomplish this, the RecyclerView's adapter should provide
 *  an override of onBindViewHolder with a ExpansionState as a payload.
 *
 *  RecyclerViewExpandedItem sets the RecyclerView passed into it's con-
 *  structor's itemAnimator to an instance of the inner class ExpandableItem-
 *  Animator. If the item animator is changed afterwards, the expand/collapse
 *  animations will not play correctly. Due to this custom item animator, it is
 *  only necessary to change the visibility of the views that should be shown
 *  or hidden on an expand/collapse.
 *
 *  The current expanded item and view holder can be queried using the property
 *  id. The property id will return null if there is no expanded item. The
 *  expanded item can be set by view holder instance with the set function, or
 *  can be set to null (no expanded item) by passing null to the set function.
 *
 *  The RecyclerView must have an adapter that uses stable ids when the set
 *  function is called, or an IllegalStateException will be thrown. Calling the
 *  set function with a view holder that is not part of the RecyclerView that
 *  was passed in the constructor will result in undefined behavior. */
class RecyclerViewExpandedItem(private val recyclerView: RecyclerView) {
    val id get() = _expandedId

    private var _expandedId: Long? = null
    private var expandedViewHolderCache: RecyclerView.ViewHolder? = null
    private var previouslyExpandedPos: Int? = null

    init { recyclerView.itemAnimator = ExpandableItemAnimator() }

    fun reset() {
        _expandedId = null
        expandedViewHolderCache = null
        previouslyExpandedPos = null
    }

    fun set(newExpandedVh: RecyclerView.ViewHolder?) {
        val adapter = recyclerView.adapter
        if (adapter == null || !adapter.hasStableIds())
            throw IllegalStateException("The recycler view passed to RecyclerViewExpanded" +
                                        "Item must have an adapter that uses stable IDs.")


        // Return early if new expanded item is the same as the old one
        val newExpandedId = if (newExpandedVh == null) null
                            else adapter.getItemId(newExpandedVh.adapterPosition)
        if (newExpandedId == _expandedId) return

        // Collapse old expanded view holder
        val expandedVhCache = expandedViewHolderCache
        val expandedVhCacheIsValid = expandedVhCache != null &&
                                     adapter.getItemId(expandedVhCache.adapterPosition) == _expandedId
        val collapsingVh = if (expandedVhCacheIsValid) expandedVhCache
                           else recyclerView.findViewHolderForItemId(_expandedId ?: -1)
        previouslyExpandedPos = collapsingVh?.adapterPosition
        val previouslyExpandedPos = this.previouslyExpandedPos
        if (collapsingVh != null && previouslyExpandedPos != null)
            adapter.notifyItemChanged(previouslyExpandedPos, ExpansionState.Collapsed)

        // Update _expandedId and expand newly expanded view holder
        _expandedId = newExpandedId
        expandedViewHolderCache = newExpandedVh
        if (newExpandedVh != null)
            adapter.notifyItemChanged(newExpandedVh.adapterPosition, ExpansionState.Expanded)
    }

    /** A RecyclerView.ItemAnimator that animates the expanding and collapsing of items.
     *
     *  ExpandableItemAnimator is a RecyclerView.ItemAnimator that can be used with a
     *  RecyclerView that uses a RecyclerViewExpandedItem member to easily animate the
     *  expanding and collapsing of its items. */
    inner class ExpandableItemAnimator : DefaultItemAnimator() {
        val pendingAnimations = mutableListOf<Animator>()

        /* The DefaultItemAnimator animations start playing in runPendingAnimations,
         * rather than in animateChange. If these custom animations are started here in
         * animateChange, there will be a small lag time in between these custom anima-
         * tions and the default ones due to the slight delay before runPendingAnimations
         * is called. But if the expanding/collapsing animations are started in runPending-
         * Animations, the view's final state will be briefly visible before the expan-
         * ding/collapsing animation is started, causing a flicker effect. To prevent this,
         * the expanding/collapsing animation and the possible translation animation are
         * started here in animateChange, but paused immediately. They are then resumed
         * in runPendingAnimations when the DefaultItemAnimator animations start. */
        override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder,
            preInfo: ItemHolderInfo,
            postInfo: ItemHolderInfo
        ): Boolean {
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
            expandCollapseAnim.doOnEnd { dispatchChangeFinished(newHolder, true) }
            pendingAnimations.add(expandCollapseAnim)
            expandCollapseAnim.start()

            // If another view is expanding as this one is collapsing,
            // the view on bottom must be translated by the same amount
            // as its height change to prevent visual artifacts.
            val collapsingPos = previouslyExpandedPos
            val expandingPos = expandedViewHolderCache?.adapterPosition
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

        override fun runPendingAnimations() {
            super.runPendingAnimations()
            for (anim in pendingAnimations) anim.resume()
            pendingAnimations.clear()
        }
    }

}