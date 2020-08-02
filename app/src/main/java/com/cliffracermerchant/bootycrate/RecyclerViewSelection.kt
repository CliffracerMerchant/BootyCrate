/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView

/** A wrapper around a HastSet<Int> to keep track of a multi-selection in a RecyclerView.
 *
 *  RecyclerViewSelection is a utility class that keeps track of a selection
 *  within a RecyclerView data set. It can be queried using its public function
 *  contains(pos: Int) and its size and isEmpty properties. The
 *  RecyclerView.AdapterDataObserver.onItemRangeRemoved override will cause
 *  selected items to be automatically removed from the selection when they are
 *  deleted.
 *
 *  The selection size is exposed via the sizeLiveData property to allow on
 *  selection size changed listeners to respond to a change in selection size
 *  (e.g. to start an ActionMode instance).
 *
 *  The selection modifying functions add, remove, toggle, and clear will call
 *  the appropriate notifyItemChanged function on the adapter provided in the
 *  constructor so that the items' visual selected/not-selected statuses can be
 *  updated. To support partial binding, either a selected payload or a desel-
 *  ected payload is also passed along in the notifyItemChanged calls. These
 *  values default to a simple boolean value indicating whether the item is
 *  selected or not, but can be customized via the properties selectedPayload
 *  and deselectedPayload. */
class RecyclerViewSelection(private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) :
        RecyclerView.AdapterDataObserver() {
    private val hashSet = HashSet<Int>()
    private val _sizeLiveData = MutableLiveData(size)

    val size: Int get() = hashSet.size
    val sizeLiveData: LiveData<Int> = _sizeLiveData
    val isEmpty: Boolean get() = hashSet.isEmpty()

    var selectedPayload: Any = true
    var deselectedPayload: Any = false

    init { adapter.registerAdapterDataObserver(this) }

    fun contains(pos: Int) = hashSet.contains(pos)

    fun clear() {
        if (hashSet.isEmpty()) return
        hashSet.clear()
        adapter.notifyDataSetChanged()
        _sizeLiveData.value = size
    }

    fun add(pos: Int) {
        if (pos !in 0 until adapter.itemCount) return
        hashSet.add(pos)
        adapter.notifyItemChanged(pos, selectedPayload)
        _sizeLiveData.value = size
    }

    fun remove(pos: Int) {
        if (pos !in 0 until adapter.itemCount) return
        hashSet.remove(pos)
        adapter.notifyItemChanged(pos, deselectedPayload)
        _sizeLiveData.value = size
    }

    fun toggle(pos: Int) {
        if (pos !in 0 until adapter.itemCount) return
        val payload = if (hashSet.contains(pos)) {
            hashSet.remove(pos)
            deselectedPayload
        } else {
            hashSet.add(pos)
            selectedPayload
        }
        adapter.notifyItemChanged(pos, payload)
        _sizeLiveData.value = size
    }

    fun currentState() = hashSet.toIntArray()

    fun restoreState(savedState: IntArray) {
        hashSet.clear()
        for (pos in savedState) hashSet.add(pos)
        for (pos in 0 until adapter.itemCount) {
            if (hashSet.contains(pos))
                adapter.notifyItemChanged(pos, selectedPayload)
        }
        _sizeLiveData.value = size
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        for (pos in positionStart until positionStart + itemCount)
            hashSet.remove(pos)
        _sizeLiveData.value = size
    }
}
