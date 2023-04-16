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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Return the minimum required DpSize to display the single line [text],
 * using the provided [textStyle], [density], and [fontFamilyResolver]. */
private fun singleLineTextSize(
    text: String,
    textStyle: TextStyle,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): DpSize {
    val paragraph = Paragraph(
        text = text,
        style = textStyle,
        maxLines = 1,
        ellipsis = false,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
        constraints = Constraints())
    return with (density) {
        DpSize(paragraph.minIntrinsicWidth.toDp(),
               paragraph.height.toDp())
    }
}

class ListItemViewSizes(
    private val maxWidth: Dp,
    val verticalPadding: Dp,
    val otherContentHeight: Dp,
    val nameTextStyle: TextStyle,
    val extraInfoTextStyle: TextStyle,
    amountTextStyle: TextStyle,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
) {
    val colorIndicatorSize = 48.dp
    private val colorPickerOptionSize = 48.dp
    private val colorPickerSize = verticalPadding * 2 + colorPickerOptionSize * 2

    val amountEditSizes = AmountEditSizes(amountTextStyle, fontFamilyResolver, density)

    private val uneditableNameHeight =
        singleLineTextSize("A", nameTextStyle, density, fontFamilyResolver).height
    private val editableNameHeight = maxOf(48.dp, uneditableNameHeight)
    private val nameHeightChange = editableNameHeight - uneditableNameHeight
    fun nameHeight(isEditable: Boolean) =
        if (isEditable) editableNameHeight
        else            uneditableNameHeight

    private val uneditableExtraInfoHeight =
        singleLineTextSize("A", extraInfoTextStyle, density, fontFamilyResolver).height
    private val editableExtraInfoHeight = maxOf(48.dp, uneditableExtraInfoHeight)
    private val extraInfoHeightChange = editableExtraInfoHeight - uneditableExtraInfoHeight
    fun extraInfoHeight(isEditable: Boolean) =
        if (isEditable) editableExtraInfoHeight
        else            uneditableExtraInfoHeight

    private val uneditableHeight = verticalPadding * 2 + maxOf(uneditableNameHeight + uneditableExtraInfoHeight, 48.dp)
    private val editableHeight = verticalPadding * 2 + editableNameHeight + editableExtraInfoHeight + otherContentHeight

    fun height(showingColorPicker: Boolean, isEditable: Boolean) = when {
        showingColorPicker -> colorPickerSize
        isEditable ->         editableHeight
        else ->               uneditableHeight
    }

    private val uneditableTextFieldWidth: Dp = run {
        val editButtonSize = 48.dp
        maxWidth - colorIndicatorSize - editButtonSize - amountEditSizes.width(false)
    }

    private val editableTextFieldWidth: Dp = run {
        maxWidth - colorIndicatorSize - amountEditSizes.width(true)
    }

    /** Return the width that the name and extra info text field edits should
     * occupy, depending on whether or not the [ListItemView] [isEditable]. */
    fun textFieldWidth(isEditable: Boolean): Dp =
        if (isEditable) editableTextFieldWidth
        else            uneditableTextFieldWidth

    /** The y offset of the name when it is not editable and the extra
     * info is not blank. This will often be 0.dp, but may be a non-zero
     * value for small enough text sizes. */
    private val uneditableNameOffsetY =
        (maxOf(48.dp, uneditableNameHeight + uneditableExtraInfoHeight) - uneditableNameHeight - uneditableExtraInfoHeight) / 2f
    /** The y offset of the name when it is not editable and the extra
     * info is blank. This will usually be more than 0.dp, but may be
     * zero for large text sizes. */
    private val uneditableNameOffsetYWithoutExtraInfo =
        (maxOf(48.dp, uneditableNameHeight) - uneditableNameHeight) / 2f

    /** The y offset of the name, depending on whether or not the [ListItemView]
     * [isEditable] and whether or not [extraInfoIsBlank]. [editableTransitionProgress]
     * can be used in order to interpolate the returned value between its values
     * when [isEditable] is true or false (corresponding to a [editableTransitionProgress]
     * value of 1f or 0f, respectively). */
    fun nameOffsetY(
        isEditable: Boolean,
        extraInfoIsBlank: Boolean,
        editableTransitionProgress: Float = if (isEditable) 1f else 0f
    ): Dp {
        val uneditableNameOffsetY = if (!extraInfoIsBlank) uneditableNameOffsetY
                                    else uneditableNameOffsetYWithoutExtraInfo
        val topChangeOffset = uneditableNameOffsetY * (1f - editableTransitionProgress)

        val heightChangeOffset = if (!isEditable) nameHeightChange / 2f * editableTransitionProgress
                                 else -nameHeightChange / 2f * (1f - editableTransitionProgress)

        return topChangeOffset + heightChangeOffset
    }

    private val uneditableExtraInfoOffsetY = uneditableNameOffsetY + uneditableNameHeight
    private val extraInfoOffsetYChange = nameHeightChange - uneditableNameOffsetY
    fun extraInfoOffsetY(
        isEditable: Boolean,
        interpolation: Float
    ): Dp {
        val topChangeOffset = uneditableExtraInfoOffsetY + extraInfoOffsetYChange * interpolation

        val heightChangeOffset = if (!isEditable) extraInfoHeightChange / 2f * interpolation
                                 else -extraInfoHeightChange / 2f * (1f - interpolation)

        return topChangeOffset + heightChangeOffset
    }

    private val colorIndicatorMaxTopOffset =
        (editableNameHeight + editableExtraInfoHeight - 48.dp) / 2f
    fun colorIndicatorTopOffset(interpolation: Float) =
        colorIndicatorMaxTopOffset * interpolation

    private val amountEditMaxOffsetX = editableTextFieldWidth - uneditableTextFieldWidth
    fun amountEditOffsetX(
        isEditable: Boolean,
        interpolation: Float
    ) = if (isEditable) -amountEditMaxOffsetX * (1f - interpolation)
        else            amountEditMaxOffsetX * interpolation - 48.dp

    private val otherContentMaxTopOffset =
        editableNameHeight + editableExtraInfoHeight
    fun otherContentTopOffset(interpolation: Float) =
        otherContentMaxTopOffset * (interpolation / 2f + 0.5f)

    private val editButtonMaxYOffset = editableHeight - uneditableHeight
    fun editButtonOffsetY(interpolation: Float) =
        editButtonMaxYOffset * interpolation
}

