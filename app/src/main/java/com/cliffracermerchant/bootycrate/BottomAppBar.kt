/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.withClip
import com.google.android.material.shape.*
import kotlin.math.atan

/** A custom toolbar that has a cutout in its top edge to hold the contents of a layout.
 *
 *  BottomAppBar functions mostly as a regular GradientToolbar, except that its
 *  custom CradleTopEdgeTreatment used on its top edge gives it a cutout in its
 *  shape that can be used to hold the contents of a layout (probably a linear
 *  or constraint layout). The layout in question should be passed to the func-
 *  tion prepareCradleLayout during app startup so that BottomAppBar can set up
 *  its layout params.
 *
 *  BottomAppBar can also draw an indicator along its top edge, in case it is
 *  used in conjunction with a BottomNavigationView. The primary reason to use
 *  BottomAppBar's indicator over, for example, one inside a BottomNavigation-
 *  View subclass, is that BottomAppBar's indicator will always appear along
 *  its top edge, taking into account the cradle layout cutaway. Unfortunately,
 *  BottomAppBar has no way of knowing the exact on-screen positions of the
 *  menu items displayed in the BottomNavigationView, so the position must be
 *  manually set in pixels using the property indicatorXPos. The width (also in
 *  pixels) can be set via the XML attribute indicatorWidth, or at runtime using
 *  the property of the same name. The indicator color can be set using the XML
 *  property indicatorColor. The indicator can also have a paint shader applied
 *  as well using setIndicatorGradient.
 *
 *  XML attributes:
 *  - cradleAlignmentMode: CradleAlignmentMode = CradleAlignmentMode.Center:
 *        Where the cradle is drawn on the BottomAppBar.
 *  - cradleDepth: dimension = 0: The depth of the cradle
 *  - cradleTopCornerRadius: dimension = 0: The radius of the top corners of the cradle
 *  - cradleBottomCornerRadius: dimension = 0: The radius of the bottom corners of the cradle
 *  - cradleStartEndMargin: dimension = 90: The start or end margin of the cradle when the Cradle-
 *        AlignmentMode is Start or End. Does nothing when the CradleAlignmentMode is Center.
 *  - cradleContentsMargin: dimension = 0: The margin between the cradle and its nested layout
 *  - indicatorWidth: dimension = 0: The width of the indicator.
 *  - indicatorColor: color = 0: The color of the indicator. */
class BottomAppBar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {
    private val arcQuarter = 90f
    private val angleRight = 0f
    private val angleDown = 90f
    private val angleLeft = 180f
    private val angleUp = 270f

    enum class CradleAlignmentMode { Start, Center, End }
    val cradleAlignmentMode: CradleAlignmentMode
    var cradleWidth: Int = 0
        set(value) { field = value
                     materialShapeDrawable.invalidateSelf() }
    var cradleDepth: Int
    var cradleTopCornerRadius: Int
    var cradleBottomCornerRadius: Int
    var cradleStartEndMargin: Int
    var cradleContentsMargin: Int

    var indicatorXPos = 0
    var indicatorWidth = 0
    var indicatorColor: Int get() = indicatorPaint.color
                            set(value) { indicatorPaint.color = value }

    private val materialShapeDrawable = MaterialShapeDrawable()
    private val outlinePath = Path()
    private val topEdgePath = Path()

    protected val backgroundPaint = Paint().apply { style = Paint.Style.FILL }
    protected val borderPaint = Paint().apply { style = Paint.Style.STROKE }
    private val indicatorPaint = Paint().apply { style = Paint.Style.STROKE }

