/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.actionbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.springStiffness
import com.cliffracertech.bootycrate.utils.EnumDropdownMenu

/** Compose a [Row] with a gradient background and vertically centered
 * content, while providing the current theme's onPrimary color as the
 * [LocalContentColor]. */
@Composable fun GradientToolBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val gradStart = MaterialTheme.colors.primary
    val gradEnd = MaterialTheme.colors.secondary
    val gradient = remember(gradStart, gradEnd) {
        Brush.horizontalGradient(listOf(gradStart, gradEnd))
    }
    Row(modifier = modifier
            .fillMaxWidth()
            .background(gradient)
            .statusBarsPadding()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = MaterialTheme.colors.onPrimary
        CompositionLocalProvider(LocalContentColor provides color) {
            content()
        }
    }
}

/**
 * A horizontal bar that is suitable to be used as a top action bar when
 * displaying a list of items. The bar integrates an optional back button, a
 * navigation title / search query, an optional search button, a button that
 * doubles as a change sort button (which opens a drop down menu to choose a
 * sorting method when clicked) or a delete button, and any other content
 * passed in through the parameter [otherContent]. The title will be replaced
 * by the search query if it is not null.
 *
 * @param modifier The [Modifier] to use for the ListActionBar.
 * @param showBackButton Whether or not the back button will be shown.
 * @param onBackButtonClick The callback that will be invoked when the back button is clicked.
 * @param title The title that will be displayed when there is no search query.
 * @param searchQuery The current search query that will be displayed if not null.
 * @param onSearchQueryChanged The callback that will be invoked when the
 *     user input should modify the value of the search query.
 * @param showSearchButton Whether or not the search button will be shown.
 * @param onSearchButtonClick The callback that will be invoked when the
 *     user clicks the search button. Typically this should set the search
 *     query to an empty string if it is already null so that the search
 *     query entry will appear, or set it to null if it is not null so that
 *     the search query entry will be closed.
 * @param changeSortDeleteButtonState The value of [ChangeSortDeleteButtonState]
 *     that indicates the state of the  change sort / delete button.
 * @param sortOptions An array of all possible sorting enum values,
 *     usually accessed with [enumValues]<T>()
 * @param sortOptionDescriptions An array containing the [String] values that
 *     should represent each sorting option.
 * @param currentSortOption A value of the type parameter that indicates
 *     the currently selected sort option.
 * @param onSortOptionClick The callback that will be invoked when a sort
 *     option in the change sort menu is clicked.
 * @param onDeleteButtonClick The callback that will be invoked when the
 *     change sort / delete button is clicked while
 * @param otherContent A composable containing other contents that should
 *     be placed at the end of the action bar.
 */
@Composable fun <T> ListActionBar(
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackButtonClick: () -> Unit,
    title: String,
    searchQuery: String? = null,
    onSearchQueryChanged: (String) -> Unit,
    showSearchButton: Boolean = true,
    onSearchButtonClick: () -> Unit,
    changeSortDeleteButtonState: ChangeSortDeleteButtonState,
    sortOptions: List<T>,
    sortOptionDescriptions: List<String>,
    currentSortOption: T,
    onSortOptionClick: (T) -> Unit,
    onDeleteButtonClick: () -> Unit,
    otherContent: @Composable RowScope.() -> Unit,
) = GradientToolBar(modifier) {
    // Back button
    val springSpec = spring<IntOffset>(stiffness = springStiffness)
    AnimatedContent(
        targetState = showBackButton,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            slideInHorizontally(springSpec) { -it } with
            slideOutHorizontally(springSpec) { -it } using
            SizeTransform(clip = false) { _, _ ->
                spring(stiffness = springStiffness)
            }}
    ) { backButtonIsVisible ->
        if (!backButtonIsVisible)
            Spacer(Modifier.width(24.dp))
        else IconButton(onClick = onBackButtonClick) {
            Icon(Icons.Default.ArrowBack, stringResource(R.string.back_description))
        }
    }

    // Title / search query
    ActionBarTitle(
        title,
        Modifier.weight(1f),
        searchQuery,
        onSearchQueryChanged)

    val enter = fadeIn(spring(stiffness = springStiffness)) +
                scaleIn(spring(stiffness = springStiffness), 1.25f)
    val exit = fadeOut(spring(stiffness = springStiffness)) +
               scaleOut(spring(stiffness = springStiffness), 0.8f)
    // Search button
    AnimatedVisibility(showSearchButton, Modifier, enter, exit) {
        val vector = AnimatedImageVector.animatedVectorResource(R.drawable.animated_search_to_close)
        val painter = rememberAnimatedVectorPainter(vector, searchQuery != null)
        IconButton(onClick = onSearchButtonClick) {
            Icon(painter, stringResource(R.string.search_description))
        }
    }
    // Change sort button
    AnimatedVisibility(
        !changeSortDeleteButtonState.isInvisible,
        Modifier, enter, exit
    ) {
        var showingSortMenu by remember { mutableStateOf(false) }
        IconButton(onClick = {
            if (changeSortDeleteButtonState.isChangeSort)
                showingSortMenu = true
            else onDeleteButtonClick()
        }) {
            val vector = AnimatedImageVector.animatedVectorResource(R.drawable.animated_sort_to_delete)
            val isDelete = changeSortDeleteButtonState.isDelete
            Icon(painter = rememberAnimatedVectorPainter(vector, isDelete),
                 contentDescription = stringResource(
                     if (isDelete) R.string.delete_button_description
                     else          R.string.change_sorting_description))
            EnumDropdownMenu(
                expanded = showingSortMenu,
                values = sortOptions,
                valueDescriptions = sortOptionDescriptions,
                currentValue = currentSortOption,
                onValueClick = onSortOptionClick,
                onDismissRequest = { showingSortMenu = false })
        }
    }
    otherContent()
}