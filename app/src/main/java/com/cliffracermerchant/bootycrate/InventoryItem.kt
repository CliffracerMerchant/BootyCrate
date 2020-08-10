/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A Room entity that represents an inventory item in the user's inventory. */
@Entity(tableName = "inventory_item")
class InventoryItem {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")        var id: Long = 0
    @ColumnInfo(name = "name")      var name: String
    @ColumnInfo(name = "extraInfo") var extraInfo: String
    @ColumnInfo(name = "color")     var color: Int
    @ColumnInfo(name = "amount", defaultValue = "1") var amount: Int
    @ColumnInfo(name = "autoAddToShoppingList", defaultValue = "0") var autoAddToShoppingList: Boolean
    @ColumnInfo(name = "autoAddToShoppingListTrigger", defaultValue = "1") var autoAddToShoppingListTrigger: Int
    @ColumnInfo(name = "inTrash", defaultValue = "0") var inTrash: Boolean = false

    constructor(name: String = "",
                extraInfo: String = "",
                color: Int = -144976720,
                amount: Int = 1,
                autoAddToShoppingList: Boolean = false,
                autoAddToShoppingListTrigger: Int = 1) {
        this.name = name
        this.extraInfo = extraInfo
        this.color = color
        this.amount = amount
        this.autoAddToShoppingList = autoAddToShoppingList
        this.autoAddToShoppingListTrigger = autoAddToShoppingListTrigger
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is InventoryItem) return false
        return this.id == other.id &&
               this.name == other.name &&
               this.amount == other.amount &&
               this.extraInfo == other.extraInfo &&
               this.autoAddToShoppingList == other.autoAddToShoppingList &&
               this.autoAddToShoppingListTrigger == other.autoAddToShoppingListTrigger &&
               this.color == other.color
    }

    override fun toString(): String {
        return "\nid = $id" +
               "\nname = $name" +
               "\namount = $amount" +
               "\nextraInfo = $extraInfo" +
               "\nautoAddToShoppingList = $autoAddToShoppingList" +
               "\nautoAddToShoppingListTrigger = $autoAddToShoppingListTrigger" +
               "\ncolor = $color"
    }

    override fun hashCode(): Int = id.hashCode()

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. Field values (in the form of an EnumSet<Field>)
     *  are used as a payload in the adapter notifyItemChanged calls in order
     *  to identify which fields were changed.*/
    enum class Field { Name, Amount, ExtraInfo, AutoAddToShoppingList,
                       AutoAddToShoppingListTrigger, Color }
}

