/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.cliffracertech.bootycrate.utils.getValue
import com.cliffracertech.bootycrate.utils.setValue
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.SoftKeyboard
import com.cliffracertech.bootycrate.utils.resolveIntAttribute
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * An abstract AndroidViewModel that provides the data for an activity or
 * fragment to display a list of ListItems.
 *
 * ItemListViewModel provides two sorting options through the properties sort
 * and searchFilter. sort describes a value of the enum class ListItem.Sort,
 * while searchFilter describes a string whose value the name and/or extra info
 * of items will be matched to. Changes to the value of sort or searchFilter
 * will result in the List<T> of items, exposed through the StateFlow property
 * items, to be updated to reflect the changed sorting option or search filter.
 *
 * If the properties newItemName and newItemExtraInfo are updated with the
 * proposed name and extra info for a new item, the StateFlow property
 * newItemNameIsAlreadyUsed can be collected to tell if the name and extra info
 * combination is already in use by another item on the same list, is in use by
 * an item in a neighboring collection, or if it is not in use by either, as
 * indicated by the values of the enum class NameIsAlreadyUsed.
 *
 * The current sorting option and search filter are exposed as StateFlows to
 * subclasses through the properties sortFlow and searchFilterFlow. Subclasses
 * should override the abstract property items to return a Flow<List<T>>
 * containing all of the items in the database that match the current sorting
 * option and search filter, as well as any additional sorting parameters that
 * they themselves add.
 *
 * Subclasses will need to override newItemNameIsAlreadyUsed with a Flow whose
 * emitted values are the correct NameIsAlreadyUsed value given the current
 * values of the StateFlow properties nameIsAlreadyUsedInShoppingList and
 * nameIsAlreadyUsedInInventory.
 */
abstract class ItemListViewModel<T: ListItem>(app: Application): AndroidViewModel(app) {
    protected val dao = BootyCrateDatabase.get(app).itemDao()

    protected val sortFlow = MutableStateFlow(ListItem.Sort.Color)
    var sort by sortFlow

    protected val searchFilterFlow = MutableStateFlow<String?>(null)
    var searchFilter by searchFilterFlow

    abstract val items: StateFlow<List<T>>

    /** An enum whose values represent whether a given name for a new
     * item is already in use by another item in a given list. */
    enum class NameIsAlreadyUsed {
        /** The name is already taken by an item in the list whose contents are being checked. */
        TrueForCurrentList,
        /** The name is already taken by an item in a sibling list of items, but not the current one. */
        TrueForSiblingList,
        /**  The name is not in use. */
        False }

    private val _newItemName = MutableStateFlow("")
    var newItemName by _newItemName

    private val _newItemExtraInfo = MutableStateFlow("")
    var newItemExtraInfo by _newItemExtraInfo

    protected val nameIsAlreadyUsedInShoppingList =
        _newItemName.combine(_newItemExtraInfo, dao::nameAlreadyUsedInShoppingList)
    protected val nameIsAlreadyUsedInInventory =
        _newItemName.combine(_newItemExtraInfo, dao::nameAlreadyUsedInInventory)

    abstract val newItemNameIsAlreadyUsed: StateFlow<NameIsAlreadyUsed>

    fun add(item: T, groupId: Long) =
        viewModelScope.launch { dao.add(item.toDbListItem(groupId)) }

    fun updateName(id: Long, name: String) =
        viewModelScope.launch { dao.updateName(id, name) }

    fun updateExtraInfo(id: Long, extraInfo: String) =
        viewModelScope.launch { dao.updateExtraInfo(id, extraInfo) }

    fun updateColor(id: Long, color: Int) =
        viewModelScope.launch { dao.updateColor(id, color) }

    abstract fun updateAmount(id: Long, amount: Int)
    abstract fun setExpandedItem(id: Long?)

    abstract val selectedItemCount: StateFlow<Int>
    abstract fun updateIsSelected(id: Long, isSelected: Boolean)
    abstract fun toggleIsSelected(id: Long)
    abstract fun selectAll()
    abstract fun clearSelection()

    abstract fun deleteSelected(snackBarAnchor: View)
    abstract fun delete(ids: LongArray, snackBarAnchor: View)
    abstract fun deleteAll()
    abstract fun emptyTrash()
    protected abstract fun undoDelete()
}



