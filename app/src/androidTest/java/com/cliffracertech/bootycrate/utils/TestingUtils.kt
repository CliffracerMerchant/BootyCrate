/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.content.Context
import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemView
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemListView
import com.cliffracertech.bootycrate.recyclerview.InventoryItemView
import com.cliffracertech.bootycrate.view.BottomNavigationDrawer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import java.util.concurrent.TimeoutException

fun getTestDatabase(context: Context) =
    Room.inMemoryDatabaseBuilder(
        context, BootyCrateDatabase::class.java
    ).addCallback(BootyCrateDatabase.Callback()).build()

/** A ViewAction that allows direct operation on a view.
 * Thanks to the author of this blog post for the idea.
 * https://medium.com/android-news/call-view-methods-when-testing-by-espresso-and-kotlin-in-android-781262f7348e */
class doStuff<T: View>(private val method: (view: T) -> Unit): ViewAction {
    override fun getDescription() = method.toString()
    override fun getConstraints(): Matcher<View> = isEnabled()
    @Suppress("UNCHECKED_CAST")
    override fun perform(uiController: UiController?, view: View) =
        method(view as? T ?: throw IllegalStateException("The matched view is null or not of type T"))
}

/** Perform the given actions on the recycler view item view at the given position. */
fun actionsOnItemAtPosition(pos: Int, vararg actions: ViewAction) = object : ViewAction {
    override fun getConstraints() = allOf(isAssignableFrom(RecyclerView::class.java), isDisplayed())
    override fun getDescription() = "Perform ${actions.asList()} on the view of the item at pos $pos."
    override fun perform(uiController: UiController, view: View) {
        val vh = (view as RecyclerView).findViewHolderForAdapterPosition(pos)
        assertThat(vh).isNotNull()
        for (action in actions)
            action.perform(uiController, vh!!.itemView)
    }
}

fun actionOnChildWithId(viewId: Int, vararg actions: ViewAction) = object : ViewAction {
    override fun getConstraints() = null
    override fun getDescription() = "Perform an action on a child view with specified id."
    override fun perform(uiController: UiController, view: View) {
        val child = view.findViewById<View>(viewId)
        for (action in actions) action.perform(uiController, child)
    }
}

fun clickEditButton() = actionOnChildWithId(R.id.editButton, click())
fun clickCheckBox() = actionOnChildWithId(R.id.checkBox, click())
fun clickAddToShoppingListCheckBox() = actionOnChildWithId(R.id.autoAddToShoppingListCheckBox, click())
fun onAmount(vararg viewActions: ViewAction) = actionOnChildWithId(R.id.amountEdit, *viewActions)
fun onAddToShoppingListTrigger(vararg viewActions: ViewAction) =
    actionOnChildWithId(R.id.autoAddToShoppingListAmountEdit, *viewActions)
fun onIncreaseButton(vararg viewActions: ViewAction) = actionOnChildWithId(R.id.increaseButton, *viewActions)
fun onDecreaseButton(vararg viewActions: ViewAction) = actionOnChildWithId(R.id.decreaseButton, *viewActions)
fun replaceValueEditText(text: String) = actionOnChildWithId(R.id.valueEdit, click(), replaceText(text))

fun onPopupView(viewMatcher: Matcher<View>) = onView(viewMatcher).inRoot(isPlatformPopup())

/** Assert that the view is an ExpandableItemListView with only one expanded item at index expandedIndex. */
fun onlyExpandedIndexIs(expandedIndex: Int?) = ViewAssertion { view, e ->
    if (view == null) throw e!!
    assertThat(view).isInstanceOf(ExpandableItemListView::class.java)
    val it = view as ExpandableItemListView<*>
    val expandedViewHeight = if (expandedIndex == null) Integer.MAX_VALUE else
        it.findViewHolderForAdapterPosition(expandedIndex)?.itemView?.height ?: throw e
    for (i in 0 until it.listAdapter.getItemCount()) {
        val vh = it.findViewHolderForAdapterPosition(i)
        if (i != expandedIndex)
            assertThat(vh?.itemView?.height).isLessThan(expandedViewHeight)
        else assertThat(vh?.itemView?.height).isEqualTo(expandedViewHeight)
    }
}

/** Asserts that the view is an ExpandableItemListView, with the items at
 * the specified indices all selected, and with no other selected items. */
fun onlySelectedIndicesAre(vararg indices: Int) = ViewAssertion { view, e ->
    if (view == null) throw e!!
    assertThat(view).isInstanceOf(ExpandableItemListView::class.java)
    val it = view as ExpandableItemListView<*>
    for (i in 0 until it.listAdapter.getItemCount()) {
        val vh = it.findViewHolderForAdapterPosition(i)!! as ExpandableItemListView<*>.ViewHolder
        val shouldBeSelected = i in indices
        assertThat(vh.item.isSelected).isEqualTo(shouldBeSelected)
        val itemView = vh.itemView as ExpandableItemView<*>
        assertThat(itemView.isSelected).isEqualTo(shouldBeSelected)
    }
}

