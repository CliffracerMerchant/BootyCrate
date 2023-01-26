/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.MessageHandler
import com.cliffracertech.bootycrate.activity.collectAsState
import com.cliffracertech.bootycrate.model.SearchQueryState
import com.cliffracertech.bootycrate.model.database.*
import com.cliffracertech.bootycrate.settings.PrefKeys
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.enumPreferenceState
import com.cliffracertech.bootycrate.utils.preferenceState
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
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
    dataStore: DataStore<Preferences>,
    searchQueryState: SearchQueryState,
    private val messageHandler: MessageHandler,
    protected val dao: ItemDao,
    private val itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?
): ViewModel() {

    protected val scope = coroutineScope ?: viewModelScope
    abstract val collectionNameResId: StringResource.Id
    protected val searchQuery = searchQueryState.query

    protected val sort by dataStore.enumPreferenceState(
        intPreferencesKey(PrefKeys.itemSort), scope, ListItem.Sort.Color)

    abstract val items: ImmutableList<T>

    val emptyMessage by derivedStateOf {
    //    lazy { // items has not been overridden at this point
        when {
            items.isNotEmpty() -> null
            searchQuery != null -> StringResource(R.string.no_search_results_message)
            else -> StringResource(R.string.empty_list_message, collectionNameResId)
        }
    }

    fun onRenameItemRequest(id: Long, name: String) {
        scope.launch { dao.updateName(id, name) }
    }
    fun onChangeItemExtraInfoRequest(id: Long, extraInfo: String) {
        scope.launch { dao.updateExtraInfo(id, extraInfo) }
    }
    fun onChangeItemColorIndexRequest(id: Long, color: Int) {
        scope.launch { dao.updateColorIndex(id, color) }
    }
    abstract fun onChangeItemAmountRequest(id: Long, amount: Int)

    abstract fun onItemEditButtonClick(id: Long)

    abstract val selectedItemCount: Int

    fun onItemClick(id: Long) {
        if (selectedItemCount > 0)
            toggleIsSelected(id)
    }

    fun onItemLongClick(id: Long) = toggleIsSelected(id)

    protected abstract fun toggleIsSelected(id: Long)

    abstract fun onClearSelectionRequest()

    fun onItemSwipe(id: Long) { scope.launch {
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
class ShoppingListViewModel(
    dataStore: DataStore<Preferences>,
    searchQueryState: SearchQueryState,
    messageHandler: MessageHandler,
    dao: ItemDao,
    itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?
) : ItemListViewModel<ShoppingListItem>(
    dataStore, searchQueryState, messageHandler, dao, itemGroupDao, coroutineScope
) {
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        searchQueryState: SearchQueryState,
        messageHandler: MessageHandler,
        dao: ItemDao,
        itemGroupDao: ItemGroupDao,
    ) : this(dataStore, searchQueryState, messageHandler, dao, itemGroupDao, null)

    override val collectionNameResId = StringResource.Id(R.string.shopping_list_description)
    private val sortByCheckedKey = booleanPreferencesKey(PrefKeys.sortByChecked)
    private val sortByChecked by dataStore.preferenceState(sortByCheckedKey, false, scope)

    override val items by dao.getShoppingList(sort, searchQuery, sortByChecked)
        .map { it.toImmutableList() }
        .collectAsState(emptyList<ShoppingListItem>().toImmutableList(), scope)

    override fun onChangeItemAmountRequest(id: Long, amount: Int) {
        scope.launch { dao.updateShoppingListAmount(id, amount) }
    }

    override fun onItemEditButtonClick(id: Long) {
        scope.launch { dao.toggleExpandedInShoppingList(id) }
    }

    fun onItemCheckboxClicked(id: Long) {
        scope.launch { dao.toggleIsChecked(id) }
    }


    override val selectedItemCount by dao.getSelectedShoppingListItemCount()
        .collectAsState(0, scope)

    override fun toggleIsSelected(id: Long) {
        scope.launch { dao.toggleSelectedInShoppingList(id) }
    }
    override fun onClearSelectionRequest() {
        scope.launch { dao.clearShoppingListSelection() }
    }


    override suspend fun deleteItem(id: Long) = dao.deleteShoppingListItem(id)

    override fun emptyTrash() {
        scope.launch { dao.emptyShoppingListTrash() }
    }
    override fun undoDelete() {
        scope.launch { dao.undoDeleteShoppingListItems() }
    }
}



/** An implementation of ItemListViewModel<InventoryItem> that adds functions
 * to manipulate the autoAddToShoppingList and autoAddToShoppingListAmount
 * fields of items in the database. */
@HiltViewModel
class InventoryViewModel(
    dataStore: DataStore<Preferences>,
    searchQueryState: SearchQueryState,
    messageHandler: MessageHandler,
    dao: ItemDao,
    itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?
) : ItemListViewModel<InventoryItem>(
    dataStore, searchQueryState, messageHandler, dao, itemGroupDao, coroutineScope
) {
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        searchQueryState: SearchQueryState,
        messageHandler: MessageHandler,
        dao: ItemDao,
        itemGroupDao: ItemGroupDao,
    ) : this(dataStore, searchQueryState, messageHandler, dao, itemGroupDao, null)

    override val collectionNameResId = StringResource.Id(R.string.inventory_description)

    override val items by dao.getInventory(sort, searchQuery)
        .map { it.toImmutableList() }
        .collectAsState(emptyList<InventoryItem>().toImmutableList(), scope)

    override fun onChangeItemAmountRequest(id: Long, amount: Int) {
        scope.launch { dao.updateInventoryAmount(id, amount) }
    }
    override fun onItemEditButtonClick(id: Long) {
        scope.launch { dao.toggleExpandedInInventory(id) }
    }
    fun onAutoAddToShoppingListCheckboxClick(id: Long) {
        scope.launch { dao.toggleAutoAddToShoppingList(id) }
    }
    fun onAutoAddToShoppingListAmountUpdateRequest(id: Long, amount: Int) {
        scope.launch { dao.updateAutoAddToShoppingListAmount(id, amount) }
    }


    override val selectedItemCount by dao.getSelectedInventoryItemCount()
        .collectAsState(0, scope)

    override fun toggleIsSelected(id: Long) {
        scope.launch { dao.toggleSelectedInInventory(id) }
    }
    override fun onClearSelectionRequest() {
        scope.launch { dao.clearInventorySelection() }
    }


    override suspend fun deleteItem(id: Long) = dao.deleteInventoryItem(id)

    override fun emptyTrash() {
        scope.launch { dao.emptyInventoryTrash() }
    }
    override fun undoDelete() {
        scope.launch { dao.undoDeleteInventoryItems() }
    }
}