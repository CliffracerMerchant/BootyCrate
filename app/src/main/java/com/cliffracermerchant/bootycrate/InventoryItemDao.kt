package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
abstract class InventoryItemDao {
    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter")
    abstract fun getAll(filter: String): LiveData<List<InventoryItem>>
    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY name ASC")
    abstract fun getAllSortedByNameAsc(filter: String): LiveData<List<InventoryItem>>
    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY name DESC")
    abstract fun getAllSortedByNameDesc(filter: String): LiveData<List<InventoryItem>>
    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY amount ASC")
    abstract fun getAllSortedByAmountAsc(filter: String): LiveData<List<InventoryItem>>
    @Query("SELECT * FROM inventory_item WHERE NOT inTrash AND name LIKE :filter ORDER BY amount DESC")
    abstract fun getAllSortedByAmountDesc(filter: String): LiveData<List<InventoryItem>>

    @Insert
    abstract suspend fun insert(vararg items: InventoryItem)

    @Query("INSERT INTO inventory_item (name, extraInfo) " +
           "SELECT name, extraInfo " +
           "FROM shopping_list_item " +
           "WHERE shopping_list_item.linkedInventoryItemId IS NULL " +
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
    abstract suspend fun deleteAll()

    @Query("UPDATE shopping_list_item " +
            "SET linkedInventoryItemId = NULL " +
            "WHERE linkedInventoryItemId in " +
            "(SELECT id FROM inventory_item WHERE inTrash = 1)")
    abstract suspend fun clearShoppingListItemLinks()

    @Query("DELETE FROM inventory_item " +
           "WHERE inTrash = 1")
    abstract suspend fun emptyTrashPrivate()

    @Transaction
    open suspend fun emptyTrash() {
        clearShoppingListItemLinks()
        emptyTrashPrivate()
    }

    @Query("UPDATE inventory_item " +
           "SET inTrash = 1 " +
           "WHERE id IN (:ids)")
    abstract suspend fun moveToTrash(vararg ids: Long)

    @Transaction
    open suspend fun delete(vararg ids: Long) {
        clearShoppingListItemLinks()
        emptyTrashPrivate()
        moveToTrash(*ids)
    }

    @Query("UPDATE inventory_item " +
            "SET inTrash = 0 " +
            "WHERE inTrash = 1")
    abstract suspend fun undoDelete()
}
