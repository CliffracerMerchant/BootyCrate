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
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.database.ShoppingListViewModel
import com.cliffracertech.bootycrate.recyclerview.ShoppingListRecyclerView
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test

fun inNewItemDialog(matcher: Matcher<View>) =
    allOf(matcher, isDescendantOfA(withId(R.id.newItemViewContainer)))

class NewShoppingListItemDialogTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)
    private val viewModel = ShoppingListViewModel(context as Application)

    private fun addTestShoppingListItems(leaveNewItemDialogOpen: Boolean, vararg items: ShoppingListItem) {
        val lastItem = items.last()
        for (item in items) {
            onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText(item.name))
            onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click(), typeText(item.extraInfo))
            onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
            onView(withId(R.id.colorSheetList)).perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(item.color, click()))
            for (i in 1 until item.amount)
                onView(inNewItemDialog(withId(R.id.increaseButton))).perform(click())
            if (item != lastItem)
                onView(withText(R.string.add_another_item_button_description)).perform(click())
            else if (!leaveNewItemDialogOpen)
                onView(withText(android.R.string.ok)).perform(click())
        }
    }

    @Test fun appears() {
        onView(withId(R.id.add_button)).perform(click())
        onView(withId(R.id.newItemViewContainer)).check(matches(isDisplayed()))
        onView(inNewItemDialog(instanceOf(ExpandableSelectableItemView::class.java))).check(matches(isDisplayed()))
        onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).check(doesNotExist())
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
        onView(withText(R.string.new_item_no_name_error)).check(matches(not(isDisplayed())))
    }

    @Test fun correctStartingValues() {
        appears()
        testCorrectStartingValues()
    }

    private fun testCorrectStartingValues() {
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
        viewModel.deleteAll()
        correctStartingValues()
        val testItem = ShoppingListItem(name = "Test Item 1",
                                        extraInfo = "Test Item 1 Extra Info",
                                        color = 5, amount = 3)
        addTestShoppingListItems(leaveNewItemDialogOpen = true, testItem)
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
        viewModel.deleteAll()
        addItem()
        onView(withId(R.id.add_button)).perform(click())
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
        viewModel.deleteAll()
        appears()
        val testItem = ShoppingListItem(name = "Test Item 1",
                                        extraInfo = "Test Item 1 Extra Info",
                                        color = 5, amount = 3)
        addTestShoppingListItems(leaveNewItemDialogOpen = false, testItem)

        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(testItem)
        })
    }

    @Test fun addSeveralItems() {
        viewModel.deleteAll()
        appears()
        val testItem1 = ShoppingListItem(name = "Test Item 1",
                                         extraInfo = "Test Item 1 Extra Info",
                                         color = 5, amount = 3)
        val testItem2 = ShoppingListItem(name = "Test Item 2",
                                         extraInfo = "Test Item 2 Extra Info",
                                         color = 7, amount = 8)
        addTestShoppingListItems(leaveNewItemDialogOpen = false, testItem1, testItem2)

        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<ShoppingListRecyclerView> {
            assertThat(it.itemFromVhAtPos(0)).isEqualTo(testItem1)
            assertThat(it.itemFromVhAtPos(1)).isEqualTo(testItem2)
        })
    }
}