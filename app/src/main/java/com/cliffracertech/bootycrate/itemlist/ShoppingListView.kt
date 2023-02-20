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
import androidx.compose.ui.graphics.Brush
import com.cliffracertech.bootycrate.model.database.ShoppingListItem

/** An interface containing callbacks for interactions with [ShoppingListView]'s
 * [ShoppingListItemView]s. The Long parameter in each method indicates the id of the
 * [ShoppingListItem] that was interacted with. */
interface ShoppingListCallback : ItemListCallback {
    /** The callback that will be invoked when an item's checkbox is clicked */
    fun onItemCheckboxClick(id: Long)
}

/**
 * An interactable list of [ShoppingListItemView]s.
 *
 * @param shoppingListState The [ItemListState]`<ShoppingListItem>` instance that
 *     contains the [ShoppingListItem]s to display as well as the selection and
 *     item expansion state
 * @param callback A [ShoppingListCallback] that describes callbacks
 *     to use for interactions with the item views
 * @param selectionBrush The [Brush] that will be shown at half
 *     opacity over the normal item background when an item is selected
 * @param modifier The [Modifier] that will be used for the root layout
 * @param lazyListState The [LazyListState] to use for the internal [LazyColumn]
 * @param contentPadding The [PaddingValues] instance to use for the [LazyColumn]'s content
 */
@Composable fun ShoppingListView(
    shoppingListState: ItemListState<ShoppingListItem>,
    callback: ShoppingListCallback,
    selectionBrush: Brush,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) = ItemListView(
    itemListState = shoppingListState,
    modifier = modifier,
    lazyListState = lazyListState,
    contentPadding = contentPadding,
) { item ->
    val itemCallback = remember(callback) {
        shoppingListItemCallback(
            onClick = { callback.onItemClick(item.id) },
            onLongClick = { callback.onItemLongClick(item.id) },
            onColorGroupChangeRequest = { callback.onItemColorGroupChangeRequest(item.id, it) },
            onRenameRequest = { callback.onItemRenameRequest(item.id, it) },
            onExtraInfoChangeRequest = { callback.onItemExtraInfoChangeRequest(item.id, it) },
            onAmountChangeRequest = { callback.onItemAmountChangeRequest(item.id, it) },
            onEditButtonClick = { callback.onItemEditButtonClick(item.id) },
            onCheckboxClick = { callback.onItemCheckboxClick(item.id) })
    }
    ShoppingListItemView(
        item = item,
        isSelected = shoppingListState.selectedItemIds.contains(item.id),
        selectionBrush = selectionBrush,
        isEditable = shoppingListState.expandedItemId == item.id,
        callback = itemCallback)
}