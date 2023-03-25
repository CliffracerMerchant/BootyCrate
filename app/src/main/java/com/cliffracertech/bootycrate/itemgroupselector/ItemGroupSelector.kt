/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemgroupselector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ItemGroup
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable private fun ItemGroupSelectorTopBar(
    title: String,
    onSelectAllClick: () -> Unit,
    multiSelectGroups: Boolean,
    onMultiSelectClick: () -> Unit,
    modifier: Modifier = Modifier,
    otherContent: @Composable () -> Unit,
) = Row(
    modifier = modifier.height(56.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(title, Modifier.padding(start = 24.dp).weight(1f))
    otherContent()

    var showingOptionsMenu by remember { mutableStateOf(false) }
    IconButton({ showingOptionsMenu = true }) {
        Icon(Icons.Default.MoreVert, stringResource(
            R.string.item_group_selector_options_description))

        DropdownMenu(
            expanded = showingOptionsMenu,
            onDismissRequest = { showingOptionsMenu = false },
        ) {
            DropdownMenuItem({
                onMultiSelectClick()
                showingOptionsMenu = false
            }) {
                Text(stringResource(R.string.multi_select_item_groups_description))
                Switch(checked = multiSelectGroups,
                       onCheckedChange = null,
                       modifier = Modifier.padding(start = 8.dp))
            }
            DropdownMenuItem({
                onSelectAllClick()
                showingOptionsMenu = false
            }) {
                Text(stringResource(R.string.select_all_item_groups_description))
            }
        }
    }
}

/**
 * A display of an [ImmutableList] of [ItemGroup]s that matches [itemGroups],
 * along with a overhead top bar and a floating add button. The top bar contains
 * a title that matches the value of [title], an options overflow menu at the
 * end, and space for additional content defined by [otherTopBarContent] that
 * will be placed in between the title and the options menu. The options menu
 * contains a 'select all' option that calls [onSelectAllClick] when clicked,
 * and a 'multi-select groups' check box option that displays the current value
 * of [multiSelectGroups] and calls [onMultiSelectClick] when clicked.
 *
 * Interactions with the displayed [ItemGroup]s will invoke [onItemGroupClick],
 * [onItemGroupRenameClick], and [onItemGroupDeleteClick]. The add button will
 * invoke [onAddButtonClick] when clicked.
 */
@Composable fun ItemGroupSelector(
    title: String,
    onSelectAllClick: () -> Unit,
    multiSelectGroups: Boolean,
    onMultiSelectClick: () -> Unit,
    itemGroups: ImmutableList<ItemGroup>,
    onItemGroupClick: (ItemGroup) -> Unit,
    onItemGroupRenameClick: (ItemGroup) -> Unit,
    onItemGroupDeleteClick: (ItemGroup) -> Unit,
    onAddButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 8.dp,
    otherTopBarContent: @Composable () -> Unit,
) = Column(modifier) {
    ItemGroupSelectorTopBar(
        title = title,
        onSelectAllClick = onSelectAllClick,
        multiSelectGroups = multiSelectGroups,
        onMultiSelectClick = onMultiSelectClick,
        otherContent = otherTopBarContent)
    Box(Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 8.dp)
        .background(
            color = MaterialTheme.colors.background,
            shape = RoundedCornerShape(
                // 32.dp for the top corners is derived from the 24.dp corner
                // radius of the inner ItemGroupViews plus the 8.dp item content
                // padding. This causes the top ItemGroupView's corners to be
                // concentric with the background's top corners.
                topStart = 32.dp, topEnd = 32.dp,
                bottomStart = 0.dp, bottomEnd = 0.dp))
    ) {
        ItemGroupListView(
            itemGroups = itemGroups,
            onItemGroupClick = onItemGroupClick,
            onItemGroupRenameClick = onItemGroupRenameClick,
            onItemGroupDeleteClick = onItemGroupDeleteClick,
            contentPadding = PaddingValues(
                start = 8.dp, end = 8.dp, top = 8.dp,
                bottom = bottomContentPadding))
        FloatingActionButton(
            onClick = onAddButtonClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = bottomContentPadding),
            backgroundColor = MaterialTheme.colors.secondary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 12.dp,
                pressedElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, stringResource(
                R.string.add_item_group_description))
        }
    }
}

@Composable fun ItemGroupSelectorPreview(
    useDarkTheme: Boolean
) = BootyCrateTheme(useDarkTheme) {
    val startColor = MaterialTheme.colors.primary
    val endColor = MaterialTheme.colors.secondary
    val brush = remember { Brush.horizontalGradient(listOf(startColor, endColor)) }
    Box(Modifier
        .size(456.dp)
        .background(brush, RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp,
            bottomStart = 0.dp, bottomEnd = 0.dp)),
    ) {
        val itemGroups = remember {
            List(5) { ItemGroup(
                name = "Item Group $it",
                isSelected = it == 0,
                shoppingListItemCount = it,
                inventoryItemCount = 5 - it)
            }.toMutableStateList()
        }
        var multiSelectGroups by remember { mutableStateOf(false) }

        ItemGroupSelector(
            title = "ItemGroupSelector",
            onSelectAllClick = {
                multiSelectGroups = true
                itemGroups.replaceAll {
                    ItemGroup(
                        it.name,
                        it.shoppingListItemCount,
                        it.inventoryItemCount,
                        isSelected = true)
                }
            }, multiSelectGroups = multiSelectGroups,
            onMultiSelectClick = { multiSelectGroups = !multiSelectGroups },
            itemGroups = itemGroups.toImmutableList(),
            onItemGroupClick = { itemGroup ->
                when {
                    !multiSelectGroups -> {
                        itemGroups.replaceAll {
                            ItemGroup(
                                it.name,
                                it.shoppingListItemCount,
                                it.inventoryItemCount,
                                isSelected = it == itemGroup)
                        }
                    } !itemGroup.isSelected -> {
                        itemGroups.replaceAll {
                            ItemGroup(
                                it.name,
                                it.shoppingListItemCount,
                                it.inventoryItemCount,
                                isSelected = it.isSelected || it == itemGroup)
                        }
                    } else -> {
                        val otherSelectedItem = itemGroups
                            .find { it.isSelected && it != itemGroup }
                        if (otherSelectedItem != null)
                            itemGroups.replaceAll {
                                ItemGroup(
                                    it.name,
                                    it.shoppingListItemCount,
                                    it.inventoryItemCount,
                                    isSelected = it.isSelected && it != itemGroup)
                            }
                    }
                }
            },
            onItemGroupRenameClick = {},
            onItemGroupDeleteClick = itemGroups::remove,
            onAddButtonClick = {
                val sortedItemGroups = itemGroups.sortedBy { it.name.last().digitToInt() }
                val number = sortedItemGroups.last().name.last().digitToInt() + 1
                itemGroups.add(ItemGroup(
                    name = "Item Group $number",
                    shoppingListItemCount = number,
                    inventoryItemCount = number + 1))
            }, otherTopBarContent = {
                IconButton({}) {
                    Icon(Icons.Default.Settings, null)
                }
            })
    }
}

@Preview @Composable fun LightItemGroupSelectorPreview() = ItemGroupSelectorPreview(false)
@Preview @Composable fun DarkItemGroupSelectorPreview() = ItemGroupSelectorPreview(true)