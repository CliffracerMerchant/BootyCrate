/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    @Query("UPDATE dbSettings SET multiSelectInventories = :multiSelect")
    abstract suspend fun updateMultiSelectInventories(multiSelect: Boolean)
}

class DatabaseSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = BootyCrateDatabase.get(app).dbSettingsDao()

    val multiSelectInventories = dao.getMultiSelectInventories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun updateMultiSelectInventories(multiSelect: Boolean): Job =
        viewModelScope.launch { dao.updateMultiSelectInventories(multiSelect) }

    fun toggleMultiSelectInventories() =
        viewModelScope.launch { dao.updateMultiSelectInventories(!multiSelectInventories.value) }
}