/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.springStiffness

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
    /** The callback that will be invoked when the given [colorGroup]
     * has been clicked in the item's color group selector */
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

@Composable
private fun ListItemViewTextFields(
    sizes: ListItemViewSizes,
    isEditable: Boolean,
    editableTransitionProgressGetter: () -> Float,
    isFullyCollapsed: Boolean,
    id: Long,
    name: String,
    extraInfo: String,
    tint: Color,
    callback: ListItemCallback,
) {
    // We want the text fields to stay at their expanded editable widths until
    // the view is fully collapsed to prevent the underlines from noticeably
    // changing in width at the beginning of the collapse animation.
    Box(Modifier
        .width(sizes.textFieldWidth(!isFullyCollapsed))
        .offset(x = sizes.colorIndicatorSize)
    ) {
        TextFieldEdit(
            text = name,
            onTextChange = { callback.onRenameRequest(id, it) },
            modifier = Modifier
                .height(sizes.nameHeight(isEditable))
                .graphicsLayer {
                    val interp = editableTransitionProgressGetter()
                    translationY = sizes.nameOffsetY(
                        isEditable, extraInfo.isBlank(), interp).toPx()
                },
            tint = tint,
            isEditable = isEditable,
            editableTransitionProgressGetter = editableTransitionProgressGetter,
            textStyle = sizes.nameTextStyle)
        if (!isFullyCollapsed || extraInfo.isNotBlank())
            TextFieldEdit(
                text = extraInfo,
                onTextChange = { callback.onExtraInfoChangeRequest(id, it) },
                modifier = Modifier
                    .height(sizes.extraInfoHeight(isEditable))
                    .graphicsLayer {
                        translationY = sizes.extraInfoOffsetY(
                            isEditable, editableTransitionProgressGetter()).toPx()
                    },
                tint = tint,
                isEditable = !isEditable,
                editableTransitionProgressGetter = editableTransitionProgressGetter,
                textStyle = sizes.extraInfoTextStyle)
    }
}

@Composable
private fun BoxScope.ListItemViewAmountEdit(
    sizes: ListItemViewSizes,
    valueIsFocusable: Boolean,
    valueIsFocusableTransitionProgressGetter: () -> Float,
    id: Long,
    name: String,
    amount: Int,
    tint: Color,
    callback: ListItemCallback,
) = AmountEdit(
    sizes = sizes.amountEditSizes,
    amount = amount,
    valueIsFocusable = valueIsFocusable,
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
            val interp = valueIsFocusableTransitionProgressGetter()
            translationX = sizes.amountEditOffsetX(valueIsFocusable, interp).toPx()
        },
    valueIsFocusableTransitionProgressGetter = valueIsFocusableTransitionProgressGetter)

@Composable
private fun BoxScope.ListItemViewEditCollapseButton(
    sizes: ListItemViewSizes,
    isEditable: Boolean,
    isEditableTransitionProgressGetter: () -> Float,
    id: Long,
    name: String,
    callback: ListItemCallback,
) {
    if (!callback.showEditButton)
        return
    AnimatedEditToCloseButton(
        onClick = { callback.onEditButtonClick(id) },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .requiredSize(48.dp)
            .graphicsLayer {
                val interp = isEditableTransitionProgressGetter()
                translationY = sizes.editButtonOffsetY(interp).toPx()
            },
        isEditable = isEditable,
        itemName = name)
}

@Composable
private fun BoxScope.ListItemViewLinkedIndicator(show: Boolean) {
    val linkedIndicatorAppearanceProgress by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = spring(stiffness = springStiffness))
    if (linkedIndicatorAppearanceProgress == 0f) return
    Icon(imageVector = Icons.Default.Link,
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .requiredSize(48.dp)
            .padding(12.dp)
            .offset(x = (-48).dp, y = 48.dp)
            .graphicsLayer {
                alpha = linkedIndicatorAppearanceProgress
                translationY = -24.dp.toPx() * (1f - linkedIndicatorAppearanceProgress)
            })
}

