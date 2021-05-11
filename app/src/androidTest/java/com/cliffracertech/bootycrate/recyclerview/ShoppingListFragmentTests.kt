/*
 * Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */
package com.cliffracertech.bootycrate.recyclerview

import android.app.Application
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.cliffracertech.bootycrate.*
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.ShoppingListItem
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
        onPopupView(withText(R.string.color_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun sortByNameAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_ascending_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(grayItem11, orangeItem1, redItem0, yellowItem2))
    }

    @Test
    fun sortByNameDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_descending_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(yellowItem2, redItem0, orangeItem1, grayItem11))
    }

    @Test fun sortByAmountAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_ascending_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(yellowItem2, orangeItem1, redItem0, grayItem11))
    }

    @Test fun sortByAmountDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_descending_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(grayItem11, redItem0, orangeItem1, yellowItem2))
    }

    private var collapsedItemHeight = 0
    @Test fun expandItem() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<RecyclerView> {
            (it.itemAnimator as ExpandableItemAnimator).notifyExpandedItemChanged(null)
            collapsedItemHeight = it.findViewHolderForAdapterPosition(0)!!.itemView.height
        }).check(onlyExpandedIndexIs(null, collapsedItemHeight))

        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<ShoppingListRecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click()))
        ).check(onlyExpandedIndexIs(1, collapsedItemHeight))
    }

    @Test fun expandAnotherItem() {
        expandItem()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<ShoppingListRecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.editButton, click()))
        ).check(onlyExpandedIndexIs(3, collapsedItemHeight))
    }

    private fun expandedItemSurvives(action: Runnable) {
        expandItem()
        action.run()
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlyExpandedIndexIs(1, collapsedItemHeight))
    }

    private fun switchToInventoryAndBack() {
        onView(withId(R.id.inventory_button)).perform(click())
        onView(withId(R.id.shopping_list_button)).perform(click())
    }

    private fun switchToPreferencesAndBack() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.settings_description)).perform(click())
        onView(withId(R.id.backButton)).perform(click())
    }

    private fun changeOrientationAndBack() {
        uiDevice.setOrientationLeft()
        uiDevice.setOrientationNatural()
    }

    private fun changeOrientationWhileInInventory() {
        onView(withId(R.id.inventory_button)).perform(click())
        changeOrientationAndBack()
        onView(withId(R.id.shopping_list_button)).perform(click())
    }

    private fun changeOrientationWhileInPreferences() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.settings_description)).perform(click())
        uiDevice.setOrientationLeft()
        uiDevice.setOrientationNatural()
        onView(withId(R.id.backButton)).perform(click())
    }

    @Test fun expandedItemSurvivesSwitchingToInventory() = expandedItemSurvives(::switchToInventoryAndBack)
    @Test fun expandedItemSurvivesSwitchingToPreferences() = expandedItemSurvives(::switchToPreferencesAndBack)
    @Test fun expandedItemSurvivesOrientationChange() = expandedItemSurvives(::changeOrientationAndBack)
    @Test fun expandedItemSurvivesOrientationChangeWhileInInventory() = expandedItemSurvives(::changeOrientationWhileInInventory)
    @Test fun expandedItemSurvivesOrientationChangeWhileInPreferences() = expandedItemSurvives(::changeOrientationWhileInPreferences)

    @Test fun selectIndividualItems() {
        runBlocking { db.shoppingListItemDao().clearSelection() }
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick())
        ).check(onlySelectedIndicesAre(1))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click())
        ).check(onlySelectedIndicesAre(1, 3))
    }

    @Test fun deselectIndividualItems() {
        selectIndividualItems()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click())
        ).check(onlySelectedIndicesAre(3))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click())
        ).check(onlySelectedIndicesAre())
    }

    @Test fun selectAllItems() {
        runBlocking { db.shoppingListItemDao().clearSelection() }
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.select_all_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlySelectedIndicesAre(0, 1, 2, 3))
    }

    @Test fun deselectAllItemsWithActionBarBackButton() {
        runBlocking { db.shoppingListItemDao().selectAll() }
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    @Test fun deselectAllItemsWithNavigationBackButton() {
        runBlocking { db.shoppingListItemDao().selectAll() }
        pressBack()
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    private fun selectionSurvives(action: Runnable) {
        selectIndividualItems()
        action.run()
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlySelectedIndicesAre(1, 3))
    }
    @Test fun selectionSurvivesSwitchingToInventory() = selectionSurvives(::switchToInventoryAndBack)
    @Test fun selectionSurvivesSwitchingToPreferences() = selectionSurvives(::switchToPreferencesAndBack)
    @Test fun selectionSurvivesOrientationChange() = selectionSurvives(::changeOrientationAndBack)
    @Test fun selectionSurvivesOrientationChangeWhileInInventory() = selectionSurvives(::changeOrientationWhileInInventory)
    @Test fun selectionSurvivesOrientationChangeWhileInPreferences() = selectionSurvives(::changeOrientationWhileInPreferences)

    private fun hasOnlyCheckedItemsAtIndices(vararg checkedItemsIndices: Int) = ViewAssertion { view, e ->
        if (view == null) throw e
        assertThat(view).isInstanceOf(ShoppingListRecyclerView::class.java)
        val it = view as ShoppingListRecyclerView
        for (i in 0 until it.adapter.itemCount) {
            val ui = (it.findViewHolderForAdapterPosition(i)!!.itemView as ShoppingListItemView).ui
            val shouldBeChecked = checkedItemsIndices.contains(i)
            assertThat(ui.checkBox.isChecked).isEqualTo(shouldBeChecked)
            assertThat(ui.nameEdit.hasStrikeThrough).isEqualTo(shouldBeChecked)
            if (ui.extraInfoEdit.text?.isNotBlank() == true)
                assertThat(ui.extraInfoEdit.hasStrikeThrough).isEqualTo(shouldBeChecked)
        }
    }

    private fun clickCheckBox() = actionOnChildWithId(R.id.checkBox, click())

    @Test fun checkIndividualItems() {
        runBlocking { db.shoppingListItemDao().uncheckAll() }
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickCheckBox()),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2, clickCheckBox())
        ).check(hasOnlyCheckedItemsAtIndices(0, 2))
    }

    @Test fun uncheckIndividualItems() {
        checkIndividualItems()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickCheckBox()),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2, clickCheckBox())
        ).check(hasOnlyCheckedItemsAtIndices())
    }

    @Test fun checkAllItems() {
        runBlocking { db.shoppingListItemDao().uncheckAll() }
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.check_all_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView))
            .check(hasOnlyCheckedItemsAtIndices(0, 1, 2, 3))
    }

    @Test fun uncheckAllItems() {
        checkAllItems()
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.uncheck_all_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView))
            .check(hasOnlyCheckedItemsAtIndices())
    }

    @Test fun checkoutButtonEnabledAfterCheckingIndividualItems() {
        runBlocking { db.shoppingListItemDao().uncheckAll() }
        onView(withId(R.id.checkout_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, actionOnChildWithId(R.id.checkBox, click())))
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, actionOnChildWithId(R.id.checkBox, click())))
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
    }

    @Test fun checkoutButtonDisabledAfterUncheckingIndividualItems() {
        checkoutButtonEnabledAfterCheckingIndividualItems()
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
        onPopupView(withText(R.string.check_all_description)).perform(click())
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
    }

    @Test fun checkoutButtonDisabledAfterUncheckingAllItems() {
        runBlocking { db.shoppingListItemDao().checkAll() }
        onView(withId(R.id.checkout_button)).check(matches(isEnabled()))
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.uncheck_all_description)).perform(click())
        onView(withId(R.id.checkout_button)).check(matches(not(isEnabled())))
    }
}