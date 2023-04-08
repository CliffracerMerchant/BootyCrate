/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

open class ListItemViewSizes(
    val verticalPadding: Dp = 4.dp,
    private val uneditableNameHeight: Dp = 27.dp,
    private val uneditableExtraInfoHeight: Dp = 21.dp
) {
    private val colorPickerOptionSize = 48.dp
    private val colorPickerSize = verticalPadding * 2 + colorPickerOptionSize * 2

    private val editableNameHeight = maxOf(48.dp, uneditableNameHeight)
    private val editableExtraInfoHeight = maxOf(48.dp, uneditableExtraInfoHeight)

    open val editableHeight: Dp =
        verticalPadding * 2 + editableNameHeight + editableExtraInfoHeight

    val uneditableHeight: Dp =
        verticalPadding * 2 + uneditableNameHeight + uneditableExtraInfoHeight

    fun height(showingColorPicker: Boolean, isEditable: Boolean) = when {
        showingColorPicker -> colorPickerSize
        isEditable ->         editableHeight
        else ->               uneditableHeight
    }

    private val nameHeightChange = editableNameHeight - uneditableNameHeight
    fun nameTopOffset(interpolation: Float) =
        uneditableNameHeight + nameHeightChange * interpolation

    private val extraInfoHeightChange = editableExtraInfoHeight - uneditableExtraInfoHeight
    fun extraInfoTopOffset(interpolation: Float) =
        nameTopOffset(interpolation) + uneditableExtraInfoHeight + extraInfoHeightChange * interpolation

    private val colorIndicatorMaxTopOffset =
        (editableNameHeight + editableExtraInfoHeight - 48.dp) / 2f
    fun colorIndicatorTopOffset(interpolation: Float) =
        colorIndicatorMaxTopOffset * interpolation

    fun amountEditEndOffset(interpolation: Float) =
        (-48).dp * (1f - interpolation)

    private val otherContentMaxTopOffset =
        editableNameHeight + editableExtraInfoHeight
    fun otherContentTopOffset(interpolation: Float) =
        otherContentMaxTopOffset * (interpolation / 2f + 0.5f)

    protected open val editableHeightChange = height(false, true) - height(false, false)

    fun editButtonTopOffset(interpolation: Float) =
        editableHeightChange * interpolation
}

class InventoryItemViewSizes(
    verticalPadding: Dp = 4.dp,
    uneditableNameHeight: Dp = 27.dp,
    uneditableExtraInfoHeight: Dp = 21.dp,
    val autoAddToShoppingListHeight: Dp = 48.dp
): ListItemViewSizes(verticalPadding, uneditableNameHeight, uneditableExtraInfoHeight) {
    override val editableHeight =
        super.editableHeight + autoAddToShoppingListHeight

    override val editableHeightChange = height(false, true) - height(false, false)

}