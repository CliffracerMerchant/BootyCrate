/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/**
 * A [DropdownMenu] that displays an option for each value of the enum type
 * parameter, and a checked or unchecked radio button besides each to show
 * the currently selected value.
 *
 * @param expanded Whether the dropdown menu is displayed.
 * @param values An array of all possible values for the enum type,
 *               usually accessed with [enumValues]<T>().
 * @param valueDescriptions An Array<String> containing [String] values to use
 *                          to represent each value of the parameter enum type T.
 * @param currentValue The currently selected enum value.
 * @param onValueClick The callback that will be invoked when a value's button is clicked.
 * @param onDismissRequest The callback that will be invoked when the menu should be dismissed.
 */
@Composable fun <T> EnumDropdownMenu(
    expanded: Boolean,
    values: List<T>,
    valueDescriptions: List<String>,
    currentValue: T,
    onValueClick: (T) -> Unit,
    onDismissRequest: () -> Unit
) = DropdownMenu(expanded, onDismissRequest) {
    values.forEachIndexed { index, value ->
        DropdownMenuItem({ onValueClick(value); onDismissRequest() }) {
            val name = valueDescriptions.getOrNull(index) ?: "Error"
            Text(text = name)
            Spacer(Modifier.weight(1f))
            val vector = if (value == currentValue)
                Icons.Default.RadioButtonChecked
            else Icons.Default.RadioButtonUnchecked
            Icon(vector, name, Modifier.size(36.dp).padding(8.dp))
        }
    }
}

@Composable fun <T> EnumDropdownMenuButton(
    modifier: Modifier = Modifier,
    description: String? = null,
    values: List<T>,
    valueDescriptions: List<String>,
    currentValue: T,
    onValueClick: (T) -> Unit,
    icon: @Composable (String?) -> Unit = {
        Icon(Icons.Default.MoreVert, it)
    }
) {
    var showingMenu by rememberSaveable { mutableStateOf(false) }
    IconButton({ showingMenu = true }, modifier) {
        icon(description)
        EnumDropdownMenu(
            expanded = showingMenu,
            values = values,
            valueDescriptions = valueDescriptions,
            currentValue = currentValue,
            onValueClick = onValueClick,
            onDismissRequest = { showingMenu = false })
    }
}