/**
 * An implementation of ItemListViewModel<ShoppingListItem>.
 *
 * ShoppingListViewModel adds a new sortByChecked option, which will sort
 * ShoppingListItems by their checked state in addition to the sorting method
 * described by the sort property. It also adds functions to manipulate the
 * checked state of items, and the checkout function.
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

    override val newItemNameIsAlreadyUsed = combine(
        nameIsAlreadyUsedInShoppingList,
        nameIsAlreadyUsedInInventory
    ) { existsInShoppingList, existsInInventory ->
        when { (existsInShoppingList) -> NameIsAlreadyUsed.TrueForCurrentList
               (existsInInventory) ->    NameIsAlreadyUsed.TrueForSiblingList
               else ->                   NameIsAlreadyUsed.False }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), NameIsAlreadyUsed.False)


    override fun updateAmount(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateShoppingListAmount(id, amount) }
    }
    override fun setExpandedItem(id: Long?) {
        viewModelScope.launch { dao.setExpandedShoppingListItem(id) }
    }


    val checkedItemsSize = dao.getCheckedShoppingListItemsSize()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    fun updateIsChecked(id: Long, isChecked: Boolean) =
        viewModelScope.launch { dao.updateIsChecked(id, isChecked) }

    fun checkAll() {
        viewModelScope.launch { dao.checkAllShoppingListItems() }
    }
    fun uncheckAll() {
        viewModelScope.launch { dao.uncheckAllShoppingListItems() }
    }
    fun checkout() {
        viewModelScope.launch { dao.checkout() }
    }


    override val selectedItemCount = dao.getSelectedShoppingListItemCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    override fun updateIsSelected(id: Long, isSelected: Boolean) {
        viewModelScope.launch { dao.updateSelectedInShoppingList(id, isSelected) }
    }
    override fun toggleIsSelected(id: Long) {
        viewModelScope.launch { dao.toggleSelectedInShoppingList(id) }
    }
    override fun selectAll() {
        viewModelScope.launch { dao.selectAllShoppingListItems() }
    }
    override fun clearSelection() {
        viewModelScope.launch { dao.clearShoppingListSelection() }
    }
    fun addFromSelectedInventoryItems() {
        viewModelScope.launch { dao.addToShoppingListFromSelectedInventoryItems() }
    }


    override fun deleteSelected(snackBarAnchor: View) {
        viewModelScope.launch {
            dao.deleteSelectedShoppingListItems()
            showDeletedItemsSnackBar(selectedItemCount.value, snackBarAnchor)
            SoftKeyboard.hide(snackBarAnchor)
        }
    }
    override fun delete(ids: LongArray, snackBarAnchor: View) {
        viewModelScope.launch {
            dao.deleteShoppingListItems(ids)
            showDeletedItemsSnackBar(ids.size, snackBarAnchor)
            SoftKeyboard.hide(snackBarAnchor)
        }
    }
    override fun deleteAll() {
        viewModelScope.launch { dao.deleteAllShoppingListItems() }
    }
    override fun emptyTrash() {
        viewModelScope.launch { dao.emptyShoppingListTrash() }
    }
    override fun undoDelete() {
        viewModelScope.launch { dao.undoDeleteShoppingListItems() }
    }

    private var totalDeletedItemCount: Int = 0
    private fun showDeletedItemsSnackBar(newlyDeletedItemCount: Int, anchor: View) {
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
                        if (b != DISMISS_EVENT_ACTION)
                            emptyTrash()
                    }
                }
            }).show()
    }
}



/** An implementation of ItemListViewModel<InventoryItem> that adds functions
 * to manipulate the autoAddToShoppingList and autoAddToShoppingListAmount
 * fields of items in the database. */
class InventoryViewModel(app: Application) : ItemListViewModel<InventoryItem>(app) {

    override val items = sortFlow.combine(searchFilterFlow, dao::getInventoryContents)
        .transformLatest { emitAll(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    override val newItemNameIsAlreadyUsed = combine(
        nameIsAlreadyUsedInInventory,
        nameIsAlreadyUsedInShoppingList
    ) { existsInInventory, existsInShoppingList ->
        when { (existsInInventory) ->    NameIsAlreadyUsed.TrueForCurrentList
               (existsInShoppingList) -> NameIsAlreadyUsed.TrueForSiblingList
               else ->                   NameIsAlreadyUsed.False }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), NameIsAlreadyUsed.False)


    override fun updateAmount(id: Long, amount: Int) {
        viewModelScope.launch { dao.updateInventoryAmount(id, amount) }
    }
    override fun setExpandedItem(id: Long?) {
        viewModelScope.launch { dao.setExpandedInventoryItem(id) }
    }
    fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean) {
        viewModelScope.launch { dao.updateAutoAddToShoppingList(id, autoAddToShoppingList) }
    }
    fun updateAutoAddToShoppingListAmount(id: Long, autoAddToShoppingListAmount: Int) {
        viewModelScope.launch { dao.updateAutoAddToShoppingListAmount(id, autoAddToShoppingListAmount) }
    }


    override val selectedItemCount = dao.getSelectedInventoryItemCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    override fun updateIsSelected(id: Long, isSelected: Boolean) {
        viewModelScope.launch { dao.updateSelectedInInventory(id, isSelected) }
    }
    override fun toggleIsSelected(id: Long) {
        viewModelScope.launch { dao.toggleSelectedInInventory(id) }
    }
    override fun selectAll() {
        viewModelScope.launch { dao.selectAllInventoryItems() }
    }
    override fun clearSelection() {
        viewModelScope.launch { dao.clearInventorySelection() }
    }
    fun addFromSelectedShoppingListItems() {
        viewModelScope.launch { dao.addToInventoryFromSelectedShoppingListItems() }
    }


    override fun deleteSelected(snackBarAnchor: View) {
        viewModelScope.launch { dao.deleteSelectedInventoryItems() }
    }
    override fun delete(ids: LongArray, snackBarAnchor: View) {
        viewModelScope.launch { dao.deleteInventoryItems(ids) }
    }
    override fun deleteAll() {
        viewModelScope.launch { dao.deleteAllInventoryItems() }
    }
    override fun emptyTrash() {
        viewModelScope.launch { dao.emptyInventoryTrash() }
    }
    override fun undoDelete() {
        viewModelScope.launch { dao.undoDeleteInventoryItems() }
    }
}