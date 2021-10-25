/*
 * Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateInventory
import com.cliffracertech.bootycrate.database.DatabaseSettingsViewModel
import com.cliffracertech.bootycrate.database.InventoryViewModel
import com.cliffracertech.bootycrate.databinding.InventoryViewBinding
import com.cliffracertech.bootycrate.utils.deleteInventoryDialog
import com.cliffracertech.bootycrate.utils.dpToPixels
import com.cliffracertech.bootycrate.utils.intValueAnimator
import com.cliffracertech.bootycrate.utils.inventoryNameDialog
import java.util.*

/**
 * A RecyclerView to display a list of all of the inventories exposed by an
 * instance of InventoryViewModel.
 *
 * InventorySelectorRecyclerView will display a list of all of the user's
 * inventories exposed by an instance of InventoryViewModel. An instance
 * of InventoryViewModel must be initialized with the function initViewModel,
 * during which an appropriate LifecycleOwner should be provided as well.
 *
 *  In its normal mode, accessed by passing a false value for the parameter
 * inSelectedInventoryPickerMode, all inventories will be shown. Inventories
 * can be tapped to update their corresponding view model item's selected
 * state, and each inventory will have an options button that allows the
 * user to rename or delete items.
 *
 * InventorySelectorRecyclerView's alternate single inventory picker mode
 * is accessed by passing a true value for the parameter inSelectedInventoryPickerMode
 * during construction. In this mode, only inventories that are selected
 * will be displayed. A tap on an inventory will pick it, and unpick the
 * previously picked inventory, if any. The chosen inventory's id, if any,
 * can be accessed through the property chosenInventoryId.
 */
class InventorySelectorRecyclerView(
    context: Context, attrs: AttributeSet,
    private val inSelectedInventoryPickerMode: Boolean
) : RecyclerView(context, attrs) {

    private lateinit var viewModel: InventoryViewModel
    private val adapter = Adapter()
    private var chosenPosition: Int? = null
        private set

    val chosenInventoryId get() = adapter.currentList.getOrNull(chosenPosition ?: -1)?.id

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, false)

    init {
        setAdapter(adapter)
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        val spacing = resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing)
        addItemDecoration(ItemSpacingDecoration(spacing))
    }

    fun initViewModel(viewModel: InventoryViewModel, lifecycleOwner: LifecycleOwner) {
        this.viewModel = viewModel
        val liveData = if (inSelectedInventoryPickerMode) viewModel.selectedInventories
                       else                               viewModel.inventories
        liveData.observe(lifecycleOwner) { adapter.submitList(it) }
    }

    private inner class Adapter : ListAdapter<BootyCrateInventory, ViewHolder>(DiffUtilCallback()) {

        init { setHasStableIds(true) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(InventoryView(context))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            (holder.itemView as InventoryView).update(getItem(position))

        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.size == 0)
                return onBindViewHolder(holder, position)

            for (payload in payloads) {
                val item = getItem(position)
                val ui = (holder.itemView as InventoryView).ui
                @Suppress("UNCHECKED_CAST")
                val changes = payload as EnumSet<Field>

                if (changes.contains(Field.Name) && item.name != ui.nameView.text.toString())
                    ui.nameView.text = item.name
                if (changes.contains(Field.ShoppingListItemCount))
                    ui.shoppingListItemCountView.text = item.shoppingListItemCount.toString()
                if (changes.contains(Field.InventoryItemCount))
                    ui.inventoryItemCountView.text = item.inventoryItemCount.toString()
                if (changes.contains(Field.IsSelected))
                    holder.view.setSelectedState(item.isSelected)
            }
        }

        override fun getItemId(position: Int) = currentList[position].id
    }

    private inner class ViewHolder(view: InventoryView) : RecyclerView.ViewHolder(view) {
        val view get() = itemView as InventoryView
        val item: BootyCrateInventory get() = adapter.currentList[adapterPosition]

        init { view.apply {
            if (inSelectedInventoryPickerMode) {
                ui.optionsButton.isVisible = false
                setSelectedState(adapterPosition == chosenPosition, animate = false)
                setOnClickListener {
                    chosenPosition?.let {
                        (findViewHolderForAdapterPosition(it) as? ViewHolder)
                            ?.view?.setSelectedState(false)
                    }
                    setSelectedState(true)
                    chosenPosition = adapterPosition
                }
            } else {
                setOnClickListener { viewModel.updateIsSelected(item.id) }
                ui.optionsButton.setOnClickListener {
                    val menu = PopupMenu(context, ui.optionsButton)
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
        }}
    }

    private enum class Field { Name, ShoppingListItemCount, InventoryItemCount, IsSelected }

    private class DiffUtilCallback : DiffUtil.ItemCallback<BootyCrateInventory>() {
        private val listChanges = mutableMapOf<Long, EnumSet<Field>>()
        private val itemChanges = EnumSet.noneOf(Field::class.java)

        override fun areItemsTheSame(oldItem: BootyCrateInventory, newItem: BootyCrateInventory) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BootyCrateInventory, newItem: BootyCrateInventory) =
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

        override fun getChangePayload(oldItem: BootyCrateInventory, newItem: BootyCrateInventory) =
            listChanges.remove(newItem.id)
    }

    /** Show a popup options menu with options relating to the function of the
     * InventorySelectorRecyclerView. Because InventorySelectorRecyclerView
     * doesn't itself have a view that will open this menu, it is expected that
     * an external entity will set showOptionsMenu as the onClickListener for
     * a suitable view. This same view should also be passed as the anchor. */
    fun showOptionsMenu(activity: ComponentActivity, anchor: View) =
        PopupMenu(context, anchor).apply {
            val dbSettingsViewModel = ViewModelProvider(activity).get(DatabaseSettingsViewModel::class.java)
            inflate(R.menu.inventory_selector_options)
            val checkbox = menu.findItem(R.id.multiSelectInventoriesSwitch)
            checkbox.isChecked = dbSettingsViewModel.multiSelectInventories.value

            val selectAll = menu.findItem(R.id.selectAllInventories)
            selectAll.isEnabled = checkbox.isChecked

            setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.multiSelectInventoriesSwitch -> {
                        checkbox.isChecked = !checkbox.isChecked
                        dbSettingsViewModel.toggleMultiSelectInventories()
                    } R.id.selectAllInventories ->
                        viewModel.selectAll()
                }; true
            }
        }.show()
}

