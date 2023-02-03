 /* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracertech.bootycrate.bottombar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scaleMatrix
import com.cliffracertech.bootycrate.R
import kotlinx.coroutines.delay

 @Composable fun CheckoutButton(
     enabled: Boolean,
     backgroundBrush: Brush,
     modifier: Modifier = Modifier,
     timeOutMillis: Long = 2000L,
     onConfirm: () -> Unit,
) {
    val density = LocalDensity.current
    val shape = remember {
        GenericShape { size, _ ->
            val pathData = "M108,0 h-88 A 20 20 0 0 0 0,20 A 28 28 0 0 0 28,48 h90 A 32,32 0 0 1 108,0 Z"
            PathParser().parsePathString(pathData).toPath(this)
            // The coordinates used in the path data are intended to be in dp, so
            // they need to be scaled by the density to be converted to pixel sizes
            val matrix = scaleMatrix(density.density, density.density)
            asAndroidPath().transform(matrix)
        }
    }
    // The value of enabled is used as a key for inConfirmatoryState so
    // that the button will immediately drop out of its confirmatory
    // state when the button becomes disabled
    var inConfirmatoryState by remember(enabled) { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.3f)

    Box(modifier = modifier
            .size(120.dp, 48.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(backgroundBrush, shape)
            .clip(shape)
            .clickable(enabled) {
                if (inConfirmatoryState) {
                    inConfirmatoryState = false
                    onConfirm()
                } else inConfirmatoryState = true
            }.padding(end = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        val text = stringResource(
            if (inConfirmatoryState) R.string.checkout_confirm_description
            else                     R.string.checkout_description)
        Text(text, fontSize = 15.sp)
    }

    if (inConfirmatoryState)
        LaunchedEffect(Unit) {
            delay(timeOutMillis)
            inConfirmatoryState = false
        }
}

@Preview @Composable fun CheckoutButtonPreview(

) {
    var enabled by remember { mutableStateOf(true) }
    Row(Modifier.padding(8.dp),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterVertically
    ) {
        CheckoutButton(
            enabled = enabled,
            backgroundBrush = remember {
                Brush.horizontalGradient(listOf(Color.Red, Color.Yellow))
            }, onConfirm = {})
        Button(onClick = { enabled = !enabled }) {
            Text(if (enabled) "Disable" else "Enable")
        }
    }
}


