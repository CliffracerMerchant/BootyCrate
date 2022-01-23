/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.fragment.app.activityViewModels
import com.cliffracertech.bootycrate.*
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.databinding.InventoryFragmentBinding
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.recyclerview.InventoryView
import com.cliffracertech.bootycrate.utils.NewInventoryItemDialog
import com.cliffracertech.bootycrate.viewmodel.InventoryViewModel
import com.cliffracertech.bootycrate.viewmodel.ShoppingListViewModel

/** A ListViewFragment to display and modify the user's inventory using an InventoryView. */
@Keep class InventoryFragment: ListViewFragment<InventoryItem>() {
    override val viewModel: InventoryViewModel by activityViewModels()
    private val shoppingListItemViewModel: ShoppingListViewModel by activityViewModels()
    override var listView: InventoryView? = null
    override lateinit var collectionName: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = InventoryFragmentBinding.inflate(inflater, container, false).apply {
        listView = inventoryView
        inventoryView.onItemAutoAddToShoppingListCheckboxClick =
            viewModel::onAutoAddToShoppingListCheckboxClick
        inventoryView.onItemAutoAddToShoppingListAmountChangeRequest =
            viewModel::onAutoAddToShoppingListAmountUpdateRequest
        collectionName = inflater.context.getString(R.string.inventory_item_collection_name)
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        listView = null
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.add_to_shopping_list_button) {
            shoppingListItemViewModel.onAddFromSelectedInventoryItemsRequest()
            true
        } else super.onOptionsItemSelected(item)

//    override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
//        super.onActiveStateChanged(isActive, activityUi)
//        activityUi.actionBar.optionsMenu.setGroupVisible(
//            R.id.inventory_view_menu_group, isActive)
//        if (!isActive) return
//
//        activityUi.addButton.setOnClickListener {
//            val activity = this.activity ?: return@setOnClickListener
//            NewInventoryItemDialog(activity)
//                .show(activity.supportFragmentManager, null)
//        }
//        activityUi.checkoutButton.checkoutCallback = null
//    }
}