/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

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
import kotlinx.android.synthetic.main.inventory_item_layout.view.editButton
import kotlinx.android.synthetic.main.inventory_item_layout.view.extraInfoEdit
import kotlinx.android.synthetic.main.inventory_item_layout.view.nameEdit
import java.util.*

/** A RecyclerView to display the data provided by an InventoryViewModel.
 *
 *  InventoryRecyclerView is a ExpandableSelectableRecyclerView subclass spec-
 *  ialized for displaying the contents of an inventory. Several of Inventory-
 *  RecyclerView's necessary fields can not be obtained when it is inflated
 *  from XML, such as its viewmodels. To finish initialization with these
 *  required members, the function finishInit MUST be called during runtime,
 *  but before any sort of data access is attempted. InventoryRecyclerView's
 *  version of finishInit will call ExpandableSelectableRecyclerView's version
 *  to prevent the implementing activity or fragment from needing to call both.
 *
 *  Adding or removing inventory items is accomplished using the ViewModel-
 *  RecyclerView and ExpandableSelectableRecyclerView functions and the new
 *  function addNewItem. */
class InventoryRecyclerView(context: Context, attrs: AttributeSet) :
        ExpandableSelectableRecyclerView<InventoryItem>(context, attrs) {
    override val diffUtilCallback = InventoryItemDiffUtilCallback()
    override val adapter = InventoryAdapter()
    override val collectionNameResId = R.string.inventory_item_collection_name
    private lateinit var inventoryViewModel: InventoryViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var fragmentManager: FragmentManager

    fun finishInit(
        owner: LifecycleOwner,
        inventoryViewModel: InventoryViewModel,
        shoppingListViewModel: ShoppingListViewModel,
        fragmentManager: FragmentManager,
        initialSort: ViewModelItem.Sort? = null
    ) {
        this.inventoryViewModel = inventoryViewModel
        this.shoppingListViewModel = shoppingListViewModel
        this.fragmentManager = fragmentManager
        finishInit(owner, inventoryViewModel, initialSort)
    }

    fun addNewItem() = newInventoryItemDialog(context, fragmentManager) { newItem ->
        inventoryViewModel.add(newItem)
    }

    /** A RecyclerView.Adapter to display the contents of a list of inventory items.
     *
     *  InventoryAdapter is a subclass of BootyCrateAdapter using InventoryItem-
     *  InventoryItemViewHolder instances to represent inventory items. Its
     *  overrides of onBindViewHolder make use of the InventoryItem.Field val-
     *  ues passed by InventoryItemDiffUtilCallback to support partial binding. */
    inner class InventoryAdapter : ExpandableSelectableItemAdapter<InventoryItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : InventoryItemViewHolder {
            val view = InventoryItemView(context)
            return InventoryItemViewHolder(view)
        }

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

                    if (changes.contains(InventoryItem.Field.Name) &&
                        holder.view.nameEdit.text.toString() != item.name)
                            holder.view.nameEdit.setText(item.name)
                    if (changes.contains(InventoryItem.Field.ExtraInfo) &&
                        holder.view.extraInfoEdit.text.toString() != item.extraInfo)
                            holder.view.extraInfoEdit.setText(item.extraInfo)
                    if (changes.contains(InventoryItem.Field.Color)) {
                        val colorEditBg = holder.view.colorEdit.drawable
                        colorEditBg.setTint(ViewModelItem.Colors[item.color])
                    }
                    if (changes.contains(InventoryItem.Field.Amount) &&
                        holder.view.inventoryAmountEdit.currentValue != item.amount)
                            holder.view.inventoryAmountEdit.currentValue = item.amount
                    if (changes.contains(InventoryItem.Field.IsExpanded))
                        holder.view.setExpanded(item.isExpanded)
                    if (changes.contains(InventoryItem.Field.IsSelected))
                        holder.view.setSelectedState(item.isSelected)
                    if (changes.contains(InventoryItem.Field.AddToShoppingList) &&
                        holder.view.addToShoppingListCheckBox.isChecked != item.addToShoppingList)
                            holder.view.addToShoppingListCheckBox.isChecked = item.addToShoppingList
                    if (changes.contains(InventoryItem.Field.AddToShoppingListTrigger) &&
                        holder.view.addToShoppingListTriggerEdit.currentValue != item.addToShoppingListTrigger)
                            holder.view.addToShoppingListTriggerEdit.currentValue = item.addToShoppingListTrigger
                }
                else unhandledChanges.add(payload)
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
    inner class InventoryItemViewHolder(val view: InventoryItemView) :
        ExpandableSelectableItemViewHolder(view) {

        init {
            // Click & long click listeners
            val onClick = OnClickListener { if (!selection.isEmpty) selection.toggle(itemId) }
            view.setOnClickListener(onClick)
            view.nameEdit.setOnClickListener(onClick)
            view.extraInfoEdit.setOnClickListener(onClick)
            view.inventoryAmountEdit.valueEdit.setOnClickListener(onClick)

            val onLongClick = OnLongClickListener { selection.toggle(itemId); true }
            view.setOnLongClickListener(onLongClick)
            view.nameEdit.setOnLongClickListener(onLongClick)
            view.extraInfoEdit.setOnLongClickListener(onLongClick)
            view.inventoryAmountEdit.valueEdit.setOnLongClickListener(onLongClick)

            view.colorEdit.setOnClickListener {
                colorPickerDialog(fragmentManager, item.color) { chosenColor ->
                    inventoryViewModel.updateColor(item.id, chosenColor) }
            }
            view.editButton.setOnClickListener {
                if (!view.isExpanded) setExpandedItem(adapterPosition)
            }
            view.collapseButton.setOnClickListener { setExpandedItem(null) }

            // Data change listeners
            view.nameEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                inventoryViewModel.updateName(item.id, value)
            }
            view.extraInfoEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                inventoryViewModel.updateExtraInfo(item.id, value)
            }
            view.inventoryAmountEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                inventoryViewModel.updateAmount(item.id, value)
            }
            view.addToShoppingListCheckBox.setOnCheckedChangeListener { _, checked ->
                inventoryViewModel.updateAddToShoppingList(item.id, checked)
            }
            view.addToShoppingListTriggerEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                inventoryViewModel.updateAddToShoppingListTrigger(item.id, value)
            }
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
            if (newItem.name != oldItem.name)             itemChanges.add(InventoryItem.Field.Name)
            if (newItem.extraInfo != oldItem.extraInfo)   itemChanges.add(InventoryItem.Field.ExtraInfo)
            if (newItem.color != oldItem.color)           itemChanges.add(InventoryItem.Field.Color)
            if (newItem.amount != oldItem.amount)         itemChanges.add(InventoryItem.Field.Amount)
            if (newItem.isExpanded != oldItem.isExpanded) itemChanges.add(InventoryItem.Field.IsExpanded)
            if (newItem.isSelected != oldItem.isSelected) itemChanges.add(InventoryItem.Field.IsSelected)
            if (newItem.addToShoppingList != oldItem.addToShoppingList)
                itemChanges.add(InventoryItem.Field.AddToShoppingList)
            if (newItem.addToShoppingListTrigger != oldItem.addToShoppingListTrigger)
                itemChanges.add(InventoryItem.Field.AddToShoppingListTrigger)

            if (!itemChanges.isEmpty())
                listChanges[newItem.id] = EnumSet.copyOf(itemChanges)
            return itemChanges.isEmpty()
        }

        override fun getChangePayload(oldItem: InventoryItem, newItem: InventoryItem) =
            listChanges.remove(newItem.id)
    }
}