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
    @ColumnInfo(name="id")                            var id: Long = 0,
    @ColumnInfo(name="name")                          var name: String = "",
    @ColumnInfo(name="extraInfo", defaultValue="")    var extraInfo: String = "",
    @ColumnInfo(name="isChecked", defaultValue="0")   var isChecked: Boolean = false,
    @ColumnInfo(name="color", defaultValue="0")       var color: Int = 0,
    @ColumnInfo(name="shoppingListAmount", defaultValue="0") var shoppingListAmount: Int = 0,
    @ColumnInfo(name="inventoryAmount", defaultValue="0") var inventoryAmount: Int = 0,
    @ColumnInfo(name="inInventory", defaultValue="0") var inInventory: Boolean = false,
    @ColumnInfo(name="isExpanded", defaultValue="0")  var isExpanded: Boolean = false,
    @ColumnInfo(name="isSelected", defaultValue="0")  var isSelected: Boolean = false,
    @ColumnInfo(name="autoAddToShoppingList", defaultValue="0") var autoAddToShoppingList: Boolean = false,
    @ColumnInfo(name="autoAddToShoppingListAmount", defaultValue="1") var autoAddToShoppingListAmount: Int = 1,
    @ColumnInfo(name="inTrash", defaultValue="0")     var inTrash: Boolean = false
) {
    abstract var amount: Int

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
isChecked = $isChecked
color = $color
shoppingListAmount = $shoppingListAmount
inventoryAmount = $inventoryAmount
inInventory = $inInventory
autoAddToShoppingList = $autoAddToShoppingList
autoAddToShoppingListAmount = $autoAddToShoppingListAmount
isExpanded = $isExpanded
isSelected = $isSelected
inTrash = $inTrash"""

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

/** A Room entity that represents a shopping list item in the user's shopping list. */
class ShoppingListItem(
    id: Long = 0,
    isChecked: Boolean = false,
    color: Int = 0,
    name: String = "",
    extraInfo: String = "",
    shoppingListAmount: Int = 0,
    inventoryAmount: Int = 0,
    inInventory: Boolean = false,
    isExpanded: Boolean = false,
    isSelected: Boolean = false,
    autoAddToShoppingList: Boolean = false,
    autoAddToShoppingListAmount: Int = 1,
    inTrash: Boolean = false,
) : BootyCrateItem(id, name, extraInfo, isChecked, color, shoppingListAmount,
                   inventoryAmount, inInventory, isExpanded, isSelected,
                   autoAddToShoppingList, autoAddToShoppingListAmount, inTrash)
{
    override var amount get() = shoppingListAmount
                        set(value) { shoppingListAmount = value }

    /** The enum class Field identifies user facing fields
     * that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount,
                       IsExpanded, IsSelected, IsChecked }
}

/** A Room entity that represents an inventory item in the user's inventory. */
class InventoryItem(
    id: Long = 0,
    isChecked: Boolean = false,
    color: Int = 0,
    name: String = "",
    extraInfo: String = "",
    shoppingListAmount: Int = 0,
    inventoryAmount: Int = 0,
    inInventory: Boolean = false,
    isExpanded: Boolean = false,
    isSelected: Boolean = false,
    autoAddToShoppingList: Boolean = false,
    autoAddToShoppingListAmount: Int = 1,
    inTrash: Boolean = false,
) : BootyCrateItem(id, name, extraInfo, isChecked, color, shoppingListAmount,
                   inventoryAmount, inInventory, isExpanded, isSelected,
                   autoAddToShoppingList, autoAddToShoppingListAmount, inTrash)
{
    override var amount get() = inventoryAmount
                        set(value) { inventoryAmount = value }

    /** The enum class Field identifies user facing fields that are potentially
     * editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount, IsExpanded,
                       IsSelected, AutoAddToShoppingList,
                       AutoAddToShoppingListAmount }
}
