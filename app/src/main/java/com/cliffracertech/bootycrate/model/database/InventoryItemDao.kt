/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.Dao
import androidx.room.Query
import com.cliffracertech.bootycrate.settings.PrefKeys
import com.cliffracertech.bootycrate.utils.collectAsState
import com.cliffracertech.bootycrate.utils.enumPreferenceFlow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

/** A subclass of [ListItemDao] that adds methods relating to [ListItem]'s subclass
 * [InventoryItem]. InventoryItemDao assumes that only [ItemGroup]s whose
 * [ItemGroup.isSelected] property is true will be shown to the user. Methods that
 * have 'visible' in their name therefore only act on items in selected [ItemGroup]s. */
@Dao abstract class InventoryItemDao: ListItemDao() {
    @Query("$selectInventoryItems ORDER BY colorGroup")
    protected abstract fun getItemsSortedByColor(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY item.name COLLATE NOCASE ASC")
    protected abstract fun getItemsSortedByNameAsc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY item.name COLLATE NOCASE DESC")
    protected abstract fun getItemsSortedByNameDesc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY inventoryAmount ASC")
    protected abstract fun getItemsSortedByAmountAsc(filter: String): Flow<List<InventoryItem>>

    @Query("$selectInventoryItems ORDER BY inventoryAmount DESC")
    protected abstract fun getItemsSortedByAmountDesc(filter: String): Flow<List<InventoryItem>>

    fun getItems(
        sort: ListItem.Sort,
        searchFilter: String? = null
    ): Flow<ImmutableList<InventoryItem>> {
        val filter = "%${searchFilter ?: ""}%"
        return when (sort) {
            ListItem.Sort.Color -> getItemsSortedByColor(filter)
            ListItem.Sort.NameAsc -> getItemsSortedByNameAsc(filter)
            ListItem.Sort.NameDesc -> getItemsSortedByNameDesc(filter)
            ListItem.Sort.AmountAsc -> getItemsSortedByAmountAsc(filter)
            ListItem.Sort.AmountDesc -> getItemsSortedByAmountDesc(filter)
        }.map(List<InventoryItem>::toImmutableList)
    }

    @Query("UPDATE item SET inInventoryTrash = 1 " +
           "WHERE id = :id AND $inInventory")
    abstract suspend fun delete(id: Long)

    @Query("UPDATE item SET inInventoryTrash = 1 " +
           "WHERE id in (:itemIds) AND $inInventory")
    abstract suspend fun delete(itemIds: Collection<Long>)

    @Query("SELECT EXISTS(SELECT item.id FROM item " +
           "JOIN itemGroup ON item.groupName = itemGroup.name " +
           "WHERE itemGroup.isSelected AND $inInventory " +
           "AND item.name = :name AND extraInfo = :extraInfo)")
    abstract suspend fun nameAlreadyUsed(name: String, extraInfo: String): Boolean

    @Query("UPDATE item SET inventoryAmount = :amount WHERE id = :id")
    abstract suspend fun setAmount(id: Long, amount: Int)

    @Query("UPDATE item " +
           "SET inventoryAmount = 0, inInventoryTrash = 0 " +
           "WHERE id in (:shoppingListItemIds) " +
           "AND inventoryAmount = -1 AND $onShoppingList")
    abstract suspend fun addFromShoppingListItems(shoppingListItemIds: Collection<Long>)

    @Query("$withSelectedGroups " +
           "UPDATE item SET inInventoryTrash = 0 " +
           "WHERE groupName IN selectedGroups")
    abstract suspend fun undoDelete()

    @Query("UPDATE item " +
           "SET inventoryAmount = -1, " +
               "inInventoryTrash = 0 " +
           "WHERE inInventoryTrash")
    abstract suspend fun emptyTrash()

    @Query("UPDATE item SET " +
               "autoAddToShoppingList = 1 - autoAddToShoppingList " +
           "WHERE id = :id AND $inInventory")
    abstract suspend fun toggleAutoAddToShoppingList(id: Long)

    @Query("UPDATE item SET " +
               "autoAddToShoppingListAmount = :autoAddToShoppingListAmount " +
           "WHERE id = :id AND $inInventory")
    abstract suspend fun setAutoAddToShoppingListAmount(id: Long, autoAddToShoppingListAmount: Int)
}

/** A provider of the current [inventory]. */
interface InventoryProvider {
    val inventory: ImmutableList<InventoryItem>?
}

/**
 * An implementation of [InventoryProvider] that provides the current [inventory]
 * given a search query and a sorting method.
 *
 * @param searchQueryState A [StateFlow]`<String?>` that contains the current search query
 * @param dao A [ShoppingListItemDao] instance
 * @param dataStore: A [DataStore]`<Preferences>` that contains the app's preferences
 * @param scope The [CoroutineScope] that the [inventory] property will be updated in
 */
class SearchableSortableInventoryProvider(
    searchQueryState: StateFlow<String?>,
    dao: InventoryItemDao,
    dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
): InventoryProvider {
    private val sort = dataStore.enumPreferenceFlow(
        intPreferencesKey(PrefKeys.itemSort), ListItem.Sort.Color)

    override val inventory by
    combine(sort, searchQueryState, dao::getItems)
        .transformLatest { emitAll(it) }
        .collectAsState(null, scope)
}