/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.MessageHandler
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.model.SearchQueryState
import com.cliffracertech.bootycrate.model.database.*
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.enumPreferenceFlow
import com.cliffracertech.bootycrate.utils.preferenceFlow
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * An abstract view model to provide data and callbacks for a fragment that
 * displays a list of ListItem subclasses.
 *
 * ItemListViewModel's items property represents the latest list of items that
 * should be displayed by implementing activities/fragments. The property
 * emptyMessage represents a StringResource that, when resolved to a string,
 * should be displayed to the user in place of the list of items.
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
 * search query, as well as any additional sorting parameters that they add.
 */
abstract class ItemListViewModel<T: ListItem>(
    context: Context,
    searchQueryState: SearchQueryState,
    private val messageHandler: MessageHandler,
    protected val dao: ItemDao,
    private val itemGroupDao: ItemGroupDao,
): ViewModel() {

    abstract val collectionNameResId: StringResource.Id
    protected val searchQuery = searchQueryState.query.asStateFlow()

    protected val sort = context.dataStore.enumPreferenceFlow(
        intPreferencesKey(context.getString(R.string.pref_item_sort_key)), ListItem.Sort.Color)

    abstract val items: StateFlow<List<T>>

    val emptyMessage by lazy { // items has not been overridden at this point
        items.combine(searchQuery) { items, query -> when {
            items.isNotEmpty() -> null
            query != null -> StringResource(R.string.no_search_results_message)
            else -> StringResource(R.string.empty_list_message, collectionNameResId)
        }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), null)
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
    abstract fun onClearSelectionRequest()

    fun onItemSwipe(id: Long) { viewModelScope.launch {
        deleteItem(id)
        messageHandler.postItemsDeletedMessage(1, ::undoDelete) {
            if (it != DISMISS_EVENT_ACTION && it != DISMISS_EVENT_CONSECUTIVE)
                emptyTrash()
        }
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
    @ApplicationContext context: Context,
    searchQueryState: SearchQueryState,
    messageHandler: MessageHandler,
    dao: ItemDao,
    itemGroupDao: ItemGroupDao,
) : ItemListViewModel<ShoppingListItem>(context, searchQueryState, messageHandler, dao, itemGroupDao) {

    override val collectionNameResId =
        StringResource.Id(R.string.shopping_list_description)
    private val sortByCheckedKey = booleanPreferencesKey(context.getString(R.string.pref_sort_by_checked_key))
    private val sortByChecked = context.dataStore.preferenceFlow(
        key = sortByCheckedKey, defaultValue = false)

    override val items = combine(sort, searchQuery, sortByChecked, dao::getShoppingList)
        .transformLatest { emitAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), emptyList())

    override fun onChangeItemAmountRequest(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateShoppingListAmount(id, amount) }
    }

    override fun onItemEditButtonClick(id: Long) {
        viewModelScope.launch { dao.toggleExpandedInShoppingList(id) }
    }

    fun onItemCheckboxClicked(id: Long) {
        viewModelScope.launch { dao.toggleIsChecked(id) }
    }


    override val selectedItemCount = dao.getSelectedShoppingListItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    override fun toggleIsSelected(id: Long) {
        viewModelScope.launch { dao.toggleSelectedInShoppingList(id) }
    }
    override fun onClearSelectionRequest() {
        viewModelScope.launch { dao.clearShoppingListSelection() }
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
    @ApplicationContext context: Context,
    searchQueryState: SearchQueryState,
    messageHandler: MessageHandler,
    dao: ItemDao,
    itemGroupDao: ItemGroupDao,
) : ItemListViewModel<InventoryItem>(context, searchQueryState, messageHandler, dao, itemGroupDao) {

    override val collectionNameResId =
        StringResource.Id(R.string.inventory_description)

    override val items = sort.combine(searchQuery, dao::getInventory)
        .transformLatest { emitAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), emptyList())


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
    override fun onClearSelectionRequest() {
        viewModelScope.launch { dao.clearInventorySelection() }
    }


    override suspend fun deleteItem(id: Long) = dao.deleteInventoryItem(id)

    override fun emptyTrash() {
        viewModelScope.launch { dao.emptyInventoryTrash() }
    }
    override fun undoDelete() {
        viewModelScope.launch { dao.undoDeleteInventoryItems() }
    }
}