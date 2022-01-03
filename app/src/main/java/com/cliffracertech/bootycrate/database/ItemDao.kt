/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** A Room DAO that provides methods to manipulate a database of ListItems. */
@Dao abstract class ItemDao {

    companion object {
        private const val likeSearchFilter = "(item.name LIKE :filter OR extraInfo LIKE :filter)"
        private const val onShoppingList = "shoppingListAmount != -1 AND NOT inShoppingListTrash"
        private const val inInventory = "inventoryAmount != -1 AND NOT inInventoryTrash"

        private const val shoppingListItemFields =
            "item.id, item.name, extraInfo, color, " +
            "shoppingListAmount as amount, " +
            "expandedInShoppingList as isExpanded, " +
            "selectedInShoppingList as isSelected, " +
            "$inInventory as isLinked, isChecked"

        private const val inventoryItemFields =
            "item.id, item.name, extraInfo, color, " +
            "inventoryAmount as amount, " +
            "expandedInInventory as isExpanded, " +
            "selectedInInventory as isSelected, " +
            "$onShoppingList as isLinked, " +
            "autoAddToShoppingList, autoAddToShoppingListAmount"

        private const val selectShoppingListItems =
            "SELECT $shoppingListItemFields FROM item " +
            "JOIN itemGroup ON item.groupId = itemGroup.id " +
            "WHERE $likeSearchFilter AND $onShoppingList AND itemGroup.isSelected"

        private const val selectInventoryItems =
            "SELECT $inventoryItemFields FROM item " +
            "JOIN itemGroup ON item.groupId = itemGroup.id " +
            "WHERE $likeSearchFilter AND $inInventory AND itemGroup.isSelected"
    }

    @Insert abstract suspend fun add(item: DatabaseListItem): Long
    @Insert abstract suspend fun add(items: List<DatabaseListItem>)

    suspend fun addConvertibles(itemGroupId: Long, items: List<DatabaseListItem.Convertible>) =
        add(items.map { it.toDbListItem(itemGroupId) })

