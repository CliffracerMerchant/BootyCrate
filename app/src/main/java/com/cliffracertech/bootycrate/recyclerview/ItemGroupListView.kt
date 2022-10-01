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
import com.cliffracertech.bootycrate.model.database.ItemGroup
import com.cliffracertech.bootycrate.databinding.ItemGroupBinding
import com.cliffracertech.bootycrate.dialog.deleteItemGroupDialog
import com.cliffracertech.bootycrate.dialog.itemGroupNameDialog
import com.cliffracertech.bootycrate.utils.dpToPixels
import java.util.*

/**
 * A view to display a list of ItemGroup instances.
 *
 * The list of ItemGroup instances that will be displayed is set through the
 * function submitList. ItemGroupListView's adapter type uses a custom
 * DiffUtil.ItemCallback to support full and partial binding of the ItemGroupView
 * instances it uses to display each ItemGroup in the submitted list, but does
 * not provide any onClickListeners or callbacks for item interactions.
 *
 * ItemGroupListView does not set its own adapter, as it is assumed that
 * subclasses will override ItemGroupListAdapter with their own implementation.
 * It does set its layoutManager to a vertical LinearLayoutManager (due to the
 * stretched horizontal aspect ratio of ItemGroupView's making another layout
 * type impractical), and it provides its own item spacing decoration with
 * spacing equal to to the value of the dimension R.dimen.recycler_view_item_spacing,
 * or 0 if the dimension resource is not found.
 */
open class ItemGroupListView(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs)
{
    protected open val listAdapter: ItemGroupListAdapter = ItemGroupListAdapter()

    init {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        val spacing = try { resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing) }
                      catch(e: Resources.NotFoundException) { 0 }
        addItemDecoration(ItemSpacingDecoration(spacing))
    }

    final override fun setHasFixedSize(hasFixedSize: Boolean) =
        super.setHasFixedSize(hasFixedSize)

    final override fun addItemDecoration(decor: ItemDecoration) =
        super.addItemDecoration(decor)

    fun submitList(list: List<ItemGroup>) = listAdapter.submitList(list)

    final override fun setAdapter(adapter: Adapter<*>?) {
        if (this.adapter == null && adapter is ItemGroupListAdapter)
            super.setAdapter(adapter)
    }

    /** A ListAdapter to display a list of ItemGroup instances.
     *
     * ItemGroupListAdapter enforces the use of stable ids for its items,
     * enforces the use of the ItemGroupViewHolder as its view holder type, and
     * provides implementations for all of ListAdapter's abstract methods.
     * ItemGroupListAdapter is open to allow for subclasses that wish to
     * provide further customization in, e.g., onCreateViewHolder.*/
    protected open inner class ItemGroupListAdapter :
        ListAdapter<ItemGroup, ItemGroupViewHolder>(DiffUtilCallback())
    {
        init { setHasStableIds(true) }

        final override fun setHasStableIds(a: Boolean) = super.setHasStableIds(true)

        override fun getItemId(position: Int) = currentList[position].id

        override fun getItemCount() = currentList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemGroupViewHolder {
            val view = ItemGroupView(context)
            view.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                             ViewGroup.LayoutParams.WRAP_CONTENT)
            return ItemGroupViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemGroupViewHolder, position: Int) =
            holder.view.update(getItem(position))

        override fun onBindViewHolder(holder: ItemGroupViewHolder, position: Int, payloads: MutableList<Any>) {
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

    inner class ItemGroupViewHolder(view: ItemGroupView) : RecyclerView.ViewHolder(view) {
        val view get() = itemView as ItemGroupView
    }

    protected enum class Field { Name, ShoppingListItemCount, InventoryItemCount, IsSelected }

    private class DiffUtilCallback : DiffUtil.ItemCallback<ItemGroup>() {
        private val listChanges = mutableMapOf<Long, EnumSet<Field>>()
        private val itemChanges = EnumSet.noneOf(Field::class.java)

        override fun areItemsTheSame(oldItem: ItemGroup, newItem: ItemGroup) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ItemGroup, newItem: ItemGroup) =
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

        override fun getChangePayload(oldItem: ItemGroup, newItem: ItemGroup) =
            listChanges.remove(newItem.id)
    }
}

/**
 * An ItemGroupListView to display and allow for editing of a list of ItemGroup instances.
 *
 * ItemGroupSelector displays the list of ItemGroup instances submitted through
 * ItemGroupListView.submitList, and provides modifiable callback members that
 * can be used to respond to item UI interaction.
 *
 * ItemGroupSelector implements a popup menu that is displayed when the user
 * taps the options button for an item group, and provides menu items that open
 * dialogs to allow renaming or deleting of item groups.
 *
 * Clicks on an item group will trigger ItemGroupSelector's onItemClick
 * callback if not null, while confirming the rename or delete dialogs will call
 * onItemRenameRequest or onItemDeletionRequest, respectively, if they are not
 * null.
 */
