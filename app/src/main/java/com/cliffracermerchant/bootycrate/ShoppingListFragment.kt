/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import kotlinx.android.synthetic.main.shopping_list_fragment.*
import kotlinx.android.synthetic.main.shopping_list_fragment.view.*

/** A fragment to display and modify the user's shopping list.
 *
 *  ShoppingListFragment is a RecyclerViewFragment subclass to view and modify
 *  the user's shopping list using an ShoppingListRecyclerView. ShoppingList-
 *  Fragment overrides RecyclerViewFragment's abstract recyclerView property
 *  with an instance of ShoppingListRecyclerView, and overrides its ActionMode-
 *  Callback with its own version.
 *
 *  ShoppingListFragment also manages the state and function of the checkout
 *  button. The checkout button is enabled when the user has checked at least
 *  one shopping list item, and disabled when no items are checked through its
 *  observation of ShoppingListRecyclerView's checkedItems member. If the check-
 *  out button is clicked while it is enabled, it switches to its confirmatory
 *  state to safeguard the user from checking out accidentally. If the user
 *  does not press the button again within two seconds, it will revert to its
 *  normal state. */
class ShoppingListFragment(isActive: Boolean = false) :
        RecyclerViewFragment<ShoppingListItem>(isActive) {

    override lateinit var recyclerView: ShoppingListRecyclerView
    override val actionMode = ShoppingListActionMode()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.shopping_list_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = shoppingListFragmentView.shoppingListRecyclerView
        val mainActivity = requireActivity() as MainActivity
        recyclerView.finishInit(viewLifecycleOwner,
                                mainActivity.shoppingListViewModel,
                                mainActivity.inventoryViewModel)

        recyclerView.checkedItems.sizeLiveData.observe(viewLifecycleOwner)
            { newSize -> activity.checkoutButton.isEnabled = newSize != 0 }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActiveStateChanged(active: Boolean) {
        super.onActiveStateChanged(active)
        if (active) {
            activity.addButton.setOnClickListener {
//                NewShoppingListItemDialog(activity, activity.shoppingListViewModel)
//                    .show(activity.supportFragmentManager, null)
            }
            activity.checkoutButton.checkoutCallback = { activity.shoppingListViewModel.checkout() }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_to_inventory_button -> {
            activity.inventoryViewModel.addFromSelectedShoppingListItems()
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
        menu?.setGroupVisible(R.id.shopping_list_view_menu_group, showing)
    }

    /** An override of RecyclerViewActionMode that alters the visibility of menu items specific to shopping list items. */
    inner class ShoppingListActionMode() : RecyclerViewFragment<ShoppingListItem>.RecyclerViewActionMode() {
        override fun onStart(actionBar: ActionBar, menu: Menu, titleView: TextView?) {
            super.onStart(actionBar, menu, titleView)
            menu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, true)
        }

        override fun onFinish(actionBar: ActionBar, menu: Menu, titleView: TextView?) {
            super.onFinish(actionBar, menu, titleView)
            menu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, false)
        }
    }
}