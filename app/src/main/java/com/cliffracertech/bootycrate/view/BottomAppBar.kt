/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
 import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.withClip
import androidx.core.view.doOnNextLayout
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.applyConfig
import com.google.android.gms.common.internal.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapePath
import kotlin.math.atan
import kotlin.math.atan2

/**
 * A custom toolbar that has a cutout in its top edge to hold the contents of a layout.
 *
 * BottomAppBar functions mostly as a regular Toolbar, except that its custom
 * CradleTopEdgeTreatment used on its top edge gives it a cutout in its shape
 * that can be used to hold the contents of a layout. The layout in question
 * must be referenced by the XML attribute cradleLayoutResId, and must be a
 * child of the BottomAppBar. The activity or fragment that uses a BottomAppbar
 * is responsible for setting the correct on screen position of the cradle
 * layout contents. This being the case, BottomAppBar will draw the cradle
 * around the cradle layout using the values of the parameters cradleWidth,
 * cradleDepth, cradleTopCornerRadius, cradleBottomCornerRadius, and
 * cradleContentsMargin.
 *
 * Like the Material library BottomAppBar, BottomAppBar manages its own
 * background. In order to tint the background a solid color, the property
 * backgroundTint can be set in XML or programmatically to an int color code.
 * Alternatively the background can be set to a Shader instance using the
 * property backgroundGradient.
 */
