/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A Room database to access the tables shopping_list_item and inventory_item.
 *
 * BootyCrateDatabase is an implementation of RoomDatabase that provides
 * instances of a ShoppingListItemDao and an InventoryItemDao in order to
 * query the contents of the shopping_list_item and inventory_item tables.
 *
 * BootyCrateDatabase functions as a singleton, with the current instance
 * obtained using the static function get. get also takes an optional boolean
 * parameter overwriteExistingDb that, when true, will overwrite the current
 * instance with a new one. This might be necessary when, for example, the
 * database file is overwritten with another one, and the database needs to be
 * reopened.
 *
 * The function backup will export a copy of the current database file to the
 * location pointed to by the parameter backupUri.
 *
 * The function replaceWithBackup will overwrite the existing database with
 * the one pointed to by the parameter backupUri. Note that when the existing
 * database is replaced, activities that have obtained instances of the con-
 * tained DAOs will need to retrieve new DAOs, or crashes are likely to occur
 * when the old DAOs are used.
 *
 * The function mergeWithBackup will attempt to open the database pointed to
 * by the parameter backupUri as a temporary second database, read the shop-
 * ping list and inventory items in the database, and then add them to the
 * current database.
 */
@Database(entities = [BootyCrateItem::class], version = 1)
abstract class BootyCrateDatabase : RoomDatabase() {

    abstract fun dao(): BootyCrateItemDao

    companion object {
        var instance: BootyCrateDatabase? = null
        fun get(app: Application) = instance ?: Room.databaseBuilder(
            app, BootyCrateDatabase::class.java, "booty-crate-db")
            .addCallback(callback).build().apply { instance = this }

//        fun backup(context: Context, backupUri: Uri) {
//            val db = provideBootyCrateDatabase(context)
//            val databasePath = db.openHelper.readableDatabase.path
//            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
//            db.close()
//            val writer = context.contentResolver.openOutputStream(backupUri)
//            writer?.write(File(databasePath).readBytes())
//            writer?.close()
//        }
//
//        fun replaceWithBackup(context: Context, backupUri: Uri) {
//            val importReader = context.contentResolver.openInputStream(backupUri) ?: return
//            val currentDb = provideBootyCrateDatabase(context)
//            val dbFile = File(currentDb.openHelper.readableDatabase.path)
//            currentDb.close()
//            dbFile.delete()
//            dbFile.writeBytes(importReader.readBytes())
//            get(context, overwriteExistingDb = true) // To make a new instance instead of retaining the old one
//            return
//        }
//
//        fun mergeWithBackup(context: Context, backupUri: Uri) {
//            val importReader = context.contentResolver.openInputStream(backupUri) ?: return
//            val currentDb = get(context)
//
//            // Room can only open databases in the app's database directory,
//            // making it necessary to copy the imported database here first.
//            val tempDbName = "tempDb"
//            val tempDbFile = context.getDatabasePath(tempDbName)
//            tempDbFile.writeBytes(importReader.readBytes())
//
//            val importedDb = Room.databaseBuilder(context, BootyCrateDatabase::class.java, tempDbName).
//                    allowMainThreadQueries().createFromFile(tempDbFile).build()
//            val shoppingListItems = importedDb.shoppingListItemDao().getAllNow()
//            val inventoryItems = importedDb.inventoryItemDao().getAllNow()
//            for (item in shoppingListItems) { item.id = 0; item.linkedItemId = null }
//            for (item in inventoryItems) { item.id = 0; item.linkedItemId = null }
//            importedDb.close()
//            tempDbFile.delete()
//
//            // Add the imported items to the current database
//            GlobalScope.launch { currentDb.shoppingListItemDao().add(shoppingListItems) }
//            GlobalScope.launch { currentDb.inventoryItemDao().add(inventoryItems) }
//        }

        private val callback = object: Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("UPDATE shopping_list_item SET isSelected = 0, isExpanded = 0")
                db.execSQL("UPDATE inventory_item SET isSelected = 0, isExpanded = 0")
                db.execSQL("PRAGMA recursive_triggers = false")
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Auto link / unlink triggers
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_link_inventory_item`
                              AFTER INSERT ON shopping_list_item
                                    WHEN new.linkedItemId NOT NULL
                              BEGIN UPDATE inventory_item
                                    SET linkedItemId = new.id
                                    WHERE id = new.linkedItemId; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_link_shopping_list_item` 
                              AFTER INSERT ON inventory_item
                                    WHEN new.linkedItemId NOT NULL
                              BEGIN UPDATE shopping_list_item 
                                    SET linkedItemId = new.id
                                    WHERE id = new.linkedItemId; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_unlink_inventory_item`
                              AFTER DELETE ON shopping_list_item
                                    WHEN old.linkedItemId NOT NULL
                              BEGIN UPDATE inventory_item
                                    SET linkedItemId = NULL
                                    WHERE id = old.linkedItemId; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_unlink_shopping_list_item`
                              AFTER DELETE ON inventory_item
                                    WHEN old.linkedItemId NOT NULL
                              BEGIN UPDATE shopping_list_item
                                    SET linkedItemId = NULL
                                    WHERE id = old.linkedItemId; END""")

                // Auto update linked item field
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_update_linked_inventory_item_name`
                              AFTER UPDATE OF name ON shopping_list_item
                                    WHEN old.linkedItemId == new.linkedItemId
                                    AND new.linkedItemId NOT NULL
                              BEGIN UPDATE inventory_item
                                    SET name = new.name
                                    WHERE id = new.linkedItemId; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_update_linked_shopping_list_item_name`
                              AFTER UPDATE OF name ON inventory_item
                                    WHEN old.linkedItemId == new.linkedItemId
                                    AND new.linkedItemId NOT NULL
                              BEGIN UPDATE shopping_list_item
                                    SET name = new.name
                                    WHERE id = new.linkedItemId; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_update_linked_inventory_item_extra_info`
                              AFTER UPDATE OF extraInfo ON shopping_list_item
                                    WHEN old.linkedItemId == new.linkedItemId
                                    AND new.linkedItemId NOT NULL
                              BEGIN UPDATE inventory_item
                                    SET extraInfo = new.extraInfo
                                    WHERE id = new.linkedItemId; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_update_linked_shopping_list_extra_info`
                              AFTER UPDATE OF extraInfo ON inventory_item
                                    WHEN old.linkedItemId = new.linkedItemId
                                    AND new.linkedItemId NOT NULL
                              BEGIN UPDATE shopping_list_item
                                    SET extraInfo = new.extraInfo
                                    WHERE id = new.linkedItemId; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_update_linked_inventory_item_color`
                              AFTER UPDATE OF color ON shopping_list_item
                                    WHEN old.linkedItemId == new.linkedItemId
                                    AND new.linkedItemId NOT NULL
                              BEGIN UPDATE inventory_item
                                    SET color = new.color
                                    WHERE id = new.linkedItemId; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `auto_update_linked_shopping_list_item_color`
                              AFTER UPDATE OF color ON inventory_item
                                    WHEN old.linkedItemId == new.linkedItemId
                                    AND new.linkedItemId NOT NULL
                              BEGIN UPDATE shopping_list_item
                                    SET color = new.color
                                    WHERE id = new.linkedItemId; END""")

