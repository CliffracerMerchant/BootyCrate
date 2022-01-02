/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.app.Application
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.MainActivity
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemView
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
    @get:Rule var activityRule = ActivityScenarioRule(MainActivity::class.java)
    private val db = BootyCrateDatabase.get(context as Application)
    private val dao = db.itemDao()
    private val groupId = db.run { runBlocking { itemGroupDao().deleteAll() }
                                       itemGroupDao().getAllNow()[0].id }

    private val testItem = ShoppingListItem(color = 5, name = "Test Item 1",
                                            extraInfo = "Test Item 1 Extra Info",
                                            amount = 3)

    @Before fun setup() {
        runBlocking { dao.deleteAllShoppingListItems()
                      dao.deleteAllInventoryItems() }
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
        onView(inNewItemDialog(instanceOf(ExpandableItemView::class.java))).check(matches(isDisplayed()))
        onView(inNewItemDialog(withId(R.id.autoAddToShoppingListCheckBox))).check(doesNotExist())
        onView(withId(R.id.warningMessage)).check(matches(not(isDisplayed())))
        onView(withText(android.R.string.ok)).check(matches(isEnabled()))
        onView(withText(R.string.add_another_item_button_description)).check(matches(isEnabled()))
    }

    @Test fun correctStartingValues() {
        appears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.valueEdit))).check(matches(withText("1")))
        onView(inNewItemDialog(withId(R.id.linkIndicator))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.editButton))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(instanceOf(ExpandableItemView::class.java)))
            .perform(doStuff<ExpandableItemView<ShoppingListItem>> {
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
        onView(inNewItemDialog(instanceOf(ExpandableItemView::class.java)))
            .perform(doStuff<ExpandableItemView<ShoppingListItem>> {
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
        onView(allOf(withId(R.id.warningMessage), withText(
            context.getString(R.string.new_item_no_name_error)))
        ).check(matches(isDisplayed()))
        onView(withText(android.R.string.ok)).check(matches(not(isEnabled())))
        onView(withText(R.string.add_another_item_button_description)).check(matches(not(isEnabled())))
    }

    @Test fun noNameErrorMessageDisappears() {
        noNameErrorMessageAppears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("a"))
        onView(withId(R.id.warningMessage)).check(matches(not(isDisplayed())))
        onView(withText(android.R.string.ok)).check(matches(isEnabled()))
        onView(withText(R.string.add_another_item_button_description)).check(matches(isEnabled()))
    }

    @Test fun duplicateNameWarningAppears() {
        addItem()
        onView(withId(R.id.addButton)).perform(click())
        onView(withId(R.id.warningMessage)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("Test Item 1"))
        onView(withId(R.id.warningMessage)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click(), typeText("Test Item 1 Extra Info"))
        onView(allOf(withId(R.id.warningMessage), withText(
            context.getString(R.string.new_shopping_list_item_duplicate_name_warning)))
        ).check(matches(isDisplayed()))
    }

    @Test fun duplicateNameWarningDisappears() {
        duplicateNameWarningAppears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("a"))
        onView(withId(R.id.warningMessage)).check(matches(not(isDisplayed())))
    }

    @Test fun duplicateNameInOtherListWarningAppears() {
        val item = InventoryItem(name = "Test Item 1", amount = 5,
                                 extraInfo = "Test Item 1 Extra Info")
        runBlocking { dao.add(item.toDbListItem(groupId)) }
        onView(withId(R.id.addButton)).perform(click())
        onView(withId(R.id.warningMessage)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("Test Item 1"))
        onView(withId(R.id.warningMessage)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit)))
            .perform(click(), typeText("Test Item 1 Extra Info"))
        onView(allOf(withId(R.id.warningMessage), withText(
            context.getString(R.string.new_shopping_list_item_will_not_be_linked_warning,
                context.getString(R.string.add_to_shopping_list_description))))
        ).check(matches(isDisplayed()))
    }

    @Test fun duplicateNameInOtherListWarningDisappears() {
        duplicateNameInOtherListWarningAppears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("a"))
        onView(withId(R.id.warningMessage)).check(matches(not(isDisplayed())))
    }

    @Test fun addItem() {
        appears()
        addTestShoppingListItems(leaveDialogOpen = false, testItem)
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlyShownShoppingListItemsAre(testItem))
    }

    @Test fun addSeveralItems() {
        appears()
        val testItem2 = ShoppingListItem(name = "Test Item 2", color = 7, amount = 8,
                                         extraInfo = "Test Item 2 Extra Info")
        addTestShoppingListItems(leaveDialogOpen = false, testItem, testItem2)
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(testItem, testItem2))
    }
}