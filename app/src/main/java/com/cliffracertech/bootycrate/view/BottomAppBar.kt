/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.doOnNextLayout
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.applyConfig
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.shape.*
import kotlin.math.*

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
 * In addition to the cradle cutout, BottomAppBar also styles its top left
 * and top right corners with rounded corners with a radius equal to the
 * value of the XML attr topOuterCornerRadius.
 *
 * Like the Material library BottomAppBar, BottomAppBar manages its own
 * background. In order to tint the background a solid color, the property
 * backgroundTint can be set in XML or programmatically to an int color code.
 * Alternatively the background can be set to a Shader instance using the
 * property backgroundGradient.
 */
class BottomAppBar(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val drawable = PathDrawable()
    private var navBar: BottomNavigationView? = null
    private var _topOuterCornerRadius = 0f
    private var topOuterCornerArcLength = 0f

    val cradle = Cradle()
    val indicator = Indicator()

    var topOuterCornerRadius get() = _topOuterCornerRadius
                             set(value) = setTopCornerRadiusPrivate(value)

    var backgroundTint: Int get() = drawable.paint.color
                            set(value) { drawable.paint.color = value }
    var backgroundGradient get() = drawable.paint.shader
                           set(value) { drawable.paint.shader = value }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)
        val cradleLayoutResId = a.getResourceIdOrThrow(R.styleable.BottomAppBar_cradleLayoutResId)
        val navBarResId = a.getResourceIdOrThrow(R.styleable.BottomAppBar_navBarResId)
        topOuterCornerRadius = a.getDimension(R.styleable.BottomAppBar_topOuterCornerRadius, 0f)
        backgroundTint = a.getColor(R.styleable.BottomAppBar_backgroundTint,
                                    ContextCompat.getColor(context, android.R.color.white))

        cradle.depth = a.getDimension(R.styleable.BottomAppBar_cradleDepth, 0f)
        cradle.topCornerRadius = a.getDimension(R.styleable.BottomAppBar_cradleTopCornerRadius, 0f)
        cradle.bottomCornerRadius = a.getDimension(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0f)
        cradle.contentsMargin = a.getDimension(R.styleable.BottomAppBar_cradleContentsMargin, 0f)

        indicator.height = a.getDimension(R.styleable.BottomAppBar_indicatorHeight, 0f)
        indicator.width = a.getDimension(R.styleable.BottomAppBar_indicatorWidth, 0f)
        indicator.tint = a.getColor(R.styleable.BottomAppBar_indicatorTint,
                                    ContextCompat.getColor(context, android.R.color.black))

        a.recycle()
        setWillNotDraw(false)
        background = drawable
        doOnNextLayout {
            cradle.initLayout(cradleLayoutResId)
            navBar = findViewById(navBarResId)
            invalidate()
            navBar?.let { indicator.moveToNavBarItem(it.selectedItemId, animate = false) }
        }
    }

    private val angleDown = 90f
    private val angleLeft = 180f
    private val angleUp = 270f

    private fun Path.arcTo(
        centerX: Float, centerY: Float,
        radius: Float,
        startAngle: Float, sweepAngle: Float,
    ) = arcTo(centerX - radius, centerY - radius,
              centerX + radius, centerY + radius,
              startAngle, sweepAngle, false)

    override fun invalidate() = drawable.path.run {
        rewind()
        moveTo(0f, topOuterCornerRadius)
        arcTo(centerX = topOuterCornerRadius,
              centerY = topOuterCornerRadius,
              radius = topOuterCornerRadius,
              startAngle = angleLeft,
              sweepAngle = 90f)
        cradle.addTo(this)
        arcTo(centerX = width - topOuterCornerRadius,
              centerY = topOuterCornerRadius,
              radius = topOuterCornerRadius,
              startAngle = angleUp,
              sweepAngle = 90f)
        lineTo(width.toFloat(), height.toFloat())
        lineTo(0f, height.toFloat())
        close()
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        indicator.draw(canvas)
    }

    private fun setTopCornerRadiusPrivate(radius: Float) {
        _topOuterCornerRadius = radius
        // Since circumference of a circle = 2*pi*r,
        // a quarter circle circumference = 2*pi*r / 4 = pi/2 * r
        topOuterCornerArcLength = (radius * Math.PI / 2.0).toFloat()
        invalidate()
    }

    inner class Cradle {
        private var _totalLength = 0f
        private var _totalWidth = 0f
        private var _startXPos = 0f
        private var layout: ViewGroup? = null

        val totalWidth get() = _totalWidth
        val totalLength get() = _totalLength
        val startXPos get() = _startXPos

        var interpolation = 1f
        var width = 0f
        var depth = 0f
        var contentsMargin = 0f
        var topCornerRadius = 0f
        var bottomCornerRadius = 0f

        fun initLayout(cradleLayoutResId: Int) {
            layout = findViewById(cradleLayoutResId)
        }

        fun addTo(path: Path) = path.apply {
            if (interpolation < 0.01f) return@apply
            val layoutX = layout?.x ?: return@apply
            val fullWidth = width + 2 * contentsMargin
            // The cradle width is interpolated down to 90% of its full width
            val startEndAdjust = fullWidth * 0.1f * (1f - interpolation)

            // start will be the x coordinate of the start of the cradle if cradleTopCornerRadius is zero
            val start = layoutX - contentsMargin + startEndAdjust
            val end = start + fullWidth - 2 * startEndAdjust
            val yDistance = depth * interpolation

            val topRadiusFraction = topCornerRadius / (topCornerRadius + bottomCornerRadius)
            val topCurveYDistance = topRadiusFraction * yDistance

            val sweepAngleRads = Math.PI / 2 - asin(1.0 - topCurveYDistance / topCornerRadius).coerceIn(0.0, 90.0)
            val topCurveXDistance = topCornerRadius * cos(Math.PI / 2 - sweepAngleRads).toFloat()
            val bottomCurveXDistance = bottomCornerRadius * cos(Math.PI / 2 - sweepAngleRads).toFloat()

            val sweepAngle = Math.toDegrees(sweepAngleRads).toFloat()

            lineTo(start - topCornerRadius, 0f)
            arcTo(centerX =    start - topCurveXDistance,
                  centerY =    topCornerRadius,
                  radius =     topCornerRadius,
                  startAngle = angleUp,
                  sweepAngle = sweepAngle)
            arcTo(centerX =    start + bottomCurveXDistance,
                  centerY =    yDistance - bottomCornerRadius,
                  radius =     bottomCornerRadius,
                  startAngle = angleDown + sweepAngle,
                  sweepAngle = -sweepAngle)
            arcTo(centerX =    end - bottomCurveXDistance,
                  centerY =    yDistance - bottomCornerRadius,
                  radius =     bottomCornerRadius,
                  startAngle = angleDown,
                  sweepAngle = -sweepAngle)
            arcTo(centerX =    end + topCurveXDistance,
                  centerY =    topCornerRadius,
                  radius =     topCornerRadius,
                  startAngle = angleUp - sweepAngle,
                  sweepAngle = sweepAngle)

            val cradleBottomLength = (end - start - 2 * bottomCurveXDistance)
            val cradleCurvesLength = 2 * sweepAngleRads * (topCornerRadius + bottomCornerRadius)
            _totalLength = cradleCurvesLength.toFloat() + cradleBottomLength
            _totalWidth = end - start + 2 * topCurveXDistance
            _startXPos = start - topCurveXDistance
        }
    }

    inner class Indicator() {
        private val path = Path()
        private val pathMeasure = PathMeasure()
        private val paint = Paint().apply { style = Paint.Style.STROKE
                                            strokeCap = Paint.Cap.ROUND }

        var animatorConfig: AnimatorConfig? = null
        var startDistance = 0f
            set(value) { field = value; updatePath() }
        var width = 0f

        var height get() = paint.strokeWidth
                   set(value) { paint.strokeWidth = value }

        var alpha get() = paint.alpha / 255f
                  set(value) { paint.alpha = (value * 255).roundToInt().coerceIn(0, 255) }

        var tint: Int get() = paint.color
                      set(value) { paint.color = value }

        var gradient: Shader? get() = paint.shader
                              set(value) { paint.shader = value }

        fun draw(canvas: Canvas?) = canvas?.drawPath(path, paint)

        private val rect = Rect()
        /** Move the indicator to be above the item with id equal to @param menuItemId,
         * animating the change if @param animate is equal to true. */
        fun moveToNavBarItem(menuItemId: Int, animate: Boolean = true) {
            navBar?.findViewById<View>(menuItemId)?.let {
                // Using it.centerX() instead of getting the global visible rect's centerX
                // seems to result in a slightly incorrect value when in landscape mode.
                it.getGlobalVisibleRect(rect)
                val newXPos = rect.centerX() - width / 2f
                val targetStartDistance = findDistanceForX(newXPos)
                if (!animate) startDistance = targetStartDistance
                else ValueAnimator.ofFloat(startDistance, targetStartDistance).apply {
                    addUpdateListener { startDistance = animatedValue as Float; invalidate() }
                    applyConfig(animatorConfig)
                }.start()
            }
        }

        private fun findDistanceForX(x: Float) = when {
            x < cradle.startXPos -> {
                x + topOuterCornerArcLength - topOuterCornerRadius
            } x > (cradle.startXPos + cradle.totalWidth) -> {
                val cradleEndDistance = cradle.startXPos + topOuterCornerArcLength - topOuterCornerRadius + cradle.totalLength
                val cradleEndX = cradle.startXPos + cradle.totalWidth
                (x - cradleEndX) + cradleEndDistance
                //x + topOuterCornerArcLength - topOuterCornerRadius + cradle.totalLength - cradle.totalWidth
            } else -> {
                cradle.startXPos + (x - cradle.startXPos * cradle.totalLength / cradle.totalWidth)
            }
        }

        private fun updatePath() {
            path.rewind()
            pathMeasure.setPath(drawable.path, false)
            pathMeasure.getSegment(startDistance, startDistance + width, path, true)
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
 * descendant of the BottomAppBarWithIndicator. The XML attributes indicatorHeight
 * and indicatorWidth are used to define the dimensions of the indicator.
 * Besides a solid color tint, the indicator can also be painted with a Shader
 * object using the property indicatorGradient.
 */