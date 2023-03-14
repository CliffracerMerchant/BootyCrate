/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.scaleMatrix
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.springStiffness
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme
import com.cliffracertech.bootycrate.utils.clippedGradientBackground
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val checkoutButtonWidthDp = 120
private const val checkoutButtonHeightDp = 48
private const val checkoutAddButtonOverlapDp = 14
private const val addButtonSizeDp = 56
private const val addButtonTranslationXDp =
    -0.5f * (checkoutButtonWidthDp - checkoutAddButtonOverlapDp)

private fun checkoutButtonShape(density: Density) =
    GenericShape { size, _ ->
        val pathData = "M108,0 h-88 A 20 20 0 0 0 0,20 A 28 28 0 0 0 28,48 h90 A 32,32 0 0 1 108,0 Z"
        PathParser().parsePathString(pathData).toPath(this)
        // The coordinates used in the path data are intended to be in dp, so
        // they need to be scaled by the density to be converted to pixel sizes
        val matrix = scaleMatrix(density.density, density.density)
        asAndroidPath().transform(matrix)
    }

/** A button with a custom shape used to checkout.
 *
 * CheckoutButton will move to a confirmatory state, indicated by a change in
 * text, when it is pressed. If the button is pressed again before [timeOutMillis]
 * elapses, then [onConfirm] will be invoked. Otherwise, the button will drop
 * back out of its confirmatory state. The provided [backgroundBrush] will be
 * used inside the button's custom shape. */
