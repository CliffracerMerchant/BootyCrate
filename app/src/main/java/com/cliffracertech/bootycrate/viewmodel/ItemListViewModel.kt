/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.database.*
import com.cliffracertech.bootycrate.utils.enumPreferenceFlow
import com.cliffracertech.bootycrate.utils.preferenceFlow
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * An abstract AndroidViewModel to provide data and callbacks for a fragment
 * that displays a list of ListItem subclasses.
 *
 * ItemListViewModel's StateFlow property uiState's represents the latest value
 * of ItemListViewModel.UiState, values of which represent different states of
 * a screen displaying a list of searchable items. If uiState's latest emitted
 * value is a UiState.Content instance, then the list of items to be displayed
 * is contained inside the UiState instance through the property items.
 *
 * ItemListViewModel listens to changes in the integer datastore value pointed
 * to by the value of the string resource R.string.pref_item_sort_key, maps
 * this value to a value of ListItem.Sort, and uses this sort value when it
 * provides the list of ListItems. It also uses the current value of an
 * injected SearchQueryState's query property to filter the returned items. As
 * modifying the value of either of these parameters does not fall into the
 * purview of a fragment displaying a list of items, writes to the datastore
 * preference and changes to the injected SearchQueryState's query property
 * need to be performed in another entity.
 *
 * The current sorting option and search query are exposed as Flows to
 * subclasses through the properties searchQuery and sort. Subclasses should
 * override the abstract property items to return a Flow<List<T>> containing
 * all of the items in the database that match the current sorting option and
 * search query, as well as any additional sorting parameters that they
 * themselves add.
 */
abstract class ItemListViewModel<T: ListItem>(
    context: Context,
    searchQueryState: SearchQueryState,
    private val messenger: Messenger,
    protected val dao: ItemDao,
    private val itemGroupDao: ItemGroupDao,
): ViewModel() {

    protected val searchQuery = searchQueryState.query.asStateFlow()

    protected val sort = context.dataStore.enumPreferenceFlow(
        intPreferencesKey(context.getString(R.string.pref_item_sort_key)), ListItem.Sort.Color)

    /** A class representing the UI state for an activity / fragment displaying a list of ListItems. */
    sealed class UiState {
        /** The activity/fragment should display a loading indicator. */
        object Loading : ItemListViewModel.UiState()
        /** The activity/fragment should display a message
         * indicating that the list of items is empty. */
        object EmptyContents: ItemListViewModel.UiState()
        /** The activity/fragment should display a message indicating that the
         * list of items is empty due to no items matching the search filter. */
        object EmptySearchResults : ItemListViewModel.UiState()
        /** The activity/fragment should display the provided list
         * of items provided through the state's 'items' property . */
        data class Content<T>(val items: List<T>) : UiState()
    }

    protected abstract val items: StateFlow<List<T>>

    val uiState by lazy { // items has not been overridden at this point
        items.combine(searchQuery) { items, query -> when {
            items.isNotEmpty() -> UiState.Content(items)
            query != null ->      UiState.EmptySearchResults
            else ->               UiState.EmptyContents
        }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), UiState.Loading)
    }

    fun onRenameItemRequest(id: Long, name: String) {
        viewModelScope.launch { dao.updateName(id, name) }
    }
    fun onChangeItemExtraInfoRequest(id: Long, extraInfo: String) {
        viewModelScope.launch { dao.updateExtraInfo(id, extraInfo) }
    }
    fun onChangeItemColorIndexRequest(id: Long, color: Int) {
        viewModelScope.launch { dao.updateColorIndex(id, color) }
    }
    abstract fun onChangeItemAmountRequest(id: Long, amount: Int)

    abstract fun onItemEditButtonClick(id: Long)

    abstract val selectedItemCount: StateFlow<Int>
    fun onItemClick(id: Long) {
        if (selectedItemCount.value > 0)
            toggleIsSelected(id)
    }
    fun onItemLongClick(id: Long) = toggleIsSelected(id)

    protected abstract fun toggleIsSelected(id: Long)
    abstract fun onSelectAllRequest()
    abstract fun onClearSelectionRequest()

    fun onItemSwipe(id: Long) { viewModelScope.launch {
        deleteItem(id)
        val message = Messenger.DeletedItemsMessage(1, ::undoDelete) {
            if (it != DISMISS_EVENT_ACTION && it != DISMISS_EVENT_CONSECUTIVE)
                emptyTrash()
        }
        messenger.postItemsDeletedMessage(message)
    }}

    protected abstract suspend fun deleteItem(id: Long)
    protected abstract fun emptyTrash()
    protected abstract fun undoDelete()
}



