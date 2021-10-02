/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class BootyCrateItemSort { Color, NameAsc, NameDesc, AmountAsc, AmountDesc;
    companion object {
        fun fromString(string: String?) =
            if (string == null) Color
            else try { valueOf(string) }
                 catch(e: IllegalArgumentException) { Color }
    }
}

fun shoppingListViewModel(context: Context) =
    ViewModelProvider(context.asFragmentActivity()).get(ShoppingListItemViewModel::class.java)

fun inventoryViewModel(context: Context) =
    ViewModelProvider(context.asFragmentActivity()).get(InventoryItemViewModel::class.java)

/**
 * An abstract AndroidViewModel that provides an interface for asynchronously manipulating a BootyCrateDatabase.
 *
 * The public property items returns a LiveData<List<T>> containing all of the
 * items in the database that match the current sorting options. BootyCrateViewModel
 * provides two sorting options through the properties sort and searchFilter.
 * sort describes a value of the enum class BootyCrateItemSort, while searchFilter
 * describes a string whose value the name and/or extra info of items will be
 * matched to. Additional sorting options can be add in subclasses if
 * notifySortOptionsChanged is called when they are changed. Subclass
 * implementations of the function itemsSwitchMapFunc should return the
 * LiveData<List<T>> given the current values of sort, searchFilter, and any
 * additional sort options the subclass adds.
 *
 * If the properties newItemName and newItemExtraInfo are updated with the
 * proposed name and extra info for a new item, the LiveData property
 * newItemNameIsAlreadyUsed can be observed to tell if the name and extra info
 * combination is already in use by another item on the same list, is in use by
 * an item in a neighboring collection, or if it is not in use by either, as
 * indicated by the values of the enum class NameIsAlreadyUsed. Subclasses will
 * need to implement newItemNameIsAlreadyUsed as a LiveData member whose value
 * will be equal to the correct NameIsAlreadyUsed value given the current
 * values of the StateFlow properties itemWithNameAlreadyExistsInShoppingList
 * and itemWithNameAlreadyExistsInInventory.
 */
abstract class BootyCrateViewModel<T: BootyCrateItem>(app: Application): AndroidViewModel(app) {
    protected val dao = BootyCrateDatabase.get(app).itemDao()

    fun add(item: T) = viewModelScope.launch { dao.add(item.toDbBootyCrateItem()) }
    fun add(items: List<T>) = viewModelScope.launch {
        dao.add(items.map { it.toDbBootyCrateItem() })
    }

    fun updateName(id: Long, name: String) =
        viewModelScope.launch { dao.updateName(id, name) }

    fun updateExtraInfo(id: Long, extraInfo: String) =
        viewModelScope.launch { dao.updateExtraInfo(id, extraInfo) }

    fun updateColor(id: Long, color: Int) =
        viewModelScope.launch { dao.updateColor(id, color) }

    /* For those wondering why item fetching uses LiveData all the way from the DAO
    to the view model level and does not use Flows at all: An attempt was made to
    make the DAO return Flow<List<T>> for its item fetching queries instead of
    LiveData<List<T>>, choose from among these Flows in a combineTransform of
    the sorting option StateFlows, and then only expose the final result as a
    LiveData<List<T>> for the UI layer. Unfortunately this was resulting in some
    truly bizarre bugs, such as selecting some items causing other items to
    randomly disappear, and then reappear again once the other items were
    deselected. Since there's probably no end user benefit for this particular
    LiveData to Flow migration, the item fetching is going to stay with LiveData
    for the time being. */
    protected val sortOptionsChanged = MutableLiveData<Boolean>()
    protected fun notifySortOptionsChanged() { sortOptionsChanged.value = true }

    var sort: BootyCrateItemSort = BootyCrateItemSort.Color
        set(value) { field = value; notifySortOptionsChanged() }

    var searchFilter: String = ""
        set(value) { field = value; notifySortOptionsChanged() }

    val items = Transformations.switchMap(sortOptionsChanged) { itemsSwitchMapFunc() }

    abstract fun itemsSwitchMapFunc(): LiveData<List<T>>

    enum class NameIsAlreadyUsed { TrueForCurrentList, TrueForSiblingList, False }

    var newItemName = ""
        set(value) { field = value; onNewItemNameUpdated() }
    var newItemExtraInfo = ""
        set(value) { field = value; onNewItemNameUpdated() }

