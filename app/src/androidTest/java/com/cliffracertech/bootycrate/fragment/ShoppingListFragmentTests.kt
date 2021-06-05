/*
 * Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */
package com.cliffracertech.bootycrate.fragment

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.recyclerview.ShoppingListItemView
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.*
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemAnimator
import com.cliffracertech.bootycrate.recyclerview.ShoppingListRecyclerView
import com.cliffracertech.bootycrate.utils.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ShoppingListFragmentTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)
    private val dao = BootyCrateDatabase.get(context as Application).dao()
    private val uiDevice: UiDevice = UiDevice.getInstance(getInstrumentation())

    private val redItem0 = ShoppingListItem(name = "Red", extraInfo = "Extra info", color = 0, amount = 8)
    private val orangeItem1 = ShoppingListItem(name = "Orange", extraInfo = "Extra info", color = 1, amount = 2)
    private val yellowItem2 = ShoppingListItem(name = "Yellow", color = 2, amount = 1)
    private val grayItem11 = ShoppingListItem(name = "Gray", color = 11, amount = 9)

    @Before fun resetItems() {
        activityRule.scenario.onActivity {
            val shoppingListViewModel: ShoppingListViewModel by it.viewModels()
            shoppingListViewModel.sortByChecked = false
            shoppingListViewModel.sort = BootyCrateItemSort.Color
            val prefs = PreferenceManager.getDefaultSharedPreferences(it)
            prefs.edit().putBoolean(it.getString(R.string.pref_sort_by_checked_key), false).apply()
        }
        runBlocking {
            dao.deleteAllInventoryItems()
            dao.deleteAllShoppingListItems()
            dao.addConvertibles(listOf(redItem0, orangeItem1, yellowItem2, grayItem11))
        }
        Thread.sleep(50L)
    }

    @Test fun sortByColor() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun sortByNameAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_ascending_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(grayItem11, orangeItem1, redItem0, yellowItem2))
    }

    @Test fun sortByNameDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_descending_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(yellowItem2, redItem0, orangeItem1, grayItem11))
    }

    @Test fun sortByAmountAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_ascending_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(yellowItem2, orangeItem1, redItem0, grayItem11))
    }

    @Test fun sortByAmountDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_descending_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(grayItem11, redItem0, orangeItem1, yellowItem2))
    }

    private var collapsedItemHeight = 0
    @Test fun expandItem() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<RecyclerView> {
            (it.itemAnimator as ExpandableItemAnimator).notifyExpandedItemChanged(null)
            collapsedItemHeight = it.findViewHolderForAdapterPosition(0)!!.itemView.height
        }).check(onlyExpandedIndexIs(null, collapsedItemHeight))

        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1, clickEditButton())
        ).check(onlyExpandedIndexIs(1, collapsedItemHeight))
    }

    @Test fun expandAnotherItem() {
        expandItem()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(3, clickEditButton())
        ).check(onlyExpandedIndexIs(3, collapsedItemHeight))
    }

    private fun expandedItemSurvives(action: Runnable) {
        expandItem()
        action.run()
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlyExpandedIndexIs(1, collapsedItemHeight))
    }

    private fun switchToInventoryAndBack() {
        onView(withId(R.id.inventoryButton)).perform(click())
        onView(withId(R.id.shoppingListButton)).perform(click())
    }

    private fun switchToPreferencesAndBack() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.settings_description)).perform(click())
        onView(withId(R.id.backButton)).perform(click())
    }

    private fun changeOrientationAndBack() {
        uiDevice.setOrientationLeft()
        uiDevice.setOrientationNatural()
    }

    private fun changeOrientationWhileInInventory() {
        onView(withId(R.id.inventoryButton)).perform(click())
        changeOrientationAndBack()
        onView(withId(R.id.shoppingListButton)).perform(click())
    }

    private fun changeOrientationWhileInPreferences() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.settings_description)).perform(click())
        uiDevice.setOrientationLeft()
        uiDevice.setOrientationNatural()
        onView(withId(R.id.backButton)).perform(click())
    }

    @Test fun expandedItemSurvivesSwitchingToInventory() = expandedItemSurvives(::switchToInventoryAndBack)
    @Test fun expandedItemSurvivesSwitchingToPreferences() = expandedItemSurvives(::switchToPreferencesAndBack)
    @Test fun expandedItemSurvivesOrientationChange() = expandedItemSurvives(::changeOrientationAndBack)
    @Test fun expandedItemSurvivesOrientationChangeWhileInInventory() = expandedItemSurvives(::changeOrientationWhileInInventory)
    @Test fun expandedItemSurvivesOrientationChangeWhileInPreferences() = expandedItemSurvives(::changeOrientationWhileInPreferences)

    @Test fun selectIndividualItems() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1, longClick())
        ).check(onlySelectedIndicesAre(1))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(3, click())
        ).check(onlySelectedIndicesAre(1, 3))
    }

    @Test fun deselectIndividualItems() {
        selectIndividualItems()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1, click())
        ).check(onlySelectedIndicesAre(3))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(3, click())
        ).check(onlySelectedIndicesAre())
    }

    @Test fun selectAll() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.select_all_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlySelectedIndicesAre(0, 1, 2, 3))
    }

    @Test fun deselectAllWithActionBarBackButton() {
        runBlocking { dao.selectAllShoppingListItems() }
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    @Test fun deselectAllWithNavigationBackButton() {
        runBlocking { dao.selectAllShoppingListItems() }
        pressBack()
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    private fun selectionSurvives(action: Runnable) {
        selectIndividualItems()
        action.run()
        onView(withId(R.id.shoppingListRecyclerView))
            .check(onlySelectedIndicesAre(1, 3))
    }
    @Test fun selectionSurvivesSwitchingToInventory() = selectionSurvives(::switchToInventoryAndBack)
    @Test fun selectionSurvivesSwitchingToPreferences() = selectionSurvives(::switchToPreferencesAndBack)
    @Test fun selectionSurvivesOrientationChange() = selectionSurvives(::changeOrientationAndBack)
    @Test fun selectionSurvivesOrientationChangeWhileInInventory() = selectionSurvives(::changeOrientationWhileInInventory)
    @Test fun selectionSurvivesOrientationChangeWhileInPreferences() = selectionSurvives(::changeOrientationWhileInPreferences)

    @Test fun search() {
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("y"))
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(yellowItem2, grayItem11))
    }

    @Test fun addToExistingSearchQuery() {
        search()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("e"))
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(yellowItem2))
    }

    @Test fun searchExtraInfo() {
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("extra info"))
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(redItem0, orangeItem1))
    }

    @Test fun clearSearchQueryViaBackspace() {
        addToExistingSearchQuery()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(pressKey(KeyEvent.KEYCODE_DEL))
        Thread.sleep(30L) // Test works fine with a small sleep, or if stepping through while debugging
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(yellowItem2, grayItem11))
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(pressKey(KeyEvent.KEYCODE_DEL))
        Thread.sleep(30L)
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun clearSearchQueryViaActionBarBackButton() {
        search()
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("")))
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun clearSearchQueryViaNavigationBackButton() {
        search()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(closeSoftKeyboard())
        pressBack()
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("")))
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    private fun searchQuerySurvives(action: Runnable) {
        searchExtraInfo()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(closeSoftKeyboard())
        action.run()
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("extra info")))
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(redItem0, orangeItem1))
    }

    @Test fun searchQuerySurvivesSwitchingToInventory() = searchQuerySurvives(::switchToInventoryAndBack)
    @Test fun searchQuerySurvivesSwitchingToPreferences() = searchQuerySurvives(::switchToPreferencesAndBack)
    @Test fun searchQuerySurvivesOrientationChange() = searchQuerySurvives(::changeOrientationAndBack)
    @Test fun searchQuerySurvivesOrientationChangeWhileInInventory() = searchQuerySurvives(::changeOrientationWhileInInventory)
    @Test fun searchQuerySurvivesOrientationChangeWhileInPreferences() = searchQuerySurvives(::changeOrientationWhileInPreferences)
    @Test fun searchQuerySurvivesSelectionAndDeselection() = searchQuerySurvives(::deselectAllWithActionBarBackButton)

    private fun emptySearchResultsMessage() = allOf(withId(R.id.emptyRecyclerViewMessage),
                                                    withParent(withId(R.id.shoppingListFragmentView)),
                                                    withText(R.string.no_search_results_message))
    private fun emptyRecyclerViewMessage() = allOf(withId(R.id.emptyRecyclerViewMessage),
                                                   withParent(withId(R.id.shoppingListFragmentView)),
                                                   not(withText(R.string.no_search_results_message)))

    @Test fun emptyMessageAppears() {
        runBlocking { dao.deleteAllShoppingListItems() }
        Thread.sleep(30L)
        onView(emptyRecyclerViewMessage()).check(matches(isDisplayed()))
        onView(emptySearchResultsMessage()).check(matches(not(isDisplayed())))
        onView(withId(R.id.shoppingListRecyclerView)).check(matches(not(isDisplayed())))
    }

    @Test fun emptyMessageDisappears() {
        emptyMessageAppears()
        runBlocking { dao.add(ShoppingListItem(name = "new item")) }
        Thread.sleep(30L)
        onView(emptyRecyclerViewMessage()).check(matches(not(isDisplayed())))
        onView(emptySearchResultsMessage()).check(matches(not(isDisplayed())))
        onView(withId(R.id.shoppingListRecyclerView)).check(matches(isDisplayed()))
    }

    @Test fun noSearchResultsMessageAppears() {
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("Nonexistent item"))
        onView(emptyRecyclerViewMessage()).check(matches(not(isDisplayed())))
        onView(emptySearchResultsMessage()).check(matches(isDisplayed()))
        onView(withId(R.id.shoppingListRecyclerView)).check(matches(not(isDisplayed())))
    }

    @Test fun noSearchResultsMessageDisappears() {
        noSearchResultsMessageAppears()
        onView(withId(R.id.backButton)).perform(click())
        onView(emptyRecyclerViewMessage()).check(matches(not(isDisplayed())))
        onView(emptySearchResultsMessage()).check(matches(not(isDisplayed())))
        onView(withId(R.id.shoppingListRecyclerView)).check(matches(isDisplayed()))
    }

    @Test fun deleteItemsViaSwiping() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1, swipeLeft())
        ).check(onlyShownShoppingListItemsAre(redItem0, yellowItem2, grayItem11))

        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(2, swipeRight())
        ).check(onlyShownShoppingListItemsAre(redItem0, yellowItem2))
    }

    @Test fun deleteItemsViaActionBarDeleteButton() {
        selectIndividualItems()
        onView(withId(R.id.changeSortButton)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(redItem0, yellowItem2))
    }

    @Test fun shareEntireList() {
        Intents.init()
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.share_description)).perform(click())
        val intendedMessage = redItem0.toUserFacingString() + "\n" +
                orangeItem1.toUserFacingString() + "\n" +
                yellowItem2.toUserFacingString() + "\n" +
                grayItem11.toUserFacingString()
        val innerIntent = allOf(hasAction(Intent.ACTION_SEND),
                                hasType("text/plain"),
                                hasExtra(Intent.EXTRA_TEXT, intendedMessage))
        val collectionName = context.getString(R.string.shopping_list_item_collection_name)
        val intendedTitle = context.getString(R.string.share_whole_list_title, collectionName)
        Intents.intended(allOf(hasAction(Intent.ACTION_CHOOSER),
                               hasExtra(Intent.EXTRA_TITLE, intendedTitle),
                               hasExtra(equalTo(Intent.EXTRA_INTENT), innerIntent)))
        Intents.release()
    }

    @Test fun shareSelectedItems() {
        selectIndividualItems()
        Intents.init()
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.share_description)).perform(click())
        val intendedMessage = orangeItem1.toUserFacingString() + "\n" +
                grayItem11.toUserFacingString()
        val innerIntent = allOf(hasAction(Intent.ACTION_SEND),
                                hasType("text/plain"),
                                hasExtra(Intent.EXTRA_TEXT, intendedMessage))
        val collectionName = context.getString(R.string.shopping_list_item_collection_name)
        val intendedTitle = context.getString(R.string.share_selected_items_title, collectionName)
        Intents.intended(allOf(hasAction(Intent.ACTION_CHOOSER),
                               hasExtra(Intent.EXTRA_TITLE, intendedTitle),
                               hasExtra(equalTo(Intent.EXTRA_INTENT), innerIntent)))
        Intents.release()
    }

    @Test fun addItemsToInventory() {
        selectIndividualItems()
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.add_to_inventory_description)).perform(click())
        onView(withId(R.id.inventoryButton)).perform(click())
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(onlyShownInventoryItemsAre(
            InventoryItem(name = orangeItem1.name, extraInfo = orangeItem1.extraInfo,
                          amount = 0, color = orangeItem1.color),
            InventoryItem(name = grayItem11.name, amount = 0, color = grayItem11.color)))
    }

    @Test fun changeItemColor() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(2, clickEditButton(), clickCheckBox()))
        onView(withId(R.id.colorSheetList)).perform(
            actionsOnItemAtPosition(6, click()))
        onView(withId(R.id.shoppingListRecyclerView)).perform(doStuff<RecyclerView> {
            val item = (it.adapter as ListAdapter<*, *>).currentList[2] as ShoppingListItem
            assertThat(item.color).isEqualTo(6)
            val vh = it.findViewHolderForAdapterPosition(2) as ShoppingListRecyclerView.ViewHolder
            assertThat(vh.view.ui.checkBox.colorIndex).isEqualTo(6)
        })
    }

    @Test fun changeItemName() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(2,
                clickEditButton(),
                actionOnChildWithId(R.id.nameEdit, click(), typeText("er")),
                clickEditButton()),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[2] as ShoppingListItem
                assertThat(item.name).isEqualTo("Yellower")
                val vh = it.findViewHolderForAdapterPosition(2) as ShoppingListRecyclerView.ViewHolder
                assertThat(vh.view.ui.nameEdit.text.toString()).isEqualTo("Yellower")
            })
    }

    @Test fun changeItemExtraInfo() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1,
                clickEditButton(),
                actionOnChildWithId(R.id.extraInfoEdit, click(), typeText(" 2.0")),
                clickEditButton()),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as ShoppingListItem
                assertThat(item.extraInfo).isEqualTo("Extra info 2.0")
                val vh = it.findViewHolderForAdapterPosition(1) as ShoppingListRecyclerView.ViewHolder
                assertThat(vh.view.ui.extraInfoEdit.text.toString()).isEqualTo("Extra info 2.0")
            })
    }

    @Test fun changeItemAmountUsingButtons() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1,
                onIncreaseButton(click(), click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as ShoppingListItem
                assertThat(item.amount).isEqualTo(4)
                val vh = it.findViewHolderForAdapterPosition(1) as ShoppingListRecyclerView.ViewHolder
                assertThat(vh.view.ui.amountEdit.value).isEqualTo(4)
            }, actionsOnItemAtPosition(1,
                onDecreaseButton(click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as ShoppingListItem
                assertThat(item.amount).isEqualTo(3)
                val vh = it.findViewHolderForAdapterPosition(1) as ShoppingListRecyclerView.ViewHolder
                assertThat(vh.view.ui.amountEdit.value).isEqualTo(3)
            })
    }

    @Test fun changeItemAmountUsingKeyboard() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1,
                clickEditButton(),
                typeIntoValueEdit("9"),
                clickEditButton()),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as ShoppingListItem
                assertThat(item.amount).isEqualTo(29)
                val vh = it.findViewHolderForAdapterPosition(1) as ShoppingListRecyclerView.ViewHolder
                assertThat(vh.view.ui.amountEdit.value).isEqualTo(29)
            })
    }

    private fun hasOnlyCheckedItemsAtIndices(vararg checkedItemsIndices: Int) = ViewAssertion { view, e ->
        if (view == null) throw e
        assertThat(view).isInstanceOf(ShoppingListRecyclerView::class.java)
        val it = view as ShoppingListRecyclerView
        for (i in 0 until it.adapter.itemCount) {
            val ui = (it.findViewHolderForAdapterPosition(i)!!.itemView as ShoppingListItemView).ui
            val shouldBeChecked = checkedItemsIndices.contains(i)
            assertThat(ui.checkBox.isChecked).isEqualTo(shouldBeChecked)
            assertThat(ui.nameEdit.hasStrikeThrough).isEqualTo(shouldBeChecked)
            if (ui.extraInfoEdit.text?.isNotBlank() == true)
                assertThat(ui.extraInfoEdit.hasStrikeThrough).isEqualTo(shouldBeChecked)
        }
    }

    private fun clickCheckBox() = actionOnChildWithId(R.id.checkBox, click())

    @Test fun checkIndividualItems() {
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(0, clickCheckBox()),
            actionsOnItemAtPosition(2, clickCheckBox())
        ).check(hasOnlyCheckedItemsAtIndices(0, 2))
    }

    @Test fun uncheckIndividualItems() {
        checkIndividualItems()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(0, clickCheckBox()),
            actionsOnItemAtPosition(2, clickCheckBox())
        ).check(hasOnlyCheckedItemsAtIndices())
    }

    @Test fun checkAllItems() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.check_all_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView))
            .check(hasOnlyCheckedItemsAtIndices(0, 1, 2, 3))
    }

    @Test fun uncheckAllItems() {
        checkAllItems()
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.uncheck_all_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView))
            .check(hasOnlyCheckedItemsAtIndices())
    }

    @Test fun checkoutButtonEnabledAfterCheckingIndividualItems() {
        onView(withId(R.id.checkoutButton)).check(matches(not(isEnabled())))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(0, clickCheckBox()))
        onView(withId(R.id.checkoutButton)).check(matches(isEnabled()))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1, clickCheckBox()))
        onView(withId(R.id.checkoutButton)).check(matches(isEnabled()))
    }

    @Test fun checkoutButtonDisabledAfterUncheckingIndividualItems() {
        checkoutButtonEnabledAfterCheckingIndividualItems()
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(0, clickCheckBox()))
        onView(withId(R.id.checkoutButton)).check(matches(isEnabled()))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(1, clickCheckBox()))
        onView(withId(R.id.checkoutButton)).check(matches(not(isEnabled())))
    }

    @Test fun checkoutButtonEnabledAfterCheckingAllItems() {
        onView(withId(R.id.checkoutButton)).check(matches(not(isEnabled())))
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.check_all_description)).perform(click())
        onView(withId(R.id.checkoutButton)).check(matches(isEnabled()))
    }

    @Test fun checkoutButtonDisabledAfterUncheckingAllItems() {
        runBlocking { dao.checkAllShoppingListItems() }
        onView(withId(R.id.checkoutButton)).check(matches(isEnabled()))
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.uncheck_all_description)).perform(click())
        onView(withId(R.id.checkoutButton)).check(matches(not(isEnabled())))
    }

    @Test fun checkoutRemovesCheckedItems() {
        addItemsToInventory()
        onView(withId(R.id.shoppingListButton)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(0, clickCheckBox()),
            actionsOnItemAtPosition(1, clickCheckBox()))
        onView(withId(R.id.checkoutButton)).perform(click(), click())
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(yellowItem2, grayItem11))
    }

    @Test fun checkoutUpdatesAmountOfLinkedItems() {
        checkoutRemovesCheckedItems()
        onView(withId(R.id.inventoryButton)).perform(click())
        val expectedItem1 = InventoryItem(name = orangeItem1.name, extraInfo = orangeItem1.extraInfo,
                                          color = orangeItem1.color, amount = 1 + orangeItem1.amount)
        val expectedItem2 = InventoryItem(name = grayItem11.name, color = grayItem11.color, amount = 1)
        // grayItem11 was not checked and should not have its amount updated.
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(expectedItem1, expectedItem2))
    }

    @Test fun sortByChecked() {
        checkIndividualItems()
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.settings_description)).perform(click())
        onView(withText(R.string.pref_sort_by_checked_title)).perform(click())
        pressBack()
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(orangeItem1, grayItem11, redItem0, yellowItem2))
        onView(withId(R.id.shoppingListRecyclerView)).perform(
            actionsOnItemAtPosition(2, clickCheckBox())
        ).check(onlyShownShoppingListItemsAre(redItem0, orangeItem1, grayItem11, yellowItem2))
    }
}