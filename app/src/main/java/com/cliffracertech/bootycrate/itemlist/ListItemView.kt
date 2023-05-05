/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.defaultSpring
import com.cliffracertech.bootycrate.model.database.ListItem

fun Modifier.minTouchTargetSize() =
    this.sizeIn(minWidth = 48.dp, minHeight = 48.dp)

/** An interface containing callbacks for ListItem related interactions.
 * The id parameter in each callback identifies the item that was the
 * target of the interaction */
interface ListItemCallback {
    /** The callback that will be invoked when the item is clicked */
    fun onClick(id: Long)
    /** The callback that will be invoked when the item is long clicked */
    fun onLongClick(id: Long)
    /** The callback that will be invoked when the item is swiped off the screen */
    fun onSwipe(id: Long)
    /** The callback that will be invoked when the item's color indicator is clicked */
    fun onColorIndicatorClick(id: Long)
    /** The callback that will be invoked when the given [colorGroup]
     * has been clicked in the item's color group picker */
    fun onColorGroupClick(id: Long, colorGroup: ListItem.ColorGroup)
    /** The callback that will be invoked when the item's
     * name has been requested to be changed to [newName] */
    fun onRenameRequest(id: Long, newName: String)
    /** The callback that will be invoked when the item's extraInfo
     * has been requested to be changed to [newExtraInfo] */
    fun onExtraInfoChangeRequest(id: Long, newExtraInfo: String)
    /**  The callback that will be invoked when the item's amount
     * has been requested to be changed to [newAmount] */
    fun onAmountChangeRequest(id: Long, newAmount: Int)
    /** The callback that will be invoked when the item's edit button is clicked */
    fun onEditButtonClick(id: Long)
    /** Whether or not the edit button should be shown */
    val showEditButton: Boolean
}

@Composable private fun ListItemViewTextFields(
    sizes: ListItemViewSizes,
    editable: Boolean,
    editableTransitionProgressGetter: () -> Float,
    fullyCollapsed: Boolean,
    id: Long,
    name: String,
    extraInfo: String,
    tint: Color,
    callback: ListItemCallback,
) = Box(Modifier
    // We want the text fields to stay at their expanded editable widths until
    // the view is fully collapsed to prevent the underlines from noticeably
    // changing in width at the beginning of the collapse animation.
    .width(sizes.textFieldWidth(!fullyCollapsed))
    .offset(x = sizes.colorIndicatorSize)
) {
    ListItemTextField(
        text = name,
        onTextChange = { callback.onRenameRequest(id, it) },
        modifier = Modifier
            .height(sizes.nameHeight(editable))
            .graphicsLayer {
                val interp = editableTransitionProgressGetter()
                translationY = sizes.nameOffsetY(
                    editable, extraInfo.isBlank(), interp).toPx()
            },
        tint = tint,
        editable = editable,
        editableTransitionProgressGetter = editableTransitionProgressGetter,
        textStyle = sizes.nameTextStyle)
    if (!fullyCollapsed || extraInfo.isNotBlank())
        ListItemTextField(
            text = extraInfo,
            onTextChange = { callback.onExtraInfoChangeRequest(id, it) },
            modifier = Modifier
                .height(sizes.extraInfoHeight(editable))
                .graphicsLayer {
                    translationY = sizes.extraInfoOffsetY(
                        editable, editableTransitionProgressGetter()).toPx()
                },
            tint = tint,
            editable = editable,
            editableTransitionProgressGetter = editableTransitionProgressGetter,
            textStyle = sizes.extraInfoTextStyle)
}

@Composable private fun BoxScope.ListItemViewAmountEdit(
    sizes: ListItemViewSizes,
    focusable: Boolean,
    focusableTransitionProgressGetter: () -> Float,
    id: Long,
    name: String,
    amount: Int,
    tint: Color,
    callback: ListItemCallback,
) = AmountEdit(
    sizes = sizes.amountEditSizes,
    amount = amount,
    focusable = focusable,
    tint = tint,
    decreaseDescription = stringResource(
        R.string.item_amount_decrease_description, name),
    increaseDescription = stringResource(
        R.string.item_amount_increase_description, name),
    onAmountChangeRequest = {
        callback.onAmountChangeRequest(id, it)
    }, modifier = Modifier
        .align(Alignment.TopEnd)
        .graphicsLayer {
            val interp = focusableTransitionProgressGetter()
            translationX = sizes.amountEditOffsetX(focusable, interp).toPx()
        },
    focusableTransitionProgressGetter = focusableTransitionProgressGetter)

