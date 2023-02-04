/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme
import com.cliffracertech.bootycrate.utils.clippedGradientBackground
import kotlin.math.roundToInt

@Composable fun BootyCrateAddButton(
    backgroundGradientWidth: Dp,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .size(56.dp).clip(CircleShape)
        .clippedGradientBackground(
            Orientation.Horizontal,
            CircleShape,
            backgroundGradientWidth)
        .clickable(
            onClickLabel = stringResource(
                R.string.add_button_description),
            role = Role.Button,
            onClick = {}),
    contentAlignment = Alignment.Center
) {
    Icon(Icons.Default.Add,
         stringResource(R.string.add_button_description))
}


@Preview @Composable fun BootyCrateAddButtonPreview() =
    BootyCrateTheme {
        var xOffset by remember { mutableStateOf(0f) }
        val buttonWidth = with (LocalDensity.current) { 56.dp.toPx() }
        val width = with (LocalDensity.current) { 1080.toDp() }

        Column(Modifier.width(width), Arrangement.spacedBy(6.dp)) {
            BootyCrateAddButton(
                backgroundGradientWidth = width,
                modifier = Modifier.offset {
                    IntOffset(xOffset.roundToInt(), 0)
                })
            Box(modifier = Modifier
                    .fillMaxWidth().height(56.dp)
                    .background(Brush.horizontalGradient(
                        listOf(MaterialTheme.colors.primary,
                               MaterialTheme.colors.secondary)))
                    .draggable(
                        state = rememberDraggableState {
                            xOffset = (xOffset + it).coerceIn(0f, 1080f - buttonWidth)
                        }, orientation = Orientation.Horizontal),
                contentAlignment = Alignment.Center
            ) { Text("Slide to change button offset") }
        }

    }