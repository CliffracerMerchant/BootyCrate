/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.itemlist.InventoryItemView
import com.cliffracertech.bootycrate.itemlist.ShoppingListItemView
import com.cliffracertech.bootycrate.itemlist.inventoryItemCallback
import com.cliffracertech.bootycrate.itemlist.rememberInventoryItemViewSizes
import com.cliffracertech.bootycrate.itemlist.rememberListItemViewSizes
import com.cliffracertech.bootycrate.itemlist.shoppingListItemCallback
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.model.database.Validator
import com.cliffracertech.bootycrate.ui.ValidatorMessageList
import kotlinx.collections.immutable.ImmutableList

/**
 * A dialog to add new [ListItem] subclasses.
 *
 * @param onDismissRequest The callback that will be invoked when
 *     the user requests that the dialog be dismissed
 * @param confirmButtonsEnabled Whether or not the add another
 *     and ok buttons should be enabled
 * @param onAddAnotherClick The callback that will be invoked when
 *     the add another button is clicked
 * @param onOkClick The callback that will be invoked when the ok button is clicked
 * @param messages An [ImmutableList]`<Validator.Message>` of messages to display
 *     regarding the current new item data (e.g. why the current name is invalid
 * @param modifier The [Modifier] to use for the dialog
 * @param newItemView A composable lambda that contains the new item view. The
 *     width that the item view should be is provided as a parameter
 */
@Composable fun NewItemDialog(
    onDismissRequest: () -> Unit,
    confirmButtonsEnabled: Boolean,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
    messages: ImmutableList<Validator.Message>,
    modifier: Modifier = Modifier,
    newItemView: @Composable (width: Dp) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(horizontal = 8.dp),
        text = {
            Column {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.subtitle1,
                    LocalContentAlpha provides ContentAlpha.high
                ) {
                    Text(text = stringResource(R.string.add_button_description),
                         modifier = Modifier.padding(bottom = 12.dp))
                }
                newItemView(screenWidth - 48.dp)
                // itemGroupPicker
                ValidatorMessageList(messages)
            }
        }, buttons = {
            Row(Modifier.padding(horizontal = 16.dp)) {
                // Cancel button
                TextButton(onDismissRequest) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.weight(1f))
                // Add another button
                TextButton(
                    onClick = onAddAnotherClick,
                    enabled = confirmButtonsEnabled
                ) { Text(stringResource(R.string.add_another_item_button_description)) }
                // Ok button
                TextButton(
                    onClick = onOkClick,
                    enabled = confirmButtonsEnabled,
                ) { Text(stringResource(android.R.string.ok)) }
            }
        }, backgroundColor = MaterialTheme.colors.background,
        properties = DialogProperties(usePlatformDefaultWidth = false))
}

/** A [NewItemDialog] that contains a [ShoppingListItemView] to allow the user
 * to create new [ShoppingListItem]s. */
@Composable fun NewShoppingListItemDialog(modifier: Modifier = Modifier) {
    val viewModel: NewShoppingListItemDialogViewModel = viewModel()
    var showColorPicker by remember { mutableStateOf(false) }

    NewItemDialog(
        onDismissRequest = viewModel::onDismissRequest,
        confirmButtonsEnabled = viewModel.confirmButtonsEnabled,
        onAddAnotherClick = viewModel::onAddAnotherClick,
        onOkClick = viewModel::onOkClick,
        messages = viewModel.messages,
        modifier = modifier,
    ) { itemViewWidth ->
        ShoppingListItemView(
            sizes = rememberListItemViewSizes(itemViewWidth),
            id = 0,
            viewModel.itemColorGroup,
            viewModel.itemName,
            viewModel.itemExtraInfo,
            viewModel.itemAmount,
            viewModel.itemIsChecked,
            linked = false,
            selectionBrush = SolidColor(Color.Transparent),
            selected = false,
            expanded = true,
            showColorPicker = showColorPicker,
            callback = remember {
                shoppingListItemCallback(
                    onColorIndicatorClick = { _ -> showColorPicker = true },
                    onColorGroupClick = { _, newColorGroup ->
                        viewModel.itemColorGroup = newColorGroup
                        showColorPicker = false
                    }, onRenameRequest = { _, newName -> viewModel.itemName = newName },
                    onExtraInfoChangeRequest = { _, newExtraInfo -> viewModel.itemExtraInfo = newExtraInfo },
                    onAmountChangeRequest = { _, newAmount -> viewModel.itemAmount = newAmount },
                    showEditButton = false)
            })
    }
}

/** A [NewItemDialog] that contains an [InventoryItemView] to allow the user to
 * create new [InventoryItem]s. */
@Composable fun NewInventoryItemDialog(modifier: Modifier = Modifier) {
    val viewModel: NewInventoryItemDialogViewModel = viewModel()
    var showColorPicker by remember { mutableStateOf(false) }

    NewItemDialog(
        onDismissRequest = viewModel::onDismissRequest,
        confirmButtonsEnabled = viewModel.confirmButtonsEnabled,
        onAddAnotherClick = viewModel::onAddAnotherClick,
        onOkClick = viewModel::onOkClick,
        messages = viewModel.messages,
        modifier = modifier,
    ) { itemViewWidth ->
        InventoryItemView(
            sizes = rememberInventoryItemViewSizes(itemViewWidth),
            id = 0,
            viewModel.itemColorGroup,
            viewModel.itemName,
            viewModel.itemExtraInfo,
            viewModel.itemAmount,
            linked = false,
            viewModel.itemAutoAddToShoppingList,
            viewModel.itemAutoAddToShoppingListAmount,
            selectionBrush = SolidColor(Color.Transparent),
            selected = false,
            expanded = true,
            showColorPicker = showColorPicker,
            callback = remember {
                inventoryItemCallback(
                    onColorIndicatorClick = { _ -> showColorPicker = true },
                    onColorGroupClick = { _, newColorGroup ->
                        viewModel.itemColorGroup = newColorGroup
                        showColorPicker = false
                    }, onRenameRequest = { _, newName -> viewModel.itemName = newName },
                    onExtraInfoChangeRequest = { _, newExtraInfo -> viewModel.itemExtraInfo = newExtraInfo },
                    onAmountChangeRequest = { _, newAmount -> viewModel.itemAmount = newAmount },
                    showEditButton = false,
                    onAutoAddToShoppingListCheckboxClick = {
                        viewModel.itemAutoAddToShoppingList = !viewModel.itemAutoAddToShoppingList
                    }, onAutoAddToShoppingListAmountChangeRequest = { _, newAmount ->
                        viewModel.itemAutoAddToShoppingListAmount = newAmount
                    })
            })
    }
}