@Composable private fun BoxScope.ListItemViewEditCollapseButton(
    sizes: ListItemViewSizes,
    editable: Boolean,
    editableTransitionProgressGetter: () -> Float,
    id: Long,
    name: String,
    callback: ListItemCallback,
) {
    if (callback.showEditButton)
        AnimatedEditToCloseButton(
            onClick = { callback.onEditButtonClick(id) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(sizes.editButtonSize)
                .graphicsLayer {
                    val interp = editableTransitionProgressGetter()
                    translationY = sizes.editButtonOffsetY(interp).toPx()
                },
            isEditable = editable,
            itemName = name)
}

@Composable private fun BoxScope.ListItemViewLinkedIndicator(
    appearanceProgressGetter: () -> Float
) = Icon(
    imageVector = Icons.Default.Link,
    contentDescription = null,
    modifier = Modifier
        .align(Alignment.TopEnd)
        .requiredSize(48.dp)
        .padding(12.dp)
        .offset(x = (-48).dp, y = 48.dp)
        .graphicsLayer {
            alpha = appearanceProgressGetter()
            translationY = -24.dp.toPx() * (1f - alpha)
        })

/** The inner content for a ListItemView when it is not showing its color picker. */
@Composable private fun ListItemViewRegularContent(
    sizes: ListItemViewSizes,
    id: Long,
    colorGroup: ListItem.ColorGroup,
    name: String,
    extraInfo: String,
    amount: Int,
    showLinkIndicator: Boolean,
    /** For some reason, passing isEditable as a boolean prevents this
     * content from skipping recomposition during the expand collapse
     * animations. Passing it as some other type prevents this. */
    editable: Int,
    callback: ListItemCallback,
    colorIndicator: @Composable (
            collapsed: Boolean,
            colorIndicatorOnClick: () -> Unit,
            modifier: Modifier,
        ) -> Unit,
    otherContent: @Composable (otherContentModifier: Modifier) -> Unit = {},
    colorIndicatorOnClick: () -> Unit,
) = Box(Modifier.heightIn(min = 48.dp)) {

    val editable = editable == 1
    val expansionProgress by animateFloatAsState(
        if (editable) 1f else 0f, defaultSpring())
    val collapsed by remember { derivedStateOf { expansionProgress == 0f }}

    val colors = ListItem.ColorGroup.colors()
    val tint = remember(colorGroup) {
        colors.getOrElse(colorGroup.ordinal) { colors.first() }
    }

    colorIndicator(
        collapsed , colorIndicatorOnClick,
        Modifier.graphicsLayer {
            translationY = sizes.colorIndicatorOffsetY(expansionProgress).toPx()
        })
    ListItemViewTextFields(
        sizes, editable, { expansionProgress },
        collapsed, id, name, extraInfo, tint, callback)
    ListItemViewAmountEdit(
        sizes, editable, { expansionProgress },
        id, name, amount, tint, callback)
    ListItemViewEditCollapseButton(
        sizes, editable,
        { expansionProgress },
        id, name, callback)

    val linkedIndicatorAppearanceProgress by animateFloatAsState(
        if (editable && showLinkIndicator) 1f else 0f, defaultSpring())
    if (linkedIndicatorAppearanceProgress > 0f)
        ListItemViewLinkedIndicator { linkedIndicatorAppearanceProgress }

    if (!collapsed)
        otherContent(Modifier.graphicsLayer {
            translationY = sizes.otherContentOffsetY(expansionProgress).toPx()
            alpha = expansionProgress
        })
}

/**
 * A visual display of a [ListItem] that also allows user interactions to
 * e.g. change the [ListItem]'s state.
 *
 * @param sizes The [ListItemViewSizes] instance to use for the view
 * @param id The [ListItem.id] for the item being represented
 * @param colorGroup The [ListItem.ColorGroup] that the item belongs to
 * @param name The name of the displayed item
 * @param extraInfo The extra info of the displayed item
 * @param amount The amount of the displayed item
 * @param selectionBrush The [Brush] that will be shown at half
 *     opacity over the normal background when selected is true
 * @param selected Whether or not the item is selected
 * @param expanded Whether or not the item will present itself in its
 *     expanded state. This will allow the text fields and the value of
 *     the amount edit to be keyboard focusable.
 * @param showColorPicker Whether or not the item should show its color picker
 * @param callback The [ListItemCallback] whose method implementations
 *     will be used as the callbacks for user interactions
 * @param modifier The [Modifier] that will be used for the root layout
 * @param colorIndicator A composable lambda whose contents will be used as
 *     the start aligned color indicator for the list item. Three parameters
 *     are provided: whether or not the view is fully collapsed, an onClick
 *     lambda to use for the content, and a [Modifier] should be used as the
 *     root modifier for the content.
 * @param otherContent A composable lambda whose contents will be displayed
 *     beneath the other content. If the provided [Modifier] is used as the
 *     base modifier for the content, it will automatically be animated in/
 *     out depending on the value of [isExpanded].
 */
@Composable fun ListItemView(
    sizes: ListItemViewSizes,
    id: Long,
    colorGroup: ListItem.ColorGroup,
    name: String,
    extraInfo: String,
    amount: Int,
    linked: Boolean,
    selectionBrush: Brush,
    selected: Boolean,
    expanded: Boolean,
    showColorPicker: Boolean,
    callback: ListItemCallback,
    modifier: Modifier = Modifier,
    colorIndicator: @Composable (
            isCollapsed: Boolean,
            onClick: () -> Unit,
            modifier: Modifier,
        ) -> Unit,
    otherContent: @Composable (otherContentModifier: Modifier) -> Unit = {},
) {
    val backgroundShape = MaterialTheme.shapes.large
    val selectionBackgroundAlpha by animateFloatAsState(
        targetValue = if (selected && !showColorPicker) 0.5f else 0f,
        animationSpec = tween(durationMillis = 300))
    val height by animateDpAsState(
        targetValue = sizes.height(showColorPicker, expanded),
        animationSpec = defaultSpring())

    AnimatedContent(
        targetState = showColorPicker,
        modifier = modifier
            .height(height)
            .horizontalSwipeToDeleteSurface(
                backgroundShape = backgroundShape,
                backgroundColor = MaterialTheme.colors.surface,
                horizontalContentPadding = sizes.externalHorizontalPadding,
                anchors = sizes.swipeableAnchors,
                onSwipe = { callback.onSwipe(id) })
            .drawBehind {
                drawRect(selectionBrush, alpha = selectionBackgroundAlpha)
            }.combinedClickable(
                role = Role.Switch,
                onLongClickLabel = stringResource(
                    if (selected) R.string.deselect_item_description
                    else            R.string.select_item_description),
                onLongClick = { callback.onLongClick(id) },
                onClickLabel = if (!selected) null else
                    stringResource(R.string.deselect_item_description),
                onClick = { callback.onClick(id) }
            ).padding(vertical = sizes.internalVerticalPadding),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val spring = defaultSpring<Float>()
            fadeIn(spring) + scaleIn(spring, initialScale = 0.9f) with
            fadeOut(spring) + scaleOut(spring, targetScale = 0.9f)
        }
    ) { showingColorPicker ->
        if (showingColorPicker)
            ListItemColorGroupPicker(Modifier, colorGroup) {
                callback.onColorGroupClick(id, it)
            }
        else ListItemViewRegularContent(
            sizes, id, colorGroup, name, extraInfo, amount, linked,
            // See ListItemViewRegularContent param isEditable for
            // why this is passed as an int instead of a boolean.
            editable = if (expanded) 1 else 0,
            callback, colorIndicator, otherContent,
            colorIndicatorOnClick = { callback.onColorIndicatorClick(id) })
    }
}