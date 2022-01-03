/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.cliffracertech.bootycrate.utils.getValue
import com.cliffracertech.bootycrate.utils.setValue
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ListItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.utils.SoftKeyboard
import com.cliffracertech.bootycrate.utils.resolveIntAttribute
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * An abstract AndroidViewModel to provide data and callbacks for a fragment
 * that displays a list of ListItem subclasses.
 *
 * ItemListViewModel provides two sorting options through the properties sort
 * and searchFilter. sort describes a value of the enum class ListItem.Sort,
 * while searchFilter describes a string whose value the name and/or extra info
 * of items will be matched to. Changes to the value of sort or searchFilter
 * will result in the List<T> of items, exposed through the StateFlow property
 * items, to be updated to reflect the changed sorting option or search filter.
 *
 * The property selectedItemGroupName is a StateFlow<String> whose current
 * value will be equal to the name of the selected item group if only one is
 * selected, or a string to express that multiple item groups are selected
 * otherwise.
 *
 * The current sorting option and search filter are exposed as StateFlows to
 * subclasses through the properties sortFlow and searchFilterFlow. Subclasses
 * should override the abstract property items to return a Flow<List<T>>
 * containing all of the items in the database that match the current sorting
 * option and search filter, as well as any additional sorting parameters that
 * they themselves add.
 */
abstract class ItemListViewModel<T: ListItem>(app: Application): AndroidViewModel(app) {
    protected val dao = BootyCrateDatabase.get(app).itemDao()
    private val itemGroupDao = BootyCrateDatabase.get(app).itemGroupDao()

    protected val sortFlow = MutableStateFlow(ListItem.Sort.Color)
    var sort by sortFlow

    protected val searchFilterFlow = MutableStateFlow<String?>(null)
    var searchFilter by searchFilterFlow

    abstract val items: StateFlow<List<T>>

    private val nameForMultiSelection = app.getString(R.string.multiple_selected_item_groups_description)
    val selectedItemGroupName = itemGroupDao.getSelectedGroups().map {
        if (it.size == 1) it.first().name
        else nameForMultiSelection
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    fun onRenameItemRequest(id: Long, name: String) {
        viewModelScope.launch { dao.updateName(id, name) }
    }
    fun onChangeItemExtraInfoRequest(id: Long, extraInfo: String) {
        viewModelScope.launch { dao.updateExtraInfo(id, extraInfo) }
    }
    fun onChangeItemColorRequest(id: Long, color: Int) {
        viewModelScope.launch { dao.updateColor(id, color) }
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

    fun onDeletionRequest(id: Long, snackBarAnchor: View) =
        onDeletionRequest(arrayOf(id), snackBarAnchor)

    fun onDeletionRequest(ids: Array<Long>, snackBarAnchor: View) {
        deleteItems(ids)
        showDeletedItemsSnackBar(ids.size, snackBarAnchor)
    }
    fun onDeleteSelectedRequest(snackBarAnchor: View) {
        val count = selectedItemCount.value
        deleteSelectedItems()
        showDeletedItemsSnackBar(count, snackBarAnchor)
    }

    protected abstract fun deleteItems(ids: Array<Long>)
    protected abstract fun deleteSelectedItems()
    protected abstract fun emptyTrash()
    protected abstract fun undoDelete()

    private var totalDeletedItemCount: Int = 0
    protected fun showDeletedItemsSnackBar(newlyDeletedItemCount: Int, anchor: View) {
        totalDeletedItemCount += newlyDeletedItemCount
        val text = anchor.context.getString(R.string.delete_snackbar_text, totalDeletedItemCount)
        Snackbar.make(anchor, text, Snackbar.LENGTH_LONG)
            .setAnchorView(anchor)
            .setAction(R.string.delete_snackbar_undo_text) { undoDelete() }
            .setActionTextColor(anchor.context.theme.resolveIntAttribute(R.attr.colorAccent))
            .addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(a: Snackbar?, b: Int) {
                    if (b != DISMISS_EVENT_CONSECUTIVE) {
                        totalDeletedItemCount = 0
                        if (b != DISMISS_EVENT_ACTION) emptyTrash()
                    }
                }
            }).show()
        SoftKeyboard.hide(anchor)
    }
}



/**
 * An implementation of ItemListViewModel<ShoppingListItem>.
 *
 * ShoppingListViewModel adds a new sortByChecked option, which will sort
 * ShoppingListItems by their checked state in addition to the sorting method
 * described by the sort property. It also adds functions to respond to clicks
 * on the checkbox of items, and to respond to a request to checkout.
 */
class ShoppingListViewModel(app: Application) : ItemListViewModel<ShoppingListItem>(app) {

    private val _sortByChecked = MutableStateFlow(false)
    var sortByChecked by _sortByChecked

    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(app)
        sortByChecked = prefs.getBoolean(app.getString(R.string.pref_sort_by_checked_key), false)
    }

    override val items =
        combine(sortFlow, searchFilterFlow, _sortByChecked, dao::getShoppingList)
            .transformLatest { emitAll(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun onChangeItemAmountRequest(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateShoppingListAmount(id, amount) }
    }

    override fun onItemEditButtonClick(id: Long) {
        viewModelScope.launch { dao.toggleExpandedInShoppingList(id) }
    }

    val checkedItemsSize = dao.getCheckedShoppingListItemsSize()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

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

    override fun deleteItems(ids: Array<Long>) {
        viewModelScope.launch { dao.deleteShoppingListItems(ids) }
    }
    override fun deleteSelectedItems() {
        viewModelScope.launch { dao.deleteSelectedShoppingListItems() }
    }
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
class InventoryViewModel(app: Application) : ItemListViewModel<InventoryItem>(app) {

    override val items = sortFlow.combine(searchFilterFlow, dao::getInventoryContents)
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

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

    override fun deleteItems(ids: Array<Long>) {
        viewModelScope.launch { dao.deleteInventoryItems(ids) }
    }
    override fun deleteSelectedItems() {
        viewModelScope.launch { dao.deleteSelectedInventoryItems() }
    }
    override fun emptyTrash() {
        viewModelScope.launch { dao.emptyInventoryTrash() }
    }
    override fun undoDelete() {
        viewModelScope.launch { dao.undoDeleteInventoryItems() }
    }
}