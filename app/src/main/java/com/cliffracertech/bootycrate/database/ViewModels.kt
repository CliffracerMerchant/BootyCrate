/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import kotlinx.coroutines.launch

/**
 * An AndroidViewModel that provides asynchronous functions for manipulating a BootyCrateDatabase.
 *
 * The public properties shoppingList and inventory return LiveData<List>
 * objects that represents all of the shopping list and inventory items in
 * the database. The public properties shoppingListSort, shoppingListSearchFilter,
 * sortShoppingListByChecked, inventorySort, and inventorySearchFilter allow
 * entities to modify the sorting or filtering used in retrieving the items.
 *
 * If the properties newShoppingListItemName and newShoppingListItemExtraInfo
 * or NewInventoryItemName and newInventoryItemExtraInfo are updated with the
 * proposed name and extra info for a new shopping list or inventory item, the
 * property newShoppingListItemNameIsAlreadyUsed or newInventoryItemNameIsAlreadyUsed
 * can be observed to tell if the name and extra info combination is already in
 * use by another shopping list or inventory item.
 */
abstract class BootyCrateViewModel(app: Application): AndroidViewModel(app) {
    private val dao = BootyCrateDatabase.get(app).dao()

    fun add(item: BootyCrateItem) = viewModelScope.launch {dao.add(item) }
    fun add(items: List<BootyCrateItem>) = viewModelScope.launch { dao.add(items) }

    fun updateName(id: Long, name: String) =
        viewModelScope.launch { dao.updateName(id, name) }
    fun updateExtraInfo(id: Long, extraInfo: String) =
        viewModelScope.launch { dao.updateExtraInfo(id, extraInfo) }
    fun updateColor(id: Long, color: Int) =
        viewModelScope.launch { dao.updateColor(id, color) }

    private val shoppingListSortOptionsChanged = MutableLiveData<Boolean>()
    var shoppingListSort: BootyCrateItem.Sort = BootyCrateItem.Sort.Color
        set(value) { field = value; shoppingListSortOptionsChanged.value = true }
    var shoppingListSearchFilter: String? = null
        set(value) { field = value; shoppingListSortOptionsChanged.value = true }
    var sortShoppingListByChecked = false
        set(value) { field = value; shoppingListSortOptionsChanged.value = true }

    val shoppingList = Transformations.switchMap(shoppingListSortOptionsChanged) {
        dao.getShoppingList(shoppingListSort, sortShoppingListByChecked, shoppingListSearchFilter)
    }

    var newShoppingListItemName: String? = null
        set(value) { field = value; newShoppingListItemNameChanged.value = true }
    var newShoppingListItemExtraInfo: String? = null
        set(value) { field = value; newShoppingListItemNameChanged.value = true }
    fun resetNewShoppingListItemName() {
        newShoppingListItemName = null
        newShoppingListItemExtraInfo = null
    }
    private val newShoppingListItemNameChanged = MutableLiveData<Boolean>()
    val newItemNameIsAlreadyUsed = Transformations.switchMap(newShoppingListItemNameChanged) {
        dao.shoppingListItemWithNameAlreadyExists(newShoppingListItemName ?: "",
                                                  newShoppingListItemExtraInfo ?: "")
    }

    fun deleteShoppingList() = viewModelScope.launch { dao.deleteAllShoppingListItems() }
    fun emptyShoppingListTrash() = viewModelScope.launch { dao.emptyShoppingListTrash() }
    fun deleteShoppingListItems(ids: LongArray) =
        viewModelScope.launch { dao.deleteShoppingListItems(ids) }
    fun undoDeleteShoppingListItems() = viewModelScope.launch { dao.undoDeleteShoppingListItems() }
    fun updateShoppingListAmount(id: Long, amountOnList: Int) =
        viewModelScope.launch { dao.updateShoppingListAmount(id, amountOnList) }

    val selectedShoppingListItems: LiveData<List<ShoppingListItem>> =
        dao.getSelectedShoppingListItems()

    fun setExpandedShoppingListItem(id: Long?) =
        viewModelScope.launch { dao.setExpandedShoppingListItem(id) }

    fun updateSelectedInShoppingList(id: Long, isSelected: Boolean) =
        viewModelScope.launch { dao.updateSelectedInShoppingList(id, isSelected) }

    fun toggleSelectedInShoppingList(id: Long) =
        viewModelScope.launch { dao.toggleSelectedInShoppingList(id) }

