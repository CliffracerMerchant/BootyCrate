package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
abstract class ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash")
    abstract fun getAll(): LiveData<List<ShoppingListItem>>
    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash ORDER BY name ASC")
    abstract fun getAllSortedByNameAsc(): LiveData<List<ShoppingListItem>>
    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash ORDER BY name DESC")
    abstract fun getAllSortedByNameDesc(): LiveData<List<ShoppingListItem>>
    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash ORDER BY amount ASC")
    abstract fun getAllSortedByAmountAsc(): LiveData<List<ShoppingListItem>>
    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash ORDER BY amount DESC")
    abstract fun getAllSortedByAmountDesc(): LiveData<List<ShoppingListItem>>

    @Insert
    abstract suspend fun insert(vararg items: ShoppingListItem)

    @Query("INSERT INTO shopping_list_item (name, extraInfo, linkedInventoryItemId) " +
           "SELECT name, extraInfo, id " +
           "FROM inventory_item " +
           "WHERE inventory_item.id IN (:inventoryItemIds) " +
           "AND inventory_item.id NOT IN (SELECT linkedInventoryItemId from shopping_list_item)")
    abstract suspend fun insertFromInventoryItems(vararg inventoryItemIds: Long)

    @Query("UPDATE shopping_list_item " +
           "SET name = :name " +
           "WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("UPDATE shopping_list_item " +
           "SET name = :name " +
           "WHERE linkedInventoryItemId = :inventoryItemId")
    abstract suspend fun updateNameFromLinkedInventoryItem(inventoryItemId: Long, name: String)

    @Query("UPDATE shopping_list_item " +
           "SET extraInfo = :extraInfo " +
           "WHERE id = :id")
    abstract suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("UPDATE shopping_list_item " +
            "SET extraInfo = :extraInfo " +
            "WHERE linkedInventoryItemId = :inventoryItemId")
    abstract suspend fun updateExtraInfoFromLinkedInventoryItem(inventoryItemId: Long, extraInfo: String)

    @Query("UPDATE shopping_list_item " +
           "SET amountInCart = :amountInCart " +
           "WHERE id = :id")
    abstract suspend fun updateAmountInCart(id: Long, amountInCart: Int)

    @Query("UPDATE shopping_list_item " +
           "SET amount = :amount " +
           "WHERE id = :id")
    abstract suspend fun updateAmount(id: Long, amount: Int)

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

    @Query("DELETE FROM shopping_list_item")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM shopping_list_item " +
           "WHERE inTrash = 1")
    abstract suspend fun emptyTrash()

    @Query("UPDATE shopping_list_item " +
           "SET inTrash = 1 " +
           "WHERE id IN (:ids)")
    abstract suspend fun moveToTrash(vararg ids: Long)

    @Transaction
    open suspend fun delete(vararg ids: Long) {
        emptyTrash()
        moveToTrash(*ids)
    }

    @Query("UPDATE shopping_list_item " +
            "SET inTrash = 0 " +
            "WHERE inTrash = 1")
    abstract suspend fun undoDelete()
}
