/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "dbSettings")
class DatabaseSettings(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Long = 0,
    @ColumnInfo(name="singleSelectInventories", defaultValue="1")
    var multiSelectInventories: Boolean = false)

@Dao abstract class DatabaseSettingsDao {
    @Query("SELECT singleSelectInventories FROM dbSettings LIMIT 1")
    abstract fun getSingleSelectInventories(): Flow<Boolean>

    @Query("UPDATE dbSettings SET singleSelectInventories = :singleSelect")
    abstract suspend fun updateSingleSelectInventories(singleSelect: Boolean)
}

class DatabaseSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = BootyCrateDatabase.get(app).dbSettingsDao()

    val singleSelectInventories get() = dao.getSingleSelectInventories().asLiveData()

    fun updateSingleSelectInventories(singleSelect: Boolean) {
        viewModelScope.launch { updateSingleSelectInventories(singleSelect) }
    }
}