/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.*

/** A Room DAO for BootyCrateDatabase's shopping_list_item table. */
@Dao abstract class ShoppingListItemDao : DataAccessObject<ShoppingListItem>() {

    @Query("SELECT * FROM shopping_list_item")
    abstract override fun getAllNow(): List<ShoppingListItem>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY color""")
    abstract override fun getAllSortedByColor(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY name ASC""")
    abstract override fun getAllSortedByNameAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY name DESC""")
    abstract override fun getAllSortedByNameDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY amount ASC""")
    abstract override fun getAllSortedByAmountAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND name LIKE :filter AND extraInfo LIKE :filter ORDER BY amount DESC""")
    abstract override fun getAllSortedByAmountDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""INSERT INTO shopping_list_item (name, extraInfo, color, linkedItemId)
              SELECT name, extraInfo, color, id
              FROM inventory_item
              WHERE id IN (:inventoryItemIds)
              AND inTrash = 0
              AND linkedItemId IS NULL""")
    abstract suspend fun addFromInventoryItems(inventoryItemIds: LongArray)

    @Query("""UPDATE shopping_list_item
              SET name = :name
              WHERE id = :id""")
    abstract override suspend fun updateName(id: Long, name: String)

    @Query("""UPDATE shopping_list_item
              SET extraInfo = :extraInfo
              WHERE id = :id""")
    abstract override suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("""UPDATE shopping_list_item
              SET color = :color
              WHERE id = :id""")
    abstract override suspend fun updateColor(id: Long, color: Int)

    @Query("""UPDATE shopping_list_item
              SET amount = :amount
              WHERE id = :id""")
    abstract override suspend fun updateAmount(id: Long, amount: Int)

    @Query("""UPDATE shopping_list_item
              SET isChecked = :isChecked
              WHERE id = :id""")
    abstract suspend fun updateIsChecked(id: Long, isChecked: Boolean)

    @Query("""DELETE FROM shopping_list_item""")
    abstract override suspend fun deleteAll()

    @Query("""UPDATE shopping_list_item
              SET inTrash = 1
              WHERE id IN (:ids)""")
    abstract override suspend fun delete(ids: LongArray)

    @Query("""UPDATE shopping_list_item
              SET inTrash = 0
              WHERE inTrash = 1""")
    abstract override suspend fun undoDelete()

    @Query("DELETE FROM shopping_list_item WHERE inTrash = 1")
    abstract override suspend fun emptyTrash()

    @Query("""WITH bought_amounts AS (SELECT linkedItemId, amount
                                     FROM shopping_list_item
                                     WHERE linkedItemId IS NOT NULL
                                     AND isChecked = 1)
              UPDATE inventory_item
              SET amount = amount + (SELECT amount FROM bought_amounts
                                     WHERE linkedItemId = inventory_item.id)
              WHERE id IN (SELECT linkedItemId FROM bought_amounts)""")
    protected abstract suspend fun updateInventoryItemAmountsFromShoppingList()

    @Query("DELETE FROM shopping_list_item WHERE isChecked = 1")
    protected abstract suspend fun deleteCheckedItems()

    /** The checkout function allows the user to clear all checked shopping
     *  list items at once and modify the amounts of linked inventory items
     *  appropriately. For non-linked shopping list items, which are simply
     *  removed from the list, the checkout function acts no differently than
     *  removing them via swiping or the delete button. */
    @Transaction open suspend fun checkout() {
        updateInventoryItemAmountsFromShoppingList()
        deleteCheckedItems()
    }
}