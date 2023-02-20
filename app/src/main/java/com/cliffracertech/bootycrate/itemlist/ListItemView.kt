/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.*
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ListItem

fun Modifier.minTouchTargetSize() =
    this.sizeIn(minWidth = 48.dp, minHeight = 48.dp)

/** An interface containing callbacks for ListItem related interactions. */
interface ListItemCallback {
    /** The callback that will be invoked when the item is clicked. */
    fun onClick()
    /** The callback that will be invoked when the item is long clicked. */
    fun onLongClick()
    /** The callback that will be invoked when the item's color
     * group has been requested to be changed to [newColorGroup]. */
    fun onColorGroupChangeRequest(newColorGroup: ListItem.ColorGroup)
    /** The callback that will be invoked when the item's
     * name has been requested to be changed to [newName]*/
    fun onRenameRequest(newName: String)
    /** The callback that will be invoked when the item's extraInfo
     * has been requested to be changed to [newExtraInfo]*/
    fun onExtraInfoChangeRequest(newExtraInfo: String)
    /**  The callback that will be invoked when the item's amount
     * has been requested to be changed to [newAmount]*/
    fun onAmountChangeRequest(newAmount: Int)
    /** The callback that will be invoked when the item's edit button is clicked. */
    fun onEditButtonClick()
    /** Whether or not the edit button should be shown */
    val showEditButton: Boolean
}

/**
 * A visual display of a [ListItem] that also allows user interactions to
 * e.g. change the [ListItem]'s state.
 *
 * @param isSelected Whether or not the item is selected
 * @param selectionBrush The [Brush] that will be shown at half
 *     opacity over the normal background when isSelected is true
 * @param isEditable Whether or not the item will present itself in its editable state
 * @param name The name of the displayed item
 * @param extraInfo The extra info of the displayed item
 * @param amount The amount of the displayed item
 * @param callback The [ListItemCallback] whose method implementations
 *     will be used as the callbacks for user interactions
 * @param modifier The [Modifier] that will be used for the root layout
 * @param colorIndicator A composable lambda whose contents will be used as the
 *     start aligned color indicator for the list item. As the content's on
 *     click should usually open the color picker, a lambda that will show the
 *     [ListItemView]'s color picker when invoked is provided.
 * @param otherContent A composable lambda whose contents will be displayed
 *     beneath the other content. The [Transition]`<Boolean>` that is used when the
 *     view animates after [isEditable] changes is provided in case the added
 *     content needs to synchronize its appearance/disappearance with the rest
 *     of the view.
 */
@Composable fun ListItemView(
    isSelected: Boolean,
    selectionBrush: Brush,
    isEditable: Boolean,
    colorGroupOrdinal: Int,
    name: String,
    extraInfo: String,
    amount: Int,
    callback: ListItemCallback,
    modifier: Modifier = Modifier,
    colorIndicator: @Composable (showColorPicker: () -> Unit) -> Unit,
    otherContent: @Composable ColumnScope.(transition: Transition<Boolean>) -> Unit = {},
) {
    Surface(modifier.animateContentSize(), MaterialTheme.shapes.large) {

        val selectionBackgroundAlpha by
            animateFloatAsState(if (isSelected) 0.5f else 0f)
        Box(Modifier
            .fillMaxSize()
            .alpha(selectionBackgroundAlpha)
            .background(selectionBrush, MaterialTheme.shapes.large))

        var showColorPicker by remember { mutableStateOf(false) }
        val colors = ListItem.ColorGroup.colors()
        val color = remember(colorGroupOrdinal) {
            colors.getOrElse(colorGroupOrdinal) { colors.first() }
        }

        AnimatedContent(
            targetState = showColorPicker,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            transitionSpec = { scaleIn(initialScale = 0.9f) + fadeIn() with
                    scaleOut(targetScale = 0.9f) + fadeOut() }
        ) { showingColorPicker ->
            if (showingColorPicker)
                ListItemColorGroupPicker(Modifier, colorGroupOrdinal) {
                    callback.onColorGroupChangeRequest(it)
                    showColorPicker = false
                }
            else Column {
                val expansionTransition = updateTransition(isEditable, "item expand/collapse")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    colorIndicator { showColorPicker = true }
                    Column(Modifier.weight(1f)) {
                        TextFieldEdit(
                            text = name,
                            onTextChange = callback::onRenameRequest,
                            tint = color,
                            readOnly = !isEditable,
                            textStyle = MaterialTheme.typography.body1)
                        TextFieldEdit(
                            text = extraInfo,
                            onTextChange = callback::onExtraInfoChangeRequest,
                            tint = color,
                            readOnly = !isEditable,
                            textStyle = MaterialTheme.typography.subtitle1)
                    }
                    Box(Modifier.animateContentSize()) {
                        val amountEditEndPadding by
                            expansionTransition.animateDp(label = "amountEditSlideAnim") { isEditable ->
                                if (isEditable || !callback.showEditButton) 0.dp else 48.dp
                            }
                        AmountEdit(
                            amount = amount,
                            isEditableByKeyboard = isEditable,
                            tint = color,
                            onAmountChangeRequest = callback::onAmountChangeRequest,
                            decreaseDescription = stringResource(
                                R.string.item_amount_decrease_description, name),
                            increaseDescription = stringResource(
                                R.string.item_amount_increase_description, name),
                            modifier = Modifier.padding(end = amountEditEndPadding))

                        if (callback.showEditButton) {
                            val editButtonTopPadding by
                            expansionTransition.animateDp(label = "editButtonSlideAnim") { isEditable ->
                                if (isEditable) 48.dp else 0.dp
                            }
                            AnimatedEditToCloseButton(
                                onClick = callback::onEditButtonClick,
                                modifier = Modifier.padding(top = editButtonTopPadding)
                                    .align(Alignment.TopEnd),
                                isEditable = isEditable,
                                itemName = name)
                        }
                    }
                }
                otherContent(expansionTransition)
            }
        }
    }
}