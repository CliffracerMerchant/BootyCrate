/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView

/** A RecyclerView utility to keep track of a multi-selection in a RecyclerView.
 *
 *  RecyclerViewSelection is a utility class intended to be incorporated into a
 *  RecyclerView via composition that keeps track of a multi-selection of the
 *  RecyclerView items. It can be queried using its public function contains
 *  and its size and isEmpty properties. The RecyclerView.AdapterDataObserver.-
 *  onItemRangeRemoved override will cause selected items to be automatically
 *  removed from the selection when they are deleted.
 *
 *  The selection size is exposed via the sizeLiveData property to allow on
 *  selection size changed listeners to respond to a change in selection size.
 *
 *  The selection modifying functions add, remove, toggle, and clear will call
 *  the appropriate notifyItemChanged function on the adapter provided in the
 *  constructor so that the items' visual selected/not-selected statuses can be
 *  updated. To support partial binding, these notifyItemChanged calls use a
 *  SelectionState value as a payload to indicate the new state of the item.
 *
 *  In the case that items need to be removed, but do not need a visual change
 *  (e.g. when they are being deleted), the function removeIdsWithoutVisual-
 *  Update can accomplish this.*/
class RecyclerViewSelection(private val recyclerView: RecyclerView) :
    RecyclerView.AdapterDataObserver() {
    private val hashMap = HashMap<Long, Int>()
    private val _sizeLiveData = MutableLiveData(size)

    init { recyclerView.adapter?.registerAdapterDataObserver(this) }

    val size: Int get() = hashMap.size
    val sizeLiveData: LiveData<Int> = _sizeLiveData
    val isEmpty: Boolean get() = hashMap.isEmpty()

    fun contains(id: Long) = hashMap.contains(id)

    fun clear() {
        val adapter = assertValidAdapter()
        if (hashMap.isEmpty()) return
        val it = hashMap.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val id = entry.component1()
            val posCache = entry.component2()
            it.remove()
            val pos = if (posCache in 0 until adapter.itemCount &&
                          adapter.getItemId(posCache) == id) posCache
                      else recyclerView.findViewHolderForItemId(id).adapterPosition
            adapter.notifyItemChanged(pos, SelectionState.NotSelected)
        }
        _sizeLiveData.value = size
    }

    fun removeIdsWithoutVisualUpdate(ids: LongArray) {
        for (id in ids) hashMap.remove(id)
        _sizeLiveData.value = size
    }

    fun add(pos: Int) {
        val adapter = assertValidAdapter()
        if (pos !in 0 until adapter.itemCount) return
        hashMap[adapter.getItemId(pos)] = pos
        adapter.notifyItemChanged(pos, SelectionState.Selected)
        _sizeLiveData.value = size
    }

    fun remove(pos: Int) {
        val adapter = assertValidAdapter()
        if (pos !in 0 until adapter.itemCount) return
        val id = adapter.getItemId(pos)
        if (!hashMap.contains(id)) return
        hashMap.remove(id)
        adapter.notifyItemChanged(pos, SelectionState.NotSelected)
        _sizeLiveData.value = size
    }

    fun toggle(pos: Int) {
        val adapter = assertValidAdapter()
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

    fun currentState() = Pair(hashMap.keys.toLongArray(), hashMap.values.toIntArray())

    fun restoreState(savedState: Pair<LongArray, IntArray>) = restoreState(savedState.first, savedState.second)
    fun restoreState(savedStateIds: LongArray, savedStatePositions: IntArray) {
        val adapter = assertValidAdapter()
        hashMap.clear()
        for (i in savedStateIds.indices) {
            hashMap[savedStateIds[i]] = savedStatePositions[i]
            adapter.notifyItemChanged(savedStatePositions[i], SelectionState.Selected)
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

    private fun assertValidAdapter(): RecyclerView.Adapter<*> {
        val adapter = recyclerView.adapter
        if (adapter == null || !adapter.hasStableIds())
            throw IllegalStateException("The recycler view passed to RecyclerViewSelection" +
                                        "must have an adapter that uses stable IDs.")
        return adapter
    }
}