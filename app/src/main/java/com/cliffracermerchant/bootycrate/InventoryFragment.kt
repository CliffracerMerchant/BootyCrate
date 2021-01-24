/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import kotlinx.android.synthetic.main.inventory_view_fragment_layout.*

/** A fragment to display and modify the user's inventory.
 *
 *  InventoryFragment is a RecyclerViewFragment subclass to view and modify the
 *  user's inventory using an InventoryRecyclerView. It does almost nothing
 *  different from RecyclerViewFragment apart from implementing its abstract
 *  properties with values suitable for display of an InventoryRecyclerView,
 *  and using its own action mode callback. */
class InventoryFragment(isActive: Boolean = false) :
        RecyclerViewFragment<InventoryItem>(isActive) {
    override lateinit var recyclerView: InventoryRecyclerView
    override val actionMode = InventoryActionMode()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.inventory_view_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = inventoryRecyclerView
        val mainActivity = requireActivity() as MainActivity
        recyclerView.finishInit(viewLifecycleOwner,
                                mainActivity.inventoryViewModel,
                                mainActivity.shoppingListViewModel)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActiveStateChanged(active: Boolean) {
        super.onActiveStateChanged(active)
        if (active) {
            activity.addButton.setOnClickListener {
                NewInventoryItemDialog(activity, activity.inventoryViewModel)
                    .show(activity.supportFragmentManager, null)
            }
            activity.checkoutButton.checkoutCallback = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.add_to_shopping_list_button) {
            activity.shoppingListViewModel.addFromSelectedInventoryItems()
            actionMode.finishAndClearSelection()
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun setOptionsMenuItemsVisible(showing: Boolean) {
        super.setOptionsMenuItemsVisible(showing)
        menu?.setGroupVisible(R.id.inventory_view_menu_group, showing)
    }

    /** An override of RecyclerViewActionMode that alters the visibility of menu items specific to inventory items. */
    inner class InventoryActionMode() : RecyclerViewFragment<InventoryItem>.RecyclerViewActionMode() {
        override fun onStart(actionBar: ActionBar, menu: Menu, titleView: TextView?) {
            super.onStart(actionBar, menu, titleView)
            menu.setGroupVisible(R.id.inventory_view_action_mode_menu_group, true)
        }

        override fun onFinish(actionBar: ActionBar, menu: Menu, titleView: TextView?) {
            super.onFinish(actionBar, menu, titleView)
            menu.setGroupVisible(R.id.inventory_view_action_mode_menu_group, false)
        }
    }
}