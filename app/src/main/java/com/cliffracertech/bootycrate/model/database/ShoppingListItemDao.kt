/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.Dao
import androidx.room.Query
import com.cliffracertech.bootycrate.settings.PrefKeys
import com.cliffracertech.bootycrate.utils.collectAsState
import com.cliffracertech.bootycrate.utils.enumPreferenceFlow
import com.cliffracertech.bootycrate.utils.preferenceFlow
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
 * [ShoppingListItem]. ShoppingListItemDao assumes that only [ItemGroup]s whose
 * [ItemGroup.isSelected] property is true will be shown to the user. Methods that
 * have 'visible' in their name therefore only act on items in selected [ItemGroup]s. */
@Dao abstract class ShoppingListItemDao : ListItemDao() {
    @Query("$selectShoppingListItems ORDER BY colorGroup")
    protected abstract fun getItemsSortedByColor(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY item.name COLLATE NOCASE ASC")
    protected abstract fun getItemsSortedByNameAsc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY item.name COLLATE NOCASE DESC")
    protected abstract fun getItemsSortedByNameDesc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY shoppingListAmount ASC")
    protected abstract fun getItemsSortedByAmountAsc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY shoppingListAmount DESC")
    protected abstract fun getItemsSortedByAmountDesc(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, colorGroup")
    protected abstract fun getItemsSortedByColorAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, item.name COLLATE NOCASE ASC")
    protected abstract fun getItemsSortedByNameAscAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, item.name COLLATE NOCASE DESC")
    protected abstract fun getItemsSortedByNameDescAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, shoppingListAmount ASC")
    protected abstract fun getItemsSortedByAmountAscAndChecked(filter: String): Flow<List<ShoppingListItem>>

    @Query("$selectShoppingListItems ORDER BY isChecked, shoppingListAmount DESC")
    protected abstract fun getItemsSortedByAmountDescAndChecked(filter: String): Flow<List<ShoppingListItem>>

    fun getItems(
        sort: ListItem.Sort,
        searchFilter: String?,
        sortByChecked: Boolean,
    ): Flow<ImmutableList<ShoppingListItem>> {
        val filter = "%${searchFilter ?: ""}%"
        return if (sortByChecked) when (sort) {
                ListItem.Sort.Color -> getItemsSortedByColorAndChecked(filter)
                ListItem.Sort.NameAsc -> getItemsSortedByNameAscAndChecked(filter)
                ListItem.Sort.NameDesc -> getItemsSortedByNameDescAndChecked(filter)
                ListItem.Sort.AmountAsc -> getItemsSortedByAmountAscAndChecked(filter)
                ListItem.Sort.AmountDesc -> getItemsSortedByAmountDescAndChecked(filter)
            }.map(List<ShoppingListItem>::toImmutableList)
        else when (sort) {
                ListItem.Sort.Color -> getItemsSortedByColor(filter)
                ListItem.Sort.NameAsc -> getItemsSortedByNameAsc(filter)
                ListItem.Sort.NameDesc -> getItemsSortedByNameDesc(filter)
                ListItem.Sort.AmountAsc -> getItemsSortedByAmountAsc(filter)
                ListItem.Sort.AmountDesc -> getItemsSortedByAmountDesc(filter)
            }.map(List<ShoppingListItem>::toImmutableList)
    }

    @Query("UPDATE item " +
           "SET shoppingListAmount = 1, inShoppingListTrash = 0 " +
           "WHERE id in (:inventoryItemIds) " +
           "AND shoppingListAmount = -1 AND $inInventory")
    abstract suspend fun addFromInventoryItems(inventoryItemIds: Collection<Long>)

    @Query("UPDATE item SET inShoppingListTrash = 1 " +
           "WHERE id = :id AND $onShoppingList")
    abstract suspend fun delete(id: Long)

    @Query("UPDATE item SET inShoppingListTrash = 1 " +
           "WHERE id in (:itemIds) AND $onShoppingList")
    abstract suspend fun delete(itemIds: Collection<Long>)

    @Query("SELECT EXISTS(SELECT item.id FROM item " +
           "JOIN itemGroup ON item.groupName = itemGroup.name " +
           "WHERE itemGroup.isSelected AND $onShoppingList " +
           "AND item.name = :name AND extraInfo = :extraInfo)")
    abstract suspend fun nameAlreadyUsed(name: String, extraInfo: String): Boolean

    @Query("UPDATE item SET shoppingListAmount = :amount WHERE id = :id")
    abstract suspend fun setAmount(id: Long, amount: Int)

    @Query("$withSelectedGroups " +
           "UPDATE item SET inShoppingListTrash = 0 " +
           "WHERE groupName IN selectedGroups")
    abstract suspend fun undoDelete()

    @Query("UPDATE item " +
           "SET shoppingListAmount = -1," +
               "inShoppingListTrash = 0 " +
           "WHERE inShoppingListTrash")
    abstract suspend fun emptyTrash()

    @Query("SELECT COUNT(item.id) FROM item " +
           "JOIN itemGroup ON item.groupName = itemGroup.name " +
           "WHERE $onShoppingList AND itemGroup.isSelected")
    abstract fun getVisibleItemCount() : Flow<Int>

    @Query("SELECT COUNT(item.id) FROM item " +
           "JOIN itemGroup ON item.groupName = itemGroup.name " +
           "WHERE isChecked AND $onShoppingList AND itemGroup.isSelected")
    abstract fun getVisibleCheckedItemCount(): Flow<Int>

    @Query("UPDATE item SET isChecked = 1 - isChecked WHERE id = :id")
    abstract suspend fun toggleIsChecked(id: Long)

    @Query("$withSelectedGroups " +
           "UPDATE item SET isChecked = 1 " +
           "WHERE $onShoppingList AND groupName IN selectedGroups")
    abstract suspend fun checkAllVisibleItems()

    @Query("$withSelectedGroups " +
           "UPDATE item SET isChecked = 0 " +
           "WHERE $onShoppingList AND groupName IN selectedGroups")
    abstract suspend fun uncheckAllVisibleItems()

    @Query("$withSelectedGroups " +
           "UPDATE item " +
           "SET inventoryAmount = " +
                   "CASE WHEN NOT $inInventory THEN -1 " +
                   "ELSE inventoryAmount + shoppingListAmount END, " +
               "isChecked = 0, " +
               "shoppingListAmount = -1 " +
           "WHERE $onShoppingList AND isChecked " +
           "AND groupName IN selectedGroups")
    abstract suspend fun checkoutVisibleItems()
}

/** A provider of the current [shoppingList]. */
interface ShoppingListProvider {
    val shoppingList: ImmutableList<ShoppingListItem>?
}

/**
 * An implementation of [ShoppingListProvider] that provides the current
 * [shoppingList], given a search query and a sorting method.
 *
 * @param searchQueryState A [StateFlow]`<String?>` that contains the current search query
 * @param dao A [ShoppingListItemDao] instance
 * @param dataStore: A [DataStore]`<Preferences>` that contains the app's preferences
 * @param scope The [CoroutineScope] that the [shoppingList] property will be updated in
 */
class SearchableSortableShoppingListProvider(
    searchQueryState: StateFlow<String?>,
    dao: ShoppingListItemDao,
    dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
): ShoppingListProvider {
    private val sortByCheckedKey = booleanPreferencesKey(PrefKeys.sortByChecked)
    private val sortByChecked = dataStore.preferenceFlow(sortByCheckedKey, false)
    private val sort = dataStore.enumPreferenceFlow(
        intPreferencesKey(PrefKeys.itemSort), ListItem.Sort.Color)

    override val shoppingList by
        combine(sort, searchQueryState, sortByChecked, dao::getItems)
            .transformLatest { emitAll(it) }
            .collectAsState(null, scope)
}