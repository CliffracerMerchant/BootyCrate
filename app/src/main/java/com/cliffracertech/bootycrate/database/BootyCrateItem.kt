/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.cliffracertech.bootycrate.R

@Entity(tableName = "inventory")
class BootyCrateInventory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")   var id: Long = 0,
    @ColumnInfo(name="name") var name: String = "",
)

/** DatabaseBootyCrateItem describes the entities stored in the bootycrate_item table. */
@Entity(tableName = "bootycrate_item",
        foreignKeys = [ForeignKey(entity = BootyCrateInventory::class,
                                  parentColumns=["id"],
                                  childColumns=["inventoryId"],
                                  onDelete=ForeignKey.CASCADE)])
class DatabaseBootyCrateItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")                         var id: Long = 0,
    @ColumnInfo(name="inventoryId")                var inventoryId: Long = 0,
    @ColumnInfo(name="name")                       var name: String = "",
    @ColumnInfo(name="extraInfo", defaultValue="") var extraInfo: String = "",
    @ColumnInfo(name="color", defaultValue="0")    var color: Int = 0,

    // ShoppingListItem fields
    @ColumnInfo(name="isChecked", defaultValue="0")
    var isChecked: Boolean = false,
    @ColumnInfo(name="shoppingListAmount", defaultValue="-1")
    var shoppingListAmount: Int = -1,
    @ColumnInfo(name="expandedInShoppingList", defaultValue="0")
    var expandedInShoppingList: Boolean = false,
    @ColumnInfo(name="selectedInShoppingList", defaultValue="0")
    var selectedInShoppingList: Boolean = false,
    @ColumnInfo(name="inShoppingListTrash", defaultValue="0")
    var inShoppingListTrash: Boolean = false,

    // InventoryItem fields
    @ColumnInfo(name="inventoryAmount", defaultValue="-1")
    var inventoryAmount: Int = -1,
    @ColumnInfo(name="expandedInInventory", defaultValue="0")
    var expandedInInventory: Boolean = false,
    @ColumnInfo(name="selectedInInventory", defaultValue="0")
    var selectedInInventory: Boolean = false,
    @ColumnInfo(name="autoAddToShoppingList", defaultValue="0")
    var autoAddToShoppingList: Boolean = false,
    @ColumnInfo(name="autoAddToShoppingListAmount", defaultValue="1")
    var autoAddToShoppingListAmount: Int = 1,
    @ColumnInfo(name="inInventoryTrash", defaultValue="0")
    var inInventoryTrash: Boolean = false
) {
    init { color.coerceIn(BootyCrateItem.Colors.indices) }

    // An interface for objects to provide a way to convert themselves into a DataBaseBootyCrateItem
    interface Convertible {
        fun toDbBootyCrateItem(): DatabaseBootyCrateItem
    }

    override fun toString() ="""
id = $id
inventoryId = $inventoryId
name = $name
extraInfo = $extraInfo
color = $color
checked = $isChecked
shoppingListAmount = $shoppingListAmount
expandedInShoppingList = $expandedInShoppingList
selectedInShoppingList = $selectedInShoppingList
inShoppingListTrash = $inShoppingListTrash
inventoryAmount = $inventoryAmount
expandedInInventory = $expandedInInventory
selectedInInventory = $selectedInInventory
autoAddToShoppingList = $autoAddToShoppingList
autoAddToShoppingListAmount = $autoAddToShoppingListAmount
inInventoryTrash = $inInventoryTrash"""
}

/** An abstract class that mirrors DatabaseBootyCrateItem, but only contains
 * the fields necessary for a visual representation of the object. Subclasses
 * should also add any additional required fields and provide an implementation
 * of toDbBootyCrateItem that will return a DatabaseBootyCrateItem representation
 * of the object. */
abstract class BootyCrateItem(
    var id: Long = 0,
    var inventoryId: Long = 0,
    var name: String = "",
    var extraInfo: String = "",
    var color: Int = 0,
    var amount: Int = 1,
    var isExpanded: Boolean = false,
    var isSelected: Boolean = false,
    var isLinked: Boolean = false,
) : DatabaseBootyCrateItem.Convertible {

    // For a user-facing string representation of the object
    fun toUserFacingString() = "${amount}x $name" + (if (extraInfo.isNotBlank()) ", $extraInfo" else "")

    companion object {
        val Colors: List<Int> get() = _Colors.asList()
        val ColorDescriptions: List<String> get() = _ColorDescriptions.asList()
        private lateinit var _Colors: IntArray
        private lateinit var _ColorDescriptions: Array<String>

        fun initColors(context: Context) {
            _Colors = context.resources.getIntArray(R.array.bootycrate_item_colors)
            _ColorDescriptions = context.resources.getStringArray(R.array.bootycrate_item_color_descriptions)
        }
    }
}

/** A BootyCrateItem subclass that provides an implementation of toDbBootyCrateItem
 * and adds the isChecked field to mirror the DatabaseBootyCrateItem field. */
class ShoppingListItem(
    id: Long = 0,
    inventoryId: Long = 0,
    name: String = "",
    extraInfo: String = "",
    color: Int = 0,
    amount: Int = 1,
    isExpanded: Boolean = false,
    isSelected: Boolean = false,
    isLinked: Boolean = false,
    var isChecked: Boolean = false
): BootyCrateItem(id, inventoryId, name, extraInfo, color, amount, isExpanded, isSelected, isLinked) {

    /** The enum class Field identifies user facing fields
     * that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount,
                       IsExpanded, IsSelected, IsLinked, IsChecked }

    override fun toDbBootyCrateItem() = DatabaseBootyCrateItem(
        id, inventoryId, name, extraInfo, color,
        isChecked = isChecked,
        shoppingListAmount = amount,
        expandedInShoppingList = isExpanded,
        selectedInShoppingList = isSelected)
}

/** A BootyCrateItem subclass that provides an implementation of toDbBootyCrateItem
 * and adds the autoAddToShoppingList and autoAddToShoppingListAmount fields to
 * mirror the DatabaseBootyCrateItem fields. */
class InventoryItem(
    id: Long = 0,
    inventoryId: Long = 0,
    name: String = "",
    extraInfo: String = "",
    color: Int = 0,
    amount: Int = 0,
    isExpanded: Boolean = false,
    isSelected: Boolean = false,
    isLinked: Boolean = false,
    var autoAddToShoppingList: Boolean = false,
    var autoAddToShoppingListAmount: Int = 1
): BootyCrateItem(id, inventoryId, name, extraInfo, color, amount, isExpanded, isSelected, isLinked) {

    /** The enum class Field identifies user facing fields
     * that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount,
                       IsExpanded, IsSelected, IsLinked,
                       AutoAddToShoppingList,
                       AutoAddToShoppingListAmount }

    override fun toDbBootyCrateItem() = DatabaseBootyCrateItem(
        id, inventoryId, name, extraInfo, color,
        inventoryAmount = amount,
        expandedInInventory = isExpanded,
        selectedInInventory = isSelected,
        autoAddToShoppingList = autoAddToShoppingList,
        autoAddToShoppingListAmount = autoAddToShoppingListAmount)
}
