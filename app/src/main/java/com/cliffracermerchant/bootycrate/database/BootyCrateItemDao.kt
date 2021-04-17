/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Transaction
import com.cliffracermerchant.bootycrate.BootyCrateItem
import com.cliffracermerchant.bootycrate.ExpandableSelectableItem

/** A pure abstract class that declares a generic data access object interface for BootyCrateItems. */
@Dao abstract class BootyCrateItemDao<Entity: BootyCrateItem> {
    abstract fun getAllNow(): List<Entity>
    abstract fun getAllSortedByColor(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByNameAsc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByNameDesc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByAmountAsc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByAmountDesc(filter: String): LiveData<List<Entity>>
    abstract fun itemWithNameAlreadyExists(name: String, extraInfo: String): LiveData<Boolean>
    @Insert abstract suspend fun add(item: Entity): Long
    @Insert abstract suspend fun add(items: List<Entity>)
    abstract suspend fun deleteAll()
    abstract suspend fun emptyTrash()
    abstract suspend fun delete(ids: LongArray)
    abstract suspend fun undoDelete()
    abstract suspend fun updateName(id: Long, name: String)
    abstract suspend fun updateExtraInfo(id: Long, extraInfo: String)
    abstract suspend fun updateColor(id: Long, color: Int)
    abstract suspend fun updateAmount(id: Long, amount: Int)
}

/** An abstract DAO subclass of BootyCrateItemDao<Entity> that extends the interface with
 *  operations related to the expanding/collapsing of items and the selection of items. */
@Dao abstract class ExpandableSelectableItemDao<Entity: ExpandableSelectableItem> : BootyCrateItemDao<Entity>() {
    protected abstract suspend fun updateIsExpanded(id: Long, isExpanded: Boolean)
    @Transaction open suspend fun setExpandedItem(id: Long?) {
        clearExpandedItem()
        if (id != null) updateIsExpanded(id, true)
    }
    abstract fun getSelectedItems(): LiveData<List<Entity>>
    abstract suspend fun updateIsSelected(id: Long, isSelected: Boolean)
    abstract suspend fun toggleIsSelected(id: Long)
    abstract suspend fun deleteSelected()

    abstract suspend fun selectAll()
    abstract suspend fun clearExpandedItem()
    abstract suspend fun clearSelection()
    @Transaction open suspend fun resetExpandedItemAndSelection() {
        clearExpandedItem()
        clearSelection()
    }
}