/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** A Room DAO for BootyCrateDatabase's shopping_list_item table. */
@Dao abstract class ShoppingListItemDao : ExpandableSelectableItemDao<ShoppingListItem>() {

    @Query("SELECT * FROM shopping_list_item")
    abstract override fun getAllNow(): List<ShoppingListItem>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter)
              ORDER BY color""")
    abstract override fun getAllSortedByColor(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter)
              ORDER BY name COLLATE NOCASE ASC""")
    abstract override fun getAllSortedByNameAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter) 
              ORDER BY name COLLATE NOCASE DESC""")
    abstract override fun getAllSortedByNameDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter) 
              ORDER BY amount ASC""")
    abstract override fun getAllSortedByAmountAsc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter) 
              ORDER BY amount DESC""")
    abstract override fun getAllSortedByAmountDesc(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter)
              ORDER BY isChecked, color""")
    abstract fun getAllSortedByColorAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter)
              ORDER BY isChecked, name COLLATE NOCASE ASC""")
    abstract fun getAllSortedByNameAscAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter) 
              ORDER BY isChecked, name COLLATE NOCASE DESC""")
    abstract fun getAllSortedByNameDescAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter) 
              ORDER BY isChecked, amount ASC""")
    abstract fun getAllSortedByAmountAscAndChecked(filter: String): LiveData<List<ShoppingListItem>>

    @Query("""SELECT * FROM shopping_list_item WHERE NOT inTrash
              AND (name LIKE :filter OR extraInfo LIKE :filter) 
              ORDER BY isChecked, amount DESC""")
    abstract fun getAllSortedByAmountDescAndChecked(filter: String): LiveData<List<ShoppingListItem>>
    
    @Query("""SELECT EXISTS(SELECT * FROM shopping_list_item
                            WHERE (name = :name AND extraInfo = :extraInfo)
                            AND NOT inTrash)""")
    abstract override fun itemWithNameAlreadyExists(name: String, extraInfo: String): LiveData<Boolean>

    @Query("SELECT * FROM shopping_list_item WHERE isSelected AND NOT inTrash")
    abstract override fun getSelectedItems(): LiveData<List<ShoppingListItem>>

    @Query("""INSERT INTO shopping_list_item (name, extraInfo, color, linkedItemId)
              SELECT name, extraInfo, color, id
              FROM inventory_item
              WHERE isSelected
              AND inTrash = 0
              AND linkedItemId IS NULL""")
    protected abstract suspend fun _addFromSelectedInventoryItems()

    @Query("UPDATE inventory_item SET isSelected = 0")
    protected abstract suspend fun clearInventorySelection()

    @Transaction open suspend fun addFromSelectedInventoryItems() {
        _addFromSelectedInventoryItems()
        clearInventorySelection()
    }

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
              SET isExpanded = :isExpanded
              WHERE id = :id""")
    abstract override suspend fun updateIsExpanded(id: Long, isExpanded: Boolean)

    @Query("UPDATE shopping_list_item SET isExpanded = 0")
    abstract override suspend fun clearExpandedItem()

    @Query("""UPDATE shopping_list_item
              SET isSelected = :isSelected
              WHERE id = :id""")
    abstract override suspend fun updateIsSelected(id: Long, isSelected: Boolean)

    @Query("""UPDATE shopping_list_item
              SET isSelected = CASE WHEN isSelected THEN 0
                                    ELSE 1 END
              WHERE id = :id""")
    abstract override suspend fun toggleIsSelected(id: Long)

    @Query("""UPDATE shopping_list_item
              SET inTrash = 1,
                  isExpanded = 0,
                  isSelected = 0
              WHERE isSelected""")
    abstract override suspend fun deleteSelected()

    @Query("UPDATE shopping_list_item set isSelected = 1")
    abstract override suspend fun selectAll()

    @Query("UPDATE shopping_list_item SET isSelected = 0")
    abstract override suspend fun clearSelection()

    @Query("""UPDATE shopping_list_item
              SET isChecked = :isChecked
              WHERE id = :id""")
    abstract suspend fun updateIsChecked(id: Long, isChecked: Boolean)

    @Query("UPDATE shopping_list_item SET isChecked = 1")
    abstract suspend fun checkAll()

    @Query("UPDATE shopping_list_item SET isChecked = 0")
    abstract suspend fun uncheckAll()

    @Query("SELECT COUNT(*) FROM shopping_list_item WHERE isChecked AND NOT inTrash")
    abstract fun getCheckedItemsSize() : LiveData<Int>

    @Query("DELETE FROM shopping_list_item")
    abstract override suspend fun deleteAll()

    @Query("""UPDATE shopping_list_item
              SET inTrash = 1,
                  isExpanded = 0,
                  isSelected = 0
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
     * list items at once and modify the amounts of linked inventory items
     * appropriately. For non-linked shopping list items, which are simply
     * removed from the list, the checkout function acts no differently than
     * deleting them. */
    @Transaction open suspend fun checkout() {
        updateInventoryItemAmountsFromShoppingList()
        deleteCheckedItems()
    }
}