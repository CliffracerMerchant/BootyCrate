/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.BottomAppBarBinding
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.applyConfig
import com.cliffracertech.bootycrate.utils.layoutTransition
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.math.MathUtils.lerp
import com.google.android.material.shape.*
import kotlin.math.*

/**
 * A custom toolbar that has a cutout in its top edge to hold the contents of a layout.
 *
 * BottomAppBar is a FrameLayout that is intended to hold a inner BottomNavigationView
 * instance. It also draws a cutout in its top edge that can be used to hold
 * the contents of a child layout, and it draws an indicator along its top edge
 * (taking into account the cradle cutout) that can be set to match the
 * position of the navigation view child's active item. The layout that holds
 * the cradle contents must be added as a child of the BottomAppBar in XML),
 * and the value of the BottomAppBar XML attribute cradleLayoutResId must be
 * equal to the id of the child cradle layout that will be contained in the
 * cradle cutout. Likewise, the BottomNavigationView must be a child of the
 * BottomAppBar, and must referenced by the XML attribute navViewResId.
 *
 * BottomAppBar contains a member, cradle, that holds the values (e.g. width)
 * that describe how it draws its cradle around the cradle layout contents. See
 * the inner class Cradle documentation for a description of these properties
 * and the corresponding XML attributes that they are initialized with.
 * Likewise, the values relating to the navigation indicator are held in the
 * member navIndicator, an instance of the inner class NavIndicator. The nav
 * indicator is moved to new items using the NavIndicator function moveToItem.
 *
 * In addition to the cradle cutout, BottomAppBar also styles its top left
 * and top right corners with rounded corners. The property interpolation
 * describes how it will draw its cradle and its top outer corners. An
 * interpolation value of 1f will result in a fully drawn cradle and top outer
 * corners with a radius equal to the value of the XML attribute
 * topOuterCornerStartRadius, while a value of 0f will result in a flat line
 * where the cradle would be and top outer corners with a radius equal to the
 * value of the XML attr topOuterCornerEndRadius.
 *
 * Like the Material library BottomAppBar, BottomAppBar manages its own
 * background. In order to tint the background a solid color, the property
 * backgroundTint can be set in XML or programmatically to an int color code.
 * Alternatively, the background can be set to a Shader instance using the
 * property backgroundGradient.
 */
