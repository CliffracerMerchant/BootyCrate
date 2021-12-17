/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** A Room DAO that provides methods to manipulate a database of BootyCrateItems. */
@Dao abstract class BootyCrateItemDao {

    companion object {
        private const val likeSearchFilter = "(bootycrate_item.name LIKE :filter OR extraInfo LIKE :filter)"
        private const val onShoppingList = "shoppingListAmount != -1 AND NOT inShoppingListTrash"
        private const val inInventory = "inventoryAmount != -1 AND NOT inInventoryTrash"

        private const val shoppingListItemFields =
            "bootycrate_item.id, bootycrate_item.name, extraInfo, color, " +
            "shoppingListAmount as amount, expandedInShoppingList as isExpanded, " +
            "selectedInShoppingList as isSelected, $inInventory as isLinked, isChecked"
        private const val inventoryItemFields =
            "bootycrate_item.id, bootycrate_item.name, extraInfo, color, " +
            "inventoryAmount as amount, expandedInInventory as isExpanded, " +
            "selectedInInventory as isSelected, $onShoppingList as isLinked, " +
            "autoAddToShoppingList, autoAddToShoppingListAmount"
        private const val selectShoppingListItems =
            "SELECT $shoppingListItemFields FROM bootycrate_item " +
            "JOIN inventory ON bootycrate_item.inventoryId = inventory.id " +
            "WHERE $likeSearchFilter AND $onShoppingList AND inventory.isSelected"
        private const val selectInventoryItems =
            "SELECT $inventoryItemFields FROM bootycrate_item " +
            "JOIN inventory ON bootycrate_item.inventoryId = inventory.id " +
            "WHERE $likeSearchFilter AND $inInventory AND inventory.isSelected"
    }

    @Insert abstract suspend fun add(item: DatabaseBootyCrateItem): Long
    @Insert abstract suspend fun add(items: List<DatabaseBootyCrateItem>)

    suspend fun addConvertibles(inventoryId: Long, items: List<DatabaseBootyCrateItem.Convertible>) =
        add(items.map { it.toDbBootyCrateItem(inventoryId) })

