/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
}