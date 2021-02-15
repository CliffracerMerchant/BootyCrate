/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import kotlinx.android.synthetic.main.integer_edit.view.*
import java.util.*

/** A RecyclerView to display the data provided by an InventoryViewModel.
 *
 *  InventoryRecyclerView is a ExpandableSelectableRecyclerView subclass spec-
 *  ialized for displaying the contents of an inventory. Several of Inventory-
 *  RecyclerView's necessary fields can not be obtained when it is inflated
 *  from XML, such as its view models. To finish initialization with these
 *  required members, the function finishInit must be called during runtime,
 *  but before any sort of data access is attempted. InventoryRecyclerView's
 *  version of finishInit will call ExpandableSelectableRecyclerView's version
 *  to prevent the implementing activity or fragment from needing to call both. */
class InventoryRecyclerView(context: Context, attrs: AttributeSet) :
        ExpandableSelectableRecyclerView<InventoryItem>(context, attrs) {
    override val diffUtilCallback = InventoryItemDiffUtilCallback()
    override val adapter = InventoryAdapter()
    override val collectionName = context.getString(R.string.inventory_item_collection_name)
    override lateinit var viewModel: InventoryViewModel
    lateinit var shoppingListViewModel: ShoppingListViewModel

    fun finishInit(
        owner: LifecycleOwner,
        viewModel: InventoryViewModel,
        shoppingListViewModel: ShoppingListViewModel
    ) {
        this.viewModel = viewModel
        this.shoppingListViewModel = shoppingListViewModel
        finishInit(owner)
    }

    /** A RecyclerView.Adapter to display the contents of a list of inventory items.
     *
     *  InventoryAdapter is a subclass of ExpandableSelectableItemAdapter using
     *  InventoryItemViewHolder instances to represent inventory items. Its over-
     *  rides of onBindViewHolder make use of the InventoryItem.Field values passed
     *  by InventoryItemDiffUtilCallback to support partial binding. Note that
     *  InventoryAdapter assumes that any payloads passed to it are of the type
     *  EnumSet<InventoryItem.Field>. If a payload of another type is passed to it,
     *  an exception will be thrown. */
    @Suppress("UNCHECKED_CAST")
    inner class InventoryAdapter : ExpandableSelectableItemAdapter<InventoryItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            InventoryItemViewHolder(InventoryItemView(context))

        override fun onBindViewHolder(holder: InventoryItemViewHolder, position: Int) {
            holder.view.update(holder.item)
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
                    val ui = holder.view.ui
                    val detailsUi = holder.view.detailsUi

                    if (changes.contains(InventoryItem.Field.Name) &&
                        ui.nameEdit.text.toString() != item.name)
                            ui.nameEdit.setText(item.name)
                    if (changes.contains(InventoryItem.Field.ExtraInfo) &&
                        ui.extraInfoEdit.text.toString() != item.extraInfo)
                            ui.extraInfoEdit.setText(item.extraInfo)
                    if (changes.contains(InventoryItem.Field.Color) &&
                        ui.checkBox.colorIndex != item.color)
                            ui.checkBox.colorIndex = item.color
                    if (changes.contains(InventoryItem.Field.Amount) &&
                        ui.amountEdit.value != item.amount)
                            ui.amountEdit.value = item.amount
                    if (changes.contains(InventoryItem.Field.IsExpanded))
                        holder.view.setExpanded(item.isExpanded)
                    if (changes.contains(InventoryItem.Field.IsSelected))
                        holder.view.setSelectedState(item.isSelected)
                    if (changes.contains(InventoryItem.Field.AddToShoppingList) &&
                        detailsUi.addToShoppingListCheckBox.isChecked != item.addToShoppingList)
                            detailsUi.addToShoppingListCheckBox.isChecked = item.addToShoppingList
                    if (changes.contains(InventoryItem.Field.AddToShoppingListTrigger) &&
                        detailsUi.addToShoppingListTriggerEdit.value != item.addToShoppingListTrigger)
                            detailsUi.addToShoppingListTriggerEdit.value = item.addToShoppingListTrigger
                }
                else unhandledChanges.add(payload)
            }
            if (unhandledChanges.isNotEmpty())
                super.onBindViewHolder(holder, position, unhandledChanges)
        }
    }

    /** A ExpandableSelectableItemViewHolder that wraps an instance of InventoryItemView.
     *
     *  InventoryItemViewHolder is a subclass of ExpandableSelectableItemViewHolder
     *  that holds an instance of InventoryItemView to display the data for an
     *  InventoryItem. It also connects changes in the InventoryItemView extra
     *  details section to view model update calls. */
    inner class InventoryItemViewHolder(val view: InventoryItemView) :
        ExpandableSelectableItemViewHolder(view) {

        init {
            view.detailsUi.addToShoppingListCheckBox.setOnCheckedChangeListener { _, checked ->
                viewModel.updateAddToShoppingList(item.id, checked)
            }
            view.detailsUi.addToShoppingListTriggerEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                viewModel.updateAddToShoppingListTrigger(item.id, value)
            }
        }
    }

    /** Computes a diff between two inventory item lists.
     *
     *  InventoryItemDiffUtilCallback uses the ids of inventory items to determine
     *  if they are the same or not. If they are the same, changes are logged by
     *  setting the appropriate bit of an instance of EnumSet<InventoryItem.Field>.
     *  The change payload for modified items will then be the enum set containing
     *  all of the fields that were changed. */
    class InventoryItemDiffUtilCallback : DiffUtil.ItemCallback<InventoryItem>() {
        private val listChanges = mutableMapOf<Long, EnumSet<InventoryItem.Field>>()
        private val itemChanges = EnumSet.noneOf(InventoryItem.Field::class.java)

        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
            itemChanges.apply {
                clear()
                if (newItem.name != oldItem.name)             add(InventoryItem.Field.Name)
                if (newItem.extraInfo != oldItem.extraInfo)   add(InventoryItem.Field.ExtraInfo)
                if (newItem.color != oldItem.color)           add(InventoryItem.Field.Color)
                if (newItem.amount != oldItem.amount)         add(InventoryItem.Field.Amount)
                if (newItem.isExpanded != oldItem.isExpanded) add(InventoryItem.Field.IsExpanded)
                if (newItem.isSelected != oldItem.isSelected) add(InventoryItem.Field.IsSelected)
                if (newItem.addToShoppingList != oldItem.addToShoppingList)
                    add(InventoryItem.Field.AddToShoppingList)
                if (newItem.addToShoppingListTrigger != oldItem.addToShoppingListTrigger)
                    add(InventoryItem.Field.AddToShoppingListTrigger)

                if (!isEmpty())
                    listChanges[newItem.id] = EnumSet.copyOf(this)
            }.isEmpty()

        override fun getChangePayload(oldItem: InventoryItem, newItem: InventoryItem) =
            listChanges.remove(newItem.id)
    }
}