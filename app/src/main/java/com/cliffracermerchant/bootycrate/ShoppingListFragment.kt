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
import androidx.preference.PreferenceManager
import com.cliffracermerchant.bootycrate.databinding.ShoppingListFragmentBinding

/**
 * A fragment to display and modify the user's shopping list.
 *
 * ShoppingListFragment is a RecyclerViewFragment subclass to view and modify
 * the user's shopping list using an ShoppingListRecyclerView. ShoppingList-
 * Fragment overrides RecyclerViewFragment's abstract recyclerView property
 * with an instance of ShoppingListRecyclerView, and overrides its ActionMode-
 * Callback with its own version.
 *
 * ShoppingListFragment also manages the state and function of the checkout
 * button. The checkout button is enabled when the user has checked at least
 * one shopping list item, and disabled when no items are checked through its
 * observation of ShoppingListRecyclerView's checkedItems member. If the check-
 * out button is clicked while it is enabled, it switches to its confirmatory
 * state to safeguard the user from checking out accidentally. If the user
 * does not press the button again within two seconds, it will revert to its
 * normal state.
 */
class ShoppingListFragment(isActive: Boolean = false) :
        RecyclerViewFragment<ShoppingListItem>(isActive) {

    override lateinit var recyclerView: ShoppingListRecyclerView
    override val actionMode = ShoppingListActionMode()
    lateinit var ui: ShoppingListFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        ui = ShoppingListFragmentBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = ui.shoppingListRecyclerView

        val activity = requireActivity() as MainActivity
        recyclerView.checkedItems.sizeLiveData.observe(viewLifecycleOwner)
            { newSize -> activity.ui.checkoutButton.isEnabled = newSize != 0 }

        val sortByCheckedPrefKey = getString(R.string.pref_sort_by_checked)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sortByChecked = prefs.getBoolean(sortByCheckedPrefKey, false)
        recyclerView.sortByChecked = sortByChecked

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActiveStateChanged(active: Boolean) {
        super.onActiveStateChanged(active)
        val activity = activity as? MainActivity ?: return
        if (active) {
            activity.ui.addButton.setOnClickListener {
                NewShoppingListItemDialog(activity, activity.shoppingListViewModel)
                    .show(activity.supportFragmentManager, null)
            }
            activity.ui.checkoutButton.checkoutCallback = activity.shoppingListViewModel::checkout
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_to_inventory_button -> {
            val activity = this.activity as? MainActivity
            activity?.inventoryViewModel?.addFromSelectedShoppingListItems()
            actionMode.finishAndClearSelection()
            true
        } R.id.check_all_menu_item -> {
            recyclerView.checkedItems.checkAll()
            true
        } R.id.uncheck_all_menu_item -> {
            recyclerView.checkedItems.clear()
            true
        } else -> super.onOptionsItemSelected(item)
    }

    override fun setOptionsMenuItemsVisible(showing: Boolean) {
        super.setOptionsMenuItemsVisible(showing)
        mainActivity?.ui?.topActionBar?.optionsMenu?.setGroupVisible(R.id.shopping_list_view_menu_group, showing)
    }

    /** An override of RecyclerViewActionMode that alters the visibility of menu items specific to shopping list items. */
    inner class ShoppingListActionMode : RecyclerViewFragment<ShoppingListItem>.ActionMode() {
        override fun onStart(actionBar: RecyclerViewActionBar) {
            super.onStart(actionBar)
            actionBar.optionsMenu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, true)
        }

        override fun onFinish(actionBar: RecyclerViewActionBar) {
            super.onFinish(actionBar)
            actionBar.optionsMenu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, false)
        }
    }
}