/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.actionbar

import android.content.Intent
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.ViewModel
import com.cliffracertech.bootycrate.activity.MessageHandler
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.model.SelectionState
import com.cliffracertech.bootycrate.model.SharedState
import com.cliffracertech.bootycrate.model.database.InventoryItemDao
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.DefaultInventoryProvider
import com.cliffracertech.bootycrate.model.database.DefaultShoppingListProvider
import com.cliffracertech.bootycrate.model.database.InventoryProvider
import com.cliffracertech.bootycrate.model.database.ShoppingListItemDao
import com.cliffracertech.bootycrate.model.database.ShoppingListProvider
import com.cliffracertech.bootycrate.settings.PrefKeys
import com.cliffracertech.bootycrate.settings.edit
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.collectAsState
import com.cliffracertech.bootycrate.utils.enumPreferenceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/** A set of values representing the state of a combo change sorting method and delete button. */
enum class ChangeSortDeleteButtonState {
    /** The button is visible with a change sort icon. */
    ChangeSort,
    /** The button is visible with a delete icon. */
    Delete,
    /** The button is invisible. */
    Invisible;

    val isChangeSort get() = this == ChangeSort
    val isDelete get() = this == Delete
    val isInvisible get() = this == Invisible
}

@HiltViewModel class ActionBarViewModel(
    private val dataStore: DataStore<Preferences>,
    private val shoppingListDao: ShoppingListItemDao,
    private val inventoryDao: InventoryItemDao,
    itemGroupDao: ItemGroupDao,
    private val navigationState: NavigationState,
    private val messageHandler: MessageHandler,
    private val searchQueryState: MutableStateFlow<String?>,
    private val shoppingListSelection: SelectionState,
    private val inventorySelection: SelectionState,
    coroutineScope: CoroutineScope
) : ViewModel(coroutineScope),
    ShoppingListProvider by DefaultShoppingListProvider(
        searchQueryState, shoppingListDao, dataStore, coroutineScope),
    InventoryProvider by DefaultInventoryProvider(
        searchQueryState, inventoryDao, dataStore, coroutineScope)
{
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        shoppingListDao: ShoppingListItemDao,
        inventoryDao: InventoryItemDao,
        itemGroupDao: ItemGroupDao,
        navigationState: NavigationState,
        messageHandler: MessageHandler,
        @SharedState.SearchQuery
        searchQueryState: MutableStateFlow<String?>,
        @SharedState.ShoppingListVolatileState
        shoppingListSelection: SelectionState,
        @SharedState.InventoryVolatileState
        inventorySelection: SelectionState,
    ) : this(dataStore, shoppingListDao, inventoryDao, itemGroupDao,
             navigationState, messageHandler, searchQueryState,
             shoppingListSelection, inventorySelection, viewModelScope())

    private val _searchQuery by searchQueryState.collectAsState(null, coroutineScope)

    private val visibleScreen by navigationState::visibleScreen

    private val selectedItemCount by derivedStateOf {
        when {
            visibleScreen.isShoppingList -> shoppingListSelection.size
            visibleScreen.isInventory ->    inventorySelection.size
            else ->                         0
        }
    }
    private val selectedItemGroups by
        itemGroupDao.getSelectedGroups().collectAsState(emptyList(), coroutineScope)

    private val _intents = MutableSharedFlow<Intent>(
        replay = 0, extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val intents = _intents.asSharedFlow()

    val showBackButton by derivedStateOf {
        visibleScreen.isAppSettings ||
        _searchQuery != null ||
        selectedItemCount != 0
    }

    fun onBackPressed() = when {
        selectedItemCount > 0 -> {
            if (visibleScreen.isShoppingList)
                shoppingListSelection.clear()
            if (visibleScreen.isInventory)
                inventorySelection.clear()
            true
        } _searchQuery != null -> {
            searchQueryState.value = null
            true
        } else -> false
    }


    val title by derivedStateOf { when {
        visibleScreen.isAppSettings ->
            StringResource(R.string.settings_description)
        selectedItemCount == 1 ->
            StringResource(R.string.single_selection_description)
        selectedItemCount > 1 ->
            StringResource(R.string.multi_selection_description, selectedItemCount)
        else -> when(selectedItemGroups.size) {
            0 ->    StringResource("")
            1 ->    StringResource(selectedItemGroups.first().name)
            else -> StringResource(R.string.multiple_selected_item_groups_description)
        }
    }}

    val searchQuery: String? by derivedStateOf {
        if (selectedItemCount > 0) null else _searchQuery
    }

    val showSearchButton by derivedStateOf {
        !visibleScreen.isAppSettings && selectedItemCount == 0
    }

    fun onSearchButtonClick() {
        searchQueryState.value = if (_searchQuery == null) "" else null
    }

    fun onSearchQueryChangeRequest(newQuery: String) {
        searchQueryState.value = newQuery
    }


    private val sortKey = intPreferencesKey(PrefKeys.itemSort)
    val sort by dataStore.enumPreferenceState(sortKey, coroutineScope, ListItem.Sort.Color)
    fun onSortOptionClick(sort: ListItem.Sort) =
        dataStore.edit(sortKey, sort.ordinal, coroutineScope)

    val changeSortDeleteButtonState by derivedStateOf { when {
        visibleScreen.isAppSettings -> ChangeSortDeleteButtonState.Invisible
        selectedItemCount > 0 ->       ChangeSortDeleteButtonState.Delete
        else ->                        ChangeSortDeleteButtonState.ChangeSort
    }}

    fun onDeleteButtonClick() {
        val screen = visibleScreen
        if (screen.isAppSettings) return
        coroutineScope.launch {
            val itemCount = selectedItemCount

            if (screen.isShoppingList)
                shoppingListDao.delete(shoppingListSelection.ids)
            else inventoryDao.delete(inventorySelection.ids)

            messageHandler.postItemsDeletedMessage(
                count = itemCount,
                onUndo = {
                    if (screen.isShoppingList)
                        coroutineScope.launch { shoppingListDao.undoDelete() }
                    else coroutineScope.launch { inventoryDao.undoDelete() }
                }, onTimeout = {
                    coroutineScope.launch {
                        if (screen.isShoppingList)
                            shoppingListDao.emptyTrash()
                        else inventoryDao.emptyTrash()
                    }
                })
        }
    }


    val showMoreOptionsButton by derivedStateOf {
        !visibleScreen.isAppSettings
    }

    val addToInventoryActionVisible by derivedStateOf {
        shoppingListSelection.size > 0
    }
    val addToShoppingListActionVisible by derivedStateOf {
        inventorySelection.size > 0
    }
    val checkAllActionVisible by derivedStateOf {
        visibleScreen.isShoppingList
    }
    val uncheckAllActionVisible by ::checkAllActionVisible

    val selectAllActionVisible = true
    val shareActionVisible = true

    fun onAddToInventoryClick() {
        coroutineScope.launch {
            inventoryDao.addFromShoppingListItems(shoppingListSelection.ids)
            shoppingListSelection.clear()
        }
    }
    fun onAddToShoppingListClick() {
        coroutineScope.launch {
            shoppingListDao.addFromInventoryItems(inventorySelection.ids)
            inventorySelection.clear()
        }
    }
    fun onCheckAllClick() {
        coroutineScope.launch { shoppingListDao.checkAllVisibleItems() }
    }
    fun onUncheckAllClick() {
        coroutineScope.launch { shoppingListDao.uncheckAllVisibleItems() }
    }
    fun onSelectAllClick() {
        if (visibleScreen.isShoppingList) {
            val shoppingList = this.shoppingList ?: return
            shoppingListSelection.addAll(shoppingList.map { it.id })
        } else if (visibleScreen.isInventory) {
            val inventory = this.inventory ?: return
            inventorySelection.addAll(inventory.map { it.id })
        }
    }
    fun onShareClick() {
        val screen = visibleScreen
        if (!screen.isShoppingList && !screen.isInventory)
            return
        coroutineScope.launch {
            val allItems = if (screen.isInventory) inventory
                           else                    shoppingList
            val selection = if (screen.isShoppingList) shoppingListSelection
                            else                       inventorySelection
            val items = if (selection.isEmpty) allItems
                        else allItems?.filter { it.id in selection }
                                      .also { selection.clear() }

            if (items?.isEmpty() != false) {
                val collectionNameResId =
                    if (screen.isInventory) R.string.shopping_list_description
                    else                    R.string.inventory_description
                messageHandler.postMessage(StringResource(
                    R.string.empty_list_message, collectionNameResId))
            } else {
                val message = StringJoiner("\n").apply {
                        for (item in items)
                            add(item.toUserFacingString())
                    }.toString()
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, message)
                intent.type = "text/plain"
                val chooserIntent = Intent.createChooser(intent, null)
                _intents.tryEmit(chooserIntent)
            }
        }
    }
}