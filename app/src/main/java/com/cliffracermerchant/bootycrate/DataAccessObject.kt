/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

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
    @Insert abstract suspend fun insert(item: Entity): Long
    @Insert abstract suspend fun insert(items: Array<Entity>)
    abstract suspend fun deleteAll()
    abstract suspend fun emptyTrash()
    abstract suspend fun delete(ids: LongArray)
    abstract suspend fun undoDelete()
}