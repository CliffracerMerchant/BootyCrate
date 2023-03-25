/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** A database representation of a group of [ListItem]s. **/
@Entity(tableName = "itemGroup")
open class DatabaseItemGroup(
    @PrimaryKey
    @ColumnInfo(name="name")
    val name: String,
    @ColumnInfo(name="isSelected", defaultValue="0")
    val isSelected: Boolean = false)

/** A subclass of [DatabaseItemGroup] that also contains the
 * [ShoppingListItem] and [InventoryItem] counts for the [ItemGroup]. */
class ItemGroup(
    name: String,
    val shoppingListItemCount: Int,
    val inventoryItemCount: Int,
    isSelected: Boolean = false,
) : DatabaseItemGroup(name, isSelected) {

    override fun toString() = """
        name = $name
        isSelected = $isSelected
        shoppingListItemCount = $shoppingListItemCount
        inventoryItemCount = $inventoryItemCount
    """.trimIndent()
}

private const val shoppingListItemCount = "(SELECT count(*) FROM item " +
                                          "WHERE item.groupName = itemGroup.name " +
                                          "AND item.shoppingListAmount != -1 " +
                                          "AND NOT item.inShoppingListTrash) " +
                                          "AS shoppingListItemCount"
private const val inventoryItemCount = "(SELECT count(*) FROM item " +
                                       "WHERE item.groupName = itemGroup.name " +
                                       "AND item.inventoryAmount != -1 " +
                                       "AND NOT item.inInventoryTrash) " +
                                       "AS inventoryItemCount"

@Dao abstract class ItemGroupDao {
    @Insert abstract suspend fun add(itemGroup: DatabaseItemGroup): Long
    @Insert abstract suspend fun add(itemGroups: List<DatabaseItemGroup>): List<Long>
    @Query("INSERT INTO itemGroup (name, isSelected) VALUES (:name, 1)")
    abstract suspend fun add(name: String): Long

    @Query("UPDATE itemGroup SET name = :name WHERE name = :oldName")
    abstract suspend fun setName(oldName: String, name: String)

    @Query("SELECT name, $shoppingListItemCount, " +
                  "$inventoryItemCount, isSelected " +
           "FROM itemGroup")
    abstract fun getAll() : Flow<List<ItemGroup>>

    @Query("SELECT * FROM itemGroup")
    abstract fun getAllNow() : List<DatabaseItemGroup>

    @Query("SELECT name, $shoppingListItemCount, $inventoryItemCount, isSelected " +
           "FROM itemGroup WHERE isSelected")
    abstract fun getSelectedGroups(): Flow<List<ItemGroup>>

    @Query("UPDATE itemGroup SET isSelected = (1 - isSelected) WHERE name = :name")
    abstract suspend fun toggleIsSelected(name: String)

    @Query("UPDATE itemGroup SET isSelected = 1")
    abstract suspend fun selectAll()

    @Query("DELETE FROM itemGroup WHERE name = :name")
    abstract suspend fun delete(name: String)

    @Query("DELETE FROM itemGroup")
    abstract suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT name FROM itemGroup WHERE name = :name)")
    abstract suspend fun nameAlreadyUsed(name: String): Boolean
}