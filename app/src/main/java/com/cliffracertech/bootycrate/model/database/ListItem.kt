/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import android.content.Context
import androidx.room.*
import com.cliffracertech.bootycrate.R

/** DatabaseListItem describes the entities stored in the item table. */
@Entity(tableName = "item",
        foreignKeys = [ForeignKey(entity = DatabaseItemGroup::class,
                                  parentColumns=["id"],
                                  childColumns=["groupId"],
                                  onDelete=ForeignKey.CASCADE)])
class DatabaseListItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Long = 0,
    @ColumnInfo(name="groupId", index = true)
    var groupId: Long = 0,
    @ColumnInfo(name="name", collate = ColumnInfo.NOCASE, index = true)
    var name: String = "",
    @ColumnInfo(name="extraInfo", defaultValue="", collate = ColumnInfo.NOCASE)
    var extraInfo: String = "",
    @ColumnInfo(name="color", defaultValue="0", index = true)
    var color: Int = 0,

    // ShoppingListItem fields
    @ColumnInfo(name="isChecked", defaultValue="0")
    var isChecked: Boolean = false,
    @ColumnInfo(name="shoppingListAmount", defaultValue="-1", index = true)
    var shoppingListAmount: Int = -1,
    @ColumnInfo(name="expandedInShoppingList", defaultValue="0")
    var expandedInShoppingList: Boolean = false,
    @ColumnInfo(name="selectedInShoppingList", defaultValue="0")
    var selectedInShoppingList: Boolean = false,
    @ColumnInfo(name="inShoppingListTrash", defaultValue="0")
    var inShoppingListTrash: Boolean = false,

    // InventoryItem fields
    @ColumnInfo(name="inventoryAmount", defaultValue="-1", index = true)
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
    init { color.coerceIn(ListItem.Colors.indices) }

    /** An interface for objects to provide a way to convert themselves into a DatabaseListItem. */
    interface Convertible {
        /** Return the convertible as a DatabaseListItem,
         *  with its itemGroupId field set to the provided id. **/
        fun toDbListItem(groupId: Long): DatabaseListItem
    }

    override fun toString() ="""
id = $id
groupId = $groupId
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

/** An abstract class that mirrors DatabaseListItem, but only contains
 * the fields necessary for a visual representation of the object. Subclasses
 * should also add any additional required fields and provide an implementation
 * of toDbListItem that will return a DatabaseListItem representation of the
 * object. */
abstract class ListItem(
    var id: Long = 0,
    var name: String,
    var extraInfo: String = "",
    var color: Int = 0,
    var amount: Int = 1,
    var isExpanded: Boolean = false,
    var isSelected: Boolean = false,
    var isLinked: Boolean = false,
) : DatabaseListItem.Convertible {

    // For a user-facing string representation of the object
    fun toUserFacingString() = "${amount}x $name" + (if (extraInfo.isNotBlank()) ", $extraInfo" else "")

    companion object {
        val Colors: List<Int> get() = _Colors.asList()
        val ColorDescriptions: List<String> get() = _ColorDescriptions.asList()
        private lateinit var _Colors: IntArray
        private lateinit var _ColorDescriptions: Array<String>

        fun initColors(context: Context) {
            _Colors = context.resources.getIntArray(R.array.list_item_colors)
            _ColorDescriptions = context.resources.getStringArray(R.array.list_item_color_descriptions)
        }
    }

    enum class Sort { Color, NameAsc, NameDesc, AmountAsc, AmountDesc;
        companion object {
            fun fromString(string: String?) =
                    if (string == null) Color
                    else try { valueOf(string) }
                    catch(e: IllegalArgumentException) { Color }
        }
    }
}

/** A ListItem subclass that provides an implementation of toDbListItem
 * and adds the isChecked field to mirror the DatabaseListItem field. */
class ShoppingListItem(
    id: Long = 0,
    name: String,
    extraInfo: String = "",
    color: Int = 0,
    amount: Int = 1,
    isExpanded: Boolean = false,
    isSelected: Boolean = false,
    isLinked: Boolean = false,
    var isChecked: Boolean = false
): ListItem(id, name, extraInfo, color, amount, isExpanded, isSelected, isLinked) {

    /** The enum class Field identifies user facing fields
     * that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount,
                       IsExpanded, IsSelected, IsLinked, IsChecked }

    override fun toDbListItem(groupId: Long) = DatabaseListItem(
        id, groupId, name, extraInfo, color,
        isChecked = isChecked,
        shoppingListAmount = amount,
        expandedInShoppingList = isExpanded,
        selectedInShoppingList = isSelected)
}

/** A ListItem subclass that provides an implementation of toDbListItem
 * and adds the autoAddToShoppingList and autoAddToShoppingListAmount fields to
 * mirror the DatabaseListItem fields. */
class InventoryItem(
    id: Long = 0,
    name: String,
    extraInfo: String = "",
    color: Int = 0,
    amount: Int = 0,
    isExpanded: Boolean = false,
    isSelected: Boolean = false,
    isLinked: Boolean = false,
    var autoAddToShoppingList: Boolean = false,
    var autoAddToShoppingListAmount: Int = 1
): ListItem(id, name, extraInfo, color, amount, isExpanded, isSelected, isLinked) {

    /** The enum class Field identifies user facing fields
     * that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount,
                       IsExpanded, IsSelected, IsLinked,
                       AutoAddToShoppingList,
                       AutoAddToShoppingListAmount }

    override fun toDbListItem(groupId: Long) = DatabaseListItem(
        id, groupId, name, extraInfo, color,
        inventoryAmount = amount,
        expandedInInventory = isExpanded,
        selectedInInventory = isSelected,
        autoAddToShoppingList = autoAddToShoppingList,
        autoAddToShoppingListAmount = autoAddToShoppingListAmount)
}
