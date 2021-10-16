/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.themedAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * The BootyCrate application database.
 *
 * BootyCrateDatabase is an implementation of RoomDatabase that provides an
 * instance of a BootyCrateItemDao in order to query and alter the contents
 * of the bootycrate_item table. BootyCrateDatabase functions as a singleton,
 * with the current instance obtained using the static function get.
 */
@Database(entities = [DatabaseBootyCrateItem::class, DatabaseInventory::class,
                      DatabaseSettings::class], version = 3)
abstract class BootyCrateDatabase : RoomDatabase() {

    abstract fun itemDao(): BootyCrateItemDao
    abstract fun inventoryDao(): BootyCrateInventoryDao
    abstract fun dbSettingsDao(): DatabaseSettingsDao

    companion object {
        private const val firstInventoryName="BootyCrate"
        var instance: BootyCrateDatabase? = null
        fun get(context: Context, overwriteExistingDb: Boolean = false) = run {
            val instance = this.instance
            if (!overwriteExistingDb && instance != null) instance
            else Room.databaseBuilder(context.applicationContext,
                                      BootyCrateDatabase::class.java,
                                      "booty-crate-db").addCallback(callback)
                                      .addMigrations(Migration1to2(), Migration2to3())
                                      .build().also { this.instance = it }
        }

        fun getInMemoryDb(context: Context) =
            Room.inMemoryDatabaseBuilder(context.applicationContext, BootyCrateDatabase::class.java)
                .addCallback(callback).build()

        fun backup(context: Context, backupUri: Uri) {
            val db = get(context)
            val databasePath = db.openHelper.readableDatabase.path
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
            db.close()
            val writer = context.contentResolver.openOutputStream(backupUri)
            writer?.write(File(databasePath).readBytes())
            writer?.close()
        }

        fun importBackup(context: Context, backupUri: Uri, overwriteExistingDb: Boolean) {
            val importReader = context.contentResolver.openInputStream(backupUri) ?: return
            // Room can only open databases in the app's database directory,
            // making it necessary to copy the imported database here first.
            val tempDbName = "tempDb"
            val tempDbFile = context.getDatabasePath(tempDbName)
            tempDbFile.writeBytes(importReader.readBytes())

            val tempDb = Room.databaseBuilder(context, BootyCrateDatabase::class.java, tempDbName).
                                        allowMainThreadQueries().createFromFile(tempDbFile).build()
            val inventories = try { tempDb.inventoryDao().getAllNow() }
                              catch(e: IllegalStateException) { emptyList() }
            val items = try { tempDb.itemDao().getAllNow() }
                        catch(e: IllegalStateException) { emptyList() }
            tempDb.close()
            tempDbFile.delete()

            if (inventories.isEmpty() || items.isEmpty())
                themedAlertDialogBuilder(context)
                    .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .setMessage(R.string.invalid_imported_db_error_message)
                    .setTitle(R.string.error).show()
            else CoroutineScope(Dispatchers.IO).launch {
                val currentDb = get(context)
                if (overwriteExistingDb) {
                    currentDb.openHelper.writableDatabase.execSQL("DROP TRIGGER `ensure_at_least_one_inventory`")
                    currentDb.inventoryDao().deleteAll()
                    currentDb.inventoryDao().add(inventories)
                    currentDb.openHelper.writableDatabase.addEnsureAtLeastOneInventoryTrigger()
                    currentDb.itemDao().deleteAll()
                } else for (inventory in inventories) {
                    val oldId = inventory.id
                    inventory.id = 0
                    val newId = currentDb.inventoryDao().add(inventory)
                    items.filter { it.inventoryId == oldId && it.id != 0L }.forEach {
                        it.inventoryId = newId
                        it.id = 0
                    }
                }
                currentDb.itemDao().add(items)
            }
        }

        private fun SupportSQLiteDatabase.addEnsureAtLeastOneInventoryTrigger() =
            execSQL("""CREATE TRIGGER IF NOT EXISTS `ensure_at_least_one_inventory`
                       BEFORE DELETE ON inventory WHEN (SELECT count(*) FROM inventory) == 1
                       BEGIN SELECT RAISE(IGNORE); END;""")
        private fun SupportSQLiteDatabase.addEnsureAtLeastOneSelectedInventoryTrigger() =
            execSQL("""CREATE TRIGGER IF NOT EXISTS `ensure_at_least_one_selected_inventory`
                       AFTER DELETE ON inventory
                       WHEN (SELECT COUNT(*) FROM inventory WHERE isSelected) == 0
                       BEGIN UPDATE inventory SET isSelected = 1
                             WHERE id = (SELECT id FROM inventory LIMIT 1);
                       END;""")
        private fun SupportSQLiteDatabase.addEnforceSingleSelectInventoryTriggers() {
            execSQL("""CREATE TRIGGER IF NOT EXISTS `enforce_single_select_inventory_1`
                       BEFORE UPDATE OF isSelected ON inventory
                       WHEN new.isSelected == 1
                       AND (SELECT singleSelectInventories FROM dbSettings LIMIT 1) == 1
                       BEGIN UPDATE inventory SET isSelected = 0; END;""")
            execSQL("""CREATE TRIGGER IF NOT EXISTS `enforce_single_select_inventory_2`
                       AFTER UPDATE OF singleSelectInventories ON dbSettings
                       WHEN new.singleSelectInventories == 1
                       BEGIN UPDATE inventory SET isSelected = 1
                             WHERE inventory.id = (SELECT id FROM inventory
                                                   WHERE isSelected LIMIT 1);
                       END;""")
            execSQL("""CREATE TRIGGER IF NOT EXISTS `enforce_single_select_inventory_3`
                       AFTER INSERT ON inventory
                       WHEN (SELECT COUNT(*) FROM inventory WHERE isSelected) > 1
                       AND (select singleSelectInventories FROM dbSettings LIMIT 1) == 1
                       BEGIN UPDATE inventory SET isSelected = 1 WHERE id = new.id; END;""")
        }

        /** The auto_delete_items trigger will remove an item from the database when
         * its shoppingListAmount and inventoryAmount fields both are equal to -1. */
        private fun SupportSQLiteDatabase.addAutoDeleteTrigger() = execSQL("""
            CREATE TRIGGER IF NOT EXISTS `auto_delete_items`
            AFTER UPDATE OF inventoryAmount, shoppingListAmount ON bootycrate_item
            WHEN new.shoppingListAmount == -1 AND new.inventoryAmount == -1
            BEGIN DELETE FROM bootycrate_item WHERE id = new.id; END""")

        /** The auto add to shopping list triggers will execute the auto add to shopping
         * list feature of inventory items if an item's amount falls below its
         * autoAddToShoppingListAmount and its autoAddToShoppingList field is true */
        private fun SupportSQLiteDatabase.addAutoAddToShoppingListTriggers() {
            execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_amount_update`
                              AFTER UPDATE OF inventoryAmount ON bootycrate_item
                                    WHEN new.autoAddToShoppingList == 1
                                    AND new.inventoryAmount < new.autoAddToShoppingListAmount
                              BEGIN $updateShoppingListAmount; END""")
            execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_auto_add_update`
                              AFTER UPDATE OF autoAddToShoppingList ON bootycrate_item
                                    WHEN new.autoAddToShoppingList == 1
                                    AND new.inventoryAmount < new.autoAddToShoppingListAmount
                              BEGIN $updateShoppingListAmount; END""")
            execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_auto_add_amount_update`
                              AFTER UPDATE OF autoAddToShoppingListAmount ON bootycrate_item
                                    WHEN new.autoAddToShoppingList == 1
                                    AND new.inventoryAmount < new.autoAddToShoppingListAmount
                              BEGIN $updateShoppingListAmount; END""")
        }

