/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** A pure abstract class that declares a generic data access object interface. */
abstract class DataAccessObject<Entity: BootyCrateItem>() {
    abstract fun getAllSortedByColor(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByNameAsc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByNameDesc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByAmountAsc(filter: String): LiveData<List<Entity>>
    abstract fun getAllSortedByAmountDesc(filter: String): LiveData<List<Entity>>
    abstract suspend fun insert(item: Entity): Long
    abstract suspend fun insert(vararg items: Entity)
    abstract suspend fun deleteAll()
    abstract suspend fun emptyTrash()
    abstract suspend fun delete(vararg ids: Long)
    abstract suspend fun undoDelete()
}