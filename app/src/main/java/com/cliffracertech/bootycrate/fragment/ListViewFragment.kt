/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.animation.LayoutTransition
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.databinding.InventoryFragmentBinding
import com.cliffracertech.bootycrate.databinding.ShoppingListFragmentBinding
import com.cliffracertech.bootycrate.recyclerview.*
import com.cliffracertech.bootycrate.utils.repeatWhenStarted
import com.cliffracertech.bootycrate.utils.setPadding
import com.kennyc.view.MultiStateView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * A fragment to display an ExpandableItemListView to the user.
 *
 * ListViewFragment is an abstract fragment whose main purpose is to display an
 * instance of a ExpandableItemListView to the user. It has two abstract
 * properties, viewModel and listView, that must be overridden in subclasses
 * with concrete implementations of ItemListViewModel and ExpandableItemListView,
 * respectively. Because ListViewFragment's implementation of onViewCreated
 * references its abstract listView property, it is important that subclasses
 * override the listView property and initialize it before calling
 * super.onViewCreated, or an exception will occur.
 *
 * Due to the fact that lists frequently need to have their bottom padding
 * adjusted to account for ui elements that overlay them (e.g. a floating
 * action button), the function setListBottomPadding is provided. Calling it
 * will set the listView's bottom padding immediately if it is already created,
 * or upon creation otherwise.
 *
 * The value of the open property collectionName should be overridden in
 * subclasses with a value that describes what the collection of items
 * should be called in user facing strings. This override must occur before
 * super.onViewCreated is called in subclasses, or an UninitializedPropertyAccessException
 * will be thrown.
 */
abstract class ListViewFragment<T: ListItem> : Fragment() {

    protected abstract val viewModel: ItemListViewModel<T>
    protected abstract val listView: ExpandableItemListView<T>?
    private var listViewBottomPadding: Int? = null
    fun setListBottomPadding(paddingBottom: Int) {
        listViewBottomPadding = paddingBottom
        listView?.setPadding(bottom = paddingBottom)
    }

    private var multiStateView: MultiStateView? = null
    protected open val collectionName = ""
    private var bottomAppBar: View? = null

    private val emptyList = emptyList<T>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // We set the listViewBottomPadding value again here so that the
        // listView's bottom padding is set to the correct value even if
        // setListBottomPadding was called while listView was still null.
        listViewBottomPadding?.let { setListBottomPadding(it) }

        multiStateView = view as? MultiStateView
        multiStateView?.layoutTransition = LayoutTransition()
        (multiStateView?.getView(MultiStateView.ViewState.EMPTY) as? TextView)
            ?.text = getString(R.string.empty_list_message, collectionName)

        val content = multiStateView?.getView(MultiStateView.ViewState.CONTENT)
        (content as? ExpandableItemListView<*>)?.apply {
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
            launch { viewModel.chooserIntents.collect {
                context?.startActivity(Intent.createChooser(it, null))
            }}
            launch { viewModel.uiState.collect(::setUiState) }
        }
    }

    private fun setUiState(state: ItemListViewModel.UiState) {
        val view = multiStateView ?: return
        // The list of items is submitted even if it's empty to fix a bug
        // where swiping the last item to delete it and then undoing the
        // deletion results in the last item's view being stuck off the
        // side of the screen. Submitting an empty list prevents this.
        val items = if (state is ItemListViewModel.UiState.Content<*>)
            state.items as List<T>
        else emptyList
        listView?.submitList(items)
        view.viewState = when (state) {
            is ItemListViewModel.UiState.Loading ->
                MultiStateView.ViewState.LOADING
            is ItemListViewModel.UiState.EmptyContents ->
                MultiStateView.ViewState.EMPTY
            is ItemListViewModel.UiState.EmptySearchResults ->
                MultiStateView.ViewState.ERROR
            is ItemListViewModel.UiState.Content<*> ->
                MultiStateView.ViewState.CONTENT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        multiStateView = null
    }

    override fun onDetach() {
        super.onDetach()
        bottomAppBar = null
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        viewModel.onOptionsItemClick(item.itemId)
}

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
    override var listView: ShoppingListView? = null
    override lateinit var collectionName: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ShoppingListFragmentBinding.inflate(inflater, container, false).apply {
        listView = shoppingListView
        shoppingListView.onItemCheckBoxClick = viewModel::onItemCheckboxClicked
        collectionName = inflater.context.getString(R.string.shopping_list_description)
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        listView = null
    }
}

/** A ListViewFragment to display and modify the user's inventory using an InventoryView. */
@Keep class InventoryFragment: ListViewFragment<InventoryItem>() {
    override val viewModel: InventoryViewModel by activityViewModels()
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
        collectionName = inflater.context.getString(R.string.inventory_description)
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        listView = null
    }
}