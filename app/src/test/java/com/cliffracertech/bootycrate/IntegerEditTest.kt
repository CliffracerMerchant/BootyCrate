/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cliffracertech.bootycrate.utils.dpToPixels
import com.cliffracertech.bootycrate.utils.spToPixels
import com.cliffracertech.bootycrate.view.IntegerEdit
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntegerEditTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun defaultValuesIntegerEdit(): IntegerEdit {
        val attrs = Robolectric.buildAttributeSet().build()
        return IntegerEdit(context, attrs)
    }

    private fun nonDefaultValuesIntegerEdit(): IntegerEdit {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.initialValue, "34")
            .addAttribute(R.attr.minValue, "10")
            .addAttribute(R.attr.maxValue, "500")
            .addAttribute(R.attr.stepSize, "3")
            .addAttribute(R.attr.valueIsFocusable, "true")
            .addAttribute(android.R.attr.textSize, "5sp")
            .build()
        return IntegerEdit(context, attrs)
    }

    @Test fun integerEdit_testDefaultValues() {
        val integerEdit = defaultValuesIntegerEdit()
        assertThat(integerEdit.minValue).isEqualTo(0)
        assertThat(integerEdit.maxValue).isEqualTo(999)
        assertThat(integerEdit.value).isEqualTo(0)
        assertThat(integerEdit.stepSize).isEqualTo(1)
        assertThat(integerEdit.valueIsFocusable).isFalse()
        assertThat(integerEdit.ui.valueEdit.textSize).isZero()
        assertThat(integerEdit.ui.valueEdit.paint.strokeWidth).isEqualTo(context.dpToPixels(1f))
    }

    @Test fun integerEdit_testNonDefaultValues() {
        val integerEdit = nonDefaultValuesIntegerEdit()
        assertThat(integerEdit.value).isEqualTo(34)
        assertThat(integerEdit.minValue).isEqualTo(10)
        assertThat(integerEdit.maxValue).isEqualTo(500)
        assertThat(integerEdit.stepSize).isEqualTo(3)
        assertThat(integerEdit.valueIsFocusable).isTrue()
        assertThat(integerEdit.ui.valueEdit.textSize).isEqualTo(context.spToPixels(5f))
    }

    @Test fun integerEdit_testSetValues() {
        val integerEdit = nonDefaultValuesIntegerEdit()
        integerEdit.value = 50
        assertThat(integerEdit.value).isEqualTo(50)
    }

    @Test fun integerEdit_testIncrementationAndStepSize() {
        val ie = nonDefaultValuesIntegerEdit()
        ie.increment()
        assertThat(ie.value).isEqualTo(37)
        ie.stepSize = 8
        ie.increment()
        assertThat(ie.value).isEqualTo(45)
        ie.decrement()
        assertThat(ie.value).isEqualTo(37)
        ie.stepSize = 20
        ie.decrement()
        assertThat(ie.value).isEqualTo(17)
    }

    @Test fun integerEdit_testDecreaseIncreaseButtons() {
        val ie = nonDefaultValuesIntegerEdit()
        ie.ui.increaseButton.performClick()
        assertThat(ie.value).isEqualTo(37)
        ie.stepSize = 8
        ie.ui.increaseButton.performClick()
        assertThat(ie.value).isEqualTo(45)
        ie.ui.decreaseButton.performClick()
        assertThat(ie.value).isEqualTo(37)
        ie.stepSize = 20
        ie.ui.decreaseButton.performClick()
        assertThat(ie.value).isEqualTo(17)
    }

    @Test fun integerEdit_testMinMaxValues() {
        val ie = nonDefaultValuesIntegerEdit()
        ie.value = ie.minValue - 4
        assertThat(ie.value).isEqualTo(ie.minValue)
        ie.value = ie.maxValue + 10
        assertThat(ie.value).isEqualTo(ie.maxValue)

        ie.value = ie.minValue + 1
        ie.decrement()
        assertThat(ie.value).isEqualTo(ie.minValue)

        ie.value = ie.maxValue - 1
        ie.increment()
        assertThat(ie.value).isEqualTo(ie.maxValue)

        ie.value = 50
        ie.minValue = 60
        assertThat(ie.value).isEqualTo(ie.minValue)

        ie.value = 400
        ie.maxValue = 350
        assertThat(ie.value).isEqualTo(ie.maxValue)

        ie.value = 200
        ie.minValue = 100
        ie.maxValue = 300
        assertThat(ie.value).isEqualTo(200)
    }

    @Test fun integerEdit_testValueChangedListener() {
        val ie = defaultValuesIntegerEdit()
        var test = 0
        ie.onValueChangedListener = { value -> test = value }
        ie.value = 5
        assertThat(test).isEqualTo(ie.value)
    }

    @Test fun integerEdit_testValueIsFocusable() {
        val ie = defaultValuesIntegerEdit()
        assertThat(ie.ui.valueEdit.isFocusableInTouchMode).isFalse()
        ie.valueIsFocusable = true
        assertThat(ie.ui.valueEdit.isFocusableInTouchMode).isTrue()
        assertThat(ie.ui.valueEdit.minWidth).isEqualTo(
            context.resources.getDimensionPixelSize(R.dimen.integer_edit_editable_value_min_width))
    }
}
