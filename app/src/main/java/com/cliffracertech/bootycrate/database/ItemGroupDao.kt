/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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