/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracertech.bootycrate

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import org.hamcrest.core.IsNot.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class NavigationTests {
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)

    @Test fun startingFragmentVisibility() {
        onView(withId(R.id.shoppingListFragmentView)).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryFragmentView)).check(matches(not(isDisplayed())))
    }

    @Test fun switchingToInventoryFragment() {
        startingFragmentVisibility()
        onView(withId(R.id.inventory_button)).perform(click())
        onView(withId(R.id.shoppingListFragmentView)).check(matches(not(isCompletelyDisplayed())))
        onView(withId(R.id.inventoryFragmentView)).check(matches(isDisplayed()))
    }

    @Test fun switchingBackToShoppingListFragment() {
        switchingToInventoryFragment()
        onView(withId(R.id.shopping_list_button)).perform(click())
        onView(withId(R.id.shoppingListFragmentView)).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryFragmentView)).check(matches(not(isCompletelyDisplayed())))
    }

    @Test fun navigatingToPreferencesFragment() {
        onView(withId(R.id.menuButton)).perform(click())
        onView(withText(R.string.settings_description)).inRoot(isPlatformPopup()).perform(click())
        onView(withText(R.string.pref_light_dark_mode_title)).check(matches(isDisplayed()))
    }

    @Test fun navigatingUpFromPreferencesUsingNavigationBackButton() {
        navigatingToPreferencesFragment()
        pressBack()
        onView(withId(R.id.shoppingListFragmentView)).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryFragmentView)).check(matches(not(isDisplayed())))
        onView(withText(R.string.pref_light_dark_mode_title)).check(doesNotExist())
    }

    @Test fun navigatingUpFromPreferencesUsingActionBarBackButton() {
        navigatingToPreferencesFragment()
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.shoppingListFragmentView)).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryFragmentView)).check(matches(not(isDisplayed())))
        onView(withText(R.string.pref_light_dark_mode_title)).check(doesNotExist())
    }

    @Test fun navigatingToNestedPreferenceScreen() {
        navigatingToPreferencesFragment()
        onView(withText(R.string.pref_update_list_reminder_title)).perform(click())
        onView(withText(R.string.pref_update_list_reminder_title)).check(doesNotExist())
        onView(withText(R.string.update_list_reminder_description)).check(matches(isDisplayed()))
    }

    @Test fun navigatingUpFromNestedPreferenceScreenUsingNavigationBackButton() {
        navigatingToNestedPreferenceScreen()
        pressBack()
        onView(withText(R.string.pref_light_dark_mode_title)).check(matches(isDisplayed()))
        onView(withText(R.string.update_list_reminder_description)).check(doesNotExist())
    }

    @Test fun navigatingUpFromNestedPreferencesScreenUsingActionBarBackButton() {
        navigatingToNestedPreferenceScreen()
        onView(withId(R.id.backButton)).perform(click())
        onView(withText(R.string.pref_light_dark_mode_title)).check(matches(isDisplayed()))
        onView(withText(R.string.update_list_reminder_description)).check(doesNotExist())
    }

}