@Composable fun rememberListItemViewSizes(
    maxWidth: Dp,
    verticalPadding: Dp = 8.dp,
    otherContentHeight: Dp = 0.dp,
    nameTextStyle: TextStyle = MaterialTheme.typography.body1,
    extraInfoTextStyle: TextStyle = MaterialTheme.typography.subtitle1,
    amountTextStyle: TextStyle = MaterialTheme.typography.h6.copy(textAlign = TextAlign.Center)
): ListItemViewSizes {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    return remember(density) {
        ListItemViewSizes(
            maxWidth, verticalPadding, otherContentHeight,
            nameTextStyle, extraInfoTextStyle, amountTextStyle,
            density, fontFamilyResolver)
    }
}

@Composable fun rememberInventoryItemViewSizes(
    maxWidth: Dp,
    verticalPadding: Dp = 8.dp,
    nameTextStyle: TextStyle = MaterialTheme.typography.body1,
    extraInfoTextStyle: TextStyle = MaterialTheme.typography.subtitle1,
    amountTextStyle: TextStyle = MaterialTheme.typography.h6.copy(textAlign = TextAlign.Center)
) = rememberListItemViewSizes(maxWidth, verticalPadding, otherContentHeight = 48.dp,
                              nameTextStyle, extraInfoTextStyle, amountTextStyle)

class AmountEditSizes(
    val textStyle: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
    density: Density,
) {
    private val unfocusableValueWidth: Dp
    val height: Dp

    init {
        val valueSize = singleLineTextSize("88", textStyle, density, fontFamilyResolver)
        height = maxOf(valueSize.height, 48.dp)
        // When the value is not focusable, the decrease and increase buttons are
        // allowed to overlap the value by up to 10.dp to save horizontal space.
        // This 20.dp min width for an unfocusable value ensures that the decrease
        // and increase buttons won't overlap each other in this case.
        unfocusableValueWidth = (valueSize.width + 8.dp).coerceAtLeast(20.dp)
    }
    private val focusableValueWidth = maxOf(unfocusableValueWidth, 48.dp)

    fun valueWidth(isFocusable: Boolean) =
        if (isFocusable) focusableValueWidth
        else             unfocusableValueWidth

    private val unfocusableWidth = 38.dp * 2 + unfocusableValueWidth
    private val focusableWidth = 48.dp * 2 + focusableValueWidth
    private val widthChange = focusableWidth - unfocusableWidth

    fun width(valueIsFocusable: Boolean): Dp =
        if (valueIsFocusable) focusableWidth
        else                  unfocusableWidth

    fun increaseButtonXOffset(isFocusable: Boolean, interpolation: Float): Dp =
        -widthChange * if (isFocusable) (1f - interpolation)
                       else             -interpolation

    fun valueXOffset(isFocusable: Boolean, interpolation: Float): Dp =
        increaseButtonXOffset(isFocusable, interpolation) * 0.5f
}