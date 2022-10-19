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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme

/**
 * A circle that can be used to indicate a [color] for an object for the
 * purpose of categorization, while also serving as a button that invokes
 * [onClick] when clicked. [clickLabel] will be used as the accessibility
 * label for the on-click action.
 */
@Composable fun ColorIndicator(
    color: Color,
    clickLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = Box(modifier
    .minTouchTargetSize()
    .padding(10.dp)
    .background(color, CircleShape)
    .clickable(onClickLabel = clickLabel,
               role = Role.Button,
               onClick = onClick))

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
    .padding(12.dp)
    .clickable(true, onClickLabel, Role.Checkbox, onClick)
) {
    val vector = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_to_checked_background)
    Icon(painter = rememberAnimatedVectorPainter(vector, checked),
         contentDescription = null, tint = tint)
    AnimatedCheckmark(checked)
}

/** An interface containing callbacks for InventoryItem related interactions. */
interface InventoryItemCallback : ListItemCallback {
    fun onAutoAddToShoppingListCheckboxClick()
    fun onAutoAddToShoppingListAmountChangeRequest(newAmount: Int)
}

/** Return a [InventoryItemCallback] implementation using the provided lambdas
* as the implementations for the [InventoryItemCallback] methods of the same name. */
fun inventoryItemCallback(
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onColorChangeRequest: (ListItem.Color) -> Unit = {},
    onRenameRequest: (String) -> Unit = {},
    onExtraInfoChangeRequest: (String) -> Unit = {},
    onAmountChangeRequest: (Int) -> Unit = {},
    onEditButtonClick: () -> Unit = {},
    onAutoAddToShoppingListCheckboxClick: () -> Unit = {},
    onAutoAddToShoppingListAmountChangeRequest: (Int) -> Unit = {}
) = object: InventoryItemCallback {
    override fun onClick() = onClick()
    override fun onLongClick() = onLongClick()
    override fun onColorChangeRequest(newColor: ListItem.Color) = onColorChangeRequest(newColor)
    override fun onRenameRequest(newName: String) = onRenameRequest(newName)
    override fun onExtraInfoChangeRequest(newExtraInfo: String) = onExtraInfoChangeRequest(newExtraInfo)
    override fun onAmountChangeRequest(newAmount: Int) = onAmountChangeRequest(newAmount)
    override fun onEditButtonClick() = onEditButtonClick()
    override fun onAutoAddToShoppingListCheckboxClick() = onAutoAddToShoppingListCheckboxClick()
    override fun onAutoAddToShoppingListAmountChangeRequest(newAmount: Int) =
        onAutoAddToShoppingListAmountChangeRequest(newAmount)
}

/**
* A visual display of an [InventoryItem] that also allows user
* interactions to e.g. change the [InventoryItem]'s state.
*
* @param item The [InventoryItem] instance whose data is being displayed
* @param isEditableProvider A lambda that returns Whether or not the view
*     will display itself in its expanded state intended for editing the
*     [InventoryItem]'s state. When [isEditableProvider] returns true, the
*     name, extra info, and amount of the item will expand if necessary to
*     meet minimum touch target sizes, and the auto add to shopping list
*     checkbox and amount edit will be displayed. The [InventoryItem]'s
*     amount's decrease / increase buttons will still invoke their
*     callbacks even when isEditable is false.
*     am callback The InventoryItemCallback whose method implementations
*     will be used as the callbacks for user interactions
* @param modifier The [Modifier] that will be used for the root layout
*/
@Composable fun InventoryItemView (
    item: InventoryItem,
    isEditableProvider: () -> Boolean,
    callback: InventoryItemCallback,
    modifier: Modifier = Modifier
) = ListItemView(
    item, isEditableProvider, callback,
    colorIndicator = { showColorPicker ->
        val colors = ListItem.Color.asComposeColors()
        ColorIndicator(
            color = colors[item.color],
            clickLabel = stringResource(
                R.string.edit_item_color_description, item.name),
            onClick = showColorPicker)
    }, modifier,
) {
    val colors = ListItem.Color.asComposeColors()
    val color = remember(item.color) {
        colors.getOrElse(item.color) { Color.Red }
    }
    val isEditable = isEditableProvider()
    AnimatedVisibility(isEditable) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedCheckbox(
                checked = item.autoAddToShoppingList,
                onClick = callback::onAutoAddToShoppingListCheckboxClick,
                onClickLabel = stringResource(
                    R.string.item_auto_add_to_shopping_list_checkbox_description, item.name),
                tint = color)
            Text(stringResource(R.string.auto_add_to_shopping_list_checkbox_text),
                 style = MaterialTheme.typography.subtitle1)
            AmountEdit(
                amount = item.autoAddToShoppingListAmount,
                isEditableByKeyboard = true,
                tint = color,
                amountDecreaseDescription = stringResource(
                    R.string.item_auto_add_to_shopping_list_amount_decrease_description, item.name),
                amountIncreaseDescription = stringResource(
                    R.string.item_auto_add_to_shopping_list_amount_increase_description, item.name),
                onAmountChangeRequest = callback::onAutoAddToShoppingListAmountChangeRequest)
        }
    }
}

@Preview @Composable
fun InventoryItemViewPreview() = BootyCrateTheme {
    var name by remember { mutableStateOf("Test item") }
    var extraInfo by remember { mutableStateOf("Test extra info") }
    var colorIndex by remember { mutableStateOf(ListItem.Color.Orange.ordinal) }
    var amount by remember { mutableStateOf(5) }
    var isEditable by remember { mutableStateOf(false) }
    var autoAddToShoppingList by remember { mutableStateOf(false) }
    var autoAddToShoppingListAmount by remember { mutableStateOf(1) }
    val item by derivedStateOf {
        InventoryItem(1, name, extraInfo, colorIndex, amount,
            autoAddToShoppingList = autoAddToShoppingList,
            autoAddToShoppingListAmount = autoAddToShoppingListAmount)
    }
    val callback = remember { inventoryItemCallback(
        onColorChangeRequest = { colorIndex = it.ordinal },
        onRenameRequest = { name = it },
        onExtraInfoChangeRequest = { extraInfo = it },
        onAmountChangeRequest = { amount = it },
        onEditButtonClick = { isEditable = !isEditable },
        onAutoAddToShoppingListCheckboxClick = { autoAddToShoppingList = !autoAddToShoppingList },
        onAutoAddToShoppingListAmountChangeRequest = { autoAddToShoppingListAmount = it })
    }
    InventoryItemView(item, { isEditable }, callback)
}