@Composable fun CheckoutButton(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    backgroundBrush: Brush,
    timeOutMillis: Long = 2000L,
    onConfirm: () -> Unit,
) {
    val density = LocalDensity.current
    val shape = remember { checkoutButtonShape(density) }
    // The value of enabled is used as a key for inConfirmatoryState so
    // that the button will immediately drop out of its confirmatory
    // state when the button becomes disabled
    var inConfirmatoryState by remember(enabled) {
        mutableStateOf(false)
    }
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f)

    Box(modifier = modifier
        .size(checkoutButtonWidthDp.dp, checkoutButtonHeightDp.dp)
        .graphicsLayer { this.alpha = alpha }
        .background(backgroundBrush, shape)
        .clip(shape)
        .clickable(enabled) {
            if (inConfirmatoryState) {
                inConfirmatoryState = false
                onConfirm()
            } else inConfirmatoryState = true
        }.padding(end = 16.dp),
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

@Preview @Composable fun CheckoutButtonPreview() {
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

/** A circular button that appears similarly to a floating action button with
 * an add icon. When the width and colors of a background horizontal gradient
 * are provided via [backgroundGradientWidth] and [backgroundGradientColors],
 * the button will update its background brush so that it matches the colors
 * of the background horizontal gradient. */
@Composable fun AddButton(
    modifier: Modifier = Modifier,
    backgroundGradientWidth: Dp,
    backgroundGradientColors: ImmutableList<Color> = listOf(
            MaterialTheme.colors.primary,
            MaterialTheme.colors.secondary
        ).toImmutableList(),
    onClick: () -> Unit,
) {
    Box(modifier = modifier
            .size(56.dp).clip(CircleShape)
            .clippedGradientBackground(
                Orientation.Horizontal,
                CircleShape,
                backgroundGradientWidth,
                backgroundGradientColors)
            .clickable(
                onClickLabel = stringResource(
                    R.string.add_button_description),
                role = Role.Button,
                onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, stringResource(R.string.add_button_description))
    }
}

@Preview @Composable fun AddButtonPreview() = BootyCrateTheme {
    var xOffset by remember { mutableStateOf(0f) }
    val buttonWidth = with (LocalDensity.current) { 56.dp.toPx() }
    val width = with (LocalDensity.current) { 1080.toDp() }

    Column(Modifier.width(width), Arrangement.spacedBy(6.dp)) {
        AddButton(
            backgroundGradientWidth = width,
            modifier = Modifier.offset {
                IntOffset(xOffset.roundToInt(), 0)
            }, onClick = {})
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

/** Return the [Dp] width of a [CutoutContent], depending on
 * whether or not the checkout button is currently visible. */
fun cutoutContentWidth(showingCheckoutButton: Boolean) =
    if (!showingCheckoutButton) addButtonSizeDp.dp
    // Checkout button width + add button width + negative margin between them
    else checkoutButtonWidthDp.dp + addButtonSizeDp.dp - checkoutAddButtonOverlapDp.dp

/**
 * The content of the [BootyCrateBottomAppBar]'s top cutout.
 *
 * @param modifier The [Modifier] to use for the content
 * @param backgroundGradientWidth The width of the bottom app bar's background gradient
 * @param backgroundGradientColors The colors used for the bottom app bar's background gradient
 * @param checkoutButtonIsVisible Whether or not the checkout button should be visible or hidden
 * @param checkoutButtonIsEnabled Whether or not the checkout button should be enabled, if it is not hidden
 * @param checkoutButtonTimeOutMillis The timeout value for the inner [CheckoutButton]
 * @param onCheckoutConfirm The callback that will be invoked when the
 *     inner [CheckoutButton] is tapped again within it timeout period
 * @param onAddButtonClick The callback that will be invoked when the add button is clicked
 * @param interpolationProvider A method that will return the interpolation
 *     value, in the range [0f, 1f], to use for the cutout content. A returned
 *     value of 0f will make the content disappear, while a value of 1f will
 *     make it normally visible.
 */
@Composable fun CutoutContent(
    modifier: Modifier = Modifier,
    backgroundGradientWidth: Dp,
    backgroundGradientColors: ImmutableList<Color> = listOf(
            MaterialTheme.colors.primary,
            MaterialTheme.colors.secondary
        ).toImmutableList(),
    checkoutButtonIsVisible: Boolean,
    checkoutButtonIsEnabled: Boolean,
    checkoutButtonTimeOutMillis: Long = 2000L,
    onCheckoutConfirm: () -> Unit,
    onAddButtonClick: () -> Unit,
    interpolationProvider: () -> Float,
) {
    val density = LocalDensity.current
    var coords by remember { mutableStateOf(Rect.Zero) }
    val checkoutButtonBrush = remember(
        coords, backgroundGradientWidth, backgroundGradientColors
    ) {
        val width = with (density) { backgroundGradientWidth.toPx() }
        Brush.horizontalGradient(
            colors = backgroundGradientColors,
            startX = -coords.left,
            endX = width - coords.left)
    }

    Row(modifier = modifier
            .onPlaced {
                coords = Rect(it.positionInParent(), it.size.toSize())
            }.graphicsLayer {
                val interp = interpolationProvider()
                alpha = interp
                scaleX = 0.8f + 0.2f * interp
                scaleY = 0.8f + 0.2f * interp
                translationY = 48.dp.toPx() * (interp - 1f)
            },
        horizontalArrangement = Arrangement.spacedBy((-14).dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val checkoutButtonAppearanceProgress by animateFloatAsState(
            targetValue = if (checkoutButtonIsVisible) 1f else 0f,
            animationSpec = spring(stiffness = springStiffness))

        if (checkoutButtonAppearanceProgress > 0f)
            CheckoutButton(
                enabled = checkoutButtonIsEnabled,
                modifier = Modifier.graphicsLayer {

                    alpha = checkoutButtonAppearanceProgress
                }, backgroundBrush = checkoutButtonBrush,
                timeOutMillis = checkoutButtonTimeOutMillis,
                onConfirm = onCheckoutConfirm)

        AddButton(
            modifier = Modifier.graphicsLayer {
                translationX = if (checkoutButtonAppearanceProgress == 0f) 0f
                               else addButtonTranslationXDp.dp.toPx() *
                                   (1f - checkoutButtonAppearanceProgress)
            }, backgroundGradientWidth = backgroundGradientWidth,
            backgroundGradientColors = backgroundGradientColors,
            onClick = onAddButtonClick)
    }
}