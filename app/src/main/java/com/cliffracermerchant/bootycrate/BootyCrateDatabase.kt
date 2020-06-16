package com.cliffracermerchant.bootycrate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [InventoryItem::class, ShoppingListItem::class], version = 3, exportSchema = false)
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
                                                       addMigrations(MIGRATION_2_3).build()
                this.instance = newInstance
                return newInstance
            }
        }

        private val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_ke")
            }
        }

        private val MIGRATION_2_3 = object: Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys=off")
                database.execSQL("BEGIN TRANSACTION")
                database.execSQL("CREATE TABLE IF NOT EXISTS `inventory_item_copy` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount` INTEGER NOT NULL DEFAULT 0, `extraInfo` TEXT NOT NULL DEFAULT '', `autoAddToShoppingList` INTEGER NOT NULL DEFAULT 0, `autoAddToShoppingListTrigger` INTEGER NOT NULL DEFAULT 1, `inTrash` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `shopping_list_item_copy` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `extraInfo` TEXT NOT NULL DEFAULT '', `amount` INTEGER NOT NULL DEFAULT 1, `amountInCart` INTEGER NOT NULL DEFAULT 0, `linkedInventoryItemId` INTEGER, `inTrash` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("INSERT INTO inventory_item_copy (id, name, amount, extraInfo, autoAddToShoppingList, autoAddToShoppingListTrigger, inTrash) SELECT id, name, amount, extraInfo, autoAddToShoppingList, autoAddToShoppingListTrigger, inTrash FROM inventory_item")
                database.execSQL("INSERT INTO shopping_list_item_copy (id, name, extraInfo, amount, amountInCart, linkedInventoryItemId, inTrash) SELECT id, name, extraInfo, amount, amountInCart, linkedInventoryItemId, inTrash FROM shopping_list_item")
                database.execSQL("DROP TABLE inventory_item")
                database.execSQL("DROP TABLE shopping_list_item")
                database.execSQL("ALTER TABLE inventory_item_copy RENAME TO inventory_item")
                database.execSQL("ALTER TABLE shopping_list_item_copy RENAME TO shopping_list_item")
                database.execSQL("COMMIT")
                database.execSQL("PRAGMA foreign_keys=on")
            }
        }
    }
}