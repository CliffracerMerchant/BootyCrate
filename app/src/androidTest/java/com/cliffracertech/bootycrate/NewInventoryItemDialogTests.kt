/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.google.common.truth.Truth.assertThat
import junit.framework.AssertionFailedError
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

class NewInventoryItemDialogTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)

    @Test fun appears() {
        onView(withId(R.id.inventory_button)).perform(click())
        onView(withId(R.id.add_button)).perform(click())
        onView(withId(R.id.newItemViewContainer)).check(matches(isDisplayed()))
        onView(inNewItemDialog(instanceOf(InventoryItemView::class.java))).check(matches(isDisplayed()))
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
        onView(inNewItemDialog(instanceOf(InventoryItemView::class.java))).perform(callMethod<InventoryItemView> {
            assertThat(it.ui.checkBox.colorIndex).isEqualTo(0)
            assertThat(it.ui.checkBox.inColorEditMode).isTrue()
            assertThat(it.ui.nameEdit.isEditable).isTrue()
            assertThat(it.ui.extraInfoEdit.isEditable).isTrue()
            assertThat(it.ui.amountEdit.value).isEqualTo(1)
            assertThat(it.ui.amountEdit.valueIsFocusable).isTrue()
            assertThat(it.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(1)
            assertThat(it.detailsUi.addToShoppingListTriggerEdit.valueIsFocusable).isTrue()
            assertThat(it.ui.amountEdit.minValue).isEqualTo(0)
        })
    }

    @Test fun correctValuesAfterAddAnother() {
        deleteAllItems()
        correctStartingValues()
        val testItemName = "Test Item 1"
        val testItemExtraInfo = "Test Extra Info 1"
        val testItemColorIndex = 5
        val testItemAmount = 3
        val testItemAddToShoppingListAmount = 4
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click()).perform(typeText(testItemName))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click()).perform(typeText(testItemExtraInfo))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(testItemColorIndex, click()))
        val amountIncreaseButton = inNewItemDialog(allOf(
            withId(R.id.increaseButton),
            isDescendantOfA(withId(R.id.amountEdit))))
        for (i in 1 until testItemAmount)
            onView(amountIncreaseButton).perform(click())
        onView(withId(R.id.addToShoppingListCheckBox)).perform(click())
        val autoAddTriggerIncreaseButton = inNewItemDialog(allOf(
            withId(R.id.increaseButton),
            isDescendantOfA(withId(R.id.addToShoppingListTriggerEdit))))
        for (i in 1 until testItemAddToShoppingListAmount)
            onView(autoAddTriggerIncreaseButton).perform(click())
        onView(withText(R.string.add_another_item_button_description)).perform(click())

        onView(inNewItemDialog(withId(R.id.nameEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).check(matches(withText("")))
        onView(inNewItemDialog(withId(R.id.linkIndicator))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.editButton))).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).check(matches(isNotChecked()))
        onView(inNewItemDialog(instanceOf(InventoryItemView::class.java))).perform(callMethod<InventoryItemView> {
            // The color edit is intended to stay the same value after the add another button
            // is pressed, but the rest of the fields should be reset to their default values.
            assertThat(it.ui.checkBox.colorIndex).isEqualTo(testItemColorIndex)
            assertThat(it.ui.checkBox.inColorEditMode).isTrue()
            assertThat(it.ui.nameEdit.isEditable).isTrue()
            assertThat(it.ui.extraInfoEdit.isEditable).isTrue()
            assertThat(it.ui.amountEdit.valueIsFocusable).isTrue()
            assertThat(it.detailsUi.addToShoppingListTriggerEdit.valueIsFocusable).isTrue()
            assertThat(it.ui.amountEdit.minValue).isEqualTo(0)
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
        onView(withId(R.id.add_button)).perform(click())
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click(), typeText("Test Item 1"))
        onView(withText(R.string.new_item_duplicate_name_warning)).check(matches(not(isDisplayed())))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click(), typeText("Test Extra Info 1"))
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
        deleteAllItems()
        appears()
        val testItemColorIndex = 5
        val testItemAmount = 3
        val testItemAddToShoppingListAmount = 4

        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click()).perform(typeText("Test Item 1"))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click()).perform(typeText("Test Extra Info 1"))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(testItemColorIndex, click()))
        val amountIncreaseButton = inNewItemDialog(allOf(withId(R.id.increaseButton),
                                                         isDescendantOfA(withId(R.id.amountEdit))))
        for (i in 1 until testItemAmount)
            onView(amountIncreaseButton).perform(click())
        onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).perform(click())
        val autoAddTriggerIncreaseButton = inNewItemDialog(allOf(withId(R.id.increaseButton),
                                                                 isDescendantOfA(withId(R.id.addToShoppingListTriggerEdit))))
        for (i in 1 until testItemAddToShoppingListAmount)
            onView(autoAddTriggerIncreaseButton).perform(click())
        onView(withText(android.R.string.ok)).perform(click())

        onView(withId(R.id.inventoryRecyclerView)).perform(callMethod<RecyclerView> {
            val vh = it.findViewHolderForAdapterPosition(0)
            assertThat(vh).isNotNull()
            val itemView = vh!!.itemView as? InventoryItemView
            assertThat(itemView).isNotNull()
            assertThat(itemView!!.ui.nameEdit.text.toString()).isEqualTo("Test Item 1")
            assertThat(itemView.ui.extraInfoEdit.text.toString()).isEqualTo("Test Extra Info 1")
            assertThat(itemView.ui.checkBox.colorIndex).isEqualTo(testItemColorIndex)
            assertThat(itemView.ui.amountEdit.value).isEqualTo(testItemAmount)
            assertThat(itemView.detailsUi.addToShoppingListCheckBox.isChecked).isTrue()
            assertThat(itemView.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(testItemAddToShoppingListAmount)
        })
    }

    @Test fun addSeveralItems() {
        deleteAllItems()
        appears()
        val testItem1ColorIndex = 5
        val testItem1Amount = 3
        val testItem1AddToShoppingListAmount = 4
        val testItem2ColorIndex = 7
        val testItem2Amount = 8
        val testItem2AddToShoppingListAmount = 2

        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click()).perform(typeText("Test Item 1"))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click()).perform(typeText("Test Extra Info 1"))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(testItem1ColorIndex, click()))
        val amountIncreaseButton = inNewItemDialog(allOf(withId(R.id.increaseButton),
            isDescendantOfA(withId(R.id.amountEdit))))
        for (i in 1 until testItem1Amount)
            onView(amountIncreaseButton).perform(click())
        onView(inNewItemDialog(withId(R.id.addToShoppingListCheckBox))).perform(click())
        val autoAddTriggerIncreaseButton = inNewItemDialog(allOf(withId(R.id.increaseButton),
            isDescendantOfA(withId(R.id.addToShoppingListTriggerEdit))))
        for (i in 1 until testItem1AddToShoppingListAmount)
            onView(autoAddTriggerIncreaseButton).perform(click())
        onView(withText(R.string.add_another_item_button_description)).perform(click())

        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click()).perform(typeText("Test Item 2"))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click()).perform(typeText("Test Extra Info 2"))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(testItem2ColorIndex, click()))
        for (i in 1 until testItem2Amount)
            onView(amountIncreaseButton).perform(click())
        for (i in 1 until testItem2AddToShoppingListAmount)
            onView(autoAddTriggerIncreaseButton).perform(click())
        onView(withText(android.R.string.ok)).perform(click())

        onView(withId(R.id.inventoryRecyclerView)).perform(callMethod<RecyclerView> {
            var vh = it.findViewHolderForAdapterPosition(0)
            assertThat(vh).isNotNull()
            var itemView = vh!!.itemView as? InventoryItemView
            assertThat(itemView).isNotNull()
            assertThat(itemView!!.ui.nameEdit.text.toString()).isEqualTo("Test Item 1")
            assertThat(itemView.ui.extraInfoEdit.text.toString()).isEqualTo("Test Extra Info 1")
            assertThat(itemView.ui.checkBox.colorIndex).isEqualTo(testItem1ColorIndex)
            assertThat(itemView.ui.amountEdit.value).isEqualTo(testItem1Amount)
            assertThat(itemView.detailsUi.addToShoppingListCheckBox.isChecked).isTrue()
            assertThat(itemView.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(testItem1AddToShoppingListAmount)

            vh = it.findViewHolderForAdapterPosition(1)
            assertThat(vh).isNotNull()
            itemView = vh!!.itemView as? InventoryItemView
            assertThat(itemView).isNotNull()
            assertThat(itemView!!.ui.nameEdit.text.toString()).isEqualTo("Test Item 2")
            assertThat(itemView.ui.extraInfoEdit.text.toString()).isEqualTo("Test Extra Info 2")
            assertThat(itemView.ui.checkBox.colorIndex).isEqualTo(testItem2ColorIndex)
            assertThat(itemView.ui.amountEdit.value).isEqualTo(testItem2Amount)
            assertThat(itemView.detailsUi.addToShoppingListCheckBox.isChecked).isFalse()
            assertThat(itemView.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(testItem2AddToShoppingListAmount)
        })
    }

    private fun deleteAllItems() {
        onView(withId(R.id.inventory_button)).perform(click())
        val emptyInventoryMessage =
            context.getString(R.string.empty_recycler_view_message,
                context.getString(R.string.inventory_item_collection_name))
        val empty = try { onView(withText(emptyInventoryMessage)).check(matches(isDisplayed())); true }
                    catch(e: AssertionFailedError) { false }
        if (!empty) {
            onView(withId(R.id.menuButton)).perform(click())
            onView(withText(R.string.select_all_description)).inRoot(isPlatformPopup()).perform(click())
            onView(withId(R.id.changeSortButton)).perform(click())
        }
    }
}
