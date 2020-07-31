/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A Room entity that represents an shopping list item in the user's shopping list. */
@Entity(tableName = "shopping_list_item")
class ShoppingListItem {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")        var id: Long = 0
    @ColumnInfo(name = "name")      var name: String
    @ColumnInfo(name = "extraInfo") var extraInfo: String = ""
    @ColumnInfo(name = "color")     var color: Int
    @ColumnInfo(name = "isChecked", defaultValue = "0")    var isChecked: Boolean = false
    @ColumnInfo(name = "amountOnList", defaultValue = "1") var amountOnList: Int = 1
    @ColumnInfo(name = "amountInCart", defaultValue = "0") var amountInCart: Int = 0
    @ColumnInfo(name = "linkedInventoryItemId")            var linkedInventoryItemId: Long?
    @ColumnInfo(name = "inTrash", defaultValue = "0")      var inTrash: Boolean = false

    constructor(name: String = "",
                extraInfo: String = "",
                color: Int = -144976720,
                isChecked: Boolean = false,
                amountOnList: Int = 1,
                amountInCart: Int = 0,
                linkedInventoryItemId: Long? = null) {
        this.name = name
        this.extraInfo = extraInfo
        this.color = color
        this.isChecked = isChecked
        this.amountOnList = amountOnList
        this.amountInCart = amountInCart
        this.linkedInventoryItemId = linkedInventoryItemId
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is ShoppingListItem) return false
        return this.id == other.id &&
               this.name == other.name &&
               this.extraInfo == other.extraInfo &&
               this.color == other.color &&
               this.isChecked == other.isChecked &&
               this.amountOnList == other.amountOnList &&
               this.amountInCart == other.amountInCart &&
               this.linkedInventoryItemId == other.linkedInventoryItemId
    }

    override fun toString(): String {
        return "\nid = $id" +
               "\nname = $name" +
               "\nextraInfo = $extraInfo" +
               "\ncolor = $color" +
               "\nisChecked = $isChecked" +
               "\namountOnList = $amountOnList" +
               "\namountInCart = $amountInCart" +
               "\nlinkedInventoryItemId = $linkedInventoryItemId"
    }

    override fun hashCode(): Int = id.hashCode()
}