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
import com.cliffracertech.bootycrate.bottombar.TopEdgeWithCutout.Indicator
import com.cliffracertech.bootycrate.utils.toPx
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


/**
 * A set of parameters describing the shape of a cutout of a top edge.
 *
 * @param density The local [Density] instance
 * @param depth The vertical depth of the cutout. This value should be less than
 *     the height of the shape whose top edge [TopCutout] is being applied to.
 * @param contentHeight The height of the content that will be placed inside
 *     the cutout. This can be greater than the [depth] minus [margin}, in
 *     which case the content will protrude above the top edge of the shape
 *     whose top edge [TopCutout] is being applied to.
 * @param contentMargin The margin between the cutout and its content
 * @param topCornerRadius The [Dp] radius of the top corners of the cutout
 * @param bottomCornerRadius The [Dp] radius of the bottom corners of the cutout
 * @param widthProvider A method that will return the desired width of the
 *     cutout. This value is provided in a function so that the width of
 *     the cutout can be animated.
 * @param interpolationProvider A method that will return the current
 *     interpolation value to use for the cutout. An interpolation value of
 *     0f will make the cutout flatten itself so that it disappears, while a
 *     value of 1f will draw the cutout at it's full height.
 * @param contents The contents that will appear inside the cutout
 */
data class TopCutout(
    val density: Density,
    val depth: Dp,
    val contentHeight: Dp,
    val contentMargin: Dp,
    val topCornerRadius: Dp,
    val bottomCornerRadius: Dp,
    val widthProvider: () -> Dp,
    val interpolationProvider: () -> Float,
    val contents: @Composable () -> Unit,
) {
    val depthPx = depth.toPx(density)
    val contentHeightPx = contentHeight.toPx(density)
    val contentMarginPx = contentMargin.toPx(density)
    val topCornerRadiusPx = topCornerRadius.toPx(density)
    val bottomCornerRadiusPx = bottomCornerRadius.toPx(density)
    val contentVerticalOverflow = contentHeight + contentMargin - depth
    val contentVerticalOverflowPx = contentHeightPx + contentMarginPx - depthPx

    /** The x position of the start of the top left curve */
    var leftCurveStartX = 0f
        private set
    /** The x position of the inflection point between the top left and bottom left curves */
    var leftCurveInflectionX = 0f
        private set
    /** The x position of the end of the bottom left curve */
    var leftCurveEndX = 0f
        private set
    /** The x position of the start of the bottom right curve */
    var rightCurveStartX = 0f
        private set
    /** The x position of the inflection point between the top right and bottom right curves */
    var rightCurveInflectionX = 0f
        private set
    /** The x position of the end of the top right curve */
    var rightCurveEndX = 0f
        private set
    /** The sweep angle of each of the curves */
    var sweepAngle = 0.0
        private set
    /** The radius value used for the top curves. This will be equal to
     * [topCornerRadius] when [interpolationProvider] returns 1f, and
     * approach [Float.POSITIVE_INFINITY] as the return value approaches 0f*/
    var actualTopRadius = 0f
        private set
    /** The radius value used for the bottom curves. This will be equal to
     * [bottomCornerRadius] when [interpolationProvider] returns 1f, and
     * approach [Float.POSITIVE_INFINITY] as the return value approaches 0f*/
    var actualBottomRadius = 0f
        private set
    private val topRadiusFraction = topCornerRadiusPx / (topCornerRadiusPx + bottomCornerRadiusPx)

    /** The width of the horizontal line at the bottom of the
     * cutout. This can be zero if [bottomCornerRadius] is more
     * than half of the width returned by [widthProvider]. */
    // bottomWidth would actually be negative if the bottom corner radius is
    // larger than half of the width, so it needs to be coerced to at least 0f
    val bottomWidth get() = (rightCurveStartX - leftCurveEndX).coerceAtLeast(0f)
    /** The arc lengths of the top left and top right curves */
    val topCurveLength get() = (actualTopRadius * sweepAngle).toFloat()
    /** The arc lengths of the bottom left and bottom right curves  */
    val bottomCurveLength get() = (actualBottomRadius * sweepAngle).toFloat()

    fun addTo(path: Path, shapeWidth: Float) = path.apply {
        val interp = interpolationProvider()
        val width = widthProvider()
        val widthPx = width.toPx(density)

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

        val fullWidth = widthPx + 2 * contentMarginPx
        // The cutout width is interpolated between its full width and 90% of its full width
        val interpedWidth = fullWidth * (0.9f + 0.1f * interp)

        // start will be the x coordinate of the start of the cutout if topCornerRadius is zero
        val start = (shapeWidth - interpedWidth) / 2f
        val end = start + interpedWidth

        // yDistance is the y distance covered by both the top and bottom curves together.
        val yDistance = depthPx * interp
        val topYDistance = yDistance * topRadiusFraction

        // The top and bottom radius values are divided by the interpolation value so that
        // they increase to infinity as the interpolation approaches zero. This gives the
        // appearance of circular arcs that flatten as interpolation approaches zero.
        actualTopRadius = topCornerRadiusPx / interp
        actualBottomRadius = bottomCornerRadiusPx / interp

        // The ϴ calculation is derived from the basic trig equation y = r * sinϴ,
        // Since we are measuring theta starting from the up position, and measuring
        // ϴ clockwise instead of counterclockwise, the equation for our use case is
        // y = r - r * sin(π/2 - ϴ)
        // y = r * (1 - sin(π/2 - ϴ))
        // Solving this for ϴ gives:
        // ϴ = π/2 - arcsin(1 - y/r)
        sweepAngle = Math.PI / 2 - asin(1 - topYDistance / actualTopRadius)
        val sweepAngleDegrees = Math.toDegrees(sweepAngle).toFloat()

        val unscaledXDistance = cos(Math.PI / 2 - sweepAngle)
        val topXDistance = (actualTopRadius * unscaledXDistance).toFloat()
        val botXDistance = (actualBottomRadius * unscaledXDistance).toFloat()

        leftCurveStartX = start - topXDistance
        leftCurveInflectionX = start
        leftCurveEndX = start + botXDistance

        rightCurveStartX = end - botXDistance
        rightCurveInflectionX = end
        rightCurveEndX = end + topXDistance

        lineTo(start - topCornerRadiusPx, 0f)
        arcTo(centerX = start - topXDistance,
            centerY = actualTopRadius,
            radius = actualTopRadius,
            startAngle = angleUp,
            sweepAngle = sweepAngleDegrees)
        arcTo(centerX = start + botXDistance,
            centerY = yDistance - actualBottomRadius,
            radius = actualBottomRadius,
            startAngle = angleDown + sweepAngleDegrees,
            sweepAngle = -sweepAngleDegrees)
        arcTo(centerX = end - botXDistance,
            centerY = yDistance - actualBottomRadius,
            radius = actualBottomRadius,
            startAngle = angleDown,
            sweepAngle = -sweepAngleDegrees)
        arcTo(centerX = end + topXDistance,
            centerY = actualTopRadius,
            radius = actualTopRadius,
            startAngle = angleUp - sweepAngleDegrees,
            sweepAngle = sweepAngleDegrees)
    }
}

