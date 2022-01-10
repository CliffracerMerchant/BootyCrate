/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.ListItem
import com.cliffracertech.bootycrate.dlog
import com.cliffracertech.bootycrate.fragment.AppSettingsFragment
import com.cliffracertech.bootycrate.fragment.InventoryFragment
import com.cliffracertech.bootycrate.fragment.ShoppingListFragment
import com.cliffracertech.bootycrate.utils.mutableEnumPreferenceFlow
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * An AndroidViewModel for activities that can display messages to the user, e.g. through a SnackBar.
 *
 * Because many activities need to display popup messages to the user,
 * MessageViewModel is open so that it can be inherited by a more activity
 * specific view model. New messages can be posted using the postMessage
 * function. Activities that use MessageViewModel can listen to the SharedFlow
 * member flow for new messages. The function postItemsDeletedMessage is
 * provided for convenience for the common use case of showing an X item(s)
 * deleted message after items are deleted from a list, along with an undo
 * action.
 */
open class MessageViewModel(private val app: Application) : AndroidViewModel(app) {
    /**
     * A message to be displayed to the user.
     * @param text The text of the message.
     * @param actionText The text of the message action, if any.
     * @param onActionClick The callback that will be invoked if the message action is clicked.
     * @param onDismiss The callback that will be invoked when the message is dismissed. The
     * int parameter will be equal to a value of BaseTransientBottomBar.BaseCallback.DismissEvent.
     */
    data class Message(
        val text: String,
        val actionText: String? = null,
        val onActionClick: (() -> Unit)? = null,
        val onDismiss: ((Int) -> Unit)? = null)

    private val _messages = MutableSharedFlow<Message>(extraBufferCapacity = 1,
                                                       onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages = _messages.asSharedFlow()

    /** Post the message to the message queue. */
    fun postMessage(message: Message) =  _messages.tryEmit(message)

    /** Post the message described by the parameters to the message queue. */
    fun postMessage(
        text: String,
        actionText: String? = null,
        onActionClick: (() -> Unit)? = null,
        onDismiss: ((Int) -> Unit)? = null
    ) = postMessage(Message(text, actionText, onActionClick, onDismiss))

    private var totalDeletedItemCount: Int = 0

    data class DeletedItemsMessage(
        val count: Int,
        val onUndo: () -> Unit,
        val onDismiss: ((Int) -> Unit)? = null)

    /**
     * Post a message for the common use case of deleting items in a list while
     * providing an undo action. If postItemsDeletedMessage is called repeatedly
     * before the message is dismissed, the count of deleted items will continue
     * to accumulate.
     * @param count The number of items that were deleted in the deletion that
     * led to this call of postItemsDeletedMessage. Callers that make successive
     * calls to postItemsDeletedMessage do not need to accumulate this number
     * manually, as this is done automatically if a new call is made before the
     * message is dismissed.
     * @param onUndo The callback that will be invoked if the undo action is performed.
     * @param onDismiss The callback that will be invoked when the message is dismissed.
     */
    fun postItemsDeletedMessage(
        count: Int,
        onUndo: () -> Unit,
        onDismiss: ((Int) -> Unit)? = null
    ) {
        totalDeletedItemCount += count
        val text = app.getString(R.string.delete_snackbar_text, totalDeletedItemCount)
        val actionText = app.getString(R.string.undo_description)
        val onDismissPrivate = { dismissCode: Int ->
            if (dismissCode != DISMISS_EVENT_CONSECUTIVE)
                totalDeletedItemCount = 0
            onDismiss?.invoke(dismissCode)
            Unit
        }
        postMessage(text, actionText, onUndo, onDismissPrivate)
    }

    fun postItemsDeletedMessage(message: DeletedItemsMessage) =
        postItemsDeletedMessage(message.count, message.onUndo, message.onDismiss)
}

/** A class representing a title for an action bar. */
sealed class TitleState(val title: String)
/** A TitleState that represents the title for an active action mode. */
class ActionModeState(actionModeTitle: String) : TitleState(actionModeTitle)
/** A TitleState that represents the title for an active search query. */
class SearchQueryState(searchQuery: String) : TitleState(searchQuery)
/** A TitleState that represents the title for an active fragment / activity. */
class RegularTitleState(title: String) : TitleState(title)

/** A set of values representing the state of a combo change sorting method and delete button. */
enum class ChangeSortButtonState {
    /** The button is visible with a change sort icon. */
    Visible,
    /** The button is visible with a delete icon. */
    MorphedToDelete,
    /** The button is invisible. */
    Invisible
}

/** A set of values representing the state of a combo search / close button. */
enum class SearchButtonState {
    /** The button is visible with a search icon. */
    Visible,
    /** The button is visible with a close icon. */
    MorphedToClose,
    /** The button is invisible. */
    Invisible,
}

class MainActivityViewModel(private val app: Application) : MessageViewModel(app) {
    private val itemDao = BootyCrateDatabase.get(app).itemDao()
    private val itemGroupDao = BootyCrateDatabase.get(app).itemGroupDao()

