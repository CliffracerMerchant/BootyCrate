package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class InventoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: InventoryItemDao = BootyCrateDatabase.get(app).inventoryItemDao()
    private val items: LiveData<List<InventoryItem>> = dao.getAll()

    init { viewModelScope.launch{ dao.emptyTrash() } }

    fun getAll() : LiveData<List<InventoryItem>> = items

    fun insert(vararg items: InventoryItem) = viewModelScope.launch { dao.insert(*items) }

    fun updateName(id: Long, name: String) = viewModelScope.launch {
        dao.updateName(id, name)
    }
    fun updateAmount(id: Long, amount: Int) = viewModelScope.launch {
        dao.updateAmount(id, amount)
    }
    fun updateExtraInfo(id: Long, extraInfo: String) = viewModelScope.launch {
        dao.updateExtraInfo(id, extraInfo)
    }
    fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean) =
            viewModelScope.launch {
        dao.updateAutoAddToShoppingList(id, autoAddToShoppingList)
    }
    fun updateAutoAddToShoppingListTrigger(id: Long, autoAddToShoppingListTrigger: Int) =
            viewModelScope.launch {
        dao.updateAutoAddToShoppingListTrigger(id, autoAddToShoppingListTrigger)
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
