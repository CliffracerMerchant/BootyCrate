/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.content.Context
import android.net.Uri
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.GlobalScope
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
@Database(entities = [DatabaseBootyCrateItem::class], version = 1)
abstract class BootyCrateDatabase : RoomDatabase() {

    abstract fun dao(): BootyCrateItemDao

    companion object {
        var instance: BootyCrateDatabase? = null
        fun get(context: Context, overwriteExistingDb: Boolean = false) = run {
            val instance = this.instance
            if (!overwriteExistingDb && instance != null) instance
            else Room.databaseBuilder(context.applicationContext,
                                      BootyCrateDatabase::class.java,
                                      "booty-crate-db").addCallback(callback)
                                      .build().also { this.instance = it }
        }

        fun backup(context: Context, backupUri: Uri) {
            val db = get(context)
            val databasePath = db.openHelper.readableDatabase.path
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
            db.close()
            val writer = context.contentResolver.openOutputStream(backupUri)
            writer?.write(File(databasePath).readBytes())
            writer?.close()
        }

        fun replaceWithBackup(context: Context, backupUri: Uri) {
            val importReader = context.contentResolver.openInputStream(backupUri) ?: return
            val currentDb = get(context)
            val dbFile = File(currentDb.openHelper.readableDatabase.path)
            currentDb.close()
            dbFile.delete()
            dbFile.writeBytes(importReader.readBytes())
            // To make a new instance instead of retaining the old one
            get(context, overwriteExistingDb = true)
            return
        }

        fun mergeWithBackup(context: Context, backupUri: Uri) {
            val importReader = context.contentResolver.openInputStream(backupUri) ?: return
            val currentDb = get(context)

            // Room can only open databases in the app's database directory,
            // making it necessary to copy the imported database here first.
            val tempDbName = "tempDb"
            val tempDbFile = context.getDatabasePath(tempDbName)
            tempDbFile.writeBytes(importReader.readBytes())

            val importedDb = Room.databaseBuilder(context, BootyCrateDatabase::class.java, tempDbName).
                    allowMainThreadQueries().createFromFile(tempDbFile).build()
            val items = importedDb.dao().getAllNow()
            for (item in items) { item.id = 0 }
            importedDb.close()
            tempDbFile.delete()

            // Add the imported items to the current database
            GlobalScope.launch { currentDb.dao().add(items) }
        }

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
                // The auto_delete_items trigger will remove an item from the database when
                // its shoppingListAmount and inventoryAmount fields both are equal to -1
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_delete_items`
                              AFTER UPDATE OF inventoryAmount, shoppingListAmount
                                    ON bootycrate_item
                                    WHEN new.shoppingListAmount == -1
                                    AND new.inventoryAmount == -1
                              BEGIN DELETE FROM bootycrate_item WHERE id = new.id; END""")

                // These triggers will execute the auto add to shopping list feature of
                // inventory items if an item's amount falls below its autoAddToShoppingListAmount
                // and its autoAddToShoppingList field is true
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_amount_update`
                              AFTER UPDATE OF inventoryAmount ON bootycrate_item
                                    WHEN new.autoAddToShoppingList == 1
                                    AND new.inventoryAmount < new.autoAddToShoppingListAmount
                              BEGIN $updateShoppingListAmount; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_auto_add_update`
                              AFTER UPDATE OF autoAddToShoppingList ON bootycrate_item
                                    WHEN new.autoAddToShoppingList == 1
                                    AND new.inventoryAmount < new.autoAddToShoppingListAmount
                              BEGIN $updateShoppingListAmount; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_auto_add_amount_update`
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
            private val updateShoppingListAmount =
                """UPDATE bootycrate_item
                     SET inShoppingListTrash = 0, shoppingListAmount =
                       CASE WHEN shoppingListAmount >
                                 (SELECT new.autoAddToShoppingListAmount - new.inventoryAmount)
                            THEN shoppingListAmount
                            ELSE (SELECT new.autoAddToShoppingListAmount - new.inventoryAmount)
                       END
                     WHERE id = new.id"""
        }
    }
}