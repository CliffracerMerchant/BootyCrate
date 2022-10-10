/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextSwitcher
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cliffracertech.bootycrate.databinding.InventoryFragmentBinding
import com.cliffracertech.bootycrate.databinding.ShoppingListFragmentBinding
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemListView
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.repeatWhenStarted
import com.cliffracertech.bootycrate.utils.setPadding
import kotlinx.coroutines.launch

/**
 * A fragment to display an ExpandableItemListView to the user.
 *
 * ItemListFragment is an abstract fragment whose main purpose is to display an
 * instance of a ExpandableItemListView to the user. It's abstract property
 * viewModel must be overridden in subclasses with a concrete implementation of
 * ItemListViewModel. The properties listView and emptyListMessageView should
 * also be initialized to an instance of an ExpandableItemListView and a
 * TextSwitcher, respectively, in subclass implementations of onCreateView.
 * Because ItemListFragment's implementation of onViewCreated references these
 * properties, subclasses must initialize these properties before calling
 * super.onViewCreated.
 *
 * Due to the fact that lists frequently need to have their bottom padding
 * adjusted to account for ui elements that overlay them (e.g. a floating
 * action button), the function setListBottomPadding is provided. Calling it
 * will set the listView's bottom padding immediately if it is already created,
 * or upon creation otherwise.
 */
abstract class ItemListFragment<T: ListItem> : Fragment() {

    protected abstract val viewModel: ItemListViewModel<T>
    protected var listView: ExpandableItemListView<T>? = null
    var emptyListMessageView: TextSwitcher? = null
        protected set

    private var listViewBottomPadding: Int? = null
    fun setListBottomPadding(paddingBottom: Int) {
        listViewBottomPadding = paddingBottom
        listView?.setPadding(bottom = paddingBottom)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView?.apply {
            // We set the listView's bottom padding manually here so that it
            // is set to the value of listViewBottomPadding even if
            // setListBottomPadding was called before listView was initialized.
            listViewBottomPadding?.let { setPadding(bottom = it) }
            onItemClick = viewModel::onItemClick
            onItemLongClick = viewModel::onItemLongClick
            onItemColorIndexChangeRequest = viewModel::onChangeItemColorIndexRequest
            onItemRenameRequest = viewModel::onRenameItemRequest
            onItemExtraInfoChangeRequest = viewModel::onChangeItemExtraInfoRequest
            onItemAmountChangeRequest = viewModel::onChangeItemAmountRequest
            onItemEditButtonClick = viewModel::onItemEditButtonClick
            onItemSwipe = viewModel::onItemSwipe
        }

        viewLifecycleOwner.repeatWhenStarted {
            launch { viewModel.items.collect { listView?.submitList(it) }}
            launch { viewModel.emptyMessage.collect(::showItemsMessage) }
        }
    }

    private fun showItemsMessage(message: StringResource?) {
        val messageView = emptyListMessageView ?: return
        val messageText = message?.resolve(context) ?: ""
        messageView.setText(messageText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listView = null
        emptyListMessageView = null
    }
}

/**
 * A fragment to display and modify the user's shopping list using a ShoppingListView. */
@Keep class ShoppingListFragment : ItemListFragment<ShoppingListItem>() {
    override val viewModel: ShoppingListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ShoppingListFragmentBinding.inflate(inflater, container, false).apply {
        listView = shoppingListView
        emptyListMessageView = itemListMessage
        shoppingListView.onItemCheckBoxClick = viewModel::onItemCheckboxClicked
    }.root
}

/** A fragment to display and modify the user's inventory using an InventoryView. */
@Keep class InventoryFragment: ItemListFragment<InventoryItem>() {
    override val viewModel: InventoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = InventoryFragmentBinding.inflate(inflater, container, false).apply {
        listView = inventoryView
        emptyListMessageView = itemListMessage
        inventoryView.onItemAutoAddToShoppingListCheckboxClick =
            viewModel::onAutoAddToShoppingListCheckboxClick
        inventoryView.onItemAutoAddToShoppingListAmountChangeRequest =
            viewModel::onAutoAddToShoppingListAmountUpdateRequest
    }.root
}