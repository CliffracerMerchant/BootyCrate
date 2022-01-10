/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ListItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.utils.getValue
import com.cliffracertech.bootycrate.utils.setValue
import com.cliffracertech.bootycrate.utils.preferenceFlow
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * An abstract AndroidViewModel to provide data and callbacks for a fragment
 * that displays a list of ListItem subclasses.
 *
 * ItemListViewModel listens to changes in the integer datastore value pointed
 * to by the key item_sort, maps this value to a value of ListItem.Sort, and
 * uses this sort value when it provides the list of ListItems. It also uses
 * the current value of the property searchFilter to filter the returned items.
 * As modifying the value of either of these parameters does not fall into the
 * purview of a fragment displaying a list of items, ItemListViewModel needs to
 * be manually informed of changes to the searchFilter, and writes to the
 * integer datastore preference need to be performed in another entity.
 *
 * The current sorting option and search filter are exposed as StateFlows to
 * subclasses through the properties sort and searchFilterFlow. Subclasses
 * should override the abstract property items to return a Flow<List<T>>
 * containing all of the items in the database that match the current sorting
 * option and search filter, as well as any additional sorting parameters that
 * they themselves add.
 *
 * ItemListViewModel or its subclasses might need to pass messages to the user,
 * e.g. about items being deleted. ItemListViewModel will call its properties
 * onMessage and onDeletedItemsMessage in these instances. These properties
 * will need to be set to functions that pass these messages to another entity
 * that is capable of displaying messages to the user.
 */
abstract class ItemListViewModel<T: ListItem>(app: Application):
    AndroidViewModel(app)
{
    protected val dao = BootyCrateDatabase.get(app).itemDao()
    private val itemGroupDao = BootyCrateDatabase.get(app).itemGroupDao()
    var onMessage: ((MessageViewModel.Message) -> Unit)? = null
    var onDeletedItemsMessage: ((MessageViewModel.DeletedItemsMessage) -> Unit)? = null

    protected val searchFilterFlow = MutableStateFlow<String?>(null)
    var searchFilter by searchFilterFlow

    protected val sort = app.dataStore.preferenceFlow(
        intPreferencesKey("item_sort"), ListItem.Sort.Color.ordinal)
        .map { ListItem.Sort.values().getOrElse(it) { ListItem.Sort.Color } }

    abstract val items: StateFlow<List<T>>

    fun onRenameItemRequest(id: Long, name: String) {
        viewModelScope.launch { dao.updateName(id, name) }
    }
    fun onChangeItemExtraInfoRequest(id: Long, extraInfo: String) {
        viewModelScope.launch { dao.updateExtraInfo(id, extraInfo) }
    }
    fun onChangeItemColorIndexRequest(id: Long, color: Int) {
        viewModelScope.launch { dao.updateColorIndex(id, color) }
    }
    abstract fun onChangeItemAmountRequest(id: Long, amount: Int)

    abstract fun onItemEditButtonClick(id: Long)

    abstract val selectedItemCount: StateFlow<Int>
    fun onItemClick(id: Long) {
        if (selectedItemCount.value > 0)
            toggleIsSelected(id)
    }
    fun onItemLongClick(id: Long) = toggleIsSelected(id)

    protected abstract fun toggleIsSelected(id: Long)
    abstract fun onSelectAllRequest()
    abstract fun onClearSelectionRequest()

    fun onItemSwipe(id: Long) { viewModelScope.launch {
        deleteItem(id)
        val message = MessageViewModel.DeletedItemsMessage(1, ::undoDelete) {
            if (it != DISMISS_EVENT_ACTION && it != DISMISS_EVENT_CONSECUTIVE)
                emptyTrash()
        }
        onDeletedItemsMessage?.invoke(message)
    }}

    protected abstract suspend fun deleteItem(id: Long)
    protected abstract fun emptyTrash()
    protected abstract fun undoDelete()
}



/**
 * An implementation of ItemListViewModel<ShoppingListItem>.
 *
 * ShoppingListViewModel adds callbacks to respond to clicks on the checkbox of
 * items, and to respond to a request to checkout. It also adds a StateFlow
 * property checkoutButtonIsEnabled, the current value of which indicates the
 * disabled/enabled state of the checkout button.
 */
