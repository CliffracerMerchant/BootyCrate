/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** A ViewModelRecyclerView subclass that enables multi-selection and expansion of items.
 *
 *  SelectableExpandableRecyclerView extends ViewModelRecyclerView by incorpora-
 *  ting a Selection and an ExpandedItem as members to facilitate multi-selec-
 *  tion and expansion of the items it contains. SelectableExpandableRecycler-
 *  View also utilizes its own custom adapter, SelectableItemAdapter, and view
 *  holder, ExpandableViewHolder.
 *
 *  Because users would likely want to view extra details of newly inserted
 *  items, SelectableExpandableRecyclerView overrides ViewModelRecyclerView's
 *  onNewItemInsertion so that new items are automatically expanded. */
abstract class SelectableExpandableRecyclerView<Entity: ViewModelItem>(
    context: Context,
    attrs: AttributeSet
) : ViewModelRecyclerView<Entity>(context, attrs) {

    abstract override val adapter: SelectableItemAdapter<out ExpandableViewHolder>
    val selection = Selection()
    val expandedItem = ExpandedItem()
    private val selectedColor: Int
    private val itemNormalBackgroundColor: Int

    init {
        addItemDecoration(ItemSpacingDecoration(context))
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
        selectedColor = typedValue.data
        context.theme.resolveAttribute(R.attr.recyclerViewItemColor, typedValue, true)
        itemNormalBackgroundColor = typedValue.data
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
    }

    override fun onNewItemInsertion(item: Entity, vh: ViewModelItemViewHolder) {
        super.onNewItemInsertion(item, vh)
        expandedItem.set((vh as SelectableExpandableRecyclerView<Entity>.ExpandableViewHolder),
                         animateCollapse = true, animateExpand = false)
    }

    override fun deleteItems(ids: LongArray) {
        for (id in ids) if (selection.contains(id)) selection
        super.deleteItems(ids)
        // Since the items are being removed, no visual deselection change is necessary
        selection.clearWithoutVisualUpdate(ids)
    }

    /** A subclass of ViewModelAdapter that contains onBindViewHolder overrides to
     *  update the items visual selected / not selected state, and enforces the use
     *  of BootyCrateViewHolder. It does not implement onCreateViewHolder, and is
     *  therefore abstract.*/
    abstract inner class SelectableItemAdapter<VHType: ExpandableViewHolder> :
            ViewModelAdapter<VHType>() {

        override fun onBindViewHolder(holder: VHType, position: Int) {
            super.onBindViewHolder(holder, position)
            val isSelected = selection.contains(getItemId(position))
            holder.itemView.background.setTint(if (isSelected) selectedColor
                                               else itemNormalBackgroundColor)
        }

        override fun onBindViewHolder(
            holder: VHType,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty())
                return onBindViewHolder(holder, position)
            val unhandledChanges = mutableListOf<Any>()
            for (payload in payloads)
                if (payload is SelectionState) {
                    val isSelected = payload == SelectionState.Selected
                    val startColor = if (isSelected) itemNormalBackgroundColor else selectedColor
                    val endColor =   if (isSelected) selectedColor else itemNormalBackgroundColor
                    ObjectAnimator.ofArgb(holder.itemView.background,
                                          "tint", startColor, endColor).start()
                }
                else unhandledChanges.add(payload)
            if (unhandledChanges.isNotEmpty())
                super.onBindViewHolder(holder, position, payloads)
        }
    }

    /** A ViewModelItemViewHolder subclass that provides an overridable function onExpansionStateChanged(). */
    open inner class ExpandableViewHolder(view: View) : ViewModelItemViewHolder(view) {
        open fun onExpansionStateChanged(expanding: Boolean, animate: Boolean = true) = 0
    }

    /** A RecyclerView utility to keep track of a multi-selection in a RecyclerView.
     *
     *  Selection is a utility class intended to be incorporated into a RecyclerView
     *  via composition that keeps track of a multi-selection of the RecyclerView
     *  items. It can be queried using its public function contains and its size and
     *  isEmpty properties. The RecyclerView.AdapterDataObserver.onItemRangeRemoved
     *  override will cause selected items to be automatically removed from the sel-
     *  ection when they are deleted.
     *
     *  The selection size is exposed via the sizeLiveData property to allow on
     *  selection size changed listeners to respond to a change in selection size.
     *
     *  The selection modifying functions add, remove, toggle, and clear will call
     *  the appropriate notifyItemChanged function on the adapter provided in the
     *  constructor so that the items' visual selected/not-selected statuses can be
     *  updated. To support partial binding, these notifyItemChanged calls use a
     *  SelectionState value as a payload to indicate the new state of
     *  the item. */
    inner class Selection :
        RecyclerView.AdapterDataObserver() {
        private val hashMap = HashMap<Long, Int>()
        private val _sizeLiveData = MutableLiveData(size)

        val size: Int get() = hashMap.size
        val sizeLiveData: LiveData<Int> = _sizeLiveData
        val isEmpty: Boolean get() = hashMap.isEmpty()

        fun contains(id: Long) = hashMap.contains(id)

        fun clear() {
            if (hashMap.isEmpty()) return
            val it = hashMap.iterator()
            while (it.hasNext()) {
                // If the id for the item at
                val entry = it.next()
                val id = entry.component1()
                val posCache = entry.component2()
                it.remove()
                val pos = if (adapter.getItemId(posCache) == id) posCache
                else findViewHolderForItemId(id).adapterPosition
                adapter.notifyItemChanged(pos, SelectionState.NotSelected)
            }
            _sizeLiveData.value = size
        }

        fun clearWithoutVisualUpdate(ids: LongArray) {
            for (id in ids) hashMap.remove(id)
            _sizeLiveData.value = size
        }

        fun add(pos: Int) {
            if (pos !in 0 until adapter.itemCount) return
            hashMap[adapter.getItemId(pos)] = pos
            adapter.notifyItemChanged(pos, SelectionState.Selected)
            _sizeLiveData.value = size
        }

        fun remove(pos: Int) {
            if (pos !in 0 until adapter.itemCount) return
            val id = adapter.getItemId(pos)
            if (!hashMap.contains(id)) return
            hashMap.remove(id)
            adapter.notifyItemChanged(pos, SelectionState.NotSelected)
            _sizeLiveData.value = size
        }

        fun toggle(pos: Int) {
            if (pos !in 0 until adapter.itemCount) return
            val id = adapter.getItemId(pos)
            val payload = if (hashMap.contains(id)) {
                hashMap.remove(id)
                SelectionState.NotSelected
            } else {
                hashMap[id] = pos
                SelectionState.Selected
            }
            adapter.notifyItemChanged(pos, payload)
            _sizeLiveData.value = size
        }

        fun allSelectedIds() = hashMap.keys.toLongArray()

        fun currentState() = hashMap.toList()

        fun restoreState(savedState: List<Pair<Long, Int>>) {
            hashMap.clear()
            for (pair in savedState) {
                hashMap[pair.first] = pair.second
                adapter.notifyItemChanged(pair.second, SelectionState.Selected)
            }
            _sizeLiveData.value = size
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            for (entry in hashMap)
                if (entry.component2() in positionStart until positionStart + itemCount)
                    hashMap.remove(entry.component1())
            // Since the items have been removed, no notifyItemChanged calls are necessary
            _sizeLiveData.value = size
        }
    }

    /** A RecyclerView utility that manages the expansion of a single RecyclerView item at a time.
     *
     *  RecyclerViewExpandedItem is intended to be incorporated into a RecyclerView
     *  via composition to help it manage the expanded or collapsed state of its
     *  items. To accomplish this, the RecyclerView's view holder must inherit the
     *  ExpandableViewHolder interface and implement the function onExpansionState-
     *  Changed to determine what will happen with the view upon its collapse or
     *  expansion.
     *
     *  The current expanded item and view holder can be queried using the property
     *  id. The property id will return null if there is no expanded item. The
     *  expanded item can be set by view holder instance with the set function, or
     *  can be set to null (no expanded item) by passing null to the set function. */
    inner class ExpandedItem : RecyclerView.AdapterDataObserver() {
        private var _expandedId: Long? = null
        val id get() = _expandedId
        private var expandedViewHolderCache: ExpandableViewHolder? = null

        fun set(
            newExpandedVh: ExpandableViewHolder?,
            animateCollapse: Boolean = true,
            animateExpand: Boolean = true
        ) {
            val newExpandedId = if (newExpandedVh == null) null
                                else adapter.getItemId(newExpandedVh.adapterPosition)
            if (newExpandedId == _expandedId) return

            val collapsingViewPos: Int?
            val expandingViewPos: Int?
            var heightChange = 0

            // Collapse old expanded view holder and record its position
            val expandedVhCache = expandedViewHolderCache
            if (expandedVhCache != null && adapter.getItemId(expandedVhCache.adapterPosition) == _expandedId) {
                    collapsingViewPos = expandedVhCache.adapterPosition
                    heightChange = expandedVhCache.onExpansionStateChanged(false, animateCollapse)
            }
            else collapsingViewPos = null

            // Update _expandedId, expand newly expanded view holder, and record its position
            _expandedId = newExpandedId
            this.expandedViewHolderCache = newExpandedVh
            if (newExpandedVh != null) {
                expandingViewPos = newExpandedVh.adapterPosition
                heightChange = newExpandedVh.onExpansionStateChanged(true, animateExpand)
            } else expandingViewPos = null

            // Calling onExpansionStateChanged on the collapsing and expanding view holders
            // above should take care of their collapsing/expanding, but now the views below
            // or in between need to be slid up or down accordingly.
            if (!animateCollapse && !animateExpand) return
            val layoutManager = this@SelectableExpandableRecyclerView.layoutManager ?: return
            // If the recycler view is scrolled down, the adapter binding position of the
            // first child in the layout will not be 0, and the start and end positions
            // (which are adapter binding positions, not layout positions) will need to
            // be offset by this value.
            val firstChild = layoutManager.getChildAt(0) ?: return
            val firstChildBindingPosition = getChildAdapterPosition(firstChild)

            val animSet = ViewPropertyAnimatorSet()
            val toBeTranslatedStart: Int
            val toBeTranslatedEnd: Int
            var translationDistance = heightChange.toFloat()

            if (collapsingViewPos == null && expandingViewPos != null) {
                // All views starting one below the one being expanding should be slid
                // down. heightChange should already be positive from the return value
                // of newExpandedVh.onExpansionStateChanged()
                toBeTranslatedStart = expandingViewPos + 1 - firstChildBindingPosition
                toBeTranslatedEnd = layoutManager.childCount - 1
            } else if (collapsingViewPos != null && expandingViewPos == null) {
                // All views starting one below the one being collapsed should be slid
                // up. heightChange should already be negative from the return value
                // of expandedViewHolderCache.onExpansionStateChanged
                toBeTranslatedStart = collapsingViewPos + 1 - firstChildBindingPosition
                toBeTranslatedEnd = layoutManager.childCount - 1
            } else if (collapsingViewPos == null || expandingViewPos == null) {
                throw IllegalStateException("collapsingViewPos and expandingViewPos should not both be null")
//            } else  {
//                if (collapsingViewPos < firstChildBindingPosition)
            } else if (expandingViewPos < collapsingViewPos) {
                // All views in between the expanding and collapsed views should be
                // slid down. heightChange should already be positive from the return
                // value of newExpandedVh.onExpansionStateChanged()
                toBeTranslatedStart = expandingViewPos + 1 - firstChildBindingPosition
                toBeTranslatedEnd = collapsingViewPos - 1 - firstChildBindingPosition
            } else {// collapsingViewPos < expandingViewPos
                // Same as above, except that since the collapsing view is on top all
                // of the views need to be slid up rather than down. Because height-
                // Change should be positive from the return value of newExpandedVh.-
                // onExpansionStateChanged(), it is reversed here.
                if (collapsingViewPos >= firstChildBindingPosition) {
                    toBeTranslatedStart = collapsingViewPos + 1 - firstChildBindingPosition
                    toBeTranslatedEnd = expandingViewPos - 1 - firstChildBindingPosition
                    translationDistance *= -1f
                } else {
                    // If the collapsing view is off of the top of the screen, the layout
                    // manager will not take into account its height change when it collapses.
                    // This causes the items to jump back to their pre-translation position
                    // after their animation. To prevent this the items below the newly
                    // expanded item are translated down, as if there was no collapsing view.
                    toBeTranslatedStart = expandingViewPos + 1 - firstChildBindingPosition
                    toBeTranslatedEnd = layoutManager.childCount - 1
                }
            }

            // In the case that there is both an expanding and collapsing view, the
            // expanding or collapsing view on bottom will also need to be a part
            // of the vertical translation. It is animated separately here from the
            // range of translated in between views because it should be expanding
            // or collapsing at the same time as this translation, and therefore
            // should not use a graphical layer.
            if (collapsingViewPos != null && expandingViewPos != null) {
                val endChild = layoutManager.getChildAt(toBeTranslatedEnd + 1)
                if (endChild != null) animSet.add(endChild.animate().setDuration(200).
                                                  translationY(translationDistance).
                                                  withEndAction{ endChild.translationY = 0f })
            }

            for (i in toBeTranslatedStart..toBeTranslatedEnd) {
                val child = layoutManager.getChildAt(i)
                if (child != null) animSet.add(child.animate().setDuration(200).withLayer().
                                               translationY(translationDistance).
                                               withEndAction { child.translationY = 0f })
            }
            animSet.start()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            for (pos in positionStart until positionStart + itemCount)
                if (adapter.getItemId(pos) == _expandedId)
                    _expandedId = null
        }
    }
}