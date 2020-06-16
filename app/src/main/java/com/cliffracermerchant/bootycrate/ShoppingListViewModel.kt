package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ShoppingListViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: ShoppingListItemDao = BootyCrateDatabase.get(app).shoppingListItemDao()
    private val items: LiveData<List<ShoppingListItem>> = dao.getAll()

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun getAll() : LiveData<List<ShoppingListItem>> = items

    fun insert(vararg items: ShoppingListItem) = viewModelScope.launch {
        dao.insert(*items)
    }
    fun insertFromInventoryItems(vararg itemIds: Long) = viewModelScope.launch {
        dao.insertFromInventoryItems(*itemIds)
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
    fun updateAmountInCart(id: Long, amountInCart: Int) = viewModelScope.launch {
        dao.updateAmountInCart(id, amountInCart)
    }
    fun updateAmount(id: Long, amount: Int) = viewModelScope.launch {
        dao.updateAmount(id, amount)
    }
    fun updateLinkedInventoryItemId(id: Long, linkedInventoryItem: InventoryItem) = viewModelScope.launch {
        dao.updateLinkedInventoryItemId(id, linkedInventoryItem.id,
                                        linkedInventoryItem.name,
                                        linkedInventoryItem.extraInfo)
    }
    fun delete(vararg ids: Long) = viewModelScope.launch {
        dao.delete(*ids)
    }
    fun deleteAll() = viewModelScope.launch {
        dao.deleteAll()
    }
    fun undoDelete() = viewModelScope.launch {
        dao.undoDelete()
    }
}
