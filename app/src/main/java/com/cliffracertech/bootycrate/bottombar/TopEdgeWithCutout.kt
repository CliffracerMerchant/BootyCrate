/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos

private const val angleDown = 90f
private const val angleLeft = 180f
private const val angleUp = 270f

fun Path.arcTo(
    centerX: Float, centerY: Float,
    radius: Float,
    startAngle: Float, sweepAngle: Float,
) = asAndroidPath().arcTo(
    centerX - radius, centerY - radius,
    centerX + radius, centerY + radius,
    startAngle, sweepAngle, false)

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

    val bottomWidth get() = rightCurveStartX - leftCurveEndX
    val topCurveLength get() = (interpedTopRadius * theta).toFloat()
    val bottomCurveLength get() = (interpedBotRadius * theta).toFloat()

    var leftCurveStartX = 0f
        private set
    var leftCurveInflectionX = 0f
        private set
    var leftCurveEndX = 0f
        private set
    var rightCurveStartX = 0f
        private set
    var rightCurveInflectionX = 0f
        private set
    var rightCurveEndX = 0f
        private set
    var theta = 0.0
        private set
    var interpedTopRadius = 0f
        private set
    var interpedBotRadius = 0f
        private set

    fun addTo(path: Path, canvasWidth: Float) = path.apply {
        val interp = interpolationProvider()
        val width = widthProvider()
        val widthPx = with (density) { width.toPx() }

        // If the interpolation is sufficiently small, we can just draw a straight line instead.
        if (interp < 0.01f) {
            leftCurveStartX = widthPx / 2f
            leftCurveInflectionX = leftCurveStartX
            leftCurveEndX = leftCurveStartX
            rightCurveStartX = leftCurveStartX
            rightCurveInflectionX = leftCurveStartX
            rightCurveEndX = leftCurveStartX
            return@apply
        }

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
        interpedTopRadius = topCornerRadiusPx / interp
        interpedBotRadius = bottomCornerRadiusPx / interp

        // The ϴ calculation is derived from the basic trig equation y = r * sinϴ,
        // Since we are measuring theta starting from the up position, and measuring
        // ϴ clockwise instead of counterclockwise, the equation for our use case is
        // y = r - r * sin(π/2 - ϴ)
        //   = r * (1 - sin(π/2 - ϴ))
        // Solving this for ϴ gives:
        // ϴ = π/2 - arcsin(1 - y/r)
        theta = Math.PI / 2 - asin(1 - topYDistance / interpedTopRadius)
        val thetaDegrees = Math.toDegrees(theta).toFloat()

        val unscaledXDistance = cos(Math.PI / 2 - theta)
        val topXDistance = (interpedTopRadius * unscaledXDistance).toFloat()
        val botXDistance = (interpedBotRadius * unscaledXDistance).toFloat()

        leftCurveStartX = start - topXDistance
        leftCurveInflectionX = start
        leftCurveEndX = start + botXDistance

        rightCurveStartX = end - botXDistance
        rightCurveInflectionX = end
        rightCurveEndX = end + topXDistance

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

data class TopEdgeWithCutout(
    val density: Density,
    val cutout: TopCutout,
    val indicator: Indicator,
    val topOuterCornerRadius: Dp,
) {
    val topOuterCornerRadiusPx = with (density) { topOuterCornerRadius.toPx() }
    private val barContentPositions = mutableStateMapOf<Any, Rect>()
    fun resetElementPositions() = barContentPositions.clear()
    fun updateElementPosition(id: Any, coords: LayoutCoordinates) {
        val rect = Rect(coords.positionInParent(),
                        coords.size.toSize())
        barContentPositions.set(id, rect)
    }

    fun addTopEdgeTo(path: Path, canvasWidth: Float) {
        path.arcTo(
            centerX = cutout.topCornerRadiusPx,
            centerY = cutout.topCornerRadiusPx,
            radius = cutout.topCornerRadiusPx,
            startAngle = angleLeft,
            sweepAngle = 90f)
        cutout.addTo(path, canvasWidth)
        path.arcTo(
            centerX = canvasWidth - cutout.topCornerRadiusPx,
            centerY = cutout.topCornerRadiusPx,
            radius = cutout.topCornerRadiusPx,
            startAngle = angleUp,
            sweepAngle = 90f)
    }

    fun drawIndicator(
        scope: DrawScope,
        topEdgePath: Path,
        startDistance: Float,
    ) = indicator.draw(scope, topEdgePath, startDistance)

    /** Find the distance along the top edge path that will make the indicator be
     * centered above the target returned by [Indicator.targetProvider]. */
    fun findIndicatorStartDistance(canvasWidth: Float): Float {
        val target = indicator.targetProvider()
        val targetRect = barContentPositions.get(target) ?: Rect.Zero
        val x = targetRect.center.x - indicator.widthPx / 2f
        var distance = 0f

        if (x < topOuterCornerRadiusPx) {
            val theta = acos(1.0 - x / topOuterCornerRadiusPx)
            return topOuterCornerRadiusPx * theta.toFloat()
        } else distance += topOuterCornerRadiusPx * (Math.PI / 2).toFloat()

        if (x < cutout.leftCurveStartX)
            return distance + (x - topOuterCornerRadiusPx)
        else distance += (cutout.leftCurveStartX - topOuterCornerRadiusPx)

        if (x < cutout.leftCurveInflectionX) {
            val theta = Math.PI / 2 - acos((x - cutout.leftCurveStartX) / cutout.interpedTopRadius)
            return distance + (cutout.interpedTopRadius * theta).toFloat()
        } else distance += cutout.topCurveLength

        if (x < cutout.leftCurveEndX) {
            val theta = acos(1.0 - (x - cutout.leftCurveInflectionX) / cutout.interpedBotRadius)
            return distance + (cutout.interpedBotRadius * theta).toFloat()
        } else distance += cutout.bottomCurveLength

        if (x < cutout.rightCurveStartX)
            return distance + (x - cutout.leftCurveEndX)
        else distance += abs(cutout.bottomWidth)
        // cradle.bottomWidth can sometimes be negative if the bottom
        // corner radius is larger than half of the cradle.width

        if (x < cutout.rightCurveInflectionX) {
            val theta = Math.PI / 2 - acos((x - cutout.rightCurveStartX) / cutout.interpedBotRadius)
            return distance + (cutout.interpedBotRadius * theta).toFloat()
        } else distance += cutout.bottomCurveLength

        if (x < cutout.rightCurveEndX) {
            val theta = (Math.PI / 2 - acos((x - cutout.rightCurveInflectionX) / cutout.interpedTopRadius))
            return distance + (cutout.interpedTopRadius * theta).toFloat()
        } else distance += cutout.topCurveLength

        val topRightCornerStartX = canvasWidth - topOuterCornerRadiusPx
        if (x < topRightCornerStartX)
            return distance + (x - cutout.rightCurveEndX)
        else distance += (canvasWidth - topOuterCornerRadiusPx) - x

        if (x < canvasWidth) {
            val theta = Math.PI / 2 - acos((x - topRightCornerStartX) / topOuterCornerRadiusPx)
            return topOuterCornerRadiusPx * theta.toFloat()
        } else distance += topOuterCornerRadiusPx * (Math.PI / 2).toFloat()

        return distance
    }

    data class Indicator(
        val density: Density,
        val width: Dp,
        val thickness: Dp,
        val color: Color,
        val targetProvider: () -> Any?
    ) {
        val widthPx = with (density) { width.toPx() }

        private val path = Path()
        private val pathMeasure = PathMeasure()
        private val pathStroke = Stroke(
            width = with (density) { thickness.toPx() },
            cap = StrokeCap.Round)

        fun draw(
            scope: DrawScope,
            topEdgePath: Path,
            startDistanceAlongEdge: Float
        ) {
            pathMeasure.setPath(topEdgePath, forceClosed = false)
            pathMeasure.getSegment(
                startDistanceAlongEdge,
                startDistanceAlongEdge + widthPx,
                destination = path,
                startWithMoveTo = true)
            scope.drawPath(path, color, style = pathStroke)
        }
    }
}