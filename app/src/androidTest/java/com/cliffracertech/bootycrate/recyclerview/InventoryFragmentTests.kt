/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.app.Application
import android.content.Context
import android.view.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.utils.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class InventoryFragmentTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)
    private val db = BootyCrateDatabase.get(context as Application)
    private val uiDevice: UiDevice = UiDevice.getInstance(getInstrumentation())

    private val redItem0 = InventoryItem(name = "Red", extraInfo = "Extra info", color = 0, amount = 8)
    private val orangeItem1 = InventoryItem(name = "Orange", extraInfo = "Extra info", color = 1, amount = 2)
    private val yellowItem2 = InventoryItem(name = "Yellow", color = 2, amount = 1)
    private val grayItem11 = InventoryItem(name = "Gray", color = 11, amount = 9)

    @Before fun setup() {
        runBlocking {
            db.inventoryItemDao().deleteAll()
            db.inventoryItemDao().add(listOf(redItem0, orangeItem1, yellowItem2, grayItem11))
        }
        onView(withId(R.id.inventory_button)).perform(click())
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
    }

    @After fun finish() { activityRule.scenario.moveToState(Lifecycle.State.DESTROYED) }

    @Test fun sortByColor() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun sortByNameAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_ascending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(grayItem11, orangeItem1, redItem0, yellowItem2))
    }

    @Test fun sortByNameDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_descending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2, redItem0, orangeItem1, grayItem11))
    }

    @Test fun sortByAmountAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_ascending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2, orangeItem1, redItem0, grayItem11))
    }

    @Test fun sortByAmountDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_descending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(grayItem11, redItem0, orangeItem1, yellowItem2))
    }

    private var collapsedItemHeight = 0
    @Test fun expandItem() {
        onView(withId(R.id.inventoryRecyclerView)).perform(doStuff<RecyclerView> {
            (it.itemAnimator as ExpandableItemAnimator).notifyExpandedItemChanged(null)
            collapsedItemHeight = it.findViewHolderForAdapterPosition(0)!!.itemView.height
        }).check(onlyExpandedIndexIs(null, collapsedItemHeight))

        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click()))
        ).check(onlyExpandedIndexIs(1, collapsedItemHeight))
    }

    @Test fun expandAnotherItem() {
        expandItem()
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.editButton, click()))
        ).check(onlyExpandedIndexIs(3, collapsedItemHeight))
    }

    private fun expandedItemSurvives(action: Runnable) {
        expandItem()
        action.run()
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlyExpandedIndexIs(1, collapsedItemHeight))
    }

    private fun switchToShoppingListAndBack() {
        onView(withId(R.id.shopping_list_button)).perform(click())
        onView(withId(R.id.inventory_button)).perform(click())
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

    private fun changeOrientationWhileInShoppingList() {
        onView(withId(R.id.shopping_list_button)).perform(click())
        changeOrientationAndBack()
        onView(withId(R.id.inventory_button)).perform(click())
    }

    private fun changeOrientationWhileInPreferences() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.settings_description)).perform(click())
        uiDevice.setOrientationLeft()
        uiDevice.setOrientationNatural()
        onView(withId(R.id.backButton)).perform(click())
    }

    @Test fun expandedItemSurvivesSwitchingToShoppingList() = expandedItemSurvives(::switchToShoppingListAndBack)
    @Test fun expandedItemSurvivesSwitchingToPreferences() = expandedItemSurvives(::switchToPreferencesAndBack)
    @Test fun expandedItemSurvivesOrientationChange() = expandedItemSurvives(::changeOrientationAndBack)
    @Test fun expandedItemSurvivesOrientationChangeWhileInShoppingList() = selectionSurvives(::changeOrientationWhileInShoppingList)
    @Test fun expandedItemSurvivesOrientationChangeWhileInPreferences() = expandedItemSurvives(::changeOrientationWhileInPreferences)

    @Test fun selectIndividualItems() {
        runBlocking { db.inventoryItemDao().clearSelection() }
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick())
        ).check(onlySelectedIndicesAre(1))
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click())
        ).check(onlySelectedIndicesAre(1, 3))
    }

    @Test fun deselectIndividualItems() {
        selectIndividualItems()
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click())
        ).check(onlySelectedIndicesAre(3))
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click())
        ).check(onlySelectedIndicesAre())
    }

    @Test fun selectAll() {
        runBlocking { db.inventoryItemDao().clearSelection() }
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.select_all_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre(0, 1, 2, 3))
    }

    @Test fun deselectAllWithActionBarBackButton() {
        runBlocking { db.inventoryItemDao().selectAll() }
        // This test mysteriously works without the sleep for the shopping list,
        // but not for the inventory?
        Thread.sleep(50L)
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    @Test fun deselectAllWithNavigationBackButton() {
        runBlocking { db.inventoryItemDao().selectAll() }
        pressBack()
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    private fun selectionSurvives(action: Runnable) {
        selectIndividualItems()
        action.run()
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre(1, 3))
    }

    @Test fun selectionSurvivesSwitchingToShoppingList() = selectionSurvives(::switchToShoppingListAndBack)
    @Test fun selectionSurvivesSwitchingToPreferences() = selectionSurvives(::switchToPreferencesAndBack)
    @Test fun selectionSurvivesOrientationChange() = selectionSurvives(::changeOrientationAndBack)
    @Test fun selectionSurvivesOrientationChangeWhileInShoppingList() = selectionSurvives(::changeOrientationWhileInShoppingList)
    @Test fun selectionSurvivesOrientationChangeWhileInPreferences() = selectionSurvives(::changeOrientationWhileInPreferences)

    @Test fun searching() {
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("y"))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2, grayItem11))
    }

    @Test fun addingToExistingSearchQuery() {
        searching()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("e"))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2))
    }

    @Test fun searchingExtraInfo() {
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("extra info"))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1))
    }

    @Test fun clearingSearchQueryViaBackspace() {
        addingToExistingSearchQuery()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(pressKey(KeyEvent.KEYCODE_DEL))
        Thread.sleep(30L) // Test works fine with a small sleep, or if stepping through while debugging
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2, grayItem11))
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(pressKey(KeyEvent.KEYCODE_DEL))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun clearingSearchQueryViaActionBarBackButton() {
        searching()
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("")))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun clearingSearchQueryViaNavigationBackButton() {
        searching()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(closeSoftKeyboard())
        pressBack()
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("")))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    private fun searchQuerySurvives(action: Runnable) {
        searchingExtraInfo()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(closeSoftKeyboard())
        action.run()
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("extra info")))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1))
    }

    @Test fun searchQuerySurvivesSwitchingToShoppingList() = searchQuerySurvives(::switchToShoppingListAndBack)
    @Test fun searchQuerySurvivesSwitchingToPreferences() = searchQuerySurvives(::switchToPreferencesAndBack)
    @Test fun searchQuerySurvivesOrientationChange() = searchQuerySurvives(::changeOrientationAndBack)
    @Test fun searchQuerySurvivesOrientationChangeWhileInShoppingList() = searchQuerySurvives(::changeOrientationWhileInShoppingList)
    @Test fun searchQuerySurvivesOrientationChangeWhileInPreferences() = searchQuerySurvives(::changeOrientationWhileInPreferences)
    @Test fun searchQuerySurvivesSelectionAndDeselection() = searchQuerySurvives(::deselectAllWithActionBarBackButton)
}