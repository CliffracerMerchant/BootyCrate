/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.ExpandableSelectableRecyclerView
import com.cliffracertech.bootycrate.recyclerview.InventoryRecyclerView
import com.cliffracertech.bootycrate.recyclerview.ShoppingListRecyclerView
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/** Thanks to the author of this blog post for the idea.
 * https://medium.com/android-news/call-view-methods-when-testing-by-espresso-and-kotlin-in-android-781262f7348e */
fun <T>doStuff(method: (view: T) -> Unit): ViewAction {
    return object: ViewAction {
        override fun getDescription() = method.toString()
        override fun getConstraints() = isEnabled()
        override fun perform(uiController: UiController?, view: View) =
            method(view as? T ?: throw IllegalStateException("The matched view is null or not of type T"))
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

fun actionOnChildWithId(viewId: Int, action: ViewAction) = object : ViewAction {
    override fun getConstraints() = null
    override fun getDescription() = "Click on a child view with specified id."
    override fun perform(uiController: UiController, view: View) =
        action.perform(uiController, view.findViewById(viewId))
}

class isEnabled : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) { description.appendText("is enabled: ") }
    override fun matchesSafely(item: View) = item.isEnabled
}

fun onPopupView(viewMatcher: Matcher<View>) = onView(viewMatcher).inRoot(isPlatformPopup())

/** Assert that the view is an ExpandableSelectableRecyclerView with only one expanded item at
 * index expandedIndex. The height of collapsed items must also be provided. */
fun onlyExpandedIndexIs(expandedIndex: Int?, collapsedHeight: Int) = ViewAssertion { view, e ->
    if (view == null) throw e!!
    assertThat(view).isInstanceOf(ExpandableSelectableRecyclerView::class.java)
    val it = view as ExpandableSelectableRecyclerView<*>
    for (i in 0 until it.adapter.itemCount) {
        val vh = it.findViewHolderForAdapterPosition(i)
        if (i != expandedIndex) assertThat(vh!!.itemView.height).isEqualTo(collapsedHeight)
        else                    assertThat(vh!!.itemView.height).isGreaterThan(collapsedHeight)
    }
}

/** Asserts that the view is an ExpandableSelectableRecyclerView, with the items
 * at the specified indices all selected, and with no other selected items. */
fun onlySelectedIndicesAre(vararg indices: Int) = ViewAssertion { view, e ->
    if (view == null) throw e!!
    assertThat(view).isInstanceOf(ExpandableSelectableRecyclerView::class.java)
    val it = view as ExpandableSelectableRecyclerView<*>
    for (i in 0 until it.adapter.itemCount) {
        val vh = it.findViewHolderForAdapterPosition(i)!! as ExpandableSelectableRecyclerView<*>.ViewHolder
        val shouldBeSelected = i in indices
        assertThat(vh.item.isSelected).isEqualTo(shouldBeSelected)
        val itemView = vh.itemView as ExpandableSelectableItemView<*>
        assertThat(itemView.isInSelectedState).isEqualTo(shouldBeSelected)
    }
}