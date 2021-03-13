/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.withClip
import androidx.core.view.doOnNextLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.shape.*
import kotlin.math.atan

/**
 * A custom toolbar that has a cutout in its top edge to hold the contents of a layout.
 *
 * BottomAppBar functions mostly as a regular Toolbar, except that its custom
 * CradleTopEdgeTreatment used on its top edge gives it a cutout in its shape
 * that can be used to hold the contents of a layout. The layout in question
 * should be passed to the function prepareCradleLayout during app startup so
 * that BottomAppBar can set up its layout params.
 *
 * The gradients used for the background, border, and indicator can be set
 * through the public properties backgroundGradient, borderGradient, and indi-
 * catorGradient, respectively.
 */
open class BottomAppBar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {

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

    var backgroundGradient: Shader? get() = backgroundPaint.shader
                                    set(value) { backgroundPaint.shader = value }
    var borderGradient: Shader? get() = borderPaint.shader
                                set(value) { borderPaint.shader = value }

    private val materialShapeDrawable = MaterialShapeDrawable()
    protected val outlinePath = Path()
    protected val topEdgePath = Path()

    protected val backgroundPaint = Paint().apply { style = Paint.Style.FILL }
    protected val borderPaint = Paint().apply { style = Paint.Style.STROKE }

    private val arcQuarter = 90f
    private val angleRight = 0f
    private val angleDown = 90f
    private val angleLeft = 180f
    private val angleUp = 270f

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)
        cradleAlignmentMode = CradleAlignmentMode.values()[
            a.getInt(R.styleable.BottomAppBar_cradleAlignmentMode, CradleAlignmentMode.Center.ordinal)]
        cradleDepth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleDepth, 0)
        cradleTopCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleTopCornerRadius, 0)
        cradleBottomCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0)
        cradleStartEndMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleStartEndMargin, 90)
        cradleContentsMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleContentsMargin, 0)
        borderPaint.strokeWidth = a.getDimensionPixelSize(R.styleable.BottomAppBar_topBorderWidth, 0).toFloat()
        a.recycle()

        materialShapeDrawable.shapeAppearanceModel = ShapeAppearanceModel.builder().
                                                     setTopEdge(CradleTopEdgeTreatment()).build()
        background = materialShapeDrawable
        @Suppress("LeakingThis") setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return
        canvas.drawPath(outlinePath, backgroundPaint)
        canvas.drawPath(topEdgePath, borderPaint)
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
            // The path needs to be offset by half of the border paint's strokeWidth
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
                @Suppress("DEPRECATION")
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

/**
 * A BottomAppBar that also draws an indicator above the selected navigation bar item.
 *
 * BottomAppBarWithIndicator extends BottomAppBar by also drawing an indicator,
 * either with the solid color described by the @property indicatorColor, or
 * with the shader object described by the @property indicatorGradient if set.
 * The indicator can be moved to be above a given nav bar menu item by calling
 * the function moveIndicatorToNavBarItem with the id of the menu item. The
 * BottomNavigationView must be referenced through the XML attribute naviga-
 * tionBarResId, and should be a descendant of the BottomAppBarWithIndicator.
 */
class BottomAppBarWithIndicator(context: Context, attrs: AttributeSet) :
    BottomAppBar(context, attrs)
{
    private val indicatorPaint = Paint().apply { style = Paint.Style.STROKE }
    private var indicatorXPos = 0
    private lateinit var navBar: BottomNavigationView
    var indicatorAnimatorConfig: AnimatorConfig? = null
    var indicatorWidth = 0
    var indicatorColor: Int get() = indicatorPaint.color
                            set(value) { indicatorPaint.color = value }
    var indicatorGradient: Shader? get() = indicatorPaint.shader
                                   set(gradient) { indicatorPaint.shader = gradient }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBarWithIndicator)
        indicatorWidth = a.getDimensionPixelOffset(R.styleable.BottomAppBarWithIndicator_indicatorWidth, 0)
        indicatorPaint.color = a.getColor(R.styleable.BottomAppBarWithIndicator_indicatorColor, 0)
        val navViewResId = a.getResourceIdOrThrow(R.styleable.BottomAppBarWithIndicator_navigationBarResId)
        a.recycle()

        setWillNotDraw(false)
        indicatorPaint.strokeWidth = 2.5f * borderPaint.strokeWidth
        doOnNextLayout { navBar = findViewById(navViewResId) }
    }

    /** Move the indicator to be above the item with id equal to @param menuItemId,
     * animating the change if @param animate is equal to true. */
    fun moveIndicatorToNavBarItem(menuItemId: Int, animate: Boolean = true) {
        navBar.findViewById<View>(menuItemId)?.let {
            val newIndicatorXPos = (it.width - indicatorWidth) / 2 + it.left
            if (!animate) indicatorXPos = newIndicatorXPos
            else ValueAnimator.ofInt(indicatorXPos, newIndicatorXPos).apply {
                addUpdateListener { indicatorXPos = animatedValue as Int }
                applyConfig(indicatorAnimatorConfig)
            }.start()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.withClip(indicatorXPos, 0, indicatorXPos + indicatorWidth, bottom) {
            drawPath(topEdgePath, indicatorPaint) }
    }
}