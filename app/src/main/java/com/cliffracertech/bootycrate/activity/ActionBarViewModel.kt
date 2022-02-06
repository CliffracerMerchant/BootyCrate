/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.model.MainActivityNavigationState
import com.cliffracertech.bootycrate.model.SearchQueryState
import com.cliffracertech.bootycrate.model.database.ItemDao
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.mutableEnumPreferenceFlow
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A state holder representing a title for an action bar. */
sealed class TitleState {
    /** A TitleState that represents the title for an active action mode. */
    class ActionMode(val titleRes: StringResource) : TitleState() {
        constructor(titleResId: Int, amount: Int): this(StringResource(titleResId, amount))
    }
    /** A TitleState that represents the title for an active search query. */
    class SearchQuery(val searchQuery: String) : TitleState()

    /** A TitleState that represents the title for an active fragment/activity. */
    class NormalTitle(val titleRes: StringResource) : TitleState() {
        constructor(titleResId: Int): this(StringResource(titleResId))
    }

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

    private val selectedItemCount = navigationState.visibleScreen.transformLatest { when {
        it.isShoppingList -> emitAll(selectedShoppingListItemCount)
        it.isInventory ->    emitAll(selectedInventoryItemCount)
        else ->              emit(0)
    }}.stateIn(viewModelScope, SharingStarted.Eagerly, 0)


    val backButtonIsVisible = combine(
        navigationState.visibleScreen,
        selectedItemCount,
        searchQuery
    ) { screen, selectedItemCount, filter ->
        screen.isOther || filter != null || selectedItemCount != 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), false)

    fun onBackPressed() = when {
        selectedItemCount.value > 0 -> {
            if (navigationState.visibleScreen.value.isShoppingList)
                viewModelScope.launch { itemDao.clearShoppingListSelection() }
            if (navigationState.visibleScreen.value.isInventory)
                viewModelScope.launch { itemDao.clearInventorySelection() }
            true
        } searchQuery.value != null -> {
            onSearchQueryChangeRequest(null)
            true
        } else -> false
    }


    private val selectedItemGroupName = itemGroupDao.getSelectedGroups().map {
        when (it.size) {
            0 ->    StringResource("")
            1 ->    StringResource(it.first().name)
            else -> StringResource(R.string.multiple_selected_item_groups_description)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), StringResource(""))

    val titleState = combine(
        navigationState.visibleScreen,
        selectedItemCount,
        searchQuery,
        selectedItemGroupName
    ) { screen, selectedItemCount, query, selectedItemGroupName -> when {
        screen.isOther ->        TitleState.NormalTitle(R.string.settings_description)
        selectedItemCount > 0 -> TitleState.ActionMode(R.string.action_mode_title, selectedItemCount)
        query != null ->         TitleState.SearchQuery(query)
        else ->                  TitleState.NormalTitle(selectedItemGroupName)
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000),
               TitleState.NormalTitle(StringResource("")))


    val searchButtonState = combine(
        navigationState.visibleScreen,
        selectedItemCount,
        searchQuery
    ) { screen, selectedItemCount, query -> when {
        screen.isOther || selectedItemCount > 0 -> SearchButtonState.Invisible
        query != null ->                           SearchButtonState.MorphedToClose
        else ->                                    SearchButtonState.Visible
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), SearchButtonState.Visible)

    fun onSearchButtonClick() {
        val newQuery = if (searchQuery.value == null) "" else null
        onSearchQueryChangeRequest(newQuery)
    }


    private val sort = context.dataStore.mutableEnumPreferenceFlow(
        intPreferencesKey(context.getString(R.string.pref_item_sort_key)),
                          viewModelScope, ListItem.Sort.Color)

    val changeSortButtonState = combine(
        navigationState.visibleScreen,
        titleState,
        sort
    ) { screen, titleState, sort -> when {
        screen.isOther ->          ChangeSortButtonState.Invisible
        titleState.isActionMode -> ChangeSortButtonState.MorphedToDelete
        else ->                    ChangeSortButtonState.Visible(sort.ordinal)
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000),
               ChangeSortButtonState.Visible(ListItem.Sort.Color.ordinal))

    fun onSortOptionClick(menuItemId: Int): Boolean {
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
        val screen = navigationState.visibleScreen.value
        if (!screen.isShoppingList && !screen.isInventory) return
        viewModelScope.launch {
            val itemCount = selectedItemCount.value

            if (screen.isShoppingList)
                itemDao.deleteSelectedShoppingListItems()
            else itemDao.deleteSelectedInventoryItems()

            val onUndo = {
                viewModelScope.launch {
                    if (screen.isShoppingList)
                        itemDao.undoDeleteShoppingListItems()
                    else itemDao.undoDeleteInventoryItems()
                }; Unit
            }
            val onDismiss = { reason: Int ->
                if (reason != DISMISS_EVENT_ACTION && reason != DISMISS_EVENT_CONSECUTIVE)
                    viewModelScope.launch {
                        if (screen.isShoppingList)
                            itemDao.emptyShoppingListTrash()
                        else itemDao.emptyInventoryTrash()
                    }
            }
            messageHandler.postItemsDeletedMessage(itemCount, onUndo, onDismiss)
        }
    }


    val moreOptionsButtonVisible = navigationState.visibleScreen
        .map { !it.isOther }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), true)


    val optionsMenuContent = combine(
        navigationState.visibleScreen,
        selectedShoppingListItemCount,
        selectedInventoryItemCount
    ) { screen, selectedShoppingListItemCount, selectedInventoryItemCount ->
        mutableListOf<Int>().apply {
            if (selectedShoppingListItemCount > 0)
                add(R.string.add_to_inventory_description)
            if (selectedInventoryItemCount > 0)
                add(R.string.add_to_shopping_list_description)
            if (screen.isShoppingList) {
                add(R.string.check_all_description)
                add(R.string.uncheck_all_description)
            }
            add(R.string.select_all_description)
            add(R.string.share_description)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000),
              listOf(R.string.select_all_description, R.string.share_description))
}