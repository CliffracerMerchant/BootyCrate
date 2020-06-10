package com.cliffracermerchant.bootycrate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [InventoryItem::class, ShoppingListItem::class], version = 2, exportSchema = false)
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

        val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE shopping_list_item " +
                                 "ADD COLUMN linkedInventoryItemId INTEGER")
            }
        }
    }
}