/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapePath

/** A custom toolbar that has a cradle cutout in its shape to hold the contents of a layout.
 *
 *  BottomAppBar functions as a regular toolbar, except that its custom Cradle-
 *  EdgeTreatment used on its top edge gives it a cutout in its shape that can
 *  be used to hold the contents of a layout (probably a linear or constraint
 *  layout). The layout in question should be passed to the function prepare-
 *  CradleLayout during app startup so that BottomAppBar can set up its layout
 *  params.
 *
 *  XML attributes:
 *  - CradleAlignmentMode cradleAlignmentMode = CradleAlignmentMode.Center:
 *        Where the cradle is drawn on the BottomAppBar.
 *  - dimension cradleDepth = 0: The depth of the cradle
 *  - dimension cradleTopCornerRadius = 0: The radius of the top corners of the cradle
 *  - dimension cradleBottomCornerRadius = 0: The radius of the bottom corners of the cradle
 *  - dimension cradleStartEndMargin = 90: The start or end margin of the cradle
 *    when the CradleAlignmentMode is Start or End. Does nothing when the Cradle-
 *    AlignmentMode is Center.
 *  - dimension cradleContentsMargin = 0: The margin between the cradle and its
 *    nested layout
 *  - color backgroundTint = 0: The color ID of the color to use for the BottomApp-
 *    Bar's background. */
class BottomAppBar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {
    private val arcQuarter = 90f
    private val angleRight = 0f
    private val angleDown = 90f
    private val angleLeft = 180f
    private val angleUp = 270f
    private val materialShapeDrawable = MaterialShapeDrawable()

    enum class CradleAlignmentMode { Start, Center, End }
    val cradleAlignmentMode: CradleAlignmentMode

    var cradleWidth: Int = 0
    var cradleDepth: Int
    var cradleTopCornerRadius: Int
    var cradleBottomCornerRadius: Int
    var cradleStartEndMargin: Int
    var cradleContentsMargin: Int
    var backgroundTint: Int? = 0
        get() = materialShapeDrawable.tintList?.defaultColor
        set(value) { field = value
                     materialShapeDrawable.tintList = ColorStateList.valueOf(value ?: 0) }

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
        a.recycle()

        materialShapeDrawable.shapeAppearanceModel = ShapeAppearanceModel.builder().
                                                     setTopEdge(CradleTopEdgeTreatment()).build()
        materialShapeDrawable.tintList = ColorStateList.valueOf(backgroundTint ?: 0)
        background = materialShapeDrawable
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
        override fun getEdgePath(length: Float, center: Float,
                                 interpolation: Float, shapePath: ShapePath) {
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
        }
    }
}