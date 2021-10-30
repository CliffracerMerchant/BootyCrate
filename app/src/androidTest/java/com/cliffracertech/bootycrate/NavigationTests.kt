/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.cliffracertech.bootycrate.activity.MainActivity
import com.cliffracertech.bootycrate.utils.*
import com.cliffracertech.bootycrate.view.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsNot.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class NavigationTests {
    @get:Rule var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test fun startingFragmentVisibility() {
        onView(withId(R.id.shoppingListFragmentView)).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryItemFragmentView)).check(matches(not(isDisplayed())))
    }

    @Test fun switchingToInventoryItemFragment() {
        startingFragmentVisibility()
        onView(withId(R.id.inventoryButton)).perform(click())
        onView(withId(R.id.shoppingListFragmentView)).check(matches(not(isCompletelyDisplayed())))
        onView(withId(R.id.inventoryItemFragmentView)).check(matches(isDisplayed()))
    }

    @Test fun switchingBackToShoppingListFragment() {
        switchingToInventoryItemFragment()
        onView(withId(R.id.shoppingListButton)).perform(click())
        onView(withId(R.id.shoppingListFragmentView)).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryItemFragmentView)).check(matches(not(isCompletelyDisplayed())))
    }

    @Test fun navigatingToSettingsFragment() {
        onView(withId(R.id.bottomNavigationDrawer)).perform(setExpandedAndWaitForSettling())
        onView(withId(R.id.settingsButton)).perform(click())
        onView(withText(R.string.pref_light_dark_mode_title)).check(matches(isDisplayed()))
    }

    @Test fun navigatingUpFromSettingsUsingNavigationBackButton() {
        navigatingToSettingsFragment()
        pressBack()
        onView(withId(R.id.shoppingListFragmentView)).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryItemFragmentView)).check(matches(not(isDisplayed())))
        onView(withText(R.string.pref_light_dark_mode_title)).check(doesNotExist())
    }

    @Test fun navigatingUpFromSettingsUsingActionBarBackButton() {
        navigatingToSettingsFragment()
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.shoppingListFragmentView)).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryItemFragmentView)).check(matches(not(isDisplayed())))
        onView(withText(R.string.pref_light_dark_mode_title)).check(doesNotExist())
    }

    @Test fun navigatingToNestedSettingsScreen() {
        navigatingToSettingsFragment()
        onView(withText(R.string.pref_update_list_reminder_title)).perform(click())
        onView(withText(R.string.pref_update_list_reminder_title)).check(doesNotExist())
        onView(withText(R.string.update_list_reminder_description)).check(matches(isDisplayed()))
    }

    @Test fun navigatingUpFromNestedSettingsScreenUsingNavigationBackButton() {
        navigatingToNestedSettingsScreen()
        pressBack()
        onView(withText(R.string.pref_light_dark_mode_title)).check(matches(isDisplayed()))
        onView(withText(R.string.update_list_reminder_description)).check(doesNotExist())
    }

    @Test fun navigatingUpFromNestedSettingsScreenUsingActionBarBackButton() {
        navigatingToNestedSettingsScreen()
        onView(withId(R.id.backButton)).perform(click())
        onView(withText(R.string.pref_light_dark_mode_title)).check(matches(isDisplayed()))
        onView(withText(R.string.update_list_reminder_description)).check(doesNotExist())
    }

    @Test fun navigationDrawerCollapsedState() {
        onView(withId(R.id.bottomNavigationDrawer)).check(matches(allOf(isDisplayed(), not(isCompletelyDisplayed()))))
        onView(withId(R.id.appTitle)).check(matches(not(isDisplayed())))
        onView(withId(R.id.settingsButton)).check(matches(not(isDisplayed())))
        onView(withId(R.id.inventorySelector)).check(matches(allOf(hasAlpha(0f), not(isCompletelyDisplayed()))))
        onView(withId(R.id.bottomNavigationView)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomAppBar)).perform(doStuff<BottomAppBar> {
            assertThat(it.interpolation).isEqualTo(1f)
            assertThat(it.navIndicator.alpha).isEqualTo(1f)
        })
    }

    @Test fun navigationDrawerExpandedState() {
        navigationDrawerCollapsedState()
        onView(withId(R.id.bottomNavigationDrawer)).perform(setExpandedAndWaitForSettling())

        onView(withId(R.id.bottomNavigationDrawer)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.appTitle)).check(matches(allOf(isCompletelyDisplayed(), hasAlpha(1f))))
        onView(withId(R.id.settingsButton)).check(matches(allOf(isCompletelyDisplayed(), hasAlpha(1f))))
        onView(withId(R.id.inventorySelector)).check(matches(allOf(isCompletelyDisplayed(), hasAlpha(1f))))
        onView(withId(R.id.bottomNavigationView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.bottomAppBar)).perform(doStuff<BottomAppBar> {
            assertThat(it.interpolation).isEqualTo(0f)
            assertThat(it.navIndicator.alpha).isEqualTo(0f)
        })
    }

    @Test fun navigationDrawerCollapsesAfterExpandedState() {
        navigationDrawerExpandedState()
        onView(withId(R.id.bottomNavigationDrawer)).perform(setCollapsedAndWaitForSettling())
        navigationDrawerCollapsedState()
    }

    @Test fun navigationDrawerHidesInSettings() {
        navigationDrawerCollapsedState()
        navigatingToSettingsFragment()
        Thread.sleep(500L)
        onView(withId(R.id.bottomNavigationDrawer)).check(
            matches(hasSheetState(BottomSheetBehavior.STATE_HIDDEN)))
    }

    @Test fun navigationDrawerUnhidesAfterLeavingSettings() {
        navigationDrawerHidesInSettings()
        onView(withId(R.id.backButton)).perform(click())
        navigationDrawerCollapsedState()
    }
}