package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class InventoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: InventoryItemDao = BootyCrateDatabase.get(app).inventoryItemDao()
    private val sortAndFilterLiveData =
        MutableLiveData(Pair<Sort?, String?>(Sort.Color, ""))
    val items = Transformations.switchMap(sortAndFilterLiveData) { sortAndFilter ->
        val filter = '%' + (sortAndFilter.second ?: "") + '%'
        when (sortAndFilter.first) {
            null -> dao.getAllSortedByColor(filter)
            Sort.Color -> dao.getAllSortedByColor(filter)
            Sort.NameAsc -> dao.getAllSortedByNameAsc(filter)
            Sort.NameDesc -> dao.getAllSortedByNameDesc(filter)
            Sort.AmountAsc -> dao.getAllSortedByAmountAsc(filter)
            Sort.AmountDesc -> dao.getAllSortedByAmountDesc(filter)
        }
    }

    var sort get() = sortAndFilterLiveData.value?.first
             set(value) { sortAndFilterLiveData.value = Pair(value, searchFilter) }
    var searchFilter get() = sortAndFilterLiveData.value?.second
                     set(value) { sortAndFilterLiveData.value = Pair(sort, value) }

    private var _newlyInsertedItemId = AtomicLong()
    val newlyInsertedItemId: Long get() = _newlyInsertedItemId.get()
    fun resetNewlyInsertedItemId() = _newlyInsertedItemId.set(0)

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun insert(item: InventoryItem) = viewModelScope.launch {
        _newlyInsertedItemId.set(dao.insert(item))
    }
    fun insert(vararg items: InventoryItem) = viewModelScope.launch {
        dao.insert(*items)
    }
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
