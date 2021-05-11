/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.app.Application
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemAnimator
import com.cliffracertech.bootycrate.recyclerview.InventoryRecyclerView
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
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

    private val redItem0 = InventoryItem(name = "Red", color = 0, amount = 8)
    private val orangeItem1 = InventoryItem(name = "Orange", color = 1, amount = 2)
    private val yellowItem2 = InventoryItem(name = "Yellow", color = 2, amount = 1)
    private val grayItem11 = InventoryItem(name = "Gray", color = 11, amount = 9)

    @Before fun setup() {
        runBlocking {
            db.inventoryItemDao().deleteAll()
            db.inventoryItemDao().add(listOf(redItem0, orangeItem1, yellowItem2, grayItem11))
        }
        onView(withId(R.id.inventory_button)).perform(click())
    }

    @Test fun sortByColor() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).perform(doStuff<InventoryRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(yellowItem2)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(grayItem11)
        })
    }

    @Test fun sortByNameAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_ascending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).perform(doStuff<InventoryRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(grayItem11)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(yellowItem2)
        })
    }

    @Test fun sortByNameDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_descending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).perform(doStuff<InventoryRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(yellowItem2)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(grayItem11)
        })
    }

    @Test fun sortByAmountAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_ascending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).perform(doStuff<InventoryRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(yellowItem2)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(grayItem11)
        })
    }

    @Test fun sortByAmountDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_descending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).perform(doStuff<InventoryRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(grayItem11)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(redItem0)
            assertThat(it.itemFromVhAtPos(2)).isEqualTo(orangeItem1)
            assertThat(it.itemFromVhAtPos(3)).isEqualTo(yellowItem2)
        })
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

    @Test fun selectAllItems() {
        runBlocking { db.inventoryItemDao().clearSelection() }
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.select_all_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre(0, 1, 2, 3))
    }

    @Test fun deselectAllItemsWithActionBarBackButton() {
        runBlocking { db.inventoryItemDao().selectAll() }
        // This test mysteriously works without the sleep for the shopping list,
        // but not for the inventory?
        Thread.sleep(50L)
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    @Test fun deselectAllItemsWithNavigationBackButton() {
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
    @Test fun selectionSurvivesOrientationChangeWhileInPreferences() = selectionSurvives(::changeOrientationWhileInPreferences)
}