/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity

/** A Room entity that represents an shopping list item in the user's shopping list. */
@Entity(tableName = "shopping_list_item")
class ShoppingListItem(
    @ColumnInfo(name = "isChecked", defaultValue = "0") var isChecked: Boolean = false,
    @ColumnInfo(name = "linkedInventoryItemId")         var linkedInventoryItemId: Long? = null
) : ViewModelItem() {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is ShoppingListItem) return false
        return super.equals(other) &&
               this.isChecked == other.isChecked &&
               this.linkedInventoryItemId == other.linkedInventoryItemId
    }

    override fun toString(): String {
        return super.toString() +
               "\nisChecked = $isChecked" +
               "\nlinkedInventoryItemId = $linkedInventoryItemId"
    }

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. Field values (in the form of an EnumSet<Field>)
     *  are used as a payload in the adapter notifyItemChanged calls in order
     *  to identify which fields were changed.*/
    enum class Field { Name, ExtraInfo, Color,
                       Amount, IsChecked, LinkedTo }
}