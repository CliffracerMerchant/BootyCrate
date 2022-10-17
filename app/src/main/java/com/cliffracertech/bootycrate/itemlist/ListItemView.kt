/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.*
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    /** The callback that will be invoked when the item's
     * color has been requested to be changed to [newColor]. */
    fun onColorChangeRequest(newColor: ListItem.Color)
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
}

/**
* A visual display of a [ListItem] that also allows user interactions to
* e.g. change the [ListItem]'s state.
*
* @param item The [ListItem] instance whose properties are being displayed
* @param isEditable Whether or not the view will display itself in its expanded
*     state intended for editing the [ListItem]'s state. When [isEditable] is
*     true, the name, extra info, and amount of the item will expand if
*     necessary to meet minimum touch target sizes. The [ListItem]'s amount's
*     decrease / increase buttons will still invoke their callbacks even when
*     isEditable is false.
* @param callback The ListItemCallback whose method implementations
*     will be used as the callbacks for user interactions
* @param colorIndicator A composable lambda whose contents will be used as the
*     start aligned color indicator for the list item. As the content's on
*     click should usually open the color picker, a lambda that will show the
*     [ListItemView]'s color picker when invoked is provided.
* @param modifier The Modifier that will be used for the root layout
* @param otherContent A composable lambda whose contents will be displayed
*     beneath the other content. The Transition<Boolean> that is used when the
*     view animates after [isEditable] changes is provided in case the added
*     content needs to synchronize its appearance/disappearance with the rest
*     of the view.
*/
@Composable fun ListItemView(
    item: ListItem,
    isEditable: Boolean,
    callback: ListItemCallback,
    colorIndicator: @Composable (showColorPicker: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    otherContent: @Composable ColumnScope.(transition: Transition<Boolean>) -> Unit,
) = Surface(modifier.animateContentSize(), MaterialTheme.shapes.large) {
    val colors = ListItem.Color.asComposeColors()
    val color = remember(item.color) {
        colors.getOrElse(item.color) { Color.Red }
    }
    var showColorPicker by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showColorPicker,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
        transitionSpec = { scaleIn(initialScale = 0.9f) + fadeIn() with
                           scaleOut(targetScale = 0.9f) + fadeOut() }
    ) { showingColorPicker ->
        if (showingColorPicker) ColorPicker(
            currentColor = color,
            colors = colors,
            colorDescriptions = ListItem.Color.descriptions(),
            onColorClick = { index, _ ->
                val listItemColor = ListItem.Color.values().getOrElse(index) { ListItem.Color.Red }
                callback.onColorChangeRequest(listItemColor)
                showColorPicker = false
            })
        else Column {
            val expansionTransition = updateTransition(isEditable, "item expand/collapse")

            Row(verticalAlignment = Alignment.CenterVertically) {
                colorIndicator { showColorPicker = true }
                Column(Modifier.weight(1f)) {
                    TextFieldEdit(
                        text = item.name,
                        onTextChange = callback::onRenameRequest,
                        tint = color,
                        readOnly = !isEditable,
                        textStyle = MaterialTheme.typography.body1)
                    TextFieldEdit(
                        text = item.extraInfo,
                        onTextChange = callback::onExtraInfoChangeRequest,
                        tint = color,
                        readOnly = !isEditable,
                        textStyle = MaterialTheme.typography.subtitle1)
                }
                Box(Modifier.animateContentSize()) {
                    val amountEditEndPadding by
                    expansionTransition.animateDp(label = "amountEditSlideAnim") { isEditable ->
                        if (isEditable) 0.dp else 48.dp
                    }
                    val editButtonTopPadding by
                    expansionTransition.animateDp(label = "editButtonSlideAnim") { isEditable ->
                        if (isEditable) 48.dp else 0.dp
                    }
                    AmountEdit(
                        amount = item.amount,
                        isEditableByKeyboard = isEditable,
                        tint = color,
                        onAmountChangeRequest = callback::onAmountChangeRequest,
                        amountDecreaseDescription = stringResource(
                            R.string.item_amount_decrease_description, item.name),
                        amountIncreaseDescription = stringResource(
                            R.string.item_amount_increase_description, item.name),
                        modifier = Modifier.padding(end = amountEditEndPadding))
                    IconButton(
                        onClick = callback::onEditButtonClick,
                        modifier = Modifier.padding(top = editButtonTopPadding)
                                           .align(Alignment.TopEnd)
                    ) {
                        val vector = AnimatedImageVector.animatedVectorResource(
                                R.drawable.animated_edit_to_collapse)
                        val painter = rememberAnimatedVectorPainter(vector, isEditable)
                        val desc = stringResource(
                            if (isEditable) R.string.collapse_item_description
                            else            R.string.edit_item_description, item.name)
                        Icon(painter, desc)
                    }
                }
            }
            otherContent(expansionTransition)
        }
    }
}