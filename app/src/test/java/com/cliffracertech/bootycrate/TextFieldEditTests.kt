/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracertech.bootycrate

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextFieldEditTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun defaultValuesTFE(): TextFieldEdit {
        val attrs = Robolectric.buildAttributeSet().build()
        return TextFieldEdit(context, attrs)
    }

    private fun nonDefaultValuesTFE(): TextFieldEdit {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.isEditable, "true")
            .addAttribute(R.attr.canBeBlank, "false")
            .addAttribute(android.R.attr.text, "test")
            .build()
        return TextFieldEdit(context, attrs)
    }

    @Test fun TextFieldEdit_testDefaultValues() {
        val tfe = defaultValuesTFE()
        assertThat(tfe.isEditable).isFalse()
        assertThat(tfe.canBeBlank).isTrue()
        assertThat(tfe.isFocusableInTouchMode).isFalse()
    }

    @Test fun TextFieldEdit_testNonDefaultValues() {
        val tfe = nonDefaultValuesTFE()
        assertThat(tfe.isEditable).isTrue()
        assertThat(tfe.canBeBlank).isFalse()
        assertThat(tfe.text.toString()).isEqualTo("test")
        assertThat(tfe.isFocusableInTouchMode).isTrue()
        assertThat(tfe.minHeight).isEqualTo(context.resources.getDimensionPixelSize(R.dimen.editable_text_field_min_height))
    }
}