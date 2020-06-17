package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list_item")
class ShoppingListItem {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")                               var id: Long = 0
    @ColumnInfo(name = "name")                             var name: String
    @ColumnInfo(name = "extraInfo", defaultValue = "")     var extraInfo: String = ""
    @ColumnInfo(name = "amount", defaultValue = "1")       var amount: Int = 1
    @ColumnInfo(name = "amountInCart", defaultValue = "0") var amountInCart: Int = 0
    @ColumnInfo(name = "linkedInventoryItemId")            var linkedInventoryItemId: Long? = null
    @ColumnInfo(name = "inTrash", defaultValue = "0")      var inTrash: Boolean = false

    constructor(name: String,
                extraInfo: String = "",
                amountInCart: Int = 0,
                amount: Int = 1,
                linkedInventoryItemId: Long? = null) {
        this.name = name
        this.extraInfo = extraInfo
        this.amountInCart = amountInCart
        this.amount = amount
        this.linkedInventoryItemId = linkedInventoryItemId
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is ShoppingListItem) return false
        return this.id == other.id &&
               this.name == other.name &&
               this.extraInfo == other.extraInfo &&
               this.amountInCart == other.amountInCart &&
               this.amount == other.amount &&
               this.linkedInventoryItemId == other.linkedInventoryItemId
    }

    override fun toString(): String {
        return "\nid = $id" +
               "\nname = $name" +
               "\nextraInfo = $extraInfo" +
               "\namountInCart = $amountInCart" +
               "\namount = $amount" +
               "\nlinkedInventoryItemId = $linkedInventoryItemId"
    }

    override fun hashCode(): Int = id.hashCode()
}