class InventoryView(context: Context) : LinearLayout(context) {
    val ui = InventoryViewBinding.inflate(LayoutInflater.from(context), this)
    private val gradientOutline: GradientDrawable

    init {
        val inventoryIcon = ContextCompat.getDrawable(context, R.drawable.inventory_icon)
        val shoppingListIcon = ContextCompat.getDrawable(context, R.drawable.shopping_cart_icon)
        val iconPadding = resources.dpToPixels(3f).toInt()
        ui.shoppingListItemCountView.setCompoundDrawablesRelativeWithIntrinsicBounds(shoppingListIcon, null, null, null)
        ui.inventoryItemCountView.setCompoundDrawablesRelativeWithIntrinsicBounds(inventoryIcon, null, null, null)
        inventoryIcon?.bounds?.inset(iconPadding, iconPadding)
        shoppingListIcon?.bounds?.inset(iconPadding, iconPadding)

        background = ContextCompat.getDrawable(context, R.drawable.recycler_view_item).also {
            gradientOutline = ((it as LayerDrawable).getDrawable(1) as LayerDrawable).getDrawable(0) as GradientDrawable
        }
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun update(inventory: BootyCrateInventory) {
        ui.nameView.text = inventory.name
        ui.shoppingListItemCountView.text = inventory.shoppingListItemCount.toString()
        ui.inventoryItemCountView.text = inventory.inventoryItemCount.toString()
        setSelectedState(inventory.isSelected, animate = false)
    }

    private var _isInSelectedState = false
    val isInSelectedState get() = _isInSelectedState
    fun select() = setSelectedState(true)
    fun deselect() = setSelectedState(false)
    fun setSelectedState(selected: Boolean, animate: Boolean = true) {
        _isInSelectedState = selected
        val endAlpha = if (selected) 255 else 0
        if (animate) intValueAnimator(setter = gradientOutline::setAlpha,
                                      from = gradientOutline.alpha,
                                      to = endAlpha).start()
        else gradientOutline.alpha = endAlpha
    }
}