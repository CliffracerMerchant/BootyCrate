/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dbSettings")
class DatabaseSettings(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Long = 0,
    @ColumnInfo(name="multiSelectInventories", defaultValue="0")
    var multiSelectInventories: Boolean = false)

@Dao abstract class DatabaseSettingsDao {
    @Query("SELECT multiSelectInventories FROM dbSettings LIMIT 1")
    abstract fun getMultiSelectInventories(): Flow<Boolean>

    @Query("SELECT multiSelectInventories FROM dbSettings LIMIT 1")
    abstract suspend fun getMultiSelectInventoriesNow(): Boolean

    @Query("UPDATE dbSettings SET multiSelectInventories = :multiSelect")
    abstract suspend fun updateMultiSelectInventories(multiSelect: Boolean)

    @Query("UPDATE dbSettings SET multiSelectInventories = 1 - multiSelectInventories")
    abstract suspend fun toggleMultiSelectInventories()
}