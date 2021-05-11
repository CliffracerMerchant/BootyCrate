/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.database

import androidx.room.ColumnInfo
import androidx.room.Entity

/** A Room entity that represents a shopping list item in the user's shopping list. */
@Entity(tableName = "shopping_list_item")
class ShoppingListItem(
    id: Long = 0,
    name: String = "",
    extraInfo: String = "",
    color: Int = 0,
    amount: Int = 1,
    linkedItemId: Long? = null,
    inTrash: Boolean = false,
    isExpanded: Boolean = false,
    isSelected: Boolean = false,
    @ColumnInfo(name = "isChecked", defaultValue = "0")
    var isChecked: Boolean = false
) : ExpandableSelectableItem(id, name, extraInfo, color, amount, linkedItemId,
                             inTrash, isExpanded, isSelected) {

    override fun hashCode() = super.hashCode() * 31 + isChecked.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is ShoppingListItem) return false
        return super.equals(other) &&
               this.isChecked == other.isChecked
    }

    override fun toString() = super.toString() + "\nisChecked = $isChecked"

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. */
    enum class Field { Name, ExtraInfo, Color, Amount, LinkedTo,
                       IsExpanded, IsSelected, IsChecked }
}