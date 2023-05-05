/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.room.*
import com.cliffracertech.bootycrate.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** DatabaseListItem describes the entities stored in the item table. */
@Entity(tableName = "item",
        foreignKeys = [ForeignKey(
            entity = DatabaseItemGroup::class,
            parentColumns=["name"],
            childColumns=["groupName"],
            onDelete=ForeignKey.CASCADE)])
class DatabaseListItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    val id: Long = 0,
    @ColumnInfo(name="groupName", index = true)
    val groupName: String,
    @ColumnInfo(name="name", collate = ColumnInfo.NOCASE, index = true)
    val name: String = "",
    @ColumnInfo(name="extraInfo", defaultValue="", collate = ColumnInfo.NOCASE)
    val extraInfo: String = "",
    @ColumnInfo(name="colorGroup", defaultValue="0", index = true)
    val colorGroup: ListItem.ColorGroup = ListItem.ColorGroup.values().first(),

    // ShoppingListItem fields
    @ColumnInfo(name="isChecked", defaultValue="0")
    val isChecked: Boolean = false,
    @ColumnInfo(name="shoppingListAmount", defaultValue="-1", index = true)
    val shoppingListAmount: Int = -1,
    @ColumnInfo(name="inShoppingListTrash", defaultValue="0")
    val inShoppingListTrash: Boolean = false,

    // InventoryItem fields
    @ColumnInfo(name="inventoryAmount", defaultValue="-1", index = true)
    val inventoryAmount: Int = -1,
    @ColumnInfo(name="autoAddToShoppingList", defaultValue="0")
    val autoAddToShoppingList: Boolean = false,
    @ColumnInfo(name="autoAddToShoppingListAmount", defaultValue="1")
    val autoAddToShoppingListAmount: Int = 1,
    @ColumnInfo(name="inInventoryTrash", defaultValue="0")
    val inInventoryTrash: Boolean = false
) {
    override fun toString() ="""
        id = $id
        groupName = $groupName
        name = $name
        extraInfo = $extraInfo
        colorGroup = $colorGroup
        checked = $isChecked
        shoppingListAmount = $shoppingListAmount
        inShoppingListTrash = $inShoppingListTrash
        inventoryAmount = $inventoryAmount
        autoAddToShoppingList = $autoAddToShoppingList
        autoAddToShoppingListAmount = $autoAddToShoppingListAmount
        inInventoryTrash = $inInventoryTrash
    """.trimIndent()

    class Converters {
        @TypeConverter fun toColorGroup(index: Int) =
            ListItem.ColorGroup.values().getOrElse(index) {
                ListItem.ColorGroup.values().first()
            }
        @TypeConverter fun fromColorGroup(colorGroup: ListItem.ColorGroup) =
            colorGroup.ordinal
    }

}

/** An abstract class that mirrors [DatabaseListItem], but omits the
 * fields not necessary for a visual representation of the object,
 * and adds the calculated [linked] field that represents whether
 * or not the item is linked to a similar item on another list. */
