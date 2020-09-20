/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert

/** A pure abstract class that declares a generic data access object interface. */
@Dao abstract class DataAccessObject<Entity: ViewModelItem>() {
    abstract fun getAllSortedByColor(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByNameAsc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByNameDesc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByAmountAsc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByAmountDesc(filter: String): LiveData<List<Entity>>
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
    abstract fun getAllNow(): List<Entity>
}