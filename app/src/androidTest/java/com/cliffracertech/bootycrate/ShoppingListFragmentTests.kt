/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.app.Application
import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemAnimator
import com.cliffracertech.bootycrate.recyclerview.ShoppingListRecyclerView
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ShoppingListFragmentTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)
    private val db = BootyCrateDatabase.get(context as Application)
    private val uiDevice: UiDevice = UiDevice.getInstance(getInstrumentation())

    private val redItem0 = ShoppingListItem(name = "Red", extraInfo = "Extra info", color = 0, amount = 8)
    private val orangeItem1 = ShoppingListItem(name = "Orange", extraInfo = "Extra info", color = 1, amount = 2)
    private val yellowItem2 = ShoppingListItem(name = "Yellow", color = 2, amount = 1)
    private val grayItem11 = ShoppingListItem(name = "Gray", color = 11, amount = 9)

    @Before fun setup() {
        runBlocking {
            db.inventoryItemDao().deleteAll()
            db.shoppingListItemDao().deleteAll()
            db.shoppingListItemDao().add(listOf(redItem0, orangeItem1, yellowItem2, grayItem11))
        }
    }

    @Test fun sortByColor() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onView(withText(R.string.color_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(yellowItem2)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(grayItem11)
        })
    }

    @Test fun sortByNameAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onView(withText(R.string.name_ascending_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(grayItem11)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(yellowItem2)
        })
    }

    @Test fun sortByNameDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onView(withText(R.string.name_descending_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(yellowItem2)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(grayItem11)
        })
    }

    @Test fun sortByAmountAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onView(withText(R.string.amount_ascending_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(yellowItem2)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(grayItem11)
        })
    }

    @Test fun sortByAmountDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onView(withText(R.string.amount_descending_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(grayItem11)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(yellowItem2)
        })
    }

    private class ExpandedItemsState(initialExpandedItemIndex: Int?) {
        val collapsedHeight = run {
            var h = 0
            onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
                val nonExpandedIndex = (0 until it.adapter.itemCount).find { it != initialExpandedItemIndex }
                h = it.findViewHolderForAdapterPosition(nonExpandedIndex!!)!!.itemView.height
            })
            h
        }

        fun assertOnlyOneExpandedIndex(expandedIndex: Int? = null) {
            onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
                for (i in 0 until it.adapter.itemCount) {
                    val vh = it.findViewHolderForAdapterPosition(i)
                    if (i != expandedIndex) assertThat(vh!!.itemView.height).isEqualTo(collapsedHeight)
                    else                    assertThat(vh!!.itemView.height).isGreaterThan(collapsedHeight)
        }})}
    }

    @Test fun expandItem() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            (it.itemAnimator as ExpandableItemAnimator).notifyExpandedItemChanged(null)
        })
        val expandedState = ExpandedItemsState(null)
        expandedState.assertOnlyOneExpandedIndex(null)
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<ShoppingListRecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())))
        expandedState.assertOnlyOneExpandedIndex(1)
    }

    @Test fun expandAnotherItem() {
        expandItem()
        val expandedState = ExpandedItemsState(1)
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<ShoppingListRecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.editButton, click())))
        expandedState.assertOnlyOneExpandedIndex(3)
    }

    @Test fun expandedItemSurvivesSwitchingToInventory() {
        expandItem()
        val expandedItemState = ExpandedItemsState(1)
        onView(withId(R.id.inventory_button)).perform(click())
        onView(withId(R.id.shopping_list_button)).perform(click())
        expandedItemState.assertOnlyOneExpandedIndex(1)
    }

    @Test fun expandedItemSurvivesSwitchingToPreferences() {
        expandItem()
        val expandedItemState = ExpandedItemsState(1)
        onView(withId(R.id.menuButton)).perform(click())
        onView(withText(R.string.settings_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.backButton)).perform(click())
        expandedItemState.assertOnlyOneExpandedIndex(1)
    }

    @Test fun expandedItemSurvivesActivityRecreation() {
        expandItem()
        val expandedItemState = ExpandedItemsState(1)
        uiDevice.setOrientationLeft()
        uiDevice.setOrientationNatural();
        expandedItemState.assertOnlyOneExpandedIndex(1)
    }

    private fun View.assertIsShoppingListItemViewWithCheckedState(checked: Boolean) {
        assertThat(this as? ShoppingListItemView).isNotNull()
        val ui = (this as ShoppingListItemView).ui
        assertThat(ui.checkBox.isChecked).isEqualTo(checked)
        assertThat(ui.nameEdit.hasStrikeThrough).isEqualTo(checked)
        if (ui.extraInfoEdit.text?.isNotBlank() == true)
            assertThat(ui.extraInfoEdit.hasStrikeThrough).isEqualTo(checked)
    }

    @Test fun checkIndividualItems() {
        runBlocking { db.shoppingListItemDao().uncheckAll() }
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, actionOnChildWithId(R.id.checkBox, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2, actionOnChildWithId(R.id.checkBox, click())),
            doStuff<ShoppingListRecyclerView> {
                it.findViewHolderForAdapterPosition(0)!!.itemView.assertIsShoppingListItemViewWithCheckedState(true)
                it.findViewHolderForAdapterPosition(1)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
                it.findViewHolderForAdapterPosition(2)!!.itemView.assertIsShoppingListItemViewWithCheckedState(true)
                it.findViewHolderForAdapterPosition(3)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
            })
    }

    @Test fun uncheckIndividualItems() {
        checkIndividualItems()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, actionOnChildWithId(R.id.checkBox, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2, actionOnChildWithId(R.id.checkBox, click())),
            doStuff<ShoppingListRecyclerView> {
                it.findViewHolderForAdapterPosition(0)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
                it.findViewHolderForAdapterPosition(1)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
                it.findViewHolderForAdapterPosition(2)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
                it.findViewHolderForAdapterPosition(3)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
            })
    }

    @Test fun checkAllItems() {
        runBlocking { db.shoppingListItemDao().uncheckAll() }
        onView(withId(R.id.menuButton)).perform(click())
        onView(withText(R.string.check_all_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            it.findViewHolderForAdapterPosition(0)!!.itemView.assertIsShoppingListItemViewWithCheckedState(true)
            it.findViewHolderForAdapterPosition(1)!!.itemView.assertIsShoppingListItemViewWithCheckedState(true)
            it.findViewHolderForAdapterPosition(2)!!.itemView.assertIsShoppingListItemViewWithCheckedState(true)
            it.findViewHolderForAdapterPosition(3)!!.itemView.assertIsShoppingListItemViewWithCheckedState(true)
        })
    }

    @Test fun uncheckAllItems() {
        checkAllItems()
        onView(withId(R.id.menuButton)).perform(click())
        onView(withText(R.string.uncheck_all_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            it.findViewHolderForAdapterPosition(0)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
            it.findViewHolderForAdapterPosition(1)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
            it.findViewHolderForAdapterPosition(2)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
            it.findViewHolderForAdapterPosition(3)!!.itemView.assertIsShoppingListItemViewWithCheckedState(false)
        })
    }

    @Test fun checkoutButtonEnablesAfterCheckingIndividualItems() {
        runBlocking { db.shoppingListItemDao().uncheckAll() }
        onView(withId(R.id.checkout_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, actionOnChildWithId(R.id.checkBox, click())))
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, actionOnChildWithId(R.id.checkBox, click())))
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
    }

    @Test fun checkoutButtonDisablesAfterUncheckingIndividualItems() {
        checkoutButtonEnablesAfterCheckingIndividualItems()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, actionOnChildWithId(R.id.checkBox, click())))
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, actionOnChildWithId(R.id.checkBox, click())))
        onView(withId(R.id.checkout_button)).check(matches(not(isEnabled())))
    }

    @Test fun checkoutButtonEnabledAfterCheckingAllItems() {
        runBlocking { db.shoppingListItemDao().uncheckAll() }
        onView(withId(R.id.checkout_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.menuButton)).perform(click())
        onView(withText(R.string.check_all_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
    }

    @Test fun checkoutButtonDisabledAfterUncheckingAllItems() {
        runBlocking { db.shoppingListItemDao().checkAll() }
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
        onView(withId(R.id.menuButton)).perform(click())
        onView(withText(R.string.uncheck_all_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.checkout_button)).check(matches(not(isEnabled())))
    }
}