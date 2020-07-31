/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** A Room database to access the tables shopping_list_item and inventory_item. */
@Database(entities = [ShoppingListItem::class, InventoryItem::class],
          version = 2, exportSchema = false)
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
                                                       addMigrations(MIGRATION_1_2).build()
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

//        private val MIGRATION_1_2 = object: Migration(4, 1) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("PRAGMA foreign_keys=off")
//                database.execSQL("BEGIN TRANSACTION")
//                database.execSQL("CREATE TABLE IF NOT EXISTS `shopping_list_item_copy` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `extraInfo` TEXT NOT NULL , `color` INTEGER NOT NULL, `amountOnList` INTEGER NOT NULL DEFAULT 1, `amountInCart` INTEGER NOT NULL DEFAULT 0, `linkedInventoryItemId` INTEGER, `inTrash` INTEGER NOT NULL DEFAULT 0)")
//                database.execSQL("INSERT INTO shopping_list_item_copy (id, name, extraInfo, color, amountOnList, amountInCart, linkedInventoryItemId, inTrash) SELECT id, name, extraInfo, color, amount, amountInCart, linkedInventoryItemId, inTrash FROM shopping_list_item")
//                database.execSQL("DROP TABLE shopping_list_item")
//                database.execSQL("ALTER TABLE shopping_list_item_copy RENAME TO shopping_list_item")
//                database.execSQL("COMMIT")
//                database.execSQL("PRAGMA foreign_keys=on")
//            }
//        }
    }
}

enum class Sort { Color, NameAsc, NameDesc, AmountAsc, AmountDesc }