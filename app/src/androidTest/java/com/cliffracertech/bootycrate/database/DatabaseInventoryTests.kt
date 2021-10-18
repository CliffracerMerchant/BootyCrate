/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cliffracertech.bootycrate.dlog
import com.cliffracertech.bootycrate.utils.collectForTesting
import com.cliffracertech.bootycrate.utils.resultAfter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseInventoryTests {
    private lateinit var db: BootyCrateDatabase
    private lateinit var dao: BootyCrateInventoryDao

    @Before fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = BootyCrateDatabase.getInMemoryDb(context)
        dao = db.inventoryDao()
    }

    @After fun closeDb() = db.close()

    @Test fun initialState() {
        val items = dao.getAllNow()
        assertThat(items.size).isEqualTo(1)
        assertThat(items[0].isSelected).isTrue()
        assertThat(items[0].name).isNotEmpty()
    }

    @Test fun inserting() {
        val names = listOf("Pantry", "Refrigerator", "Pantry2", "Refrigerator2")
        runBlocking {
            dao.add(names[0])
            dao.add(DatabaseInventory(name = names[1]))
            dao.add(listOf(DatabaseInventory(name = names[2]),
                           DatabaseInventory(name = names[3])))
        }
        val itemNames = dao.getAllNow().map { it.name }
        assertThat(itemNames.size).isEqualTo(names.size + 1)
        names.forEach { assertThat(it in itemNames) }
    }

    @Test fun deleting() {
        runBlocking {
            listOf("Pantry", "Refrigerator", "Pantry2", "Refrigerator2")
                .forEach { dao.add(it) }
        }
        val startItemIds = dao.getAllNow().map { it.id }

        runBlocking { dao.delete(startItemIds[2]) }
        var itemIds = dao.getAllNow().map { it.id }
        assertThat(startItemIds[2] !in itemIds)
        assertThat(itemIds.size).isEqualTo(4)

        runBlocking { dao.delete(startItemIds[3]) }
        itemIds = dao.getAllNow().map { it.id }
        assertThat(startItemIds[3] !in itemIds)
        assertThat(itemIds.size).isEqualTo(3)
    }

    @Test fun lastInventoryIsUndeletable() {
        val itemId = dao.getAllNow()[0].id
        runBlocking { dao.delete(itemId) }
        var items = dao.getAllNow()
        assertThat(items.size).isEqualTo(1)

        runBlocking { dao.add("second inventory")
                      dao.delete(itemId) }
        items = dao.getAllNow()
        assertThat(items.size).isEqualTo(1)
        val newItemId = dao.getAllNow()[0].id
        runBlocking { dao.delete(newItemId) }
        items = dao.getAllNow()
        assertThat(items.size).isEqualTo(1)
    }

    @Test fun updateNames() {
        var id = dao.getAllNow()[0].id
        runBlocking { dao.updateName(id, "new name")
                      id = dao.add("new item") }
        var names = dao.getAllNow().map { it.name }
        assertThat(names.contains("new name")).isTrue()
        assertThat(names.contains("new item")).isTrue()

        runBlocking { dao.updateName(id, "another new name") }
        names = dao.getAllNow().map { it.name }
        assertThat(names.contains("another new name")).isTrue()
    }

    @Test fun inventoriesFlow() {
        var newInventoryId = -1L
        val results = dao.getAll().collectForTesting(timeOut = 100L, { },
            { newInventoryId = runBlocking { dao.add("new inventory") }},
            { runBlocking { db.itemDao().addConvertibles(listOf(
                ShoppingListItem(inventoryId = newInventoryId, name = ""),
                ShoppingListItem(inventoryId = newInventoryId, name = ""),
                InventoryItem(inventoryId = newInventoryId, name = "")))
            }})

        assertThat(results.size).isEqualTo(3)
        var items = results[0]
        assertThat(items.size).isEqualTo(1)
        assertThat(items[0].shoppingListItemCount).isEqualTo(0)
        assertThat(items[0].inventoryItemCount).isEqualTo(0)

        items = results[1]
        assertThat(items.size).isEqualTo(2)
        assertThat(items.find { it.name == "new inventory"}).isNotNull()

        items = results[2]
        assertThat(items).isNotNull()
        val inventory = items.find { it.id == newInventoryId }
        assertThat(inventory).isNotNull()

        assertThat(inventory!!.inventoryItemCount).isEqualTo(1)
        assertThat(inventory.shoppingListItemCount).isEqualTo(2)

    }

    @Test fun addedInventoryIsSelected() {
        val newId = runBlocking { dao.add("") }
        val newItem = dao.getAllNow().find { it.id == newId }
        assertThat(newItem).isNotNull()
        assertThat(newItem!!.isSelected).isTrue()
    }

    @Test fun isSingleSelectByDefault() {
        val singleSelect = runBlocking {
            val cursor = db.query("SELECT singleSelectInventories FROM dbSettings LIMIT 1", null)
            cursor.moveToFirst()
            cursor.getInt(0) == 1
        }
        assertThat(singleSelect).isTrue()
    }

    @Test fun singleSelectInventories() {
        var firstItem = dao.getAllNow()[0]
        assertThat(firstItem.isSelected).isTrue()
        addedInventoryIsSelected()
        runBlocking { dao.updateIsSelected(firstItem.id, true) }

        val items = dao.getAllNow()
        firstItem = items.find { it.id == firstItem.id }!!
        assertThat(firstItem.isSelected).isTrue()
        val secondItem = items.find { it.id != firstItem.id }!!
        assertThat(secondItem.isSelected).isFalse()
    }

    @Test fun updateSingleSelect() {
        val dao = db.dbSettingsDao()
        val flow = dao.getSingleSelectInventories()
        var result = flow.resultAfter(200L) { runBlocking { dao.updateSingleSelectInventories(false) }}
        dlog("1 " + result.toString())
        //assertThat(result).isEqualTo(false)
        result = flow.resultAfter(200L) { runBlocking { dao.updateSingleSelectInventories(true) }}
        dlog("2 " + result.toString())
        //assertThat(result).isEqualTo(true)
        result = flow.resultAfter(200L) { }
        dlog("3 " + result.toString())
//        val results = flow.collectForTesting(timeOut = 500L,
//            { },
//            { runBlocking { dao.updateSingleSelectInventories(true) }})
//        dlog(results.toString())
//        assertThat(results.size).isEqualTo(2)
//        assertThat(results[0]).isFalse()
//        assertThat(results[1]).isTrue()
    }

    @Test fun multiSelectInventories() {
        runBlocking {
            db.dbSettingsDao().updateSingleSelectInventories(false)
            dao.add("")
        }
        assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(2)
        runBlocking { dao.add("") }
        assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(3)
    }

    @Test fun changingToSingleSelectWithMultiSelection() {
        runBlocking {
            db.dbSettingsDao().updateSingleSelectInventories(false)
            dao.add("")
            dao.add("")
            assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(3)
            db.dbSettingsDao().updateSingleSelectInventories(true)
        }
        assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(1)
    }


}