    @Query("UPDATE item SET name = :name WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("UPDATE item SET extraInfo = :extraInfo WHERE id = :id")
    abstract suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("UPDATE item SET color = :color WHERE id = :id")
    abstract suspend fun updateColor(id: Long, color: Int)

    @Query("SELECT * FROM item")
    abstract fun getAllNow(): List<DatabaseListItem>

    @Query("DELETE FROM item")
    abstract fun deleteAll()

    @Query("$selectShoppingListItems ORDER BY color")
    protected abstract fun getShoppingListSortedByColor(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY item.name COLLATE NOCASE ASC")
    protected abstract fun getShoppingListSortedByNameAsc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY item.name COLLATE NOCASE DESC")
    protected abstract fun getShoppingListSortedByNameDesc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY shoppingListAmount ASC")
    protected abstract fun getShoppingListSortedByAmountAsc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY shoppingListAmount DESC")
    protected abstract fun getShoppingListSortedByAmountDesc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, color")
    protected abstract fun getShoppingListSortedByColorAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, item.name COLLATE NOCASE ASC")
    protected abstract fun getShoppingListSortedByNameAscAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, item.name COLLATE NOCASE DESC")
    protected abstract fun getShoppingListSortedByNameDescAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, shoppingListAmount ASC")
    protected abstract fun getShoppingListSortedByAmountAscAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, shoppingListAmount DESC")
    protected abstract fun getShoppingListSortedByAmountDescAndChecked(filter: String): Flow<List<ShoppingListItem>>

    fun getShoppingList(
        sort: ListItem.Sort,
        searchFilter: String?,
        sortByChecked: Boolean,
    ): Flow<List<ShoppingListItem>> {
        val filter = "%${searchFilter ?: ""}%"
        return if (!sortByChecked) when (sort) {
            ListItem.Sort.Color -> getShoppingListSortedByColor(filter)
            ListItem.Sort.NameAsc -> getShoppingListSortedByNameAsc(filter)
            ListItem.Sort.NameDesc -> getShoppingListSortedByNameDesc(filter)
            ListItem.Sort.AmountAsc -> getShoppingListSortedByAmountAsc(filter)
            ListItem.Sort.AmountDesc -> getShoppingListSortedByAmountDesc(filter)
        } else when (sort) {
            ListItem.Sort.Color -> getShoppingListSortedByColorAndChecked(filter)
            ListItem.Sort.NameAsc -> getShoppingListSortedByNameAscAndChecked(filter)
            ListItem.Sort.NameDesc -> getShoppingListSortedByNameDescAndChecked(filter)
            ListItem.Sort.AmountAsc -> getShoppingListSortedByAmountAscAndChecked(filter)
            ListItem.Sort.AmountDesc -> getShoppingListSortedByAmountDescAndChecked(filter)
        }
    }

    @Query("SELECT EXISTS(SELECT item.id FROM item " +
                         "JOIN itemGroup ON item.groupId = itemGroup.id " +
                         "WHERE itemGroup.isSelected AND $onShoppingList " +
                         "AND item.name = :name AND extraInfo = :extraInfo)")
    abstract suspend fun nameAlreadyUsedInShoppingList(name: String, extraInfo: String): Boolean

    @Query("UPDATE item SET shoppingListAmount = :amount WHERE id = :id")
    abstract suspend fun updateShoppingListAmount(id: Long, amount: Int)

    @Query("SELECT id FROM item WHERE expandedInShoppingList LIMIT 1")
    protected abstract suspend fun getExpandedShoppingListItemId(): Long?

    @Query("UPDATE item SET expandedInShoppingList = 0")
    abstract suspend fun clearExpandedShoppingListItem()

    @Query("UPDATE item SET expandedInShoppingList = 1 WHERE id = :id")
    protected abstract suspend fun expandShoppingListItem(id: Long)

    @Transaction open suspend fun toggleExpandedInShoppingList(id: Long?) {
        val expandedId = getExpandedShoppingListItemId()
        clearExpandedShoppingListItem()
        if (expandedId != id && id != null)
            expandShoppingListItem(id)
    }

    @Query("SELECT COUNT(item.id) FROM item " +
           "JOIN itemGroup ON item.groupId = itemGroup.id " +
           "WHERE itemGroup.isSelected AND selectedInShoppingList AND $onShoppingList")
    abstract fun getSelectedShoppingListItemCount(): Flow<Int>

    @Query("UPDATE item SET selectedInShoppingList = :selected WHERE id = :id")
    abstract suspend fun updateSelectedInShoppingList(id: Long, selected: Boolean)

    @Query("UPDATE item SET selectedInShoppingList = 1 - selectedInShoppingList WHERE id = :id")
    abstract suspend fun toggleSelectedInShoppingList(id: Long)

    @Query("UPDATE item set selectedInShoppingList = 1 " +
           "WHERE $onShoppingList AND id IN (:ids)")
    abstract suspend fun selectShoppingListItems(ids: List<Long>)

    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item set selectedInShoppingList = 1 " +
           "WHERE groupId IN selectedGroups AND $onShoppingList")
    abstract suspend fun selectAllShoppingListItems()

    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item SET selectedInShoppingList = 0 " +
           "WHERE groupId IN selectedGroups")
    abstract suspend fun clearShoppingListSelection()

    @Query("""WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected)
              UPDATE item 
              SET shoppingListAmount = 1,
                  inShoppingListTrash = 0
              WHERE selectedInInventory 
              AND shoppingListAmount = -1
              AND $inInventory
              AND groupId IN selectedGroups""")
    abstract suspend fun addToShoppingListFromSelectedInventoryItems()

    @Query("""WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected)
              UPDATE item
              SET inShoppingListTrash = 1,
                  expandedInShoppingList = 0,
                  selectedInShoppingList = 0
              WHERE selectedInShoppingList
              AND $onShoppingList
              AND groupId IN selectedGroups""")
    abstract suspend fun deleteSelectedShoppingListItems()

