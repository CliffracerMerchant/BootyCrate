/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity open class ViewModelItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")                           var id: Long = 0,
    @ColumnInfo(name = "name")                         var name: String = "",
    @ColumnInfo(name = "extraInfo", defaultValue = "") var extraInfo: String = "",
    @ColumnInfo(name = "color", defaultValue = "0")    var color: Int = 0,
    @ColumnInfo(name = "amount", defaultValue = "1")   var amount: Int = 1,
    @ColumnInfo(name = "linkedItemId")                 var linkedItemId: Long? = null,
    @ColumnInfo(name = "inTrash", defaultValue = "0")  var inTrash: Boolean = false
) {

    init { color.coerceIn(Colors.indices) }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is ViewModelItem) return false
        return this.id == other.id &&
               this.name == other.name &&
               this.extraInfo == other.extraInfo &&
               this.color == other.color &&
               this.amount == other.amount &&
               this.linkedItemId == other.linkedItemId
    }

    // For debugging purposes, when all info is desired
    open fun toDebugString() ="""
        id = $id
        name = $name
        extraInfo = $extraInfo
        color = $color
        amount = $amount
        linkedItemId = $linkedItemId"""

    // For a user-facing string representation of the object
    override fun toString() = "${amount}x $name" + (if (extraInfo.isNotBlank()) ", $extraInfo" else "")

    override fun hashCode() = id.hashCode()

    enum class Sort { Color, NameAsc, NameDesc, AmountAsc, AmountDesc;
        companion object {
            fun fromString(string: String?): Sort =
                if (string == null) Color
                else try { valueOf(string) }
                     // If string value doesn't match a Sort value
                     catch(e: IllegalArgumentException) { Color }
        }
    }

    companion object {
        val Colors = intArrayOf(
            -3452848,
            -3437488,
            -3421360,
            -7550128,
            -11482288,
            -11482228,
            -11482165,
            -11498293,
            -11513653,
            -7581493,
            -3452725,
            -3452788)
    }
}

/** An extension of ViewModelItem that adds two fields, isExpanded and isSelected. */
@Entity open class ExpandableSelectableItem(
    id: Long = 0,
    name: String = "",
    extraInfo: String = "",
    color: Int = 0,
    amount: Int = 1,
    linkedItemId: Long? = null,
    inTrash: Boolean = false,
    @ColumnInfo(name="isExpanded", defaultValue = "0") var isExpanded: Boolean = false,
    @ColumnInfo(name="isSelected", defaultValue = "0") var isSelected: Boolean = false
) : ViewModelItem(id, name, extraInfo, color, amount, linkedItemId, inTrash) {

    override fun toDebugString() = super.toString() +
            "\nisExpanded = $isExpanded\nisSelected = $isSelected"
}
