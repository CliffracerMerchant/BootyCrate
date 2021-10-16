/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateItem
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.ExpandableSelectableItemView
import com.cliffracertech.bootycrate.recyclerview.ExpandableSelectableRecyclerView
import com.cliffracertech.bootycrate.recyclerview.InventoryItemView
import com.cliffracertech.bootycrate.view.BottomNavigationDrawer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import java.util.concurrent.TimeoutException

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

/** Assert that the view is an ExpandableSelectableRecyclerView with only one expanded item at index expandedIndex. */
fun onlyExpandedIndexIs(expandedIndex: Int?) = ViewAssertion { view, e ->
    if (view == null) throw e!!
    assertThat(view).isInstanceOf(ExpandableSelectableRecyclerView::class.java)
    val it = view as ExpandableSelectableRecyclerView<*>
    val expandedViewHeight = if (expandedIndex == null) Integer.MAX_VALUE else
        it.findViewHolderForAdapterPosition(expandedIndex)?.itemView?.height ?: throw e
    for (i in 0 until it.adapter.itemCount) {
        val vh = it.findViewHolderForAdapterPosition(i)
        if (i != expandedIndex) assertThat(vh?.itemView?.height).isLessThan(expandedViewHeight)
        else                    assertThat(vh?.itemView?.height).isEqualTo(expandedViewHeight)
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

/** Asserts that the view is an ExpandableSelectableRecyclerView that
    contains only the specified items of type T, in the order given. */
open class onlyShownItemsAre<T: BootyCrateItem>(vararg items: T) : ViewAssertion {
    private val items = items.asList()

    @CallSuper
    open fun assertItemFromViewMatchesOriginalItem(view: ExpandableSelectableItemView<T>, item: T) {
        assertThat(view.ui.nameEdit.text.toString()).isEqualTo(item.name)
        assertThat(view.ui.extraInfoEdit.text.toString()).isEqualTo(item.extraInfo)
        assertThat(view.ui.checkBox.colorIndex).isEqualTo(item.color)
        assertThat(view.ui.amountEdit.value).isEqualTo(item.amount)
    }

    override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
        if (view == null) throw noViewFoundException!!
        assertThat(view).isInstanceOf(ExpandableSelectableRecyclerView::class.java)
        val it = view as ExpandableSelectableRecyclerView<*>
        assertThat(items.size).isEqualTo(it.adapter.itemCount)
        for (i in 0 until it.adapter.itemCount) {
            val vh = it.findViewHolderForAdapterPosition(i)
            assertThat(vh).isNotNull()
            val itemView = vh!!.itemView as ExpandableSelectableItemView<T>
            assertThat(itemView).isNotNull()
            assertThat(assertItemFromViewMatchesOriginalItem(itemView, items[i]))
        }
    }
}

/** Asserts that the matching view is an ExpandableSelectableRecyclerView subclass
 * that only shows the provided shopping list items, in the order given. */
class onlyShownShoppingListItemsAre(vararg items: ShoppingListItem) :
    onlyShownItemsAre<ShoppingListItem>(*items)
{
    override fun assertItemFromViewMatchesOriginalItem(
        view: ExpandableSelectableItemView<ShoppingListItem>,
        item: ShoppingListItem
    ) {
        super.assertItemFromViewMatchesOriginalItem(view, item)
        assertThat(view.ui.checkBox.isChecked).isEqualTo(item.isChecked)
    }
}

/** Asserts that the matching view is an ExpandableSelectableRecyclerView
 * subclass that only shows the provided inventory items, in the order given. */
class onlyShownInventoryItemsAre(vararg items: InventoryItem) :
    onlyShownItemsAre<InventoryItem>(*items)
{
    override fun assertItemFromViewMatchesOriginalItem(
        view: ExpandableSelectableItemView<InventoryItem>,
        item: InventoryItem
    ) {
        super.assertItemFromViewMatchesOriginalItem(view, item)
        view as InventoryItemView
        assertThat(view.detailsUi.autoAddToShoppingListCheckBox.isChecked).isEqualTo(item.autoAddToShoppingList)
        assertThat(view.detailsUi.autoAddToShoppingListAmountEdit.value).isEqualTo(item.autoAddToShoppingListAmount)
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

/** Perform a series of actions and consequent tests on the LiveData instance.
 * The size of actions and tests must be the same. Each action will be performed,
 * after which the change in the LiveData value will be waited for up to the
 * value of the parameter timeOut. The corresponding entry in tests will be ran
 * after the LiveData changes. The process is repeated for each entry in actions
 * and tests. */
@DelicateCoroutinesApi
fun <T> LiveData<T>.observeForTesting(
    timeOut: Long,
    actions: List<() -> Unit>,
    tests: List<() -> Unit>,
) {
    assertThat(actions.size).isEqualTo(tests.size)
    var index = 0
    val observer = Observer<T> { tests[index].invoke(); index++ }
    GlobalScope.launch(Dispatchers.Main) { observeForever(observer) }

    while (index < actions.size) {
        val start = System.currentTimeMillis()
        val currentIndex = index
        actions[index].invoke()
        while (index == currentIndex && (System.currentTimeMillis() - start) < timeOut )
            Thread.sleep(timeOut / 10L)
        if (index == currentIndex) throw TimeoutException(
            "The LiveData did not update before the timeout ($timeOut) was reached.")
    }
    GlobalScope.launch(Dispatchers.Main) { removeObserver(observer) }
}