/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.MessageHandler
import com.cliffracertech.bootycrate.model.SearchQueryState
import com.cliffracertech.bootycrate.model.SelectionState
import com.cliffracertech.bootycrate.model.database.*
import com.cliffracertech.bootycrate.settings.PrefKeys
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.collectAsState
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
 * An abstract view model to provide data and callbacks for a screen
 * that displays a list of ListItem subclasses.
 *
 * The property [uiState] contains the current [UiState] instance that
 * describes the data that should be displayed. UI interactions with
 * individual items should be connected to the methods [onItemRenameRequest],
 * [onItemExtraInfoChangeRequest], [onItemColorChangeRequest],
 * [onItemAmountChangeRequest], [onItemEditButtonClick], [onItemClick],
 * [onItemLongClick], and [onItemSwipe].

 * The current sorting option and search query are exposed to subclasses
 * through the properties [searchQuery] and [sort]. Subclasses should override
 * the abstract property [items] to to be equal to an ImmutableList<T>?
 * containing all of the items in the database that match the current sorting
 * option and search query, as well as any additional sorting parameters that
 * they add. [items] can be null if the list of items is still loading.
 */
abstract class ItemListViewModel<T: ListItem>(
    dataStore: DataStore<Preferences>,
    searchQueryState: SearchQueryState,
    private val messageHandler: MessageHandler,
    protected val dao: ItemDao,
    private val itemGroupDao: ItemGroupDao,
    private val selection: SelectionState,
    coroutineScope: CoroutineScope?
): ViewModel() {

    protected val scope = coroutineScope ?: viewModelScope
    abstract val collectionNameResId: StringResource.Id
    protected val searchQuery by searchQueryState::query

    protected val sort by dataStore.enumPreferenceState(
        intPreferencesKey(PrefKeys.itemSort), scope, ListItem.Sort.Color)

    protected abstract val items: ImmutableList<T>?
    private val selectedItemIds get() = selection.selectedIds.keys
    private var expandedItemId by mutableStateOf<Long?>(null)

    /** The possible types of content for a screen showing a list of
     * [ListItem]s. These types are [Message], [Loading], and [Items]. */
    sealed class UiState {
        /** The list of items is still being loaded */
        object Loading : UiState()
        /** A message regarding the list of items should be displayed instead */
        class Message(val text: StringResource) : UiState()
        /** The list of items along with the set of selected item
         * ids and the id, if any, of the item that is expanded */
        class Items<T>(
            val list: ImmutableList<T>,
            val selectedItemIds: Set<Long>,
            val expandedItemId: Long?,
        ) : UiState()
    }

    val uiState by derivedStateOf {
        val items = this.items
        when {
            items == null -> UiState.Loading
            items.isEmpty() -> {
                val message = if (searchQuery != null)
                                  StringResource(R.string.no_search_results_message)
                              else StringResource(R.string.empty_list_message, collectionNameResId)
                UiState.Message(message)
            }
            else -> UiState.Items(items, selectedItemIds, expandedItemId)
        }
    }

    fun onItemRenameRequest(id: Long, name: String) {
        scope.launch { dao.updateName(id, name) }
    }
    fun onItemExtraInfoChangeRequest(id: Long, extraInfo: String) {
        scope.launch { dao.updateExtraInfo(id, extraInfo) }
    }
    fun onItemColorChangeRequest(id: Long, color: ListItem.Color) {
        scope.launch { dao.updateColorIndex(id, color.ordinal) }
    }
    abstract fun onItemAmountChangeRequest(id: Long, amount: Int)

    fun onItemEditButtonClick(id: Long) {
        expandedItemId = if (expandedItemId == id) null else id
    }

    fun onItemClick(id: Long) {
        if (selection.selectedIds.isNotEmpty())
            selection.toggle(id)
    }

    fun onItemLongClick(id: Long) = selection.toggle(id)

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
 * An implementation of ItemListViewModel<ShoppingListItem> that adds the
 * callback [onItemCheckboxClick] to use as a callback for item checkbox clicks.
 */
@HiltViewModel class ShoppingListViewModel(
    dataStore: DataStore<Preferences>,
    searchQueryState: SearchQueryState,
    messageHandler: MessageHandler,
    dao: ItemDao,
    itemGroupDao: ItemGroupDao,
    selection: SelectionState,
    coroutineScope: CoroutineScope?
) : ItemListViewModel<ShoppingListItem>(
    dataStore, searchQueryState, messageHandler,
    dao, itemGroupDao, selection, coroutineScope
) {
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        searchQueryState: SearchQueryState,
        messageHandler: MessageHandler,
        dao: ItemDao,
        itemGroupDao: ItemGroupDao,
        selection: SelectionState,
    ) : this(dataStore, searchQueryState, messageHandler,
             dao, itemGroupDao, selection, null)

    override val collectionNameResId = StringResource.Id(R.string.shopping_list_description)
    private val sortByCheckedKey = booleanPreferencesKey(PrefKeys.sortByChecked)
    private val sortByChecked by dataStore.preferenceState(sortByCheckedKey, false, scope)

    override val items by dao.getShoppingList(sort, searchQuery, sortByChecked)
        .map { it.toImmutableList() }
        .collectAsState(null, scope)

    override fun onItemAmountChangeRequest(id: Long, amount: Int) {
        scope.launch { dao.updateShoppingListAmount(id, amount) }
    }

    fun onItemCheckboxClick(id: Long) {
        scope.launch { dao.toggleIsChecked(id) }
    }

    override suspend fun deleteItem(id: Long) = dao.deleteShoppingListItem(id)

    override fun emptyTrash() {
        scope.launch { dao.emptyShoppingListTrash() }
    }
    override fun undoDelete() {
        scope.launch { dao.undoDeleteShoppingListItems() }
    }
}


