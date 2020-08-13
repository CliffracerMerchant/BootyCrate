/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import androidx.recyclerview.widget.RecyclerView

/** A RecyclerView utility that manages the expansion o of a single RecyclerView item at a time.
 *
 *  RecyclerViewExpandedItem is intended to be incorporated into a RecyclerView
 *  via composition to help it manage the expanded or collapsed state of its
 *  items. To accomplish this, the RecyclerView's view holder must inherit the
 *  ExpandableViewHolder interface and implement the function onExpansionState-
 *  Changed to determine what will happen with the view upon its collapse or
 *  expansion.
 *
 *  The current expanded item and view holder can be queried using the proper-
 *  ties pos and viewHolder. Both of these properties will return null if there
 *  is no expanded item. The expanded item can be set either by position or by
 *  view holder instance with the set function overloads, or can be set to null
 *  (no expanded item) by passing null to the set function. Setting the
 *  expanded item by view holder is recommended, as it prevents a relatively
 *  costly call to findViewHolderForAdapterPosition.
 *
 *  The expanded item does not need to be manually updated for a move or dele-
 *  tion due to RecyclerViewExpandedItem's AdapterDataObserver overrides, which
 *  will automatically update the expanded item in response to these operations. */
class RecyclerViewExpandedItem(private val recyclerView: RecyclerView) :
        RecyclerView.AdapterDataObserver() {
    private var _expandedId: Long? = null
    val id get() = _expandedId
    private var expandedViewHolderCache: ExpandableViewHolder? = null

    fun set(
        newExpandedVh: ExpandableViewHolder?,
        animateCollapse: Boolean = true,
        animateExpand: Boolean = true
    ) {
        val adapter = recyclerView.adapter ?: return
        val expandedVhCache = expandedViewHolderCache
        if (expandedVhCache != null && adapter.getItemId(expandedVhCache.adapterPos) == _expandedId)
            expandedVhCache.onExpansionStateChange(false, animateCollapse)
        _expandedId = if (newExpandedVh == null) null
                      else adapter.getItemId(newExpandedVh.adapterPos)
        expandedViewHolderCache = newExpandedVh
        newExpandedVh?.onExpansionStateChange(true, animateExpand)
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        val adapter = recyclerView.adapter ?: return
        for (pos in positionStart until positionStart + itemCount)
            if (adapter.getItemId(pos) == _expandedId)
                _expandedId = null
    }

    interface ExpandableViewHolder {
        fun onExpansionStateChange(expanding: Boolean, animate: Boolean = true)
        val adapterPos get() = (this as RecyclerView.ViewHolder).adapterPosition
    }
}