/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*
import java.util.*

/** A RecyclerView to display the data provided by an InventoryViewModel.
 *
 *  InventoryRecyclerView is a RecyclerView subclass specialized for displaying
 *  the contents of an inventory. Several of InventoryRecyclerView's necessary
 *  fields can not be obtained when it is inflated from XML, such as its view-
 *  models. To finish initialization with these required members, the function
 *  finishInit MUST be called during runtime, but before any sort of data
 *  access is attempted. The activity's FragmentManager is also required in
 *  this finish init function to use as a dependency for the color edit popup.
 *
 *  Adding or removing inventory items is accomplished using the ViewModel-
 *  RecyclerView functions and the new function addNewItem. InventoryRecycler-
 *  View also provides a new function, addItemsToShoppingList, to add new shop-
 *  ping list items from existing inventory items. */
class InventoryRecyclerView(context: Context, attrs: AttributeSet) :
        BootyCrateRecyclerView<InventoryItem>(context, attrs) {
    override val diffUtilCallback = InventoryItemDiffUtilCallback()
    override val adapter = InventoryAdapter()
    private lateinit var inventoryViewModel: InventoryViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var fragmentManager: FragmentManager

    fun finishInit(
        owner: LifecycleOwner,
        inventoryViewModel: InventoryViewModel,
        shoppingListViewModel: ShoppingListViewModel,
        fragmentManager: FragmentManager,
        initialSort: BootyCrateItem.Sort? = null
    ) {
        this.inventoryViewModel = inventoryViewModel
        this.shoppingListViewModel = shoppingListViewModel
        this.fragmentManager = fragmentManager
        finishInit(owner, inventoryViewModel, initialSort)
        initSelection()
    }

    fun addNewItem() = newInventoryItemDialog(context, fragmentManager) { newItem ->
        if (newItem != null) inventoryViewModel.insert(newItem)
    }

    fun addItemsToShoppingList(ids: LongArray) {
        shoppingListViewModel.insertFromInventoryItems(ids)
    }

    fun checkAutoAddToShoppingList(position: Int) {
        val item = adapter.currentList[position]
        if (!item.autoAddToShoppingList) return
        if (item.amount < item.autoAddToShoppingListTrigger) {
            val minAmount = item.autoAddToShoppingListTrigger - item.amount
            shoppingListViewModel.autoAddFromInventoryItem(item.id, minAmount)
        }
    }

    /** A RecyclerView.Adapter to display the contents of a list of inventory items.
     *
     *  InventoryAdapter is a subclass of BootyCrateAdapter using InventoryItem-
     *  InventoryItemViewHolder instances to represent inventory items. Its
     *  overrides of onBindViewHolder make use of the InventoryItem.Field val-
     *  ues passed by InventoryItemDiffUtilCallback to support partial binding. */
    inner class InventoryAdapter : BootyCrateAdapter<InventoryItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :
                InventoryItemViewHolder {
            val view = InventoryItemView(context)
            view.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                             ViewGroup.LayoutParams.WRAP_CONTENT)
            return InventoryItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: InventoryItemViewHolder, position: Int) {
            holder.view.update(holder.item, isExpanded = getItemId(position) == expandedItem.id)
            super.onBindViewHolder(holder, position)
        }

        override fun onBindViewHolder(
            holder: InventoryItemViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.size == 0)
                return onBindViewHolder(holder, position)
            val unhandledChanges = mutableListOf<Any>()
            for (payload in payloads) {
                if (payload is EnumSet<*>) {
                    val item = getItem(position)
                    val changes = payload as EnumSet<InventoryItem.Field>
                    if (changes.contains(InventoryItem.Field.Name))
                        holder.view.nameEdit.setText(item.name)
                    if (changes.contains(InventoryItem.Field.Amount)) {
                        holder.view.inventoryAmountEdit.currentValue = item.amount
                        checkAutoAddToShoppingList(holder.adapterPosition) }
                    if (changes.contains(InventoryItem.Field.ExtraInfo))
                        holder.view.extraInfoEdit.setText(item.extraInfo)
                    if (changes.contains(InventoryItem.Field.AutoAddToShoppingList)) {
                        holder.view.autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
                        checkAutoAddToShoppingList(holder.adapterPosition) }
                    if (changes.contains(InventoryItem.Field.AutoAddToShoppingListTrigger)) {
                        holder.view.autoAddToShoppingListTriggerEdit.currentValue = item.autoAddToShoppingListTrigger
                        checkAutoAddToShoppingList(holder.adapterPosition) }
                    if (changes.contains(InventoryItem.Field.Color)) {
                        val colorEditBg = holder.view.colorEdit.background as ColoredCircleDrawable
                        val startColor = colorEditBg.color
                        val endColor = BootyCrateItem.Colors[item.color]
                        ObjectAnimator.ofArgb(colorEditBg, "color", startColor, endColor).start()
                    }
                } else unhandledChanges.add(payload)
            }
            if (unhandledChanges.isNotEmpty())
                super.onBindViewHolder(holder, position, unhandledChanges)
        }
    }

    /** A BootyCrateViewHolder that wraps an instance of InventoryItemView.
     *
     *  InventoryItemViewHolder is a subclass of BootyCrateViewHolder that
     *  holds an instance of InventoryItemView to display the data for an Inven-
     *  toryItem. Besides its use of this custom item view, its differences
     *  from BootyCrateViewHolder are:
     * - It sets the on click listeners of each of the sub views in the Inven-
     *   toryItemView to permit the user to select/deselect items, and to edit
     *   the displayed data when allowed.
     * - Its override of the expand details button's onClickListener calls the
     *   RecyclerViewExpandableItem.set function on itself. Its override of
     *   ExpandableViewHolder.onExpansionStateChanged calls the corresponding
     *   expand or collapse functions on its InventoryItemView instance. */
    inner class InventoryItemViewHolder(val view: InventoryItemView) : BootyCrateViewHolder(view) {

        init {
            // Click & long click listeners
            val onClick = OnClickListener {
                if (!selection.isEmpty) selection.toggle(adapterPosition)
            }
            view.setOnClickListener(onClick)
            view.nameEdit.setOnClickListener(onClick)
            view.inventoryAmountEdit.valueEdit.setOnClickListener(onClick)
            view.extraInfoEdit.setOnClickListener(onClick)

            val onLongClick = OnLongClickListener {
                selection.toggle(adapterPosition); true
            }
            view.setOnLongClickListener(onLongClick)
            view.nameEdit.setOnLongClickListener(onLongClick)
            view.inventoryAmountEdit.valueEdit.setOnLongClickListener(onLongClick)
            view.extraInfoEdit.setOnLongClickListener(onLongClick)

            view.colorEdit.setOnClickListener {
                colorPickerDialog(fragmentManager, item.color) { chosenColor ->
                    inventoryViewModel.updateColor(item.id, chosenColor) }
            }
            view.editButton.setOnClickListener { expandedItem.set(this) }
            view.collapseButton.setOnClickListener { expandedItem.set(null) }

            // Data change listeners
            view.nameEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                inventoryViewModel.updateName(item.id, value)
                shoppingListViewModel.updateNameFromLinkedInventoryItem(item.id, value)
            }
            view.inventoryAmountEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                inventoryViewModel.updateAmount(item.id, value)
            }
            view.extraInfoEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                inventoryViewModel.updateExtraInfo(item.id, value)
                shoppingListViewModel.updateExtraInfoFromLinkedInventoryItem(item.id, value)
            }
            view.autoAddToShoppingListCheckBox.setOnCheckedChangeListener { _, checked ->
                inventoryViewModel.updateAutoAddToShoppingList(item.id, checked)
            }
            view.autoAddToShoppingListTriggerEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                inventoryViewModel.updateAutoAddToShoppingListTrigger(item.id, value)
            }
        }
        override fun onExpansionStateChange(expanding: Boolean, animate: Boolean) {
            if (expanding) view.expand(animate)
            else           view.collapse(animate)
        }
    }

    /** Computes a diff between two inventory items.
     *
     *  InventoryItemDiffUtilCallback uses the ids of inventory items to determine
     *  if they are the same or not. If they are the same, changes are logged by
     *  setting the appropriate bit of an instance of EnumSet<InventoryItem.Field>.
     *  The change payload for modified items will then be the enum set containing
     *  all of the Fields that were changed. */
    class InventoryItemDiffUtilCallback : DiffUtil.ItemCallback<InventoryItem>() {
        private val listChanges = mutableMapOf<Long, EnumSet<InventoryItem.Field>>()
        private val itemChanges = EnumSet.noneOf(InventoryItem.Field::class.java)

        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryItem,
                                        newItem: InventoryItem): Boolean {
            itemChanges.clear()
            if (newItem.name != oldItem.name)
                itemChanges.add(InventoryItem.Field.Name)
            if (newItem.extraInfo != oldItem.extraInfo)
                itemChanges.add(InventoryItem.Field.ExtraInfo)
            if (newItem.color != oldItem.color)
                itemChanges.add(InventoryItem.Field.Color)
            if (newItem.amount != oldItem.amount)
                itemChanges.add(InventoryItem.Field.Amount)
            if (newItem.autoAddToShoppingList != oldItem.autoAddToShoppingList)
                itemChanges.add(InventoryItem.Field.AutoAddToShoppingList)
            if (newItem.autoAddToShoppingListTrigger != oldItem.autoAddToShoppingListTrigger)
                itemChanges.add(InventoryItem.Field.AutoAddToShoppingListTrigger)

            if (!itemChanges.isEmpty())
                listChanges[newItem.id] = EnumSet.copyOf(itemChanges)
            return itemChanges.isEmpty()
        }

        override fun getChangePayload(oldItem: InventoryItem, newItem: InventoryItem) =
            listChanges.remove(newItem.id)
    }
}