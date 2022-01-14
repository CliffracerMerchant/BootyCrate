/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.os.Bundle
import android.view.*
import androidx.annotation.Keep
import androidx.fragment.app.activityViewModels
import com.cliffracertech.bootycrate.*
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.databinding.ShoppingListFragmentBinding
import com.cliffracertech.bootycrate.recyclerview.ShoppingListView
import com.cliffracertech.bootycrate.utils.NewShoppingListItemDialog
import com.cliffracertech.bootycrate.utils.recollectWhenStarted
import com.cliffracertech.bootycrate.view.CheckoutButton
import com.cliffracertech.bootycrate.viewmodel.InventoryViewModel
import com.cliffracertech.bootycrate.viewmodel.ShoppingListViewModel

/**
 * A fragment to display and modify the user's shopping list.
 *
 * ShoppingListFragment is a ListViewFragment subclass to view and modify a
 * list of ShoppingListItems using a ShoppingListView. ShoppingListFragment
 * overrides ListViewFragment's abstract listView property with an instance of
 * ShoppingListView.
 */
@Keep class ShoppingListFragment : ListViewFragment<ShoppingListItem>() {
    override val viewModel: ShoppingListViewModel by activityViewModels()
    private val inventoryItemViewModel: InventoryViewModel by activityViewModels()
    override var listView: ShoppingListView? = null
    override lateinit var collectionName: String

    private var checkoutButton: CheckoutButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ShoppingListFragmentBinding.inflate(inflater, container, false).apply {
        listView = shoppingListView
        shoppingListView.onItemCheckBoxClick = viewModel::onItemCheckboxClicked
        collectionName = inflater.context.getString(
            R.string.shopping_list_item_collection_name)
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.recollectWhenStarted(viewModel.checkoutButtonIsEnabled) {
            checkoutButton?.isEnabled = it
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listView = null
    }

    override fun onDetach() {
        super.onDetach()
        checkoutButton = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_to_inventory_button -> {
            inventoryItemViewModel.onAddFromSelectedShoppingListItemsRequest()
            true
        } R.id.check_all_menu_item -> { viewModel.onCheckAllRequest(); true }
        R.id.uncheck_all_menu_item -> { viewModel.onUncheckAllRequest(); true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun showsCheckoutButton() = true
    override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
        super.onActiveStateChanged(isActive, activityUi)
        checkoutButton = activityUi.checkoutButton
        activityUi.actionBar.optionsMenu.setGroupVisible(
            R.id.shopping_list_view_menu_group, isActive)
        if (!isActive) return

        activityUi.addButton.setOnClickListener {
            val activity = this.activity ?: return@setOnClickListener
            NewShoppingListItemDialog(activity)
                .show(activity.supportFragmentManager, null)
        }
        activityUi.checkoutButton.checkoutCallback = viewModel::onCheckoutRequest
    }
}