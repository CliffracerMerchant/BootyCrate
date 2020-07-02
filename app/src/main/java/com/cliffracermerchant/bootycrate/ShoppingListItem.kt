package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list_item")
class ShoppingListItem {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")                               var id: Long = 0
    @ColumnInfo(name = "name")                             var name: String
    @ColumnInfo(name = "extraInfo")                        var extraInfo: String = ""
    @ColumnInfo(name = "color")                            var color: Int
    @ColumnInfo(name = "amountOnList", defaultValue = "1") var amountOnList: Int = 1
    @ColumnInfo(name = "amountInCart", defaultValue = "0") var amountInCart: Int = 0
    @ColumnInfo(name = "linkedInventoryItemId")            var linkedInventoryItemId: Long?
    @ColumnInfo(name = "inTrash", defaultValue = "0")      var inTrash: Boolean = false

    constructor(name: String = "",
                extraInfo: String = "",
                color: Int = -144976720,
                amountOnList: Int = 1,
                amountInCart: Int = 0,
                linkedInventoryItemId: Long? = null) {
        this.name = name
        this.extraInfo = extraInfo
        this.color = color
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
               this.amountOnList == other.amountOnList &&
               this.amountInCart == other.amountInCart &&
               this.linkedInventoryItemId == other.linkedInventoryItemId
    }

    override fun toString(): String {
        return "\nid = $id" +
               "\nname = $name" +
               "\nextraInfo = $extraInfo" +
               "\ncolor = $color" +
               "\namountOnList = $amountOnList" +
               "\namountInCart = $amountInCart" +
               "\nlinkedInventoryItemId = $linkedInventoryItemId"
    }

    override fun hashCode(): Int = id.hashCode()
}