class ShoppingListViewModel(app: Application) :
    ItemListViewModel<ShoppingListItem>(app)
{
    private val sortByCheckedKey = booleanPreferencesKey(app.getString(R.string.pref_sort_by_checked_key))
    private val sortByChecked = app.dataStore.preferenceFlow(
        key = sortByCheckedKey, defaultValue = false)

    override val items = combine(sort, searchFilterFlow, sortByChecked, dao::getShoppingList)
        .transformLatest { emitAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun onChangeItemAmountRequest(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateShoppingListAmount(id, amount) }
    }

    override fun onItemEditButtonClick(id: Long) {
        viewModelScope.launch { dao.toggleExpandedInShoppingList(id) }
    }

    val checkoutButtonIsEnabled = dao.getCheckedShoppingListItemsSize()
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun onItemCheckboxClicked(id: Long) {
        viewModelScope.launch { dao.toggleIsChecked(id) }
    }
    fun onCheckAllRequest() {
        viewModelScope.launch { dao.checkAllShoppingListItems() }
    }
    fun onUncheckAllRequest() {
        viewModelScope.launch { dao.uncheckAllShoppingListItems() }
    }
    fun onCheckoutRequest() {
        viewModelScope.launch { dao.checkout() }
    }


    override val selectedItemCount = dao.getSelectedShoppingListItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    override fun toggleIsSelected(id: Long) {
        viewModelScope.launch { dao.toggleSelectedInShoppingList(id) }
    }
    override fun onSelectAllRequest() {
        viewModelScope.launch { dao.selectAllShoppingListItems() }
    }
    override fun onClearSelectionRequest() {
        viewModelScope.launch { dao.clearShoppingListSelection() }
    }
    fun onAddFromSelectedInventoryItemsRequest() {
        viewModelScope.launch {
            dao.addToShoppingListFromSelectedInventoryItems()
            dao.clearInventorySelection()
        }
    }

    override suspend fun deleteItem(id: Long) = dao.deleteShoppingListItem(id)

    override fun emptyTrash() {
        viewModelScope.launch { dao.emptyShoppingListTrash() }
    }
    override fun undoDelete() {
        viewModelScope.launch { dao.undoDeleteShoppingListItems() }
    }
}



/** An implementation of ItemListViewModel<InventoryItem> that adds functions
 * to manipulate the autoAddToShoppingList and autoAddToShoppingListAmount
 * fields of items in the database. */
class InventoryViewModel(app: Application) :
    ItemListViewModel<InventoryItem>(app)
{
    override val items = sort.combine(searchFilterFlow, dao::getInventoryContents)
        .transformLatest { emitAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())


    override fun onChangeItemAmountRequest(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateInventoryAmount(id, amount) }
    }
    override fun onItemEditButtonClick(id: Long) {
        viewModelScope.launch { dao.toggleExpandedInInventory(id) }
    }
    fun onAutoAddToShoppingListCheckboxClick(id: Long) {
        viewModelScope.launch { dao.toggleAutoAddToShoppingList(id) }
    }
    fun onAutoAddToShoppingListAmountUpdateRequest(id: Long, autoAddToShoppingListAmount: Int) {
        viewModelScope.launch { dao.updateAutoAddToShoppingListAmount(id, autoAddToShoppingListAmount) }
    }


    override val selectedItemCount = dao.getSelectedInventoryItemCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    override fun toggleIsSelected(id: Long) {
        viewModelScope.launch { dao.toggleSelectedInInventory(id) }
    }
    override fun onSelectAllRequest() {
        viewModelScope.launch { dao.selectAllInventoryItems() }
    }
    override fun onClearSelectionRequest() {
        viewModelScope.launch { dao.clearInventorySelection() }
    }
    fun onAddFromSelectedShoppingListItemsRequest() {
        viewModelScope.launch {
            dao.addToInventoryFromSelectedShoppingListItems()
            dao.clearShoppingListSelection()
        }
    }

    override suspend fun deleteItem(id: Long) = dao.deleteInventoryItem(id)

    override fun emptyTrash() {
        viewModelScope.launch { dao.emptyInventoryTrash() }
    }
    override fun undoDelete() {
        viewModelScope.launch { dao.undoDeleteInventoryItems() }
    }
}