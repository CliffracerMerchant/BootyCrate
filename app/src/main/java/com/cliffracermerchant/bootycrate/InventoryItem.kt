/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity

/** A Room entity that represents an inventory item in the user's inventory. */
@Entity(tableName = "inventory_item")
class InventoryItem(
    id: Long = 0,
    name: String = "",
    extraInfo: String = "",
    color: Int = 0,
    amount: Int = 1,
    inTrash: Boolean = false,
    @ColumnInfo(name = "addToShoppingList", defaultValue = "0")        var addToShoppingList: Boolean = false,
    @ColumnInfo(name = "addToShoppingListTrigger", defaultValue = "1") var addToShoppingListTrigger: Int = 1
) : ViewModelItem(id, name, extraInfo, color, amount, inTrash) {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is InventoryItem) return false
        return super.equals(other) &&
               this.addToShoppingList == other.addToShoppingList &&
               this.addToShoppingListTrigger == other.addToShoppingListTrigger
    }

    override fun toDebugString(): String {
        return super.toString() +
               "\nautoAddToShoppingList = $addToShoppingList" +
               "\nautoAddToShoppingListTrigger = $addToShoppingListTrigger"
    }

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. Field values (in the form of an EnumSet<Field>)
     *  are used as a payload in the adapter notifyItemChanged calls in order
     *  to identify which fields were changed.*/
    enum class Field { Name, ExtraInfo, Color,
                       Amount, AddToShoppingList,
                       AddToShoppingListTrigger }
}

