/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
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
    override val actionModeCallback = ActionModeCallback()
    override val fabRegularOnClickListener = View.OnClickListener { recyclerView.addNewItem() }
    override val fabActionModeOnClickListener = View.OnClickListener { recyclerView.deleteSelectedItems() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.inventory_view_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = inventoryRecyclerView
        val mainActivity = requireActivity() as MainActivity
        recyclerView.finishInit(viewLifecycleOwner,
                                mainActivity.inventoryViewModel,
                                mainActivity.shoppingListViewModel,
                                mainActivity.supportFragmentManager)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActiveStateChanged(active: Boolean) {
        super.onActiveStateChanged(active)
        if (active) activity.checkoutBtn.setOnClickListener(null)
    }

    override fun setOptionsMenuItemsVisible(showing: Boolean) {
        super.setOptionsMenuItemsVisible(showing)
        menu?.setGroupVisible(R.id.inventory_view_menu_group, showing)
    }

    /** An ActionMode.Callback for use when the user selects one or more inventory items.
     *
     *  ActionModeCallback overrides RecyclerViewFragment.ActionModeCallback with
     *  new implementations of onActionItemClicked and onPrepareActionMode. */
    inner class ActionModeCallback : RecyclerViewFragment<InventoryItem>.ActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            return if (super.onCreateActionMode(mode, menu)) {
                menu.setGroupVisible(R.id.inventory_view_action_mode_menu_group, true)
                true
            } else false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.add_to_shopping_list_button -> {
                    activity.shoppingListViewModel.addFromSelectedInventoryItems()
                    true
                } else -> activity.onOptionsItemSelected(item)
            }
        }
    }
}