/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/** An abstract AndroidViewModel that provides an asynchronous interface for DataAccessObject<Entity> functions.
 *
 *  ViewModel<Entity> is an abstract AndroidViewModel that provides asynchro-
 *  nous functions for executing DataAccessObject<Entity> methods. The public
 *  property items returns a LiveData<List<Entity>> object that represents all
 *  of the items in the SQLite table. The public properties sort and searchFil-
 *  ter allow the view model user to modify the sorting or filtering used in
 *  retrieving the items.
 *
 *  ViewModel provides support for treating new items differently through the
 *  public property newlyAddedItemId. This value will change to match the id of
 *  the most recently added item. External entities can compare this value to
 *  the id of items in order to determine the new item. resetNewlyAddedItemId()
 *  should usually be called after this value is utilized so that the most
 *  recently added item will not be treated as new forever until a new item is
 *  added. */
abstract class ViewModel<Entity: ViewModelItem>(app: Application): AndroidViewModel(app) {
    protected abstract val dao: DataAccessObject<Entity>

    var sort get() = sortAndFilterLiveData.value?.first
        set(value) { sortAndFilterLiveData.value = Pair(value, searchFilter) }
    var searchFilter get() = sortAndFilterLiveData.value?.second
        set(value) { sortAndFilterLiveData.value = Pair(sort, value) }
    private val sortAndFilterLiveData =
        MutableLiveData(Pair<ViewModelItem.Sort?, String?>(ViewModelItem.Sort.Color, ""))
    val items = Transformations.switchMap(sortAndFilterLiveData) { sortAndFilter ->
        val filter = "%${sortAndFilter.second ?: ""}%"
        when (sortAndFilter.first) {
            null -> dao.getAllSortedByColor(filter)
            ViewModelItem.Sort.Color -> dao.getAllSortedByColor(filter)
            ViewModelItem.Sort.NameAsc -> dao.getAllSortedByNameAsc(filter)
            ViewModelItem.Sort.NameDesc -> dao.getAllSortedByNameDesc(filter)
            ViewModelItem.Sort.AmountAsc -> dao.getAllSortedByAmountAsc(filter)
            ViewModelItem.Sort.AmountDesc -> dao.getAllSortedByAmountDesc(filter)
        }
    }

    var newItemName get() = newItemNameLiveData.value?.first
        set(value) { newItemNameLiveData.value = Pair(value, newItemExtraInfo) }
    var newItemExtraInfo get() = newItemNameLiveData.value?.second
        set(value) { newItemNameLiveData.value = Pair(newItemName, value) }
    private val newItemNameLiveData = MutableLiveData(Pair<String?, String?>("", ""))
    val newItemNameIsAlreadyUsed =
        Transformations.switchMap(newItemNameLiveData) { newItemNameAndExtraInfo ->
            dao.itemWithNameAlreadyExists(newItemNameAndExtraInfo.first ?: "",
                                          newItemNameAndExtraInfo.second ?: "")
        }
    fun resetNewItemName() { newItemName = null; newItemExtraInfo = null }

    private var _newlyAddedItemId = AtomicLong()
    val newlyAddedItemId get() = _newlyAddedItemId.get()
    fun resetNewlyAddedItemId() = _newlyAddedItemId.set(0)

    fun add(item: Entity) = viewModelScope.launch {
        _newlyAddedItemId.set(dao.add(item))
    }
    fun add(items: List<Entity>) = viewModelScope.launch {
        dao.add(items)
    }
    fun deleteAll() = viewModelScope.launch {
        dao.deleteAll()
    }
    fun emptyTrash() = viewModelScope.launch {
        dao.emptyTrash()
    }
    fun delete(ids: LongArray) = viewModelScope.launch {
        dao.delete(ids)
    }
    fun undoDelete() = viewModelScope.launch {
        dao.undoDelete()
    }
    fun updateName(id: Long, name: String) = viewModelScope.launch {
        dao.updateName(id, name)
    }
    fun updateExtraInfo(id: Long, extraInfo: String) = viewModelScope.launch {
        dao.updateExtraInfo(id, extraInfo)
    }
    fun updateColor(id: Long, color: Int) = viewModelScope.launch {
        dao.updateColor(id, color)
    }
    fun updateAmount(id: Long, amountOnList: Int) = viewModelScope.launch {
        dao.updateAmount(id, amountOnList)
    }
}

/** An extension of ViewModel that provides access to ExpandableSelectableItemDao methods.
 *
 *  ExpandableSelectableItemViewModel overrides ViewModel's dao property with
 *  an instance of ExpandableSelectableItemDao, and provides methods for acces-
 *  sing ExpandableSelectableItemDao's methods. */
abstract class ExpandableSelectableItemViewModel<Entity: ExpandableSelectableItem>(
    app: Application
) : ViewModel<Entity>(app) {

    abstract override val dao : ExpandableSelectableItemDao<Entity>
    /* Selection size is initialized lazily to prevent getSelectedItems() from
     * being called on the dao during initialization of ExpandableSelectable-
     * ItemViewModel, before it is overridden in the descendant class. */
    val selectedItems: LiveData<List<Entity>> by lazy { dao.getSelectedItems() }

    fun resetExpandedItemAndSelection() = viewModelScope.launch {
        dao.resetExpandedItemAndSelection()
    }
    fun setExpandedId(id: Long?) = viewModelScope.launch {
        dao.setExpandedItem(id)
    }
    fun updateIsSelected(id: Long, isSelected: Boolean) = viewModelScope.launch {
        dao.updateIsSelected(id, isSelected)
    }
    fun toggleIsSelected(id: Long) = viewModelScope.launch {
        dao.toggleIsSelected(id)
    }
    fun deleteSelected() = viewModelScope.launch {
        dao.deleteSelected()
    }
    fun clearSelection() = viewModelScope.launch {
        dao.clearSelection()
    }
}

/** A ViewModel<ShoppingListItem> subclass that provides functions to asynchronously execute ShoppingListItemDao's functions. */
class ShoppingListViewModel(app: Application) : ExpandableSelectableItemViewModel<ShoppingListItem>(app) {
    override val dao = BootyCrateDatabase.get(app).shoppingListItemDao()
    val checkedItemsSize = dao.getCheckedItemsSize()

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun addFromSelectedInventoryItems() = viewModelScope.launch {
        dao.addFromSelectedInventoryItems()
    }
    fun updateIsChecked(id: Long, isChecked: Boolean) = viewModelScope.launch {
        dao.updateIsChecked(id, isChecked)
    }
    fun uncheckAll() = viewModelScope.launch {
        dao.uncheckAll()
    }
    fun checkout() = viewModelScope.launch {
        dao.checkout()
    }
}

/** A ViewModel<InventoryItem> subclass that provides functions to asynchronously execute InventoryItemDao's functions. */
class InventoryViewModel(app: Application) : ExpandableSelectableItemViewModel<InventoryItem>(app) {
    override val dao = BootyCrateDatabase.get(app).inventoryItemDao()

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun addFromSelectedShoppingListItems() = viewModelScope.launch {
        dao.addFromSelectedShoppingListItems()
    }
    fun updateAddToShoppingList(id: Long, addToShoppingList: Boolean) = viewModelScope.launch {
        dao.updateAddToShoppingList(id, addToShoppingList)
    }
    fun updateAddToShoppingListTrigger(id: Long, addToShoppingListTrigger: Int) = viewModelScope.launch {
        dao.updateAddToShoppingListTrigger(id, addToShoppingListTrigger)
    }
}