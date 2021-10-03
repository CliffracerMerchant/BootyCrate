/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.*
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Entity(tableName = "inventory")
open class DatabaseInventory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Long = 0,
    @ColumnInfo(name="name")
    var name: String,
    @ColumnInfo(name="isExpanded", defaultValue="0")
    var isExpanded: Boolean = false,
    @ColumnInfo(name="useInCombinedShoppingList", defaultValue="1")
    var useInCombinedShoppingList: Boolean = true,
)

class BootyCrateInventory(
    id: Long = 0,
    name: String,
    var shoppingListItemCount: Int,
    var inventoryItemCount: Int,
) : DatabaseInventory(id, name) {
    override fun equals(other: Any?) = when (other) {
        null -> false
        !is BootyCrateInventory -> false
        else ->  id == other.id && name == other.name &&
                shoppingListItemCount == other.shoppingListItemCount &&
                inventoryItemCount == other.inventoryItemCount &&
                isExpanded == other.isExpanded &&
                useInCombinedShoppingList == other.useInCombinedShoppingList
    }

    override fun toString() = """
id = $id
name = $name
shoppingListItemCount = $shoppingListItemCount
inventoryItemCount = $inventoryItemCount
isExpanded = $isExpanded
useInCombinedShoppingList = $useInCombinedShoppingList
"""

    override fun hashCode() = shoppingListItemCount * 31 + inventoryItemCount
}

private const val shoppingListItemCount = "(SELECT count(*) FROM bootycrate_item " +
                                          "WHERE bootycrate_item.inventoryId = inventory.id " +
                                          "AND bootycrate_item.shoppingListAmount != -1) " +
                                          "AS shoppingListItemCount"
private const val inventoryItemCount = "(SELECT count(*) FROM bootycrate_item " +
                                       "WHERE bootycrate_item.inventoryId = inventory.id " +
                                       "AND bootycrate_item.inventoryAmount != -1) " +
                                       "AS inventoryItemCount"
private const val allFields = "id, name, $shoppingListItemCount, $inventoryItemCount, isExpanded, useInCombinedShoppingList"

@Dao abstract class BootyCrateInventoryDao {
    @Insert abstract suspend fun add(inventory: DatabaseInventory): Long
    @Insert abstract suspend fun add(items: List<DatabaseInventory>)

    @Query("UPDATE inventory SET name = :name WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("SELECT $allFields FROM inventory")
    abstract fun getAll() : LiveData<List<BootyCrateInventory>>

    @Query("SELECT * FROM inventory")
    abstract fun getAllNow() : List<DatabaseInventory>

    @Query("UPDATE inventory SET isExpanded = 1 WHERE id = :id")
    protected abstract suspend fun expand(id: Long)

    @Query("UPDATE inventory SET isExpanded = 0")
    abstract suspend fun clearExpandedItem()

    @Transaction open suspend fun setExpandedItem(id: Long?) {
        clearExpandedItem()
        if (id != null) expand(id)
    }

    @Query("UPDATE inventory SET useInCombinedShoppingList = :useInCombinedShoppingList WHERE id = :id")
    abstract suspend fun updateUseInCombinedShoppingList(id: Long, useInCombinedShoppingList: Boolean)

    @Query("DELETE FROM inventory WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM inventory")
    abstract suspend fun deleteAll()
}

fun inventoryViewModel(context: Context) =
    ViewModelProvider(context.asFragmentActivity()).get(InventoryViewModel::class.java)

class InventoryViewModel(app: Application): AndroidViewModel(app) {
    private val dao = BootyCrateDatabase.get(app).inventoryDao()

    private val _selectedInventoryId = MutableStateFlow(-1L)
    val selectedInventoryId = _selectedInventoryId.asStateFlow()

    val inventories = dao.getAll()

    fun updateName(id: Long, name: String) = viewModelScope.launch { dao.updateName(id, name) }

    fun setExpandedItem(id: Long?) = viewModelScope.launch { dao.setExpandedItem(id) }

    fun updateUseInCombinedShoppingList(id: Long, useInCombinedShoppingList: Boolean) =
        viewModelScope.launch { dao.updateUseInCombinedShoppingList(id, useInCombinedShoppingList) }
}