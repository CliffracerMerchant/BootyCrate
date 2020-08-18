/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** A ViewModelRecyclerView subclass that enables multi-selection and expansion of items.
 *
 *  SelectableExpandableRecyclerView extends ViewModelRecyclerView by incorpora-
 *  ting a Selection and RecyclerViewExpandedItem as members to facilitate
 *  multi-selection and expansion of the items it contains. SelectableExpand-
 *  ableRecyclerView also utilizes its own custom adapter, BootyCrateAdapter, and view holder, BootyCrateViewHolder.
 *
 *  Because users would likely want to view extra details of newly inserted
 *  items, BootyCrateRecyclerView overrides ViewModelRecyclerView's onNewItem-
 *  Insertion so that new items are automatically expanded. */
abstract class SelectableExpandableRecyclerView<Entity: BootyCrateItem>(
    context: Context,
    attrs: AttributeSet
) : ViewModelRecyclerView<Entity>(context, attrs) {
    abstract override val adapter: SelectableExpandableItemAdapter<out SelectableExpandableViewHolder>
    val selection = Selection()
    val expandedItem = ExpandedItem()
    private val selectedColor: Int
    private val itemNormalBackgroundColor: Int

    enum class SelectionState { Selected, NotSelected }

    init {
        addItemDecoration(ItemSpacingDecoration(context))
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
        selectedColor = typedValue.data
        context.theme.resolveAttribute(R.attr.recyclerViewItemColor, typedValue, true)
        itemNormalBackgroundColor = typedValue.data
        layoutManager = LinearLayoutManager(context)
        setHasFixedSize(true)
    }

    override fun onNewItemInsertion(item: Entity, vh: ViewHolder) {
        super.onNewItemInsertion(item, vh)
        expandedItem.set((vh as SelectableExpandableRecyclerView<Entity>.SelectableExpandableViewHolder),
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
    abstract inner class SelectableExpandableItemAdapter<VHType: SelectableExpandableViewHolder> :
            ViewModelAdapter<VHType>() {

        override fun onBindViewHolder(holder: VHType, position: Int) {
            super.onBindViewHolder(holder, position)
            val background = (holder.itemView as ViewGroup).getChildAt(0).background
            val isSelected = selection.contains(getItemId(position))
            background.setTint(if (isSelected) selectedColor
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
                    val background = (holder.itemView as ViewGroup).getChildAt(0).background
                    val selected = payload == SelectionState.Selected
                    val startColor = if (selected) itemNormalBackgroundColor else selectedColor
                    val endColor =   if (selected) selectedColor else itemNormalBackgroundColor
                    val anim = ValueAnimator.ofArgb(startColor, endColor)
                    anim.addUpdateListener { background.setTint(anim.animatedValue as Int) }
                    anim.start()
                }
                else unhandledChanges.add(payload)
            if (unhandledChanges.isNotEmpty())
                super.onBindViewHolder(holder, position, payloads)
        }
    }

    /** A ViewHolder subclass that provides a simplified way of obtaining the
     *  instance of the item that it represents through the property item, and
     *  enforces the use of the ExpandableViewHolder interface. It does not imple-
     *  ment ExpandableViewHolder's one abstract function, onExpansionStateChanged,
     *  and is therefore abstract. */
    abstract inner class SelectableExpandableViewHolder(view: View) : ViewHolder(view) {
        val item: Entity get() = adapter.currentList[adapterPosition]

        open fun onExpansionStateChanged(expanding: Boolean, animate: Boolean = true) { }
    }

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

    inner class ExpandedItem :
        RecyclerView.AdapterDataObserver() {
        private var _expandedId: Long? = null
        val id get() = _expandedId
        private var expandedViewHolderCache: SelectableExpandableViewHolder? = null

        fun set(
            newExpandedVh: SelectableExpandableViewHolder?,
            animateCollapse: Boolean = true,
            animateExpand: Boolean = true
        ) {
            val newExpandedId = if (newExpandedVh == null) null
            else adapter.getItemId(newExpandedVh.adapterPosition)
            if (newExpandedId == _expandedId) return
            val expandedVhCache = expandedViewHolderCache
            if (expandedVhCache != null && adapter.getItemId(expandedVhCache.adapterPosition) == _expandedId)
                expandedVhCache.onExpansionStateChanged(false, animateCollapse)
            _expandedId = newExpandedId
            expandedViewHolderCache = newExpandedVh
            newExpandedVh?.onExpansionStateChanged(true, animateExpand)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            for (pos in positionStart until positionStart + itemCount)
                if (adapter.getItemId(pos) == _expandedId)
                    _expandedId = null
        }
    }
}