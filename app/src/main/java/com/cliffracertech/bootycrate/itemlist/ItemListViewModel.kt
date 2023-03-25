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
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.ViewModel
import com.cliffracertech.bootycrate.activity.MessageHandler
import com.cliffracertech.bootycrate.itemlist.ItemListViewModel.UiState
import com.cliffracertech.bootycrate.itemlist.ItemListViewModel.UiState.*
import com.cliffracertech.bootycrate.model.SelectionState
import com.cliffracertech.bootycrate.model.SharedState
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.InventoryItemDao
import com.cliffracertech.bootycrate.model.database.InventoryProvider
import com.cliffracertech.bootycrate.model.database.InventoryProviderImpl
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ListItemDao
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItemDao
import com.cliffracertech.bootycrate.model.database.ShoppingListProvider
import com.cliffracertech.bootycrate.model.database.ShoppingListProviderImpl
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.collectAsState
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * An abstract view model to provide data and callbacks for a screen
 * that displays a list of ListItem subclasses.
 *
 * The property [uiState] contains the current [UiState] instance that
 * describes the data that should be displayed. UI interactions with
 * individual items should be connected to the methods [onItemRenameRequest],
 * [onItemExtraInfoChangeRequest], [onItemColorGroupChangeRequest],
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
    searchQueryState: StateFlow<String?>,
    private val messageHandler: MessageHandler,
    private val dao: ListItemDao,
    private val itemGroupDao: ItemGroupDao,
    private val selection: SelectionState,
    coroutineScope: CoroutineScope
): ViewModel(coroutineScope) {

    abstract val collectionNameResId: StringResource.Id
    private val searchQuery by searchQueryState
        .collectAsState(null, coroutineScope)

    protected abstract val items: ImmutableList<T>?
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
        class Items<T: ListItem>(
            override val itemList: ImmutableList<T>,
            override val selectedItemIds: ImmutableSet<Long>,
            override val expandedItemId: Long?,
        ) : UiState(), ItemListState<T>
    }

    val uiState by derivedStateOf {
        val items = this.items
        when {
            items == null -> Loading
            items.isEmpty() -> Message(
                if (searchQuery != null)
                    StringResource(R.string.no_search_results_message)
                else StringResource(R.string.empty_list_message, collectionNameResId))
            else -> Items(items, selection.ids.toImmutableSet(), expandedItemId)
        }
    }

    fun onItemRenameRequest(id: Long, name: String) {
        coroutineScope.launch { dao.setName(id, name) }
    }
    fun onItemExtraInfoChangeRequest(id: Long, extraInfo: String) {
        coroutineScope.launch { dao.setExtraInfo(id, extraInfo) }
    }
    fun onItemColorGroupChangeRequest(id: Long, colorGroup: ListItem.ColorGroup) {
        coroutineScope.launch { dao.setColorGroup(id, colorGroup) }
    }
    abstract fun onItemAmountChangeRequest(id: Long, amount: Int)

    fun onItemEditButtonClick(id: Long) {
        expandedItemId = if (expandedItemId == id) null else id
    }

    fun onItemClick(id: Long) {
        if (selection.ids.isNotEmpty())
            selection.toggle(id)
    }

    fun onItemLongClick(id: Long) = selection.toggle(id)

    fun onItemSwipe(id: Long) { coroutineScope.launch {
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


/** An implementation of ItemListViewModel<ShoppingListItem> that adds the
 * callback [onItemCheckboxClick] to use as a callback for item checkbox clicks. */
@HiltViewModel class ShoppingListViewModel(
    dataStore: DataStore<Preferences>,
    searchQueryState: StateFlow<String?>,
    messageHandler: MessageHandler,
    private val dao: ShoppingListItemDao,
    itemGroupDao: ItemGroupDao,
    selection: SelectionState,
    coroutineScope: CoroutineScope,
) : ItemListViewModel<ShoppingListItem>(
        searchQueryState, messageHandler,
        dao, itemGroupDao, selection, coroutineScope),
    ShoppingListProvider by ShoppingListProviderImpl(
        searchQueryState, dao, dataStore, coroutineScope)
{
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        @SharedState.SearchQuery
        searchQueryState: MutableStateFlow<String?>,
        messageHandler: MessageHandler,
        dao: ShoppingListItemDao,
        itemGroupDao: ItemGroupDao,
        @SharedState.InventorySelection
        selection: SelectionState,
    ) : this(dataStore, searchQueryState, messageHandler,
             dao, itemGroupDao, selection, viewModelScope())

    override val collectionNameResId = StringResource.Id(R.string.shopping_list_description)

    override val items by ::shoppingList

    override fun onItemAmountChangeRequest(id: Long, amount: Int) {
        coroutineScope.launch { dao.setAmount(id, amount) }
    }

    fun onItemCheckboxClick(id: Long) {
        coroutineScope.launch { dao.toggleIsChecked(id) }
    }

    override suspend fun deleteItem(id: Long) = dao.delete(id)

    override fun emptyTrash() {
        coroutineScope.launch { dao.emptyTrash() }
    }
    override fun undoDelete() {
        coroutineScope.launch { dao.undoDelete() }
    }
}


/** An implementation of ItemListViewModel<InventoryItem> that adds the methods
 * [onAutoAddToShoppingListCheckboxClick] and [onAutoAddToShoppingListAmountChangeRequest]
 * to use as callbacks for item interactions. */
@HiltViewModel class InventoryViewModel(
    dataStore: DataStore<Preferences>,
    searchQueryState: StateFlow<String?>,
    messageHandler: MessageHandler,
    private val dao: InventoryItemDao,
    itemGroupDao: ItemGroupDao,
    selection: SelectionState,
    coroutineScope: CoroutineScope,
) : ItemListViewModel<InventoryItem>(
        searchQueryState, messageHandler,
        dao, itemGroupDao, selection, coroutineScope),
    InventoryProvider by InventoryProviderImpl(
        searchQueryState, dao, dataStore, coroutineScope)
{
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        @SharedState.SearchQuery
        searchQueryState: MutableStateFlow<String?>,
        messageHandler: MessageHandler,
        dao: InventoryItemDao,
        itemGroupDao: ItemGroupDao,
        selection: SelectionState,
    ) : this(dataStore, searchQueryState, messageHandler,
             dao, itemGroupDao, selection, viewModelScope())

    override val collectionNameResId = StringResource.Id(R.string.inventory_description)

    override val items by ::inventory

    override fun onItemAmountChangeRequest(id: Long, amount: Int) {
        coroutineScope.launch { dao.setAmount(id, amount) }
    }

    fun onAutoAddToShoppingListCheckboxClick(id: Long) {
        coroutineScope.launch { dao.toggleAutoAddToShoppingList(id) }
    }
    fun onAutoAddToShoppingListAmountChangeRequest(id: Long, amount: Int) {
        coroutineScope.launch { dao.setAutoAddToShoppingListAmount(id, amount) }
    }


    override suspend fun deleteItem(id: Long) = dao.delete(id)

    override fun emptyTrash() {
        coroutineScope.launch { dao.emptyTrash() }
    }
    override fun undoDelete() {
        coroutineScope.launch { dao.undoDelete() }
    }
}