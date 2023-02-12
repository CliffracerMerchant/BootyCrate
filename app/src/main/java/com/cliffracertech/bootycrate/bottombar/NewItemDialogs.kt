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
import androidx.compose.runtime.derivedStateOf
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
 *
 */
@Composable fun NewItemDialog(
    onDismissRequest: () -> Unit,
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
            TextButton(onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onAddAnotherClick) {
                Text(stringResource(R.string.add_another_item_button_description))
            }
            TextButton(onOkClick) {
                Text(stringResource(android.R.string.ok))
            }
        }},
        //properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable fun NewShoppingListItemDialog(
    onDismissRequest: () -> Unit,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) = NewItemDialog(onDismissRequest, onAddAnotherClick, onOkClick, modifier) {

    val viewModel: NewShoppingListItemDialogViewModel = viewModel()
    var color by rememberSaveable { mutableStateOf(ListItem.Color.values().first())}
    var amount by rememberSaveable { mutableStateOf(1) }

    val newItem by remember { derivedStateOf {
        ShoppingListItem(
            name = viewModel.itemName,
            extraInfo = viewModel.itemExtraInfo,
            color = color.ordinal,
            amount = amount)
    }}

    val callback = remember {
        shoppingListItemCallback(
            onColorChangeRequest = { color = it },
            onRenameRequest = { viewModel.itemName = it },
            onExtraInfoChangeRequest = { viewModel.itemExtraInfo = it },
            onAmountChangeRequest = { amount = it },
            showEditButton = false)
    }

    ShoppingListItemView(newItem, callback)
}

@Composable fun NewInventoryItemDialog(
    onDismissRequest: () -> Unit,
    onAddAnotherClick: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) = NewItemDialog(onDismissRequest, onAddAnotherClick, onOkClick, modifier) {

    val viewModel: NewInventoryItemDialogViewModel = viewModel()
    var color by rememberSaveable { mutableStateOf(ListItem.Color.values().first())}
    var amount by rememberSaveable { mutableStateOf(1) }
    var autoAddToShoppingList by rememberSaveable { mutableStateOf(false) }
    var autoAddToShoppingListAmount by rememberSaveable { mutableStateOf(1) }

    val newItem by remember { derivedStateOf {
        InventoryItem(
            name = viewModel.itemName,
            extraInfo = viewModel.itemExtraInfo,
            color = color.ordinal,
            amount = amount,
            autoAddToShoppingList = autoAddToShoppingList,
            autoAddToShoppingListAmount = autoAddToShoppingListAmount)
    }}

    val callback = remember {
        inventoryItemCallback(
            onColorChangeRequest = { color = it },
            onRenameRequest = { viewModel.itemName = it },
            onExtraInfoChangeRequest = { viewModel.itemExtraInfo = it },
            onAmountChangeRequest = { amount = it },
            showEditButton = false,
            onAutoAddToShoppingListCheckboxClick = { autoAddToShoppingList = !autoAddToShoppingList },
            onAutoAddToShoppingListAmountChangeRequest = { autoAddToShoppingListAmount = it })
    }

    InventoryItemView(newItem, callback)
}