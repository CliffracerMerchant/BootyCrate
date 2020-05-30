package com.cliffracermerchant.bootycrate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [InventoryItem::class, ShoppingListItem::class], version = 1, exportSchema = false)
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
                                                       "booty-crate-db").build()
                this.instance = newInstance
                return newInstance
            }
        }
    }
}