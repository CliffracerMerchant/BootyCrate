/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.InventoryRecyclerView
import com.cliffracertech.bootycrate.recyclerview.ShoppingListRecyclerView
import com.google.common.truth.Truth.assertThat
import junit.framework.AssertionFailedError
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher

fun inNewItemDialog(matcher: Matcher<View>) =
    allOf(matcher, isDescendantOfA(withId(R.id.newItemViewContainer)))

/** Thanks to the author of this blog post for the idea.
 * https://medium.com/android-news/call-view-methods-when-testing-by-espresso-and-kotlin-in-android-781262f7348e */
fun <T>callMethod(method: (view: T) -> Unit): ViewAction {
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

fun deleteAllTestShoppingListItems(context: Context) = deleteAllTestItemsPrivate(context, false)

fun deleteAllTestInventoryItems(context: Context) {
    onView(withId(R.id.inventory_button)).perform(click())
    deleteAllTestItemsPrivate(context, true)
}

private fun deleteAllTestItemsPrivate(context: Context, inventory: Boolean) {
    val collectionName = context.getString(if (inventory) R.string.inventory_item_collection_name
                                           else           R.string.shopping_list_item_collection_name)
    val emptyMessage = context.getString(R.string.empty_recycler_view_message, collectionName)
    val empty = try { onView(withText(emptyMessage)).check(matches(isDisplayed())); true }
                catch(e: AssertionFailedError) { false }
    if (!empty) {
        onView(withId(R.id.menuButton)).perform(click())
        onView(withText(R.string.select_all_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.changeSortButton)).perform(click())
    }
}

fun addTestShoppingListItems(leaveNewItemDialogOpen: Boolean, vararg items: ShoppingListItem) {
    val lastItem = items.last()
    for (item in items) {
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText(item.name))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click(), typeText(item.extraInfo))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(item.color, click()))
        for (i in 1 until item.amount)
            onView(inNewItemDialog(withId(R.id.increaseButton))).perform(click())
        if (item != lastItem)
            onView(withText(R.string.add_another_item_button_description)).perform(click())
        else if (!leaveNewItemDialogOpen)
            onView(withText(android.R.string.ok)).perform(click())
    }
}

private fun amountIncreaseButton() = allOf(withId(R.id.increaseButton),
                                           isDescendantOfA(withId(R.id.amountEdit)))
private fun autoAddTriggerIncreaseButton() = allOf(withId(R.id.increaseButton),
                                                   isDescendantOfA(withId(R.id.addToShoppingListTriggerEdit)))

fun addTestInventoryItems(leaveNewItemDialogOpen: Boolean, vararg items: InventoryItem) {
    val lastItem = items.last()
    for (item in items) {
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText(item.name))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click(), typeText(item.extraInfo))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(item.color, click()))
        val amountIncreaseButton = inNewItemDialog(amountIncreaseButton())
        for (i in 1 until item.amount)
            onView(amountIncreaseButton).perform(click())
        onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).perform(click())
        val autoAddTriggerIncreaseButton = inNewItemDialog(autoAddTriggerIncreaseButton())
        for (i in 1 until item.addToShoppingListTrigger)
            onView(autoAddTriggerIncreaseButton).perform(click())
        if (item != lastItem)
            onView(withText(R.string.add_another_item_button_description)).perform(click())
        else if (!leaveNewItemDialogOpen)
            onView(withText(android.R.string.ok)).perform(click())
    }
}