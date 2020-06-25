package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
abstract class ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY color")
    abstract fun getAllSortedByColor(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY name ASC")
    abstract fun getAllSortedByNameAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY name DESC")
    abstract fun getAllSortedByNameDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY amountOnList ASC")
    abstract fun getAllSortedByAmountAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash AND name LIKE :filter ORDER BY amountOnList DESC")
    abstract fun getAllSortedByAmountDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Insert
    abstract suspend fun insert(vararg items: ShoppingListItem)

    @Query("INSERT INTO shopping_list_item (name, extraInfo, " +
                                           "color, linkedInventoryItemId) " +
           "SELECT name, extraInfo, color, id FROM inventory_item " +
           "WHERE inventory_item.id IN (:inventoryItemIds) " +
           "AND inventory_item.id NOT IN (SELECT linkedInventoryItemId " +
                                         "FROM shopping_list_item " +
                                         "WHERE NOT inTrash)")
    abstract suspend fun insertFromInventoryItems(vararg inventoryItemIds: Long)

    @Transaction
    open suspend fun autoAddFromInventoryItem(inventoryItemId: Long, minAmount: Int) {
        insertFromInventoryItems(inventoryItemId)
        setMinimumAmountFromLinkedItem(inventoryItemId, minAmount)
    }

    @Query("UPDATE shopping_list_item " +
           "SET name = :name WHERE id = :id")
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
           "SET amountOnList = :amountOnList " +
           "WHERE id = :id")
    abstract suspend fun updateAmountOnList(id: Long, amountOnList: Int)

    @Query("UPDATE shopping_list_item " +
           "SET amountOnList = CASE WHEN amountOnList < :minAmount THEN :minAmount " +
                                                                  "ELSE amountOnList END " +
           "WHERE linkedInventoryItemId = :inventoryItemId")
    abstract suspend fun setMinimumAmountFromLinkedItem(inventoryItemId: Long, minAmount: Int)

    @Query("UPDATE shopping_list_item " +
           "SET amountOnList = :amountOnList " +
           "WHERE linkedInventoryItemId = :inventoryItemId")
    abstract suspend fun updateAmountOnListFromLinkedItem(inventoryItemId: Long, amountOnList: Int)

    @Query("UPDATE shopping_list_item " +
           "SET amountInCart = :amountInCart " +
           "WHERE id = :id")
    abstract suspend fun updateAmountInCart(id: Long, amountInCart: Int)

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

    @Query("UPDATE shopping_list_item " +
            "SET color = :color " +
            "WHERE id = :id")
    abstract suspend fun updateColor(id: Long, color: Int)

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
