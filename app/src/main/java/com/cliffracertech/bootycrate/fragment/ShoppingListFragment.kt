/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.cliffracertech.bootycrate.*
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.databinding.ShoppingListFragmentBinding
import com.cliffracertech.bootycrate.recyclerview.ShoppingListRecyclerView
import com.cliffracertech.bootycrate.view.CheckoutButton
import com.cliffracertech.bootycrate.view.RecyclerViewActionBar

/**
 * A fragment to display and modify the user's shopping list.
 *
 * ShoppingListFragment is a RecyclerViewFragment subclass to view and modify
 * the user's shopping list using an ShoppingListRecyclerView. ShoppingListFragment
 * overrides RecyclerViewFragment's abstract recyclerView property with an
 * instance of ShoppingListRecyclerView, and overrides its ActionModeCallback
 * with its own version.
 *
 * ShoppingListFragment also manages the state and function of the checkout
 * button. The checkout button is enabled when the user has checked at least
 * one shopping list item, and disabled when no items are checked through its
 * observation of ShoppingListRecyclerView's checkedItems member. If the
 * checkout button is clicked while it is enabled, it switches to its
 * confirmatory state to safeguard the user from checking out accidentally. If
 * the user does not press the button again within two seconds, it will revert
 * to its normal state.
 */
class ShoppingListFragment : RecyclerViewFragment<ShoppingListItem>() {
    override val viewModel: ShoppingListViewModel by activityViewModels()
    private val inventoryViewModel: InventoryViewModel by activityViewModels()
    override var recyclerView: ShoppingListRecyclerView? = null
    override lateinit var collectionName: String
    override val actionModeCallback = ShoppingListActionModeCallback()

    private var checkoutButton: CheckoutButton? = null
    private var checkoutButtonShouldBeEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ShoppingListFragmentBinding.inflate(inflater, container, false).apply {
        recyclerView = shoppingListRecyclerView
        collectionName = inflater.context.getString(R.string.shopping_list_item_collection_name)
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sortByCheckedPrefKey = getString(R.string.pref_sort_by_checked)
        val sortByCheckedPrefValue = prefs.getBoolean(sortByCheckedPrefKey, false)
        recyclerView?.sortByChecked = sortByCheckedPrefValue
        viewModel.checkedItemsSize.observe(viewLifecycleOwner) { newSize ->
            checkoutButton?.isEnabled = newSize != 0
            // Sometimes onChanged is called before onActiveStateChanged is called, causing
            // the checkout button to still be null. In this case we'll store the value
            // and initialize the checkout button's state in onActiveStateChanged.
            checkoutButtonShouldBeEnabled =  newSize != 0
        }
        viewModel.items.observe(viewLifecycleOwner) { newList -> updateBadge(newList) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
    }

    override fun onDetach() {
        super.onDetach()
        checkoutButton = null
        newItemsBadge = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_to_inventory_button -> {
            inventoryViewModel.addFromSelectedShoppingListItems()
            recyclerView?.selection?.clear()
            true
        } R.id.check_all_menu_item -> { viewModel.checkAll(); true }
        R.id.uncheck_all_menu_item -> { viewModel.uncheckAll(); true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun showsCheckoutButton() = true
    override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
        super.onActiveStateChanged(isActive, activityUi)
        if (checkoutButton == null) {
            checkoutButton = activityUi.checkoutButton
            activityUi.checkoutButton.isEnabled = checkoutButtonShouldBeEnabled
        }
        newItemsBadge = activityUi.shoppingListBadge
        activityUi.actionBar.optionsMenu.setGroupVisible(R.id.shopping_list_view_menu_group, isActive)
        if (!isActive) return
        activityUi.addButton.setOnClickListener {
            val activity = this.activity ?: return@setOnClickListener
            NewShoppingListItemDialog(activity).show(activity.supportFragmentManager, null)
        }
        activityUi.checkoutButton.checkoutCallback = { viewModel.checkout() }
    }

    private var shoppingListSize = -1
    private var shoppingListNumNewItems = 0
    private var newItemsBadge: TextView? = null
    private fun updateBadge(newShoppingList: List<ShoppingListItem>) {
        if (shoppingListSize == -1) {
            if (newShoppingList.isNotEmpty())
                shoppingListSize = newShoppingList.size
        } else {
            val sizeChange = newShoppingList.size - shoppingListSize
            val badge = newItemsBadge ?: return
            if (view?.isVisible == false && sizeChange > 0) {
                shoppingListNumNewItems += sizeChange
                badge.text = getString(R.string.shopping_list_badge_text, shoppingListNumNewItems)
                badge.alpha = 1f
                badge.animate().alpha(0f).setDuration(1000).setStartDelay(1500).
                    withLayer().withEndAction { shoppingListNumNewItems = 0 }.start()
            }
            shoppingListSize = newShoppingList.size
        }
    }

    /** An override of SelectionActionModeCallback that alters the visibility of menu items specific to shopping list items. */
    inner class ShoppingListActionModeCallback : SelectionActionModeCallback() {
        override fun onStart(actionMode: RecyclerViewActionBar.ActionMode, actionBar: RecyclerViewActionBar) {
            super.onStart(actionMode, actionBar)
            actionBar.optionsMenu.setGroupVisible(
                R.id.shopping_list_view_action_mode_menu_group, true)
        }

        override fun onFinish(actionMode: RecyclerViewActionBar.ActionMode, actionBar: RecyclerViewActionBar) =
            actionBar.optionsMenu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, false)
    }
}