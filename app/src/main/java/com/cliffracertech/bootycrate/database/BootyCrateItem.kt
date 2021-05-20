/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cliffracertech.bootycrate.R

@Entity(tableName = "item")
abstract class BootyCrateItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")                         var id: Long = 0,
    @ColumnInfo(name="name")                       var name: String = "",
    @ColumnInfo(name="extraInfo", defaultValue="") var extraInfo: String = "",
    @ColumnInfo(name="color", defaultValue="0")    var color: Int = 0,

    // ShoppingListItem fields
    @ColumnInfo(name="checked", defaultValue="0")
    var checked: Boolean = false,
    @ColumnInfo(name="shoppingListAmount", defaultValue="0")
    var shoppingListAmount: Int = 0,
    @ColumnInfo(name="expandedInShoppingList", defaultValue="0")
    var expandedInShoppingList: Boolean = false,
    @ColumnInfo(name="selectedInShoppingList", defaultValue="0")
    var selectedInShoppingList: Boolean = false,
    @ColumnInfo(name="inShoppingListTrash", defaultValue="0")
    var inShoppingListTrash: Boolean = false,

    // InventoryItem fields
    @ColumnInfo(name="inventoryAmount", defaultValue="0")
    var inventoryAmount: Int = 0,
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
    abstract var amount: Int
    abstract var expanded: Boolean
    abstract var selected: Boolean

    init { color.coerceIn(Colors.indices) }

//    override fun equals(other: Any?): Boolean {
//        if (other === this) return true
//        if (other == null || other !is BootyCrateItem) return false
//        return this.id == other.id &&
//               this.name == other.name &&
//               this.extraInfo == other.extraInfo &&
//               this.color == other.color
//    }

    override fun toString() ="""
id = $id
name = $name
extraInfo = $extraInfo
color = $color
checked = $checked
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

    // For a user-facing string representation of the object
    open fun toUserFacingString() = "${amount}x $name" + (if (extraInfo.isNotBlank()) ", $extraInfo" else "")

    enum class Sort { Color, NameAsc, NameDesc, AmountAsc, AmountDesc;
        companion object {
            fun fromString(string: String?): Sort =
                if (string == null) Color
                else try { valueOf(string) }
                     catch(e: IllegalArgumentException) { Color }
        }
    }

    companion object {
        val Colors: List<Int> get() = _Colors
        private lateinit var _Colors: List<Int>

        fun initColors(context: Context) {
            _Colors = context.resources.getIntArray(R.array.bootycrate_item_colors).asList()
        }
    }
}

/** A BootyCrateItem subclass with a constructor that only
 * requires the fields relevant to items on the shopping list. */
class ShoppingListItem(
    id: Long = 0,
    name: String = "",
    extraInfo: String = "",
    color: Int = 0,
    checked: Boolean = false,
    amount: Int = 1,
    expanded: Boolean = false,
    selected: Boolean = false,
    inventoryAmount: Int = -1
) : BootyCrateItem(id, name, extraInfo, color,
                   checked = checked,
                   shoppingListAmount = amount,
                   expandedInShoppingList = expanded,
                   selectedInShoppingList = selected,
                   inventoryAmount = inventoryAmount)
{
    override var amount get() = shoppingListAmount
                        set(value) { shoppingListAmount = value }
    override var expanded get() = expandedInShoppingList
                          set(value) { expandedInShoppingList = value }
    override var selected get() = selectedInShoppingList
                          set(value) { selectedInShoppingList = value }

    /** The enum class Field identifies user facing fields
     * that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount,
                       Expanded, Selected, IsChecked }
}

/** A BootyCrateItem subclass with a constructor that only
 * requires the fields relevant to items in the inventory */
class InventoryItem(
    id: Long = 0,
    name: String = "",
    extraInfo: String = "",
    color: Int = 0,
    amount: Int = 0,
    expanded: Boolean = false,
    selected: Boolean = false,
    autoAddToShoppingList: Boolean = false,
    autoAddToShoppingListAmount: Int = 1,
    shoppingListAmount: Int = -1,
) : BootyCrateItem(id, name, extraInfo, color,
                   inventoryAmount = amount,
                   expandedInInventory = expanded,
                   selectedInInventory = selected,
                   autoAddToShoppingList = autoAddToShoppingList,
                   autoAddToShoppingListAmount = autoAddToShoppingListAmount,
                   shoppingListAmount = shoppingListAmount)
{
    override var amount get() = inventoryAmount
                        set(value) { inventoryAmount = value }
    override var expanded get() = expandedInInventory
                          set(value) { expandedInInventory = value }
    override var selected get() = selectedInInventory
                          set(value) { selectedInInventory = value }

    /** The enum class Field identifies user facing fields
     * that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount,
                       Expanded, Selected, AutoAddToShoppingList,
                       AutoAddToShoppingListAmount }
}
