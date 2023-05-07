/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.annotation.CallSuper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.cliffracertech.bootycrate.ViewModel
import com.cliffracertech.bootycrate.activity.MessageHandler
import com.cliffracertech.bootycrate.model.ItemListVolatileState
import com.cliffracertech.bootycrate.model.SharedState
import com.cliffracertech.bootycrate.model.database.AsyncInventoryStateProvider
import com.cliffracertech.bootycrate.model.database.AsyncShoppingListStateProvider
import com.cliffracertech.bootycrate.model.database.DefaultAsyncInventoryStateProvider
import com.cliffracertech.bootycrate.model.database.DefaultAsyncShoppingListStateProvider
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.InventoryItemDao
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ListItemDao
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItemDao
import com.cliffracertech.bootycrate.utils.StringResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AsyncListState<out T: Any> {
    object Loading: AsyncListState<Nothing>()
    class Message(val text: StringResource) : AsyncListState<Nothing>()
    class Content<T: ListItem>(
        override val items: ImmutableList<T>,
        override val selectedItemIds: ImmutableSet<Long>,
        override val expandedItemId: Long?,
        override val colorPickerItemId: Long?,
    ) : AsyncListState<T>(), ItemListState<T> {
        constructor(
            items: ImmutableList<T>,
            volatileState: ItemListVolatileState
        ): this(items,
            volatileState.selection.ids,
            volatileState.expandedItemId,
            volatileState.colorPickerItemId)
    }
}

/**
 * An abstract view model to provide data and callbacks for a screen
 * that displays a list of ListItem subclasses.
 *
 * The property [listState] contains the current [AsyncListState] instance
 * that describes the data that should be displayed. UI interactions with
 * individual items should be connected to the methods [onItemRenameRequest],
 * [onItemExtraInfoChangeRequest], [onItemColorIndicatorClick],
 * [onItemColorGroupChangeRequest], [onItemAmountChangeRequest],
 * [onItemEditButtonClick], [onItemClick], [onItemLongClick], and [onItemSwipe].

 * Subclasses should override the method [deleteItem] and the abstract property
 * [listState]. [listState] should be overridden to be equal to an [AsyncListState]`<T>`
 * describing the list of items in the database that match the current sorting
 * option and search query, as well as any additional sorting parameters that
 * they add. [listState] can be null if the list of items is still loading.
 */