/**
 * A set of parameters describing the top edge of a shape that contains a cutout
 * and an indicator that slides along the top edge (including the cutout). The
 * method [addTo] can be used to add the top edge to a [Path] that describes a
 * shape. The [indicator]'s method [Indicator.draw] can be called directly to
 * draw it at a given length along the top edge. The start length needed to
 * draw the indicator above a given element inside the shape can be most easily
 * found using the method [findIndicatorStartLength].
 *
 * @param density A [Density] instance
 * @param cutout A [TopCutout] instance that describes the top edge's cutout
 * @param indicator An [Indicator] instance that describes the top edge's indicator
 * @param topOuterCornerRadius The radius, in [Dp], of the outer corners of the top edge
 */
data class TopEdgeWithCutout(
    val density: Density,
    val cutout: TopCutout,
    val indicator: Indicator,
    val topOuterCornerRadius: Dp,
) {
    val topOuterCornerRadiusPx = topOuterCornerRadius.toPx(density)
    private val barContentPositions = mutableStateMapOf<Any, Rect>()

    /** Record the [coordinates] of an element inside the shape that is
     * using the [TopEdgeWithCutout] for its top edge. [id] will be used
     * as a key to identify each element. This is used in conjunction
     * with [findIndicatorStartLength]. */
    fun updateElementPosition(id: Any, coordinates: LayoutCoordinates) {
        val rect = Rect(coordinates.positionInParent(),
                        coordinates.size.toSize())
        barContentPositions[id] = rect
    }

    /** Add the top edge to the provided [path], assuming a top edge width of [edgeWidth]. */
    fun addTo(path: Path, edgeWidth: Float) {
        path.arcTo(
            centerX = cutout.topCornerRadiusPx,
            centerY = cutout.topCornerRadiusPx,
            radius = cutout.topCornerRadiusPx,
            startAngle = angleLeft,
            sweepAngle = 90f)
        cutout.addTo(path, edgeWidth)
        path.arcTo(
            centerX = edgeWidth - cutout.topCornerRadiusPx,
            centerY = cutout.topCornerRadiusPx,
            radius = cutout.topCornerRadiusPx,
            startAngle = angleUp,
            sweepAngle = 90f)
    }

    /**
     * A set of parameters that describe a navigation indicator that hovers
     * above the current navigation destination's button on a bottom app bar
     * that has a custom top edge described with a [Path]. The indicator can
     * be drawn on the top edge path via the [draw] method.
     *
     * @param density A [Density] instance
     * @param width The width, in [Dp], of the indicator
     * @param thickness The thickness, in [Dp], of the indicator
     * @param color The [color] that the indicator will be drawn with
     */
    data class Indicator(
        val density: Density,
        val width: Dp,
        val thickness: Dp,
        val color: Color,
    ) {
        val widthPx = width.toPx(density)

        private val path = Path()
        private val pathMeasure = PathMeasure()
        private val pathStroke = Stroke(
            width = thickness.toPx(density),
            cap = StrokeCap.Round)

        /**
         * Draw the indicator on the provided [topEdgePath] inside [scope]. The
         * indicator will be drawn starting at [edgeStartLength], and ending at
         * [edgeStartLength] plus [width]. Note that [edgeStartLength] is the
         * length along [topEdgePath] at which the indicator will be drawn, not
         * the x position. This will need to be calculated for more complex
         * [topEdgePath]s.
         */
        fun draw(
            scope: DrawScope,
            topEdgePath: Path,
            alpha: Float,
            edgeStartLength: Float
        ) {
            pathMeasure.setPath(topEdgePath, forceClosed = false)
            path.reset()
            pathMeasure.getSegment(
                edgeStartLength,
                edgeStartLength + widthPx,
                destination = path,
                startWithMoveTo = true)
            scope.drawPath(path, color, alpha, style = pathStroke)
        }
    }

    /** Find the length along the top edge path that will make the indicator
     * be centered above the [targetElement]. This value can be used with an
     * [Indicator.draw] call. Note that the returned value will only be
     * correct if the inner elements' positions were previously recorded via
     * [updateElementPosition]. */
    fun findIndicatorStartLength(
        targetElement: Any?,
        shapeWidth: Float
    ): Float {
        val targetRect = barContentPositions[targetElement] ?: Rect.Zero
        val x = targetRect.center.x - indicator.widthPx / 2f
        var length = 0f

        if (x < topOuterCornerRadiusPx) {
            val theta = acos(1.0 - x / topOuterCornerRadiusPx)
            return topOuterCornerRadiusPx * theta.toFloat()
        } else length += topOuterCornerRadiusPx * (Math.PI / 2).toFloat()

        if (x < cutout.leftCurveStartX)
        return length + (x - topOuterCornerRadiusPx)
        else length += (cutout.leftCurveStartX - topOuterCornerRadiusPx)

        if (x < cutout.leftCurveInflectionX) {
            val theta = Math.PI / 2 - acos((x - cutout.leftCurveStartX) / cutout.actualTopRadius)
            return length + (cutout.actualTopRadius * theta).toFloat()
        } else length += cutout.topCurveLength

        if (x < cutout.leftCurveEndX) {
            val theta = acos(1.0 - (x - cutout.leftCurveInflectionX) / cutout.actualBottomRadius)
            return length + (cutout.actualBottomRadius * theta).toFloat()
        } else length += cutout.bottomCurveLength

        if (x < cutout.rightCurveStartX)
        return length + (x - cutout.leftCurveEndX)
        else length += cutout.bottomWidth

        if (x < cutout.rightCurveInflectionX) {
            val theta = Math.PI / 2 - acos((x - cutout.rightCurveStartX) / cutout.actualBottomRadius)
            return length + (cutout.actualBottomRadius * theta).toFloat()
        } else length += cutout.bottomCurveLength

        if (x < cutout.rightCurveEndX) {
            val theta = (Math.PI / 2 - acos((x - cutout.rightCurveInflectionX) / cutout.actualTopRadius))
            return length + (cutout.actualTopRadius * theta).toFloat()
        } else length += cutout.topCurveLength

        val topRightCornerStartX = shapeWidth - topOuterCornerRadiusPx
        if (x < topRightCornerStartX)
        return length + (x - cutout.rightCurveEndX)
        else length += (shapeWidth - topOuterCornerRadiusPx) - x

        if (x < shapeWidth) {
            val theta = Math.PI / 2 - acos((x - topRightCornerStartX) / topOuterCornerRadiusPx)
            return topOuterCornerRadiusPx * theta.toFloat()
        } else length += topOuterCornerRadiusPx * (Math.PI / 2).toFloat()

        return length
    }
}