/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.utils.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class InventoryFragmentTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule var activityRule = ActivityScenarioRule(GradientStyledMainActivity::class.java)
    private val db = BootyCrateDatabase.get(context as Application)
    private val uiDevice: UiDevice = UiDevice.getInstance(getInstrumentation())

    private val redItem0 = InventoryItem(name = "Red", extraInfo = "Extra info", color = 0, amount = 8)
    private val orangeItem1 = InventoryItem(name = "Orange", extraInfo = "Extra info", color = 1, amount = 2)
    private val yellowItem2 = InventoryItem(name = "Yellow", color = 2, amount = 1)
    private val grayItem11 = InventoryItem(name = "Gray", color = 11, amount = 9, addToShoppingList = true)

    @Before fun setup() {
        runBlocking {
            db.shoppingListItemDao().deleteAll()
            db.inventoryItemDao().deleteAll()
            db.inventoryItemDao().add(listOf(redItem0, orangeItem1, yellowItem2, grayItem11))
        }
        onView(withId(R.id.inventoryButton)).perform(click())
    }

    @After fun finish() { activityRule.scenario.moveToState(Lifecycle.State.DESTROYED) }

    @Test fun sortByColor() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun sortByNameAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_ascending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(grayItem11, orangeItem1, redItem0, yellowItem2))
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
    }

    @Test fun sortByNameDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.name_descending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2, redItem0, orangeItem1, grayItem11))
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
    }

    @Test fun sortByAmountAscending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_ascending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2, orangeItem1, redItem0, grayItem11))
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
    }

    @Test fun sortByAmountDescending() {
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.amount_descending_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(grayItem11, redItem0, orangeItem1, yellowItem2))
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
    }

    private var collapsedItemHeight = 0
    @Test fun expandItem() {
        onView(withId(R.id.inventoryRecyclerView)).perform(doStuff<RecyclerView> {
            (it.itemAnimator as ExpandableItemAnimator).notifyExpandedItemChanged(null)
            collapsedItemHeight = it.findViewHolderForAdapterPosition(0)!!.itemView.height
        }).check(onlyExpandedIndexIs(null, collapsedItemHeight))

        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click()))
        ).check(onlyExpandedIndexIs(1, collapsedItemHeight))
    }

    @Test fun expandAnotherItem() {
        expandItem()
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.editButton, click()))
        ).check(onlyExpandedIndexIs(3, collapsedItemHeight))
    }

    private fun expandedItemSurvives(action: Runnable) {
        expandItem()
        action.run()
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlyExpandedIndexIs(1, collapsedItemHeight))
    }

    private fun switchToShoppingListAndBack() {
        onView(withId(R.id.shoppingListButton)).perform(click())
        onView(withId(R.id.inventoryButton)).perform(click())
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

    private fun changeOrientationWhileInShoppingList() {
        onView(withId(R.id.shoppingListButton)).perform(click())
        changeOrientationAndBack()
        onView(withId(R.id.inventoryButton)).perform(click())
    }

    private fun changeOrientationWhileInPreferences() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.settings_description)).perform(click())
        uiDevice.setOrientationLeft()
        uiDevice.setOrientationNatural()
        onView(withId(R.id.backButton)).perform(click())
    }

    @Test fun expandedItemSurvivesSwitchingToShoppingList() = expandedItemSurvives(::switchToShoppingListAndBack)
    @Test fun expandedItemSurvivesSwitchingToPreferences() = expandedItemSurvives(::switchToPreferencesAndBack)
    @Test fun expandedItemSurvivesOrientationChange() = expandedItemSurvives(::changeOrientationAndBack)
    @Test fun expandedItemSurvivesOrientationChangeWhileInShoppingList() = selectionSurvives(::changeOrientationWhileInShoppingList)
    @Test fun expandedItemSurvivesOrientationChangeWhileInPreferences() = expandedItemSurvives(::changeOrientationWhileInPreferences)

    @Test fun selectIndividualItems() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick())
        ).check(onlySelectedIndicesAre(1))
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click())
        ).check(onlySelectedIndicesAre(1, 3))
    }

    @Test fun deselectIndividualItems() {
        selectIndividualItems()
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click())
        ).check(onlySelectedIndicesAre(3))
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click())
        ).check(onlySelectedIndicesAre())
    }

    @Test fun selectAll() {
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.select_all_description)).perform(click())
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre(0, 1, 2, 3))
    }

    @Test fun deselectAllWithActionBarBackButton() {
        runBlocking { db.inventoryItemDao().selectAll() }
        // This test mysteriously works without the sleep for the shopping list,
        // but not for the inventory?
        Thread.sleep(50L)
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    @Test fun deselectAllWithNavigationBackButton() {
        runBlocking { db.inventoryItemDao().selectAll() }
        Espresso.pressBack()
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre())
    }

    private fun selectionSurvives(action: Runnable) {
        selectIndividualItems()
        action.run()
        onView(withId(R.id.inventoryRecyclerView))
            .check(onlySelectedIndicesAre(1, 3))
    }

    @Test fun selectionSurvivesSwitchingToShoppingList() = selectionSurvives(::switchToShoppingListAndBack)
    @Test fun selectionSurvivesSwitchingToPreferences() = selectionSurvives(::switchToPreferencesAndBack)
    @Test fun selectionSurvivesOrientationChange() = selectionSurvives(::changeOrientationAndBack)
    @Test fun selectionSurvivesOrientationChangeWhileInShoppingList() = selectionSurvives(::changeOrientationWhileInShoppingList)
    @Test fun selectionSurvivesOrientationChangeWhileInPreferences() = selectionSurvives(::changeOrientationWhileInPreferences)

    @Test fun searching() {
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("y"))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2, grayItem11))
    }

    @Test fun addingToExistingSearchQuery() {
        searching()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("e"))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2))
    }

    @Test fun searchingExtraInfo() {
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("extra info"))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1))
    }

    @Test fun clearingSearchQueryViaBackspace() {
        addingToExistingSearchQuery()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(pressKey(KeyEvent.KEYCODE_DEL))
        Thread.sleep(30L) // Test works fine with a small sleep, or if stepping through while debugging
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(yellowItem2, grayItem11))
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(pressKey(KeyEvent.KEYCODE_DEL))
        Thread.sleep(30L)
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun clearingSearchQueryViaActionBarBackButton() {
        searching()
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("")))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    @Test fun clearingSearchQueryViaNavigationBackButton() {
        searching()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(closeSoftKeyboard())
        Espresso.pressBack()
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("")))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1, yellowItem2, grayItem11))
    }

    private fun searchQuerySurvives(action: Runnable) {
        searchingExtraInfo()
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(closeSoftKeyboard())
        action.run()
        onView(withId(R.id.actionBarTitle_searchQuery)).check(matches(withText("extra info")))
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, orangeItem1))
    }

    @Test fun searchQuerySurvivesSwitchingToShoppingList() = searchQuerySurvives(::switchToShoppingListAndBack)
    @Test fun searchQuerySurvivesSwitchingToPreferences() = searchQuerySurvives(::switchToPreferencesAndBack)
    @Test fun searchQuerySurvivesOrientationChange() = searchQuerySurvives(::changeOrientationAndBack)
    @Test fun searchQuerySurvivesOrientationChangeWhileInShoppingList() = searchQuerySurvives(::changeOrientationWhileInShoppingList)
    @Test fun searchQuerySurvivesOrientationChangeWhileInPreferences() = searchQuerySurvives(::changeOrientationWhileInPreferences)
    @Test fun searchQuerySurvivesSelectionAndDeselection() = searchQuerySurvives(::deselectAllWithActionBarBackButton)

    private fun emptySearchResultsMessage() = allOf(withId(R.id.emptyRecyclerViewMessage),
                                                    withParent(withId(R.id.inventoryFragmentView)),
                                                    withText(R.string.no_search_results_message))
    private fun emptyRecyclerViewMessage() = allOf(withId(R.id.emptyRecyclerViewMessage),
                                                   withParent(withId(R.id.inventoryFragmentView)),
                                                   not(withText(R.string.no_search_results_message)))

    @Test fun emptyMessageAppears() {
        runBlocking { db.inventoryItemDao().deleteAll() }
        Thread.sleep(30L)
        onView(emptyRecyclerViewMessage()).check(matches(isDisplayed()))
        onView(emptySearchResultsMessage()).check(matches(not(isDisplayed())))
        onView(withId(R.id.inventoryRecyclerView)).check(matches(not(isDisplayed())))
    }

    @Test fun emptyMessageDisappears() {
        emptyMessageAppears()
        runBlocking{ db.inventoryItemDao().add(InventoryItem(name = "new item")) }
        Thread.sleep(30L)
        onView(emptyRecyclerViewMessage()).check(matches(not(isDisplayed())))
        onView(emptySearchResultsMessage()).check(matches(not(isDisplayed())))
        onView(withId(R.id.inventoryRecyclerView)).check(matches(isDisplayed()))
    }

    @Test fun noSearchResultsMessageAppears() {
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.actionBarTitle_searchQuery)).perform(typeText("Nonexistent item"))
        onView(emptyRecyclerViewMessage()).check(matches(not(isDisplayed())))
        onView(emptySearchResultsMessage()).check(matches(isDisplayed()))
        onView(withId(R.id.inventoryRecyclerView)).check(matches(not(isDisplayed())))
    }

    @Test fun noSearchResultsMessageDisappears() {
        noSearchResultsMessageAppears()
        onView(withId(R.id.backButton)).perform(click())
        onView(emptyRecyclerViewMessage()).check(matches(not(isDisplayed())))
        onView(emptySearchResultsMessage()).check(matches(not(isDisplayed())))
        onView(withId(R.id.inventoryRecyclerView)).check(matches(isDisplayed()))
    }

    @Test fun deleteItemsViaSwiping() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1, swipeLeft())
        ).check(onlyShownInventoryItemsAre(redItem0, yellowItem2, grayItem11))

        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2, swipeRight())
        ).check(onlyShownInventoryItemsAre(redItem0, yellowItem2))
    }

    @Test fun deleteItemsViaActionBarDeleteButton() {
        selectIndividualItems()
        onView(withId(R.id.changeSortButton)).perform(click())
        onView(withId(R.id.inventoryRecyclerView)).check(
            onlyShownInventoryItemsAre(redItem0, yellowItem2))
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
        val collectionName = context.getString(R.string.inventory_item_collection_name)
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
        val collectionName = context.getString(R.string.inventory_item_collection_name)
        val intendedTitle = context.getString(R.string.share_selected_items_title, collectionName)
        Intents.intended(allOf(hasAction(Intent.ACTION_CHOOSER),
                               hasExtra(Intent.EXTRA_TITLE, intendedTitle),
                               hasExtra(equalTo(Intent.EXTRA_INTENT), innerIntent)))
        Intents.release()
    }

    @Test fun addToShoppingList() {
        selectIndividualItems()
        onView(withId(R.id.menuButton)).perform(click())
        onPopupView(withText(R.string.add_to_shopping_list_description)).perform(click())
        onView(withId(R.id.shoppingListButton)).perform(click())
        onView(withId(R.id.changeSortButton)).perform(click())
        onPopupView(withText(R.string.color_description)).perform(click())
        onView(withId(R.id.shoppingListRecyclerView)).check(onlyShownShoppingListItemsAre(
            ShoppingListItem(name = orangeItem1.name, extraInfo = orangeItem1.extraInfo,
                             amount = 1, color = orangeItem1.color),
            ShoppingListItem(name = grayItem11.name, amount = 1, color = grayItem11.color)))
    }

    @Test fun changeItemColor() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2,
                actionOnChildWithId(R.id.checkBox, click())))
        onView(withId(R.id.colorSheetList)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(6, click()))
        onView(withId(R.id.inventoryRecyclerView)).perform(doStuff<RecyclerView> {
            val item = (it.adapter as ListAdapter<*, *>).currentList[2] as InventoryItem
            assertThat(item.color).isEqualTo(6)
            val vh = it.findViewHolderForAdapterPosition(2) as InventoryRecyclerView.ViewHolder
            assertThat(vh.view.ui.checkBox.colorIndex).isEqualTo(6)
            assertThat(vh.view.detailsUi.addToShoppingListCheckBox.colorIndex).isEqualTo(6)
        })
    }

    @Test fun changeItemName() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2,
                actionOnChildWithId(R.id.nameEdit, click(), typeText("er"))),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(2,
                actionOnChildWithId(R.id.editButton, click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[2] as InventoryItem
                assertThat(item.name).isEqualTo("Yellower")
                val vh = it.findViewHolderForAdapterPosition(2) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.ui.nameEdit.text.toString()).isEqualTo("Yellower")
            })
    }

    @Test fun changeItemExtraInfo() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.extraInfoEdit, click(), typeText(" 2.0"))),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as InventoryItem
                assertThat(item.extraInfo).isEqualTo("Extra info 2.0")
                val vh = it.findViewHolderForAdapterPosition(1) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.ui.extraInfoEdit.text.toString()).isEqualTo("Extra info 2.0")
            })
    }

    @Test fun changeItemAmountUsingButtons() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.increaseButton, click(), click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as InventoryItem
                assertThat(item.amount).isEqualTo(4)
                val vh = it.findViewHolderForAdapterPosition(1) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.ui.amountEdit.value).isEqualTo(4)
            }, actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.decreaseButton, click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as InventoryItem
                assertThat(item.amount).isEqualTo(3)
                val vh = it.findViewHolderForAdapterPosition(1) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.ui.amountEdit.value).isEqualTo(3)
            })
    }

    @Test fun changeItemAmountUsingKeyBoard() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.amountEdit,
                    actionOnChildWithId(R.id.valueEdit, click(), replaceText("9")))),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as InventoryItem
                assertThat(item.amount).isEqualTo(9)
                val vh = it.findViewHolderForAdapterPosition(1) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.ui.amountEdit.value).isEqualTo(9)
            })
    }

    @Test fun changeItemAddToShoppingList() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.addToShoppingListCheckBox, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as InventoryItem
                assertThat(item.addToShoppingList).isTrue()
                val vh = it.findViewHolderForAdapterPosition(1) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.detailsUi.addToShoppingListCheckBox.isChecked).isTrue()
            })
    }

    @Test fun changeItemAddToShoppingListAmountUsingButtons() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.addToShoppingListTriggerEdit,
                    actionOnChildWithId(R.id.increaseButton, click(), click()))),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as InventoryItem
                assertThat(item.addToShoppingListTrigger).isEqualTo(3)
                val vh = it.findViewHolderForAdapterPosition(1) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(3)
            }, actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.addToShoppingListTriggerEdit,
                    actionOnChildWithId(R.id.decreaseButton, click()))),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as InventoryItem
                assertThat(item.addToShoppingListTrigger).isEqualTo(2)
                val vh = it.findViewHolderForAdapterPosition(1) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(2)
            })
    }

    @Test fun changeItemAddToShoppingListAmountUsingKeyBoard() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.addToShoppingListTriggerEdit,
                    actionOnChildWithId(R.id.valueEdit, click(), replaceText("12")))),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            doStuff<RecyclerView> {
                val item = (it.adapter as ListAdapter<*, *>).currentList[1] as InventoryItem
                assertThat(item.addToShoppingListTrigger).isEqualTo(12)
                val vh = it.findViewHolderForAdapterPosition(1) as InventoryRecyclerView.ViewHolder
                assertThat(vh.view.detailsUi.addToShoppingListTriggerEdit.value).isEqualTo(12)
            })
    }

    @Test fun addToShoppingListWorksWhenTurnedOnWithConditionsAlreadyMet() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.addToShoppingListTriggerEdit,
                    actionOnChildWithId(R.id.valueEdit, click(), replaceText("12")))),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.addToShoppingListCheckBox, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                actionOnChildWithId(R.id.editButton, click())))
        onView(withId(R.id.shoppingListButton)).perform(click())
        val expectedItem = ShoppingListItem(name = orangeItem1.name, extraInfo = orangeItem1.extraInfo,
                                            color = orangeItem1.color, amount = 12 - orangeItem1.amount)
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(expectedItem))
    }

    @Test fun addToShoppingListWorksWhenAmountIsDecreasedBelowTrigger() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.addToShoppingListTriggerEdit,
                    actionOnChildWithId(R.id.valueEdit, click(), replaceText("9")))),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.amountEdit,
                    actionOnChildWithId(R.id.valueEdit, click(), replaceText("6")))),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.editButton, click())))
        onView(withId(R.id.shoppingListButton)).perform(click())
        val expectedItem = ShoppingListItem(color = grayItem11.color, name = grayItem11.name,
                                            amount = 3)
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(expectedItem))
    }

    @Test fun addToShoppingListWorksWhenTriggerIsRaisedAboveAmount() {
        onView(withId(R.id.inventoryRecyclerView)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.editButton, click())),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.addToShoppingListTriggerEdit,
                    actionOnChildWithId(R.id.valueEdit, click(), replaceText("11")))),
            actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                actionOnChildWithId(R.id.editButton, click())))
        onView(withId(R.id.shoppingListButton)).perform(click())
        val expectedItem = ShoppingListItem(name = grayItem11.name, extraInfo = grayItem11.extraInfo,
                                            color = grayItem11.color, amount = 11 - grayItem11.amount)
        onView(withId(R.id.shoppingListRecyclerView)).check(
            onlyShownShoppingListItemsAre(expectedItem))
    }
}