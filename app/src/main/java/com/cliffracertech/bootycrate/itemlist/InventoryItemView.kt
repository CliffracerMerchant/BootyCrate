/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme

/** A circle that can be used to indicate a [color] for an object for the
 * purpose of categorization, while also serving as a button that invokes
 * [onClick] when clicked. [clickLabel] will be used as the accessibility
 * label for the on-click action. */
@Composable fun ColorIndicator(
    color: Color,
    clickLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = Box(modifier
    .minTouchTargetSize()
    .clickable(onClickLabel = clickLabel,
               role = Role.Button,
               onClick = onClick)
    .padding(13.dp)
    .background(color, CircleShape))

/** A [tint]ed checkbox that animates when its [checked] state changes, invokes
 * [onClick] when clicked and uses [onClickLabel] to describe its click action. */
@Composable fun AnimatedCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
    onClickLabel: String,
    tint: Color,
    modifier: Modifier = Modifier,
) = Box(modifier
    .minTouchTargetSize()
    .clip(MaterialTheme.shapes.small)
    .clickable(true, onClickLabel, Role.Checkbox, onClick)
    .padding(12.dp)
) {
    val vector = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_to_checked_background)
    Icon(painter = rememberAnimatedVectorPainter(vector, checked),
         contentDescription = null, tint = tint)
    AnimatedCheckmark(checked)
}

/** An interface containing callbacks for InventoryItem related interactions. */
interface InventoryItemCallback : ListItemCallback {
    /** The callback that will be invoked when the item view's auto add to
     * shopping list checkbox is clicked */
    fun onAutoAddToShoppingListCheckboxClick(id: Long)
    /** The callback that will be invoked when the item view's auto add to
     * shopping list amount has been requested to change to the provider amount*/
    fun onAutoAddToShoppingListAmountChangeRequest(id: Long, newAmount: Int)
}

/** Return a [InventoryItemCallback] implementation using the provided lambdas
 * as the implementations for the [InventoryItemCallback] methods of the same name */
fun inventoryItemCallback(
    onClick: (Long) -> Unit = {},
    onLongClick: (Long) -> Unit = {},
    onSwipe: (Long) -> Unit = {},
    onColorGroupClick: (Long, ListItem.ColorGroup) -> Unit = { _, _ -> },
    onRenameRequest: (Long, String) -> Unit = { _, _ -> },
    onExtraInfoChangeRequest: (Long, String) -> Unit = { _, _ -> },
    onAmountChangeRequest: (Long, Int) -> Unit = { _, _ -> },
    onEditButtonClick: (Long) -> Unit = {},
    showEditButton: Boolean = true,
    onAutoAddToShoppingListCheckboxClick: (Long) -> Unit = {},
    onAutoAddToShoppingListAmountChangeRequest: (Long, Int) -> Unit = { _, _ -> }
) = object: InventoryItemCallback {
    override fun onClick(id: Long) = onClick(id)
    override fun onLongClick(id: Long) = onLongClick(id)
    override fun onSwipe(id: Long) = onSwipe(id)
    override fun onColorGroupClick(id: Long, colorGroup: ListItem.ColorGroup) =
        onColorGroupClick(id, colorGroup)
    override fun onRenameRequest(id: Long, newName: String) =
        onRenameRequest(id, newName)
    override fun onExtraInfoChangeRequest(id: Long, newExtraInfo: String) =
        onExtraInfoChangeRequest(id, newExtraInfo)
    override fun onAmountChangeRequest(id: Long, newAmount: Int) =
        onAmountChangeRequest(id, newAmount)
    override fun onEditButtonClick(id: Long) = onEditButtonClick(id)
    override val showEditButton = showEditButton
    override fun onAutoAddToShoppingListCheckboxClick(id: Long) =
        onAutoAddToShoppingListCheckboxClick(id)
    override fun onAutoAddToShoppingListAmountChangeRequest(id: Long, newAmount: Int) =
        onAutoAddToShoppingListAmountChangeRequest(id, newAmount)
}

/**
 * A visual display of an [InventoryItem] that also allows user
 * interactions to e.g. change the [InventoryItem]'s state.
 *
 * @param id The [ListItem.id] for the item
 * @param colorGroup The [ListItem.ColorGroup] that the item belongs to
 * @param name The name of the displayed item
 * @param extraInfo The extra info of the displayed item
 * @param amount The amount of the displayed item
 * @param isLinked Whether or not the item is linked to another item
 * @param autoAddToShoppingList Whether or not auto add to shopping
 *     list is enabled for the item
 * @param autoAddToShoppingListAmount The auto add to shopping list
 *     threshold amount for the item
 * @param isEditable Whether or not the item will present itself in its editable state
 * @param isSelected Whether or not the item is selected
 * @param selectionBrush The [Brush] that will be shown at half
 *     opacity over the normal background when isSelected is true
 * @param callback The [InventoryItemCallback] whose method implementations
 *     will be used as the callbacks for user interactions
 * @param modifier The [Modifier] that will be used for the root layout
 */
