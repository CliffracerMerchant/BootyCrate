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

/**
 * A container for size measurements for a [ListItemView].
 *
 * The entire [ListItemView]'s height and vertical padding should match the
 * value returned by [height] and the value of [internalVerticalPadding], respectively.
 * The [ListItemView]'s sub-components should use the sizes, widths, or heights
 * returned by [colorIndicatorSize], [nameHeight], [extraInfoHeight], [textFieldWidth]
 * (which applies to both the name and extra info), and [editButtonSize].
 * Likewise, the sub-components should have an x or y offset applied in a
 * graphical layer that matches the [Dp] values returned by [colorIndicatorOffsetY],
 * [nameOffsetY], [extraInfoOffsetY], [amountEditOffsetX], [otherContentOffsetY],
 * and [editButtonOffsetY].
 *
 * @param maxWidth The maximum [Dp] width that the [ListItemView] should take up
 * @param internalVerticalPadding The vertical padding that the
 *     item view should have inside its background
 * @param externalHorizontalPadding The horizontal padding that the item
 *     view should have outside its background. To allow its swipe to delete
 *     background to reach the edges of the container, the item view should
 *     use a [horizontalSwipeToDeleteSurface] [Modifier], with its
 *     horizontalContentPadding set to equal externalHorizontalPadding.
 *     Due to the components at either end of the item view having their
 *     own horizontal padding, the internal (i.e. inside its background)
 *     horizontal padding of the ListItemView should be zero.
 * @param otherContentHeight The [Dp] height of the other content that will be
 *     displayed at the bottom of the [ListItemView] when in its expanded state
 * @param nameTextStyle The text style that the name text field should use
 * @param extraInfoTextStyle The text style that the extra info text field should use
 * @param amountTextStyle The text style that the amount edit's value should use
 * @param fontFamilyResolver The local [FontFamily.Resolver] in use
 * @param density The local [Density] instance in use
 */
