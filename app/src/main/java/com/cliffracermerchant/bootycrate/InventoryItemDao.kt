/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.*

/** A Room DAO for BootyCrateDatabase's inventory_item table. */
@Dao abstract class InventoryItemDao : DataAccessObject<InventoryItem>() {
    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY color")
    abstract override fun getAllSortedByColor(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY name ASC")
    abstract override fun getAllSortedByNameAsc(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY name DESC")
    abstract override fun getAllSortedByNameDesc(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY amount ASC")
    abstract override fun getAllSortedByAmountAsc(filter: String): LiveData<List<InventoryItem>>

    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY amount DESC")
    abstract override fun getAllSortedByAmountDesc(filter: String): LiveData<List<InventoryItem>>

    @Insert abstract override suspend fun insert(item: InventoryItem): Long

    @Insert abstract override suspend fun insert(vararg items: InventoryItem)

    @Query("INSERT INTO inventory_item (name, extraInfo, color) " +
           "SELECT name, extraInfo, color " +
           "FROM shopping_list_item " +
           "WHERE shopping_list_item.linkedInventoryItemId IS NULL " +
           "AND shopping_list_item.inTrash = 0 " +
           "AND shopping_list_item.id IN (:shoppingListItemIds)")
    abstract suspend fun insertFromShoppingListItems(vararg shoppingListItemIds: Long)

    @Query("UPDATE inventory_item " +
           "SET name = :name " +
           "WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("UPDATE inventory_item " +
           "SET amount = :amount " +
           "WHERE id = :id")
    abstract suspend fun updateAmount(id: Long, amount: Int)

    @Query("UPDATE inventory_item " +
           "SET extraInfo = :extraInfo " +
           "WHERE id = :id")
    abstract suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("UPDATE Inventory_item " +
           "SET autoAddToShoppingList = :autoAddToShoppingList " +
           "WHERE id = :id")
    abstract suspend fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean)

    @Query("UPDATE inventory_item " +
           "SET autoAddToShoppingListTrigger = :autoAddToShoppingListTrigger " +
           "WHERE id = :id")
    abstract suspend fun updateAutoAddToShoppingListTrigger(id: Long, autoAddToShoppingListTrigger: Int)

    @Query("DELETE FROM inventory_item")
    abstract override suspend fun deleteAll()

    @Query("UPDATE shopping_list_item " +
           "SET linkedInventoryItemId = NULL " +
           "WHERE linkedInventoryItemId in " +
           "(SELECT id FROM inventory_item WHERE inTrash = 1)")
    abstract suspend fun clearShoppingListItemLinks()

    @Query("UPDATE inventory_item " +
           "SET color = :color " +
           "WHERE id = :id")
    abstract suspend fun updateColor(id: Long, color: Int)

    @Query("DELETE FROM inventory_item " +
           "WHERE inTrash = 1")
    abstract suspend fun emptyTrashPrivate()

    @Transaction override suspend fun emptyTrash() {
        clearShoppingListItemLinks()
        emptyTrashPrivate()
    }

    @Query("UPDATE inventory_item " +
           "SET inTrash = 1 " +
           "WHERE id IN (:ids)")
    abstract suspend fun moveToTrash(vararg ids: Long)

    @Transaction override suspend fun delete(vararg ids: Long) {
        clearShoppingListItemLinks()
        emptyTrashPrivate()
        moveToTrash(*ids)
    }

    @Query("UPDATE inventory_item " +
           "SET inTrash = 0 " +
           "WHERE inTrash = 1")
    abstract override suspend fun undoDelete()
}