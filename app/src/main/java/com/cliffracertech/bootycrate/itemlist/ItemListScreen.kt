/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.springStiffness
import com.cliffracertech.bootycrate.tweenDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

/** An interface that describes the possible states for a list of selectable,
 * expandable items that only allows one expanded item at a time. */
interface ItemListState<T> {
    val items: ImmutableList<T>
    val selectedItemIds: ImmutableSet<Long>
    val expandedItemId: Long?
}

/**
 * ItemListScreen visualizes an instance of [AsyncListState]. When [listState]'s
 * value is:
 * - [AsyncListState.Loading], a loading indicator will be shown
 * - [AsyncListState.Message], the message will be shown
 * - [AsyncListState.Content]`<T>`, the list of items will be shown. Note
 *        that if the [AsyncListState.Content]'s type parameter does
 *        not match the [T] type parameter for the ItemListScreen, a
 *        [ClassCastException] will be thrown.
 * @param listState The [AsyncListState] to be visualized
 * @param modifier The [Modifier] for the root layout
 * @param lazyListState The [LazyListState] to use for the list of items
 * @param contentPadding A [PaddingValues] instance that will be used as
 *     regular padding when [listState]'s value is [AsyncListState.Loading]
 *     or [AsyncListState.Message], or as the [LazyColumn.contentPadding] when
 *     [listState] is a [AsyncListState.Content].
 * @param itemContent The content that will be shown for each [T] instance
 *     when [listState] is a [AsyncListState.Content]`<T>` instance
 */
@Composable fun <T: ListItem> ItemListScreen(
    listState: AsyncListState,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable LazyItemScope.(
            item: T,
            isSelected: Boolean,
            isExpanded: Boolean
        ) -> Unit,
) = Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    // AnimatedContent is used here instead of Crossfade due
    // to Crossfade's lack of a contentAlignment parameter
    AnimatedContent(
        targetState = listState,
        modifier = Modifier.padding(contentPadding),
        transitionSpec = { fadeIn(tween(tweenDuration, easing = LinearEasing)) with
                           fadeOut(tween(tweenDuration, easing = LinearEasing)) },
        contentAlignment = Alignment.Center
    ) { state -> when (state) {
        is AsyncListState.Loading ->
            CircularProgressIndicator(Modifier.size(50.dp))
        is AsyncListState.Message ->
            Text(state.text.resolve(LocalContext.current))
        else -> {}
    }}

    // The item list is outside of the AnimatedContent block so that the
    // item list will only fade in/out when listState changes to/from
    // AsyncListState.Content, instead of every time the list changes.
    AnimatedVisibility(
        visible = listState is AsyncListState.Content<*>,
        enter = fadeIn(tween(tweenDuration, easing = LinearEasing)),
        exit = fadeOut(tween(tweenDuration, easing = LinearEasing)),
    ) {
        @Suppress("unchecked_cast")
        val contentState = listState as? AsyncListState.Content<T>
        val currentItems = contentState?.items
        // Because LazyColumn does not have appearance/disappearance animations
        // for items, lastNonNullItems is used so that the last non-null list of
        // items will fade out when listState changes from AsyncListState.Content
        // to one of the other types instead of the items abruptly disappearing.
        var lastNonNullItems = remember { currentItems ?: emptyList() }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(
                items = currentItems ?: lastNonNullItems,
                key = ListItem::id::get
            ) {
                val isSelected = contentState?.selectedItemIds?.contains(it.id) ?: false
                val isExpanded = it.id == contentState?.expandedItemId
                itemContent(it, isSelected, isExpanded)
            }
        }
        if (currentItems != null)
            lastNonNullItems = currentItems
    }
}

/**
 * An interactable list of [ShoppingListItemView]s.
 *
 * @param modifier The [Modifier] that will be used for the root layout
 * @param lazyListState The [LazyListState] to use for the internal [LazyColumn]
 * @param contentPadding The [PaddingValues] instance to use for the [LazyColumn]'s content
 */
@Composable fun ShoppingListScreen(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val vm: ShoppingListViewModel = viewModel()
    val startColor = MaterialTheme.colors.primary
    val endColor = MaterialTheme.colors.secondary
    val selectionBrush = remember(startColor, endColor) {
        Brush.horizontalGradient(listOf(startColor, endColor))
    }
    val itemCallback = remember {
        shoppingListItemCallback(
            onClick = vm::onItemClick,
            onLongClick = vm::onItemLongClick,
            onSwipe = vm::onItemSwipe,
            onColorGroupClick = vm::onItemColorGroupChangeRequest,
            onRenameRequest = vm::onItemRenameRequest,
            onExtraInfoChangeRequest = vm::onItemExtraInfoChangeRequest,
            onAmountChangeRequest = vm::onItemAmountChangeRequest,
            onEditButtonClick = vm::onItemEditButtonClick,
            onCheckboxClick = vm::onItemCheckboxClick)
    }
    ItemListScreen<ShoppingListItem>(
        listState = vm.uiState,
        modifier = modifier,
        lazyListState = lazyListState,
        contentPadding = contentPadding,
    ) { item, isSelected, isExpanded ->
        ShoppingListItemView(
            sizes = rememberListItemViewSizes(),
            item = item,
            isEditable = isExpanded,
            isSelected = isSelected,
            selectionBrush = selectionBrush,
            callback = itemCallback,
            modifier = Modifier.animateItemPlacement(
                spring(stiffness = springStiffness)))
    }
}

/**
 * An interactable list of [InventoryItemView]s.
 *
 * @param modifier The [Modifier] that will be used for the root layout
 * @param lazyListState The [LazyListState] to use for the internal [LazyColumn]
 * @param contentPadding The [PaddingValues] instance to use for the [LazyColumn]'s content
 */
@Composable fun InventoryScreen(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val vm: InventoryViewModel = viewModel()
    val startColor = MaterialTheme.colors.primary
    val endColor = MaterialTheme.colors.secondary
    val selectionBrush = remember(startColor, endColor) {
        Brush.horizontalGradient(listOf(startColor, endColor))
    }
    val itemCallback = remember {
        inventoryItemCallback(
            onClick = vm::onItemClick,
            onLongClick = vm::onItemLongClick,
            onSwipe = vm::onItemSwipe,
            onColorGroupClick = vm::onItemColorGroupChangeRequest,
            onRenameRequest = vm::onItemRenameRequest,
            onExtraInfoChangeRequest = vm::onItemExtraInfoChangeRequest,
            onAmountChangeRequest = vm::onItemAmountChangeRequest,
            onEditButtonClick = vm::onItemEditButtonClick,
            onAutoAddToShoppingListCheckboxClick = vm::onAutoAddToShoppingListCheckboxClick,
            onAutoAddToShoppingListAmountChangeRequest = vm::onAutoAddToShoppingListAmountChangeRequest)
    }
    ItemListScreen<InventoryItem>(
        listState = vm.uiState,
        modifier = modifier,
        lazyListState = lazyListState,
        contentPadding = contentPadding,
    ) { item, isSelected, isExpanded ->
        InventoryItemView(
            sizes = rememberInventoryItemViewSizes(),
            item = item,
            isEditable = isExpanded,
            isSelected = isSelected,
            selectionBrush = selectionBrush,
            callback = itemCallback,
            modifier = Modifier.animateItemPlacement(
                spring(stiffness = springStiffness)))
    }
}