open class BottomAppBar(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var cradleLayout: ViewGroup? = null
    var cradleWidth = 0
        set(value) { field = value
                     materialShapeDrawable.invalidateSelf() }
    var cradleDepth: Int
    var cradleTopCornerRadius: Int
    var cradleBottomCornerRadius: Int
    var cradleContentsMargin: Int

    var cradleInterpolation get() = materialShapeDrawable.interpolation
        set(value) { materialShapeDrawable.interpolation = value }
    private val materialShapeDrawable = MaterialShapeDrawable()
    protected val outlinePath = Path()
    protected val topEdgePath = Path()

    protected val backgroundPaint = Paint().apply { style = Paint.Style.FILL }
    var backgroundTint: Int get() = backgroundPaint.color
                            set(value) { backgroundPaint.color = value }
    var backgroundGradient get() = backgroundPaint.shader
                           set(value) { backgroundPaint.shader = value }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)
        cradleDepth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleDepth, 0)
        cradleTopCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleTopCornerRadius, 0)
        cradleBottomCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0)
        cradleContentsMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleContentsMargin, 0)
        val cradleLayoutResId = a.getResourceIdOrThrow(R.styleable.BottomAppBar_cradleLayoutResId)
        backgroundTint = a.getColor(R.styleable.BottomAppBar_backgroundTint,
                                    ContextCompat.getColor(context, android.R.color.white))
        a.recycle()

        materialShapeDrawable.shapeAppearanceModel =
            ShapeAppearanceModel.builder().setTopEdge(CradleTopEdgeTreatment()).build()
        background = materialShapeDrawable
        @Suppress("LeakingThis") setWillNotDraw(false)
        doOnNextLayout { cradleLayout = findViewById(cradleLayoutResId) }
    }

    private val upperLeftCurve = Path()
    private val bottomLeftCurve = Path()
    private val bottomRightCurve = Path()
    private val upperRightCurve = Path()

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return
        canvas.drawPath(outlinePath, backgroundPaint)
        val bgShader = backgroundPaint.shader
        backgroundPaint.shader = null
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = 10f
        backgroundPaint.color = Color.RED
        canvas.drawPath(upperLeftCurve, backgroundPaint)
        backgroundPaint.color = Color.BLUE
        canvas.drawPath(bottomLeftCurve, backgroundPaint)
        backgroundPaint.color = Color.YELLOW
        canvas.drawPath(bottomRightCurve, backgroundPaint)
        backgroundPaint.color = Color.GREEN
        canvas.drawPath(upperRightCurve, backgroundPaint)
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.shader = bgShader
    }

    private val arcQuarter = 90f
    private val angleDown = 90f
    private val angleUp = 270f

    private val topLeftEllipse = RectF()
    private val bottomLeftEllipse = RectF()
    private val bottomRightEllipse = RectF()
    private val topRightEllipse = RectF()

    fun ShapePath.addArc(
        ellipseBounds: RectF,
        startX: Double, startY: Double,
        endX: Double, endY: Double
    ) {
        val centerX = (ellipseBounds.right - ellipseBounds.left) / 2
        val centerY = (ellipseBounds.bottom - ellipseBounds.top) / 2
        var alpha = atan2(centerY - startY, centerX - startX)
        if (alpha < 0.0)
            alpha = alpha * -1.0 + Math.PI
        var beta = atan2(centerY - endY, centerX - endX)
        if (beta < 0.0)
            beta = beta * -1.0 + Math.PI
        addArc(ellipseBounds.left, ellipseBounds.top,
               ellipseBounds.right, ellipseBounds.bottom,
               alpha.toFloat() + Math.PI,
               (beta - alpha).toFloat() + Math.PI
    }

    /** An EdgeTreatment used to draw a cradle cutout for the BottomAppBar.
     *
     *  CradleTopEdgeTreatment's getEdgePath draws a cutout shape for the BottomAppBar
     *  to hold the contents of the BottomAppBar's cradle layout. */
    inner class CradleTopEdgeTreatment : EdgeTreatment() {
        override fun getEdgePath(
            length: Float,
            center: Float,
            interpolation: Float,
            shapePath: ShapePath
        ) {
            val cradleFullWidth = cradleWidth + 2 * cradleContentsMargin
            // start will be the x coordinate of the start of the cradle if cradleTopCornerRadius is zero
            val start = (cradleLayout?.x ?: return) - cradleContentsMargin
            val end = start + cradleFullWidth
            val bottom = cradleDepth * interpolation
            val topCornerRadius = cradleTopCornerRadius * (1f)// + 9f * (1f - interpolation))
            val bottomCornerRadius = cradleBottomCornerRadius * (1f)// + 9f * (1f - interpolation))

            // If the top and bottom corner radii combined are less than the cradle depth,
            // there will be vertical sides to the cradle. Conversely, if the top and bottom
            // corner radii add up to more than the cradle depth, then the sweep angles of
            // the top and bottom corners will be less than the full ninety degrees.
            val cradleVerticalSideLength = bottom - topCornerRadius - bottomCornerRadius

            val topCornerArc = //interpolation *
                if (cradleVerticalSideLength < 0) {
                    val distanceX = topCornerRadius
                    val distanceY = -cradleVerticalSideLength.toDouble()
                    val arcOffset = Math.toDegrees(atan(distanceY / distanceX))
                    (arcQuarter - arcOffset.toFloat()).coerceAtMost(90f)
                } else arcQuarter
            val bottomCornerArc = //interpolation *
                if (cradleVerticalSideLength < 0) {
                    val distanceX = bottomCornerRadius
                    val distanceY = -cradleVerticalSideLength.toDouble()
                    val arcOffset = Math.toDegrees(atan(distanceY / distanceX))
                    (arcOffset.toFloat() - arcQuarter).coerceAtLeast(-90f)
                } else -arcQuarter

            topLeftEllipse.left = start - 2 * topCornerRadius
            topLeftEllipse.top = 0f
            topLeftEllipse.right = start + cradleTopCornerRadius - topCornerRadius
            topLeftEllipse.bottom = 2 * topCornerRadius

            bottomLeftEllipse.left = start,
            bottomLeftEllipse.top = bottom - 2 * bottomCornerRadius
            bottomLeftEllipse.right = start + 2 * bottomCornerRadius
            bottomLeftEllipse.bottom = bottom

            bottomRightEllipse.left = end - 2 * bottomCornerRadius
            bottomRightEllipse.top = bottom - 2 * bottomCornerRadius
            bottomRightEllipse.right = end
            bottomRightEllipse.bottom = bottom

            topRightEllipse.left = end
            topRightEllipse.top = 0f
            topRightEllipse.right = end + 2 * topCornerRadius
            topRightEllipse.bottom = 2 * topCornerRadius

            shapePath.apply {
                lineTo(start - cradleTopCornerRadius, 0f)
                addArc(/*left*/       topLeftEllipse.left,
                       /*top*/        topLeftEllipse.top,
                       /*right*/      topLeftEllipse.right,
                       /*bottom*/     topLeftEllipse.bottom,
                       /*startAngle*/ angleUp,
                       /*sweepAngle*/ topCornerArc)
                if (cradleVerticalSideLength > 0)
                    lineTo(start, bottom - bottomCornerRadius)
                addArc(/*left*/       start,
                       /*top*/        bottom - 2 * bottomCornerRadius,
                       /*right*/      start + 2 * bottomCornerRadius,
                       /*bottom*/     bottom,
                       /*startAngle*/ angleDown + topCornerArc,
                       /*sweepAngle*/ bottomCornerArc)
                lineTo(end - cradleBottomCornerRadius, bottom)
                addArc(/*left*/       end - 2 * bottomCornerRadius,
                       /*top*/        bottom - 2 * bottomCornerRadius,
                       /*right*/      end,
                       /*bottom*/     bottom,
                       /*startAngle*/ angleDown,
                       /*sweepAngle*/ bottomCornerArc)
                if (cradleVerticalSideLength > 0)
                    lineTo(end, bottom - bottomCornerRadius)
                addArc(/*left*/       end,
                       /*top*/        0f,
                       /*right*/      end + 2 * topCornerRadius,
                       /*bottom*/     2 * topCornerRadius,
                       /*startAngle*/ angleUp + bottomCornerArc,
                       /*sweepAngle*/ topCornerArc)

//                val c = 0.551915024494f
//                cubicToPoint(
//                    /*x1*/ start - topCornerRadius * (1 - c),
//                    /*y1*/ 0f,
//                    /*x2*/ start,
//                    /*y2*/ topCornerRadius * c,
//                    /*toX*/ start,
//                    /*toY*/ topCornerRadius)
//                cubicToPoint(
//                    /*x1*/ start,
//                    /*y1*/ bottom - bottomCornerRadius * (1 - c),
//                    /*x2*/ start + bottomCornerRadius * (1 - c),
//                    /*y2*/ bottom,
//                    /*toX*/ start + bottomCornerRadius,
//                    /*toY*/ bottom)
//                lineTo(end - bottomCornerRadius, bottom)
//                cubicToPoint(
//                    /*x1*/ end - bottomCornerRadius * (1 - c),
//                    /*y1*/ bottom,
//                    /*x2*/ end,
//                    /*y2*/ bottom - bottomCornerRadius * (1 - c),
//                    /*toX*/ end,
//                    /*toY*/ bottom - bottomCornerRadius)
//                cubicToPoint(
//                    /*x1*/ end,
//                    /*y1*/ topCornerRadius * (1 - c),
//                    /*x2*/ end + topCornerRadius * c,
//                    /*y2*/ 0f,
//                    /*toX*/ end + topCornerRadius,
//                    /*toY*/ 0f)

//                cubicToPoint(
//                    /*x1*/ start - topCornerRadius * (1 - c),
//                    /*y1*/ 0f,
//                    /*x2*/ start,
//                    /*y2*/ topCornerRadius * c,
//                    /*toX*/ start,
//                    /*toY*/ topCornerRadius)
//                cubicToPoint(
//                    /*x1*/ start,
//                    /*y1*/ bottom - bottomCornerRadius * (1 - c),
//                    /*x2*/ start + bottomCornerRadius * (1 - c),
//                    /*y2*/ bottom,
//                    /*toX*/ start + bottomCornerRadius,
//                    /*toY*/ bottom)
                lineTo(width.toFloat(), 0f)
            }
            // Copy the shapePath to topEdgePath and outlinePath, and additionally
            // finish the outlinePath so that it goes all the way around the view.
            topEdgePath.rewind()
            shapePath.applyToPath(Matrix(), topEdgePath)
            outlinePath.set(topEdgePath)
            outlinePath.lineTo(length, height.toFloat())
            outlinePath.lineTo(0f, height.toFloat())
            outlinePath.close()

            upperLeftCurve.rewind()
            bottomLeftCurve.rewind()
            bottomRightCurve.rewind()
            upperRightCurve.rewind()

            upperLeftCurve.moveTo(start - topCornerRadius, 0f)
            upperLeftCurve.addArc(
                /*left*/       start - 2 * topCornerRadius,
                /*top*/        0f,
                /*right*/      start,
                /*bottom*/     2 * topCornerRadius,
                /*startAngle*/ angleUp,
                /*sweepAngle*/ topCornerArc)
            bottomLeftCurve.moveTo(start, topCornerRadius)
            bottomLeftCurve.addArc(
                /*left*/       start,
                /*top*/        bottom - 2 * bottomCornerRadius,
                /*right*/      start + 2 * bottomCornerRadius,
                /*bottom*/     bottom,
                /*startAngle*/ angleDown + topCornerArc,
                /*sweepAngle*/ bottomCornerArc)
            bottomRightCurve.moveTo(end - bottomCornerRadius, bottom)
            bottomRightCurve.addArc(
                /*left*/       end - 2 * bottomCornerRadius,
                /*top*/        bottom - 2 * bottomCornerRadius,
                /*right*/      end,
                /*bottom*/     bottom,
                /*startAngle*/ angleDown,
                /*sweepAngle*/ bottomCornerArc)
            upperRightCurve.moveTo(end, topCornerRadius)
            upperRightCurve.addArc(
                /*left*/       end,
                /*top*/        0f,
                /*right*/      end + 2 * topCornerRadius,
                /*bottom*/     2 * topCornerRadius,
                /*startAngle*/ angleUp + bottomCornerArc,
                /*sweepAngle*/ topCornerArc)

//            upperLeftCurve.moveTo(start - topCornerRadius, 0f)
//            upperLeftCurve.cubicTo(
//                /*x1*/ start - topCornerRadius * (1 - c),
//                /*y1*/ 0f,
//                /*x2*/ start,
//                /*y2*/ topCornerRadius * c,
//                /*toX*/ start,
//                /*toY*/ topCornerRadius)
//            bottomLeftCurve.moveTo(start, topCornerRadius)
//            bottomLeftCurve.cubicTo(
//                /*x1*/ start,
//                /*y1*/ bottom - bottomCornerRadius * (1 - c),
//                /*x2*/ start + bottomCornerRadius * (1 - c),
//                /*y2*/ bottom,
//                /*toX*/ start + bottomCornerRadius,
//                /*toY*/ bottom)
//            bottomRightCurve.moveTo(end - bottomCornerRadius, bottom)
//            bottomRightCurve.cubicTo(
//                /*x1*/ end - bottomCornerRadius * (1 - c),
//                /*y1*/ bottom,
//                /*x2*/ end,
//                /*y2*/ bottom - bottomCornerRadius * (1 - c),
//                /*toX*/ end,
//                /*toY*/ bottom - bottomCornerRadius)
//            upperRightCurve.moveTo(end, topCornerRadius)
//            upperRightCurve.cubicTo(
//                /*x1*/ end,
//                /*y1*/ topCornerRadius * (1 - c),
//                /*x2*/ end + topCornerRadius * c,
//                /*y2*/ 0f,
//                /*toX*/ end + topCornerRadius,
//                /*toY*/ 0f)
        }
    }
}

