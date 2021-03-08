/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.cliffracermerchant.bootycrate.databinding.InventoryFragmentBinding

/**
 * A fragment to display and modify the user's inventory.
 *
 * InventoryFragment is a RecyclerViewFragment subclass to view and modify the
 * user's inventory using an InventoryRecyclerView. It does almost nothing
 * different from RecyclerViewFragment apart from implementing its abstract
 * properties with values suitable for display of an InventoryRecyclerView,
 * and using its own action mode callback.
 */
class InventoryFragment(isActive: Boolean = false) :
        RecyclerViewFragment<InventoryItem>(isActive) {
    override lateinit var recyclerView: InventoryRecyclerView
    override val actionMode = InventoryActionMode()
    lateinit var ui: InventoryFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        ui = InventoryFragmentBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = ui.inventoryRecyclerView
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActiveStateChanged(active: Boolean) {
        super.onActiveStateChanged(active)
        if (active) {
            val activity = activity as? MainActivity ?: return
            activity.ui.addButton.setOnClickListener {
                NewInventoryItemDialog(activity, activity.inventoryViewModel)
                    .show(activity.supportFragmentManager, null)
            }
            activity.ui.checkoutButton.checkoutCallback = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.add_to_shopping_list_button) {
            val activity = activity as? MainActivity ?: return false
            activity.shoppingListViewModel.addFromSelectedInventoryItems()
            actionMode.finishAndClearSelection()
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun setOptionsMenuItemsVisible(showing: Boolean) {
        super.setOptionsMenuItemsVisible(showing)
        mainActivity?.ui?.topActionBar?.optionsMenu?.setGroupVisible(R.id.inventory_view_menu_group, showing)
    }

    /** An override of RecyclerViewActionMode that alters the visibility of menu items specific to inventory items. */
    inner class InventoryActionMode : RecyclerViewFragment<InventoryItem>.ActionMode() {
        override fun onStart(actionBar: RecyclerViewActionBar) {
            super.onStart(actionBar)
            actionBar.optionsMenu.setGroupVisible(R.id.inventory_view_action_mode_menu_group, true)
        }

        override fun onFinish(actionBar: RecyclerViewActionBar) {
            super.onFinish(actionBar)
            actionBar.optionsMenu.setGroupVisible(R.id.inventory_view_action_mode_menu_group, false)
        }
    }
}