/**
 * An implementation of ItemListViewModel<ShoppingListItem>.
 *
 * ShoppingListViewModel adds callbacks to respond to clicks on the checkbox of
 * items, and to respond to a request to checkout. It also reads the value of
 * the boolean datastore preference pointed to by the key a StateFlow
 * property checkoutButtonIsEnabled, the current value of which indicates the
 * disabled/enabled state of the checkout button.
 */
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    @ActivityContext context: Context,
    searchQueryState: SearchQueryState,
    messenger: Messenger,
    dao: ItemDao,
    itemGroupDao: ItemGroupDao,
) : ItemListViewModel<ShoppingListItem>(context, searchQueryState, messenger, dao, itemGroupDao) {

    private val sortByCheckedKey = booleanPreferencesKey(context.getString(R.string.pref_sort_by_checked_key))
    private val sortByChecked = context.dataStore.preferenceFlow(
        key = sortByCheckedKey, defaultValue = false)

    override val items = combine(sort, searchQuery, sortByChecked, dao::getShoppingList)
        .transformLatest { emitAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun onChangeItemAmountRequest(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateShoppingListAmount(id, amount) }
    }

    override fun onItemEditButtonClick(id: Long) {
        viewModelScope.launch { dao.toggleExpandedInShoppingList(id) }
    }

    val checkoutButtonIsEnabled = dao.getCheckedShoppingListItemsSize()
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun onItemCheckboxClicked(id: Long) {
        viewModelScope.launch { dao.toggleIsChecked(id) }
    }
    fun onCheckAllRequest() {
        viewModelScope.launch { dao.checkAllShoppingListItems() }
    }
    fun onUncheckAllRequest() {
        viewModelScope.launch { dao.uncheckAllShoppingListItems() }
    }
    fun onCheckoutRequest() {
        viewModelScope.launch { dao.checkout() }
    }


    override val selectedItemCount = dao.getSelectedShoppingListItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    override fun toggleIsSelected(id: Long) {
        viewModelScope.launch { dao.toggleSelectedInShoppingList(id) }
    }
    override fun onSelectAllRequest() {
        viewModelScope.launch { dao.selectAllShoppingListItems() }
    }
    override fun onClearSelectionRequest() {
        viewModelScope.launch { dao.clearShoppingListSelection() }
    }
    fun onAddFromSelectedInventoryItemsRequest() {
        viewModelScope.launch {
            dao.addToShoppingListFromSelectedInventoryItems()
            dao.clearInventorySelection()
        }
    }


    override suspend fun deleteItem(id: Long) = dao.deleteShoppingListItem(id)

    override fun emptyTrash() {
        viewModelScope.launch { dao.emptyShoppingListTrash() }
    }
    override fun undoDelete() {
        viewModelScope.launch { dao.undoDeleteShoppingListItems() }
    }
}



/** An implementation of ItemListViewModel<InventoryItem> that adds functions
 * to manipulate the autoAddToShoppingList and autoAddToShoppingListAmount
 * fields of items in the database. */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    @ActivityContext context: Context,
    searchQueryState: SearchQueryState,
    messenger: Messenger,
    dao: ItemDao,
    itemGroupDao: ItemGroupDao,
) : ItemListViewModel<InventoryItem>(context, searchQueryState, messenger, dao, itemGroupDao) {

    override val items = sort.combine(searchQuery, dao::getInventoryContents)
        .transformLatest { emitAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())


    override fun onChangeItemAmountRequest(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateInventoryAmount(id, amount) }
    }
    override fun onItemEditButtonClick(id: Long) {
        viewModelScope.launch { dao.toggleExpandedInInventory(id) }
    }
    fun onAutoAddToShoppingListCheckboxClick(id: Long) {
        viewModelScope.launch { dao.toggleAutoAddToShoppingList(id) }
    }
    fun onAutoAddToShoppingListAmountUpdateRequest(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateAutoAddToShoppingListAmount(id, amount) }
    }


    override val selectedItemCount = dao.getSelectedInventoryItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    override fun toggleIsSelected(id: Long) {
        viewModelScope.launch { dao.toggleSelectedInInventory(id) }
    }
    override fun onSelectAllRequest() {
        viewModelScope.launch { dao.selectAllInventoryItems() }
    }
    override fun onClearSelectionRequest() {
        viewModelScope.launch { dao.clearInventorySelection() }
    }
    fun onAddFromSelectedShoppingListItemsRequest() {
        viewModelScope.launch {
            dao.addToInventoryFromSelectedShoppingListItems()
            dao.clearShoppingListSelection()
        }
    }

    override suspend fun deleteItem(id: Long) = dao.deleteInventoryItem(id)

    override fun emptyTrash() {
        viewModelScope.launch { dao.emptyInventoryTrash() }
    }
    override fun undoDelete() {
        viewModelScope.launch { dao.undoDeleteInventoryItems() }
    }
}