    @Query("UPDATE bootycrate_item SET name = :name WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("UPDATE bootycrate_item SET extraInfo = :extraInfo WHERE id = :id")
    abstract suspend fun updateExtraInfo(id: Long, extraInfo: String)

    @Query("UPDATE bootycrate_item SET color = :color WHERE id = :id")
    abstract suspend fun updateColor(id: Long, color: Int)

    @Query("SELECT * FROM bootycrate_item")
    abstract fun getAllNow(): List<DatabaseBootyCrateItem>

    @Query("DELETE FROM bootycrate_item")
    abstract fun deleteAll()

    @Query("$selectShoppingListItems ORDER BY color")
    protected abstract fun getShoppingListSortedByColor(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY bootycrate_item.name COLLATE NOCASE ASC")
    protected abstract fun getShoppingListSortedByNameAsc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY bootycrate_item.name COLLATE NOCASE DESC")
    protected abstract fun getShoppingListSortedByNameDesc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY shoppingListAmount ASC")
    protected abstract fun getShoppingListSortedByAmountAsc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY shoppingListAmount DESC")
    protected abstract fun getShoppingListSortedByAmountDesc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, color")
    protected abstract fun getShoppingListSortedByColorAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, bootycrate_item.name COLLATE NOCASE ASC")
    protected abstract fun getShoppingListSortedByNameAscAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, bootycrate_item.name COLLATE NOCASE DESC")
    protected abstract fun getShoppingListSortedByNameDescAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, shoppingListAmount ASC")
    protected abstract fun getShoppingListSortedByAmountAscAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, shoppingListAmount DESC")
    protected abstract fun getShoppingListSortedByAmountDescAndChecked(filter: String): Flow<List<ShoppingListItem>>

    fun getShoppingList(
        sort: BootyCrateItemSort,
        sortByChecked: Boolean,
        searchFilter: String? = null
    ): Flow<List<ShoppingListItem>> {
        val filter = "%${searchFilter ?: ""}%"
        return if (!sortByChecked) when (sort) {
            BootyCrateItemSort.Color -> getShoppingListSortedByColor(filter)
            BootyCrateItemSort.NameAsc -> getShoppingListSortedByNameAsc(filter)
            BootyCrateItemSort.NameDesc -> getShoppingListSortedByNameDesc(filter)
            BootyCrateItemSort.AmountAsc -> getShoppingListSortedByAmountAsc(filter)
            BootyCrateItemSort.AmountDesc -> getShoppingListSortedByAmountDesc(filter)
        } else when (sort) {
            BootyCrateItemSort.Color -> getShoppingListSortedByColorAndChecked(filter)
            BootyCrateItemSort.NameAsc -> getShoppingListSortedByNameAscAndChecked(filter)
            BootyCrateItemSort.NameDesc -> getShoppingListSortedByNameDescAndChecked(filter)
            BootyCrateItemSort.AmountAsc -> getShoppingListSortedByAmountAscAndChecked(filter)
            BootyCrateItemSort.AmountDesc -> getShoppingListSortedByAmountDescAndChecked(filter)
        }
    }

    @Query("SELECT EXISTS(SELECT bootycrate_item.id FROM bootycrate_item " +
                         "JOIN inventory ON bootycrate_item.id = inventory.id " +
                         "WHERE inventory.isSelected AND $onShoppingList " +
                         "AND bootycrate_item.name = :name AND extraInfo = :extraInfo)")
    abstract suspend fun itemWithNameAlreadyExistsInShoppingList(name: String, extraInfo: String): Boolean

    @Query("UPDATE bootycrate_item SET shoppingListAmount = :amount WHERE id = :id")
    abstract suspend fun updateShoppingListAmount(id: Long, amount: Int)

    @Query("UPDATE bootycrate_item SET expandedInShoppingList = 1 WHERE id = :id")
    protected abstract suspend fun expandShoppingListItem(id: Long)

    @Query("UPDATE bootycrate_item SET expandedInShoppingList = 0")
    abstract suspend fun clearExpandedShoppingListItem()

    @Transaction open suspend fun setExpandedShoppingListItem(id: Long?) {
        clearExpandedShoppingListItem()
        if (id != null) expandShoppingListItem(id)
    }

    @Query("SELECT COUNT(bootycrate_item.id) FROM bootycrate_item " +
           "JOIN inventory ON bootycrate_item.id = inventory.id " +
           "WHERE inventory.isSelected AND selectedInShoppingList AND $onShoppingList ")
    abstract fun getSelectedShoppingListItemCount(): Flow<Int>

    @Query("UPDATE bootycrate_item SET selectedInShoppingList = :selected WHERE id = :id")
    abstract suspend fun updateSelectedInShoppingList(id: Long, selected: Boolean)

    @Query("""UPDATE bootycrate_item SET selectedInShoppingList =
                CASE WHEN selectedInShoppingList THEN 0 ELSE 1 END
              WHERE id = :id""")
    abstract suspend fun toggleSelectedInShoppingList(id: Long)

    @Query("UPDATE bootycrate_item set selectedInShoppingList = 1 " +
           "WHERE $onShoppingList AND id in (:ids)")
    abstract suspend fun selectShoppingListItems(ids: List<Long>)

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item set selectedInShoppingList = 1 " +
           "WHERE inventoryId IN selectedInventories AND $onShoppingList")
    abstract suspend fun selectAllShoppingListItems()

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item SET selectedInShoppingList = 0 " +
           "WHERE inventoryId IN selectedInventories")
    abstract suspend fun clearShoppingListSelection()

    @Query("""WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected)
              UPDATE bootycrate_item 
              SET shoppingListAmount = 1,
                  inShoppingListTrash = 0
              WHERE selectedInInventory 
              AND shoppingListAmount = -1
              AND $inInventory AND inventoryId IN selectedInventories""")
    abstract suspend fun addToShoppingListFromSelectedInventoryItems()

    @Query("""WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected)
              UPDATE bootycrate_item
              SET inShoppingListTrash = 1,
                  expandedInShoppingList = 0,
                  selectedInShoppingList = 0
              WHERE selectedInShoppingList AND $onShoppingList
              AND inventoryId IN selectedInventories""")
    abstract suspend fun deleteSelectedShoppingListItems()

