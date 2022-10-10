/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.ActionBarViewModel
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.utils.DropdownMenuButton

/** Compose a [ListActionBar] with state provided by an instance of [ActionBarViewModel]. */
@Composable
fun BootyCrateActionBar(modifier: Modifier = Modifier) {
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
            DropdownMenuButton(
                description = stringResource(R.string.more_options_description)
            ) {
                viewModel.optionsMenuItems.forEach { (menuItem, stringResId) ->
                    DropdownMenuItem({ viewModel.onOptionsMenuItemClick(menuItem) }) {
                        Text(stringResource(stringResId))
                    }
                }
            }
        }
    }
}