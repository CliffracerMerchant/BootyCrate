/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Singleton @Provides
    fun provideBootyCrateDatabase(@ApplicationContext app: Context) =
        Room.databaseBuilder(app, BootyCrateDatabase::class.java, "booty-crate-db")
            .addCallback(BootyCrateDatabase.Callback())
            .addMigrations(*(BootyCrateDatabase.allMigrations))
            .build()

    @Provides fun provideShoppingListItemDao(db: BootyCrateDatabase) = db.shoppingListItemDao()
    @Provides fun provideInventoryItemDao(db: BootyCrateDatabase) = db.inventoryItemDao()
    @Provides fun provideItemGroupDao(db: BootyCrateDatabase) = db.itemGroupDao()
    @Provides fun provideSettingsDao(db: BootyCrateDatabase) = db.settingsDao()

    @Qualifier @Retention(AnnotationRetention.BINARY)
    annotation class InMemoryDatabase

    @InMemoryDatabase @Singleton @Provides
    fun provideInMemoryBootyCrateDb(@ApplicationContext app: Context) =
        Room.inMemoryDatabaseBuilder(app, BootyCrateDatabase::class.java)
            .addCallback(BootyCrateDatabase.Callback()).build()
}

/** The BootyCrate application database. */
@Database(version = 4, entities = [
    DatabaseListItem::class,
    DatabaseItemGroup::class,
    DatabaseSettings::class])
@TypeConverters(DatabaseListItem.Converters::class)
abstract class BootyCrateDatabase : RoomDatabase() {

    protected abstract fun itemDao(): ListItemDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun itemGroupDao(): ItemGroupDao
    abstract fun settingsDao(): SettingsDao

