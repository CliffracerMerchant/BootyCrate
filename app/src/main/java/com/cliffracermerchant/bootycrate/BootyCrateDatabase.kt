/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.view.ViewPropertyAnimator
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** A Room database to access the tables shopping_list_item and inventory_item. */
@Database(entities = [ShoppingListItem::class, InventoryItem::class],
          version = 5, exportSchema = false)
abstract class BootyCrateDatabase : RoomDatabase() {

    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao

    companion object {
        @Volatile private var instance: BootyCrateDatabase? = null

        fun get(context: Context): BootyCrateDatabase {
            val instance = this.instance
            if (instance != null) return instance

            synchronized(this) {
                val newInstance = Room.databaseBuilder(context.applicationContext,
                                                       BootyCrateDatabase::class.java,
                                                       "booty-crate-db").
                                                       addMigrations(MIGRATION_1_2).
                                                       addMigrations(MIGRATION_2_3).
                                                       addMigrations(MIGRATION_3_4).
                                                       addMigrations(MIGRATION_4_5).build()
                this.instance = newInstance
                return newInstance
            }
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

class ViewPropertyAnimatorSet {
    private val animators = mutableListOf<ViewPropertyAnimator>()

    fun add(anim: ViewPropertyAnimator) = animators.add(anim)

    fun start() {
        if (animators.isEmpty()) return
        animators.removeAt(animators.size - 1).withStartAction {
            for (anim in animators) anim.start()
        }.start()
    }
}

enum class SelectionState { Selected, NotSelected }