open class BottomAppBar(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private val rect = Rect()
    private val drawable = PathDrawable()
    private var pathIsDirty = true
        set(value) { field = value; invalidate() }

    val cradle = Cradle()
    val navIndicator = NavIndicator()
    var interpolation = 1f
        set(value) { field = value; pathIsDirty = true}
    var topOuterCornerStartRadius = 0f
        set(value) { field = value; pathIsDirty = interpolation != 1f }
    var topOuterCornerEndRadius = 0f
        set(value) { field = value; pathIsDirty = interpolation != 0f }
    val topOuterCornerRadius get() =
        lerp(topOuterCornerEndRadius, topOuterCornerStartRadius, interpolation)

    var backgroundTint: Int get() = drawable.paint.color
                            set(value) { drawable.paint.color = value }
    var backgroundGradient get() = drawable.paint.shader
                           set(value) { drawable.paint.shader = value }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)
        val cradleLayoutResId = a.getResourceIdOrThrow(R.styleable.BottomAppBar_cradleLayoutResId)
        val navBarResId = a.getResourceIdOrThrow(R.styleable.BottomAppBar_navViewResId)
        backgroundTint = a.getColor(R.styleable.BottomAppBar_backgroundTint,
                                    ContextCompat.getColor(context, android.R.color.white))
        interpolation = a.getFloat(R.styleable.BottomAppBar_interpolation, 1f)
        topOuterCornerStartRadius = a.getDimension(R.styleable.BottomAppBar_topOuterCornerStartRadius, 0f)
        topOuterCornerEndRadius = a.getDimension(R.styleable.BottomAppBar_topOuterCornerEndRadius, 0f)

        cradle.depth = a.getDimension(R.styleable.BottomAppBar_cradleDepth, 0f)
        cradle.topCornerRadius = a.getDimension(R.styleable.BottomAppBar_cradleTopCornerRadius, 0f)
        cradle.bottomCornerRadius = a.getDimension(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0f)
        cradle.contentsMargin = a.getDimension(R.styleable.BottomAppBar_cradleContentsMargin, 0f)
        
        navIndicator.height = a.getDimension(R.styleable.BottomAppBar_navIndicatorHeight, 0f)
        navIndicator.width = a.getDimension(R.styleable.BottomAppBar_navIndicatorWidth, 0f)
        navIndicator.alpha = a.getFloat(R.styleable.BottomAppBar_navIndicatorAlpha, 1f)
        navIndicator.tint = a.getColor(R.styleable.BottomAppBar_navIndicatorTint,
                                       ContextCompat.getColor(context, android.R.color.black))
        a.recycle()
        setWillNotDraw(false)
        background = drawable
        doOnNextLayout {
            cradle.initLayout(cradleLayoutResId)
            navIndicator.initNavView(navBarResId)
            invalidate()
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

    override fun invalidate() {
        if (pathIsDirty && cradle.layout != null) {
            val topOuterCornerRadius = this.topOuterCornerRadius
            drawable.path.run {
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
                pathIsDirty = false
            }
        }
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        navIndicator.draw(canvas)
    }

    /**
     * A state holder that stores values that describe how to draw a cradle
     * cutout on a path.
     *
     * Cradle stores the following publicly accessible values that describe how
     * it draws its cradle on a Path instance:

     * - width: Initialized using the XML attribute cradleWidth.
     *      The contents width of the cradle. This value is publicly accessible
     *      so that it can be used in animations.
     * - depth: Initialized using the XML attribute cradleDepth.
     *      The full depth of the cradle, when interpolation is 1f.
     * - contentsMargin: Initialized using the XML attribute cradleContentsMargin.
     *      The margin in between the cradle cutout and its contents.
     * - topCornerRadius: Initialized using the XML attribute cradleTopCornerRadius.
     *      The radius of the top-left and top-right corners of the cradle cutout.
     * - bottomCornerRadius: Initialized using the XML attribute cradleBottomCornerRadius.
     *      The radius of the bottom-left and bottom-right corners of the cradle cutout.
    */
    inner class Cradle {
        var layout: ViewGroup? = null
            private set

        var width = 0f
            set(value) { field = value; pathIsDirty = true }
        var depth = 0f
            set(value) { field = value; pathIsDirty = true }
        var contentsMargin = 0f
            set(value) { field = value; pathIsDirty = true }
        var topCornerRadius = 0f
            set(value) { field = value; pathIsDirty = true }
        var bottomCornerRadius = 0f
            set(value) { field = value; pathIsDirty = true }

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
        var interpedTopRadius = 0.0
            private set
        var interpedBotRadius = 0.0
            private set

        val bottomWidth get() = rightCurveStartX - leftCurveEndX
        val topCurveLength get() = (interpedTopRadius * theta).toFloat()
        val bottomCurveLength get() = (interpedBotRadius * theta).toFloat()

        fun initLayout(cradleLayoutResId: Int) {
            layout = findViewById(cradleLayoutResId) ?:
                throw IllegalArgumentException("No ViewGroup was found that matched the" +
                                               "cradle layout resource id $cradleLayoutResId")
            layout?.requestLayout()
        }

        private val decelerate = DecelerateInterpolator()
        fun addTo(path: Path) = path.apply {
            // If the interpolation is sufficiently small, we can just draw a straight line instead.
            if (interpolation < 0.01f) {
                leftCurveStartX = this@BottomAppBar.width / 2f
                leftCurveInflectionX = leftCurveStartX
                leftCurveEndX = leftCurveStartX
                rightCurveStartX = leftCurveStartX
                rightCurveInflectionX = leftCurveStartX
                rightCurveEndX = leftCurveStartX
                return@apply
            }

            val fullWidth = width + 2 * contentsMargin
            // The cradle width is interpolated down to 90% of its full width
            val startEndAdjust = fullWidth * 0.1f * (1f - interpolation)

            // start will be the x coordinate of the start of the cradle if topCornerRadius is zero
            val layoutX = layout?.x ?: return@apply
            val start = layoutX - contentsMargin + startEndAdjust
            val end = start + fullWidth - 2 * startEndAdjust

            // yDistance is the y distance covered by both the top and bottom curves together.
            val yDistance = depth * interpolation
            val topRadiusFraction = topCornerRadius / (topCornerRadius + bottomCornerRadius)
            val topYDistance = yDistance * topRadiusFraction

            // Because the y distances covered by the top and bottom curves varies in a non-linear
            // fashion with respect to the interpolation value, passing the interpolation value
            // through a decelerate interpolator results in a more linear change.
            val adjustedInterp = decelerate.getInterpolation(interpolation).toDouble()

            // The top and bottom radius values are divided by the interpolation value so that
            // they increase to infinity as the interpolation approaches zero. This gives the
            // appearance of circular arcs that flatten as interpolation approaches zero.
            interpedTopRadius = topCornerRadius / adjustedInterp
            interpedBotRadius = bottomCornerRadius / adjustedInterp

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

            lineTo(start - topCornerRadius, 0f)
            arcTo(centerX =    start - topXDistance,
                  centerY =    interpedTopRadius.toFloat(),
                  radius =     interpedTopRadius.toFloat(),
                  startAngle = angleUp,
                  sweepAngle = thetaDegrees)
            arcTo(centerX =    start + botXDistance,
                  centerY =    yDistance - interpedBotRadius.toFloat(),
                  radius =     interpedBotRadius.toFloat(),
                  startAngle = angleDown + thetaDegrees,
                  sweepAngle = -thetaDegrees)
            arcTo(centerX =    end - botXDistance,
                  centerY =    yDistance - interpedBotRadius.toFloat(),
                  radius =     interpedBotRadius.toFloat(),
                  startAngle = angleDown,
                  sweepAngle = -thetaDegrees)
            arcTo(centerX =    end + topXDistance,
                  centerY =    interpedTopRadius.toFloat(),
                  radius =     interpedTopRadius.toFloat(),
                  startAngle = angleUp - thetaDegrees,
                  sweepAngle = thetaDegrees)

            leftCurveStartX = start - topXDistance
            leftCurveInflectionX = start
            leftCurveEndX = start + botXDistance

            rightCurveStartX = end - botXDistance
            rightCurveInflectionX = end
            rightCurveEndX = end + topXDistance
        }
    }

    /** A state holder that stores values that describe how to draw the navigation indicator.
     *
     * NavIndicator provides the function moveToItem that, when called with the id
     * of an item in the outer BottomAppBar's navigationView member, will animate
     * the indicator from its current location to a new location that is above the
     * menu item. It also stores the following publicly accessible values that
     * describe how it draws its indicator on a BottomAppBar instance:
     *
     * - animatorConfig: Not initializable through XML.
     *      If not null, the value of animatorConfig will be used in indicator
     *      animations when calling moveToNavBarItem with animate == true.
     * - width: Initialized using the XML attribute navIndicatorWidth.
     *      The width of the indicator.
     * - height: Initialized using the XML attribute navIndicatorHeight.
     *      The height / thickness of the indicator.
     * - alpha: Initialized using the XML attribute navIndicatorAlpha.
     *      The alpha value used when drawing the indicator, with a valid range of 0f - 1f.
     * - tint: Initialized using the XML attribute navIndicatorTint.
     *      The tint used when drawing the indicator. tint will
     *      be overridden by gradient if it is not null.
     * - gradient: Not initializable through XML.
     *      The gradient used when drawing the indicator. gradient will
     *      override the value of tint if gradient is not null.
     */
    inner class NavIndicator {
        private val path = Path()
        private val pathMeasure = PathMeasure()
        private val paint = Paint().apply { style = Paint.Style.STROKE
                                            strokeCap = Paint.Cap.ROUND }
        private var navView: BottomNavigationView? = null

        var animatorConfig: AnimatorConfig? = null
        var alpha get() = paint.alpha / 255f
                  set(value) { paint.alpha = (value * 255).roundToInt().coerceIn(0, 255) }
        private var startDistance = 0f
            set(value) { field = value; updatePath() }
        var width = 0f
        var height get() = paint.strokeWidth
                   set(value) { paint.strokeWidth = value }

        var tint: Int get() = paint.color
                      set(value) { paint.color = value }
        var gradient: Shader? get() = paint.shader
                              set(value) { paint.shader = value }

        fun draw(canvas: Canvas?) = canvas?.drawPath(path, paint)

        fun initNavView(navViewResId: Int) {
            navView = findViewById(navViewResId)
            cradle.layout?.doOnNextLayout {
                invalidate()
                doOnNextLayout {
                    moveToItem(navView?.selectedItemId ?: -1, animate = false)
                }
            }
        }

        private var selectedMenuItemId = -1
        /** Move the indicator to be above the item with id equal to @param
         * menuItemId, animating the change if @param animate == true. */
        fun moveToItem(menuItemId: Int, animate: Boolean = true) {
            if (selectedMenuItemId == menuItemId) return
            navView?.findViewById<View>(menuItemId)?.let {
                it.getGlobalVisibleRect(rect)
                val newXPos = rect.centerX() - width / 2f
                val targetStartDistance = findDistanceForX(newXPos)
                if (!animate) startDistance = targetStartDistance
                else ValueAnimator.ofFloat(startDistance, targetStartDistance).apply {
                    addUpdateListener { startDistance = animatedValue as Float }
                    applyConfig(animatorConfig)
                }.start()
                selectedMenuItemId = menuItemId
            }
        }

        private fun findDistanceForX(x: Float): Float {
            val topOuterCornerRadius = this@BottomAppBar.topOuterCornerRadius
            var distance = 0f

            if (x < topOuterCornerRadius) {
                val theta = acos(1.0 - x / topOuterCornerRadius)
                return topOuterCornerRadius * theta.toFloat()
            } else distance += topOuterCornerRadius * (Math.PI / 2).toFloat()

            if (x < cradle.leftCurveStartX)
                return distance + (x - topOuterCornerRadius)
            else distance += (cradle.leftCurveStartX - topOuterCornerRadius)

            if (x < cradle.leftCurveInflectionX) {
                val theta = Math.PI / 2 - acos((x - cradle.leftCurveStartX) / cradle.interpedTopRadius)
                return distance + (cradle.interpedTopRadius * theta).toFloat()
            } else distance += cradle.topCurveLength

            if (x < cradle.leftCurveEndX) {
                val theta = acos(1.0 - (x - cradle.leftCurveInflectionX) / cradle.interpedBotRadius)
                return distance + (cradle.interpedBotRadius * theta).toFloat()
            } else distance += cradle.bottomCurveLength

            if (x < cradle.rightCurveStartX)
                return distance + (x - cradle.leftCurveEndX)
            else distance += abs(cradle.bottomWidth)
            // cradle.bottomWidth can sometimes be negative if the bottom
            // corner radius is larger than half of the cradle.width

            if (x < cradle.rightCurveInflectionX) {
                val theta = Math.PI / 2 - acos((x - cradle.rightCurveStartX) / cradle.interpedBotRadius)
                return distance + (cradle.interpedBotRadius * theta).toFloat()
            } else distance += cradle.bottomCurveLength

            if (x < cradle.rightCurveEndX) {
                val theta = (Math.PI / 2 - acos((x - cradle.rightCurveInflectionX) / cradle.interpedTopRadius))
                return distance + (cradle.interpedTopRadius * theta).toFloat()
            } else distance += cradle.topCurveLength

            val topRightCornerStartX = this@BottomAppBar.width - topOuterCornerRadius
            if (x < topRightCornerStartX)
                return distance + (x - cradle.rightCurveEndX)
            else distance += (this@BottomAppBar.width - topOuterCornerRadius) - x

            if (x < this@BottomAppBar.width) {
                val theta = Math.PI / 2 - acos((x - topRightCornerStartX) / topOuterCornerRadius)
                return topOuterCornerRadius * theta.toFloat()
            } else distance += topOuterCornerRadius * (Math.PI / 2).toFloat()

            return distance
        }

        private fun updatePath() {
            if (interpolation != 1f) return
            path.rewind()
            pathMeasure.setPath(drawable.path, false)
            pathMeasure.getSegment(startDistance, startDistance + width, path, true)
            invalidate()
        }
    }
}


