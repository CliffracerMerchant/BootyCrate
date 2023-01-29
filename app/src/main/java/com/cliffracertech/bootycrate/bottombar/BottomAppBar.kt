/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt

fun Path.arcTo(
    centerX: Float, centerY: Float,
    radius: Float,
    startAngle: Float, sweepAngle: Float,
) = asAndroidPath().arcTo(
    centerX - radius, centerY - radius,
    centerX + radius, centerY + radius,
    startAngle, sweepAngle, false)

private const val angleDown = 90f
private const val angleLeft = 180f
private const val angleUp = 270f

data class TopCutout(
    val density: Density,
    val depth: Dp,
    val contentHeight: Dp,
    val topCornerRadius: Dp,
    val bottomCornerRadius: Dp,
    val margin: Dp,
    val widthProvider: () -> Dp,
    val interpolationProvider: () -> Float,
    val contents: @Composable () -> Unit,
) {
    val depthPx = with (density) { depth.toPx() }
    val contentHeightPx = with (density) { contentHeight.toPx() }
    val marginPx = with (density) { margin.toPx() }
    val topCornerRadiusPx = with (density) { topCornerRadius.toPx() }
    val bottomCornerRadiusPx = with (density) { bottomCornerRadius.toPx() }
    val contentVerticalOverflow = contentHeight + margin - depth
    val contentVerticalOverflowPx = contentHeightPx + marginPx - depthPx

    fun addTo(path: Path, canvasWidth: Float) = path.apply {
        val interp = interpolationProvider()
        val width = widthProvider()
        val widthPx = with (density) { width.toPx() }

        // If the interpolation is sufficiently small, we can just draw a straight line instead.
        if (interp < 0.01f)
            return@apply

        val fullWidth = widthPx + 2 * marginPx
        // The cradle width is interpolated down to 90% of its full width
        val interpedWidth = fullWidth * (0.9f + 0.1f * interp)

        // start will be the x coordinate of the start of the cradle if topCornerRadius is zero
        val centerX = canvasWidth / 2f
        val start = centerX - interpedWidth * 0.5f
        val end = start + interpedWidth

        // yDistance is the y distance covered by both the top and bottom curves together.
        val yDistance = depthPx * interp
        val topRadiusFraction = topCornerRadiusPx / (topCornerRadiusPx + bottomCornerRadiusPx)
        val topYDistance = yDistance * topRadiusFraction

        // The top and bottom radius values are divided by the interpolation value so that
        // they increase to infinity as the interpolation approaches zero. This gives the
        // appearance of circular arcs that flatten as interpolation approaches zero.
        val interpedTopRadius = topCornerRadiusPx / interp
        val interpedBotRadius = bottomCornerRadiusPx / interp

        // The ϴ calculation is derived from the basic trig equation y = r * sinϴ,
        // Since we are measuring theta starting from the up position, and measuring
        // ϴ clockwise instead of counterclockwise, the equation for our use case is
        // y = r - r * sin(π/2 - ϴ)
        //   = r * (1 - sin(π/2 - ϴ))
        // Solving this for ϴ gives:
        // ϴ = π/2 - arcsin(1 - y/r)
        val theta = Math.PI / 2 - asin(1 - topYDistance / interpedTopRadius)
        val thetaDegrees = Math.toDegrees(theta).toFloat()

        val unscaledXDistance = cos(Math.PI / 2 - theta)
        val topXDistance = (interpedTopRadius * unscaledXDistance).toFloat()
        val botXDistance = (interpedBotRadius * unscaledXDistance).toFloat()

        lineTo(start - topCornerRadiusPx, 0f)
        arcTo(centerX = start - topXDistance,
              centerY = interpedTopRadius,
              radius = interpedTopRadius,
              startAngle = angleUp,
              sweepAngle = thetaDegrees)
        arcTo(centerX = start + botXDistance,
              centerY = yDistance - interpedBotRadius,
              radius = interpedBotRadius,
              startAngle = angleDown + thetaDegrees,
              sweepAngle = -thetaDegrees)
        arcTo(centerX = end - botXDistance,
              centerY = yDistance - interpedBotRadius,
              radius = interpedBotRadius,
              startAngle = angleDown,
              sweepAngle = -thetaDegrees)
        arcTo(centerX = end + topXDistance,
              centerY = interpedTopRadius,
              radius = interpedTopRadius,
              startAngle = angleUp - thetaDegrees,
              sweepAngle = thetaDegrees)
    }
}

