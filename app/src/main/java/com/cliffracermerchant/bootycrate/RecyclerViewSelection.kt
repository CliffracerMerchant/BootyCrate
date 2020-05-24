package com.cliffracermerchant.bootycrate

import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView

/**     RecyclerViewSelection is a utility class that keeps track of a selec-
 *  tion within a recyclerview data set. It can be queried using its public
 *  function contains(pos: Int) and its size and isEmpty properties. The selec-
 *  tion size is also exposed via selection's sizeLiveData property to allow on
 *  selection size changed listeners to respond to a change in selection size
 *  (e.g. to start an ActionMode instance).
 *      The selection modifying functions add, remove, toggle, clear, and res-
 *  tore will call the appropriate notifyItemChanged function in this adapter
 *  so that the items' visual selected/not-selected statuses can be updated. To
 *  support partial binding, either a selected payload or a deselected payload
 *  is also passed along in the notifyItemChanged calls. These values default
 *  to a simple boolean value indicating whether the item is selected or not,
 *  but can be customized via the properties selectedPayload and
 *  deselectedPayload. */
class RecyclerViewSelection(private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) :
        RecyclerView.AdapterDataObserver() {
    private val hashSet = HashSet<Int>()

    val size: Int get() = hashSet.size
    val sizeLiveData = MutableLiveData(size)
    val isEmpty: Boolean get() = hashSet.isEmpty()

    var selectedPayload: Any? = true
    var deselectedPayload: Any? = false

    init { adapter.registerAdapterDataObserver(this) }

    fun contains(pos: Int) = hashSet.contains(pos)

    fun clear() {
        if (hashSet.isEmpty()) return
        hashSet.clear()
        adapter.notifyDataSetChanged()
        sizeLiveData.value = size
    }

    fun add(pos: Int) {
        if (pos !in 0 until adapter.itemCount) return
        hashSet.add(pos)
        adapter.notifyItemChanged(pos, selectedPayload)
        sizeLiveData.value = size
    }

    fun remove(pos: Int) {
        if (pos !in 0 until adapter.itemCount) return
        hashSet.remove(pos)
        adapter.notifyItemChanged(pos, deselectedPayload)
        sizeLiveData.value = size
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
        sizeLiveData.value = size
    }

    fun saveState() = hashSet.toIntArray()

    fun restoreState(savedState: IntArray) {
        hashSet.clear()
        for (pos in savedState) hashSet.add(pos)
        for (pos in 0 until adapter.itemCount) {
            if (hashSet.contains(pos))
                adapter.notifyItemChanged(pos, selectedPayload)
        }
        sizeLiveData.value = size
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        for (pos in positionStart until positionStart + itemCount)
            hashSet.remove(pos)
        sizeLiveData.value = size
    }
}