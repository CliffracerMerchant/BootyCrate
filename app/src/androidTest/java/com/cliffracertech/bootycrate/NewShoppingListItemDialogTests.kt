/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.view.TintableCheckbox
import junit.framework.AssertionFailedError
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class NewShoppingListItemDialogTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)

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
        onView(inNewItemDialog(instanceOf(ExpandableSelectableItemView::class.java))).perform(callMethod<ExpandableSelectableItemView<*>> {
            if (it.ui.checkBox.colorIndex != 0)
                fail("The new item dialog's checkbox's colorIndex is not the correct starting value (0).")
            if (!it.ui.checkBox.inColorEditMode)
                fail("The new item dialog's checkbox is not in color edit mode.")
            if (!it.ui.nameEdit.isEditable)
                fail("The new item dialog's name edit is not editable.")
            if (!it.ui.extraInfoEdit.isEditable)
                fail("The new item dialog's extra info edit is not editable.")
            if (!it.ui.amountEdit.valueIsFocusable)
                fail("The new item dialog's amount edit is not focusable.")
        })
    }

    @Test fun correctValuesAfterAddAnother() {
        deleteAllItems()
        correctStartingValues()
        val testItemName = "Test Item 1"
        val testItemExtraInfo = "Test Extra Info 1"
        val testItemColorIndex = 5
        val testItemAmount = 3
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click()).perform(typeText(testItemName))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click()).perform(typeText(testItemExtraInfo))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(testItemColorIndex, click()))
        for (i in 1 until testItemAmount)
            onView(inNewItemDialog(withId(R.id.increaseButton))).perform(click())
        onView(withText(R.string.add_another_item_button_description)).perform(click())

        val incorrectColorMessage = "The new item dialog's checkbox's colorIndex is not the correct starting value (0)."
        // The color edit is intended to stay the same value after the add another button
        // is pressed, but the rest of the fields should be reset to their default values.
        try { testCorrectStartingValues() }
        catch(e: AssertionError) { if (e.message != incorrectColorMessage) throw e }
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(callMethod<TintableCheckbox> {
            if (it.colorIndex != testItemColorIndex)
                fail("The new item dialog's checkbox's colorIndex was changed after add another was pressed.")
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
        val testItemName = "Test Item 1"
        val testItemExtraInfo = "Test Extra Info 1"
        val testItemColorIndex = 1
        val testItemAmount = 3
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click()).perform(typeText(testItemName))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click()).perform(typeText(testItemExtraInfo))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(testItemColorIndex, click()))
        for (i in 1 until testItemAmount)
            onView(inNewItemDialog(withId(R.id.increaseButton))).perform(click())
        onView(withText(android.R.string.ok)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(callMethod<RecyclerView> {
            val vh = it.findViewHolderForAdapterPosition(0)
            if (vh == null) fail("recycler view is empty")
            val itemView = vh!!.itemView as? ShoppingListItemView
            if (itemView == null) fail("item view is not of type ShoppingListItemView")
            if (itemView!!.ui.nameEdit.text.toString() != testItemName) fail("name does not match expected")
            if (itemView.ui.extraInfoEdit.text.toString() != testItemExtraInfo) fail("extra info does not match expected")
            if (itemView.ui.checkBox.colorIndex != testItemColorIndex) fail("extra info does not match expected")
            if (itemView.ui.amountEdit.value != testItemAmount) fail("amount does not match expected")
        })
    }

    @Test fun addSeveralItems() {
        deleteAllItems()
        appears()
        val testItemName = "Test Item 1"
        val testItemExtraInfo = "Test Extra Info 1"
        val testItemColorIndex = 1
        val testItemAmount = 3
        onView(inNewItemDialog(withId(R.id.nameEdit))).perform(click()).perform(typeText(testItemName))
        onView(inNewItemDialog(withId(R.id.extraInfoEdit))).perform(click()).perform(typeText(testItemExtraInfo))
        onView(inNewItemDialog(withId(R.id.checkBox))).perform(click())
        onView(withId(R.id.colorSheetList)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(testItemColorIndex, click()))
        for (i in 1 until testItemAmount)
            onView(inNewItemDialog(withId(R.id.increaseButton))).perform(click())
        onView(withText(android.R.string.ok)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(callMethod<RecyclerView> {
            val vh = it.findViewHolderForAdapterPosition(0)
            if (vh == null) fail("recycler view is empty")
            val itemView = vh!!.itemView as? ShoppingListItemView
            if (itemView == null) fail("item view is not of type ShoppingListItemView")
            if (itemView!!.ui.nameEdit.text.toString() != testItemName) fail("name does not match expected")
            if (itemView.ui.extraInfoEdit.text.toString() != testItemExtraInfo) fail("extra info does not match expected")
            if (itemView.ui.checkBox.colorIndex != testItemColorIndex) fail("extra info does not match expected")
            if (itemView.ui.amountEdit.value != testItemAmount) fail("amount does not match expected")
        })
    }

    private fun deleteAllItems() {
        val emptyShoppingListMessage =
            context.getString(R.string.empty_recycler_view_message,
                context.getString(R.string.shopping_list_item_collection_name))
        val empty = try { onView(withText(emptyShoppingListMessage)).check(matches(isDisplayed())); true }
        catch(e: AssertionFailedError) { false }
        if (!empty) {
            onView(withId(R.id.menuButton)).perform(click())
            onView(withText(R.string.select_all_description)).inRoot(isPlatformPopup()).perform(click())
            onView(withId(R.id.changeSortButton)).perform(click())
        }
    }

    private fun inNewItemDialog(matcher: Matcher<View>) =
        allOf(matcher, isDescendantOfA(withId(R.id.newItemViewContainer)))

    /** Thanks to the author of this blog post for the idea.
     * https://medium.com/android-news/call-view-methods-when-testing-by-espresso-and-kotlin-in-android-781262f7348e */
    private fun <T>callMethod(method: (view: T) -> Unit): ViewAction {
        return object: ViewAction {
            override fun getDescription() = method.toString()
            override fun getConstraints() = isEnabled()
            override fun perform(uiController: UiController?, view: View?) =
                method(view as? T ?: throw IllegalStateException("The matched view is null or not of type T"))
        }
    }
}
