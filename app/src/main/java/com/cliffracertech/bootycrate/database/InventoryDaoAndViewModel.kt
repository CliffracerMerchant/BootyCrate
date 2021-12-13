/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import com.cliffracertech.bootycrate.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Entity(tableName = "inventory")
open class DatabaseInventory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Long = 0,
    @ColumnInfo(name="name")
    var name: String,
    @ColumnInfo(name="isSelected", defaultValue="0")
    var isSelected: Boolean = false,
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
                isSelected == other.isSelected
    }

    override fun toString() = """
id = $id
name = $name
shoppingListItemCount = $shoppingListItemCount
inventoryItemCount = $inventoryItemCount
isSelected = $isSelected
"""

    override fun hashCode() = shoppingListItemCount * 31 + inventoryItemCount
}

private const val shoppingListItemCount = "(SELECT count(*) FROM bootycrate_item " +
                                          "WHERE bootycrate_item.inventoryId = inventory.id " +
                                          "AND bootycrate_item.shoppingListAmount != -1 " +
                                          "AND NOT bootycrate_item.inShoppingListTrash) " +
                                          "AS shoppingListItemCount"
private const val inventoryItemCount = "(SELECT count(*) FROM bootycrate_item " +
                                       "WHERE bootycrate_item.inventoryId = inventory.id " +
                                       "AND bootycrate_item.inventoryAmount != -1 " +
                                       "AND NOT bootycrate_item.inInventoryTrash) " +
                                       "AS inventoryItemCount"

@Dao abstract class BootyCrateInventoryDao {
    @Insert abstract suspend fun add(inventory: DatabaseInventory): Long
    @Insert abstract suspend fun add(items: List<DatabaseInventory>): List<Long>
    @Query("INSERT INTO inventory (name, isSelected) VALUES (:name, 1)")
    abstract suspend fun add(name: String): Long

    @Query("UPDATE inventory SET name = :name WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("SELECT id, name, $shoppingListItemCount, $inventoryItemCount, isSelected FROM inventory")
    abstract fun getAll() : Flow<List<BootyCrateInventory>>

    @Query("SELECT * FROM inventory")
    abstract fun getAllNow() : List<DatabaseInventory>

    @Query("SELECT id, name, $shoppingListItemCount, $inventoryItemCount, isSelected " +
           "FROM inventory WHERE isSelected")
    abstract fun getSelectedInventories(): Flow<List<BootyCrateInventory>>

    @Query("UPDATE inventory SET isSelected = (1 - isSelected) WHERE id = :id")
    abstract suspend fun updateIsSelected(id: Long)

    @Query("UPDATE inventory SET isSelected = 1")
    abstract suspend fun selectAll()

    @Query("DELETE FROM inventory WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM inventory")
    abstract suspend fun deleteAll()
}

class InventoryViewModel(app: Application): AndroidViewModel(app) {
    private val dao = BootyCrateDatabase.get(app).inventoryDao()
    private val dbSettingsDao = BootyCrateDatabase.get(app).dbSettingsDao()
    private val nameForMultiSelection = app.getString(R.string.multiple_selected_inventories_description)

    val multiSelect = dbSettingsDao.getMultiSelectInventories()
        .stateIn(viewModelScope, SharingStarted.Eagerly,
            runBlocking { dbSettingsDao.getMultiSelectInventoriesNow() })

    fun toggleMultiSelect() = viewModelScope.launch {
        dbSettingsDao.updateMultiSelectInventories(!multiSelect.value)
    }

    val inventories = dao.getAll().asLiveData()

    val selectedInventories = dao.getSelectedInventories().asLiveData()

    val selectedInventoryName = dao.getSelectedInventories().map {
        if (it.size == 1) it.first().name
        else nameForMultiSelection
    }.stateIn(viewModelScope, SharingStarted.Lazily, nameForMultiSelection)

    fun add(name: String) = viewModelScope.launch { dao.add(name) }

    fun delete(id: Long) = viewModelScope.launch { dao.delete(id) }

    fun updateName(id: Long, name: String) = viewModelScope.launch { dao.updateName(id, name) }

    /** Update the selection state of the inventory whose id is equal to the
     * parameter id. If the database is in single select inventory mode, this
     * will select the given inventory; it will otherwise toggle the selection. */
    fun updateIsSelected(id: Long) = viewModelScope.launch { dao.updateIsSelected(id) }

    fun selectAll() = viewModelScope.launch { dao.selectAll() }
}