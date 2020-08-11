/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
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
 *  ViewModel also provides support for treating new items differently through
 *  the public property newlyInsertedItemId. This value will change to match
 *  the id of the most recently inserted item. External entities can compare
 *  this value to the id of items in order to determine the new item. resetNew-
 *  lyInsertedItemId should usually be called after this value is utilized so
 *  that the most recently inserted item will not be treated as new forever
 *  until a new item is inserted. */
abstract class ViewModel<Entity: BootyCrateItem>(app: Application): AndroidViewModel(app) {
    protected abstract val dao: DataAccessObject<Entity>
    private val sortAndFilterLiveData = MutableLiveData(Pair<BootyCrateItem.Sort?, String?>(
        BootyCrateItem.Sort.Color, ""))

    val items = Transformations.switchMap(sortAndFilterLiveData) { sortAndFilter ->
        val filter = '%' + (sortAndFilter.second ?: "") + '%'
        when (sortAndFilter.first) {
            null -> dao.getAllSortedByColor(filter)
            BootyCrateItem.Sort.Color -> dao.getAllSortedByColor(filter)
            BootyCrateItem.Sort.NameAsc -> dao.getAllSortedByNameAsc(filter)
            BootyCrateItem.Sort.NameDesc -> dao.getAllSortedByNameDesc(filter)
            BootyCrateItem.Sort.AmountAsc -> dao.getAllSortedByAmountAsc(filter)
            BootyCrateItem.Sort.AmountDesc -> dao.getAllSortedByAmountDesc(filter)
        }
    }
    var sort get() = sortAndFilterLiveData.value?.first
             set(value) { sortAndFilterLiveData.value = Pair(value, searchFilter) }
    var searchFilter get() = sortAndFilterLiveData.value?.second
                     set(value) { sortAndFilterLiveData.value = Pair(sort, value) }

    private var _newlyInsertedItemId = AtomicLong()
    val newlyInsertedItemId: Long get() = _newlyInsertedItemId.get()
    fun resetNewlyInsertedItemId() = _newlyInsertedItemId.set(0)

    fun insert(item: Entity) = viewModelScope.launch {
        _newlyInsertedItemId.set(dao.insert(item))
    }
    fun insert(vararg items: Entity) = viewModelScope.launch {
        dao.insert(*items)
    }
    fun deleteAll() = viewModelScope.launch {
        dao.deleteAll()
    }
    fun emptyTrash() = viewModelScope.launch {
        dao.emptyTrash()
    }
    fun delete(vararg ids: Long) = viewModelScope.launch {
        dao.delete(*ids)
    }
    fun undoDelete() = viewModelScope.launch {
        dao.undoDelete()
    }
}

/** A ViewModel<ShoppingListItem> subclass that provides functions to asynchronously execute ShoppingListItemDao's functions. */
class ShoppingListViewModel(app: Application) : ViewModel<ShoppingListItem>(app) {
    override val dao = BootyCrateDatabase.get(app).shoppingListItemDao()

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun insertFromInventoryItems(vararg itemIds: Long) = viewModelScope.launch {
        dao.insertFromInventoryItems(*itemIds)
    }
    fun autoAddFromInventoryItem(inventoryItemId: Long, minAmount: Int) = viewModelScope.launch {
        dao.autoAddFromInventoryItem(inventoryItemId, minAmount)
    }
    fun updateName(id: Long, name: String) = viewModelScope.launch {
        dao.updateName(id, name)
    }
    fun updateNameFromLinkedInventoryItem(inventoryItemId: Long, name: String) = viewModelScope.launch {
        dao.updateNameFromLinkedInventoryItem(inventoryItemId, name)
    }
    fun updateExtraInfo(id: Long, extraInfo: String) = viewModelScope.launch {
        dao.updateExtraInfo(id, extraInfo)
    }
    fun updateExtraInfoFromLinkedInventoryItem(inventoryItemId: Long, extraInfo: String) = viewModelScope.launch {
        dao.updateExtraInfoFromLinkedInventoryItem(inventoryItemId, extraInfo)
    }
    fun updateColor(id: Long, color: Int) = viewModelScope.launch {
        dao.updateColor(id, color)
    }
    fun updateIsChecked(id: Long, isChecked: Boolean) = viewModelScope.launch {
        dao.updateIsChecked(id, isChecked)
    }
    fun updateAmountOnList(id: Long, amountOnList: Int) = viewModelScope.launch {
        dao.updateAmount(id, amountOnList)
    }
    fun updateAmountOnListFromLinkedItem(inventoryItemId: Long, amount: Int) = viewModelScope.launch {
        dao.updateAmountFromLinkedItem(inventoryItemId, amount)
    }
    fun updateAmountInCart(id: Long, amountInCart: Int) = viewModelScope.launch {
        dao.updateAmountInCart(id, amountInCart)
    }
    fun updateLinkedInventoryItemId(id: Long, linkedInventoryItem: InventoryItem) = viewModelScope.launch {
        dao.updateLinkedInventoryItemId(id, linkedInventoryItem.id,
                                        linkedInventoryItem.name,
                                        linkedInventoryItem.extraInfo)
    }
    fun checkOut() = viewModelScope.launch {
        dao.checkOut()
    }
}

/** A ViewModel<InventoryItem> subclass that provides functions to asynchronously execute InventoryItemDao's functions. */
class InventoryViewModel(app: Application) : ViewModel<InventoryItem>(app) {
    override val dao = BootyCrateDatabase.get(app).inventoryItemDao()

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun insertFromShoppingListItems(vararg shoppingListItemIds: Long) = viewModelScope.launch {
        dao.insertFromShoppingListItems(*shoppingListItemIds)
    }
    fun updateName(id: Long, name: String) = viewModelScope.launch {
        dao.updateName(id, name)
    }
    fun updateAmount(id: Long, amount: Int) = viewModelScope.launch {
        dao.updateAmount(id, amount)
    }
    fun updateExtraInfo(id: Long, extraInfo: String) = viewModelScope.launch {
        dao.updateExtraInfo(id, extraInfo)
    }
    fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean) = viewModelScope.launch {
        dao.updateAutoAddToShoppingList(id, autoAddToShoppingList)
    }
    fun updateAutoAddToShoppingListTrigger(id: Long, autoAddToShoppingListTrigger: Int) = viewModelScope.launch {
        dao.updateAutoAddToShoppingListTrigger(id, autoAddToShoppingListTrigger)
    }
    fun updateColor(id: Long, color: Int) = viewModelScope.launch {
        dao.updateColor(id, color)
    }
}