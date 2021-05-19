/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.app.Application
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cliffracertech.bootycrate.ExpandableSelectableItemView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

fun inNewItemDialog(matcher: Matcher<View>) =
    allOf(matcher, isDescendantOfA(withId(R.id.newItemViewContainer)))

class NewShoppingListItemDialogTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)
    private val db = BootyCrateDatabase.get(context as Application)

    private val testItem = ShoppingListItem(color = 5, name = "Test Item 1", amount = 3,
                                            extraInfo = "Test Item 1 Extra Info")

    @Before fun setup() {
        runBlocking {
            db.shoppingListItemDao().deleteAll()
            db.inventoryItemDao().deleteAll()
        }
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
    }

    private fun addTestShoppingListItems(leaveDialogOpen: Boolean, vararg items: ShoppingListItem) {
        val lastItem = items.last()
        for (item in items) {
            onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText(item.name))
            onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click(), typeText(item.extraInfo))
            onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
            onView(withId(R.id.colorSheetList)).perform(
                actionsOnItemAtPosition(item.color, click()))
            for (i in 1 until item.amount)
                onView(inNewItemDialog(withId(R.id.increaseButton))).perform(click())
            if (item != lastItem)
                onView(withText(R.string.add_another_item_button_description)).perform(click())
            else if (!leaveDialogOpen)
                onView(withText(android.R.string.ok)).perform(click())
        }
    }

    @Test fun appears() {
        onView(withId(R.id.addButton)).perform(click())
        onView(withId(R.id.newItemViewContainer)).check(matches(isDisplayed()))
        onView(inNewItemDialog(instanceOf(ExpandableSelectableItemView::class.java))).check(matches(isDisplayed()))
        onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).check(doesNotExist())
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
        onView(withText(R.string.new_item_no_name_error)).check(matches(not(isDisplayed())))
    }

    @Test fun correctStartingValues() {
        appears()
        assertCorrectStartingValues()
    }

    private fun assertCorrectStartingValues() {
        onView(inNewItemDialog(withId(R.id.nameEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.valueEdit))).check(matches(withText("1")))
        onView(inNewItemDialog(withId(R.id.linkIndicator))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.editButton))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(instanceOf(ExpandableSelectableItemView::class.java)))
            .perform(doStuff<ExpandableSelectableItemView<ShoppingListItem>> {
                assertThat(it.ui.checkBox.colorIndex).isEqualTo(0)
                assertThat(it.ui.checkBox.inColorEditMode).isTrue()
                assertThat(it.ui.nameEdit.isEditable).isTrue()
                assertThat(it.ui.extraInfoEdit.isEditable).isTrue()
                assertThat(it.ui.amountEdit.valueIsFocusable).isTrue()
                assertThat(it.ui.amountEdit.minValue).isEqualTo(1)
            })
    }

    @Test fun correctValuesAfterAddAnother() {
        correctStartingValues()
        addTestShoppingListItems(leaveDialogOpen = true, testItem)
        onView(withText(R.string.add_another_item_button_description)).perform(click())

        onView(inNewItemDialog(withId(R.id.nameEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.valueEdit))).check(matches(withText("1")))
        onView(inNewItemDialog(withId(R.id.linkIndicator))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.editButton))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(instanceOf(ExpandableSelectableItemView::class.java)))
            .perform(doStuff<ExpandableSelectableItemView<ShoppingListItem>> {
                // The color edit is intended to stay the same value after the add another button
                // is pressed, but the rest of the fields should be reset to their default values.
                assertThat(it.ui.checkBox.colorIndex).isEqualTo(testItem.color)
                assertThat(it.ui.checkBox.inColorEditMode).isTrue()
                assertThat(it.ui.nameEdit.isEditable).isTrue()
                assertThat(it.ui.extraInfoEdit.isEditable).isTrue()
                assertThat(it.ui.amountEdit.valueIsFocusable).isTrue()
                assertThat(it.ui.amountEdit.minValue).isEqualTo(1)
            })
    }

    @Test fun noNameErrorMessageAppears() {
        appears()
        onView(withText(android.R.string.ok)).perform(click())
        onView(withText(R.string.new_item_no_name_error)).check(matches(isDisplayed()))
    }
    @Test fun noNameErrorMessageDisappears() {
        noNameErrorMessageAppears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("a"))
        onView(withText(R.string.new_item_no_name_error)).check(matches(not(isDisplayed())))
    }
    @Test fun noNameErrorMessageAppearsAfterHavingAlreadyDisappeared() {
        noNameErrorMessageDisappears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(clearText())
        onView(withText(android.R.string.ok)).perform(click())
        onView(withText(R.string.new_item_no_name_error)).check(matches(isDisplayed()))
    }

    @Test fun duplicateNameWarningAppears() {
        addItem()
        onView(withId(R.id.addButton)).perform(click())
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("Test Item 1"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click(), typeText("Test Item 1 Extra Info"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(isDisplayed()))
    }
    @Test fun duplicateNameWarningDisappears() {
        duplicateNameWarningAppears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("a"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
    }
    @Test fun duplicateNameWarningAppearsAfterHavingAlreadyDisappeared() {
        duplicateNameWarningDisappears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(clearText(), typeText("Test Item 1"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(isDisplayed()))
    }

    @Test fun addItem() {
        appears()
        addTestShoppingListItems(leaveDialogOpen = false, testItem)
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlyShownShoppingListItemsAre(testItem))
    }

    @Test fun addSeveralItems() {
        appears()
        val testItem2 = ShoppingListItem(name = "Test Item 2", extraInfo = "Test Item 2 Extra Info",
                                         color = 7, amount = 8)
        addTestShoppingListItems(leaveDialogOpen = false, testItem, testItem2)
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(testItem, testItem2))
    }
}