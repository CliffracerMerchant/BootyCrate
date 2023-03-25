/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.actionbar

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * A title for an action bar with search query support.
 *
 * ActionBarTitle functions as a combo display for an action bar's title
 * and a search query. When the parameter [searchQuery] is not null, it
 * will be displayed inside of an editable text field instead of the
 * parameter [title]. When the value inside the text field is changed,
 * the callback [onSearchQueryChanged] will be invoked.
 */
@Composable fun ActionBarTitle(
    title: String,
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    onSearchQueryChanged: (String) -> Unit,
) = Crossfade(// This outer cross fade is for when the search query appears/disappears.
    targetState = searchQuery == null,
    modifier = modifier,
) { searchQueryIsNull ->
    // lastSearchQuery is used so that when the search query changes from a
    // non-null non-blank value to null, the search query will be recomposed
    // with the value of lastSearchQuery instead of null during the search
    // query's fade out animation. This allows the last non-null search
    // query text to fade out with the rest of the search query (i.e. the
    // underline) instead of abruptly disappearing.
    var lastSearchQuery by remember { mutableStateOf("") }
    val queryText = searchQuery ?: lastSearchQuery

    if (searchQueryIsNull)
        Crossfade(title) { // This inner cross fade is for when the title changes.
            Text(it, style = MaterialTheme.typography.h6, maxLines = 1)
        }
    else {
        AutoFocusSearchQuery(queryText, onSearchQueryChanged)
        if (searchQuery != null)
            lastSearchQuery = searchQuery
    }
}

/**
 * A search query that auto-focuses when first composed,
 * and displays an underline as a background.
 *
 * @param query The current value of the search query
 * @param onQueryChanged The callback to be invoked when user input changes the query
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable fun AutoFocusSearchQuery(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        textStyle = MaterialTheme.typography.h6
            .copy(color = MaterialTheme.colors.onPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, ),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) keyboardController?.show()
            },
        singleLine = true,
    ) { innerTextField ->
        Box {
            innerTextField()
            Divider(
                modifier = Modifier.align(Alignment.BottomStart),
                color = LocalContentColor.current,
                thickness = (1.5).dp)
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}