package com.cliffracermerchant.stuffcrate

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
abstract class InventoryItemDao {
    @Query("SELECT * FROM inventory_item WHERE NOT inTrash")
    abstract fun getAll(): LiveData<List<InventoryItem>>

    @Insert
    abstract suspend fun insert(vararg items: InventoryItem)

    @Query("UPDATE inventory_item " +
           "SET name = :name " +
           "WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("UPDATE inventory_item " +
           "SET amount = :amount " +
           "WHERE id = :id")
    abstract suspend fun updateAmount(id: Long, amount: Int)

    @Query("UPDATE inventory_item " +
           "SET amount = amount + :change " +
           "WHERE id = :id")
    abstract suspend fun modifyAmount(id: Long, change: Int)

    @Query("UPDATE Inventory_item " +
           "SET autoAddToShoppingList = :autoAddToShoppingList " +
           "WHERE id = :id")
    abstract suspend fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean)

    @Query("UPDATE inventory_item " +
            "SET autoAddToShoppingListTrigger = :autoAddToShoppingListTrigger " +
            "WHERE id = :id")
    abstract suspend fun updateAutoAddToShoppingListTrigger(id: Long, autoAddToShoppingListTrigger: Int)

    @Query("UPDATE inventory_item " +
            "SET autoAddToShoppingListTrigger = autoAddToShoppingListTrigger + :change " +
            "WHERE id = :id")
    abstract suspend fun modifyAutoAddToShoppingListTrigger(id: Long, change: Int)

    @Query("DELETE FROM inventory_item")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM inventory_item " +
           "WHERE inTrash = 1")
    abstract suspend fun emptyTrash()

    @Query("UPDATE inventory_item " +
           "SET inTrash = 1 " +
           "WHERE id IN (:ids)")
    abstract suspend fun moveToTrash(vararg ids: Long)

    @Query("UPDATE inventory_item " +
           "SET inTrash = 0 " +
           "WHERE inTrash = 1")
    abstract suspend fun undoDelete()

    @Transaction
    open suspend fun delete(vararg ids: Long) {
        emptyTrash()
        moveToTrash(*ids)
    }

    @Transaction
    open suspend fun delete(vararg items: InventoryItem) {
        emptyTrash()
        moveToTrash(*(items.map { it.id }).toLongArray())
    }
}