        /* Unfortunately SQLite's limitation of not being able to use common table
         * expressions in triggers makes the following less readable than it should
         * be. The query below updates the shopping list amount of the BootyCrateItem
         * to be the greater of its current amount (if it is already on the shopping
         * list) or the inventory item it is based on's addToShoppingListAmount
         * minus its current amount). */
        private const val updateShoppingListAmount =
            """UPDATE bootycrate_item
                 SET inShoppingListTrash = 0, shoppingListAmount =
                   CASE WHEN shoppingListAmount >
                             (SELECT new.autoAddToShoppingListAmount - new.inventoryAmount)
                        THEN shoppingListAmount
                        ELSE (SELECT new.autoAddToShoppingListAmount - new.inventoryAmount) END
                 WHERE id = new.id"""

        private fun SupportSQLiteDatabase.addSelectedInventoriesIndex() = execSQL(
            "CREATE INDEX IF NOT EXISTS `selected_inventories` ON inventory (id) WHERE isSelected")

        private val callback = object: Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("UPDATE bootycrate_item " +
                           "SET selectedInShoppingList = 0, expandedInShoppingList = 0, " +
                           "selectedInInventory = 0, expandedInInventory = 0")
                db.execSQL("UPDATE bootycrate_item " +
                           "SET shoppingListAmount = -1, inShoppingListTrash = 0 " +
                           "WHERE inShoppingListTrash")
                db.execSQL("UPDATE bootycrate_item " +
                           "SET inventoryAmount = -1, inInventoryTrash = 0 " +
                           "WHERE inInventoryTrash")
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO dbSettings DEFAULT VALUES")
                db.insert("inventory", 0, ContentValues().apply { put("name", firstInventoryName)
                                                                  put("isSelected", 1) })
                db.addEnsureAtLeastOneInventoryTrigger()
                db.addEnsureAtLeastOneSelectedInventoryTrigger()
                db.addEnforceSingleSelectInventoryTriggers()
                db.addAutoDeleteTrigger()
                db.addAutoAddToShoppingListTriggers()
                db.addSelectedInventoriesIndex()
            }
        }
    }

    private class Migration1to2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE inventory (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                                  `name` TEXT NOT NULL,
                                                  `isSelected` INTEGER NOT NULL DEFAULT 0)""")
            val values = ContentValues().apply { put("name", firstInventoryName)
                                                 put("isSelected", 1) }
            val insertedId = db.insert("inventory", 0, values)

            db.execSQL("PRAGMA foreign_keys=off")
            db.execSQL("BEGIN TRANSACTION")
            db.execSQL("""CREATE TABLE IF NOT EXISTS temp_table (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `inventoryId` INTEGER NOT NULL, 
                `name` TEXT NOT NULL, `extraInfo` TEXT NOT NULL DEFAULT '', `color` INTEGER NOT NULL DEFAULT 0,
                `isChecked` INTEGER NOT NULL DEFAULT 0, `shoppingListAmount` INTEGER NOT NULL DEFAULT -1,
                `expandedInShoppingList` INTEGER NOT NULL DEFAULT 0, `selectedInShoppingList` INTEGER NOT NULL DEFAULT 0,
                `inShoppingListTrash` INTEGER NOT NULL DEFAULT 0, `inventoryAmount` INTEGER NOT NULL DEFAULT -1,
                `expandedInInventory` INTEGER NOT NULL DEFAULT 0, `selectedInInventory` INTEGER NOT NULL DEFAULT 0,
                `autoAddToShoppingList` INTEGER NOT NULL DEFAULT 0, `autoAddToShoppingListAmount` INTEGER NOT NULL DEFAULT 1,
                `inInventoryTrash` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`inventoryId`) REFERENCES `inventory`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
            db.execSQL("""INSERT INTO temp_table(
                              id, inventoryId, name, extraInfo, color, isChecked, shoppingListAmount,
                              expandedInShoppingList, selectedInShoppingList, inShoppingListTrash,
                              inventoryAmount, expandedInInventory, selectedInInventory,
                              autoAddToShoppingList, autoAddToShoppingListAmount, inInventoryTrash)
                          SELECT id, (SELECT $insertedId), name, extraInfo, color, isChecked, shoppingListAmount,
                                 expandedInShoppingList, selectedInShoppingList, inShoppingListTrash,
                                 inventoryAmount, expandedInInventory, selectedInInventory,
                                 autoAddToShoppingList, autoAddToShoppingListAmount, inInventoryTrash
                          FROM bootycrate_item;""")
            db.execSQL("CREATE INDEX `index_bootycrate_item_inventoryId` ON `temp_table` (`inventoryId`)")
            db.execSQL("DROP TABLE bootycrate_item;")
            db.execSQL("ALTER TABLE temp_table RENAME TO bootycrate_item;")
            db.execSQL("COMMIT;")
            db.execSQL("PRAGMA foreign_keys=on;")
        }
    }

    private class Migration2to3 : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS dbSettings (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `singleSelectInventories` INTEGER NOT NULL DEFAULT 1)""")
            db.execSQL("INSERT INTO dbSettings DEFAULT VALUES")
            db.addEnsureAtLeastOneInventoryTrigger()
            db.addEnsureAtLeastOneSelectedInventoryTrigger()
            db.addEnforceSingleSelectInventoryTriggers()
            db.addSelectedInventoriesIndex()
        }
    }
}