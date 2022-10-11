/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.ActionBarViewModel
import com.cliffracertech.bootycrate.model.database.ListItem

/** Compose a [ListActionBar] with state provided by an instance of [ActionBarViewModel]. */
@Composable fun BootyCrateActionBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: ActionBarViewModel = viewModel()
    val sortOptions = remember { enumValues<ListItem.Sort>().asList() }
    val sortOptionNames = remember { ListItem.Sort.stringValues(context) }
    val title = remember(viewModel.title) {
        viewModel.title.resolve(context)
    }

    ListActionBar(
        modifier = modifier,
        showBackButton = viewModel.showBackButton,
        onBackButtonClick = viewModel::onBackPressed,
        title = title,
        searchQuery = viewModel.searchQuery,
        onSearchQueryChanged = viewModel::onSearchQueryChangeRequest,
        showSearchButton = viewModel.showSearchButton,
        onSearchButtonClick = viewModel::onSearchButtonClick,
        changeSortDeleteButtonState = viewModel.changeSortDeleteButtonState,
        sortOptions = sortOptions,
        sortOptionDescriptions = sortOptionNames,
        currentSortOption = viewModel.sort,
        onSortOptionClick = viewModel::onSortOptionClick,
        onDeleteButtonClick = viewModel::onDeleteButtonClick
    ) {
        AnimatedVisibility(viewModel.showMoreOptionsButton) {
            var showingMenu by rememberSaveable { mutableStateOf(false) }
            IconButton({ showingMenu = true }) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.more_options_description))
                val dropdownMenuItem = @Composable { onClick: () -> Unit, titleResId: Int ->
                    DropdownMenuItem(onClick = {
                        showingMenu = false
                        onClick()
                    }) { Text(stringResource(titleResId)) }
                }
                DropdownMenu(
                    expanded = showingMenu,
                    onDismissRequest = { showingMenu = false },
                ) {
                    if (viewModel.addToInventoryActionVisible)
                        dropdownMenuItem(viewModel::onAddToInventoryClick, R.string.add_to_inventory_description)
                    if (viewModel.addToShoppingListActionVisible)
                        dropdownMenuItem(viewModel::onAddToShoppingListClick, R.string.add_to_shopping_list_description)
                    if (viewModel.checkAllActionVisible)
                        dropdownMenuItem(viewModel::onCheckAllClick, R.string.check_all_description)
                    if (viewModel.uncheckAllActionVisible)
                        dropdownMenuItem(viewModel::onUncheckAllClick, R.string.uncheck_all_description)
                    if (viewModel.selectAllActionVisible)
                        dropdownMenuItem(viewModel::onSelectAllClick, R.string.select_all_description)
                    if (viewModel.shareActionVisible)
                        dropdownMenuItem(viewModel::onShareClick, R.string.share_description)
                }
            }
        }
    }
}