/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemgroupselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.model.database.ItemGroup
import kotlinx.collections.immutable.ImmutableList



/**
 * A view of a list of [ItemGroup]s.
 *
 * @param itemGroups An [ImmutableList] of the [ItemGroup]s to display
 * @param onItemGroupRenameClick The callback that will be invoked when the user
 *     clicks the rename option in the provided [ItemGroup]'s options popup menu
 * @param onItemGroupDeleteClick The callback that will be invoked when the user
 *     clicks the delete option in the provided [ItemGroup]'s options popup menu
 * @param modifier The [Modifier] to use for the entire list layout
 * @param contentPadding A [PaddingValues] instance that describes the content's padding
 */
@Composable fun ItemGroupListView(
    itemGroups: ImmutableList<ItemGroup>,
    onItemGroupClick: (ItemGroup) -> Unit,
    onItemGroupRenameClick: (ItemGroup) -> Unit,
    onItemGroupDeleteClick: (ItemGroup) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val brushStartColor = MaterialTheme.colors.primary
    val brushEndColor = MaterialTheme.colors.secondary
    val selectionBrush = remember(brushStartColor, brushEndColor) {
        Brush.horizontalGradient(listOf(brushStartColor, brushEndColor))
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { items(itemGroups) {
            ItemGroupView(
                itemGroup = it,
                isSelected = false,
                selectionBrush = selectionBrush,
                onClick = { onItemGroupClick(it) },
                onRenameClick = { onItemGroupRenameClick(it) },
                onDeleteClick = { onItemGroupDeleteClick(it) })
        }})
}
