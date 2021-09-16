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
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.doOnNextLayout
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.applyConfig
import com.cliffracertech.bootycrate.utils.findIndex
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
 * value of the XML attr topCornerRadii.
 *
 * Like the Material library BottomAppBar, BottomAppBar manages its own
 * background. In order to tint the background a solid color, the property
 * backgroundTint can be set in XML or programmatically to an int color code.
 * Alternatively the background can be set to a Shader instance using the
 * property backgroundGradient.
 */
open class BottomAppBar(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private var cradleLayout: ViewGroup? = null
    var interpolation = 1f
        set(value) { field = value; invalidate() }
    var topCornerRadii = 0f
        set(value) { field = value; invalidate() }
    var cradleWidth = 0
        set(value) { field = value; invalidate() }
    var cradleDepth: Int
    var cradleTopCornerRadius: Int
    var cradleBottomCornerRadius: Int
    var cradleContentsMargin: Int

    protected val drawable = PathDrawable()

    var backgroundTint: Int get() = drawable.paint.color
                            set(value) { drawable.paint.color = value }
    var backgroundGradient get() = drawable.paint.shader
                           set(value) { drawable.paint.shader = value }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)
        topCornerRadii = a.getDimension(R.styleable.BottomAppBar_topCornerRadii, 0f)
        cradleDepth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleDepth, 0)
        cradleTopCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleTopCornerRadius, 0)
        cradleBottomCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0)
        cradleContentsMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleContentsMargin, 0)
        val cradleLayoutResId = a.getResourceIdOrThrow(R.styleable.BottomAppBar_cradleLayoutResId)
        backgroundTint = a.getColor(R.styleable.BottomAppBar_backgroundTint,
                                    ContextCompat.getColor(context, android.R.color.white))
        a.recycle()

        background = drawable
        doOnNextLayout {
            cradleLayout = findViewById(cradleLayoutResId)
            invalidate()
        }
    }

    private val curvePaint = Paint().apply { style = Paint.Style.STROKE
                                             strokeWidth = 8f
                                             alpha = 30 }

    private val angleDown = 90f
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
        moveTo(0f, topCornerRadii)
        arcTo(centerX = topCornerRadii,
              centerY = topCornerRadii,
              radius = topCornerRadii,
              startAngle = angleLeft,
              sweepAngle = 90f)

        if (interpolation > 0.01f) {
            val cradleFullWidth = cradleWidth + 2 * cradleContentsMargin
            // The cradle width is interpolated down to 90% of its full width
            val startEndAdjust = cradleFullWidth * 0.1f * (1f - interpolation)
            val cradleLayoutX = cradleLayout?.x ?: return@run

            // start will be the x coordinate of the start of the cradle if cradleTopCornerRadius is zero
            val start = cradleLayoutX - cradleContentsMargin + startEndAdjust
            val end = start + cradleFullWidth - 2 * startEndAdjust
            val yDistance = cradleDepth * interpolation

            val topRadiusFraction = cradleTopCornerRadius.toFloat() / (cradleTopCornerRadius + cradleBottomCornerRadius)
            val topCurveYDistance = topRadiusFraction * yDistance

            val sweepAngleRads = Math.PI / 2 - asin(1.0 - topCurveYDistance / cradleTopCornerRadius).coerceIn(0.0, 90.0)
            val topCurveXDistance = cradleTopCornerRadius * cos(Math.PI / 2 - sweepAngleRads).toFloat()
            val bottomCurveXDistance = cradleBottomCornerRadius * cos(Math.PI / 2 - sweepAngleRads).toFloat()

            val sweepAngle = Math.toDegrees(sweepAngleRads).toFloat()

            lineTo(start - cradleTopCornerRadius, 0f)
            arcTo(centerX =    start - topCurveXDistance,
                  centerY =    cradleTopCornerRadius.toFloat(),
                  radius =     cradleTopCornerRadius.toFloat(),
                  startAngle = angleUp,
                  sweepAngle = sweepAngle)
            arcTo(centerX =    start + bottomCurveXDistance,
                  centerY =    yDistance - cradleBottomCornerRadius,
                  radius =     cradleBottomCornerRadius.toFloat(),
                  startAngle = angleDown + sweepAngle,
                  sweepAngle = -sweepAngle)
            arcTo(centerX =    end - bottomCurveXDistance,
                  centerY =    yDistance - cradleBottomCornerRadius,
                  radius =     cradleBottomCornerRadius.toFloat(),
                  startAngle = angleDown,
                  sweepAngle = -sweepAngle)
            arcTo(centerX =    end + topCurveXDistance,
                  centerY =    cradleTopCornerRadius.toFloat(),
                  radius =     cradleTopCornerRadius.toFloat(),
                  startAngle = angleUp - sweepAngle,
                  sweepAngle = sweepAngle)
    }
        lineTo(width - topCornerRadii, 0f)
        arcTo(centerX = width - topCornerRadii,
              centerY = topCornerRadii,
              radius = topCornerRadii,
              startAngle = angleUp,
              sweepAngle = 90f)
        lineTo(width.toFloat(), height.toFloat())
        lineTo(0f, height.toFloat())
        close()
        super.invalidate()
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
@Suppress("LeakingThis")
class BottomAppBarWithIndicator(context: Context, attrs: AttributeSet) :
    BottomAppBar(context, attrs)
{
    private lateinit var navBar: BottomNavigationView
    private val indicatorPaint = Paint().apply { style = Paint.Style.STROKE }
    var indicatorWidth = 0
    var indicatorHeight get() = indicatorPaint.strokeWidth
                        set(value) { indicatorPaint.strokeWidth = value }
    var indicatorTint: Int get() = indicatorPaint.color
                            set(value) { indicatorPaint.color = value }
    var indicatorGradient get() = indicatorPaint.shader
                          set(value) { indicatorPaint.shader = value }
    var indicatorAlpha get() = indicatorPaint.alpha / 255f
                       set(value) { indicatorPaint.alpha = (value * 255).toInt().coerceIn(0, 255) }
    private var indicatorXPos = 0
    var indicatorAnimatorConfig: AnimatorConfig? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBarWithIndicator)
        indicatorHeight = a.getDimension(R.styleable.BottomAppBarWithIndicator_indicatorHeight, 0f)
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
            //drawPath(topEdgePath, indicatorPaint)
        }
    }
}