@Composable fun InventoryItemView(
    id: Long,
    colorGroup: ListItem.ColorGroup,
    name: String,
    extraInfo: String,
    amount: Int,
    isLinked: Boolean,
    autoAddToShoppingList: Boolean,
    autoAddToShoppingListAmount: Int,
    isEditable: Boolean,
    isSelected: Boolean,
    selectionBrush: Brush,
    callback: InventoryItemCallback,
    modifier: Modifier = Modifier
) {
    val colors = ListItem.ColorGroup.colors()
    val color = remember(colorGroup) {
        colors.getOrElse(colorGroup.ordinal) { colors.first() }
    }
    ListItemView(
        id, colorGroup, name, extraInfo,
        amount, isLinked, isEditable,
        isSelected, selectionBrush,
        callback, modifier,
        colorIndicator = { showColorPicker ->
            ColorIndicator(
                color = color,
                clickLabel = stringResource(
                    R.string.edit_item_color_description, name),
                onClick = showColorPicker)
        }
    ) {

        AnimatedVisibility(isEditable) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedCheckbox(
                    checked = autoAddToShoppingList,
                    onClick = { callback.onAutoAddToShoppingListCheckboxClick(id) },
                    onClickLabel = stringResource(
                        R.string.item_auto_add_to_shopping_list_checkbox_description, name),
                    tint = color)
                Text(stringResource(R.string.auto_add_to_shopping_list_checkbox_text),
                    style = MaterialTheme.typography.subtitle1)
                AmountEdit(
                    amount = autoAddToShoppingListAmount,
                    isEditableByKeyboard = true,
                    tint = color,
                    decreaseDescription = stringResource(
                        R.string.item_auto_add_to_shopping_list_amount_decrease_description, name),
                    increaseDescription = stringResource(
                        R.string.item_auto_add_to_shopping_list_amount_increase_description, name),
                    onAmountChangeRequest = {
                        callback.onAutoAddToShoppingListAmountChangeRequest(id, it)
                    })
            }
        }
    }
}

/**
 * A visual display of an [InventoryItem] that also allows user
 * interactions to e.g. change the [InventoryItem]'s state.
 *
 * @param item The [InventoryItem] instance whose data is being displayed
 * @param isSelected Whether or not the item is selected
 * @param selectionBrush The [Brush] that will be shown at half
 *     opacity over the normal background when isSelected is true
 * @param isEditable Whether or not the item will present itself in its editable state
 * @param callback The [InventoryItemCallback] whose method implementations
 *     will be used as the callbacks for user interactions
 * @param modifier The [Modifier] that will be used for the root layout
 */
@Composable fun InventoryItemView(
    item: InventoryItem,
    isSelected: Boolean,
    selectionBrush: Brush,
    isEditable: Boolean,
    callback: InventoryItemCallback,
    modifier: Modifier = Modifier
) = InventoryItemView(
    item.id, item.colorGroup,
    item.name, item.extraInfo,
    item.amount, item.isLinked,
    item.autoAddToShoppingList,
    item.autoAddToShoppingListAmount,
    isEditable, isSelected, selectionBrush,
    callback, modifier)

@Preview @Composable
fun InventoryItemViewPreview() = BootyCrateTheme {
    var isSelected by remember { mutableStateOf(false) }
    val color1 = MaterialTheme.colors.primary
    val color2 = MaterialTheme.colors.secondary
    val brush = remember { Brush.horizontalGradient(listOf(color1, color2)) }
    var isEditable by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Test item") }
    var extraInfo by remember { mutableStateOf("Test extra info") }
    var colorGroup by remember { mutableStateOf(ListItem.ColorGroup.Orange) }
    var amount by remember { mutableStateOf(5) }
    var autoAddToShoppingList by remember { mutableStateOf(false) }
    var autoAddToShoppingListAmount by remember { mutableStateOf(1) }
    val callback = remember { inventoryItemCallback(
        onClick = { isSelected = !isSelected },
        onLongClick = { isSelected = !isSelected },
        onColorGroupClick = { _, newColorGroup -> colorGroup = newColorGroup },
        onRenameRequest = { _, newName -> name = newName },
        onExtraInfoChangeRequest = { _, newExtraInfo -> extraInfo = newExtraInfo },
        onAmountChangeRequest = { _, newAmount -> amount = newAmount },
        onEditButtonClick = { isEditable = !isEditable },
        onAutoAddToShoppingListCheckboxClick = {
            autoAddToShoppingList = !autoAddToShoppingList
        }, onAutoAddToShoppingListAmountChangeRequest = { _, newAmount ->
            autoAddToShoppingListAmount = newAmount
        })
    }
    InventoryItemView(
        id = 0, colorGroup, name, extraInfo,
        amount, isLinked = true,
        autoAddToShoppingList, autoAddToShoppingListAmount,
        isEditable, isSelected, brush, callback)
}