/**
 * A BottomAppBar that also draws an indicator above the selected navigation bar item.
 *
 * BottomAppBarWithIndicator extends BottomAppBar by also drawing an indicator
 * with the solid color described by the @property indicatorTint. The indicator
 * can be moved to be above a given nav bar menu item by calling the function
 * moveIndicatorToNavBarItem with the id of the menu item. The BottomNavigationView
 * must be referenced through the XML attribute navBarResId, and must be a
 * descendant of the BottomAppBarWithIndicator. The XML attributes indicatorThickness
 * and indicatorWidth are used to define the dimensions of the indicator.
 * Besides a solid color tint, the indicator can also be painted with a Shader
 * object using the property indicatorGradient.
 */
@Suppress("LeakingThis")
class BottomAppBarWithIndicator(context: Context, attrs: AttributeSet) :
    BottomAppBar(context, attrs)
{
    private lateinit var navBar: BottomNavigationView
    private val indicatorPaint = Paint().apply { style = Paint.Style.STROKE }
    var indicatorWidth = 0
    var indicatorThickness get() = indicatorPaint.strokeWidth
                           set(value) { indicatorPaint.strokeWidth = value }
    var indicatorTint: Int get() = indicatorPaint.color
                            set(value) { indicatorPaint.color = value }
    var indicatorGradient get() = indicatorPaint.shader
                          set(value) { indicatorPaint.shader = value }
    private var indicatorXPos = 0
    var indicatorAnimatorConfig: AnimatorConfig? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBarWithIndicator)
        indicatorThickness = a.getDimension(R.styleable.BottomAppBarWithIndicator_indicatorThickness, 0f)
        indicatorWidth = a.getDimensionPixelOffset(R.styleable.BottomAppBarWithIndicator_indicatorWidth, 0)
        indicatorPaint.color = a.getColor(R.styleable.BottomAppBarWithIndicator_indicatorTint,
                                          ContextCompat.getColor(context, android.R.color.black))
        val navBarResId = a.getResourceIdOrThrow(R.styleable.BottomAppBarWithIndicator_navBarResId)
        a.recycle()

        setWillNotDraw(false)
        doOnNextLayout {
            navBar = findViewById(navBarResId)
            moveIndicatorToNavBarItem(navBar.selectedItemId, animate = false)
        }
    }

    private val rect = Rect()
    /** Move the indicator to be above the item with id equal to @param menuItemId,
     * animating the change if @param animate is equal to true. */
    fun moveIndicatorToNavBarItem(menuItemId: Int, animate: Boolean = true) {
        if (!::navBar.isInitialized) return
        navBar.findViewById<View>(menuItemId)?.let {
            // Using it.centerX() instead of getting the global visible rect's centerX
            // seems to result in a slightly incorrect value when in landscape mode.
            it.getGlobalVisibleRect(rect)
            val newIndicatorXPos = rect.centerX() - indicatorWidth / 2
            if (!animate) indicatorXPos = newIndicatorXPos
            else ValueAnimator.ofInt(indicatorXPos, newIndicatorXPos).apply {
                addUpdateListener { indicatorXPos = animatedValue as Int; invalidate() }
                applyConfig(indicatorAnimatorConfig)
            }.start()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.withClip(indicatorXPos, 0, indicatorXPos + indicatorWidth, bottom) {
            drawPath(topEdgePath, indicatorPaint)
        }
    }
}