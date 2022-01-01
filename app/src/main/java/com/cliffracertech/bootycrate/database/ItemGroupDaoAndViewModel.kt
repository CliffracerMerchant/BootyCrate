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
import com.cliffracertech.bootycrate.dlog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** A database representation of a group of ListItems. **/
@Entity(tableName = "itemGroup")
open class DatabaseItemGroup(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Long = 0,
    @ColumnInfo(name="name")
    var name: String,
    @ColumnInfo(name="isSelected", defaultValue="0")
    var isSelected: Boolean = false,
)

/** A subclass of DatabaseItemGroup that also contains the shopping list item
 * and inventory item counts for the item group. Because these are calculated
 * with an SQL selection, these extra values do not need to be stored in the
 * database. */
class ItemGroup(
    id: Long = 0,
    name: String,
    var shoppingListItemCount: Int,
    var inventoryItemCount: Int,
) : DatabaseItemGroup(id, name) {
    override fun equals(other: Any?) = when (other) {
        null -> false
        !is ItemGroup -> false
        else -> id == other.id && name == other.name &&
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

private const val shoppingListItemCount = "(SELECT count(*) FROM item " +
                                          "WHERE item.groupId = itemGroup.id " +
                                          "AND item.shoppingListAmount != -1 " +
                                          "AND NOT item.inShoppingListTrash) " +
                                          "AS shoppingListItemCount"
private const val inventoryItemCount = "(SELECT count(*) FROM item " +
                                       "WHERE item.groupId = itemGroup.id " +
                                       "AND item.inventoryAmount != -1 " +
                                       "AND NOT item.inInventoryTrash) " +
                                       "AS inventoryItemCount"

@Dao abstract class ItemGroupDao {
    @Insert abstract suspend fun add(itemGroup: DatabaseItemGroup): Long
    @Insert abstract suspend fun add(itemGroups: List<DatabaseItemGroup>): List<Long>
    @Query("INSERT INTO itemGroup (name, isSelected) VALUES (:name, 1)")
    abstract suspend fun add(name: String): Long

    @Query("UPDATE itemGroup SET name = :name WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("SELECT id, name, $shoppingListItemCount, $inventoryItemCount, isSelected FROM itemGroup")
    abstract fun getAll() : Flow<List<ItemGroup>>

    @Query("SELECT * FROM itemGroup")
    abstract fun getAllNow() : List<DatabaseItemGroup>

    @Query("SELECT id, name, $shoppingListItemCount, $inventoryItemCount, isSelected " +
           "FROM itemGroup WHERE isSelected")
    abstract fun getSelectedGroups(): Flow<List<ItemGroup>>

    @Query("SELECT id, name, $shoppingListItemCount, $inventoryItemCount, isSelected " +
           "FROM itemGroup WHERE isSelected")
    abstract suspend fun getSelectedGroupsNow(): List<ItemGroup>

    @Query("UPDATE itemGroup SET isSelected = (1 - isSelected) WHERE id = :id")
    abstract suspend fun updateIsSelected(id: Long)

    @Query("UPDATE itemGroup SET isSelected = 1")
    abstract suspend fun selectAll()

    @Query("DELETE FROM itemGroup WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM itemGroup")
    abstract suspend fun deleteAll()
}

/** An AndroidViewModel for an InventorySelector.
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
class ItemGroupSelectorViewModel(app: Application): AndroidViewModel(app) {
    private val itemGroupDao = BootyCrateDatabase.get(app).itemGroupDao()
    private val settingsDao = BootyCrateDatabase.get(app).settingsDao()

    val itemGroups = itemGroupDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val multiSelectGroups = settingsDao.getMultiSelectGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(),
            runBlocking { settingsDao.getMultiSelectGroupsNow() })

    fun toggleMultiSelect() { viewModelScope.launch {
        settingsDao.toggleMultiSelectGroups()
    }}
    fun onAddNewItemGroupRequest(name: String) {
        viewModelScope.launch { itemGroupDao.add(name) }
    }
    fun onDeleteItemGroupRequest(id: Long) {
        viewModelScope.launch { itemGroupDao.delete(id) }
    }
    fun onUpdateItemGroupNameRequest(id: Long, name: String) {
        viewModelScope.launch { itemGroupDao.updateName(id, name) }
    }

    fun onItemGroupClick(id: Long) {
        viewModelScope.launch { itemGroupDao.updateIsSelected(id) }
    }
    fun onSelectAllGroupsRequest() {
        viewModelScope.launch { itemGroupDao.selectAll() }
    }
}

/** An AndroidViewModel to expose the contents of the database's itemGroup table.
 *
 * A Flow of the list of all item groups in the database can be accessed
 * through the public property inventories. The property selectedInventories
 * exposes a Flow of the list of all inventories whose property isSelected is
 * true. The property selectedInventoryName is a StateFlow<String> whose
 * current value will be equal to the name of the selected inventory if only
 * one is selected, or a string to express that multiple inventories are
 * selected otherwise.
 *
 * The functions add, delete, and updateName can be used to add new
 * item groups, delete existing ones, or update the name of an existing
 * item group, respectively.
 *
 * The StateFlow multiSelect's current value represents the database's current
 * allowance for multi-selecting item groups. If false, selecting an item group
 * other than the current one will deselect the previous selected inventory.
 * multiSelect's value can be toggled with the function toggleMultiSelect. The
 * isSelected state of individual inventories is updated by calling updateIsSelected
 * with the inventory's id. When in multiselect mode, this will toggle the
 * inventory's isSelected state. It will single select it otherwise. The
 * function selectAll is provided for convenience, but will not work if the
 * database is not in multiselect inventory mode.
 */
class ItemGroupViewModel(app: Application): AndroidViewModel(app) {
    private val itemGroupDao = BootyCrateDatabase.get(app).itemGroupDao()
    private val settingsDao = BootyCrateDatabase.get(app).settingsDao()
    private val nameForMultiSelection = app.getString(R.string.multiple_selected_item_groups_description)

    val itemGroups = itemGroupDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val selectedItemGroups get() = runBlocking { itemGroupDao.getSelectedGroupsNow() }

    val selectedItemGroupName = itemGroupDao.getSelectedGroups().map {
        if (it.size == 1) it.first().name
        else nameForMultiSelection
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    val multiSelect get() = runBlocking { settingsDao.getMultiSelectGroupsNow() }

    fun toggleMultiSelect() { viewModelScope.launch {
        settingsDao.toggleMultiSelectGroups()
    }}
    fun add(name: String) {
        viewModelScope.launch { itemGroupDao.add(name) }
    }
    fun delete(id: Long) {
        viewModelScope.launch { itemGroupDao.delete(id) }
    }
    fun updateName(id: Long, name: String) {
        viewModelScope.launch { itemGroupDao.updateName(id, name) }
    }

    /** Update the selection state of the inventory whose id is equal to the
     * parameter id. If the database is in single select inventory mode, this
     * will select the given inventory; it will otherwise toggle the selection. */
    fun updateIsSelected(id: Long) {
        viewModelScope.launch { itemGroupDao.updateIsSelected(id) }
    }
    fun selectAll() {
        viewModelScope.launch { itemGroupDao.selectAll() }
    }
}