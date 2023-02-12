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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
 * @param modifier The [Modifier] to use for the dialog
 * @param newItemView A composable lambda that contains the new item view
 */
@Composable fun NewItemDialog(
    onDismissRequest: () -> Unit,
    confirmButtonsEnabled: Boolean,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
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
    confirmButtonsEnabled: Boolean,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) = NewItemDialog(
    onDismissRequest, confirmButtonsEnabled,
    onAddAnotherClick, onOkClick, modifier
) {
    val viewModel: NewShoppingListItemDialogViewModel = viewModel()
    var color by rememberSaveable { mutableStateOf(ListItem.Color.values().first())}
    var amount by rememberSaveable { mutableStateOf(1) }

    val callback = remember {
        shoppingListItemCallback(
            onColorChangeRequest = { color = it },
            onRenameRequest = { viewModel.itemName = it },
            onExtraInfoChangeRequest = { viewModel.itemExtraInfo = it },
            onAmountChangeRequest = { amount = it },
            showEditButton = false)
    }

    ShoppingListItemView(
        viewModel.itemColorIndex,
        viewModel.itemName,
        viewModel.itemExtraInfo,
        viewModel.itemAmount,
        viewModel.itemIsChecked,
        callback)
}

/** A [NewItemDialog] that contains an [InventoryItemView] to allow the user to
 * create new [InventoryItem]s. See [NewItemDialog] for parameter descriptions. */
@Composable fun NewInventoryItemDialog(
    onDismissRequest: () -> Unit,
    confirmButtonsEnabled: Boolean,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) = NewItemDialog(
    onDismissRequest, confirmButtonsEnabled,
    onAddAnotherClick, onOkClick, modifier
) {
    val viewModel: NewInventoryItemDialogViewModel = viewModel()

    val callback = remember {
        inventoryItemCallback(
            onColorChangeRequest = { viewModel.itemColorIndex = it.ordinal },
            onRenameRequest = { viewModel.itemName = it },
            onExtraInfoChangeRequest = { viewModel.itemExtraInfo = it },
            onAmountChangeRequest = { viewModel.itemAmount = it },
            showEditButton = false,
            onAutoAddToShoppingListCheckboxClick = {
                viewModel.itemAutoAddToShoppingList = !viewModel.itemAutoAddToShoppingList
            }, onAutoAddToShoppingListAmountChangeRequest = {
                viewModel.itemAutoAddToShoppingListAmount = it
            })
    }

    InventoryItemView(
        viewModel.itemColorIndex,
        viewModel.itemName,
        viewModel.itemExtraInfo,
        viewModel.itemAmount,
        viewModel.itemAutoAddToShoppingList,
        viewModel.itemAutoAddToShoppingListAmount,
        callback)
}