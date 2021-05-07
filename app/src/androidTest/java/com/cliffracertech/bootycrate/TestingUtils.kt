/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.InventoryRecyclerView
import com.cliffracertech.bootycrate.recyclerview.ShoppingListRecyclerView
import com.google.common.truth.Truth.assertThat

/** Thanks to the author of this blog post for the idea.
 * https://medium.com/android-news/call-view-methods-when-testing-by-espresso-and-kotlin-in-android-781262f7348e */
fun <T>doStuff(method: (view: T) -> Unit): ViewAction {
    return object: ViewAction {
        override fun getDescription() = method.toString()
        override fun getConstraints() = isEnabled()
        override fun perform(uiController: UiController?, view: View?) {
            val t = view as? T ?: throw IllegalStateException("The matched view is null or not of type T")
            method(t)
        }

    }
}

fun InventoryRecyclerView.itemFromVhAtPos(pos: Int): InventoryItem {
    val vh = findViewHolderForAdapterPosition(pos)
    assertThat(vh).isNotNull()
    val itemView = vh!!.itemView as? InventoryItemView
    assertThat(itemView).isNotNull()
    return InventoryItem(
        name = itemView!!.ui.nameEdit.text.toString(),
        extraInfo = itemView.ui.extraInfoEdit.text.toString(),
        color = itemView.ui.checkBox.colorIndex,
        amount = itemView.ui.amountEdit.value,
        addToShoppingList = itemView.detailsUi.addToShoppingListCheckBox.isChecked,
        addToShoppingListTrigger = itemView.detailsUi.addToShoppingListTriggerEdit.value)
}

fun ShoppingListRecyclerView.itemFromVhAtPos(pos: Int): ShoppingListItem {
    val vh = findViewHolderForAdapterPosition(pos)
    assertThat(vh).isNotNull()
    val itemView = vh!!.itemView as? ShoppingListItemView
    assertThat(itemView).isNotNull()
    return ShoppingListItem(
        name = itemView!!.ui.nameEdit.text.toString(),
        extraInfo = itemView.ui.extraInfoEdit.text.toString(),
        color = itemView.ui.checkBox.colorIndex,
        amount = itemView.ui.amountEdit.value)
}