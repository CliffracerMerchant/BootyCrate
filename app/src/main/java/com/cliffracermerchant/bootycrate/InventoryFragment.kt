/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.cliffracermerchant.bootycrate.databinding.InventoryFragmentBinding
import com.cliffracermerchant.bootycrate.databinding.MainActivityBinding

/**
 * A fragment to display and modify the user's inventory.
 *
 * InventoryFragment is a RecyclerViewFragment subclass to view and modify the
 * user's inventory using an InventoryRecyclerView. It does almost nothing
 * different from RecyclerViewFragment apart from implementing its abstract
 * properties with values suitable for display of an InventoryRecyclerView,
 * and using its own action mode callback.
 */
class InventoryFragment: RecyclerViewFragment<InventoryItem>() {
    override val viewModel: InventoryViewModel by activityViewModels()
    private val shoppingListViewModel: ShoppingListViewModel by activityViewModels()
    override var recyclerView: InventoryRecyclerView? = null
    override val actionModeCallback = InventoryActionMode()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = InventoryFragmentBinding.inflate(inflater, container, false).apply {
        recyclerView = inventoryRecyclerView
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.add_to_shopping_list_button) {
            shoppingListViewModel.addFromSelectedInventoryItems()
            recyclerView?.selection?.clear()
            true
        } else super.onOptionsItemSelected(item)

    override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
        super.onActiveStateChanged(isActive, activityUi)
        activityUi.actionBar.optionsMenu.setGroupVisible(R.id.inventory_view_menu_group, isActive)
        if (!isActive) return
        activityUi.addButton.setOnClickListener {
            val activity = this.activity ?: return@setOnClickListener
            NewInventoryItemDialog(activity).show(activity.supportFragmentManager, null)
        }
        activityUi.checkoutButton.checkoutCallback = null
    }

    /** An override of RecyclerViewActionMode that alters the visibility of menu items specific to inventory items. */
    inner class InventoryActionMode : ActionModeCallback() {
        override fun onStart(actionBar: RecyclerViewActionBar) =
            actionBar.optionsMenu.setGroupVisible(R.id.inventory_view_action_mode_menu_group, true)

        override fun onFinish(actionBar: RecyclerViewActionBar) =
            actionBar.optionsMenu.setGroupVisible(R.id.inventory_view_action_mode_menu_group, false)
    }
}