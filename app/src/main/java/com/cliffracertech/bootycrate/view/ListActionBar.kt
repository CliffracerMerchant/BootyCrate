/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import androidx.compose.animation.*
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R

/**
 * A horizontal bar that is suitable to be used as a top action bar when
 * displaying a list of items. The bar integrates an optional back button, a
 * navigation title / search query, an optional search button and a button to
 * open a list of sorting options, and any other content passed in through the
 * parameter [otherContent]. The bar will always display a back button if there
 * is an active search query (i.e. [searchQuery] is not null), but will otherwise
 * only display it if [showBackButtonForNavigation] is true. The title will be
 * replaced by the search query if it is not null.
 *
 * @param showBackButtonForNavigation Whether or not the back button should be
 *     visible due to other state held outside the action bar. If [searchQuery]
 *     is not null, the back button will be shown regardless.
 * @param onBackButtonClick The callback that will be invoked when the back
 *     button is clicked while [showBackButtonForNavigation] is true. If the
 *     back button is shown due to a non-null search query, the back button
 *     will close the search query and [onBackButtonClick] will not be called.
 * @param title The title that will be displayed when there is no search query.
 * @param searchQuery The current search query that will be displayed if not null.
 * @param onSearchQueryChanged The callback that will be invoked when the
 *     user input should modify the value of the search query.
 * @param showRightAlignedContent Whether or not the contents to the right
 *     of the back button and title / search query should be shown. If false
 *     the search button, change sort button, and any content described in
 *     the parameter content will be invisible.
 * @param onSearchButtonClick The callback that will be invoked when the
 *     user clicks the search button. Typically this should set the search
 *     query to an empty string if it is already null so that the search
 *     query entry will appear, or set it to null if it is not null so that
 *     the search query entry will be closed.
 * @param sortOptions An array of all possible sorting enum values,
 *     usually accessed with [enumValues]<T>()
 * @param sortOptionNames An array containing the [String] values that
 *     should represent each sorting option.
 * @param currentSortOption A value of the type parameter that indicates
 *     the currently selected sort option.
 * @param otherContent A composable containing other contents that should
 *     be placed at the end of the action bar.
 */
@Composable fun <T> ListActionBar(
    showBackButtonForNavigation: Boolean = false,
    onBackButtonClick: () -> Unit,
    title: String,
    searchQuery: String? = null,
    onSearchQueryChanged: (String?) -> Unit,
    showRightAlignedContent: Boolean = true,
    onSearchButtonClick: () -> Unit,
    sortOptions: Array<T>,
    sortOptionNames: Array<String>,
    currentSortOption: T,
    onSortOptionClick: (T) -> Unit,
    otherContent: @Composable () -> Unit,
) = Row {
    // Back button
    AnimatedContent(
        targetState = showBackButtonForNavigation || searchQuery != null,
        contentAlignment = Alignment.Center,
        transitionSpec = { slideInHorizontally { -it } with
                           slideOutHorizontally { -it } using
                           SizeTransform(clip = false) }
    ) { backButtonIsVisible ->
        if (!backButtonIsVisible)
            Spacer(Modifier.width(24.dp))
        else IconButton(onClick = {
            if (searchQuery == null)
                onBackButtonClick()
            else onSearchQueryChanged(null)
        }) {
            Icon(Icons.Default.ArrowBack, stringResource(R.string.back_description))
        }
    }

    // Title / search query
    ActionBarTitle(
        title,
        Modifier.weight(1f),
        searchQuery,
        onSearchQueryChanged)

    // Right aligned content
    AnimatedVisibility(
        visible = showRightAlignedContent,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Search button
            val vector = AnimatedImageVector.animatedVectorResource(R.drawable.search_and_close)
            val painter = rememberAnimatedVectorPainter(vector, searchQuery != null)
            IconButton(onClick = onSearchButtonClick) {
                Icon(painter, stringResource(R.string.search_description))
            }
            // Change sort button
            var sortMenuShown by rememberSaveable { mutableStateOf(false) }
            IconButton(onClick = { sortMenuShown = !sortMenuShown }) {
                Icon(imageVector = Icons.Default.Sort,
                     stringResource(R.string.change_sorting_description))
                EnumDropDownMenu(
                    expanded = sortMenuShown,
                    values = sortOptions,
                    valueNames = sortOptionNames,
                    currentValue = currentSortOption,
                    onValueChanged = onSortOptionClick,
                    onDismissRequest = { sortMenuShown = false })
            }
            otherContent()
        }
    }
}

/**
 * A [DropdownMenu] that displays an option for each value of the enum type
 * parameter, and a checked or unchecked radio button besides each to show
 * the currently selected value.
 *
 * @param expanded Whether the dropdown menu is displayed.
 * @param values An array of all possible values for the enum type,
 *               usually accessed with [enumValues]<T>().
 * @param valueNames An Array<String> containing [String] values to use
 *                   to represent each value of the parameter enum type T.
 * @param currentValue The currently selected enum value.
 * @param onValueChanged The callback that will be invoked when the user taps an item.
 * @param onDismissRequest The callback that will be invoked when the menu should
 *                         be dismissed.
 */
@Composable fun <T> EnumDropDownMenu(
    expanded: Boolean,
    values: Array<T>,
    valueNames: Array<String>,
    currentValue: T,
    onValueChanged: (T) -> Unit,
    onDismissRequest: () -> Unit
) = DropdownMenu(expanded, onDismissRequest) {
    values.forEachIndexed { index, value ->
        DropdownMenuItem({ onValueChanged(value); onDismissRequest() }) {
            val name = valueNames.getOrNull(index) ?: "Error"
            Text(text = name, style = MaterialTheme.typography.button)
            Spacer(Modifier.weight(1f))
            val vector = if (value == currentValue)
                Icons.Default.RadioButtonChecked
            else Icons.Default.RadioButtonUnchecked
            Icon(vector, name, Modifier.size(36.dp).padding(8.dp))
        }
    }
}