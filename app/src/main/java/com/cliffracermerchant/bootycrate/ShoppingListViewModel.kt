package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class ShoppingListViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: ShoppingListItemDao = BootyCrateDatabase.get(app).shoppingListItemDao()
    private val sortAndFilterLiveData = MutableLiveData(Pair<Sort?, String?>(Sort.Color, ""))

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

    fun insert(item: ShoppingListItem) = viewModelScope.launch {
        _newlyInsertedItemId.set(dao.insert(item))
    }
    fun insert(vararg items: ShoppingListItem) = viewModelScope.launch {
        dao.insert(*items)
    }
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
        dao.updateAmountOnList(id, amountOnList)
    }
    fun updateAmountOnListFromLinkedItem(inventoryItemId: Long, amount: Int) = viewModelScope.launch {
        dao.updateAmountOnListFromLinkedItem(inventoryItemId, amount)
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
