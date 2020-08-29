/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity open class ViewModelItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")        var id: Long = 0,
    @ColumnInfo(name = "name")      var name: String = "",
    @ColumnInfo(name = "extraInfo", defaultValue = "") var extraInfo: String = "",
    @ColumnInfo(name = "color", defaultValue = "0")    var color: Int = 0,
    @ColumnInfo(name = "amount", defaultValue = "1")   var amount: Int = 1,
    @ColumnInfo(name = "inTrash", defaultValue = "0")  var inTrash: Boolean = false) {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is ViewModelItem) return false
        return this.id == other.id &&
               this.name == other.name &&
               this.extraInfo == other.extraInfo &&
               this.color == other.color &&
               this.amount == other.amount
    }

    override fun toString() =
        "\nid = $id" +
        "\nname = $name" +
        "\nextraInfo = $extraInfo" +
        "\ncolor = $color" +
        "\namount = $amount"

    override fun hashCode() = id.hashCode()

    enum class Field { Name, ExtraInfo, Color, Amount }

    enum class Sort { Color, NameAsc, NameDesc, AmountAsc, AmountDesc }

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

        fun sortFrom(string: String?): Sort {
            return if (string == null) Sort.Color
            else try { Sort.valueOf(string) }
            // If sortStr value doesn't match a Sort value
            catch(e: IllegalArgumentException) { Sort.Color }
        }
    }
}