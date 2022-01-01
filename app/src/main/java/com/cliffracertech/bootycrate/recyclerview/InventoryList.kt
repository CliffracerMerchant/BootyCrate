/*
 * Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.InventorySummary
import com.cliffracertech.bootycrate.databinding.InventoryViewBinding
import com.cliffracertech.bootycrate.utils.deleteInventoryDialog
import com.cliffracertech.bootycrate.utils.dpToPixels
import com.cliffracertech.bootycrate.utils.inventoryNameDialog
import java.util.*

/**
 * A view to display a list of InventorySummary instances.
 *
 * The list of InventorySummary instances that will be displayed is set through
 * the function submitList. InventoryList's adapter type uses a custom
 * DiffUtil.ItemCallback to support full and partial binding of the InventoryView
 * instances it uses to display each InventorySummary in the submitted list,
 * but does not provide any onClickListeners or callbacks for item interactions.
 *
 * InventoryList does not set its own adapter, as it is assumed that
 * subclasses will override InventoryListAdapter with their own implementation.
 * It does set its layoutManager to a vertical LinearLayoutManager (due to the
 * stretched horizontal aspect ratio of InventoryViews making another layout
 * type impractical), and it provides its own item spacing decoration with
 * spacing equal to to the value of the dimension R.dimen.recycler_view_item_spacing,
 * or 0 if the dimension resource is not found.
 */
open class InventoryList(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs)
{
    protected open val listAdapter: InventoryListAdapter = InventoryListAdapter()

    init {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        val spacing = try { resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing) }
                      catch(e: Resources.NotFoundException) { 0 }
        addItemDecoration(ItemSpacingDecoration(spacing))
    }

    fun submitList(list: List<InventorySummary>) = listAdapter.submitList(list)

    final override fun setAdapter(adapter: Adapter<*>?) {
        if (this.adapter == null && adapter is InventoryListAdapter)
            super.setAdapter(adapter)
    }

    /** A ListAdapter to display a list of InventorySummary instances.
     *
     * InventoryListAdapter enforces the use of stable ids for its items,
     * enforces the use of the InventoryViewHolder as its view holder type, and
     * provides implementations for all of ListAdapter's abstract methods.
     * InventoryListAdapter is open to allow for subclasses that wish to
     * provide further customization in, e.g., onCreateViewHolder.*/
    protected open inner class InventoryListAdapter :
        ListAdapter<InventorySummary, InventoryViewHolder>(DiffUtilCallback())
    {
        init { setHasStableIds(true) }

        final override fun setHasStableIds(a: Boolean) = super.setHasStableIds(true)

        override fun getItemId(position: Int) = currentList[position].id

        override fun getItemCount() = currentList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
            val view = InventoryView(context)
            view.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                             ViewGroup.LayoutParams.WRAP_CONTENT)
            return InventoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) =
            holder.view.update(getItem(position))

        override fun onBindViewHolder(holder: InventoryViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.size == 0)
                return onBindViewHolder(holder, position)

            val item = getItem(position)
            payloads.forEach { try {
                @Suppress("unchecked_cast")
                val changes = it as EnumSet<Field>
                if (changes.contains(Field.Name))
                    holder.view.ui.nameView.text = item.name
                if (changes.contains(Field.ShoppingListItemCount))
                    holder.view.ui.shoppingListItemCountView.text = item.shoppingListItemCount.toString()
                if (changes.contains(Field.InventoryItemCount))
                    holder.view.ui.inventoryItemCountView.text = item.inventoryItemCount.toString()
                if (changes.contains(Field.IsSelected))
                    holder.view.isSelected = item.isSelected
            } catch(e: ClassCastException) { } }
        }
    }

    inner class InventoryViewHolder(view: InventoryView) : RecyclerView.ViewHolder(view) {
        val view get() = itemView as InventoryView
    }

    protected enum class Field { Name, ShoppingListItemCount, InventoryItemCount, IsSelected }

    private class DiffUtilCallback : DiffUtil.ItemCallback<InventorySummary>() {
        private val listChanges = mutableMapOf<Long, EnumSet<Field>>()
        private val itemChanges = EnumSet.noneOf(Field::class.java)

        override fun areItemsTheSame(oldItem: InventorySummary, newItem: InventorySummary) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventorySummary, newItem: InventorySummary) =
            itemChanges.apply {
                clear()
                if (newItem.name != oldItem.name)
                    add(Field.Name)
                if (newItem.shoppingListItemCount != oldItem.shoppingListItemCount)
                    add(Field.ShoppingListItemCount)
                if (newItem.inventoryItemCount != oldItem.inventoryItemCount)
                    add(Field.InventoryItemCount)
                if (newItem.isSelected != oldItem.isSelected)
                    add(Field.IsSelected)

                if (!isEmpty())
                    listChanges[newItem.id] = EnumSet.copyOf(this)
            }.isEmpty()

        override fun getChangePayload(oldItem: InventorySummary, newItem: InventorySummary) =
            listChanges.remove(newItem.id)
    }
}

