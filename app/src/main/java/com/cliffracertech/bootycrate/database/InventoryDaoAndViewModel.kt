/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import com.cliffracertech.bootycrate.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** A data only class that represents an inventory that contains
 * BootyCrateItems, and an associated shopping list. **/
@Entity(tableName = "inventory")
open class Inventory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Long = 0,
    @ColumnInfo(name="name")
    var name: String,
    @ColumnInfo(name="isSelected", defaultValue="0")
    var isSelected: Boolean = false,
)

/** A subclass of Inventory that also contains the shopping
 * list item and inventory item count for the inventory. */
class InventorySummary(
    id: Long = 0,
    name: String,
    var shoppingListItemCount: Int,
    var inventoryItemCount: Int,
) : Inventory(id, name) {
    override fun equals(other: Any?) = when (other) {
        null -> false
        !is InventorySummary -> false
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

@Dao abstract class InventoryDao {
    @Insert abstract suspend fun add(inventory: Inventory): Long
    @Insert abstract suspend fun add(items: List<Inventory>): List<Long>
    @Query("INSERT INTO inventory (name, isSelected) VALUES (:name, 1)")
    abstract suspend fun add(name: String): Long

    @Query("UPDATE inventory SET name = :name WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("SELECT id, name, $shoppingListItemCount, $inventoryItemCount, isSelected FROM inventory")
    abstract fun getAll() : Flow<List<InventorySummary>>

    @Query("SELECT * FROM inventory")
    abstract fun getAllNow() : List<Inventory>

    @Query("SELECT id, name, $shoppingListItemCount, $inventoryItemCount, isSelected " +
           "FROM inventory WHERE isSelected")
    abstract fun getSelectedInventories(): Flow<List<InventorySummary>>

    @Query("SELECT id, name, $shoppingListItemCount, $inventoryItemCount, isSelected " +
           "FROM inventory WHERE isSelected")
    abstract suspend fun getSelectedInventoriesNow(): List<InventorySummary>

    @Query("UPDATE inventory SET isSelected = (1 - isSelected) WHERE id = :id")
    abstract suspend fun updateIsSelected(id: Long)

    @Query("UPDATE inventory SET isSelected = 1")
    abstract suspend fun selectAll()

    @Query("DELETE FROM inventory WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM inventory")
    abstract suspend fun deleteAll()
}

/** An AndroidViewModel to expose the contents of the database's inventory table.
 *
 * A Flow of the list of all inventories in the database can be accessed
 * through the public property inventories. The property selectedInventories
 * exposes a Flow of the list of all inventories whose property isSelected is
 * true. The property selectedInventoryName is a StateFlow<String> whose
 * current value will be equal to the name of the selected inventory if only
 * one is selected, or a string to express that multiple inventories are
 * selected otherwise.
 *
 * The functions add, delete, and updateName can be used to add new
 * inventories, delete existing ones, or update the name of an existing
 * inventory, respectively.
 *
 * The StateFlow multiSelect's current value represents the database's current
 * allowance for multi-selecting inventories. If false, selecting an inventory
 * other than the current one will deselect the previous selected inventory.
 * multiSelect's value can be toggled with the function toggleMultiSelect. The
 * isSelected state of individual inventories is updated by calling updateIsSelected
 * with the inventory's id. When in multiselect mode, this will toggle the
 * inventory's isSelected state. It will single select it otherwise. The
 * function selectAll is provided for convenience, but will not work if the
 * database is not in multiselect inventory mode.
 */
class InventoryViewModel(app: Application): AndroidViewModel(app) {
    private val dao = BootyCrateDatabase.get(app).inventoryDao()
    private val dbSettingsDao = BootyCrateDatabase.get(app).dbSettingsDao()
    private val nameForMultiSelection = app.getString(R.string.multiple_selected_inventories_description)

    val inventories = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val selectedInventories = dao.getSelectedInventories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    suspend fun getSelectedInventoriesNow() = dao.getSelectedInventoriesNow()

    val selectedInventoryName = dao.getSelectedInventories().map {
        if (it.size == 1) it.first().name
        else nameForMultiSelection
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    val multiSelect = dbSettingsDao.getMultiSelectInventories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(),
            runBlocking { dbSettingsDao.getMultiSelectInventoriesNow() })

    fun toggleMultiSelect() { viewModelScope.launch {
        dbSettingsDao.toggleMultiSelectInventories()
    }}
    fun add(name: String) {
        viewModelScope.launch { dao.add(name) }
    }
    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
    fun updateName(id: Long, name: String) {
        viewModelScope.launch { dao.updateName(id, name) }
    }

    /** Update the selection state of the inventory whose id is equal to the
     * parameter id. If the database is in single select inventory mode, this
     * will select the given inventory; it will otherwise toggle the selection. */
    fun updateIsSelected(id: Long) {
        viewModelScope.launch { dao.updateIsSelected(id) }
    }
    fun selectAll() {
        viewModelScope.launch { dao.selectAll() }
    }
}