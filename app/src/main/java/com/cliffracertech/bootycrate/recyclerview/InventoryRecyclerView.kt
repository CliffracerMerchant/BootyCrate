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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.InventorySummary
import com.cliffracertech.bootycrate.database.InventoryViewModel
import com.cliffracertech.bootycrate.databinding.InventoryViewBinding
import com.cliffracertech.bootycrate.utils.deleteInventoryDialog
import com.cliffracertech.bootycrate.utils.dpToPixels
import com.cliffracertech.bootycrate.utils.inventoryNameDialog
import java.util.*

/**
 * A RecyclerView to display a list of all of the inventories exposed by an instance of InventoryViewModel.
 *
 * InventoryRecyclerView comes with an internal adapter type, InventoryListAdapter,
 * that will display a list of all of the user's inventories if provided with
 * submitList. InventoryListAdapter uses a custom DiffUtil.ItemCallback to
 * support full and partial binding of the InventoryView instances it uses to
 * display each InventorySummary in the submitted list. Subclasses may wish
 * to override InventoryListAdapter with their own in order to perform work on
 * each created InventoryView.
 *
 * InventoryRecyclerView does not set its own adapter, as it is assumed that
 * subclasses will override InventoryListAdapter with their own implementation.
 * It does set its layoutManager to a vertical LinearLayoutManager (due to the
 * stretched horizontal aspect ratio of InventoryViews making another layout
 * type impractical), and it provides its own item spacing decoration with
 * spacing equal to to the value of the dimension R.dimen.recycler_view_item_spacing,
 * or 0 if the dimension resource is not found.
 */
open class InventoryRecyclerView(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs)
{
    init {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        val spacing = try { resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing) }
                      catch(e: Resources.NotFoundException) { 0 }
        addItemDecoration(ItemSpacingDecoration(spacing))
    }

    protected open inner class InventoryListAdapter :
        ListAdapter<InventorySummary, InventoryViewHolder>(DiffUtilCallback())
    {
        init { setHasStableIds(true) }

        override fun getItemId(position: Int) = currentList[position].id

        override fun getItemCount() = currentList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            InventoryViewHolder(InventoryView(context))

        override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) =
            holder.view.update(getItem(position))

        override fun onBindViewHolder(holder: InventoryViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.size == 0)
                return onBindViewHolder(holder, position)

            for (payload in payloads) {
                val item = getItem(position)
                val ui = holder.view.ui
                @Suppress("UNCHECKED_CAST")
                val changes = payload as EnumSet<Field>

                if (changes.contains(Field.Name) && item.name != ui.nameView.text.toString())
                    ui.nameView.text = item.name
                if (changes.contains(Field.ShoppingListItemCount))
                    ui.shoppingListItemCountView.text = item.shoppingListItemCount.toString()
                if (changes.contains(Field.InventoryItemCount))
                    ui.inventoryItemCountView.text = item.inventoryItemCount.toString()
                if (changes.contains(Field.IsSelected))
                    holder.view.isSelected = item.isSelected
            }
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
 * An InventoryRecyclerView to display and allow for editing of all of the user's inventories.
 *
 * InventorySelector displays a list of all of the user's inventories, and
 * allows the user to edit their state. User taps will select a single
 * inventory when the database is in single-select mode, or select or deselect
 * inventories when it is in multi-select mode. InventorySelector implements a
 * popup menu that is displayed when the user taps the options button for an
 * inventory, and provides dialogs that allow for renaming or deleting of
 * inventories.
 *
 * In order to display the user's inventories, the function initViewModel
 * should be called after construction with an instance of InventoryViewModel
 * for it to observe and an instance of LifecycleOwner that matches the
 * InventorySelector's lifespan.
 */
class InventorySelector(context: Context, attrs: AttributeSet?) :
    InventoryRecyclerView(context, attrs)
{
    private val adapter = Adapter()
    private lateinit var viewModel: InventoryViewModel

    init { setAdapter(adapter) }

    fun initViewModel(viewModel: InventoryViewModel, lifecycleOwner: LifecycleOwner) {
        this.viewModel = viewModel
        viewModel.inventories.observe(lifecycleOwner) { adapter.submitList(it) }
    }

    val InventoryViewHolder.item: InventorySummary get() = adapter.currentList[adapterPosition]

    protected inner class Adapter: InventoryListAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            super.onCreateViewHolder(parent, viewType).apply {
                view.setOnClickListener { viewModel.updateIsSelected(item.id) }
                view.ui.optionsButton.setOnClickListener {
                    val menu = PopupMenu(context, view.ui.optionsButton)
                    menu.inflate(R.menu.inventory_options)
                    menu.setOnMenuItemClickListener {
                        if (it.itemId == R.id.renameInventoryButton)
                            inventoryNameDialog(context, item.name) { viewModel.updateName(item.id, it) }
                        else /*R.id.deleteInventoryButton*/
                            deleteInventoryDialog(context) { viewModel.delete(item.id) }
                        true
                    }
                    menu.show()
                }
            }
    }
}