    @Query("""UPDATE bootycrate_item SET inShoppingListTrash = 1,
                                         expandedInShoppingList = 0,
                                         selectedInShoppingList = 0
              WHERE id IN (:ids) AND $onShoppingList""")
    abstract suspend fun deleteShoppingListItems(ids: LongArray)

    @Query("""WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected)
              UPDATE bootycrate_item SET shoppingListAmount = -1,
                                         isChecked = 0,
                                         expandedInShoppingList = 0,
                                         selectedInShoppingList = 0
              WHERE shoppingListAmount != -1 AND inventoryId IN selectedInventories""")
    abstract suspend fun deleteAllShoppingListItems()

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item SET inShoppingListTrash = 0 " +
           "WHERE inventoryId IN selectedInventories")
    abstract suspend fun undoDeleteShoppingListItems()

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item " +
           "SET shoppingListAmount = -1, inShoppingListTrash = 0 " +
           "WHERE inShoppingListTrash AND inventoryId IN selectedInventories")
    abstract suspend fun emptyShoppingListTrash()

    @Query("UPDATE bootycrate_item SET isChecked = :isChecked WHERE id = :id")
    abstract suspend fun updateIsChecked(id: Long, isChecked: Boolean)

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item SET isChecked = 1 " +
           "WHERE $onShoppingList AND inventoryId IN selectedInventories")
    abstract suspend fun checkAllShoppingListItems()

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item SET isChecked = 0 " +
           "WHERE $onShoppingList AND inventoryId IN selectedInventories")
    abstract suspend fun uncheckAllShoppingListItems()

    @Query("SELECT COUNT(bootycrate_item.id) FROM bootycrate_item " +
           "JOIN inventory ON bootycrate_item.inventoryId = inventory.id " +
           "WHERE isChecked AND $onShoppingList AND inventory.isSelected")
    abstract fun getCheckedShoppingListItemsSize() : Flow<Int>

    @Query("""WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected)
              UPDATE bootycrate_item
              SET inventoryAmount = CASE WHEN $inInventory
                                    THEN inventoryAmount + shoppingListAmount
                                    ELSE -1 END,
                  isChecked = 0,
                  shoppingListAmount = -1,
                  expandedInShoppingList = 0,
                  selectedInShoppingList = 0
              WHERE $onShoppingList AND isChecked AND inventoryId IN selectedInventories""")
    abstract suspend fun checkout()

    @Query("$selectInventoryItems ORDER BY color")
    protected abstract fun getInventorySortedByColor(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY bootycrate_item.name COLLATE NOCASE ASC")
    protected abstract fun getInventorySortedByNameAsc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY bootycrate_item.name COLLATE NOCASE DESC")
    protected abstract fun getInventorySortedByNameDesc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY inventoryAmount ASC")
    protected abstract fun getInventorySortedByAmountAsc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY inventoryAmount DESC")
    protected abstract fun getInventorySortedByAmountDesc(filter: String): Flow<List<InventoryItem>>

    fun getInventoryContents(
        sort: BootyCrateItemSort,
        searchFilter: String? = null
    ): Flow<List<InventoryItem>> {
        val filter = "%${searchFilter ?: ""}%"
        return when (sort) {
            BootyCrateItemSort.Color -> getInventorySortedByColor(filter)
            BootyCrateItemSort.NameAsc -> getInventorySortedByNameAsc(filter)
            BootyCrateItemSort.NameDesc -> getInventorySortedByNameDesc(filter)
            BootyCrateItemSort.AmountAsc -> getInventorySortedByAmountAsc(filter)
            BootyCrateItemSort.AmountDesc -> getInventorySortedByAmountDesc(filter)
        }
    }

    @Query("SELECT EXISTS(SELECT bootycrate_item.id FROM bootycrate_item " +
                         "JOIN inventory ON bootycrate_item.inventoryId = inventory.id " +
                         "WHERE $inInventory AND bootycrate_item.name = :name " +
                         "AND extraInfo = :extraInfo AND inventory.isSelected)")
    abstract suspend fun itemWithNameAlreadyExistsInInventory(name: String, extraInfo: String): Boolean

    @Query("UPDATE bootycrate_item SET inventoryAmount = :amount WHERE id = :id")
    abstract suspend fun updateInventoryAmount(id: Long, amount: Int)

    @Query("UPDATE bootycrate_item SET expandedInInventory = 1 WHERE id = :id")
    protected abstract suspend fun expandInventoryItem(id: Long)

    @Query("UPDATE bootycrate_item SET expandedInInventory = 0")
    abstract suspend fun clearExpandedInventoryItem()

    @Transaction open suspend fun setExpandedInventoryItem(id: Long?) {
        clearExpandedInventoryItem()
        if (id != null) expandInventoryItem(id)
    }

    @Query("SELECT COUNT(bootycrate_item.id) FROM bootycrate_item " +
           "JOIN inventory ON bootycrate_item.inventoryId = inventory.id " +
           "WHERE selectedInInventory AND $inInventory " +
           "AND inventory.isSelected")
    abstract fun getSelectedInventoryItemCount(): Flow<Int>

    @Query("UPDATE bootycrate_item SET selectedInInventory = :selected WHERE id = :id")
    abstract suspend fun updateSelectedInInventory(id: Long, selected: Boolean)

    @Query("""UPDATE bootycrate_item SET selectedInInventory =
                CASE WHEN selectedInInventory THEN 0 ELSE 1 END
              WHERE id = :id""")
    abstract suspend fun toggleSelectedInInventory(id: Long)

    @Query("UPDATE bootycrate_item set selectedInInventory = 1 " +
           "WHERE $inInventory AND id in (:ids)")
    abstract suspend fun selectInventoryItems(ids: List<Long>)

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item set selectedInInventory = 1 " +
           "WHERE $inInventory AND inventoryId IN selectedInventories")
    abstract suspend fun selectAllInventoryItems()

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item SET selectedInInventory = 0 " +
           "WHERE inventoryId IN selectedInventories")
    abstract suspend fun clearInventorySelection()

    @Query("""WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected)
              UPDATE bootycrate_item 
              SET inventoryAmount = 0,
                  inInventoryTrash = 0
              WHERE selectedInShoppingList AND inventoryAmount = -1
              AND $onShoppingList AND inventoryId IN selectedInventories""")
    abstract suspend fun addToInventoryFromSelectedShoppingListItems()

    @Query("""WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected)
              UPDATE bootycrate_item
              SET inInventoryTrash = 1,
                  expandedInInventory = 0,
                  selectedInInventory = 0
              WHERE selectedInInventory AND $inInventory
              AND inventoryId IN selectedInventories""")
    abstract suspend fun deleteSelectedInventoryItems()

    @Query("""UPDATE bootycrate_item SET inInventoryTrash = 1,
                                         expandedInInventory = 0,
                                         selectedInInventory = 0,
                                         autoAddToShoppingList = 0,
                                         autoAddToShoppingListAmount = 0
              WHERE id IN (:ids) AND $inInventory""")
    abstract suspend fun deleteInventoryItems(ids: LongArray)

    @Query("""WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected)
              UPDATE bootycrate_item SET inventoryAmount = -1,
                                         expandedInInventory = 0,
                                         selectedInInventory = 0
              WHERE inventoryAmount != -1 AND inventoryId IN selectedInventories""")
    abstract suspend fun deleteAllInventoryItems()

    @Query("WITH selectedInventories AS (SELECT id FROM inventory WHERE isSelected) " +
           "UPDATE bootycrate_item SET inInventoryTrash = 0 " +
           "WHERE inventoryId IN selectedInventories")
    abstract suspend fun undoDeleteInventoryItems()

    @Query("UPDATE bootycrate_item " +
           "SET inventoryAmount = -1, inInventoryTrash = 0 " +
           "WHERE inInventoryTrash")
    abstract suspend fun emptyInventoryTrash()

    @Query("""UPDATE bootycrate_item SET autoAddToShoppingList = :autoAddToShoppingList
              WHERE id = :id AND $inInventory""")
    abstract suspend fun updateAutoAddToShoppingList(id: Long, autoAddToShoppingList: Boolean)

    @Query("""UPDATE bootycrate_item SET autoAddToShoppingListAmount = :autoAddToShoppingListAmount
              WHERE id = :id AND $inInventory""")
    abstract suspend fun updateAutoAddToShoppingListAmount(id: Long, autoAddToShoppingListAmount: Int)
}