/** Asserts that the view is an ExpandableItemListView that contains
    only the specified items of type T, in the order given. */
open class onlyShownItemsAre<T: ListItem>(vararg items: T) : ViewAssertion {
    private val items = items.asList()

    @CallSuper
    open fun assertItemFromViewMatchesOriginalItem(view: ExpandableItemView<T>, item: T) {
        assertThat(view.ui.nameEdit.text.toString()).isEqualTo(item.name)
        assertThat(view.ui.extraInfoEdit.text.toString()).isEqualTo(item.extraInfo)
        assertThat(view.ui.checkBox.colorIndex).isEqualTo(item.color)
        assertThat(view.ui.amountEdit.value).isEqualTo(item.amount)
    }

    override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
        if (view == null) throw noViewFoundException!!
        assertThat(view).isInstanceOf(ExpandableItemListView::class.java)
        val it = view as ExpandableItemListView<*>
        assertThat(items.size).isEqualTo(it.listAdapter.getItemCount())
        for (i in 0 until it.listAdapter.getItemCount()) {
            val vh = it.findViewHolderForAdapterPosition(i)
            assertThat(vh).isNotNull()
            val itemView = vh!!.itemView as ExpandableItemView<T>
            assertThat(itemView).isNotNull()
            assertThat(assertItemFromViewMatchesOriginalItem(itemView, items[i]))
        }
    }
}

/** Asserts that the matching view is an ExpandableItemListView subclass
 * that only shows the provided shopping list items, in the order given. */
class onlyShownShoppingListItemsAre(vararg items: ShoppingListItem) :
    onlyShownItemsAre<ShoppingListItem>(*items)
{
    override fun assertItemFromViewMatchesOriginalItem(
        view: ExpandableItemView<ShoppingListItem>,
        item: ShoppingListItem
    ) {
        super.assertItemFromViewMatchesOriginalItem(view, item)
        assertThat(view.ui.checkBox.isChecked).isEqualTo(item.isChecked)
    }
}

/** Asserts that the matching view is an ExpandableItemListView subclass
 * that only shows the provided inventory items, in the order given. */
class onlyShownInventoryItemsAre(vararg items: InventoryItem) :
    onlyShownItemsAre<InventoryItem>(*items)
{
    override fun assertItemFromViewMatchesOriginalItem(
        view: ExpandableItemView<InventoryItem>,
        item: InventoryItem
    ) {
        super.assertItemFromViewMatchesOriginalItem(view, item)
        view as InventoryItemView
        assertThat(view.detailsUi.autoAddToShoppingListCheckBox.isChecked)
            .isEqualTo(item.autoAddToShoppingList)
        assertThat(view.detailsUi.autoAddToShoppingListAmountEdit.value)
            .isEqualTo(item.autoAddToShoppingListAmount)
    }
}

class hasAlpha(private val alpha: Float) : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("with alpha value = $alpha")
    }
    override fun matchesSafely(item: View) = item.alpha == alpha
}

/** A matcher that matches a view that has a BottomSheetBehavior
 * instance with a state that matches the given state. */
class hasSheetState(@BottomSheetBehavior.State private val state: Int) : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("has bottom sheet behavior state = $state")
    }
    override fun matchesSafely(item: View): Boolean {
        val behavior = BottomSheetBehavior.from(item)
        return behavior.state == state
    }
}

/** A ViewAction that will set a view's BottomSheetBehavior instance to the
 * specified state, and wait up to 1.5 seconds for the change to occur. */
class setStateAndWaitForSettling(@BottomSheetBehavior.State private val state: Int) : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(BottomNavigationDrawer::class.java)
    override fun getDescription() = "Set state to $state and await settling."

    override fun perform(uiController: UiController, view: View) {
        val sheetBehavior = BottomSheetBehavior.from(view)
        sheetBehavior.state = state

        var counter = 0
        while (sheetBehavior.isSettling && counter++ < 15)
            Thread.sleep(100L)
    }
}

fun setExpandedAndWaitForSettling() = setStateAndWaitForSettling(BottomSheetBehavior.STATE_EXPANDED)
fun setCollapsedAndWaitForSettling() = setStateAndWaitForSettling(BottomSheetBehavior.STATE_COLLAPSED)

/** Perform a series of actions on the Flow instance. The emitted values
 * after each action is performed will be returned in a list. Each new
 * value will be waited for up to the value of the parameter timeOut. */
fun <T> Flow<T>.collectForTesting(timeOut: Long, vararg actions: () -> Unit): List<T> {
    var index = 0
    val results = mutableListOf<T>()
    TestCoroutineScope().launch {
        take(actions.size).collect {
            results.add(it)
            index++
        }
    }
    while (index < actions.size) {
        actions[index].invoke()
        val start = System.currentTimeMillis()
        while (results.size < (index + 1) && (System.currentTimeMillis() - start) < timeOut)
            Thread.sleep(timeOut / 10L)
        if (results.size < (index)) throw TimeoutException(
            "The Flow did not update before the timeout ($timeOut) was reached.")
    }
    return results
}