/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.model.SearchQueryState
import com.cliffracertech.bootycrate.model.database.ItemDao
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.enumPreferenceState
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
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

fun <T> Flow<T>.collectAsState(initialValue: T, scope: CoroutineScope): State<T> {
    val state = mutableStateOf(initialValue)
    onEach { state.value = it }.launchIn(scope)
    return state
}

fun <T> StateFlow<T>.collectAsState(scope: CoroutineScope): State<T> =
    collectAsState(value, scope)

@HiltViewModel class ActionBarViewModel(
    context: Context,
    private val itemDao: ItemDao,
    itemGroupDao: ItemGroupDao,
    private val navigationState: NavigationState,
    private val messageHandler: MessageHandler,
    private val searchQueryState: SearchQueryState,
    coroutineScope: CoroutineScope?
) : ViewModel() {

    @Inject constructor(
        @ApplicationContext
        context: Context,
        itemDao: ItemDao,
        itemGroupDao: ItemGroupDao,
        navigationState: NavigationState,
        messageHandler: MessageHandler,
        searchQueryState: SearchQueryState,
    ) : this(context, itemDao, itemGroupDao, navigationState,
             messageHandler, searchQueryState, null)

    private val scope = coroutineScope ?: viewModelScope
    private val dataStore = context.dataStore
    private val currentScreen by navigationState.visibleScreen.collectAsState(scope)
    private val _searchQuery by searchQueryState.query.collectAsState(scope)
    private val selectedShoppingListItemCount by
        itemDao.getSelectedShoppingListItemCount().collectAsState(0, scope)
    private val selectedInventoryItemCount by
        itemDao.getSelectedInventoryItemCount().collectAsState(0, scope)
    private val selectedItemCount by derivedStateOf {
        when {
            currentScreen.isShoppingList -> selectedShoppingListItemCount
            currentScreen.isInventory ->    selectedInventoryItemCount
            else -> 0
        }
    }
    private val selectedItemGroups by
        itemGroupDao.getSelectedGroups().collectAsState(emptyList(), scope)
    private val sortByCheckedKey = booleanPreferencesKey(
        context.getString(R.string.pref_sort_by_checked_key))
    private val _intents = MutableSharedFlow<Intent>(
        replay = 0, extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val intents = _intents.asSharedFlow()

    val showBackButton by derivedStateOf {
        currentScreen.isAppSettings || _searchQuery != null || selectedItemCount != 0
    }

    fun onBackPressed() = when {
        selectedItemCount > 0 -> {
            if (navigationState.visibleScreen.value.isShoppingList)
                scope.launch { itemDao.clearShoppingListSelection() }
            if (navigationState.visibleScreen.value.isInventory)
                scope.launch { itemDao.clearInventorySelection() }
            true
        } _searchQuery != null -> {
            onSearchQueryChangeRequest(null)
            true
        } else -> false
    }


    val title by derivedStateOf { when {
        currentScreen.isAppSettings ->
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
        if (selectedItemCount != 0) null else _searchQuery
    }

    val showSearchButton by derivedStateOf {
        !currentScreen.isAppSettings && selectedItemCount == 0
    }

    fun onSearchButtonClick() {
        val newQuery = if (_searchQuery == null) "" else null
        onSearchQueryChangeRequest(newQuery)
    }

    fun onSearchQueryChangeRequest(newQuery: String?) {
        searchQueryState.query.value = newQuery
    }


    private val sortKey = intPreferencesKey(context.getString(R.string.pref_item_sort_key))
    val sort by dataStore.enumPreferenceState(sortKey, scope, ListItem.Sort.Color)
    fun onSortOptionClick(sort: ListItem.Sort) {
        scope.launch { dataStore.edit { it[sortKey] = sort.ordinal } }
    }

    val changeSortDeleteButtonState by derivedStateOf { when {
        currentScreen.isAppSettings -> ChangeSortDeleteButtonState.Invisible
        selectedItemCount > 0 ->       ChangeSortDeleteButtonState.Delete
        else ->                        ChangeSortDeleteButtonState.ChangeSort
    }}


    fun onDeleteButtonClick() {
        val screen = currentScreen
        if (screen.isAppSettings) return
        scope.launch {
            val itemCount = selectedItemCount

            if (screen.isShoppingList)
                itemDao.deleteSelectedShoppingListItems()
            else itemDao.deleteSelectedInventoryItems()

            messageHandler.postItemsDeletedMessage(
                count = itemCount,
                onUndo = {
                    if (screen.isShoppingList)
                        scope.launch { itemDao.undoDeleteShoppingListItems() }
                    else scope.launch { itemDao.undoDeleteInventoryItems() }
                }, onDismiss = { reason: Int ->
                    if (reason != DISMISS_EVENT_ACTION && reason != DISMISS_EVENT_CONSECUTIVE)
                        scope.launch {
                            if (screen.isShoppingList)
                                itemDao.emptyShoppingListTrash()
                            else itemDao.emptyInventoryTrash()
                        }
                })
        }
    }


    val showMoreOptionsButton by derivedStateOf {
        !currentScreen.isAppSettings
    }


    val addToInventoryActionVisible by itemDao.getSelectedShoppingListItemCount()
        .map { it > 0 }.collectAsState(false, scope)
    val addToShoppingListActionVisible by itemDao.getSelectedInventoryItemCount()
        .map { it > 0 }.collectAsState(false, scope)
    var checkAllActionVisible by mutableStateOf(true)
        private set
    var uncheckAllActionVisible by mutableStateOf(true)
        private set
    init {
        navigationState.visibleScreen.onEach {
            checkAllActionVisible = it.isShoppingList
            uncheckAllActionVisible = it.isShoppingList
        }.launchIn(scope)
    }
    val selectAllActionVisible = true
    val shareActionVisible = true

    fun onAddToInventoryClick() {
        scope.launch { itemDao.addToInventoryFromSelectedShoppingListItems() }
    }
    fun onAddToShoppingListClick() {
        scope.launch { itemDao.addToShoppingListFromSelectedInventoryItems() }
    }
    fun onCheckAllClick() {
        scope.launch { itemDao.checkAllShoppingListItems() }
    }
    fun onUncheckAllClick() {
        scope.launch { itemDao.uncheckAllShoppingListItems() }
    }
    fun onSelectAllClick() {
        if (currentScreen.isShoppingList)
            scope.launch { itemDao.selectAllShoppingListItems() }
        if (currentScreen.isInventory)
            scope.launch { itemDao.selectAllInventoryItems() }
    }
    fun onShareClick() { scope.launch {
        val allItems = when {
            currentScreen.isShoppingList -> {
                val sortByChecked = dataStore.data
                    .map { it[sortByCheckedKey] }.first()
                itemDao.getShoppingList(sort, _searchQuery, sortByChecked == false).first()
            }
            currentScreen.isInventory ->
                itemDao.getInventory(sort, _searchQuery).first()
            else ->
                emptyList()
        }
        val selectionIsEmpty = selectedItemCount == 0
        val items = if (selectionIsEmpty) allItems
        else allItems.filter { it.isSelected }

        if (items.isEmpty()) {
            val collectionNameResId = when {
                currentScreen.isShoppingList -> R.string.shopping_list_description
                currentScreen.isInventory -> R.string.inventory_description
                else -> 0
            }
            if (collectionNameResId != 0)
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
            _intents.tryEmit(intent)
        }
    }}

}