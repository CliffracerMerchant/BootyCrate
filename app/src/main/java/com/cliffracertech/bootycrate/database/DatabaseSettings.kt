/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "settings")
class DatabaseSettings(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Long = 0,
    @ColumnInfo(name="multiSelectGroups", defaultValue="0")
    var multiSelectGroups: Boolean = false)

@Dao abstract class SettingsDao {
    @Query("SELECT multiSelectGroups FROM settings LIMIT 1")
    abstract fun getMultiSelectGroups(): Flow<Boolean>

    @Query("SELECT multiSelectGroups FROM settings LIMIT 1")
    abstract suspend fun getMultiSelectGroupsNow(): Boolean

    @Query("UPDATE settings SET multiSelectGroups = :multiSelect")
    abstract suspend fun updateMultiSelectGroups(multiSelect: Boolean)

    @Query("UPDATE settings SET multiSelectGroups = 1 - multiSelectGroups")
    abstract suspend fun toggleMultiSelectGroups()
}