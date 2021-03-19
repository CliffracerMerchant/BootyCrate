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
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.cliffracermerchant.bootycrate.databinding.MainActivityBinding
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
class ShoppingListFragment : RecyclerViewFragment<ShoppingListItem>() {
    override val viewModel: ShoppingListViewModel by activityViewModels()
    private val inventoryViewModel: InventoryViewModel by activityViewModels()
    override var recyclerView: ExpandableSelectableRecyclerView<ShoppingListItem>? = null
    override val actionMode = ShoppingListActionMode()
    lateinit var ui: ShoppingListFragmentBinding
    private var checkoutButton: CheckoutButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ShoppingListFragmentBinding.inflate(inflater, container, false)
        .apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = ui.shoppingListRecyclerView
        this.recyclerView = recyclerView

        recyclerView.checkedItems.sizeLiveData.observe(viewLifecycleOwner)
            { newSize -> checkoutButton?.isEnabled = newSize != 0 }

        val sortByCheckedPrefKey = getString(R.string.pref_sort_by_checked)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sortByChecked = prefs.getBoolean(sortByCheckedPrefKey, false)
        recyclerView.sortByChecked = sortByChecked

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDetach() {
        super.onDetach()
        checkoutButton = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_to_inventory_button -> {
            inventoryViewModel.addFromSelectedShoppingListItems()
            actionMode.finishAndClearSelection()
            true
        } R.id.check_all_menu_item -> { viewModel.checkAll(); true }
        R.id.uncheck_all_menu_item -> { viewModel.uncheckAll(); true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun showsCheckoutButton() = true
    override fun onActiveStateChanged(isActive: Boolean, ui: MainActivityBinding) {
        super.onActiveStateChanged(isActive, ui)
        checkoutButton = ui.checkoutButton
        ui.topActionBar.optionsMenu.setGroupVisible(R.id.shopping_list_view_menu_group, isActive)
        if (!isActive) return
        ui.addButton.setOnClickListener {
            val activity = this.activity ?: return@setOnClickListener
            NewShoppingListItemDialog(activity).show(activity.supportFragmentManager, null)
        }
        ui.checkoutButton.checkoutCallback = { viewModel.checkout() }
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