    private val _itemWithNameAlreadyExistsInShoppingList = MutableStateFlow(false)
    private val _itemWithNameAlreadyExistsInInventory = MutableStateFlow(false)
    protected val itemWithNameAlreadyExistsInShoppingList get() = _itemWithNameAlreadyExistsInShoppingList.asStateFlow()
    protected val itemWithNameAlreadyExistsInInventory get() = _itemWithNameAlreadyExistsInInventory.asStateFlow()

    private fun onNewItemNameUpdated() {
        viewModelScope.launch {
            _itemWithNameAlreadyExistsInShoppingList.value =
                dao.itemWithNameAlreadyExistsInShoppingList(newItemName, newItemExtraInfo)
            _itemWithNameAlreadyExistsInInventory.value =
                dao.itemWithNameAlreadyExistsInInventory(newItemName, newItemExtraInfo)
        }
    }
    abstract val newItemNameIsAlreadyUsed: LiveData<NameIsAlreadyUsed>

    abstract fun deleteAll(): Job
    abstract fun emptyTrash(): Job
    abstract fun delete(ids: LongArray): Job
    abstract fun undoDelete(): Job
    abstract fun updateAmount(id: Long, amount: Int): Job

    abstract fun setExpandedItem(id: Long?): Job
    abstract val selectedItemCount: LiveData<Int>
    abstract fun updateIsSelected(id: Long, isSelected: Boolean): Job
    abstract fun toggleIsSelected(id: Long): Job
    abstract fun deleteSelected(): Job
    abstract fun selectAll(): Job
    abstract fun clearSelection(): Job
}



/**
 * An implementation of BootyCrateViewModel<ShoppingListItem>.
 *
 * ShoppingListViewModel adds a new sortByChecked option, which will sort
 * ShoppingListItems by their checked state in addition to the sorting method
 * described by the sort property. It also adds functions to manipulate the
 * checked state of items, and the checkout function.
 */
class ShoppingListItemViewModel(app: Application) : BootyCrateViewModel<ShoppingListItem>(app) {
    var sortByChecked = false
        set(value) { field = value; notifySortOptionsChanged() }

    override fun itemsSwitchMapFunc() = run {
        val filter = "%$searchFilter%"
        if (!sortByChecked) when (sort) {
            BootyCrateItemSort.Color -> dao.getShoppingListSortedByColor(filter)
            BootyCrateItemSort.NameAsc -> dao.getShoppingListSortedByNameAsc(filter)
            BootyCrateItemSort.NameDesc -> dao.getShoppingListSortedByNameDesc(filter)
            BootyCrateItemSort.AmountAsc -> dao.getShoppingListSortedByAmountAsc(filter)
            BootyCrateItemSort.AmountDesc -> dao.getShoppingListSortedByAmountDesc(filter)
        } else when (sort) {
            BootyCrateItemSort.Color -> dao.getShoppingListSortedByColorAndChecked(filter)
            BootyCrateItemSort.NameAsc -> dao.getShoppingListSortedByNameAscAndChecked(filter)
            BootyCrateItemSort.NameDesc -> dao.getShoppingListSortedByNameDescAndChecked(filter)
            BootyCrateItemSort.AmountAsc -> dao.getShoppingListSortedByAmountAscAndChecked(filter)
            BootyCrateItemSort.AmountDesc -> dao.getShoppingListSortedByAmountDescAndChecked(filter)
        }
    }

    override val newItemNameIsAlreadyUsed = combine(
        itemWithNameAlreadyExistsInShoppingList,
        itemWithNameAlreadyExistsInInventory
    ) { existsInShoppingList, existsInInventory ->
        when { (existsInShoppingList) -> NameIsAlreadyUsed.TrueForCurrentList
               (existsInInventory) ->    NameIsAlreadyUsed.TrueForSiblingList
               else ->                   NameIsAlreadyUsed.False }
    }.asLiveData()

    override fun deleteAll() = viewModelScope.launch { dao.deleteAllShoppingListItems() }

    override fun emptyTrash() = viewModelScope.launch { dao.emptyShoppingListTrash() }

    override fun delete(ids: LongArray) = viewModelScope.launch { dao.deleteShoppingListItems(ids) }

    override fun undoDelete() = viewModelScope.launch { dao.undoDeleteShoppingListItems() }

    override fun updateAmount(id: Long, amount: Int) =
        viewModelScope.launch { dao.updateShoppingListAmount(id, amount) }

    override fun setExpandedItem(id: Long?) =
        viewModelScope.launch { dao.setExpandedShoppingListItem(id) }

    override val selectedItemCount = dao.getSelectedShoppingListItemCount()

