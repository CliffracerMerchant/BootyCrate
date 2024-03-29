/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cliffracertech.bootycrate.utils.collectForTesting
import com.cliffracertech.bootycrate.utils.getTestDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemGroupDatabaseTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = getTestDatabase(context)
    private val dao = db.itemGroupDao()

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
            dao.add(DatabaseItemGroup(name = names[1]))
            dao.add(listOf(DatabaseItemGroup(name = names[2]),
                           DatabaseItemGroup(name = names[3])))
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

    @Test fun itemGroupsFlow() {
        var newItemGroupId = -1L
        val results = dao.getAll().collectForTesting(
            timeOut = 200L,
            { },
            { newItemGroupId = runBlocking { dao.add("new item group") }},
            { runBlocking {
                db.itemDao().add(
                    itemGroupId = newItemGroupId,
                    items = listOf(
                        ShoppingListItem(name = ""),
                        ShoppingListItem(name = ""),
                        InventoryItem(name = "")))
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
        val inventory = items.find { it.id == newItemGroupId }
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
            val cursor = db.query("SELECT multiSelectGroups FROM settings LIMIT 1", null)
            cursor.moveToFirst()
            cursor.getInt(0) == 0
        }
        assertThat(singleSelect).isTrue()
    }

    @Test fun singleSelectItemGroups() {
        var firstItem = dao.getAllNow()[0]
        assertThat(firstItem.isSelected).isTrue()
        addedInventoryIsSelected()
        runBlocking { dao.updateIsSelected(firstItem.id) }

        val items = dao.getAllNow()
        firstItem = items.find { it.id == firstItem.id }!!
        assertThat(firstItem.isSelected).isTrue()
        val secondItem = items.find { it.id != firstItem.id }!!
        assertThat(secondItem.isSelected).isFalse()
    }

    @Test fun multiSelectItemGroups() {
        val secondItemId = runBlocking {
            db.settingsDao().updateMultiSelectGroups(false)
            dao.add("")
        }
        assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(2)
        runBlocking { dao.add("") }
        assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(3)

        runBlocking { dao.updateIsSelected(secondItemId) }
        val items = dao.getAllNow()
        assertThat(items.count { it.isSelected }).isEqualTo(2)
        assertThat(items.find { it.id == secondItemId}?.isSelected).isEqualTo(false)
    }

    @Test fun changingToSingleSelectWithMultiSelectionUnselectsAllButOne() {
        runBlocking {
            db.settingsDao().updateMultiSelectGroups(false)
            dao.add("")
            dao.add("")
            assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(3)
            db.settingsDao().updateMultiSelectGroups(true)
        }
        assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(1)
    }

    @Test fun cannotDeselectLastInventoryWhileMultiSelecting() {
        val firstItemId = dao.getAllNow()[0].id
        runBlocking {
            db.settingsDao().updateMultiSelectGroups(false)
            val id = dao.add("")
            dao.updateIsSelected(id)
        }
        assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(1)
        assertThat(dao.getAllNow().find { it.id == firstItemId }?.isSelected).isEqualTo(true)
        runBlocking { dao.updateIsSelected(firstItemId) }
        assertThat(dao.getAllNow().count { it.isSelected }).isEqualTo(1)
        assertThat(dao.getAllNow().find { it.id == firstItemId }?.isSelected).isEqualTo(true)
    }
}