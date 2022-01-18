/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.ReadOnlyNavigationManager
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.database.ItemDao
import com.cliffracertech.bootycrate.database.ItemGroupDao
import com.cliffracertech.bootycrate.database.ListItem
import com.cliffracertech.bootycrate.fragment.AppSettingsFragment
import com.cliffracertech.bootycrate.fragment.InventoryFragment
import com.cliffracertech.bootycrate.fragment.ShoppingListFragment
import com.cliffracertech.bootycrate.utils.mutableEnumPreferenceFlow
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


val Fragment?.isShoppingList get() = this is ShoppingListFragment
val Fragment?.isInventory get() = this is InventoryFragment
val Fragment?.isAppSettings get() = this is AppSettingsFragment



@Module @InstallIn(ActivityRetainedComponent::class)
class SearchQueryState {
    val query = MutableStateFlow<String?>(null)
}



/** A class representing a title for an action bar. */
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

/** A set of values representing the state of a combo search / close button. */
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
class MainActivityViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val itemDao: ItemDao,
    itemGroupDao: ItemGroupDao,
    navManager: ReadOnlyNavigationManager,
    private val messenger: Messenger,
    private val searchQueryState: SearchQueryState
) : ViewModel() {

    val messages = messenger.messages

    val activeFragment = navManager.activeFragment

    private val searchQuery = searchQueryState.query.asStateFlow()
    fun onSearchQueryChangeRequest(newQuery: CharSequence?) {
        searchQueryState.query.value = newQuery?.toString()
    }

    private val selectedShoppingListItemCount = itemDao.getSelectedShoppingListItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val selectedInventoryItemCount = itemDao.getSelectedInventoryItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val selectedItemCount = activeFragment.transformLatest { when {
        it.isShoppingList -> emitAll(selectedShoppingListItemCount)
        it.isInventory ->    emitAll(selectedInventoryItemCount)
        else ->              emit(0)
    }}.stateIn(viewModelScope, SharingStarted.Eagerly, 0)


    val backButtonIsVisible = combine(
        activeFragment,
        selectedItemCount,
        searchQuery
    ) { fragment, selectedItemCount, filter ->
        fragment?.isAppSettings == true || filter != null || selectedItemCount != 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun onBackPressed() = when {
        selectedItemCount.value > 0 -> {
            if (activeFragment.value.isShoppingList)
                viewModelScope.launch { itemDao.clearShoppingListSelection() }
            if (activeFragment.value.isInventory)
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    private val actionModeTitleBase = context.getString(R.string.action_mode_title)
    private fun actionModeTitle(selectedItemCount: Int) =
        String.format(actionModeTitleBase, selectedItemCount)
    private val settingsTitle = context.getString(R.string.settings_description)

    val titleState = combine(
        activeFragment,
        selectedItemCount,
        searchQuery,
        selectedItemGroupName
    ) { fragment, selectedItemCount, query, selectedItemGroupName -> when {
        fragment.isAppSettings -> TitleState.NormalTitle(settingsTitle)
        selectedItemCount > 0 ->  TitleState.ActionMode(actionModeTitle(selectedItemCount))
        query != null ->          TitleState.SearchQuery(query)
        else ->                   TitleState.NormalTitle(selectedItemGroupName)
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), TitleState.NormalTitle(""))


    val searchButtonState = combine(
        activeFragment,
        selectedItemCount,
        searchQuery
    ) { fragment, selectedItemCount, query -> when {
        fragment.isAppSettings || selectedItemCount > 0 ->
            SearchButtonState.Invisible
        query != null ->
            SearchButtonState.MorphedToClose
        else ->
            SearchButtonState.Visible
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SearchButtonState.Visible)

    fun onSearchButtonClick() {
        val newQuery = if (searchQuery.value == null) "" else null
        onSearchQueryChangeRequest(newQuery)
    }


    private val sort = context.dataStore.mutableEnumPreferenceFlow(
        intPreferencesKey(context.getString(R.string.pref_item_sort_key)), viewModelScope, ListItem.Sort.Color)

    val changeSortButtonState = combine(
        activeFragment,
        titleState,
        sort
    ) { fragment, titleState, sort -> when {
        fragment.isAppSettings ->  ChangeSortButtonState.Invisible
        titleState.isActionMode -> ChangeSortButtonState.MorphedToDelete
        else ->                    ChangeSortButtonState.Visible(sort.ordinal)
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(),
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
        val fragment = activeFragment.value
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
            val onDismiss = { dismissCode: Int ->
                if (dismissCode != DISMISS_EVENT_ACTION && dismissCode != DISMISS_EVENT_CONSECUTIVE)
                    viewModelScope.launch {
                        if (fragment.isShoppingList)
                            itemDao.emptyShoppingListTrash()
                        else itemDao.emptyInventoryTrash()
                    }
            }
            messenger.postItemsDeletedMessage(itemCount, onUndo, onDismiss)
        }
    }


    val moreOptionsButtonVisible = activeFragment
        .map { !it.isAppSettings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(),
                 !activeFragment.value.isAppSettings)


    data class BottomAppBarState(val visible: Boolean, val checkoutButtonVisible: Boolean)

    val bottomAppBarState = activeFragment.map {
        BottomAppBarState(visible = !it.isAppSettings,
                          checkoutButtonVisible = it.isShoppingList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(),
              BottomAppBarState(visible = true, checkoutButtonVisible = true))


    private var oldShoppingListSize = 0
    val shoppingListSizeChange = itemDao.getShoppingListItemCount().map { shoppingListSize ->
        val change = shoppingListSize - oldShoppingListSize
        oldShoppingListSize = shoppingListSize
        if (activeFragment.value.isInventory) 0
        else                                  change
    }.drop(1).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
}