/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
            viewModel.itemColorIndex,
            viewModel.itemName,
            viewModel.itemExtraInfo,
            viewModel.itemAmount,
            viewModel.itemIsChecked,
            callback = remember {
                shoppingListItemCallback(
                    onColorChangeRequest = { viewModel.itemColorIndex = it.ordinal },
                    onRenameRequest = { viewModel.itemName = it },
                    onExtraInfoChangeRequest = { viewModel.itemExtraInfo = it },
                    onAmountChangeRequest = { viewModel.itemAmount = it },
                    showEditButton = false)
            })
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
            viewModel.itemColorIndex,
            viewModel.itemName,
            viewModel.itemExtraInfo,
            viewModel.itemAmount,
            viewModel.itemAutoAddToShoppingList,
            viewModel.itemAutoAddToShoppingListAmount,
            callback = remember {
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
            })
    }
}

@Composable fun ValidatorMessageList(
    messages: ImmutableList<Validator.Message>,
    modifier: Modifier = Modifier
) {
    val messageHeight = 48.dp
    val context = LocalContext.current
    LazyColumn(modifier.animateContentSize()) {
        items(messages) {
            Row(Modifier.fillMaxWidth().height(messageHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val vector = when {
                    it.isInformational -> Icons.Default.Info
                    it.isWarning ->       Icons.Default.Warning
                    else ->/*it.isError*/ Icons.Default.Error
                }
                val tint = when {
                    it.isInformational -> Color.Blue
                    it.isWarning ->       Color.Yellow
                    else ->/*it.isError*/ MaterialTheme.colors.error
                }
                Icon(vector, null, tint = tint)
                Text(it.stringResource.resolve(context))
            }
        }
    }
}