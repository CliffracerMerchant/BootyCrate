package com.cliffracermerchant.stuffcrate

import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView

//TODO: Persist selection through activity changes
/**     SelectionEnabledAdapter is a RecyclerView.Adapter that also keeps track
 *  of a selection within the data set. Its selection member (an instance of
 *  the inner class Selection is publicly visible, and can therefore be queried
 *  using its public function contains(pos: Int) and its size and isEmpty pro-
 *  perties. The selection size is also exposed via selection's sizeLiveData
 *  property to allow on selection size changed listeners to respond to a
 *  change in selection size (e.g. to start an ActionMode instance).
 *      The internal selection modifying functions add, remove, toggle, clear,
 *  and restore will call the appropriate notifyItemChanged function in this
 *  adapter so that the items' visual selected/not-selected statuses can be
 *  updated. To support partial binding, either a selected payload or a desel-
 *  ected payload is also passed along in the notifyItemChanged calls. These
 *  values default to a simple boolean value indicating whether the item is
 *  selected or not, but can be customized via the properties selectedPayload
 *  and deselectedPayload. */
abstract class SelectionEnabledAdapter<VH: RecyclerView.ViewHolder> :
        RecyclerView.Adapter<VH>() {

    val selection = Selection()
    var selectedPayload: Any? = true
    var deselectedPayload: Any? = false

    init { setHasStableIds(true) }

    inner class Selection internal constructor() {
        val hashSet = HashSet<Long>()
        val size: Int get() = hashSet.size
        val sizeLiveData = MutableLiveData(size)
        val isEmpty: Boolean get() = hashSet.isEmpty()

        internal constructor(savedState: HashSet<Long>) : this() {
            this.hashSet.retainAll(savedState)
        }

        fun contains(pos: Int) = hashSet.contains(getItemId(pos))

        internal fun clear() {
            if (hashSet.isEmpty()) return
            hashSet.clear()
            notifyDataSetChanged()
            sizeLiveData.value = size
        }

        internal fun add(pos: Int) {
            if (pos !in 0 until itemCount) return
            hashSet.add(getItemId(pos))
            notifyItemChanged(pos, selectedPayload)
            sizeLiveData.value = size
        }

        internal fun remove(pos: Int) {
            if (pos !in 0 until itemCount) return
            hashSet.remove(getItemId(pos))
            notifyItemChanged(pos, deselectedPayload)
            sizeLiveData.value = size
        }

        internal fun toggle(pos: Int) {
            if (pos !in 0 until itemCount) return
            val id = getItemId(pos)
            if (hashSet.contains(id)) hashSet.remove(id)
            else                      hashSet.add(id)
            val payload = if (hashSet.contains(id)) selectedPayload
                          else                      deselectedPayload
            notifyItemChanged(pos, payload)
            sizeLiveData.value = size
        }

        fun saveState() = hashSet

        fun restoreState(savedState: HashSet<Long>) = hashSet.retainAll(savedState)
    }
}