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
    fun updateName(item: ShoppingListItem, name: String) = viewModelScope.launch {
        dao.updateName(item.id, name)
    }
    fun updateExtraInfo(item: ShoppingListItem, extraInfo: String) = viewModelScope.launch {
        dao.updateExtraInfo(item.id, extraInfo)
    }
    fun updateAmountInCart(item: ShoppingListItem, amountInCart: Int) = viewModelScope.launch {
        dao.updateAmountInCart(item.id, amountInCart)
    }
    fun updateAmount(item: ShoppingListItem, amount: Int) = viewModelScope.launch {
        dao.updateAmount(item.id, amount)
    }
    fun updateLinkedInventoryItemId(item: ShoppingListItem, linkedInventoryItemId: Long) = viewModelScope.launch {
        dao.updateLinkedInventoryItemId(item.id, linkedInventoryItemId)
    }
    fun delete(vararg items: ShoppingListItem) = viewModelScope.launch {
        dao.delete(*items)
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
