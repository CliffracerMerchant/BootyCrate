/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.itemlist.InventoryItemView
import com.cliffracertech.bootycrate.itemlist.ShoppingListItemView
import com.cliffracertech.bootycrate.itemlist.inventoryItemCallback
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
 * @param newItemView A composable lambda that contains the new item view
 */
@Composable fun NewItemDialog(
    onDismissRequest: () -> Unit,
    confirmButtonsEnabled: Boolean,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
    messages: ImmutableList<Validator.Message>,
    modifier: Modifier = Modifier,
    newItemView: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Row {
            stringResource(R.string.add_button_description)
        }},
        text = { Column {
            newItemView()
            // itemGroupPicker
            ValidatorMessageList(messages)
        }},
        buttons = { Row {
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
        }},
        //properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

/** A [NewItemDialog] that contains a [ShoppingListItemView] to allow the user
 * to create new [ShoppingListItem]s. See [NewItemDialog] for parameter descriptions. */
@Composable fun NewShoppingListItemDialog(
    onDismissRequest: () -> Unit,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
    messages: ImmutableList<Validator.Message>,
    modifier: Modifier = Modifier,
) {
    val viewModel: NewShoppingListItemDialogViewModel = viewModel()

    NewItemDialog(
        onDismissRequest, viewModel.confirmButtonsEnabled,
        onAddAnotherClick, onOkClick, messages, modifier
    ) {
        ShoppingListItemView(
            id = 0,
            viewModel.itemColorGroup,
            viewModel.itemName,
            viewModel.itemExtraInfo,
            viewModel.itemAmount,
            viewModel.itemIsChecked,
            isLinked = false,
            isEditable = true,
            isSelected = false,
            selectionBrush = Brush.horizontalGradient(listOf(Color.Transparent)),
            callback = remember {
                shoppingListItemCallback(
                    onColorGroupClick = { _, newColorGroup -> viewModel.itemColorGroup = newColorGroup },
                    onRenameRequest = { _, newName -> viewModel.itemName = newName },
                    onExtraInfoChangeRequest = { _, newExtraInfo -> viewModel.itemExtraInfo = newExtraInfo },
                    onAmountChangeRequest = { _, newAmount -> viewModel.itemAmount = newAmount },
                    showEditButton = false)
            }
        )
    }
}

/** A [NewItemDialog] that contains an [InventoryItemView] to allow the user to
 * create new [InventoryItem]s. See [NewItemDialog] for parameter descriptions. */
@Composable fun NewInventoryItemDialog(
    onDismissRequest: () -> Unit,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
    messages: ImmutableList<Validator.Message>,
    modifier: Modifier = Modifier,
) {
    val viewModel: NewInventoryItemDialogViewModel = viewModel()

    NewItemDialog(
        onDismissRequest, viewModel.confirmButtonsEnabled,
        onAddAnotherClick, onOkClick, messages, modifier
    ) {
        InventoryItemView(
            id = 0,
            viewModel.itemColorGroup,
            viewModel.itemName,
            viewModel.itemExtraInfo,
            viewModel.itemAmount,
            isLinked = false,
            viewModel.itemAutoAddToShoppingList,
            viewModel.itemAutoAddToShoppingListAmount,
            isEditable = true,
            isSelected = false,
            selectionBrush = Brush.horizontalGradient(listOf(Color.Transparent)),
            callback = remember {
                inventoryItemCallback(
                    onColorGroupClick = { _, newColorGroup -> viewModel.itemColorGroup = newColorGroup },
                    onRenameRequest = { _, newName -> viewModel.itemName = newName },
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