/**
 * A BottomAppBar with a custom layout containing a BottomNavigationView with
 * buttons for a ShoppingListFragment and an InventoryFragment, and a cradle
 * layout that contains a checkout button and an add button.
 *
 * Showing or hiding the checkout button should only be performed with the
 * function showCheckoutButton so that the bottom app bar can update its
 * cradle's width appropriately. The shopping list badge can be displayed with
 * the a number of new shopping list items using the function
 * updateShoppingListBadge.
 */
class BootyCrateBottomAppBar(context: Context, attrs: AttributeSet) :
    BottomAppBar(context, attrs)
{
    val ui = BottomAppBarBinding.inflate(LayoutInflater.from(context), this)
    var animatorConfig: AnimatorConfig? = null
        set(value) {
            field = value
            ui.cradleLayout.layoutTransition = layoutTransition(value)
            ui.checkoutButton.animatorConfig = value
            navIndicator.animatorConfig = value
        }

    private val checkoutButtonClipBounds = Rect()
    /** Show or hide the checkout button according to the value of @param
     * showing, returning the animator used if @param animate == true or
     * null otherwise. */
    fun showCheckoutButton(
        showing: Boolean,
        animate: Boolean = true
    ): Animator? {
        if (ui.checkoutButton.isVisible == showing)
            return null

        ui.checkoutButton.isVisible = showing
        val cradleNewWidth =
            if (!showing)
                ui.addButton.layoutParams.width.toFloat()
            else {
                val wrapContent = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                ui.cradleLayout.measure(wrapContent, wrapContent)
                ui.cradleLayout.measuredWidth.toFloat()
            }
        if (!animate) {
            cradle.width = cradleNewWidth
            return null
        }

        // Ideally we would only animate the cradle's width, and let the cradleLayout's
        // layoutTransition handle the rest. Unfortunately it will only animate its own
        // translationX if it has animateParentHierarchy set to true. Due to this
        // causing the BottomNavigationDrawer to jump to the top of the screen whenever
        // the layoutTransition animates (apparently a bug with BottomSheetBehavior),
        // we have to animate the cradleLayout's translationX ourselves.
        val cradleNewLeft = (width - cradleNewWidth) / 2
        val cradleStartTranslationX = ui.cradleLayout.left - cradleNewLeft

        // The checkoutButton's clipBounds is set here to prevent it's right edge
        // from sticking out underneath the addButton during the animation
        ui.checkoutButton.getDrawingRect(checkoutButtonClipBounds)
        val addButtonHalfWidth = ui.addButton.width / 2

        return ValueAnimator.ofFloat(cradle.width, cradleNewWidth).apply {
            applyConfig(animatorConfig)
            addUpdateListener {
                ui.cradleLayout.translationX = cradleStartTranslationX * (1f - it.animatedFraction)
                cradle.width = it.animatedValue as Float
                checkoutButtonClipBounds.right = ui.addButton.x.toInt() + addButtonHalfWidth
                ui.checkoutButton.clipBounds = checkoutButtonClipBounds
                invalidate()
            }
            doOnEnd { ui.checkoutButton.clipBounds = null }
        }
    }

    private var accumulatedNewItemCount = 0
    /** Display the shopping list added items badge with a delayed fade out
     * animation. The displayed amount will at first be equal to @param
     * shoppingListSizeChange, but will accumulate if updateShoppingListBadge
     * is called again before the fade out has finished. */
    fun updateShoppingListBadge(shoppingListSizeChange: Int) {
        accumulatedNewItemCount += shoppingListSizeChange
        if (accumulatedNewItemCount == 0) return

        val badgeParent = ui.shoppingListBadge.parent as? View
        if (badgeParent?.isVisible != true) return

        ui.shoppingListBadge.isVisible = true
        ui.shoppingListBadge.alpha = 1f
        ui.shoppingListBadge.text = context.getString(R.string.shopping_list_badge_text,
                                                      accumulatedNewItemCount)
        ui.shoppingListBadge.animate().alpha(0f)
            .setDuration(1000).setStartDelay(1500)
            .withEndAction { accumulatedNewItemCount = 0 }
            .withLayer().start()
    }
}