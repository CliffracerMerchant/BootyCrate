/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

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
            val name = valueDescriptions.getOrElse(index) { "" }
            Text(text = name)
            Spacer(Modifier.weight(1f))
            val vector = if (value == currentValue)
                Icons.Default.RadioButtonChecked
            else Icons.Default.RadioButtonUnchecked
            Icon(vector, name, Modifier.size(36.dp).padding(8.dp))
        }
    }
}

/**
 * Add a gradient background that matches a portion of a background gradient.
 *
 * @param orientation The [Orientation] of the gradient
 * @param shape The shape of the applied background
 * @param backgroundGradientWidth The total width of the background gradient
 * @param colors An [ImmutableList]`<Color>` that matches the [Color]s of the
 *     background gradient
 */
@SuppressLint("ComposableModifierFactory")
@Composable fun Modifier.clippedGradientBackground(
    orientation: Orientation,
    shape: Shape,
    backgroundGradientWidth: Dp,
    colors: ImmutableList<Color> = listOf(
        MaterialTheme.colors.primary,
        MaterialTheme.colors.secondary
    ).toImmutableList()
): Modifier = composed {
    val density = LocalDensity.current

    var layoutStartX by remember { mutableStateOf(0f) }

    val backgroundBrush = remember(backgroundGradientWidth, layoutStartX) {
        val gradientWidthPx = with (density) { backgroundGradientWidth.toPx() }
        val startX = -layoutStartX
        val endX = gradientWidthPx - layoutStartX
        if (orientation == Orientation.Horizontal)
            Brush.horizontalGradient(colors, startX, endX)
        else Brush.verticalGradient(colors, startX, endX)
    }

    background(backgroundBrush, shape)
        .onPlaced { layoutStartX = it.positionInRoot().x }
}