    enum class Fragment { ShoppingList, Inventory, AppSettings }
    val currentFragment = MutableStateFlow<Fragment?>(null)
    fun onNewFragmentSelected(fragment: androidx.fragment.app.Fragment) {
        currentFragment.value = when (fragment) {
            is ShoppingListFragment -> Fragment.ShoppingList
            is InventoryFragment ->    Fragment.Inventory
            is AppSettingsFragment ->  Fragment.AppSettings
            else ->                    null
        }
    }



    private val _searchFilter = MutableStateFlow<String?>(null)
    val searchFilter = _searchFilter.asStateFlow()



    private val selectedShoppingListItemCount = itemDao.getSelectedShoppingListItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val selectedInventoryItemCount = itemDao.getSelectedInventoryItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val selectedItemCount = currentFragment.transformLatest { fragment ->
        when (fragment) {
            Fragment.ShoppingList -> emitAll(selectedShoppingListItemCount)
            Fragment.Inventory ->    emitAll(selectedInventoryItemCount)
            else ->                  emit(0)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)



    val backButtonIsVisible = combine(
        currentFragment,
        selectedItemCount,
        _searchFilter
    ) { fragment, selectedItemCount, filter ->
        fragment == Fragment.AppSettings || filter != null || selectedItemCount != 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
        .onEach { dlog("backButtonIsVisible emitted $it")}

    fun onBackPressed() = when {
        selectedItemCount.value > 0 -> {
            if (currentFragment.value == Fragment.ShoppingList)
                viewModelScope.launch { itemDao.clearShoppingListSelection() }
            if (currentFragment.value == Fragment.Inventory)
                viewModelScope.launch { itemDao.clearInventorySelection() }
            true
        } _searchFilter.value != null -> {
            _searchFilter.value = null
            true
        } else -> false
    }



    fun actionModeTitle(selectedItemCount: Int) =
        app.getString(R.string.action_mode_title, selectedItemCount)

    private val nameForMultiSelection = app.getString(R.string.multiple_selected_item_groups_description)
    private val selectedItemGroupName = itemGroupDao.getSelectedGroups().map {
        if (it.size == 1) it.first().name
        else nameForMultiSelection
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    val titleState = combine(
        currentFragment,
        selectedItemCount,
        _searchFilter,
        selectedItemGroupName
    ) { fragment, selectedItemCount, filter, selectedItemGroupName ->
        when {
            fragment == Fragment.AppSettings ->
                RegularTitleState(app.getString(R.string.settings_description))
            selectedItemCount > 0 ->
                ActionModeState(actionModeTitle(selectedItemCount))
            filter != null -> SearchQueryState(filter)
            else -> RegularTitleState(selectedItemGroupName)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RegularTitleState(""))


    val searchButtonState = combine(
        currentFragment,
        selectedItemCount,
        _searchFilter
    ) { fragment, selectedItemCount, searchFilter ->
        when {
            fragment == Fragment.AppSettings || selectedItemCount > 0 ->
                SearchButtonState.Invisible
            searchFilter != null ->
                SearchButtonState.MorphedToClose
            else ->
                SearchButtonState.Visible
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SearchButtonState.Visible)

    fun onSearchButtonClick() {
        _searchFilter.value = if (_searchFilter.value == null) ""
                             else null
    }

    fun onSearchFilterChangeRequest(newFilter: CharSequence? = null) {
        _searchFilter.value = newFilter?.toString()
    }



    private val sort = app.dataStore.mutableEnumPreferenceFlow(
        intPreferencesKey("item_sort"), viewModelScope, ListItem.Sort.Color)

    val sortIndex = sort.map { it.ordinal }
        .onEach { dlog("sortIndex emitted $it") }

    val changeSortButtonState = combine(currentFragment, titleState, sort) { fragment, titleState, sort ->
        when {
            titleState is ActionModeState ->
                ChangeSortButtonState.MorphedToDelete
            fragment == Fragment.AppSettings ->
                ChangeSortButtonState.Invisible
            else ->
                ChangeSortButtonState.Visible
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ChangeSortButtonState.Visible)
        .onEach { dlog("changeSortButtonState emitted $it") }

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
        val fragment = currentFragment.value
        if (fragment != Fragment.AppSettings) viewModelScope.launch {
            val itemCount = selectedItemCount.value
            if (fragment == Fragment.ShoppingList)
                itemDao.deleteSelectedShoppingListItems()
            if (fragment == Fragment.Inventory)
                itemDao.deleteSelectedInventoryItems()
            val onUndo = {
                viewModelScope.launch {
                    if (fragment == Fragment.ShoppingList)
                        itemDao.undoDeleteShoppingListItems()
                    if (fragment == Fragment.Inventory)
                        itemDao.undoDeleteInventoryItems()
                }; Unit
            }
            val onDismiss = { dismissCode: Int ->
                if (dismissCode != DISMISS_EVENT_ACTION &&
                    dismissCode != DISMISS_EVENT_CONSECUTIVE)
                {
                    viewModelScope.launch {
                        if (fragment == Fragment.ShoppingList)
                            itemDao.emptyShoppingListTrash()
                        if (fragment == Fragment.Inventory)
                            itemDao.emptyInventoryTrash()
                    }
                }
            }
            postItemsDeletedMessage(itemCount, onUndo, onDismiss)
        }
    }



    val  moreOptionsButtonVisible = currentFragment.map {
        it != Fragment.AppSettings
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
        .onEach { dlog("moreOptionsButtonVisible emitted $it") }
}