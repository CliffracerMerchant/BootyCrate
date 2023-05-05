/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import androidx.room.*

/** A Room DAO that provides methods to manipulate a database of [ListItem]s. */
@Dao abstract class ListItemDao {

    protected companion object {
        private const val likeSearchFilter = "(item.name LIKE :filter OR extraInfo LIKE :filter)"
        const val onShoppingList = "shoppingListAmount != -1 AND NOT inShoppingListTrash"
        const val inInventory = "inventoryAmount != -1 AND NOT inInventoryTrash"
        const val withSelectedGroups = "WITH selectedGroups AS (SELECT name FROM itemGroup WHERE isSelected)"

        private const val shoppingListItemFields =
            "item.id, item.name, extraInfo, colorGroup, " +
            "shoppingListAmount as amount, " +
            "$inInventory as linked, isChecked as checked"

        private const val inventoryItemFields =
            "item.id, item.name, extraInfo, colorGroup, " +
            "inventoryAmount as amount, " +
            "$onShoppingList as linked, " +
            "autoAddToShoppingList, autoAddToShoppingListAmount"

        const val selectShoppingListItems =
            "SELECT $shoppingListItemFields FROM item " +
            "JOIN itemGroup ON item.groupName = itemGroup.name " +
            "WHERE $likeSearchFilter AND $onShoppingList AND itemGroup.isSelected"

        const val selectInventoryItems =
            "SELECT $inventoryItemFields FROM item " +
            "JOIN itemGroup ON item.groupName = itemGroup.name " +
            "WHERE $likeSearchFilter AND $inInventory AND itemGroup.isSelected"
    }

    @Query("SELECT * FROM item")
    abstract fun getAllNow(): List<DatabaseListItem>

    @Insert abstract suspend fun add(item: DatabaseListItem): Long
    @Insert abstract suspend fun add(items: List<DatabaseListItem>)

    @Query("DELETE FROM item")
    abstract fun deleteAll()

    @Query("UPDATE item SET name = :name WHERE id = :id")
    abstract suspend fun setName(id: Long, name: String)

    @Query("UPDATE item SET extraInfo = :extraInfo WHERE id = :id")
    abstract suspend fun setExtraInfo(id: Long, extraInfo: String)

    @Query("UPDATE item SET colorGroup = :colorGroup WHERE id = :id")
    abstract suspend fun setColorGroup(id: Long, colorGroup: ListItem.ColorGroup)
}