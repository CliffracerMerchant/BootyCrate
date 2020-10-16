/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.recyclerview.widget.LinearLayoutManager

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

    abstract override val adapter: SelectableItemAdapter<out ViewModelItemViewHolder>
    val selection = RecyclerViewSelection(this)
    val expandedItem = RecyclerViewExpandedItem(this)
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

    override fun deleteItems(ids: LongArray) {
        //TODO: Find out how to get an undeleted view to collapse to prevent visual bugs
        val expandedItemId = expandedItem.id
        if (expandedItemId != null && expandedItemId in ids)
            expandedItem.reset()
            //expandedItem.set(null, animateExpand = false, animateCollapse = false)
        super.deleteItems(ids)
        // Since the items are being removed, no visual deselection change is necessary
        selection.removeIdsWithoutVisualUpdate(ids)
    }



    /** A subclass of ViewModelAdapter that contains onBindViewHolder overrides to
     *  update the items visual selected / not selected state, and enforces the use
     *  of BootyCrateViewHolder. It does not implement onCreateViewHolder, and is
     *  therefore abstract.*/
    abstract inner class SelectableItemAdapter<VHType: ViewModelItemViewHolder> :
            ViewModelAdapter<VHType>() {

        override fun onBindViewHolder(holder: VHType, position: Int) {
            val isSelected = selection.contains(getItemId(position))
            holder.itemView.background.setTint(if (isSelected) selectedColor
                                               else itemNormalBackgroundColor)
        }

        override fun onBindViewHolder(holder: VHType, position: Int, payloads: MutableList<Any>) {
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
}