class ListItemViewSizes(
    maxWidth: Dp,
    val internalVerticalPadding: Dp,
    val externalHorizontalPadding: Dp,
    val otherContentHeight: Dp,
    val nameTextStyle: TextStyle,
    val extraInfoTextStyle: TextStyle,
    amountTextStyle: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
    density: Density,
) {
    /** The [Dp] size (i.e. width and height) that the color indicator should use */
    val colorIndicatorSize get() = 48.dp
    private val colorPickerOptionSize get() = 48.dp
    private val colorPickerSize = internalVerticalPadding * 2 + colorPickerOptionSize * 2

    /** The [AmountEditSizes] instance that the amount edit should use */
    val amountEditSizes = AmountEditSizes(amountTextStyle, fontFamilyResolver, density)

    private val uneditableNameHeight =
        singleLineTextSize("A", nameTextStyle, density, fontFamilyResolver).height
    private val editableNameHeight = maxOf(48.dp, uneditableNameHeight)
    private val nameHeightChange = editableNameHeight - uneditableNameHeight

    private val uneditableExtraInfoHeight =
        singleLineTextSize("A", extraInfoTextStyle, density, fontFamilyResolver).height
    private val editableExtraInfoHeight = maxOf(48.dp, uneditableExtraInfoHeight)
    private val extraInfoHeightChange = editableExtraInfoHeight - uneditableExtraInfoHeight

    private val uneditableTextFieldsHeight get() = uneditableNameHeight + uneditableExtraInfoHeight
    private val editableTextFieldsHeight get() = editableNameHeight + editableExtraInfoHeight
    private val uneditableHeight =
        internalVerticalPadding * 2 + maxOf(uneditableTextFieldsHeight, 48.dp)
    private val editableHeight =
        internalVerticalPadding * 2 + editableTextFieldsHeight + otherContentHeight

    val editButtonSize get() = 48.dp
    private val uneditableTextFieldWidth: Dp =
        maxWidth - externalHorizontalPadding * 2 - colorIndicatorSize - editButtonSize - amountEditSizes.width(false)
    private val editableTextFieldWidth: Dp =
        maxWidth - externalHorizontalPadding * 2 - colorIndicatorSize - amountEditSizes.width(true)

    /** The height of the entire item view  */
    fun height(showingColorPicker: Boolean, isExpanded: Boolean) = when {
        showingColorPicker -> colorPickerSize
        isExpanded ->         editableHeight
        else ->               uneditableHeight
    }

    /** The [Dp] height that the name text field should
     * use, depending on whether or not it [isExpanded] */
    fun nameHeight(isExpanded: Boolean) =
        if (isExpanded) editableNameHeight
        else            uneditableNameHeight

    /** The [Dp] height that the extra info text field should
     * use, depending on whether or not it [isExpanded] */
    fun extraInfoHeight(isExpanded: Boolean) =
        if (isExpanded) editableExtraInfoHeight
        else            uneditableExtraInfoHeight

    /** Return the width that the name and extra info text field edits should
     * occupy, depending on whether or not the [ListItemView] is expanded */
    fun textFieldWidth(isExpanded: Boolean): Dp =
        if (isExpanded) editableTextFieldWidth
        else            uneditableTextFieldWidth

    private val colorIndicatorMinOffsetY: Dp =
        (maxOf(uneditableTextFieldsHeight, 48.dp) - 48.dp) / 2f
    private val colorIndicatorOffsetYChange: Dp = run {
        val maxOffset = (editableTextFieldsHeight - 48.dp) / 2f
        maxOffset - colorIndicatorMinOffsetY
    }
    /** The y offset of the color indicator, depending on the progress of the
     * expand/collapsed animation. [interpolation] should be in the range of [0, 1f],
     * corresponding to when the view is collapsed or expanded, respectively.*/
    fun colorIndicatorOffsetY(interpolation: Float) =
        colorIndicatorMinOffsetY + colorIndicatorOffsetYChange * interpolation

    /** The y offset of the name when it is not editable and the extra
     * info is not blank. This will often be 0.dp, but may be a non-zero
     * value for small enough text sizes. */
    private val uneditableNameOffsetY =
        (maxOf(48.dp, uneditableTextFieldsHeight) - uneditableTextFieldsHeight) / 2f
    /** The y offset of the name when it is not editable and the extra
     * info is blank. This will usually be more than 0.dp, but may be
     * zero for large text sizes. */
    private val uneditableNameOffsetYWithoutExtraInfo =
        (maxOf(48.dp, uneditableNameHeight) - uneditableNameHeight) / 2f

    /** The y offset of the name, depending on whether or not the view [isExpanded]
     * and whether or not [extraInfoIsBlank]. [interpolation] can be used in order
     * to interpolate the returned value between its values when [isExpanded] is true
     * or false (corresponding to a [interpolation] value of 1f or 0f, respectively). */
    fun nameOffsetY(
        isExpanded: Boolean,
        extraInfoIsBlank: Boolean,
        interpolation: Float = if (isExpanded) 1f else 0f
    ): Dp {
        val uneditableNameOffsetY = if (!extraInfoIsBlank) uneditableNameOffsetY
                                    else uneditableNameOffsetYWithoutExtraInfo
        val topChangeOffset = uneditableNameOffsetY * (1f - interpolation)

        val heightChangeOffset = if (!isExpanded) nameHeightChange / 2f * interpolation
                                 else -nameHeightChange / 2f * (1f - interpolation)

        return topChangeOffset + heightChangeOffset
    }

    private val uneditableExtraInfoOffsetY = uneditableNameOffsetY + uneditableNameHeight
    private val extraInfoOffsetYChange = nameHeightChange - uneditableNameOffsetY
    /** The y offset of the extra info, depending on whether or not the view [isExpanded].
     * [interpolation] can be used in order to interpolate the returned value between its
     * values when [isExpanded] is true or false (corresponding to a [interpolation] value
     * of 1f or 0f, respectively). */
    fun extraInfoOffsetY(
        isExpanded: Boolean,
        interpolation: Float = if (isExpanded) 1f else 0f
    ): Dp {
        val topChangeOffset = uneditableExtraInfoOffsetY + extraInfoOffsetYChange * interpolation

        val heightChangeOffset = if (!isExpanded) extraInfoHeightChange / 2f * interpolation
                                 else -extraInfoHeightChange / 2f * (1f - interpolation)

        return topChangeOffset + heightChangeOffset
    }

    private val amountEditMaxOffsetX = editableTextFieldWidth - uneditableTextFieldWidth
    /** The x offset of the amount edit, depending on whether or not the view [isExpanded].
     * [interpolation] can be used in order to interpolate the returned value between its
     * values when [isExpanded] is true or false (corresponding to a [interpolation] value
     * of 1f or 0f, respectively). */
    fun amountEditOffsetX(
        isExpanded: Boolean,
        interpolation: Float = if (isExpanded) 1f else 0f
    ) = if (isExpanded) -amountEditMaxOffsetX * (1f - interpolation)
        else            amountEditMaxOffsetX * interpolation - 48.dp

    private val otherContentMaxOffsetY = editableNameHeight + editableExtraInfoHeight
    /** The y offset for additional content passed in the [ListItemView]'s
     * otherContent parameter. [interpolation] should be in the range of [0, 1f],
     * corresponding to when the view is collapsed or expanded, respectively. */
    fun otherContentOffsetY(interpolation: Float) =
        otherContentMaxOffsetY * (interpolation / 2f + 0.5f)

    private val editButtonMaxYOffset = editableHeight - uneditableHeight
    /** The y offset for the edit/collapse button. [interpolation] should be
     * in the range of [0, 1f], corresponding to when the view is collapsed or
     * expanded, respectively. */
    fun editButtonOffsetY(interpolation: Float) = editButtonMaxYOffset * interpolation

    val swipeableAnchors = with(density) {
        SwipeToDeleteState.anchors(maxWidth.toPx())
    }
}

