/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.model.database.ListItem

@Composable fun <T: ListItem> ItemListScreen(
    modifier: Modifier = Modifier,
    uiState: ItemListViewModel.UiState,
    listView: @Composable (ItemListViewModel.UiState.Items<T>) -> Unit
) = Box(modifier.fillMaxSize(), Alignment.Center) {
    val context = LocalContext.current

    Crossfade(targetState = uiState) { uiState ->
        when (uiState) {
            is ItemListViewModel.UiState.Loading ->
                CircularProgressIndicator(Modifier.size(50.dp))
            is ItemListViewModel.UiState.Message ->
                Text(uiState.text.resolve(context))
            is ItemListViewModel.UiState.Items<*> ->
                listView(uiState as ItemListViewModel.UiState.Items<T>)
        }
    }
}

@Composable fun ShoppingListScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val vm: ShoppingListViewModel = viewModel()

    val startColor = MaterialTheme.colors.primary
    val endColor = MaterialTheme.colors.secondary
    val selectionBrush = remember(startColor, endColor) {
        Brush.horizontalGradient(listOf(startColor, endColor))
    }

    val listViewCallback = remember {
        object: ShoppingListCallback {
            override fun onItemClick(id: Long) = vm.onItemClick(id)
            override fun onItemLongClick(id: Long) = vm.onItemLongClick(id)
            override fun onItemSwipe(id: Long) = vm.onItemSwipe(id)
            override fun onItemColorGroupClick(id: Long, colorGroup: ListItem.ColorGroup) =
                vm.onItemColorGroupChangeRequest(id, colorGroup)
            override fun onItemRenameRequest(id: Long, newName: String) =
                vm.onItemRenameRequest(id, newName)
            override fun onItemExtraInfoChangeRequest(id: Long, newExtraInfo: String) =
                vm.onItemExtraInfoChangeRequest(id, newExtraInfo)
            override fun onItemAmountChangeRequest(id: Long, newAmount: Int) =
                vm.onItemAmountChangeRequest(id, newAmount)
            override fun onItemEditButtonClick(id: Long) = vm.onItemEditButtonClick(id)
            override fun onItemCheckboxClick(id: Long) = vm.onItemCheckboxClick(id)
        }
    }
    ItemListScreen(
        modifier = modifier,
        uiState = vm.uiState,
    ) { itemListState ->
        ShoppingListView(
            shoppingListState = itemListState,
            callback = listViewCallback,
            selectionBrush = selectionBrush,
            contentPadding = contentPadding)
    }
}

@Composable fun InventoryScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val vm: InventoryViewModel = viewModel()

    val startColor = MaterialTheme.colors.primary
    val endColor = MaterialTheme.colors.secondary
    val selectionBrush = remember(startColor, endColor) {
        Brush.horizontalGradient(listOf(startColor, endColor))
    }

    val listViewCallback = remember {
        object: InventoryCallback {
            override fun onItemClick(id: Long) = vm.onItemClick(id)
            override fun onItemLongClick(id: Long) = vm.onItemLongClick(id)
            override fun onItemSwipe(id: Long) = vm.onItemSwipe(id)
            override fun onItemColorGroupClick(id: Long, colorGroup: ListItem.ColorGroup) =
                vm.onItemColorGroupChangeRequest(id, colorGroup)
            override fun onItemRenameRequest(id: Long, newName: String) =
                vm.onItemRenameRequest(id, newName)
            override fun onItemExtraInfoChangeRequest(id: Long, newExtraInfo: String) =
                vm.onItemExtraInfoChangeRequest(id, newExtraInfo)
            override fun onItemAmountChangeRequest(id: Long, newAmount: Int) =
                vm.onItemAmountChangeRequest(id, newAmount)
            override fun onItemEditButtonClick(id: Long) = vm.onItemEditButtonClick(id)
            override fun onItemAutoAddToShoppingListCheckboxClick(id: Long) =
                vm.onAutoAddToShoppingListCheckboxClick(id)
            override fun onItemAutoAddToShoppingListAmountChangeRequest(id: Long, newAmount: Int) =
                vm.onAutoAddToShoppingListAmountChangeRequest(id, newAmount)
        }
    }
    ItemListScreen(
        modifier = modifier,
        uiState = vm.uiState,
    ) { itemListState ->
        InventoryView(
            inventoryState = itemListState,
            callback = listViewCallback,
            selectionBrush = selectionBrush,
            contentPadding = contentPadding)
    }
}