/** An implementation of ItemListViewModel<InventoryItem> that adds the methods
 * [onAutoAddToShoppingListCheckboxClick] and [onAutoAddToShoppingListAmountChangeRequest]
 * to use as callbacks for item interactions. */
@HiltViewModel class InventoryViewModel(
    dataStore: DataStore<Preferences>,
    searchQueryState: SearchQueryState,
    messageHandler: MessageHandler,
    dao: ItemDao,
    itemGroupDao: ItemGroupDao,
    selection: SelectionState,
    coroutineScope: CoroutineScope?
) : ItemListViewModel<InventoryItem>(
    dataStore, searchQueryState, messageHandler,
    dao, itemGroupDao, selection, coroutineScope
) {
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        searchQueryState: SearchQueryState,
        messageHandler: MessageHandler,
        dao: ItemDao,
        itemGroupDao: ItemGroupDao,
        selection: SelectionState,
    ) : this(dataStore, searchQueryState, messageHandler,
             dao, itemGroupDao, selection, null)

    override val collectionNameResId = StringResource.Id(R.string.inventory_description)

    override val items by dao.getInventory(sort, searchQuery)
        .map { it.toImmutableList() }
        .collectAsState(null, scope)

    override fun onItemAmountChangeRequest(id: Long, amount: Int) {
        scope.launch { dao.updateInventoryAmount(id, amount) }
    }

    fun onAutoAddToShoppingListCheckboxClick(id: Long) {
        scope.launch { dao.toggleAutoAddToShoppingList(id) }
    }
    fun onAutoAddToShoppingListAmountChangeRequest(id: Long, amount: Int) {
        scope.launch { dao.updateAutoAddToShoppingListAmount(id, amount) }
    }


    override suspend fun deleteItem(id: Long) = dao.deleteInventoryItem(id)

    override fun emptyTrash() {
        scope.launch { dao.emptyInventoryTrash() }
    }
    override fun undoDelete() {
        scope.launch { dao.undoDeleteInventoryItems() }
    }
}