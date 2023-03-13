/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cliffracertech.bootycrate.itemlist.minTouchTargetSize
import kotlin.math.roundToInt

private enum class BottomAppBarWithCutoutPart { BarContent, CutoutContent }
private val path = Path()

@Composable fun BottomAppBarWithCutout(
    modifier: Modifier = Modifier,
    contentAlphaProvider: () -> Float,
    backgroundBrush: Brush,
    topEdge: TopEdgeWithCutout,
    indicatorTarget: Any?,
    barContents: @Composable (onElementLayout: (Any, LayoutCoordinates) -> Unit) -> Unit,
) {
    var canvasWidth by remember { mutableStateOf(0f) }
    val indicatorStartLength by animateFloatAsState(
        targetValue = topEdge.findIndicatorStartLength(indicatorTarget, canvasWidth),
        animationSpec = spring(stiffness = Spring.StiffnessLow))

    SubcomposeLayout(modifier
        .fillMaxWidth()
        .drawBehind {
            path.reset()
            topEdge.addTo(path, size.width)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.close()
            drawPath(path, backgroundBrush)
        }.drawWithContent {
            drawContent()
            canvasWidth = size.width
            topEdge.indicator.draw(
                scope = this, topEdgePath = path,
                edgeStartLength = indicatorStartLength)
        }
    ) { constraints ->
        val cutoutContents = subcompose(BottomAppBarWithCutoutPart.CutoutContent) {
            topEdge.cutout.contents()
        }.firstOrNull()?.measure(constraints.copy(minWidth = 0))

        val barContentsPlaceable = subcompose(BottomAppBarWithCutoutPart.BarContent) {
            barContents(topEdge::updateElementPosition)
        }.firstOrNull()?.measure(constraints)

        val cutoutOffset = IntOffset(
            x = (constraints.maxWidth - (cutoutContents?.width ?: 0)) / 2,
            y = -topEdge.cutout.contentVerticalOverflowPx.roundToInt())

        layout(constraints.maxWidth, constraints.maxHeight) {
            cutoutContents?.place(cutoutOffset)
            barContentsPlaceable?.placeWithLayer(IntOffset.Zero) {
                alpha = contentAlphaProvider()
            }
        }
    }
}

@Preview @Composable fun BottomAppBarWithCutoutPreview() {
    val primaryColor = MaterialTheme.colors.primary
    val secondaryColor = MaterialTheme.colors.secondary
    val brush = remember { Brush.horizontalGradient(listOf(primaryColor, secondaryColor)) }
    val density = LocalDensity.current

    var cutoutHidden by remember { mutableStateOf(false) }
    val interpolation by animateFloatAsState(
        targetValue = if (cutoutHidden) 0f else 1f,
        animationSpec = spring(stiffness = 20f))

    val navDestination1key = "destination 1"
    val navDestination2key = "destination 2"
    var indicatorTarget by remember { mutableStateOf<Any?>(navDestination1key) }

    var button2visible = indicatorTarget == navDestination1key
    val button2alpha by animateFloatAsState(
        targetValue = if (button2visible) 1f else 0f,
        animationSpec = spring(stiffness = 20f))
    val cutoutWidth by animateDpAsState(
        targetValue = if (!button2visible) 56.dp
                      else 56.dp + 56.dp,
        animationSpec = spring(stiffness = 20f))

    val topEdgeWithCutout = remember {
        TopEdgeWithCutout(
            density = density,
            cutout = TopCutout(
                density = density,
                depth = 51.5f.dp,
                contentHeight = 56.dp,
                topCornerRadius = 25.dp,
                bottomCornerRadius = 33.dp,
                contentMargin = 6.dp,
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
                                }.zIndex(1f),
                            content = { Text("1") })
                        FloatingActionButton(
                            onClick = {},
                            modifier = Modifier.graphicsLayer {
                                translationX = -30.dp.toPx() * (1f - button2alpha)
                            }, content = { Text("2") })
                    }
                }
            ), indicator = TopEdgeWithCutout.Indicator(
                density = density,
                width = 48.dp,
                thickness = 8.dp,
                color = Color.Gray),
            topOuterCornerRadius = 25.dp)
    }
    val cutoutContentOverflow = topEdgeWithCutout.cutout.contentVerticalOverflow

    Column(
        modifier = Modifier.height(56.dp + cutoutContentOverflow + 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BottomAppBarWithCutout(
            modifier = Modifier
                .padding(top = cutoutContentOverflow)
                .height(56.dp),
            contentAlphaProvider = { interpolation },
            backgroundBrush = brush,
            topEdge = topEdgeWithCutout,
            indicatorTarget = indicatorTarget
        ) { onElementLayout ->
            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { indicatorTarget = navDestination1key },
                    modifier = Modifier.onPlaced {
                        onElementLayout(navDestination1key, it)
                    }, colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = MaterialTheme.colors.secondary),
                ) { Text("screen 1") }
                TextButton(
                    onClick = {
                        button2visible = !button2visible
                        indicatorTarget = navDestination2key
                    }, modifier = Modifier.onPlaced {
                        onElementLayout(navDestination2key, it)
                    }, colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = MaterialTheme.colors.primary),
                ) { Text("screen 2") }
            }
        }
        TextButton(
            onClick = { cutoutHidden = !cutoutHidden },
            modifier = Modifier.minTouchTargetSize().padding(top = 4.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
        ) { Text("toggle interpolation") }
    }

}