class ItemGroupSelector(context: Context, attrs: AttributeSet?) :
    ItemGroupListView(context, attrs)
{
    override val listAdapter = Adapter()

    var onItemGroupClick: ((Long) -> Unit)? = null
    var onItemGroupRenameRequest: ((Long, String) -> Unit)? = null
    var onItemGroupDeletionRequest: ((Long) -> Unit)? = null

    init { adapter = listAdapter }

    val ItemGroupViewHolder.item: ItemGroup get() =
        listAdapter.currentList[adapterPosition]

    protected inner class Adapter: ItemGroupListAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            super.onCreateViewHolder(parent, viewType).apply {
                view.setOnClickListener { onItemGroupClick?.invoke(item.id) }
                view.ui.optionsButton.setOnClickListener {
                    val menu = PopupMenu(context, view.ui.optionsButton)
                    menu.inflate(R.menu.item_group_options)
                    menu.setOnMenuItemClickListener {
                        if (it.itemId == R.id.renameItemGroupButton)
                            itemGroupNameDialog(context, item.name) { newName ->
                                onItemGroupRenameRequest?.invoke(item.id, newName)
                            }
                        else /*R.id.deleteItemGroupButton*/
                            deleteItemGroupDialog(context) {
                                onItemGroupDeletionRequest?.invoke(item.id)
                            }
                        true
                    }
                    menu.show()
                }
            }
    }
}

/** An options menu that provides a toggle checkbox for multi-selecting
 * item groups, as well as a select all button.
 *
 * @param anchor: The view that the menu will be anchored to.
 * @param multiSelectItemGroups Whether or not the multi-select item groups checkbox will be checked.
 * @param onMultiSelectCheckboxClick The callback that will be invoked when the multi-select item groups checkbox is clicked.
 * @param onSelectAllClick The callback that will be invoked when the select all item is clicked. */
class ItemGroupSelectorOptionsMenu(
    anchor: View,
    multiSelectItemGroups: Boolean,
    onMultiSelectCheckboxClick: () -> Unit,
    onSelectAllClick: () -> Unit,
) : PopupMenu(anchor.context, anchor) {

    private val multiSelectCheckBox: MenuItem
    private val selectAllButton: MenuItem

    init {
        inflate(R.menu.item_group_selector_options)
        multiSelectCheckBox = menu.findItem(R.id.multiSelectGroupsSwitch)
        selectAllButton = menu.findItem(R.id.selectAllGroups)
        multiSelectCheckBox.isChecked = multiSelectItemGroups
        selectAllButton.isEnabled = multiSelectItemGroups

        setOnMenuItemClickListener {
            if (it.itemId == R.id.multiSelectGroupsSwitch)
                onMultiSelectCheckboxClick()
            if (it.itemId == R.id.selectAllGroups)
                onSelectAllClick()
            true
        }
    }
}

/**
 * An ItemGroupListView that allows the user to pick a single item group from among a list of them.
 *
 * ItemGroupPicker, when provided with a list of ItemGroup instances through
 * ItemGroupListView.submitList, will allow the user to pick from among these
 * item groups by tapping on one. The currently chosen group is visually
 * indicated by setting the corresponding ItemGroupView's isSelected property
 * to true. The id of the currently chosen group, or null if one has not been
 * chosen yet, can be queried with the property chosenGroupId. If the user
 * picks a new group, the member onChosenGroupIdChanged will be called if it
 * is not null.
 */
class ItemGroupPicker(context: Context, attrs: AttributeSet) : ItemGroupListView(context, attrs) {

    override val listAdapter = Adapter()

    private var chosenPosition: Int? = null
    val chosenGroupId get() = listAdapter.currentList.getOrNull(chosenPosition ?: -1)?.id
    var onChosenGroupIdChanged: ((Long?) -> Unit)? = null

    init { adapter = listAdapter }

    val ItemGroupViewHolder.item: ItemGroup get() = listAdapter.currentList[adapterPosition]

    protected inner class Adapter: ItemGroupListAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            super.onCreateViewHolder(parent, viewType).apply {
                view.ui.optionsButton.layoutParams.width = resources.dpToPixels(12f).toInt()
                view.ui.optionsButton.isInvisible = true
                view.setOnClickListener {
                    if (chosenGroupId == item.id) return@setOnClickListener

                    val selectedViewHolder = findViewHolderForAdapterPosition(chosenPosition ?: -1)
                    selectedViewHolder?.itemView?.isSelected = false

                    view.isSelected = true
                    chosenPosition = adapterPosition
                    onChosenGroupIdChanged?.invoke(chosenGroupId)
                }
            }

        override fun onBindViewHolder(holder: ItemGroupViewHolder, position: Int) {
            super.onBindViewHolder(holder, position).also {
                val isSelected = holder.adapterPosition == chosenPosition
                holder.view.isSelected = isSelected
            }
        }
    }
}

/**
 * A view to represent an instance of ItemGroup.
 *
 * ItemGroupView displays the name, the number of shopping list items, the
 * number of inventory items, and an options button for the instance of
 * ItemGroup it is bound to using the function update. The selected state of an
 * item group is set using the Android framework's View's isSelected property.
 */
class ItemGroupView(context: Context) : LinearLayout(context) {
    val ui = ItemGroupBinding.inflate(LayoutInflater.from(context), this)

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

        background = ContextCompat.getDrawable(context, R.drawable.list_item)
    }

    fun update(group: ItemGroup) {
        ui.nameView.text = group.name
        ui.shoppingListItemCountView.text = group.shoppingListItemCount.toString()
        ui.inventoryItemCountView.text = group.inventoryItemCount.toString()
        isSelected = group.isSelected
    }
}