/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Dao abstract class BootyCrateInventoryDao {
    @Insert abstract suspend fun add(inventory: DatabaseInventory): Long
    @Insert abstract suspend fun add(items: List<DatabaseInventory>)

    @Query("UPDATE inventory SET name = :name WHERE id = :id")
    abstract suspend fun updateName(id: Long, name: String)

    @Query("""SELECT id, name, (SELECT count(*) FROM bootycrate_item WHERE bootycrate_item.id = id AND shoppingListAmount != -1),
                               (SELECT count(*) FROM bootycrate_item WHERE bootycrate_item.id = id AND inventoryAmount != -1)
              FROM inventory""")
    abstract fun getAll() : LiveData<List<BootyCrateInventory>>

    @Query("SELECT * FROM inventory")
    abstract fun getAllNow() : List<DatabaseInventory>

    @Query("DELETE FROM inventory WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM inventory")
    abstract suspend fun deleteAll()
}

class InventoryViewModel(app: Application): AndroidViewModel(app) {
    private val dao = BootyCrateDatabase.get(app).inventoryDao()

    private val _selectedInventoryId = MutableStateFlow(-1L)
    val selectedInventoryId = _selectedInventoryId.asStateFlow()

    val inventories = dao.getAll()
}