abstract class ItemListViewModel<T: ListItem>(
    private val messageHandler: MessageHandler,
    private val dao: ListItemDao,
    private val itemGroupDao: ItemGroupDao,
    private val volatileState: ItemListVolatileState,
    coroutineScope: CoroutineScope
): ViewModel(coroutineScope) {
    abstract val listState: AsyncListState<T>

    fun onItemRenameRequest(id: Long, name: String) {
        coroutineScope.launch { dao.setName(id, name) }
    }
    fun onItemExtraInfoChangeRequest(id: Long, extraInfo: String) {
        coroutineScope.launch { dao.setExtraInfo(id, extraInfo) }
    }

    fun onItemColorIndicatorClick(id: Long) = volatileState.toggleShowColorPickerFor(id)

    fun onItemColorGroupChangeRequest(id: Long, colorGroup: ListItem.ColorGroup) {
        if (id == volatileState.colorPickerItemId) {
            coroutineScope.launch { dao.setColorGroup(id, colorGroup) }
            volatileState.toggleShowColorPickerFor(id)
        }
    }
    abstract fun onItemAmountChangeRequest(id: Long, amount: Int)

    fun onItemEditButtonClick(id: Long) = volatileState.toggleExpansionFor(id)

    fun onItemClick(id: Long) {
        if (volatileState.selection.isNotEmpty)
            volatileState.selection.toggle(id)
    }

    fun onItemLongClick(id: Long) = volatileState.selection.toggle(id)

    fun onItemSwipe(id: Long) {
        coroutineScope.launch { deleteItem(id) }
        messageHandler.postItemsDeletedMessage(
            count = 1,
            onUndo = ::undoDelete,
            onTimeout = ::emptyTrash)
    }

    @CallSuper
    protected open suspend fun deleteItem(id: Long) {
        if (id == volatileState.expandedItemId)
            volatileState.toggleExpansionFor(id)
        if (id == volatileState.colorPickerItemId)
            volatileState.toggleShowColorPickerFor(id)
    }
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
    volatileState: ItemListVolatileState,
    coroutineScope: CoroutineScope,
) : ItemListViewModel<ShoppingListItem>(
        messageHandler, dao, itemGroupDao, volatileState, coroutineScope),
    AsyncShoppingListStateProvider by DefaultAsyncShoppingListStateProvider(
        searchQueryState, volatileState, dao, dataStore, coroutineScope)
{
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        @SharedState.SearchQuery
        searchQueryState: MutableStateFlow<String?>,
        messageHandler: MessageHandler,
        dao: ShoppingListItemDao,
        itemGroupDao: ItemGroupDao,
        @SharedState.ShoppingListVolatileState
        volatileState: ItemListVolatileState,
    ) : this(dataStore, searchQueryState, messageHandler,
             dao, itemGroupDao, volatileState, viewModelScope())

    override val listState by ::shoppingListState

    override fun onItemAmountChangeRequest(id: Long, amount: Int) {
        coroutineScope.launch { dao.setAmount(id, amount) }
    }

    fun onItemCheckboxClick(id: Long) {
        coroutineScope.launch { dao.toggleIsChecked(id) }
    }

    override suspend fun deleteItem(id: Long) {
        dao.delete(id)
        super.deleteItem(id)
    }
    override fun emptyTrash() {
        coroutineScope.launch { dao.emptyTrash() }
    }
    override fun undoDelete() {
        coroutineScope.launch { dao.undoDelete() }
    }
}


/** An implementation of [ItemListViewModel]`<InventoryItem>` that adds the methods
 * [onAutoAddToShoppingListCheckboxClick] and [onAutoAddToShoppingListAmountChangeRequest]
 * to use as callbacks for item interactions. */
@HiltViewModel class InventoryViewModel(
    dataStore: DataStore<Preferences>,
    searchQueryState: StateFlow<String?>,
    messageHandler: MessageHandler,
    private val dao: InventoryItemDao,
    itemGroupDao: ItemGroupDao,
    volatileState: ItemListVolatileState,
    coroutineScope: CoroutineScope,
) : ItemListViewModel<InventoryItem>(
        messageHandler, dao, itemGroupDao, volatileState, coroutineScope),
    AsyncInventoryStateProvider by DefaultAsyncInventoryStateProvider(
        searchQueryState, volatileState, dao, dataStore, coroutineScope)
{
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        @SharedState.SearchQuery
        searchQueryState: MutableStateFlow<String?>,
        messageHandler: MessageHandler,
        dao: InventoryItemDao,
        itemGroupDao: ItemGroupDao,
        @SharedState.InventoryVolatileState
        volatileState: ItemListVolatileState,
    ) : this(dataStore, searchQueryState, messageHandler,
             dao, itemGroupDao, volatileState, viewModelScope())

    override val listState by ::inventoryState

    override fun onItemAmountChangeRequest(id: Long, amount: Int) {
        coroutineScope.launch { dao.setAmount(id, amount) }
    }

    fun onAutoAddToShoppingListCheckboxClick(id: Long) {
        coroutineScope.launch { dao.toggleAutoAddToShoppingList(id) }
    }
    fun onAutoAddToShoppingListAmountChangeRequest(id: Long, amount: Int) {
        coroutineScope.launch { dao.setAutoAddToShoppingListAmount(id, amount) }
    }

    override suspend fun deleteItem(id: Long) {
        dao.delete(id)
        super.deleteItem(id)
    }
    override fun emptyTrash() {
        coroutineScope.launch { dao.emptyTrash() }
    }
    override fun undoDelete() {
        coroutineScope.launch { dao.undoDelete() }
    }
}