/** Return a remembered [ListItemViewSizes] instance. */
@Composable fun rememberListItemViewSizes(
    maxWidth: Dp,
    internalVerticalPadding: Dp = 8.dp,
    externalHorizontalPadding: Dp = 8.dp,
    otherContentHeight: Dp = 0.dp,
    nameTextStyle: TextStyle = MaterialTheme.typography.body1,
    extraInfoTextStyle: TextStyle = MaterialTheme.typography.subtitle1,
    amountTextStyle: TextStyle = MaterialTheme.typography.h6.copy(textAlign = TextAlign.Center)
): ListItemViewSizes {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    return remember(density) {
        ListItemViewSizes(
            maxWidth, internalVerticalPadding,
            externalHorizontalPadding, otherContentHeight,
            nameTextStyle, extraInfoTextStyle, amountTextStyle,
            fontFamilyResolver, density)
    }
}

/** Return a remember [ListItemViewSizes] instance with a
 * [ListItemViewSizes.otherContentHeight] parameter value
 * appropriate for an [InventoryItemView]. */
@Composable fun rememberInventoryItemViewSizes(
    maxWidth: Dp,
    internalVerticalPadding: Dp = 8.dp,
    externalHorizontalPadding: Dp = 8.dp,
    nameTextStyle: TextStyle = MaterialTheme.typography.body1,
    extraInfoTextStyle: TextStyle = MaterialTheme.typography.subtitle1,
    amountTextStyle: TextStyle = MaterialTheme.typography.h6.copy(textAlign = TextAlign.Center)
): ListItemViewSizes {
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val otherContentHeight = remember {
        val textSize = singleLineTextSize(
            "A", amountTextStyle, density, fontFamilyResolver)
        maxOf(textSize.height, 48.dp)
    }
    return rememberListItemViewSizes(
        maxWidth, internalVerticalPadding,
        externalHorizontalPadding, otherContentHeight,
        nameTextStyle, extraInfoTextStyle, amountTextStyle)
}

/**
 * A container for size measurements for an [AmountEdit].
 *
 * The value returned by [width] and the value of [height] should be used as
 * width and height for the entire amount edit. The value returned by [valueWidth]
 * should be used for the amount edit's value's width. The values returned by
 * [valueXOffset] and [increaseButtonXOffset] should be applied in graphical
 * layers as the translationX for the value and increase button, respectively.
 *
 * @param textStyle The [TextStyle] to use for the amount edit
 * @param fontFamilyResolver The local [FontFamily.Resolver] in use
 * @param density The local [Density] instance in use
 */
class AmountEditSizes(
    val textStyle: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
    density: Density,
) {
    val height: Dp
    private val unfocusableValueWidth: Dp

    init {
        val valueSize = singleLineTextSize(
            "99", textStyle, density, fontFamilyResolver)

        height = maxOf(valueSize.height, 48.dp)

        val valuePadding = 8.dp
        val valueWidth = valueSize.width + valuePadding
        // When the value is not focusable, the decrease and increase buttons are
        // allowed to overlap the value by up to 10.dp to save horizontal space.
        // This 20.dp min width for an unfocusable value ensures that the decrease
        // and increase buttons won't overlap each other in this case.
        unfocusableValueWidth = (valueWidth).coerceAtLeast(20.dp)
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