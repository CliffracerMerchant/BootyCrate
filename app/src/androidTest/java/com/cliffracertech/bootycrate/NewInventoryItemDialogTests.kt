/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.app.Application
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.InventoryViewModel
import com.cliffracertech.bootycrate.recyclerview.InventoryRecyclerView
import com.google.common.truth.Truth
import org.hamcrest.CoreMatchers
import org.junit.Rule
import org.junit.Test

class NewInventoryItemDialogTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)
    private val viewModel = InventoryViewModel(context as Application)

    private fun amountIncreaseButton() = CoreMatchers.allOf(
        ViewMatchers.withId(R.id.increaseButton),
        ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.amountEdit))
    )
    private fun autoAddTriggerIncreaseButton() = CoreMatchers.allOf(
        ViewMatchers.withId(R.id.increaseButton),
        ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.addToShoppingListTriggerEdit))
    )

    private fun addTestInventoryItems(leaveNewItemDialogOpen: Boolean, vararg items: InventoryItem) {
        val lastItem = items.last()
        for (item in items) {
            Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.nameEdit)))
                .perform(ViewActions.click(), ViewActions.typeText(item.name))
            Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.extraInfoEdit)))
                .perform(ViewActions.click(), ViewActions.typeText(item.extraInfo))
            Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.checkBox)))
                .perform(ViewActions.click())
            Espresso.onView(ViewMatchers.withId(R.id.colorSheetList)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    item.color,
                    ViewActions.click()
                )
            )
            val amountIncreaseButton = inNewItemDialog(amountIncreaseButton())
            for (i in 1 until item.amount)
                Espresso.onView(amountIncreaseButton).perform(ViewActions.click())
            Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.addToShoppingListCheckBox)))
                .perform(ViewActions.click())
            val autoAddTriggerIncreaseButton = inNewItemDialog(autoAddTriggerIncreaseButton())
            for (i in 1 until item.addToShoppingListTrigger)
                Espresso.onView(autoAddTriggerIncreaseButton).perform(ViewActions.click())
            if (item != lastItem)
                Espresso.onView(ViewMatchers.withText(R.string.add_another_item_button_description))
                    .perform(ViewActions.click())
            else if (!leaveNewItemDialogOpen)
                Espresso.onView(ViewMatchers.withText(android.R.string.ok))
                    .perform(ViewActions.click())
        }
    }

    @Test
    fun appears() {
        Espresso.onView(ViewMatchers.withId(R.id.inventory_button)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.add_button)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.newItemViewContainer))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(inNewItemDialog(CoreMatchers.instanceOf(InventoryItemView::class.java)))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.new_item_duplicate_name_warning))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        Espresso.onView(ViewMatchers.withText(R.string.new_item_no_name_error))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
    }

    @Test
    fun correctStartingValues() {
        appears()
        testCorrectStartingValues()
    }

    private fun testCorrectStartingValues() {
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.nameEdit)))
            .check(ViewAssertions.matches(ViewMatchers.withText("")))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.extraInfoEdit)))
            .check(ViewAssertions.matches(ViewMatchers.withText("")))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.linkIndicator)))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.editButton)))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.addToShoppingListCheckBox)))
            .check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
        Espresso.onView(inNewItemDialog(CoreMatchers.instanceOf(InventoryItemView::class.java)))
            .perform(doStuff<InventoryItemView> {
                Truth.assertThat(it.ui.checkBox.colorIndex).isEqualTo(0)
                Truth.assertThat(it.ui.checkBox.inColorEditMode).isTrue()
                Truth.assertThat(it.ui.nameEdit.isEditable).isTrue()
                Truth.assertThat(it.ui.extraInfoEdit.isEditable).isTrue()
                Truth.assertThat(it.ui.amountEdit.value).isEqualTo(1)
                Truth.assertThat(it.ui.amountEdit.minValue).isEqualTo(0)
                Truth.assertThat(it.ui.amountEdit.valueIsFocusable).isTrue()
                Truth.assertThat(it.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(1)
                Truth.assertThat(it.detailsUi.addToShoppingListTriggerEdit.minValue).isEqualTo(1)
                Truth.assertThat(it.detailsUi.addToShoppingListTriggerEdit.valueIsFocusable)
                    .isTrue()
            })
    }

    @Test
    fun correctValuesAfterAddAnother() {
        viewModel.deleteAll()
        correctStartingValues()
        val testItem = InventoryItem(
            name = "Test Item 1",
            extraInfo = "Test Extra Info 1",
            color = 5, amount = 3,
            addToShoppingList = true,
            addToShoppingListTrigger = 4
        )
        addTestInventoryItems(leaveNewItemDialogOpen = true, testItem)
        Espresso.onView(ViewMatchers.withText(R.string.add_another_item_button_description))
            .perform(ViewActions.click())

        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.nameEdit)))
            .check(ViewAssertions.matches(ViewMatchers.withText("")))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.extraInfoEdit)))
            .check(ViewAssertions.matches(ViewMatchers.withText("")))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.linkIndicator)))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.editButton)))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.addToShoppingListCheckBox)))
            .check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
        Espresso.onView(inNewItemDialog(CoreMatchers.instanceOf(InventoryItemView::class.java)))
            .perform(doStuff<InventoryItemView> {
                // The color edit is intended to stay the same value after the add another button
                // is pressed, but the rest of the fields should be reset to their default values.
                Truth.assertThat(it.ui.checkBox.colorIndex).isEqualTo(testItem.color)
                Truth.assertThat(it.ui.checkBox.inColorEditMode).isTrue()
                Truth.assertThat(it.ui.nameEdit.isEditable).isTrue()
                Truth.assertThat(it.ui.extraInfoEdit.isEditable).isTrue()
                Truth.assertThat(it.ui.amountEdit.value).isEqualTo(1)
                Truth.assertThat(it.ui.amountEdit.valueIsFocusable).isTrue()
                Truth.assertThat(it.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(1)
                Truth.assertThat(it.detailsUi.addToShoppingListTriggerEdit.valueIsFocusable)
                    .isTrue()
            })
    }

    @Test
    fun noNameErrorMessageAppears() {
        appears()
        Espresso.onView(ViewMatchers.withText(android.R.string.ok)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.new_item_no_name_error))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    @Test
    fun noNameErrorMessageDisappears() {
        noNameErrorMessageAppears()
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.nameEdit)))
            .perform(ViewActions.click(), ViewActions.typeText("a"))
        Espresso.onView(ViewMatchers.withText(R.string.new_item_no_name_error))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
    }
    @Test
    fun noNameErrorMessageAppearsAfterHavingAlreadyDisappeared() {
        noNameErrorMessageDisappears()
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.nameEdit)))
            .perform(ViewActions.clearText())
        Espresso.onView(ViewMatchers.withText(android.R.string.ok)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.new_item_no_name_error))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun duplicateNameWarningAppears() {
        viewModel.deleteAll()
        addItem()
        Espresso.onView(ViewMatchers.withId(R.id.add_button)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.new_item_duplicate_name_warning))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.nameEdit)))
            .perform(ViewActions.click(), ViewActions.typeText("Test Item 1"))
        Espresso.onView(ViewMatchers.withText(R.string.new_item_duplicate_name_warning))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.extraInfoEdit)))
            .perform(ViewActions.click(), ViewActions.typeText("Test Item 1 Extra Info"))
        Espresso.onView(ViewMatchers.withText(R.string.new_item_duplicate_name_warning))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
    @Test
    fun duplicateNameWarningDisappears() {
        duplicateNameWarningAppears()
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.nameEdit)))
            .perform(ViewActions.click(), ViewActions.typeText("a"))
        Espresso.onView(ViewMatchers.withText(R.string.new_item_duplicate_name_warning))
            .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
    }
    @Test
    fun duplicateNameWarningAppearsAfterHavingAlreadyDisappeared() {
        duplicateNameWarningAppears()
        Espresso.onView(inNewItemDialog(ViewMatchers.withId(R.id.nameEdit)))
            .perform(ViewActions.clearText(), ViewActions.typeText("Test Item 1"))
        Espresso.onView(ViewMatchers.withText(R.string.new_item_duplicate_name_warning))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun addItem() {
        viewModel.deleteAll()
        appears()
        val testItem = InventoryItem(
            name = "Test Item 1",
            extraInfo = "Test Item 1 Extra Info",
            color = 5, amount = 3,
            addToShoppingList = true,
            addToShoppingListTrigger = 4
        )
        addTestInventoryItems(leaveNewItemDialogOpen = false, testItem)

        Espresso.onView(ViewMatchers.withId(R.id.inventoryRecyclerView))
            .perform(doStuff<InventoryRecyclerView> {
                Truth.assertThat(it.itemFromVhAtPos(0)).isEqualTo(testItem)
            })
    }

    @Test
    fun addSeveralItems() {
        viewModel.deleteAll()
        appears()
        val testItem1 = InventoryItem(
            name = "Test Item 1",
            extraInfo = "Test Item 1 Extra Info",
            color = 5, amount = 3,
            addToShoppingList = true,
            addToShoppingListTrigger = 4
        )
        val testItem2 = InventoryItem(
            name = "Test Item 2",
            extraInfo = "Test Item 2 Extra Info",
            color = 7, amount = 8,
            addToShoppingList = true,
            addToShoppingListTrigger = 2
        )
        addTestInventoryItems(leaveNewItemDialogOpen = false, testItem1, testItem2)

        Espresso.onView(ViewMatchers.withId(R.id.inventoryRecyclerView))
            .perform(doStuff<InventoryRecyclerView> {
                Truth.assertThat(it.itemFromVhAtPos(0)).isEqualTo(testItem1)
                Truth.assertThat(it.itemFromVhAtPos(1)).isEqualTo(testItem2)
            })
    }
}