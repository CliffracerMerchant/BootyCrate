/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager

/** A ViewModelRecyclerView subclass that enables multi-selection and expansion of items.
 *
 *  BootyCrateRecyclerView extends ViewModelRecyclerView by incorporating a
 *  RecyclerViewSelection and RecyclerViewExpandedItem as members to facilitate
 *  multi-selection and expansion of the items it contains, and adds a zebra
 *  stripes style item decoration. BootyCrateRecyclerView also utilizes its own
 *  custom adapter, BootyCrateAdapter, and view holder, BootyCrateViewHolder.
 *
 *  Because users would likely want to view extra details of newly inserted
 *  items, BootyCrateRecyclerView overrides ViewModelRecyclerView's onNewItem-
 *  Insertion so that new items are automatically expanded. */
abstract class BootyCrateRecyclerView<Entity: BootyCrateItem>(
    context: Context,
    attrs: AttributeSet
) : ViewModelRecyclerView<Entity>(context, attrs) {
    abstract override val adapter: BootyCrateAdapter<out BootyCrateViewHolder>
    lateinit var selection: RecyclerViewSelection
    val expandedItem = RecyclerViewExpandedItem(this)

    init {
        layoutManager = LinearLayoutManager(context)
        setHasFixedSize(true)
        addItemDecoration(AlternatingRowBackgroundDecoration(context))
    }

    protected fun initSelection() { selection = RecyclerViewSelection(context, adapter) }

    override fun onNewItemInsertion(item: Entity, vh: ViewHolder) {
        super.onNewItemInsertion(item, vh)
        expandedItem.set(vh as RecyclerViewExpandedItem.ExpandableViewHolder,
                         animateCollapse = true, animateExpand = false)
    }

    /** A subclass of ViewModelAdapter that contains onBindViewHolder overrides
     *  to update the items visual selected / not selected state, and enforces
     *  the use of BootyCrateViewHolder. It does not implement onCreateView-
     *  Holder, and is therefore abstract.*/
    abstract inner class BootyCrateAdapter<VHType: BootyCrateViewHolder> :
            ViewModelAdapter<VHType>() {

        override fun onBindViewHolder(holder: VHType, position: Int) {
            super.onBindViewHolder(holder, position)
            selection.updateVisualState(holder, animate = false)
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
                if (payload is RecyclerViewSelection.State)
                    selection.updateVisualState(holder)
                else unhandledChanges.add(payload)
            if (unhandledChanges.isNotEmpty())
                super.onBindViewHolder(holder, position, payloads)
        }
    }

    /** A ViewHolder subclass that provides a simplified way of obtaining the
     *  instance of the item that it represents through the property item, and
     *  enforces the use of the ExpandableViewHolder interface. It does not
     *  implement ExpandableViewHolder's one abstract function, onExpansion-
     *  StateChanged, and is therefore abstract.*/
    abstract inner class BootyCrateViewHolder(view: View) :
            ViewHolder(view), RecyclerViewExpandedItem.ExpandableViewHolder {
        val item: Entity get() = adapter.currentList[adapterPosition]
    }
}