    override fun updateIsSelected(id: Long, isSelected: Boolean) =
        viewModelScope.launch { dao.updateSelectedInShoppingList(id, isSelected) }

    override fun toggleIsSelected(id: Long) =
        viewModelScope.launch { dao.toggleSelectedInShoppingList(id) }

    override fun deleteSelected() =
        viewModelScope.launch { dao.deleteSelectedShoppingListItems() }

    override fun selectAll() = viewModelScope.launch {
        dao.selectShoppingListItems(items.value?.map { it.id } ?: emptyList())
    }
    override fun clearSelection() = viewModelScope.launch { dao.clearShoppingListSelection() }

    fun addFromSelectedInventoryItems() =
        viewModelScope.launch { dao.addToShoppingListFromSelectedInventoryItems() }

    val checkedItemsSize = dao.getCheckedShoppingListItemsSize()

    fun updateIsChecked(id: Long, isChecked: Boolean) =
        viewModelScope.launch { dao.updateIsChecked(id, isChecked) }

    fun checkAll() = viewModelScope.launch { dao.checkAllShoppingListItems() }

    fun uncheckAll() = viewModelScope.launch { dao.uncheckAllShoppingListItems() }

    fun checkout() = viewModelScope.launch { dao.checkout() }
}



/** An implementation of BootyCrateViewModel<InventoryItem> that adds functions
 * to  manipulate the autoAddToShoppingList and autoAddToShoppingListAmount
 * fields of items in the database. */
class InventoryItemViewModel(app: Application) : BootyCrateViewModel<InventoryItem>(app) {

    override fun itemsSwitchMapFunc() = run {
        val filter = "%$searchFilter%"
        when (sort) {
            BootyCrateItemSort.Color -> dao.getInventorySortedByColor(filter)
            BootyCrateItemSort.NameAsc -> dao.getInventorySortedByNameAsc(filter)
            BootyCrateItemSort.NameDesc -> dao.getInventorySortedByNameDesc(filter)
            BootyCrateItemSort.AmountAsc -> dao.getInventorySortedByAmountAsc(filter)
            BootyCrateItemSort.AmountDesc -> dao.getInventorySortedByAmountDesc(filter)
        }
    }

    override val newItemNameIsAlreadyUsed = combine(
        itemWithNameAlreadyExistsInInventory,
        itemWithNameAlreadyExistsInShoppingList
    ) { existsInInventory, existsInShoppingList ->
        when { (existsInInventory) ->    NameIsAlreadyUsed.TrueForCurrentList
               (existsInShoppingList) -> NameIsAlreadyUsed.TrueForSiblingList
               else ->                   NameIsAlreadyUsed.False }
    }.asLiveData()

    override fun deleteAll() = viewModelScope.launch { dao.deleteAllInventoryItems() }

    override fun emptyTrash() = viewModelScope.launch { dao.emptyInventoryTrash() }

    override fun delete(ids: LongArray) = viewModelScope.launch { dao.deleteInventoryItems(ids) }

    override fun undoDelete() = viewModelScope.launch { dao.undoDeleteInventoryItems() }

    override fun updateAmount(id: Long, amount: Int) =
        viewModelScope.launch { dao.updateInventoryAmount(id, amount) }

    override fun setExpandedItem(id: Long?) =
        viewModelScope.launch { dao.setExpandedInventoryItem(id) }

    override val selectedItemCount = dao.getSelectedInventoryItemCount()

    override fun updateIsSelected(id: Long, isSelected: Boolean) =
        viewModelScope.launch { dao.updateSelectedInInventory(id, isSelected) }

    override fun toggleIsSelected(id: Long) =
        viewModelScope.launch { dao.toggleSelectedInInventory(id) }

    override fun deleteSelected() = viewModelScope.launch { dao.deleteSelectedInventoryItems() }

    override fun selectAll() = viewModelScope.launch {
        dao.selectInventoryItems(items.value?.map { it.id } ?: emptyList())
    }
    override fun clearSelection() = viewModelScope.launch { dao.clearInventorySelection() }

    fun addFromSelectedShoppingListItems() =
        viewModelScope.launch { dao.addToInventoryFromSelectedShoppingListItems() }

    fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean) =
        viewModelScope.launch { dao.updateAutoAddToShoppingList(id, autoAddToShoppingList) }

    fun updateAutoAddToShoppingListAmount(id: Long, autoAddToShoppingListAmount: Int) =
        viewModelScope.launch { dao.updateAutoAddToShoppingListAmount(id, autoAddToShoppingListAmount) }
}