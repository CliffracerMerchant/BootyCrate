/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.util.TypedValue
import androidx.core.animation.doOnEnd
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView

/** A RecyclerView utility to keep track of a multi-selection in a RecyclerView.
 *
 *  RecyclerViewSelection is a utility class intended to be incorporated into a
 *  RecyclerView via composition that keeps track of a multi-selection of the
 *  RecyclerView items. It can be queried using its public function contains(
 *  pos: Int) and its size and isEmpty properties. The RecyclerView.Adapter-
 *  DataObserver.onItemRangeRemoved override will cause selected items to be
 *  automatically removed from the selection when they are deleted.
 *
 *  The selection size is exposed via the sizeLiveData property to allow on
 *  selection size changed listeners to respond to a change in selection size
 *  (e.g. to start an ActionMode instance).
 *
 *  The selection modifying functions add, remove, toggle, and clear will call
 *  the appropriate notifyItemChanged function on the adapter provided in the
 *  constructor so that the items' visual selected/not-selected statuses can be
 *  updated. To support partial binding, these notifyItemChanged calls use a
 *  RecyclerViewSelection.State value as a payload to indicate the new state of
 *  the item.
 *
 *  To make it easier to manage the visual state of the item views, the public
 *  function updateVisualState is provided. Passing it a view holder will
 *  update the view holder with a background color appropriate for it selection
 *  state. */
class RecyclerViewSelection(context: Context, private val adapter: RecyclerView.Adapter<*>) :
        RecyclerView.AdapterDataObserver() {
    private val hashSet = HashSet<Int>()
    private val _sizeLiveData = MutableLiveData(size)
    private val selectedColor: Int

    val size: Int get() = hashSet.size
    val sizeLiveData: LiveData<Int> = _sizeLiveData
    val isEmpty: Boolean get() = hashSet.isEmpty()

    enum class State { Selected, NotSelected }

    init {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
        selectedColor = typedValue.data
    }

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
        adapter.notifyItemChanged(pos, State.Selected)
        _sizeLiveData.value = size
    }

    fun remove(pos: Int) {
        if (pos !in 0 until adapter.itemCount) return
        hashSet.remove(pos)
        adapter.notifyItemChanged(pos, State.NotSelected)
        _sizeLiveData.value = size
    }

    fun toggle(pos: Int) {
        if (pos !in 0 until adapter.itemCount) return
        val payload = if (hashSet.contains(pos)) {
            hashSet.remove(pos)
            State.NotSelected
        } else {
            hashSet.add(pos)
            State.Selected
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
                adapter.notifyItemChanged(pos, State.Selected)
        }
        _sizeLiveData.value = size
    }
    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        for (pos in positionStart until positionStart + itemCount)
            hashSet.remove(pos)
        _sizeLiveData.value = size
    }

    fun updateVisualState(holder: RecyclerView.ViewHolder, animate: Boolean = true) {
        if (animate) {
            val selected = hashSet.contains(holder.adapterPosition)
            val startColor = if (selected) 0 else selectedColor
            val endColor =   if (selected) selectedColor else 0
            val anim = ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
                                             startColor, endColor)
            if (!selected) anim.doOnEnd { holder.itemView.background = null }
            anim.start()
        } else if (hashSet.contains(holder.adapterPosition))
                   holder.itemView.setBackgroundColor(selectedColor)
               else holder.itemView.background = null
    }

//    /** A RecyclerView.Adapter with overrides for managing item views' selected/not selected state.
//     *
//     *  SelectionEnabledAdapter can be used as the adapter type in a Recycler-
//     *  View that contains a RecyclerViewSelection member to simplify Recycler-
//     *  ViewSelection's usage. Using this custom adapter will prevent the need
//     *  to pass the adapter in to RecyclerViewSelection's constructor and to
//     *  manually make calls to updateVisualState to handle selection changes.
//     *
//     *  SelectionEnabledAdapter is not used within BootyCrate due to Kotlin's
//     *  lack of multiple inheritance making it difficult to reconcile two diff-
//     *  erent inheritance hierarchies of the same base class. */
//    abstract class SelectionEnabledAdapter<VHType: RecyclerView.ViewHolder> :
//            RecyclerView.Adapter<VHType>() {
//
//        override fun onBindViewHolder(holder: VHType, position: Int, payloads: MutableList<Any>) {
//            val unhandledChanges = mutableListOf<Any>()
//            for (payload in payloads) {
//                if (payload is State) {
//                    val state = payloads[i] as State
//                    val startColor = if (state == State.Selected)    0 else selectedColor
//                    val endColor =   if (state == State.NotSelected) selectedColor else 0
//                    ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
//                                          startColor, endColor).start()
//                    payloads.removeAt(i)
//                } else unhandledChanges.add(payload)
//            }
//            if (unhandledChanges.isNotEmpty())
//                super.onBindViewHolder(holder, position, unhandledChanges)
//        }
//        override fun onBindViewHolder(holder: VHType, position: Int) {
//            if (hashSet.contains(position))
//                holder.itemView.setBackgroundColor(selectedColor)
//            else holder.itemView.background = null
//        }
//    }
}