/** An options menu that provides a toggle checkbox for multi-selecting
 * inventories, as well as a select all button. The view passed in to
 * InventorySelectionOptionsMenu's constructor will have its onClickListener
 * set to open the options menu instance, with the view as its anchor. The
 * companion object function openOnClickOf provides the same functionality
 * in a more idiomatic way. */
class InventorySelectionOptionsMenu(
    anchor: View,
    private val inventoryViewModel: InventoryViewModel,
) : PopupMenu(anchor.context, anchor) {

    private val multiSelectCheckBox: MenuItem
    private val selectAllButton: MenuItem

    init {
        inflate(R.menu.inventory_selector_options)
        multiSelectCheckBox = menu.findItem(R.id.multiSelectInventoriesSwitch)
        selectAllButton = menu.findItem(R.id.selectAllInventories)

        setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.multiSelectInventoriesSwitch -> {
                    multiSelectCheckBox.apply { isChecked = !isChecked }
                    inventoryViewModel.toggleMultiSelect()
                } R.id.selectAllInventories ->
                    inventoryViewModel.selectAll()
            }; true
        }

        anchor.setOnClickListener {
            multiSelectCheckBox.isChecked = inventoryViewModel.multiSelect.value
            selectAllButton.isEnabled = multiSelectCheckBox.isChecked
            show()
        }
    }

    companion object {
        fun openOnClickOf(anchor: View, inventoryViewModel: InventoryViewModel) =
            InventorySelectionOptionsMenu(anchor, inventoryViewModel)
    }
}

/**
 * An InventoryRecyclerView that allows the user to pick a single inventory from among all selected inventories.
 *
 * SelectedInventoryPicker, when initialized by calling initViewModel with an
 * instance of InventoryViewModel and an appropriate LifeCycleOwner, will
 * display all of the InventorySummaries exposed by the ViewModel that are also
 * selected (i.e. the Inventory member isSelected is true). The user may pick
 * from among these inventories by tapping on one. The currently chosen
 * inventory is visually indicated by setting the corresponding InventoryView's
 * isSelected property to true. The id of the currently chosen inventory, or
 * null if one has not been chosen yet, can be queried with the property
 * chosenInventoryId. If the user picks a new inventory, the member
 * onChosenInventoryIdChangedListener will be called if it is not null.
 */
class SelectedInventoryPicker(context: Context, attrs: AttributeSet) :
    InventoryRecyclerView(context, attrs)
{
    private val adapter = Adapter()

    private var chosenPosition: Int? = null
    val chosenInventoryId get() = adapter.currentList.getOrNull(chosenPosition ?: -1)?.id
    var onChosenInventoryIdChangedListener: ((Long?) -> Unit)? = null

    init { setAdapter(adapter) }

    fun initViewModel(viewModel: InventoryViewModel, lifecycleOwner: LifecycleOwner) =
        viewModel.selectedInventories.observe(lifecycleOwner) { adapter.submitList(it) }

    val InventoryViewHolder.item: InventorySummary get() = adapter.currentList[adapterPosition]

    private inner class Adapter: InventoryListAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            super.onCreateViewHolder(parent, viewType).apply {
                view.ui.optionsButton.layoutParams.width = resources.dpToPixels(12f).toInt()
                view.ui.optionsButton.isInvisible = true
                view.setOnClickListener {
                    if (chosenInventoryId == item.id) return@setOnClickListener

                    val selectedViewHolder = findViewHolderForAdapterPosition(chosenPosition ?: -1)
                                                                            as? InventoryViewHolder
                    selectedViewHolder?.view?.isSelected = false

                    view.isSelected = true
                    chosenPosition = adapterPosition
                    onChosenInventoryIdChangedListener?.invoke(chosenInventoryId)
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
 * state of the inventory is represented with a gradient outline, and is set
 * using the Android framework's View's isSelected property.
 */
class InventoryView(context: Context) : LinearLayout(context) {
    val ui = InventoryViewBinding.inflate(LayoutInflater.from(context), this)

    init {
        val inventoryIcon = ContextCompat.getDrawable(context, R.drawable.inventory_icon)
        val shoppingListIcon = ContextCompat.getDrawable(context, R.drawable.shopping_cart_icon)
        val iconPadding = resources.dpToPixels(3f).toInt()
        ui.shoppingListItemCountView.setCompoundDrawablesRelativeWithIntrinsicBounds(shoppingListIcon, null, null, null)
        ui.inventoryItemCountView.setCompoundDrawablesRelativeWithIntrinsicBounds(inventoryIcon, null, null, null)
        inventoryIcon?.bounds?.inset(iconPadding, iconPadding)
        shoppingListIcon?.bounds?.inset(iconPadding, iconPadding)

        background = ContextCompat.getDrawable(context, R.drawable.recycler_view_item)
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun update(inventory: InventorySummary) {
        ui.nameView.text = inventory.name
        ui.shoppingListItemCountView.text = inventory.shoppingListItemCount.toString()
        ui.inventoryItemCountView.text = inventory.inventoryItemCount.toString()
        isSelected = inventory.isSelected
    }
}