enum class CutoutBottomAppBarLayout { BarContent, CutoutContent }
private val path = Path()

@Composable fun CutoutBottomAppBar(
    modifier: Modifier = Modifier,
    backgroundBrush: Brush,
    cutout: TopCutout,
    barContents: @Composable (cutoutWidth: Dp) -> Unit,
) {
    val density = LocalDensity.current
    SubcomposeLayout(
        modifier.drawBehind {
            path.reset()
            path.arcTo(
                centerX = cutout.topCornerRadiusPx,
                centerY = cutout.topCornerRadiusPx,
                radius = cutout.topCornerRadiusPx,
                startAngle = angleLeft,
                sweepAngle = 90f)
            cutout.addTo(path, size.width)
            path.arcTo(
                centerX = size.width - cutout.topCornerRadiusPx,
                centerY = cutout.topCornerRadiusPx,
                radius = cutout.topCornerRadiusPx,
                startAngle = angleUp,
                sweepAngle = 90f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.close()
            drawPath(path, backgroundBrush)
        }
    ) { constraints ->
        val cutoutContents = subcompose(CutoutBottomAppBarLayout.CutoutContent) {
            cutout.contents()
        }.first().measure(constraints)

        val barContents = subcompose(CutoutBottomAppBarLayout.BarContent) {
            val cutoutWidthDp = with (density) { cutoutContents.width.toDp() }
            barContents(cutoutWidthDp)
        }.first().measure(constraints)

        val cutoutOffset = IntOffset(
            x = (constraints.maxWidth - cutoutContents.width) / 2,
            y = -cutout.contentVerticalOverflowPx.roundToInt())

        layout(constraints.maxWidth, constraints.maxHeight) {
            cutoutContents.place(cutoutOffset)
            barContents.place(IntOffset.Zero)
        }
    }
}

@Preview @Composable fun BottomAppBarPreview() {
    val primaryColor = MaterialTheme.colors.primary
    val secondaryColor = MaterialTheme.colors.secondary
    val brush = remember { Brush.horizontalGradient(listOf(primaryColor, secondaryColor)) }
    val density = LocalDensity.current

    var cutoutHidden by remember { mutableStateOf(false) }
    val interpolation by animateFloatAsState(
        targetValue = if (cutoutHidden) 0f else 1f,
        animationSpec = spring(stiffness = 10f))

    var button2visible by remember { mutableStateOf(true) }
    val button2alpha by animateFloatAsState(
        targetValue = if (button2visible) 1f else 0f,
        animationSpec = spring(stiffness = 10f))
    val cutoutWidth by animateDpAsState(
        targetValue = if (!button2visible) 56.dp
                      else 56.dp + 56.dp,
        animationSpec = spring(stiffness = 10f))

    val cutout = remember { TopCutout(
        density = density,
        depth = 51.5f.dp,
        contentHeight = 56.dp,
        topCornerRadius = 25.dp,
        bottomCornerRadius = 33.dp,
        margin = 6.dp,
        widthProvider = { cutoutWidth },
        interpolationProvider = { interpolation },
        contents = {
            Row(modifier = Modifier.graphicsLayer {
                    alpha = interpolation
                    scaleX = 0.8f + 0.2f * interpolation
                    scaleY = 0.8f + 0.2f * interpolation
                    translationY = 48.dp.toPx() * (interpolation - 1f)
                }, horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = 30.dp.toPx() * (1f - button2alpha)
                        }.zIndex(1f)
                ) { Text("1") }
                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier.graphicsLayer {
                        translationX = -30.dp.toPx() * (1f - button2alpha)
                    }
                ) { Text("2") }
            }
        }
    )}

    Box(Modifier.height(56.dp + cutout.contentVerticalOverflow)) {
        CutoutBottomAppBar(
            modifier = Modifier.height(56.dp).align(Alignment.BottomStart),
            backgroundBrush = brush,
            cutout = cutout,
        ) {
            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { cutoutHidden = !cutoutHidden },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = MaterialTheme.colors.secondary),
                ) { Text("interp") }
                TextButton(
                    onClick = { button2visible = !button2visible },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = MaterialTheme.colors.primary),
                ) { Text("content") }
            }
        }
    }

}