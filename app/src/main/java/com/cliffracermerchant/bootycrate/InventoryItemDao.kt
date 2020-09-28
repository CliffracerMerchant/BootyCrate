/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.*

/** A Room DAO for BootyCrateDatabase's inventory_item table. */
@Dao abstract class InventoryItemDao : DataAccessObject<InventoryItem>() {

    @Query("SELECT * FROM inventory_item")
    abstract override fun getAllNow(): List<InventoryItem>

    @Query("""SELECT * FROM inventory_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY color""")
    abstract override fun getAllSortedByColor(filter: String): LiveData<List<InventoryItem>>

    @Query("""SELECT * FROM inventory_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY name ASC""")
    abstract override fun getAllSortedByNameAsc(filter: String): LiveData<List<InventoryItem>>

    @Query("""SELECT * FROM inventory_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY name DESC""")
    abstract override fun getAllSortedByNameDesc(filter: String): LiveData<List<InventoryItem>>

    @Query("""SELECT * FROM inventory_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY amount ASC""")
    abstract override fun getAllSortedByAmountAsc(filter: String): LiveData<List<InventoryItem>>

    @Query("""SELECT * FROM inventory_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY amount DESC""")
    abstract override fun getAllSortedByAmountDesc(filter: String): LiveData<List<InventoryItem>>

    @Query("""INSERT INTO inventory_item (name, extraInfo, color, linkedItemId)
              SELECT name, extraInfo, color, id
              FROM shopping_list_item
              WHERE id IN (:shoppingListItemIds)
              AND inTrash = 0
              AND linkedItemId IS NULL""")
    abstract suspend fun addFromShoppingListItems(shoppingListItemIds: LongArray)

    @Query("""UPDATE inventory_item
              SET name = :name
              WHERE id = :id""")
    abstract override suspend fun updateName(id: Long, name: String)

    @Query("""UPDATE inventory_item
              SET extraInfo = :extraInfo
              WHERE id = :id""")
    abstract override suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("""UPDATE inventory_item
              SET color = :color
              WHERE id = :id""")
    abstract override suspend fun updateColor(id: Long, color: Int)

    @Query("""UPDATE inventory_item
              SET amount = :amount
              WHERE id = :id""")
    abstract override suspend fun updateAmount(id: Long, amount: Int)

    @Query("""UPDATE Inventory_item
              SET addToShoppingList = :addToShoppingList
              WHERE id = :id""")
    abstract suspend fun updateAddToShoppingList(id: Long, addToShoppingList: Boolean)

    @Query("""UPDATE inventory_item
              SET addToShoppingListTrigger = :addToShoppingListTrigger
              WHERE id = :id""")
    abstract suspend fun updateAddToShoppingListTrigger(id: Long, addToShoppingListTrigger: Int)

    @Query("DELETE FROM inventory_item")
    abstract override suspend fun deleteAll()

    @Query("""UPDATE inventory_item
              SET inTrash = 1
              WHERE id IN (:ids)""")
    abstract override suspend fun delete(ids: LongArray)

    @Query("""UPDATE inventory_item
              SET inTrash = 0
              WHERE inTrash = 1""")
    abstract override suspend fun undoDelete()

    @Query("DELETE FROM inventory_item WHERE inTrash = 1")
    abstract override suspend fun emptyTrash()
}