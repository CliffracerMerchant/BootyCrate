/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.InventoryViewModel
import com.cliffracertech.bootycrate.utils.actionsOnItemAtPosition
import com.cliffracertech.bootycrate.utils.doStuff
import com.cliffracertech.bootycrate.utils.onlyShownInventoryItemsAre
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test

class NewInventoryItemDialogTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)
    private val viewModel = InventoryViewModel(context as Application)

    private val testItem = InventoryItem(name = "Test Item 1", extraInfo = "Test Extra Info 1",
                                         color = 5, amount = 3, addToShoppingList = true,
                                         addToShoppingListTrigger = 4)

    private fun amountIncreaseButton() = CoreMatchers.allOf(withId(R.id.increaseButton),
                                                            isDescendantOfA(withId(R.id.amountEdit)))
    private fun autoAddTriggerIncreaseButton() = CoreMatchers.allOf(withId(R.id.increaseButton),
                                                                    isDescendantOfA(withId(R.id.addToShoppingListTriggerEdit)))

    private fun addTestInventoryItems(leaveDialogOpen: Boolean, vararg items: InventoryItem) {
        val lastItem = items.last()
        for (item in items) {
            onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText(item.name))
            onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click(), typeText(item.extraInfo))
            onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
            onView(withId(R.id.colorSheetList)).perform(
                actionsOnItemAtPosition(item.color, click()))
            val amountIncreaseButton = inNewItemDialog(amountIncreaseButton())
            for (i in 1 until item.amount)
                onView(amountIncreaseButton).perform(click())
            onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).perform(click())
            val autoAddTriggerIncreaseButton = inNewItemDialog(autoAddTriggerIncreaseButton())
            for (i in 1 until item.addToShoppingListTrigger)
                onView(autoAddTriggerIncreaseButton).perform(click())
            if (item != lastItem)
                onView(withText(R.string.add_another_item_button_description)).perform(click())
            else if (!leaveDialogOpen)
                onView(withText(android.R.string.ok)).perform(click())
        }
    }

    @Test fun appears() {
        onView(withId(R.id.inventoryButton)).perform(click())
        onView(withId(R.id.addButton)).perform(click())
        onView(withId(R.id.newItemViewContainer)).check(matches(isDisplayed()))
        onView(inNewItemDialog(CoreMatchers.instanceOf(InventoryItemView::class.java)))
            .check(matches(isDisplayed()))
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
        onView(inNewItemDialog(withId(R.id.linkIndicator))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.editButton))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).check(matches(isNotChecked()))
        onView(inNewItemDialog(CoreMatchers.instanceOf(InventoryItemView::class.java)))
            .perform(doStuff<InventoryItemView> {
                assertThat(it.ui.checkBox.colorIndex).isEqualTo(0)
                assertThat(it.ui.checkBox.inColorEditMode).isTrue()
                assertThat(it.ui.nameEdit.isEditable).isTrue()
                assertThat(it.ui.extraInfoEdit.isEditable).isTrue()
                assertThat(it.ui.amountEdit.value).isEqualTo(1)
                assertThat(it.ui.amountEdit.minValue).isEqualTo(0)
                assertThat(it.ui.amountEdit.valueIsFocusable).isTrue()
                assertThat(it.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(1)
                assertThat(it.detailsUi.addToShoppingListTriggerEdit.minValue).isEqualTo(1)
                assertThat(it.detailsUi.addToShoppingListTriggerEdit.valueIsFocusable).isTrue()
            })
    }

    @Test fun correctValuesAfterAddAnother() {
        viewModel.deleteAll()
        correctStartingValues()

        addTestInventoryItems(leaveDialogOpen = true, testItem)
        onView(withText(R.string.add_another_item_button_description)).perform(click())

        onView(inNewItemDialog(withId(R.id.nameEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.linkIndicator))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.editButton))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).check(matches(isNotChecked()))
        onView(inNewItemDialog(CoreMatchers.instanceOf(InventoryItemView::class.java)))
            .perform(doStuff<InventoryItemView> {
                // The color edit is intended to stay the same value after the add another button
                // is pressed, but the rest of the fields should be reset to their default values.
                assertThat(it.ui.checkBox.colorIndex).isEqualTo(testItem.color)
                assertThat(it.ui.checkBox.inColorEditMode).isTrue()
                assertThat(it.ui.nameEdit.isEditable).isTrue()
                assertThat(it.ui.extraInfoEdit.isEditable).isTrue()
                assertThat(it.ui.amountEdit.value).isEqualTo(1)
                assertThat(it.ui.amountEdit.valueIsFocusable).isTrue()
                assertThat(it.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(1)
                assertThat(it.detailsUi.addToShoppingListTriggerEdit.valueIsFocusable).isTrue()
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
        onView(withId(R.id.addButton)).perform(click())
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("Test Item 1"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit)))
            .perform(click(), typeText("Test Item 1 Extra Info"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(isDisplayed()))
    }
    @Test fun duplicateNameWarningDisappears() {
        duplicateNameWarningAppears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("a"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
    }
    @Test fun duplicateNameWarningAppearsAfterHavingAlreadyDisappeared() {
        duplicateNameWarningAppears()
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(clearText(), typeText("Test Item 1"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(isDisplayed()))
    }

    @Test fun addItem() {
        viewModel.deleteAll()
        appears()
        val testItem = InventoryItem(name = "Test Item 1", extraInfo = "Test Item 1 Extra Info",
                                     color = 5, amount = 3, addToShoppingList = true,
                                     addToShoppingListTrigger = 4)
        addTestInventoryItems(leaveDialogOpen = false, testItem)
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlyShownInventoryItemsAre(testItem))
    }

    @Test fun addSeveralItems() {
        viewModel.deleteAll()
        appears()
        val testItem2 = InventoryItem(name = "Test Item 2", extraInfo = "Test Item 2 Extra Info",
                                      color = 7, amount = 8, addToShoppingList = true,
                                      addToShoppingListTrigger = 2)
        addTestInventoryItems(leaveDialogOpen = false, testItem, testItem2)
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(testItem, testItem2))
    }
}