    fun deleteSelectedShoppingListItems() =
        viewModelScope.launch { dao.deleteSelectedShoppingListItems() }

    fun selectAllShoppingListItems() = viewModelScope.launch {
        dao.selectShoppingListItems(shoppingList.value?.map { it.id } ?: emptyList())
    }

    fun clearShoppingListSelection() = viewModelScope.launch { dao.clearShoppingListSelection() }

    val checkedShoppingListItemsSize = dao.getCheckedShoppingListItemsSize()

    fun addToShoppingListFromSelectedInventoryItems() =
        viewModelScope.launch { dao.addToShoppingListFromSelectedInventoryItems() }

    fun updateChecked(id: Long, checked: Boolean) =
        viewModelScope.launch { dao.updateChecked(id, checked) }

    fun checkAll() = viewModelScope.launch { dao.checkAllShoppingListItems() }

    fun uncheckAll() = viewModelScope.launch { dao.uncheckAllShoppingListItems() }

    fun checkout() = viewModelScope.launch { dao.checkout() }



    private val inventorySortOptionsChanged = MutableLiveData<Boolean>()
    var inventorySort: BootyCrateItem.Sort = BootyCrateItem.Sort.Color
        set(value) { field = value; inventorySortOptionsChanged.value = true }
    var inventorySearchFilter: String? = null
        set(value) { field = value; inventorySortOptionsChanged.value = true }

    val inventory = Transformations.switchMap(inventorySortOptionsChanged) {
        dao.getInventory(inventorySort, inventorySearchFilter)
    }

    var newInventoryItemName: String? = null
        set(value) { field = value; newInventoryItemNameChanged.value = true }
    var newInventoryItemExtraInfo: String? = null
        set(value) { field = value; newInventoryItemNameChanged.value = true }
    fun resetNewInventoryItemName() {
        newInventoryItemName = null
        newInventoryItemExtraInfo = null
    }
    private val newInventoryItemNameChanged = MutableLiveData<Boolean>()
    val newInventoryItemNameIsAlreadyUsed = Transformations.switchMap(newInventoryItemNameChanged) {
        dao.inventoryItemWithNameAlreadyExists(newInventoryItemName ?: "",
                                               newInventoryItemExtraInfo ?: "")
    }

    fun deleteInventory() = viewModelScope.launch { dao.deleteAllInventoryItems() }
    fun emptyInventoryTrash() = viewModelScope.launch { dao.emptyInventoryTrash() }
    fun deleteInventoryItems(ids: LongArray) = viewModelScope.launch { dao.deleteInventoryItems(ids) }
    fun undoDeleteInventoryItems() = viewModelScope.launch { dao.undoDeleteInventoryItems() }
    fun updateInventoryAmount(id: Long, amountOnList: Int) =
        viewModelScope.launch { dao.updateInventoryAmount(id, amountOnList) }

    val selectedInventoryItems: LiveData<List<InventoryItem>> =
        dao.getSelectedInventoryItems()

    fun setExpandedInventoryItem(id: Long?) =
        viewModelScope.launch { dao.setExpandedInventoryItem(id) }

    fun updateSelectedInInventory(id: Long, isSelected: Boolean) =
        viewModelScope.launch { dao.updateSelectedInInventory(id, isSelected) }

    fun toggleSelectedInInventory(id: Long) =
        viewModelScope.launch { dao.toggleSelectedInInventory(id) }

    fun deleteSelectedInventoryItems() =
        viewModelScope.launch { dao.deleteSelectedShoppingListItems() }

    fun selectAllInventoryItems() = viewModelScope.launch {
        dao.selectInventoryItems(inventory.value?.map { it.id } ?: emptyList())
    }

    fun clearInventorySelection() = viewModelScope.launch { dao.clearInventorySelection() }

    fun addToInventoryFromSelectedShoppingListItems() =
        viewModelScope.launch { dao.addToInventoryFromSelectedShoppingListItems() }

    fun updateAddToShoppingList(id: Long, autoAddToShoppingList: Boolean) =
        viewModelScope.launch { dao.updateAutoAddToShoppingList(id, autoAddToShoppingList) }

    fun updateAddToShoppingListTrigger(id: Long, autoAddToShoppingListAmount: Int) =
        viewModelScope.launch { dao.updateAutoAddToShoppingListAmount(id, autoAddToShoppingListAmount) }

}

fun bootyCrateViewModel(context: Context) =
    ViewModelProvider(context.asFragmentActivity()).get(BootyCrateViewModel::class.java)