/** The inner content for a ListItemView when it is not showing its color picker. */
@Composable private fun ListItemViewRegularContent(
    sizes: ListItemViewSizes,
    id: Long,
    colorGroup: ListItem.ColorGroup,
    name: String,
    extraInfo: String,
    amount: Int,
    isLinked: Boolean,
    isEditable: Boolean,
    callback: ListItemCallback,
    showColorPicker: () -> Unit,
    colorIndicator: @Composable (
        isCollapsed: Boolean,
        showColorPicker: () -> Unit,
        modifier: Modifier,
    ) -> Unit,
    otherContent: @Composable (otherContentModifier: Modifier) -> Unit = {},
) = Box(Modifier.heightIn(min = 48.dp)) {
    val expansionProgress by animateFloatAsState(
        targetValue = if (isEditable) 1f else 0f,
        animationSpec = spring(stiffness = springStiffness))
    val isCollapsed by remember { derivedStateOf { expansionProgress == 0f }}

    val colors = ListItem.ColorGroup.colors()
    val tint = remember(colorGroup) {
        colors.getOrElse(colorGroup.ordinal) { colors.first() }
    }

    colorIndicator(
        modifier = Modifier.graphicsLayer {
            translationY = sizes.colorIndicatorTopOffset(expansionProgress).toPx()
        }, isCollapsed = isCollapsed,
        showColorPicker = showColorPicker)
    ListItemViewTextFields(
        sizes, isEditable, { expansionProgress },
        isCollapsed, id, name, extraInfo, tint, callback)
    ListItemViewAmountEdit(
        sizes, isEditable, { expansionProgress },
        id, name, amount, tint, callback)
    ListItemViewEditCollapseButton(
        sizes, isEditable,
        { expansionProgress },
        id, name, callback)
    ListItemViewLinkedIndicator(
        show = isEditable && isLinked)
    if (!isCollapsed)
        otherContent(Modifier.graphicsLayer {
            translationY = sizes.otherContentTopOffset(expansionProgress).toPx()
            alpha = expansionProgress
        })
}

/**
 * A visual display of a [ListItem] that also allows user interactions to
 * e.g. change the [ListItem]'s state.
 *
 * @param colorGroup The [ListItem.ColorGroup] that the item belongs to
 * @param name The name of the displayed item
 * @param extraInfo The extra info of the displayed item
 * @param amount The amount of the displayed item
 * @param isSelected Whether or not the item is selected
 * @param selectionBrush The [Brush] that will be shown at half
 *     opacity over the normal background when isSelected is true
 * @param isEditable Whether or not the item will present itself in its editable state
 * @param callback The [ListItemCallback] whose method implementations
 *     will be used as the callbacks for user interactions
 * @param modifier The [Modifier] that will be used for the root layout
 * @param colorIndicator A composable lambda whose contents will be used as the
 *     start aligned color indicator for the list item. Whether or not the view
 *     is fully collapsed (i.e. the transition that occurs when isEditable is
 *     changed to false is complete) will be provided, along with a lambda that
 *     will show the [ListItemView]'s color picker when invoked. This lambda
 *     should usually be used as the content's onClick.
 * @param otherContent A composable lambda whose contents will be displayed
 *     beneath the other content. The [Transition]`<Boolean>` that is used when the
 *     view animates after [isEditable] changes is provided in case the added
 *     content needs to synchronize its appearance/disappearance with the rest
 *     of the view.
 */
@Composable fun ListItemView(
    sizes: ListItemViewSizes,
    id: Long,
    colorGroup: ListItem.ColorGroup,
    name: String,
    extraInfo: String,
    amount: Int,
    isLinked: Boolean,
    isEditable: Boolean,
    isSelected: Boolean,
    selectionBrush: Brush,
    callback: ListItemCallback,
    modifier: Modifier = Modifier,
    colorIndicator: @Composable (
            isCollapsed: Boolean,
            showColorPicker: () -> Unit,
            modifier: Modifier,
        ) -> Unit,
    otherContent: @Composable (otherContentModifier: Modifier) -> Unit = {},
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val selectionBackgroundAlpha by animateFloatAsState(
        targetValue = if (isSelected && !showColorPicker) 0.5f else 0f,
        animationSpec = tween(durationMillis = 300))
    val height by animateDpAsState(
        targetValue = sizes.height(
            showingColorPicker = showColorPicker,
            isEditable = isEditable),
        animationSpec = spring(stiffness = springStiffness))

    AnimatedContent(
        targetState = showColorPicker,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colors.surface)
            .drawBehind {
                drawRect(selectionBrush, alpha = selectionBackgroundAlpha)
            }.combinedClickable(
                role = Role.Switch,
                onLongClickLabel = stringResource(
                    if (isSelected) R.string.deselect_item_description
                    else            R.string.select_item_description),
                onLongClick = { callback.onLongClick(id) },
                onClickLabel = if (!isSelected) null else
                    stringResource(R.string.deselect_item_description),
                onClick = { callback.onClick(id) }
            ).padding(vertical = sizes.verticalPadding),
        transitionSpec = {
            val springSpec = spring<Float>(stiffness = springStiffness)
            fadeIn(springSpec) + scaleIn(springSpec, initialScale = 0.9f) with
            fadeOut(springSpec) + scaleOut(springSpec, targetScale = 0.9f)
        }
    ) { showingColorPicker ->
        if (showingColorPicker)
            ListItemColorGroupPicker(Modifier, colorGroup) {
                callback.onColorGroupClick(id, it)
                showColorPicker = false
            }
        else ListItemViewRegularContent(
            sizes, id, colorGroup, name, extraInfo,
            amount, isLinked, isEditable, callback,
            showColorPicker = { showColorPicker = true },
            colorIndicator, otherContent)
    }
}