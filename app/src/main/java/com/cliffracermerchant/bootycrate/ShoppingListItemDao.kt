/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.*

/** A Room DAO for BootyCrateDatabase's shopping_list_item table. */
@Dao abstract class ShoppingListItemDao : DataAccessObject<ShoppingListItem>() {
    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY color")
    abstract override fun getAllSortedByColor(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY name ASC")
    abstract override fun getAllSortedByNameAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY name DESC")
    abstract override fun getAllSortedByNameDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY amount ASC")
    abstract override fun getAllSortedByAmountAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY amount DESC")
    abstract override fun getAllSortedByAmountDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("INSERT INTO shopping_list_item (name, extraInfo, color, " +
                                           "linkedInventoryItemId) " +
           "SELECT name, extraInfo, color, id " +
           "FROM inventory_item " +
           "WHERE inventory_item.id IN (:inventoryItemIds) " +
           "AND inventory_item.id NOT IN (SELECT linkedInventoryItemId " +
                                         "FROM shopping_list_item " +
                                         "WHERE linkedInventoryItemId NOT NULL " +
                                         "AND NOT inTrash)")
    abstract suspend fun addFromInventoryItems(inventoryItemIds: LongArray)

    @Transaction
    open suspend fun autoAddFromInventoryItem(inventoryItemId: Long, minAmount: Int) {
        addFromInventoryItems(LongArray(1) { inventoryItemId })
        setMinimumAmountFromLinkedItem(inventoryItemId, minAmount)
    }

    @Query("UPDATE shopping_list_item " +
           "SET name = :name WHERE id = :id")
    abstract override suspend fun updateName(id: Long, name: String)

    @Query("UPDATE shopping_list_item " +
           "SET name = :name " +
           "WHERE linkedInventoryItemId = :inventoryItemId")
    abstract suspend fun updateNameFromLinkedInventoryItem(inventoryItemId: Long, name: String)

    @Query("UPDATE shopping_list_item " +
           "SET extraInfo = :extraInfo " +
           "WHERE id = :id")
    abstract override suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("UPDATE shopping_list_item " +
           "SET color = :color " +
           "WHERE id = :id")
    abstract override suspend fun updateColor(id: Long, color: Int)

    @Query("UPDATE shopping_list_item " +
            "SET amount = :amount " +
            "WHERE id = :id")
    abstract override suspend fun updateAmount(id: Long, amount: Int)

    @Query("UPDATE shopping_list_item " +
            "SET extraInfo = :extraInfo " +
            "WHERE linkedInventoryItemId = :inventoryItemId")
    abstract suspend fun updateExtraInfoFromLinkedInventoryItem(inventoryItemId: Long, extraInfo: String)

    @Query("UPDATE shopping_list_item " +
           "SET isChecked = :isChecked " +
           "WHERE id = :id")
    abstract suspend fun updateIsChecked(id: Long, isChecked: Boolean)

    @Query("UPDATE shopping_list_item " +
           "SET amount = CASE WHEN amount < :minAmount THEN :minAmount " +
                                   "ELSE amount END " +
           "WHERE linkedInventoryItemId = :inventoryItemId")
    abstract suspend fun setMinimumAmountFromLinkedItem(inventoryItemId: Long, minAmount: Int)

    @Query("UPDATE shopping_list_item " +
           "SET amount = :amount " +
           "WHERE linkedInventoryItemId = :inventoryItemId")
    abstract suspend fun updateAmountFromLinkedItem(inventoryItemId: Long, amount: Int)

    @Query("UPDATE shopping_list_item " +
           "SET linkedInventoryItemId = :linkedInventoryItemId, " +
               "name = :linkedInventoryItemName, " +
               "extraInfo = :linkedInventoryItemExtraInfo " +
           "WHERE id = :id")
    abstract suspend fun updateLinkedInventoryItemId(id: Long, linkedInventoryItemId: Long,
                                                     linkedInventoryItemName: String,
                                                     linkedInventoryItemExtraInfo: String)

    @Query("UPDATE shopping_list_item " +
           "SET linkedInventoryItemId = null " +
           "WHERE id = :id")
    abstract suspend fun removeInventoryItemLink(id: Long)

    @Query("WITH bought_amounts AS (SELECT linkedInventoryItemId AS id, " +
                                          "amount AS amountBought " +
                                   "FROM shopping_list_item " +
                                   "WHERE linkedInventoryItemId IS NOT NULL " +
                                   "AND isChecked = 1) " +
            "UPDATE inventory_item " +
            "SET amount = amount + (SELECT amountBought FROM bought_amounts " +
                                   "WHERE id = inventory_item.id) " +
            "WHERE id IN (SELECT id FROM bought_amounts)")
    abstract suspend fun updateInventoryItemAmountsFromShoppingList()

    @Query("DELETE FROM shopping_list_item WHERE isChecked = 1")
    abstract suspend fun clearCheckedItems()

    /** The checkout function allows the user to clear all checked shopping
     *  list items at once and modify the amounts of linked inventory items
     *  appropriately. For non-linked shopping list items, which are simply
     *  removed from the list, the checkout function acts no differently than
     *  removing them via swiping or the delete button. */
    @Transaction open suspend fun checkOut() {
        updateInventoryItemAmountsFromShoppingList()
        clearCheckedItems()
    }

    @Query("DELETE FROM shopping_list_item")
    abstract override suspend fun deleteAll()

    @Query("DELETE FROM shopping_list_item " +
           "WHERE inTrash = 1")
    abstract override suspend fun emptyTrash()

    @Query("UPDATE shopping_list_item " +
           "SET inTrash = 1 " +
           "WHERE id IN (:ids)")
    abstract suspend fun moveToTrash(ids: LongArray)

    @Transaction override suspend fun delete(ids: LongArray) {
        emptyTrash()
        moveToTrash(ids)
    }

    @Query("UPDATE shopping_list_item " +
           "SET inTrash = 0 " +
           "WHERE inTrash = 1")
    abstract override suspend fun undoDelete()

    @Query("SELECT * FROM shopping_list_item")
    abstract override fun getAllNow(): List<ShoppingListItem>
}