    init {
        setWillNotDraw(false)
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)
        cradleAlignmentMode = CradleAlignmentMode.values()[
            a.getInt(R.styleable.BottomAppBar_cradleAlignmentMode, CradleAlignmentMode.Center.ordinal)]
        cradleDepth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleDepth, 0)
        cradleTopCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleTopCornerRadius, 0)
        cradleBottomCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0)
        cradleStartEndMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleStartEndMargin, 90)
        cradleContentsMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleContentsMargin, 0)
        indicatorWidth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_indicatorWidth, 0)
        indicatorPaint.color = a.getColor(R.styleable.BottomAppBar_indicatorColor, 0)
        a.recycle()

        materialShapeDrawable.shapeAppearanceModel = ShapeAppearanceModel.builder().
                                                     setTopEdge(CradleTopEdgeTreatment()).build()
        background = materialShapeDrawable

        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.borderWidth, typedValue, true)
        val borderWidth = TypedValue.complexToDimension(
            typedValue.data, context.resources.displayMetrics)

        borderPaint.strokeWidth = borderWidth
        indicatorPaint.strokeWidth = 2.5f * borderWidth
    }

    fun setIndicatorGradient(gradient: Shader) { indicatorPaint.shader = gradient }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return
        canvas.drawPath(outlinePath, backgroundPaint)
        canvas.drawPath(topEdgePath, borderPaint)
        canvas.withClip(indicatorXPos, 0, indicatorXPos + indicatorWidth, bottom) {
            drawPath(topEdgePath, indicatorPaint) }
    }

    fun prepareCradleLayout(cradleLayout: ViewGroup) {
        if (cradleLayout.parent !is CoordinatorLayout)
            throw IllegalStateException("The cradle layout should have a CoordinatorLayout as a parent.")

        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        (cradleLayout.layoutParams as CoordinatorLayout.LayoutParams).apply {
            when (cradleAlignmentMode) {
                CradleAlignmentMode.Start -> {
                    gravity = Gravity.BOTTOM or Gravity.START
                    marginStart = cradleStartEndMargin + cradleContentsMargin
                } CradleAlignmentMode.Center -> {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    this@BottomAppBar.measure(wrapContentSpec, wrapContentSpec)
                    bottomMargin = this@BottomAppBar.measuredHeight + cradleContentsMargin - cradleDepth
                } CradleAlignmentMode.End -> {
                    gravity = Gravity.BOTTOM or Gravity.END
                    marginEnd = cradleStartEndMargin + cradleContentsMargin
                }
            }
        }
        cradleLayout.clipChildren = false
        cradleLayout.measure(wrapContentSpec, wrapContentSpec)
        cradleWidth = cradleLayout.measuredWidth
        materialShapeDrawable.invalidateSelf()
    }

    /** An EdgeTreatment used to draw a cradle cutout for the BottomAppBar.
     *
     *  CradleTopEdgeTreatment's getEdgePath draws a cutout shape for the Bottom-
     *  AppBar to hold the contents of the BottomAppBar's cradle layout. It uses
     *  BottomAppBar's members (e.g. cradleHorizontalOffset or cradleDepth) to
     *  accomplish this, and is therefore not usable outside its outer class.
     *
     *  CradleTopEdgeTreatment does not support the interpolation feature of
     *  EdgeTreatment. */
    inner class CradleTopEdgeTreatment : EdgeTreatment() {
        override fun getEdgePath(
            length: Float,
            center: Float,
            interpolation: Float,
            shapePath: ShapePath
        ) {
            // The path needs to be offset by half of the border paint's strokewidth
            // value to ensure that the border is drawn entirely within the canvas.
            val pathOffset = borderPaint.strokeWidth / 2f

            val cradleFullWidth = cradleWidth + 2 * cradleContentsMargin + 2 * pathOffset
            // start will be the x coordinate of the start of the cradle if cradleTopCornerRadius is zero
            val start = when (cradleAlignmentMode) {
                CradleAlignmentMode.Start ->  left + cradleStartEndMargin
                CradleAlignmentMode.Center -> left + (width - cradleFullWidth) / 2
                CradleAlignmentMode.End ->    right - cradleStartEndMargin - cradleFullWidth
            }.toFloat()
            val end = start + cradleFullWidth

            // If the top and bottom corner radii combined are less than the cradle depth,
            // there will be vertical sides to the cradle. Conversely, if the top and bottom
            // corner radii add up to more than the cradle depth, then the sweep angles of
            // the top and bottom corners will be less than the full ninety degrees.
            val cradleVerticalSideLength = cradleDepth - cradleTopCornerRadius -
                                           cradleBottomCornerRadius.toFloat()
            val topCornerArc = if (cradleVerticalSideLength < 0) {
                val distanceX = cradleTopCornerRadius - pathOffset
                val distanceY = -cradleVerticalSideLength.toDouble()
                val arcOffset = Math.toDegrees(atan(distanceY / distanceX))
                (arcQuarter - arcOffset.toFloat()).coerceAtMost(90f)
            } else arcQuarter
            val bottomCornerArc = if (cradleVerticalSideLength < 0) {
                val distanceX = cradleBottomCornerRadius + pathOffset
                val distanceY = -cradleVerticalSideLength.toDouble()
                val arcOffset = Math.toDegrees(atan(distanceY / distanceX))
                (arcOffset.toFloat() - arcQuarter).coerceAtLeast(-90f)
            } else -arcQuarter

            shapePath.apply {
                lineTo(0f, pathOffset)
                lineTo(start - cradleTopCornerRadius, pathOffset)
                addArc(/*left*/       start - 2 * cradleTopCornerRadius,
                       /*top*/        pathOffset,
                       /*right*/      start,
                       /*bottom*/     2f * cradleTopCornerRadius + pathOffset,
                       /*startAngle*/ angleUp,
                       /*sweepAngle*/ topCornerArc)
                if (cradleVerticalSideLength > 0)
                    lineTo(start, cradleDepth - cradleBottomCornerRadius + pathOffset)
                addArc(/*left*/       start,
                       /*top*/        cradleDepth - 2 * cradleBottomCornerRadius + pathOffset,
                       /*right*/      start + 2 * cradleBottomCornerRadius,
                       /*bottom*/     cradleDepth + pathOffset,
                       /*startAngle*/ angleDown + topCornerArc,
                       /*sweepAngle*/ bottomCornerArc)
                lineTo(end - cradleBottomCornerRadius, cradleDepth + pathOffset)
                addArc(/*left*/       end - 2 * cradleBottomCornerRadius,
                       /*top*/        cradleDepth - 2 * cradleBottomCornerRadius + pathOffset,
                       /*right*/      end,
                       /*bottom*/     cradleDepth + pathOffset,
                       /*startAngle*/ angleDown,
                       /*sweepAngle*/ bottomCornerArc)
                if (cradleVerticalSideLength > 0)
                    lineTo(end, cradleDepth - cradleBottomCornerRadius + pathOffset)
                addArc(/*left*/       end,
                       /*top*/        pathOffset,
                       /*right*/      end + 2 * cradleTopCornerRadius,
                       /*bottom*/     2 * cradleTopCornerRadius + pathOffset,
                       /*startAngle*/ angleUp + bottomCornerArc,
                       /*sweepAngle*/ topCornerArc)
                // There seems to be some slight miscalculation in the topCornerArc, or
                // elsewhere, that causes the y value after the above arc to be slightly
                // off. This will cause a tilt in the final horizontal line. The follo-
                // wing lineTo puts the current y at pathOffset, where it should be.
                lineTo(endX, pathOffset)
                lineTo(width.toFloat(), pathOffset)
            }
            // Copy the shapePath to topEdgePath and outlinePath, and additionally
            // finish the outlinePath so that it goes all the way around the view.
            topEdgePath.rewind()
            shapePath.applyToPath(Matrix(), topEdgePath)
            outlinePath.set(topEdgePath)
            outlinePath.lineTo(length, height.toFloat())
            outlinePath.lineTo(0f, height.toFloat())
            outlinePath.close()
        }
    }
}