/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.actionbar

import android.content.Intent
import androidx.annotation.StringRes
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
import com.cliffracertech.bootycrate.model.database.DefaultInventoryProvider
import com.cliffracertech.bootycrate.model.database.DefaultShoppingListProvider
import com.cliffracertech.bootycrate.model.database.InventoryItemDao
import com.cliffracertech.bootycrate.model.database.InventoryProvider
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItemDao
import com.cliffracertech.bootycrate.model.database.ShoppingListProvider
import com.cliffracertech.bootycrate.settings.PrefKeys
import com.cliffracertech.bootycrate.settings.edit
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.collectAsState
import com.cliffracertech.bootycrate.utils.enumPreferenceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * An option within an options menu
 *
 * @param titleResId The id of the string resource to use as the item's title
 * @param onClick The callback that should be invoked when the item is clicked
 */
data class OptionsMenuItem(
    @StringRes val titleResId: Int,
    val onClick: () -> Unit)

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

            if (screen.isShoppingList) {
                shoppingListDao.delete(shoppingListSelection.ids)
                shoppingListSelection.clear()
            } else {
                inventoryDao.delete(inventorySelection.ids)
                inventorySelection.clear()
            }
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

    private val addToInventoryItem =
        OptionsMenuItem(R.string.add_to_inventory_description) {
            coroutineScope.launch {
                inventoryDao.addFromShoppingListItems(shoppingListSelection.ids)
                shoppingListSelection.clear()
            }
        }
    private val addToShoppingListItem =
        OptionsMenuItem(R.string.add_to_shopping_list_description) {
            coroutineScope.launch {
                shoppingListDao.addFromInventoryItems(inventorySelection.ids)
                inventorySelection.clear()
            }
        }
    private val checkAllItem = OptionsMenuItem(R.string.check_all_description) {
        coroutineScope.launch { shoppingListDao.checkAllVisibleItems() }
    }
    private val uncheckAllItem = OptionsMenuItem(R.string.uncheck_all_description) {
        coroutineScope.launch { shoppingListDao.uncheckAllVisibleItems() }
    }
    private val selectAllItem = OptionsMenuItem(R.string.select_all_description) {
        if (visibleScreen.isShoppingList) shoppingList?.let {
            shoppingListSelection.addAll(it.map(ListItem::id))
        } else if (visibleScreen.isInventory) inventory?.let {
            inventorySelection.addAll(it.map(ListItem::id))
        }
    }
    private val shareItem = OptionsMenuItem(R.string.share_description) {
        val screen = visibleScreen
        if (!screen.isShoppingList && !screen.isInventory)
            return@OptionsMenuItem
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

    private val checkedItemCount by shoppingListDao
        .getVisibleCheckedItemCount()
        .collectAsState(0, coroutineScope)

    /** The list of options menu items that should be shown. If the
     * value is null, the button to open the options menu should be
     * hidden. Otherwise, a drop down menu item should be shown for
     * each [OptionsMenuItem] in the list. */
    val optionsMenuItems by derivedStateOf {
        val shoppingListSize = shoppingList?.size ?: 0
        val inventorySize = inventory?.size ?: 0

        if (visibleScreen.isAppSettings ||
           (visibleScreen.isShoppingList && shoppingListSize == 0) ||
           (visibleScreen.isInventory && inventorySize == 0))
                null
        else mutableListOf<OptionsMenuItem>().apply {
            if (visibleScreen.isShoppingList) {
                if (shoppingListSelection.isNotEmpty)
                    add(addToInventoryItem)
                if (shoppingListSelection.size < shoppingListSize)
                    add(selectAllItem)
                if (checkedItemCount < shoppingListSize)
                    add(checkAllItem)
                if (checkedItemCount > 0)
                    add(uncheckAllItem)
            } else { // visibleScreen.isInventory
                if (inventorySelection.isNotEmpty)
                    add(addToShoppingListItem)
                if (inventorySelection.size < inventorySize)
                    add(selectAllItem)
            }
            add(shareItem)
        }.toImmutableList()
    }
}