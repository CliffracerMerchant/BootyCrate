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
    val contentMarginPx = contentMargin.toPx(density)
    val topCornerRadiusPx = topCornerRadius.toPx(density)
    val bottomCornerRadiusPx = bottomCornerRadius.toPx(density)
    val contentVerticalOverflow = contentHeight + contentMargin - depth
    val contentVerticalOverflowPx = contentHeight.toPx(density) + contentMarginPx - depthPx

    val topRadiusFraction = topCornerRadiusPx / (topCornerRadiusPx + bottomCornerRadiusPx)

    fun addTo(path: Path, edgeWidth: Float) = path.apply {
        val interp = interpolationProvider()
        val width = widthProvider()
        val widthPx = width.toPx(density)

        // If the interpolation is sufficiently small, we can just draw a straight line instead.
        if (interp < 0.01f) return@apply

        val fullWidth = widthPx + 2 * contentMarginPx
        // The cutout width is interpolated between its full width and 90% of its full width
        val interpedWidth = fullWidth * (0.9f + 0.1f * interp)

        // start will be the x coordinate of the start of the cutout if topCornerRadius is zero
        val start = (edgeWidth - interpedWidth) / 2f
        val end = start + interpedWidth

        // yDistance is the y distance covered by both the top and bottom curves together.
        val yDistance = depthPx * interp
        val topYDistance = yDistance * topRadiusFraction

        // The top and bottom radius values are divided by the interpolation value so that
        // they increase to infinity as the interpolation approaches zero. This gives the
        // appearance of circular arcs that flatten as interpolation approaches zero.
        val interpedTopRadius = topCornerRadiusPx / interp
        val interpedBottomRadius = bottomCornerRadiusPx / interp

        // The ϴ calculation is derived from the basic trig equation y = r * sinϴ,
        // Since we are measuring theta starting from the up position, and measuring
        // ϴ clockwise instead of counterclockwise, the equation for our use case is
        // y = r - r * sin(π/2 - ϴ)
        // y = r * (1 - sin(π/2 - ϴ))
        // Solving this for ϴ gives:
        // ϴ = π/2 - arcsin(1 - y/r)
        val sweepAngle = Math.PI / 2 - asin(1 - topYDistance / interpedTopRadius)
        val sweepAngleDegrees = Math.toDegrees(sweepAngle).toFloat()

        val unscaledXDistance = cos(Math.PI / 2 - sweepAngle)
        val topXDistance = (interpedTopRadius * unscaledXDistance).toFloat()
        val botXDistance = (interpedBottomRadius * unscaledXDistance).toFloat()

        lineTo(start - topCornerRadiusPx, 0f)
        arcTo(centerX = start - topXDistance,
            centerY = interpedTopRadius,
            radius = interpedTopRadius,
            startAngle = angleUp,
            sweepAngle = sweepAngleDegrees)
        arcTo(centerX = start + botXDistance,
            centerY = yDistance - interpedBottomRadius,
            radius = interpedBottomRadius,
            startAngle = angleDown + sweepAngleDegrees,
            sweepAngle = -sweepAngleDegrees)
        arcTo(centerX = end - botXDistance,
            centerY = yDistance - interpedBottomRadius,
            radius = interpedBottomRadius,
            startAngle = angleDown,
            sweepAngle = -sweepAngleDegrees)
        arcTo(centerX = end + topXDistance,
            centerY = interpedTopRadius,
            radius = interpedTopRadius,
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
 * draw the indicator above a given element inside the shape can be found using
 * the method [indicatorStartLengthFor].
 *
 * @param density The local [Density] instance
 * @param width The overall width that the top edge will take up
 * @param initialCutoutWidth The width that should be used for the cutout in
 *     calls to [indicatorStartLengthFor]. This should be equal to the value
 *     returned by the [Cutout.widthProvider] whenever the target element is
 *     to the right of the cutout. See [indicatorStartLengthFor] for
 *     additional explanation.
 * @param cutout A [TopCutout] instance that describes the top edge's cutout
 * @param indicator An [Indicator] instance that describes the top edge's indicator
 * @param topOuterCornerRadius The radius, in [Dp], of the outer corners of the top edge
 */
data class TopEdgeWithCutout(
    val density: Density,
    val width: Dp,
    val initialCutoutWidth: Dp,
    val cutout: TopCutout,
    val indicator: Indicator,
    val topOuterCornerRadius: Dp,
) {
    private val widthPx = width.toPx(density)
    private val barContentPositions = mutableStateMapOf<Any, Rect>()

    /** Record the [coordinates] of an element inside the shape that is
     * using the [TopEdgeWithCutout] for its top edge. [id] will be used
     * as a key to identify each element. This is used in conjunction
     * with [indicatorStartLengthFor]. */
    fun updateElementPosition(id: Any, coordinates: LayoutCoordinates) {
        val rect = Rect(coordinates.positionInParent(),
                        coordinates.size.toSize())
        barContentPositions[id] = rect
    }

    /** Add the top edge to the provided [path]. */
    fun addTo(path: Path) {
        path.arcTo(
            centerX = cutout.topCornerRadiusPx,
            centerY = cutout.topCornerRadiusPx,
            radius = cutout.topCornerRadiusPx,
            startAngle = angleLeft,
            sweepAngle = 90f)
        cutout.addTo(path, widthPx)
        path.arcTo(
            centerX = widthPx - cutout.topCornerRadiusPx,
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
         *
         * If [topEdgePath] is null, the indicator will be drawn at its most
         * recently calculated position. This can be utilized to temporarily
         * freeze redraws, e.g. when the cutout's shape is changing and it is
         * undesired for the indicator to shift in position during the change.
         */
        fun draw(
            scope: DrawScope,
            alpha: Float,
            edgeStartLength: Float,
            topEdgePath: Path?,
        ) {
            if (topEdgePath != null) {
                pathMeasure.setPath(topEdgePath, forceClosed = false)
                path.reset()
                pathMeasure.getSegment(
                    edgeStartLength,
                    edgeStartLength + widthPx,
                    destination = path,
                    startWithMoveTo = true)
            }
            scope.drawPath(path, color, alpha, style = pathStroke)
        }
    }

    private val topOuterCornerRadiusPx = topOuterCornerRadius.toPx(density)

    /** The x position of the start of the top left curve */
    private val leftCurveStartX: Float
    /** The x position of the inflection point between the top left and bottom left curves */
    private val leftCurveInflectionX: Float
    /** The x position of the end of the bottom left curve */
    private val leftCurveEndX: Float
    /** The x position of the start of the bottom right curve */
    private val rightCurveStartX: Float
    /** The x position of the inflection point between the top right and bottom right curves */
    private val rightCurveInflectionX: Float
    /** The x position of the end of the top right curve */
    private val rightCurveEndX: Float
    /** The sweep angle of each part of the curves */
    private val sweepAngle: Double

    init {
        // These calculations match those inside TopCutout, except that
        // they assume a constant cutout width and an interpolation of 1f.
        val cutoutWidthPx = initialCutoutWidth.toPx(density)

        val fullWidth = cutoutWidthPx + 2 * cutout.contentMarginPx
        leftCurveInflectionX = (widthPx - fullWidth) / 2f
        rightCurveInflectionX = leftCurveInflectionX + fullWidth

        val topYDistance = cutout.depthPx * cutout.topRadiusFraction
        val sweepAngle = Math.PI / 2 - asin(1 - topYDistance / cutout.topCornerRadiusPx)

        val unscaledXDistance = cos(Math.PI / 2 - sweepAngle)
        val topXDistance = (cutout.topCornerRadiusPx * unscaledXDistance).toFloat()
        val botXDistance = (cutout.bottomCornerRadiusPx * unscaledXDistance).toFloat()

        this.sweepAngle = sweepAngle
        leftCurveStartX = leftCurveInflectionX - topXDistance
        leftCurveEndX = leftCurveInflectionX + botXDistance
        rightCurveStartX = rightCurveInflectionX - botXDistance
        rightCurveEndX = rightCurveInflectionX + topXDistance
    }

    /** The width of the horizontal line at the bottom of the cutout. */
    // bottomWidth would actually be negative if the bottom corner radius is
    // larger than half of the width, so it needs to be coerced to at least 0f
    private val bottomWidth get() = (rightCurveStartX - leftCurveEndX).coerceAtLeast(0f)
    /** The arc lengths of the top left and top right curves */
    private val topCurveLength = (cutout.topCornerRadiusPx * sweepAngle).toFloat()
    /** The arc lengths of the bottom left and bottom right curves  */
    private val bottomCurveLength = (cutout.bottomCornerRadiusPx * sweepAngle).toFloat()

    /**
     * Return the length along the top edge path that will make the indicator
     * be centered above the [targetElement]. This value can be used with an
     * [Indicator.draw] call. There are two restrictions to take note of:
     *
     * - The returned value will only be correct if the target element's
     * position was previously recorded via [updateElementPosition].
     *
     * - TopEdgeWithCutout calculates and records values used in this
     * calculation during construction using the provided [initialCutoutWidth]
     * value as the cutout's width to simplify the process. If the cutout
     * width changes, then the provided [initialCutoutWidth] should be equal
     * to whatever the cutout's width will be when any [targetElement]s are
     * located on the right side of the cutout.
     */
    fun indicatorStartLengthFor(targetElement: Any?): Float {
        val targetRect = barContentPositions[targetElement] ?: Rect.Zero
        val x = targetRect.center.x - indicator.widthPx / 2f
        var length = 0f

        if (x < topOuterCornerRadiusPx) {
            val theta = acos(1.0 - x / topOuterCornerRadiusPx)
            return topOuterCornerRadiusPx * theta.toFloat()
        } else length += topOuterCornerRadiusPx * (Math.PI / 2).toFloat()

        if (x < leftCurveStartX)
            return length + (x - topOuterCornerRadiusPx)
        else length += (leftCurveStartX - topOuterCornerRadiusPx)

        if (x < leftCurveInflectionX) {
            val theta = Math.PI / 2 - acos((x - leftCurveStartX) / cutout.topCornerRadiusPx)
            return length + (cutout.topCornerRadiusPx * theta).toFloat()
        } else length += topCurveLength

        if (x < leftCurveEndX) {
            val theta = acos(1.0 - (x - leftCurveInflectionX) / cutout.bottomCornerRadiusPx)
            return length + (cutout.bottomCornerRadiusPx * theta).toFloat()
        } else length += bottomCurveLength

        if (x < rightCurveStartX)
            return length + (x - leftCurveEndX)
        else length += bottomWidth

        if (x < rightCurveInflectionX) {
            val theta = Math.PI / 2 - acos((x - rightCurveStartX) / cutout.bottomCornerRadiusPx)
            return length + (cutout.bottomCornerRadiusPx * theta).toFloat()
        } else length += bottomCurveLength

        if (x < rightCurveEndX) {
            val theta = (Math.PI / 2 - acos((x - rightCurveInflectionX) / cutout.topCornerRadiusPx))
            return length + (cutout.topCornerRadiusPx * theta).toFloat()
        } else length += topCurveLength

        val topRightCornerStartX = widthPx - topOuterCornerRadiusPx
        if (x < topRightCornerStartX)
            return length + (x - rightCurveEndX)
        else length += (widthPx - topOuterCornerRadiusPx) - x

        if (x < widthPx) {
            val theta = Math.PI / 2 - acos((x - topRightCornerStartX) / topOuterCornerRadiusPx)
            return topOuterCornerRadiusPx * theta.toFloat()
        } else length += topOuterCornerRadiusPx * (Math.PI / 2).toFloat()

        return length
    }
}