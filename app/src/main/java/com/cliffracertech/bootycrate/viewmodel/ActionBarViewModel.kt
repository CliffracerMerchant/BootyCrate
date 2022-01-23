/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.BootyCrateApplication
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.database.ItemDao
import com.cliffracertech.bootycrate.database.ItemGroupDao
import com.cliffracertech.bootycrate.database.ListItem
import com.cliffracertech.bootycrate.utils.mutableEnumPreferenceFlow
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A state holder for a search query entry. */
@ActivityRetainedScoped
class SearchQueryState @Inject constructor() {
    val query = MutableStateFlow<String?>(null)
}


/**
 * A state holder representing the navigation state for a MainActivity instance.
 *
 * MainActivityNavigationState wraps an instance of NavigationState and adds
 * the Flow activeFragment, which represents the current visible fragment in
 * the MainActivity instance from among the possible values of ShoppingListFragment,
 * InventoryFragment, or another fragment which covers these two.
 */
@ActivityRetainedScoped
class MainActivityNavigationState @Inject constructor(
    state: ReadOnlyNavigationState
) {
    /** The currently selected nav item id in the MainActivity instance. */
    val navViewSelectedItemId = state.navViewSelectedItemId

    /** An enum class whose values describe the possible fragments for an
     * instance of MainActivity: a ShoppingListFragment, an InventoryFragment,
     * or a secondary fragment that covers the primary fragment (e.g. an app
     * settings fragment). */
    enum class Fragment { ShoppingList, Inventory, Other;
        val isShoppingList get() = this == ShoppingList
        val isInventory get() = this == Inventory
        val isOther get() = this == Other
    }

    /** The current visible fragment in the MainActivity instance. */
    val activeFragment = combine(
        state.navViewSelectedItemId,
        state.backStackSize
    ) { navItemId, backStackSize -> when {
        backStackSize > 0 ->                    Fragment.Other
        navItemId == R.id.shoppingListButton -> Fragment.ShoppingList
        navItemId == R.id.inventoryButton ->    Fragment.Inventory
        else ->                                 Fragment.Other
    }}.stateIn(BootyCrateApplication.coroutineScope,
               SharingStarted.Eagerly, Fragment.ShoppingList)
}


/** A state holder representing a title for an action bar. */
sealed class TitleState(val title: String) {
    /** A TitleState that represents the title for an active action mode. */
    class ActionMode(actionModeTitle: String) : TitleState(actionModeTitle)
    /** A TitleState that represents the title for an active search query. */
    class SearchQuery(searchQuery: String) : TitleState(searchQuery)
    /** A TitleState that represents the title for an active fragment / activity. */
    class NormalTitle(title: String) : TitleState(title)

    val isActionMode get() = this is ActionMode
    val isSearchQuery get() = this is SearchQuery
    val isNormalTitle get() = this is NormalTitle
}

/** A set of values representing the state of a combo search/close button. */
enum class SearchButtonState {
    /** The button is visible with a search icon. */
    Visible,
    /** The button is visible with a close icon. */
    MorphedToClose,
    /** The button is invisible. */
    Invisible;

    val isVisible get() = this == Visible
    val isMorphedToClose get() = this == MorphedToClose
    val isInvisible get() = this == Invisible
}

/** A set of values representing the state of a combo change sorting method and delete button. */
sealed class ChangeSortButtonState {
    /** The button is visible with a change sort icon. */
    data class Visible(val selectedIndex: Int) : ChangeSortButtonState()
    /** The button is visible with a delete icon. */
    object MorphedToDelete : ChangeSortButtonState()
    /** The button is invisible. */
    object Invisible : ChangeSortButtonState()

    val isVisible get() = this is Visible
    val isMorphedToDelete get() = this is MorphedToDelete
    val isInvisible get() = this is Invisible
}


