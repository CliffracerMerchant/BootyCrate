package com.cliffracermerchant.bootycrate

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_item")
class InventoryItem {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")                           var id: Long = 0
    @ColumnInfo(name = "name")                         var name: String
    @ColumnInfo(name = "amount", defaultValue = "0")   var amount: Int
    @ColumnInfo(name = "extraInfo", defaultValue = "") var extraInfo: String
    @ColumnInfo(name = "autoAddToShoppingList", defaultValue = "0")        var autoAddToShoppingList: Boolean = false
    @ColumnInfo(name = "autoAddToShoppingListTrigger", defaultValue = "1") var autoAddToShoppingListTrigger: Int = 1
    @ColumnInfo(name = "inTrash", defaultValue = "0")  var inTrash: Boolean = false

    constructor(name: String,
                amount: Int = 1,
                extraInfo: String = "",
                autoAddToShoppingList: Boolean = false,
                autoAddToShoppingListTrigger: Int = 1) {
        this.name = name
        this.amount = amount
        this.extraInfo = extraInfo
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
               this.autoAddToShoppingListTrigger == other.autoAddToShoppingListTrigger
    }

    override fun toString(): String {
        return "\nid = " + id +
               "\nname = " + name +
               "\namount = " + amount +
               "\nextraInfo = " + extraInfo +
               "\nautoAddToShoppingList = " + autoAddToShoppingList +
               "\nautoAddToShoppingListTrigger = " + autoAddToShoppingListTrigger
    }

    override fun hashCode(): Int = id.hashCode()
}

