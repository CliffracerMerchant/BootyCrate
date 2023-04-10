/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

open class ListItemViewSizes(
    fontFamilyResolver: FontFamily.Resolver,
    density: Density,
    val verticalPadding: Dp = 8.dp,
    private val nameTextStyle: TextStyle,
    private val extraInfoTextStyle: TextStyle,
) {
    private val colorPickerOptionSize = 48.dp
    private val colorPickerSize = verticalPadding * 2 + colorPickerOptionSize * 2

    private val uneditableNameHeight = with (density) {
        Paragraph(
            text = "A",
            style = nameTextStyle,
            maxLines = 1,
            ellipsis = false,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            constraints = Constraints()
        ).height.toDp()
    }
    private val editableNameHeight = maxOf(48.dp, uneditableNameHeight)
    private val nameHeightChange = editableNameHeight - uneditableNameHeight
    fun nameHeight(isEditable: Boolean) =
        if (isEditable) editableNameHeight
        else            uneditableNameHeight

    private val uneditableExtraInfoHeight = with (density) {
        Paragraph(
            text = "A",
            style = extraInfoTextStyle,
            maxLines = 1,
            ellipsis = false,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            constraints = Constraints()
        ).height.toDp()
    }
    private val editableExtraInfoHeight = maxOf(48.dp, uneditableExtraInfoHeight)
    private val extraInfoHeightChange = editableExtraInfoHeight - uneditableExtraInfoHeight
    fun extraInfoHeight(isEditable: Boolean) =
        if (isEditable) editableExtraInfoHeight
        else            uneditableExtraInfoHeight

    val uneditableHeight = verticalPadding * 2 + maxOf(uneditableNameHeight + uneditableExtraInfoHeight, 48.dp)
    open val editableHeight = verticalPadding * 2 + editableNameHeight + editableExtraInfoHeight

    fun height(showingColorPicker: Boolean, isEditable: Boolean) = when {
        showingColorPicker -> colorPickerSize
        isEditable ->         editableHeight
        else ->               uneditableHeight
    }

    private val uneditableNameTopOffset =
        (maxOf(48.dp, uneditableNameHeight + uneditableExtraInfoHeight) - uneditableNameHeight - uneditableExtraInfoHeight) / 2f
    private val uneditableNameTopOffsetWithoutExtraInfo =
        (maxOf(48.dp, uneditableNameHeight) - uneditableNameHeight) / 2f

    fun nameTopOffset(
        isEditable: Boolean,
        extraInfoIsBlank: Boolean,
        interpolation: Float
    ): Dp {
        val uneditableTopOffset = if (!extraInfoIsBlank) uneditableNameTopOffset
                                  else uneditableNameTopOffsetWithoutExtraInfo
        val topChangeOffset = uneditableTopOffset * (1f - interpolation)

        val heightChangeOffset = if (!isEditable) nameHeightChange / 2f * interpolation
                                 else -nameHeightChange / 2f * (1f - interpolation)

        return topChangeOffset + heightChangeOffset
    }

    private val uneditableExtraInfoTopOffset = uneditableNameTopOffset + uneditableNameHeight
    private val editableExtraInfoTopOffset get() = editableNameHeight
    fun extraInfoTopOffset(
        isEditable: Boolean,
        interpolation: Float
    ): Dp {
        val topChangeOffset = editableExtraInfoTopOffset * interpolation +
                              uneditableExtraInfoTopOffset * (1f - interpolation)

        val heightChangeOffset = if (!isEditable) extraInfoHeightChange / 2f * interpolation
                                 else -extraInfoHeightChange / 2f * (1f - interpolation)

        return topChangeOffset + heightChangeOffset
    }

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

@Composable fun rememberListItemViewSizes(
    verticalPadding: Dp = 8.dp,
    nameTextStyle: TextStyle = MaterialTheme.typography.body1,
    extraInfoTextStyle: TextStyle = MaterialTheme.typography.subtitle1,
): ListItemViewSizes {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    return remember(density) {
        ListItemViewSizes(
            fontFamilyResolver, density, verticalPadding,
            nameTextStyle, extraInfoTextStyle)
    }
}

class InventoryItemViewSizes(
    fontFamilyResolver: FontFamily.Resolver,
    density: Density,
    verticalPadding: Dp = 8.dp,
    nameTextStyle: TextStyle,
    extraInfoTextStyle: TextStyle,
    val autoAddToShoppingListHeight: Dp = 48.dp
): ListItemViewSizes(
    fontFamilyResolver, density, verticalPadding,
    nameTextStyle, extraInfoTextStyle
) {
    override val editableHeight =
        super.editableHeight + autoAddToShoppingListHeight

    override val editableHeightChange = height(false, true) - height(false, false)
}

@Composable fun rememberInventoryItemViewSizes(
    verticalPadding: Dp = 8.dp,
    nameTextStyle: TextStyle = MaterialTheme.typography.body1,
    extraInfoTextStyle: TextStyle = MaterialTheme.typography.subtitle1,
): InventoryItemViewSizes {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    return remember(density) {
        InventoryItemViewSizes(
            fontFamilyResolver, density, verticalPadding,
            nameTextStyle, extraInfoTextStyle)
    }
}