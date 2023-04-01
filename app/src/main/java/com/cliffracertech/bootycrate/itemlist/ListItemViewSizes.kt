/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

open class ListItemViewSizes(
    val verticalPadding: Dp = 8.dp,
    private val uneditableNameHeight: Dp = 24.dp,
    private val uneditableExtraInfoHeight: Dp = 20.dp
) {
    private val colorPickerOptionSize = 48.dp
    private val colorPickerSize = verticalPadding * 2 + colorPickerOptionSize * 2

    private val editableNameHeight = maxOf(48.dp, uneditableNameHeight)
    private val editableExtraInfoHeight = maxOf(48.dp, uneditableExtraInfoHeight)
    open val editableHeight: Dp =
        verticalPadding * 2 + editableNameHeight + editableExtraInfoHeight

    val uneditableHeight = verticalPadding * 2 +
        uneditableNameHeight + uneditableExtraInfoHeight

    fun height(showingColorPicker: Boolean, isEditable: Boolean) = when {
        showingColorPicker -> colorPickerSize
        isEditable ->         editableHeight
        else ->               uneditableHeight
    }

    fun nameHeight(isEditable: Boolean) =
        if (isEditable) editableNameHeight
        else            uneditableNameHeight
    fun extraInfoHeight(isEditable: Boolean) =
        if (isEditable) editableExtraInfoHeight
        else            uneditableExtraInfoHeight
}

class InventoryItemViewSizes(
    verticalPadding: Dp = 8.dp,
    uneditableNameHeight: Dp = 24.dp,
    uneditableExtraInfoHeight: Dp = 20.dp,
    autoAddToShoppingListHeight: Dp = 48.dp
): ListItemViewSizes(verticalPadding, uneditableNameHeight, uneditableExtraInfoHeight) {
    override val editableHeight =
        super.editableHeight + autoAddToShoppingListHeight
}