                // Auto add to shopping list triggers
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_amount_update`
                              AFTER UPDATE OF amount ON inventory_item
                                    WHEN new.addToShoppingList == 1
                                    AND new.amount < new.addToShoppingListTrigger
                              BEGIN $insertFromInventoryItemStr; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_auto_add_update`
                              AFTER UPDATE OF addToShoppingList ON inventory_item
                                    WHEN new.addToShoppingList == 1
                                    AND new.amount < new.addToShoppingListTrigger
                              BEGIN $insertFromInventoryItemStr; END""")
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_auto_add_trigger_update`
                              AFTER UPDATE OF addToShoppingListTrigger ON inventory_item
                                    WHEN new.addToShoppingList == 1
                                    AND new.amount < new.addToShoppingListTrigger
                              BEGIN $insertFromInventoryItemStr; END""")
            }
        }

        /* Unfortunately SQLite's limitation of not being able to use common table
         * expressions in triggers makes the following less readable than it should
         * be. The query below essentially makes a shopping list item with the same
         * name, extraInfo, and color as an inventory item, while also filling in
         * its linkedItemId field to the id of the inventory item it was created
         * from. The new item's amount is the greater of its current amount (if it
         * already existed or the inventory item it is based on's addToShoppingListAmount
         * minus its current amount). It also copies the expanded, selected, and
         * checked state from the already existing item to prevent the overwriting
         * of these values if it already existed. */
        private const val insertFromInventoryItemStr =
            """INSERT OR REPLACE INTO shopping_list_item (id, name, extraInfo, color, linkedItemId,
                                                          amount, isExpanded, isSelected, isChecked)
               SELECT new.linkedItemId, new.name, new.extraInfo, new.color, new.id,
                      CASE WHEN (EXISTS(SELECT 1 FROM shopping_list_item WHERE id == new.linkedItemId) AND
                                 (SELECT amount FROM shopping_list_item WHERE id == new.linkedItemId) >
                                 (SELECT new.addToShoppingListTrigger - new.amount))
                           THEN (SELECT amount FROM shopping_list_item WHERE id == new.linkedItemId)
                           ELSE (SELECT new.addToShoppingListTrigger - new.amount) END,
                      (SELECT isExpanded FROM shopping_list_item WHERE id == new.linkedItemId),
                      (SELECT isSelected FROM shopping_list_item WHERE id == new.linkedItemId),
                      (SELECT isChecked FROM shopping_list_item WHERE id == new.linkedItemId)"""
    }
}