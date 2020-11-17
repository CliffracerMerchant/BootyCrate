/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.shape.*

/** A custom toolbar that has a cutout in its top edge to hold the contents of a layout.
 *
 *  BottomAppBar functions mostly as a regular toolbar, except that its custom
 *  CradleTopEdgeTreatment used on its top edge gives it a cutout in its shape
 *  that can be used to hold the contents of a layout (probably a linear or con-
 *  straint layout). The layout in question should be passed to the function
 *  prepareCradleLayout during app startup so that BottomAppBar can set up its
 *  layout params.
 *
 *  BottomAppBar can also draw an indicator along its top edge, in case it is
 *  used in conjunction with a BottomNavigationView. The primary reason to use
 *  BottomAppBar's indicator over, for example, one inside a BottomNavigation-
 *  View subclass, is that BottomAppBar's indicator will always appear along
 *  its top edge, taking into account the cradle layout cutaway. Unfortunately
 *  BottomAppBar has no way of knowing the exact on-screen positions of the
 *  menu items displayed in the BottomNavigationView, so the position must be
 *  manually set in pixels using the property indicatorXPos. The width (also in
 *  pixels) can be set via the XML attribute indicatorWidth, or at runtime using
 *  the property of the same name. The indicator color can be set using the XML
 *  property indicatorColor, but the Paint object used to draw the indicator is
 *  exposed through the property indicatorPaint in case more elaborate changes
 *  to the indicator drawing are desired.
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
 *  - backgroundTint: color = 0: The color ID of the color to use for the BottomAppBar's background.
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

    private val materialShapeDrawable = MaterialShapeDrawable()
    var backgroundTint: Int?
        get() = materialShapeDrawable.tintList?.defaultColor
        set(value) { materialShapeDrawable.tintList = ColorStateList.valueOf(value ?: 0) }

    private val topEdgePath = Path()
    var indicatorXPos = 0
    var indicatorWidth = 0
    val indicatorPaint = Paint()

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)
        cradleAlignmentMode = CradleAlignmentMode.values()[
            a.getInt(R.styleable.BottomAppBar_cradleAlignmentMode, CradleAlignmentMode.Center.ordinal)]
        cradleDepth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleDepth, 0)
        cradleTopCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleTopCornerRadius, 0)
        cradleBottomCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0)
        cradleStartEndMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleStartEndMargin, 90)
        cradleContentsMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleContentsMargin, 0)
        backgroundTint = a.getColor(R.styleable.BottomAppBar_backgroundTint, 0)
        indicatorWidth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_indicatorWidth, 0)
        indicatorPaint.color = a.getColor(R.styleable.BottomAppBar_indicatorColor, 0)
        a.recycle()

        materialShapeDrawable.shapeAppearanceModel = ShapeAppearanceModel.builder().
                                                     setTopEdge(CradleTopEdgeTreatment()).build()
        background = materialShapeDrawable
        setWillNotDraw(false)
        indicatorPaint.style = Paint.Style.STROKE
        // Note that the indicator drawing operation is clipped to the top edge
        // path. The apparent indicatorPaint.strokeWidth value will therefore
        // effectively be half of its set value due to half of the width being
        // drawn outside the clipped area.
        indicatorPaint.strokeWidth = 16f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return
        canvas.save()
        canvas.clipRect(indicatorXPos, 0, indicatorXPos + indicatorWidth, bottom)
        canvas.drawPath(topEdgePath, indicatorPaint)
        canvas.restore()
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
                }
                CradleAlignmentMode.Center -> {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    this@BottomAppBar.measure(wrapContentSpec, wrapContentSpec)
                    bottomMargin = this@BottomAppBar.measuredHeight + cradleContentsMargin - cradleDepth
                }
                CradleAlignmentMode.End -> {
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
     *  EdgeTreament. */
    inner class CradleTopEdgeTreatment : EdgeTreatment() {
        override fun getEdgePath(
            length: Float,
            center: Float,
            interpolation: Float,
            shapePath: ShapePath
        ) {
            val cradleFullWidth = cradleWidth + 2 * cradleContentsMargin
            val start = when (cradleAlignmentMode) {
                CradleAlignmentMode.Start ->  left + cradleStartEndMargin
                CradleAlignmentMode.Center -> left + (width - cradleFullWidth) / 2
                CradleAlignmentMode.End ->    right - cradleStartEndMargin - cradleFullWidth
            }.toFloat()
            val end = start + cradleFullWidth
            val cradleVerticalSideLength = cradleDepth - cradleTopCornerRadius -
                                           cradleBottomCornerRadius.toFloat()

            shapePath.lineTo(start - cradleTopCornerRadius, 0f)
            shapePath.addArc(/*left*/       start - 2 * cradleTopCornerRadius,
                             /*top*/        0f,
                             /*right*/      start,
                             /*bottom*/     2f * cradleTopCornerRadius,
                             /*startAngle*/ angleUp,
                             /*sweepAngle*/ arcQuarter)
            if (cradleVerticalSideLength > 0)
                shapePath.lineTo(start, cradleDepth - cradleBottomCornerRadius.toFloat())
            shapePath.addArc(/*left*/       start,
                             /*top*/        cradleDepth - 2f * cradleBottomCornerRadius,
                             /*right*/      start + 2 * cradleBottomCornerRadius,
                             /*bottom*/     cradleDepth.toFloat(),
                             /*startAngle*/ angleLeft,
                             /*sweepAngle*/ -arcQuarter)
            shapePath.lineTo(end - cradleBottomCornerRadius, cradleDepth.toFloat())
            shapePath.addArc(/*left*/       end - 2 * cradleBottomCornerRadius,
                             /*top*/        cradleDepth - 2f * cradleBottomCornerRadius,
                             /*right*/      end,
                             /*bottom*/     cradleDepth.toFloat(),
                             /*startAngle*/ angleDown,
                             /*sweepAngle*/ -arcQuarter)
            if (cradleVerticalSideLength > 0)
                shapePath.lineTo(end, cradleDepth - cradleBottomCornerRadius.toFloat())
            shapePath.addArc(/*left*/       end,
                             /*top*/        0f,
                             /*right*/      end + 2 * cradleTopCornerRadius,
                             /*bottom*/     2f * cradleTopCornerRadius,
                             /*startAngle*/ angleLeft,
                             /*sweepAngle*/ arcQuarter)
            shapePath.lineTo(length, 0f)

            topEdgePath.rewind()
            shapePath.applyToPath(Matrix(), topEdgePath)
        }
    }
}