    @Query("""UPDATE item SET inShoppingListTrash = 1,
                              expandedInShoppingList = 0,
                              selectedInShoppingList = 0
              WHERE id IN (:ids) AND $onShoppingList""")
    abstract suspend fun deleteShoppingListItems(ids: Array<Long>)

    @Query("""WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected)
              UPDATE item SET shoppingListAmount = -1,
                              isChecked = 0,
                              expandedInShoppingList = 0,
                              selectedInShoppingList = 0
              WHERE shoppingListAmount != -1 AND groupId IN selectedGroups""")
    abstract suspend fun deleteAllShoppingListItems()

    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item SET inShoppingListTrash = 0 " +
           "WHERE groupId IN selectedGroups")
    abstract suspend fun undoDeleteShoppingListItems()

    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item " +
           "SET shoppingListAmount = -1, inShoppingListTrash = 0 " +
           "WHERE inShoppingListTrash AND groupId IN selectedGroups")
    abstract suspend fun emptyShoppingListTrash()

    @Query("UPDATE item SET isChecked = 1 - isChecked WHERE id = :id")
    abstract suspend fun toggleIsChecked(id: Long)


    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item SET isChecked = 1 " +
           "WHERE $onShoppingList AND groupId IN selectedGroups")
    abstract suspend fun checkAllShoppingListItems()

    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item SET isChecked = 0 " +
           "WHERE $onShoppingList AND groupId IN selectedGroups")
    abstract suspend fun uncheckAllShoppingListItems()

    @Query("SELECT COUNT(item.id) FROM item " +
           "JOIN itemGroup ON item.groupId = itemGroup.id " +
           "WHERE isChecked AND $onShoppingList AND itemGroup.isSelected")
    abstract fun getCheckedShoppingListItemsSize() : Flow<Int>

    @Query("""WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected)
              UPDATE item
              SET inventoryAmount = CASE WHEN $inInventory
                                    THEN inventoryAmount + shoppingListAmount
                                    ELSE -1 END,
                  isChecked = 0,
                  shoppingListAmount = -1,
                  expandedInShoppingList = 0,
                  selectedInShoppingList = 0
              WHERE $onShoppingList AND isChecked AND groupId IN selectedGroups""")
    abstract suspend fun checkout()

    @Query("$selectInventoryItems ORDER BY color")
    protected abstract fun getInventorySortedByColor(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY item.name COLLATE NOCASE ASC")
    protected abstract fun getInventorySortedByNameAsc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY item.name COLLATE NOCASE DESC")
    protected abstract fun getInventorySortedByNameDesc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY inventoryAmount ASC")
    protected abstract fun getInventorySortedByAmountAsc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY inventoryAmount DESC")
    protected abstract fun getInventorySortedByAmountDesc(filter: String): Flow<List<InventoryItem>>

    fun getInventoryContents(
        sort: ListItem.Sort,
        searchFilter: String? = null
    ): Flow<List<InventoryItem>> {
        val filter = "%${searchFilter ?: ""}%"
        return when (sort) {
            ListItem.Sort.Color -> getInventorySortedByColor(filter)
            ListItem.Sort.NameAsc -> getInventorySortedByNameAsc(filter)
            ListItem.Sort.NameDesc -> getInventorySortedByNameDesc(filter)
            ListItem.Sort.AmountAsc -> getInventorySortedByAmountAsc(filter)
            ListItem.Sort.AmountDesc -> getInventorySortedByAmountDesc(filter)
        }
    }

    @Query("SELECT EXISTS(SELECT item.id FROM item " +
                         "JOIN itemGroup ON item.groupId = itemGroup.id " +
                         "WHERE $inInventory AND item.name = :name " +
                         "AND extraInfo = :extraInfo AND itemGroup.isSelected)")
    abstract suspend fun nameAlreadyUsedInInventory(name: String, extraInfo: String): Boolean

    @Query("UPDATE item SET inventoryAmount = :amount WHERE id = :id")
    abstract suspend fun updateInventoryAmount(id: Long, amount: Int)

    @Query("SELECT id FROM item WHERE expandedInInventory LIMIT 1")
    protected abstract suspend fun getExpandedInventoryItemId(): Long?

    @Query("UPDATE item SET expandedInInventory = 0")
    abstract suspend fun clearExpandedInventoryItem()

    @Query("UPDATE item SET expandedInInventory = 1 WHERE id = :id")
    protected abstract suspend fun expandInventoryItem(id: Long)

    @Transaction open suspend fun toggleExpandedInInventory(id: Long?) {
        val expandedId = getExpandedInventoryItemId()
        clearExpandedInventoryItem()
        if (expandedId != id && id != null)
            expandInventoryItem(id)
    }

    @Query("SELECT COUNT(item.id) FROM item " +
           "JOIN itemGroup ON item.groupId = itemGroup.id " +
           "WHERE selectedInInventory AND $inInventory " +
           "AND itemGroup.isSelected")
    abstract fun getSelectedInventoryItemCount(): Flow<Int>

    @Query("UPDATE item SET selectedInInventory = :selected WHERE id = :id")
    abstract suspend fun updateSelectedInInventory(id: Long, selected: Boolean)

    @Query("""UPDATE item SET selectedInInventory =
                CASE WHEN selectedInInventory THEN 0 ELSE 1 END
              WHERE id = :id""")
    abstract suspend fun toggleSelectedInInventory(id: Long)

    @Query("UPDATE item set selectedInInventory = 1 " +
           "WHERE $inInventory AND id IN (:ids)")
    abstract suspend fun selectInventoryItems(ids: List<Long>)

    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item set selectedInInventory = 1 " +
           "WHERE $inInventory AND groupId IN selectedGroups")
    abstract suspend fun selectAllInventoryItems()

    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item SET selectedInInventory = 0 " +
           "WHERE groupId IN selectedGroups")
    abstract suspend fun clearInventorySelection()

    @Query("""WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected)
              UPDATE item 
              SET inventoryAmount = 0,
                  inInventoryTrash = 0
              WHERE selectedInShoppingList AND inventoryAmount = -1
              AND $onShoppingList AND groupId IN selectedGroups""")
    abstract suspend fun addToInventoryFromSelectedShoppingListItems()

    @Query("""WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected)
              UPDATE item
              SET inInventoryTrash = 1,
                  expandedInInventory = 0,
                  selectedInInventory = 0
              WHERE selectedInInventory AND $inInventory
              AND groupId IN selectedGroups""")
    abstract suspend fun deleteSelectedInventoryItems()

    @Query("""UPDATE item SET inInventoryTrash = 1,
                              expandedInInventory = 0,
                              selectedInInventory = 0,
                              autoAddToShoppingList = 0,
                              autoAddToShoppingListAmount = 0
              WHERE id IN (:ids) AND $inInventory""")
    abstract suspend fun deleteInventoryItems(ids: Array<Long>)

    @Query("""WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected)
              UPDATE item SET inventoryAmount = -1,
                              expandedInInventory = 0,
                              selectedInInventory = 0
              WHERE inventoryAmount != -1 AND groupId IN selectedGroups""")
    abstract suspend fun deleteAllInventoryItems()

    @Query("WITH selectedGroups AS (SELECT id FROM itemGroup WHERE isSelected) " +
           "UPDATE item SET inInventoryTrash = 0 " +
           "WHERE groupId IN selectedGroups")
    abstract suspend fun undoDeleteInventoryItems()

    @Query("UPDATE item " +
           "SET inventoryAmount = -1, inInventoryTrash = 0 " +
           "WHERE inInventoryTrash")
    abstract suspend fun emptyInventoryTrash()

    @Query("""UPDATE item SET autoAddToShoppingList = 1 - autoAddToShoppingList
              WHERE id = :id AND $inInventory""")
    abstract suspend fun toggleAutoAddToShoppingList(id: Long)

    @Query("""UPDATE item SET autoAddToShoppingListAmount = :autoAddToShoppingListAmount
              WHERE id = :id AND $inInventory""")
    abstract suspend fun updateAutoAddToShoppingListAmount(id: Long, autoAddToShoppingListAmount: Int)
}