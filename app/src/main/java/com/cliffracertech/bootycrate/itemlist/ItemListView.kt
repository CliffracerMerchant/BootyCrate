/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.model.database.ListItem

/** An interface containing callbacks for interactions with [ItemListView]'s
* [ListItemView]s. The Long parameter in each method indicates the id of the
* [ListItem] that was interacted with. */
interface ItemListCallback {
    /** The callback that will be invoked when the item is clicked. */
    fun onClick(id: Long)
    /** The callback that will be invoked when the item is long clicked. */
    fun onLongClick(id: Long)
    /** The callback that will be invoked when the item is swiped left or right. */
    fun onSwipe(id: Long)
    /** The callback that will be invoked when the item's
     * color has been requested to be changed to [newColor]. */
    fun onColorChangeRequest(id: Long, newColor: ListItem.Color)
    /** The callback that will be invoked when the item's
     * name has been requested to be changed to [newName]*/
    fun onRenameRequest(id: Long, newName: String)
    /** The callback that will be invoked when the item's extraInfo
     * has been requested to be changed to [newExtraInfo]*/
    fun onExtraInfoChangeRequest(id: Long, newExtraInfo: String)
    /**  The callback that will be invoked when the item's amount
     * has been requested to be changed to [newAmount]*/
    fun onAmountChangeRequest(id: Long, newAmount: Int)
    /** The callback that will be invoked when the item's edit button is clicked. */
    fun onEditButtonClick(id: Long)
}

/**
* A [LazyColumn] to display a list of [ListItem]s or a sub-class.
*
* @param itemList The list of [ListItem]s to display
* @param callback An [ItemListCallback] that describes callbacks
*                 to use for interactions with the item views
* @param modifier The [Modifier] that will be used for the root layout
* @param state The [LazyListState] to use for the internal [LazyColumn]
* @param contentPadding The [PaddingValues] instance to use for the [LazyColumn]'s content
* @param itemContent A lambda that contains the content for each item
*                    given the [ListItem] instance it is representing
*                    and a callback to use for item interactions.
*/
@Composable fun <T: ListItem> ItemListView(
    itemList: List<T>,
    callback: ItemListCallback,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    itemContent: @Composable LazyItemScope.(item: T, callback: ItemListCallback) -> Unit
) = LazyColumn(
    modifier = modifier,
    state = state,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    items(itemList, key = { it.id }, contentType = {}) {
        itemContent(it, callback)
    }
}