package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
abstract class ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_item WHERE NOT inTrash")
    abstract fun getAll(): LiveData<List<ShoppingListItem>>

    @Insert
    abstract suspend fun insert(vararg items: ShoppingListItem)

    @Query("UPDATE shopping_list_item " +
           "SET name = :name " +
           "WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("UPDATE shopping_list_item " +
            "SET extraInfo = :extraInfo " +
            "WHERE id = :id")
    abstract suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("UPDATE shopping_list_item " +
            "SET amountInCart = :amountInCart " +
            "WHERE id = :id")
    abstract suspend fun updateAmountInCart(id: Long, amountInCart: Int)

    @Query("UPDATE shopping_list_item " +
            "SET amountInCart = amountInCart + :change " +
            "WHERE id = :id")
    abstract suspend fun modifyAmountInCart(id: Long, change: Int)

    @Query("UPDATE shopping_list_item " +
           "SET amount = :amount " +
           "WHERE id = :id")
    abstract suspend fun updateAmount(id: Long, amount: Int)

    @Query("UPDATE shopping_list_item " +
           "SET amount = amount + :change " +
           "WHERE id = :id")
    abstract suspend fun modifyAmount(id: Long, change: Int)

    @Query("DELETE FROM shopping_list_item")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM shopping_list_item " +
           "WHERE inTrash = 1")
    abstract suspend fun emptyTrash()

    @Query("UPDATE shopping_list_item " +
           "SET inTrash = 1 " +
           "WHERE id IN (:ids)")
    abstract suspend fun moveToTrash(vararg ids: Long)

    @Query("UPDATE shopping_list_item " +
           "SET inTrash = 0 " +
           "WHERE inTrash = 1")
    abstract suspend fun undoDelete()

    @Transaction
    open suspend fun delete(vararg ids: Long) {
        emptyTrash()
        moveToTrash(*ids)
    }

    @Transaction
    open suspend fun delete(vararg items: ShoppingListItem) {
        emptyTrash()
        moveToTrash(*(items.map { it.id }).toLongArray())
    }
}