@HiltViewModel
class ActionBarViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val itemDao: ItemDao,
    itemGroupDao: ItemGroupDao,
    private val navigationState: MainActivityNavigationState,
    private val messageHandler: MessageHandler,
    private val searchQueryState: SearchQueryState
) : ViewModel() {

    private val searchQuery = searchQueryState.query.asStateFlow()
    fun onSearchQueryChangeRequest(newQuery: CharSequence?) {
        searchQueryState.query.value = newQuery?.toString()
    }

    private val selectedShoppingListItemCount = itemDao.getSelectedShoppingListItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val selectedInventoryItemCount = itemDao.getSelectedInventoryItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val selectedItemCount = navigationState.activeFragment.transformLatest { when {
        it.isShoppingList -> emitAll(selectedShoppingListItemCount)
        it.isInventory ->    emitAll(selectedInventoryItemCount)
        else ->              emit(0)
    }}.stateIn(viewModelScope, SharingStarted.Eagerly, 0)


    val backButtonIsVisible = combine(
        navigationState.activeFragment,
        selectedItemCount,
        searchQuery
    ) { fragment, selectedItemCount, filter ->
        fragment.isOther || filter != null || selectedItemCount != 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), false)

    fun onBackPressed() = when {
        selectedItemCount.value > 0 -> {
            if (navigationState.activeFragment.value.isShoppingList)
                viewModelScope.launch { itemDao.clearShoppingListSelection() }
            if (navigationState.activeFragment.value.isInventory)
                viewModelScope.launch { itemDao.clearInventorySelection() }
            true
        } searchQuery.value != null -> {
            onSearchQueryChangeRequest(null)
            true
        } else -> false
    }


    private val nameForMultiSelection =
        context.getString(R.string.multiple_selected_item_groups_description)
    private val selectedItemGroupName = itemGroupDao.getSelectedGroups().map {
        if (it.size == 1) it.first().name
        else nameForMultiSelection
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), "")

    private val actionModeTitleBase = context.getString(R.string.action_mode_title)
    private fun actionModeTitle(selectedItemCount: Int) =
        String.format(actionModeTitleBase, selectedItemCount)
    private val settingsTitle = context.getString(R.string.settings_description)

    val titleState = combine(
        navigationState.activeFragment,
        selectedItemCount,
        searchQuery,
        selectedItemGroupName
    ) { fragment, selectedItemCount, query, selectedItemGroupName -> when {
        fragment.isOther ->      TitleState.NormalTitle(settingsTitle)
        selectedItemCount > 0 -> TitleState.ActionMode(actionModeTitle(selectedItemCount))
        query != null ->         TitleState.SearchQuery(query)
        else ->                  TitleState.NormalTitle(selectedItemGroupName)
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), TitleState.NormalTitle(""))


    val searchButtonState = combine(
        navigationState.activeFragment,
        selectedItemCount,
        searchQuery
    ) { fragment, selectedItemCount, query -> when {
        fragment.isOther || selectedItemCount > 0 -> SearchButtonState.Invisible
        query != null ->                             SearchButtonState.MorphedToClose
        else ->                                      SearchButtonState.Visible
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), SearchButtonState.Visible)

    fun onSearchButtonClick() {
        val newQuery = if (searchQuery.value == null) "" else null
        onSearchQueryChangeRequest(newQuery)
    }


    private val sort = context.dataStore.mutableEnumPreferenceFlow(
        intPreferencesKey(context.getString(R.string.pref_item_sort_key)),
                          viewModelScope, ListItem.Sort.Color)

    val changeSortButtonState = combine(
        navigationState.activeFragment,
        titleState,
        sort
    ) { fragment, titleState, sort -> when {
        fragment.isOther ->        ChangeSortButtonState.Invisible
        titleState.isActionMode -> ChangeSortButtonState.MorphedToDelete
        else ->                    ChangeSortButtonState.Visible(sort.ordinal)
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000),
               ChangeSortButtonState.Visible(ListItem.Sort.Color.ordinal))

    fun onSortOptionSelected(menuItemId: Int): Boolean {
        sort.value = when (menuItemId) {
            R.id.color_option ->             ListItem.Sort.Color
            R.id.name_ascending_option ->    ListItem.Sort.NameAsc
            R.id.name_descending_option ->   ListItem.Sort.NameDesc
            R.id.amount_ascending_option ->  ListItem.Sort.AmountAsc
            R.id.amount_descending_option -> ListItem.Sort.AmountDesc
            else -> ListItem.Sort.Color
        }
        return true
    }


    fun onDeleteButtonClick() {
        val fragment = navigationState.activeFragment.value
        if (!fragment.isShoppingList && !fragment.isInventory) return
        viewModelScope.launch {
            val itemCount = selectedItemCount.value

            if (fragment.isShoppingList)
                itemDao.deleteSelectedShoppingListItems()
            else itemDao.deleteSelectedInventoryItems()

            val onUndo = {
                viewModelScope.launch {
                    if (fragment.isShoppingList)
                        itemDao.undoDeleteShoppingListItems()
                    else itemDao.undoDeleteInventoryItems()
                }; Unit
            }
            val onDismiss = { code: Int ->
                if (code != DISMISS_EVENT_ACTION && code != DISMISS_EVENT_CONSECUTIVE)
                    viewModelScope.launch {
                        if (fragment.isShoppingList)
                            itemDao.emptyShoppingListTrash()
                        else itemDao.emptyInventoryTrash()
                    }
            }
            messageHandler.postItemsDeletedMessage(itemCount, onUndo, onDismiss)
        }
    }


    val moreOptionsButtonVisible = navigationState.activeFragment
        .map { !it.isOther }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), true)
}