/**
 * An InventoryList to display and allow for editing of a list of InventorySummary instances.
 *
 * InventorySelector displays the list of InventorySummary instances submitted
 * through InventoriesList.submitList, and provides modifiable callback members
 * that can be used to respond to item UI interaction.
 *
 * InventorySelector implements a popup menu that is displayed when the user
 * taps the options button for an inventory, and provides menu items that open
 * dialogs to allow renaming or deleting of items.
 *
 * Clicks on an inventory will trigger InventorySelector's onItemClick callback
 * if not null, while confirming the rename or delete dialogs will call
 * onItemRenameRequest or onItemDeletionRequest, respectively, if they are not
 * null.
 */
class InventorySelector(context: Context, attrs: AttributeSet?) :
    InventoryList(context, attrs)
{
    override val listAdapter = Adapter()

    var onItemClick: ((Long) -> Unit)? = null
    var onItemRenameRequest: ((Long, String) -> Unit)? = null
    var onItemDeletionRequest: ((Long) -> Unit)? = null

    init { adapter = listAdapter }

    val InventoryViewHolder.item: InventorySummary get() =
        listAdapter.currentList[adapterPosition]

    protected inner class Adapter: InventoryListAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            super.onCreateViewHolder(parent, viewType).apply {
                view.setOnClickListener { onItemClick?.invoke(item.id) }
                view.ui.optionsButton.setOnClickListener {
                    val menu = PopupMenu(context, view.ui.optionsButton)
                    menu.inflate(R.menu.inventory_options)
                    menu.setOnMenuItemClickListener {
                        if (it.itemId == R.id.renameInventoryButton)
                            inventoryNameDialog(context, item.name) { newName ->
                                onItemRenameRequest?.invoke(item.id, newName)
                            }
                        else /*R.id.deleteInventoryButton*/
                            deleteInventoryDialog(context) {
                                onItemDeletionRequest?.invoke(item.id)
                            }
                        true
                    }
                    menu.show()
                }
            }
    }
}

/** An options menu that provides a toggle checkbox for multi-selecting
 * inventories, as well as a select all button.
 *
 * @param anchor: The view that the menu will be anchored to.
 * @param multiSelectInventories Whether or not the multi-select inventories checkbox will be checked.
 * @param onMultiSelectCheckboxClick The callback that will be invoked when the multi-select inventories checkbox is clicked.
 * @param onSelectAllClick The callback that will be invoked when the select all item is clicked. */