// Meta note: A shopping list item being linked means it is also in the inventory
//            An inventory item being linked means it is also on the shopping list
abstract class ListItem(
    val id: Long = 0,
    val name: String,
    val extraInfo: String = "",
    val colorGroup: ColorGroup = ColorGroup.values().first(),
    val amount: Int = 1,
    val linked: Boolean = false,
) {
    /** Return a [String] that describes the name, extra info, and amount of an item. */
    fun toUserFacingString() = "${amount}x $name" + (if (extraInfo.isNotBlank()) ", $extraInfo" else "")

    /** The possible categories that can be used to group items for convenience. */
    enum class ColorGroup {
        Red, Orange, Yellow, LimeGreen, Green, Teal,
        Cyan, Blue, Violet, Magenta, Pink, Gray;

        /** Return the graphical [Color] that the [ColorGroup] uses to identify itself. */
        fun toColor() = when (this) {
            Red ->       Color(214, 92, 92)
            Orange ->    Color(214, 153, 92)
            Yellow ->    Color(214, 214, 92)
            LimeGreen -> Color(151, 207, 61)
            Green ->     Color(63, 186, 63)
            Teal ->      Color(83, 227, 147)
            Cyan ->      Color(44, 196, 218)
            Blue ->      Color(63, 126, 221)
            Violet ->    Color(131, 89, 230)
            Magenta ->   Color(177, 65, 218)
            Pink ->      Color(219, 112, 166)
            Gray ->      Color(128, 128, 128)
        }

        /** Return the resource id of the string that describes the color category. */
        fun toStringResId() = when (this) {
            Red ->       R.string.item_color_red_description
            Orange ->    R.string.item_color_orange_description
            Yellow ->    R.string.item_color_yellow_description
            LimeGreen -> R.string.item_color_lime_green_description
            Green ->     R.string.item_color_green_description
            Teal ->      R.string.item_color_teal_description
            Cyan ->      R.string.item_color_cyan_description
            Blue ->      R.string.item_color_blue_description
            Violet ->    R.string.item_color_violet_description
            Magenta ->   R.string.item_color_magenta_description
            Pink ->      R.string.item_color_pink_description
            Gray ->      R.string.item_color_gray_description
        }

        companion object {
            /** Return a list containing the descriptions of each [ColorGroup]. */
            @Composable fun descriptions(): ImmutableList<String> {
                val context = LocalContext.current
                return remember {
                    values().map { context.getString(it.toStringResId()) }
                            .toImmutableList()
                }
            }

            /** Return all of the color categories as the [Color]s that identify them. */
            @Composable fun colors() = remember {
                values().map(ColorGroup::toColor)
                        .toImmutableList()
            }
        }
    }

    enum class Sort { Color, NameAsc, NameDesc, AmountAsc, AmountDesc;

        fun toString(context: Context) = when(this) {
            Color ->      context.getString(R.string.color_description)
            NameAsc ->    context.getString(R.string.name_ascending_description)
            NameDesc ->   context.getString(R.string.name_descending_description)
            AmountAsc ->  context.getString(R.string.amount_ascending_description)
            AmountDesc -> context.getString(R.string.amount_descending_description)
        }

        companion object {
            fun stringValues(context: Context) =
                enumValues<Sort>().map { it.toString(context) }
        }
    }
}

/** A [ListItem] subclass that provides the method [toDbListItem] to convert
 * itself to a [DatabaseListItem], and adds the isChecked field to mirror the
 * [DatabaseListItem] field. */
class ShoppingListItem(
    id: Long = 0,
    name: String,
    extraInfo: String = "",
    colorGroup: ColorGroup = ColorGroup.values().first(),
    amount: Int = 1,
    linked: Boolean = false,
    val checked: Boolean = false
): ListItem(id, name, extraInfo, colorGroup, amount, linked) {
    fun toDbListItem(groupName: String) = DatabaseListItem(
        id, groupName, name, extraInfo, colorGroup, checked, amount)
}

/** A [ListItem] subclass that provides the method [toDbListItem] to convert
 * itself to a [DatabaseListItem], and adds the [autoAddToShoppingList] and
 * [autoAddToShoppingListAmount] fields to mirror the [DatabaseListItem] fields. */
class InventoryItem(
    id: Long = 0,
    name: String,
    extraInfo: String = "",
    colorGroup: ColorGroup = ColorGroup.values().first(),
    amount: Int = 0,
    linked: Boolean = false,
    val autoAddToShoppingList: Boolean = false,
    val autoAddToShoppingListAmount: Int = 1
): ListItem(id, name, extraInfo, colorGroup, amount, linked) {
    fun toDbListItem(groupName: String) = DatabaseListItem(
        id, groupName, name, extraInfo, colorGroup,
        inventoryAmount = amount,
        autoAddToShoppingList = autoAddToShoppingList,
        autoAddToShoppingListAmount = autoAddToShoppingListAmount)
}