    class Callback : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            db.execSQL("UPDATE item " +
                       "SET shoppingListAmount = -1, " +
                       "inShoppingListTrash = 0 " +
                       "WHERE inShoppingListTrash")
            db.execSQL("UPDATE item " +
                       "SET inventoryAmount = -1, " +
                       "inInventoryTrash = 0 " +
                       "WHERE inInventoryTrash")
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.execSQL("INSERT INTO settings DEFAULT VALUES")
            val values = ContentValues(2)
            values.put("name", firstGroupName)
            values.put("isSelected", 1)
            db.insert("itemGroup", 0, values)
            db.addAllItemGroupTriggers()
            db.addAllItemTriggers()
        }
    }

    companion object {
        private const val firstGroupName="BootyCrate"

        private val migration1to2 get() = Migration(1, 2) { db ->
            db.execSQL("""CREATE TABLE itemGroup (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `isSelected` INTEGER NOT NULL DEFAULT 0)""")
            val values = ContentValues(2)
            values.put("name", firstGroupName)
            values.put("isSelected", 1)
            val insertedId = db.insert("itemGroup", 0, values)

            db.execSQL("PRAGMA foreign_keys=off")
            db.execSQL("BEGIN TRANSACTION")
            db.execSQL("""CREATE TABLE IF NOT EXISTS temp_table (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `groupId` INTEGER NOT NULL, 
                    `name` TEXT NOT NULL,
                    `extraInfo` TEXT NOT NULL DEFAULT '' COLLATE NOCASE,
                    `color` INTEGER NOT NULL DEFAULT 0,
                    `isChecked` INTEGER NOT NULL DEFAULT 0,
                    `shoppingListAmount` INTEGER NOT NULL DEFAULT -1,
                    `expandedInShoppingList` INTEGER NOT NULL DEFAULT 0,
                    `selectedInShoppingList` INTEGER NOT NULL DEFAULT 0,
                    `inShoppingListTrash` INTEGER NOT NULL DEFAULT 0,
                    `inventoryAmount` INTEGER NOT NULL DEFAULT -1,
                    `expandedInInventory` INTEGER NOT NULL DEFAULT 0,
                    `selectedInInventory` INTEGER NOT NULL DEFAULT 0,
                    `autoAddToShoppingList` INTEGER NOT NULL DEFAULT 0,
                    `autoAddToShoppingListAmount` INTEGER NOT NULL DEFAULT 1,
                    `inInventoryTrash` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`groupId`) REFERENCES `itemGroup`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
            db.execSQL("""INSERT INTO temp_table(
                              id, groupId, name, extraInfo, color, isChecked, shoppingListAmount,
                              expandedInShoppingList, selectedInShoppingList, inShoppingListTrash,
                              inventoryAmount, expandedInInventory, selectedInInventory,
                              autoAddToShoppingList, autoAddToShoppingListAmount, inInventoryTrash)
                          SELECT id, $insertedId, name, extraInfo, color, isChecked, shoppingListAmount,
                                 expandedInShoppingList, selectedInShoppingList, inShoppingListTrash,
                                 inventoryAmount, expandedInInventory, selectedInInventory,
                                 autoAddToShoppingList, autoAddToShoppingListAmount, inInventoryTrash
                          FROM bootycrate_item;""")
            db.execSQL("DROP TABLE bootycrate_item;")
            db.execSQL("ALTER TABLE temp_table RENAME TO item;")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_groupId` ON item(groupId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_color` ON item(color)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_name` ON item(name)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_shoppingListAmount` ON item(shoppingListAmount)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_inventoryAmount` ON item(inventoryAmount)")
            db.addAllItemTriggers()
            db.execSQL("COMMIT;")
            db.execSQL("PRAGMA foreign_keys=on;")
        }

        private val migration2to3 get() = Migration(2, 3) { db ->
            db.execSQL("""CREATE TABLE IF NOT EXISTS settings (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `multiSelectGroups` INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("INSERT INTO settings DEFAULT VALUES")
            db.addAllItemGroupTriggers()
            db.addAutoDeselectTrigger()
        }

        private val migration3to4 get() = Migration(3, 4) { db ->
            db.execSQL("PRAGMA foreign_keys=off")
            db.execSQL("BEGIN TRANSACTION")

            db.execSQL("""CREATE TABLE IF NOT EXISTS item_temp (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `groupName` TEXT NOT NULL, 
                    `name` TEXT NOT NULL,
                    `extraInfo` TEXT NOT NULL DEFAULT '' COLLATE NOCASE,
                    `colorGroup` INTEGER NOT NULL DEFAULT 0,
                    `isChecked` INTEGER NOT NULL DEFAULT 0,
                    `shoppingListAmount` INTEGER NOT NULL DEFAULT -1,
                    `inShoppingListTrash` INTEGER NOT NULL DEFAULT 0,
                    `inventoryAmount` INTEGER NOT NULL DEFAULT -1,
                    `autoAddToShoppingList` INTEGER NOT NULL DEFAULT 0,
                    `autoAddToShoppingListAmount` INTEGER NOT NULL DEFAULT 1,
                    `inInventoryTrash` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`groupName`) REFERENCES `itemGroup`(`name`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
            db.execSQL("""INSERT INTO item_temp(
                          id, groupName, name, extraInfo, colorGroup,
                          isChecked, shoppingListAmount, inShoppingListTrash,
                          inventoryAmount, autoAddToShoppingList,
                          autoAddToShoppingListAmount, inInventoryTrash)
                          SELECT item.id, itemGroup.name, item.name, extraInfo, color,
                                 isChecked, shoppingListAmount, inShoppingListTrash,
                                 inventoryAmount, autoAddToShoppingList,
                                 autoAddToShoppingListAmount, inInventoryTrash
                          FROM item JOIN itemGroup ON item.groupId = itemGroup.id;""")
            db.execSQL("DROP TABLE item;")
            db.execSQL("ALTER TABLE item_temp RENAME TO item;")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_groupName` ON item(groupName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_colorGroup` ON item(colorGroup)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_name` ON item(name)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_shoppingListAmount` ON item(shoppingListAmount)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_inventoryAmount` ON item(inventoryAmount)")
            db.addAllItemTriggers()

            db.execSQL("""CREATE TABLE IF NOT EXISTS temp_itemGroup (
                `name` TEXT PRIMARY KEY NOT NULL,
                `isSelected` INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("INSERT INTO temp_itemGroup(name, isSelected) " +
                    "SELECT name, isSelected FROM itemGroup")
            db.execSQL("DROP TABLE itemGroup;")
            db.execSQL("ALTER TABLE temp_itemGroup RENAME TO itemGroup;")
            db.addAllItemGroupTriggers()

            db.execSQL("COMMIT;")
            db.execSQL("PRAGMA foreign_keys=on;")
        }

        val allMigrations get() = arrayOf(migration1to2, migration2to3, migration3to4)

        private fun SupportSQLiteDatabase.addEnsureAtLeastOneGroupTrigger() =
            execSQL("""CREATE TRIGGER IF NOT EXISTS `ensure_at_least_one_group`
                       BEFORE DELETE ON itemGroup WHEN (SELECT count(*) FROM itemGroup) == 1
                       BEGIN SELECT RAISE(IGNORE); END;""")

        private fun SupportSQLiteDatabase.addEnsureAtLeastOneSelectedGroupTriggers() {
            execSQL("""CREATE TRIGGER IF NOT EXISTS `ensure_at_least_one_selected_group_1`
                       AFTER DELETE ON itemGroup
                       WHEN (SELECT COUNT(isSelected) FROM itemGroup WHERE isSelected) == 0
                       BEGIN UPDATE itemGroup SET isSelected = 1
                             WHERE id = (SELECT id FROM itemGroup LIMIT 1);
                       END;""")
            execSQL("""CREATE TRIGGER IF NOT EXISTS `ensure_at_least_one_selected_group_2`
                       BEFORE UPDATE OF isSelected ON itemGroup
                       WHEN new.isSelected == 0 AND old.isSelected == 1
                       AND (SELECT COUNT(isSelected) FROM itemGroup WHERE isSelected) == 1
                       BEGIN SELECT RAISE(IGNORE); END;""")
        }

        private fun SupportSQLiteDatabase.addEnforceSingleSelectGroupsTriggers() {
            execSQL("""CREATE TRIGGER IF NOT EXISTS `enforce_single_select_groups_1`
                       AFTER UPDATE OF isSelected ON itemGroup
                       WHEN new.isSelected == 1
                       AND (SELECT multiSelectGroups FROM settings LIMIT 1) == 0
                       BEGIN UPDATE itemGroup SET isSelected = 0
                             WHERE id != new.id; END;""")

            execSQL("""CREATE TRIGGER IF NOT EXISTS `enforce_single_select_groups_2`
                       AFTER UPDATE OF multiSelectGroups ON settings
                       WHEN new.multiSelectGroups == 0
                       BEGIN UPDATE itemGroup SET isSelected = 0
                             WHERE itemGroup.id != (SELECT id FROM itemGroup
                                                    WHERE isSelected LIMIT 1);
                       END;""")

            execSQL("""CREATE TRIGGER IF NOT EXISTS `enforce_single_select_groups_3`
                       AFTER INSERT ON itemGroup
                       WHEN (SELECT COUNT(*) FROM itemGroup WHERE isSelected) > 1
                       AND (select multiSelectGroups FROM settings LIMIT 1) == 0
                       BEGIN UPDATE itemGroup SET isSelected = 0
                             WHERE itemGroup.id != (SELECT id FROM itemGroup
                                                    WHERE isSelected LIMIT 1);
                       END;""")
        }

        private fun SupportSQLiteDatabase.addAllItemGroupTriggers() = apply {
            addEnsureAtLeastOneGroupTrigger()
            addEnsureAtLeastOneSelectedGroupTriggers()
            addEnforceSingleSelectGroupsTriggers()
        }

        private fun SupportSQLiteDatabase.addAutoDeselectTrigger() = execSQL("""
            CREATE TRIGGER IF NOT EXISTS `auto_deselect_invisible_items`
            AFTER UPDATE OF isSelected ON itemGroup WHEN new.isSelected = 0
            BEGIN UPDATE item SET selectedInInventory = 0,
                                  selectedInShoppingList = 0
            WHERE groupId == new.id; END;""")

        /** The auto_delete_items trigger will remove an item from the database when
         * its shoppingListAmount and inventoryAmount fields both are equal to -1. */
        private fun SupportSQLiteDatabase.addAutoDeleteTrigger() = execSQL("""
            CREATE TRIGGER IF NOT EXISTS `auto_delete_items`
            AFTER UPDATE OF inventoryAmount, shoppingListAmount ON item
            WHEN new.shoppingListAmount == -1 AND new.inventoryAmount == -1
            BEGIN DELETE FROM item WHERE id = new.id; END""")

        /** The auto add to shopping list triggers will execute the auto add to shopping
         * list feature of inventory items if an item's amount falls below its
         * autoAddToShoppingListAmount and its autoAddToShoppingList field is true */
        private fun SupportSQLiteDatabase.addAutoAddToShoppingListTriggers() {
            execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_amount_update`
                       AFTER UPDATE OF inventoryAmount ON item
                       WHEN new.autoAddToShoppingList
                       AND new.inventoryAmount < new.autoAddToShoppingListAmount
                       BEGIN $updateShoppingListAmount; END""")
            execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_auto_add_update`
                       AFTER UPDATE OF autoAddToShoppingList ON item
                       WHEN new.autoAddToShoppingList
                       AND new.inventoryAmount < new.autoAddToShoppingListAmount
                       BEGIN $updateShoppingListAmount; END""")
            execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_auto_add_amount_update`
                       AFTER UPDATE OF autoAddToShoppingListAmount ON item
                       WHEN new.autoAddToShoppingList
                       AND new.inventoryAmount < new.autoAddToShoppingListAmount
                       BEGIN $updateShoppingListAmount; END""")
            execSQL("""CREATE TRIGGER IF NOT EXISTS `check_auto_add_after_insertion`
                       AFTER INSERT ON item
                       WHEN new.autoAddToShoppingList
                       AND new.inventoryAmount BETWEEN 0
                       AND new.autoAddToShoppingListAmount - 1
                       BEGIN $updateShoppingListAmount; END""")
        }

        /* Unfortunately SQLite's limitation of not being able to use common table
         * expressions in triggers makes the following less readable than it should
         * be. The query below updates the shopping list amount of the ListItem to
         * be the greater of its current amount (if it is already on the shopping
         * list) or the inventory item it is based on's addToShoppingListAmount
         * minus its current amount). */
        private const val updateShoppingListAmount =
            """UPDATE item
               SET inShoppingListTrash = 0, shoppingListAmount =
                   CASE WHEN shoppingListAmount >
                       (SELECT new.autoAddToShoppingListAmount - new.inventoryAmount)
                   THEN shoppingListAmount
                   ELSE (SELECT new.autoAddToShoppingListAmount - new.inventoryAmount) END
               WHERE id = new.id"""

        private fun SupportSQLiteDatabase.addAllItemTriggers() = apply {
            // The auto deselect items triggers is no longer used as of db version 4
            addAutoDeleteTrigger()
            addAutoAddToShoppingListTriggers()
        }
    }

    //TODO: Add typed exceptions
    fun backup(context: Context, backupUri: Uri) {
        val databasePath = openHelper.readableDatabase.path ?:
            throw Exception("The database file could not be opened for reading")
        openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
        close()
        val writer = context.contentResolver.openOutputStream(backupUri) ?:
            throw Exception("The backup uri ${backupUri.path} could not be opened for writing")
        writer.write(File(databasePath).readBytes())
        writer.close()
    }

    fun importBackup(
        context: Context,
        backupUri: Uri,
        overwriteExistingDb: Boolean
    ) {
        val tempDbName = "tempDb"
        val inputStream = context.contentResolver.openInputStream(backupUri) ?:
            throw Exception("The backup uri ${backupUri.path} could not be opened for reading")
        val tempDbFile = inputStream.use { input ->
            // Room can only open databases in the app's database directory,
            // making it necessary to copy the imported database here first.
            context.getDatabasePath(tempDbName).apply {
                writeBytes(input.readBytes())
            }
        }
        val importedDb = Room.databaseBuilder(
                context, BootyCrateDatabase::class.java, tempDbName
            ).allowMainThreadQueries()
            .createFromFile(tempDbFile)
            .addMigrations(*allMigrations)
            .build()
        val importedItemGroups = try { importedDb.itemGroupDao().getAllNow() }
                                 catch(e: IllegalStateException) { emptyList() }
        val importedItems = try { importedDb.itemDao().getAllNow() }
                            catch(e: IllegalStateException) { emptyList() }
        importedDb.close()
        tempDbFile.delete()

        if (importedItemGroups.isEmpty() || importedItems.isEmpty())
            throw Exception("The imported database was empty")
//            themedAlertDialogBuilder(context)
//                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
//                .setMessage(R.string.invalid_imported_db_error_message)
//                .setTitle(R.string.error).show()
        CoroutineScope(Dispatchers.IO).launch {
            if (overwriteExistingDb) {
                openHelper.writableDatabase.execSQL(
                    "DROP TRIGGER `ensure_at_least_one_group`")
                itemGroupDao().deleteAll()
                itemGroupDao().add(importedItemGroups)
                openHelper.writableDatabase.addEnsureAtLeastOneGroupTrigger()
                itemDao().deleteAll()
            } else {
                // Because the name field is the primary key for the itemGroup table,
                // we need to skip adding any imported item groups whose name is already
                // used in the existing database. Items in the imported database that
                // were in the duplicate name item group will automatically be a part
                // of the existing db's same named item group.
                val existingGroupNames = itemGroupDao()
                    .getAllNow().map { it.name }
                val filteredNames = importedItemGroups
                    .filter { it.name !in existingGroupNames }
                itemGroupDao().add(filteredNames)
            }
            itemDao().add(importedItems)
        }
    }
}