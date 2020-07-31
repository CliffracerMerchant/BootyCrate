/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/** A viewmodel to expose the data set of the shopping_list_item table.
 *
 *  ShoppingListViewModel exposes the contents of the shopping_list_item table,
 *  according to its sort and searchFilter properties, through the public pro-
 *  perty items: LiveData<List<ShoppingListItem>>. Setting the sort or search-
 *  Filter to a new value will automatically update the LiveData with the new
 *  data.
 *
 *  When a single new item is inserted, the property newlyInsertedItemId will
 *  be set equal to the new items id. This allows external entities the possi-
 *  bility of treating a newly inserted item differently that it does existing
 *  items. The function resetNewlyInsertedItemId should generally be called
 *  after the newlyInsertedItemId value is used so that the item will not be
 *  considered a new item long after it is inserted if no other item has since
 *  been inserted. */
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