class InventorySelectorOptionsMenu(
    anchor: View,
    multiSelectInventories: Boolean,
    onMultiSelectCheckboxClick: () -> Unit,
    onSelectAllClick: () -> Unit,
) : PopupMenu(anchor.context, anchor) {

    private val multiSelectCheckBox: MenuItem
    private val selectAllButton: MenuItem

    init {
        inflate(R.menu.inventory_selector_options)
        multiSelectCheckBox = menu.findItem(R.id.multiSelectInventoriesSwitch)
        selectAllButton = menu.findItem(R.id.selectAllInventories)
        multiSelectCheckBox.isChecked = multiSelectInventories
        selectAllButton.isEnabled = multiSelectInventories

        setOnMenuItemClickListener {
            if (it.itemId == R.id.multiSelectInventoriesSwitch)
                onMultiSelectCheckboxClick()
            if (it.itemId == R.id.selectAllInventories)
                onSelectAllClick()
            true
        }
    }
}

/**
 * An InventoryList that allows the user to pick a single inventory from among the list of inventories.
 *
 * InventoryPicker, when provided with a list of InventorySummary instances
 * through InventoryList.submitList, will allow the user to pick from among
 * these inventories by tapping on one. The currently chosen inventory is
 * visually indicated by setting the corresponding InventoryView's isSelected
 * property to true. The id of the currently chosen inventory, or null if one
 * has not been chosen yet, can be queried with the property chosenInventoryId.
 * If the user picks a new inventory, the member onChosenInventoryIdChanged
 * will be called if it is not null.
 */
class InventoryPicker(context: Context, attrs: AttributeSet) : InventoryList(context, attrs) {

    override val listAdapter = Adapter()

    private var chosenPosition: Int? = null
    val chosenInventoryId get() = listAdapter.currentList.getOrNull(chosenPosition ?: -1)?.id
    var onChosenInventoryIdChanged: ((Long?) -> Unit)? = null

    init { adapter = listAdapter }

    val InventoryViewHolder.item: InventorySummary get() = listAdapter.currentList[adapterPosition]

    protected inner class Adapter: InventoryListAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            super.onCreateViewHolder(parent, viewType).apply {
                view.ui.optionsButton.layoutParams.width = resources.dpToPixels(12f).toInt()
                view.ui.optionsButton.isInvisible = true
                view.setOnClickListener {
                    if (chosenInventoryId == item.id) return@setOnClickListener

                    val selectedViewHolder = findViewHolderForAdapterPosition(chosenPosition ?: -1)
                    selectedViewHolder?.itemView?.isSelected = false

                    view.isSelected = true
                    chosenPosition = adapterPosition
                    onChosenInventoryIdChanged?.invoke(chosenInventoryId)
                }
            }

        override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
            super.onBindViewHolder(holder, position).also {
                val isSelected = holder.adapterPosition == chosenPosition
                holder.view.isSelected = isSelected
            }
        }
    }
}

/**
 * A view to represent an instance of InventorySummary.
 *
 * InventoryView displays the name, the number of shopping list items, the
 * number of inventory items, and an options button for the instance of
 * InventorySummary it is bound to using the function update. The selected
 * state of an inventory is set using the Android framework's View's isSelected
 * property.
 */
class InventoryView(context: Context) : LinearLayout(context) {
    val ui = InventoryViewBinding.inflate(LayoutInflater.from(context), this)

    init {
        val inventoryIcon = ContextCompat.getDrawable(context, R.drawable.inventory_icon)
        val shoppingListIcon = ContextCompat.getDrawable(context, R.drawable.shopping_cart_icon)
        val iconPadding = resources.dpToPixels(3f).toInt()
        ui.shoppingListItemCountView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            shoppingListIcon, null, null, null)
        ui.inventoryItemCountView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            inventoryIcon, null, null, null)
        inventoryIcon?.bounds?.inset(iconPadding, iconPadding)
        shoppingListIcon?.bounds?.inset(iconPadding, iconPadding)

        background = ContextCompat.getDrawable(context, R.drawable.recycler_view_item)
    }

    fun update(inventory: InventorySummary) {
        ui.nameView.text = inventory.name
        ui.shoppingListItemCountView.text = inventory.shoppingListItemCount.toString()
        ui.inventoryItemCountView.text = inventory.inventoryItemCount.toString()
        isSelected = inventory.isSelected
    }
}