/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cliffracertech.bootycrate.model.database.InventoryItem
import kotlinx.collections.immutable.ImmutableList

/** An interface containing callbacks for interactions with [InventoryView]'s
* [InventoryItemView]s. The Long parameter in each method indicates the id of
* the [InventoryItem] that was interacted with. */
interface InventoryCallback : ItemListCallback {
    /** The callback that will be invoked when an item's
    * auto-add to shopping list checkbox is clicked */
    fun onItemAutoAddToShoppingListCheckboxClick(id: Long)
    /** The callback that will be invoked when an item's auto-add to
    * shopping list amount has been requested to be changed to [newAmount] */
    fun onItemAutoAddToShoppingListAmountChangeRequest(id: Long, newAmount: Int)
}

/**
* An interactable list of [InventoryItemView]s.
*
* @param itemList The list of [InventoryItem]s to display
* @param callback An [InventoryCallback] that describes callbacks
*                 to use for interactions with the item views
* @param modifier The [Modifier] that will be used for the root layout
* @param state The [LazyListState] to use for the internal [LazyColumn]
* @param contentPadding The [PaddingValues] instance to use for the [LazyColumn]'s content
*/
@Composable fun InventoryView(
    itemList: ImmutableList<InventoryItem>,
    callback: InventoryCallback,
    itemIsExpandedProvider: (Long) -> Boolean,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) = ItemListView<InventoryItem>(
    itemList = itemList,
    modifier = modifier,
    state = state,
    contentPadding = contentPadding,
) { item ->
    val itemCallback = remember(item.id, callback) {
        inventoryItemCallback(
            onClick = { callback.onItemClick(item.id) },
            onLongClick = { callback.onItemLongClick(item.id) },
            onColorChangeRequest = { callback.onItemColorChangeRequest(item.id, it) },
            onRenameRequest = { callback.onItemRenameRequest(item.id, it) },
            onExtraInfoChangeRequest = { callback.onItemExtraInfoChangeRequest(item.id, it) },
            onAmountChangeRequest = { callback.onItemAmountChangeRequest(item.id, it) },
            onEditButtonClick = { callback.onItemEditButtonClick(item.id) },
            onAutoAddToShoppingListCheckboxClick = {
                callback.onItemAutoAddToShoppingListCheckboxClick(item.id)
            }, onAutoAddToShoppingListAmountChangeRequest = {
                callback.onItemAutoAddToShoppingListAmountChangeRequest(item.id, it)
            })
    }
    InventoryItemView(
        item = item,
        isEditableProvider = { itemIsExpandedProvider(item.id) },
        callback = itemCallback)
}