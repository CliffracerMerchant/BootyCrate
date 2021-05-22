/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Looper
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.cliffracertech.bootycrate.database.InventoryItem
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
class InventoryItemViewTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var instance: InventoryItemView
    private lateinit var testItem: InventoryItem
    private fun waitForAnimationsToFinish() = Shadows.shadowOf(Looper.getMainLooper()).idle()

    @Before fun resetInstance() {
        instance = InventoryItemView(context)
         testItem = InventoryItem(color = 5, name = "Test item", extraInfo = "Test extra info",
                                  amount = 8, autoAddToShoppingList = true, autoAddToShoppingListAmount = 3)
    }

    private fun assertExpandedState(expanded: Boolean) {
        assertThat(instance.ui.checkBox.inColorEditMode).isTrue()
        assertThat(instance.ui.nameEdit.isEditable).isEqualTo(expanded)
        assertThat(instance.ui.extraInfoEdit.isEditable).isEqualTo(expanded)
        assertThat(instance.ui.amountEdit.valueIsFocusable).isEqualTo(expanded)
        assertThat(instance.ui.editButton.isActivated).isEqualTo(expanded)
        assertThat(instance.detailsUi.autoAddToShoppingListCheckBox.isVisible).isEqualTo(expanded)
        assertThat(instance.detailsUi.autoAddToShoppingListLabel.isVisible).isEqualTo(expanded)
        assertThat(instance.detailsUi.autoAddToShoppingListAmountEdit.isVisible).isEqualTo(expanded)
    }

    private fun assertSelectedState(selected: Boolean) {
        val background = instance.background as? LayerDrawable
        val selectedBackground = background?.getDrawable(1) as? LayerDrawable
        val gradientOutline = selectedBackground?.getDrawable(0) as? GradientDrawable
        assertThat(gradientOutline?.alpha).isEqualTo(if (selected) 255 else 0)
        assertThat(instance.isInSelectedState).isEqualTo(selected)
    }

    @Test fun initialCollapsedState() {
        instance.update(testItem)
        assertExpandedState(false)
    }

    @Test fun initialExpandedState() {
        testItem.isExpanded = true
        instance.update(testItem)
        assertExpandedState(true)
    }

    @Test fun initialDeselectedState() {
        instance.update(testItem)
        assertSelectedState(false)
    }

    @Test fun initialSelectedState() {
        testItem.isSelected = true
        instance.update(testItem)
        assertSelectedState(true)
    }

    @Test fun initialBlankExtraInfoIsHidden() {
        testItem.extraInfo = ""
        instance.update(testItem)
        assertThat(instance.ui.extraInfoEdit.isVisible).isFalse()
    }

    @Test fun valuesMatchItem() {
        instance.update(testItem)
        assertThat(instance.ui.checkBox.colorIndex).isEqualTo(testItem.color)
        assertThat(instance.ui.nameEdit.text.toString()).isEqualTo(testItem.name)
        assertThat(instance.ui.extraInfoEdit.text.toString()).isEqualTo(testItem.extraInfo)
        assertThat(instance.ui.amountEdit.value).isEqualTo(testItem.amount)
        assertThat(instance.detailsUi.autoAddToShoppingListCheckBox.isChecked).isEqualTo(testItem.autoAddToShoppingList)
        assertThat(instance.detailsUi.autoAddToShoppingListCheckBox.colorIndex).isEqualTo(testItem.color)
        assertThat(instance.detailsUi.autoAddToShoppingListAmountEdit.value).isEqualTo(testItem.autoAddToShoppingListAmount)
    }

    @Test fun editButtonExpandsWhileCollapsed() {
        initialCollapsedState()
        instance.ui.editButton.performClick()
        assertExpandedState(true)
    }

    @Test fun editButtonCollapsesWhileExpanded() {
        editButtonExpandsWhileCollapsed()
        instance.ui.editButton.performClick()
        waitForAnimationsToFinish()
        assertExpandedState(false)
    }

    @Test fun expandingMakesBlankExtraInfoVisible() {
        initialBlankExtraInfoIsHidden()
        instance.ui.editButton.performClick()
        assertThat(instance.ui.extraInfoEdit.isVisible).isTrue()
    }

    @Test fun collapsingHidesBlankExtraInfo() {
        expandingMakesBlankExtraInfoVisible()
        instance.ui.editButton.performClick()
        waitForAnimationsToFinish()
        assertThat(instance.ui.extraInfoEdit.isVisible).isFalse()
    }

    @Test fun collapsingDoesNotHideNonBlankExtraInfo() {
        editButtonCollapsesWhileExpanded()
        assertThat(instance.ui.extraInfoEdit.isVisible).isTrue()
    }

    @Test fun selecting() {
        initialDeselectedState()
        instance.select()
        waitForAnimationsToFinish()
        assertSelectedState(true)
    }

    @Test fun deselecting() {
        initialSelectedState()
        instance.deselect()
        waitForAnimationsToFinish()
        assertSelectedState(false)
    }
}