/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.net.Uri
import android.view.ViewPropertyAnimator
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

/** A Room database to access the tables shopping_list_item and inventory_item.
 *
 *  BootyCrateDatabase is an implementation of RoomDatabase that provides
 *  instances of a ShoppingListItemDao and an InventoryItemDao in order to
 *  query the contents of the shopping_list_item and inventory_item tables.
 *
 *  BootyCrateDatabase functions as a singleton, with the current instance
 *  obtained using the static function get. get also takes an optional boolean
 *  parameter overwriteExistingDb that, when true, will overwrite the current
 *  instance with a new one. This might be necessary when, for example, the
 *  database file is overwritten with another one, and the database needs to be
 *  reopened.
 *
 *  The function backup will export a copy of the current database file to the
 *  location pointed to by the parameter backupUri.
 *
 *  The function replaceWithBackup will overwrite the existing database with
 *  the one pointed to by the parameter backupUri. Note that when the existing
 *  database is replaced, activities that have obtained instances of the con-
 *  tained DAOs will need to retrieve new DAOs, or crashes are likely to occur
 *  when the old DAOs are used.
 *
 *  The function mergeWithBackup will attempt to open the database pointed to
 *  by the parameter backupUri as a temporary second database, read the shop-
 *  ping list and inventory items in the database, and then add them to the
 *  current database. */
@Database(entities = [ShoppingListItem::class, InventoryItem::class],
          version = 5, exportSchema = false)
abstract class BootyCrateDatabase : RoomDatabase() {

    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao

    companion object {
        @Volatile private var instance: BootyCrateDatabase? = null

        fun get(context: Context, overwriteExistingDb: Boolean = false): BootyCrateDatabase {
            if (!overwriteExistingDb) {
                val instance = this.instance
                if (instance != null) return instance
            }
            synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext, BootyCrateDatabase::class.java, "booty-crate-db").
                    addMigrations(MIGRATION_1_2).addMigrations(MIGRATION_2_3).
                    addMigrations(MIGRATION_3_4).addMigrations(MIGRATION_4_5).build()
                this.instance = newInstance
                return newInstance
            }
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
            get(context, overwriteExistingDb = true) // To make a new instance instead of retaining the old one
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
            val shoppingListItems = importedDb.shoppingListItemDao().getAllNow()
            val inventoryItems = importedDb.inventoryItemDao().getAllNow()
            for (item in inventoryItems) item.id = 0
            for (item in shoppingListItems) item.id = 0
            importedDb.close()
            tempDbFile.delete()

            // Add the imported items to the current database
            GlobalScope.launch { currentDb.shoppingListItemDao().add(shoppingListItems) }
            GlobalScope.launch { currentDb.inventoryItemDao().add(inventoryItems) }
        }

        private val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("BEGIN TRANSACTION")
                database.execSQL("ALTER TABLE shopping_list_item ADD `isChecked` INTEGER NOT NULL DEFAULT 0 ")
                database.execSQL("COMMIT")
            }
        }

        private val MIGRATION_2_3 = object: Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys=off")
                database.execSQL("BEGIN TRANSACTION")
                database.execSQL("CREATE TABLE IF NOT EXISTS `shopping_list_item_copy` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `extraInfo` TEXT NOT NULL DEFAULT '', `color` INTEGER NOT NULL DEFAULT 0, `amount` INTEGER NOT NULL DEFAULT 1, `inTrash` INTEGER NOT NULL DEFAULT 0, `isChecked` INTEGER NOT NULL DEFAULT 0, `amountInCart` INTEGER NOT NULL DEFAULT 0, `linkedInventoryItemId` INTEGER)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `inventory_item_copy` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `extraInfo` TEXT NOT NULL DEFAULT '', `color` INTEGER NOT NULL DEFAULT 0, `amount` INTEGER NOT NULL DEFAULT 1, `inTrash` INTEGER NOT NULL DEFAULT 0, `autoAddToShoppingList` INTEGER NOT NULL DEFAULT 0, `autoAddToShoppingListTrigger` INTEGER NOT NULL DEFAULT 1)")
                database.execSQL("INSERT INTO shopping_list_item_copy (id, name, extraInfo, color, amount, inTrash, isChecked, amountInCart, linkedInventoryItemId) SELECT id, name, extraInfo, color, amountOnList, inTrash, isChecked, amountInCart, linkedInventoryItemId FROM shopping_list_item")
                database.execSQL("INSERT INTO inventory_item_copy (id, name, extraInfo, color, amount, inTrash, autoAddToShoppingList, autoAddToShoppingListTrigger) SELECT id, name, extraInfo, color, amount, inTrash, autoAddToShoppingList, autoAddToShoppingListTrigger FROM inventory_item")
                database.execSQL("DROP TABLE shopping_list_item")
                database.execSQL("DROP TABLE inventory_item")
                database.execSQL("ALTER TABLE shopping_list_item_copy RENAME TO shopping_list_item")
                database.execSQL("ALTER TABLE inventory_item_copy RENAME TO inventory_item")
                database.execSQL("COMMIT")
                database.execSQL("PRAGMA foreign_keys=on")
            }
        }

        private val MIGRATION_3_4 = object: Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys=off")
                database.execSQL("BEGIN TRANSACTION")
                database.execSQL("CREATE TABLE IF NOT EXISTS `shopping_list_item_copy` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `extraInfo` TEXT NOT NULL DEFAULT '', `color` INTEGER NOT NULL DEFAULT 0, `amount` INTEGER NOT NULL DEFAULT 1, `inTrash` INTEGER NOT NULL DEFAULT 0, `isChecked` INTEGER NOT NULL DEFAULT 0, `linkedInventoryItemId` INTEGER)")
                database.execSQL("INSERT INTO shopping_list_item_copy (id, name, extraInfo, color, amount, inTrash, isChecked, linkedInventoryItemId) SELECT id, name, extraInfo, color, amount, inTrash, isChecked, linkedInventoryItemId FROM shopping_list_item")
                database.execSQL("DROP TABLE shopping_list_item")
                database.execSQL("ALTER TABLE shopping_list_item_copy RENAME TO shopping_list_item")
                database.execSQL("COMMIT")
                database.execSQL("PRAGMA foreign_keys=on")
            }
        }

        private val MIGRATION_4_5 = object: Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys=off")
                database.execSQL("BEGIN TRANSACTION")
                database.execSQL("CREATE TABLE IF NOT EXISTS `inventory_item_copy` (`addToShoppingList` INTEGER NOT NULL DEFAULT 0, `addToShoppingListTrigger` INTEGER NOT NULL DEFAULT 1, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `extraInfo` TEXT NOT NULL DEFAULT '', `color` INTEGER NOT NULL DEFAULT 0, `amount` INTEGER NOT NULL DEFAULT 1, `inTrash` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("INSERT INTO inventory_item_copy (id, name, extraInfo, color, amount, inTrash, addToShoppingList, addToShoppingListTrigger) SELECT id, name, extraInfo, color, amount, inTrash, autoAddToShoppingList, autoAddToShoppingListTrigger FROM inventory_item")
                database.execSQL("DROP TABLE inventory_item")
                database.execSQL("ALTER TABLE inventory_item_copy RENAME TO inventory_item")
                database.execSQL("COMMIT")
                database.execSQL("PRAGMA foreign_keys=on")
            }
        }
    }
}