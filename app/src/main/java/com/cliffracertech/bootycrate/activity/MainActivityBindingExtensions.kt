/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.dlog
import com.cliffracertech.bootycrate.fragment.ItemListFragment
import com.cliffracertech.bootycrate.utils.GradientBuilder
import com.cliffracertech.bootycrate.utils.resolveIntAttribute
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.abs

/**
 * Style the contents of the actionBar and bottomAppBar members to match their background gradients.
 *
 * Unfortunately many of the desired aspects of the app's style (e.g. the
 * checkout button's background bing presented as a "cutout" of the bottom app
 * bar's background gradient) are impossible to accomplish in XML. initGradientStyle
 * performs additional operations to initialize the desired style. The background
 * gradient (smaller subsets of which are used as the backgrounds for the checkout
 * and add buttons) is made from the colors backgroundGradientLeftColor,
 * backgroundGradientMiddleColor, and backgroundGradientRightColor.
 */
fun MainActivity.initGradientStyle() {
    window.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_gradient))

    val bgColors = intArrayOf(theme.resolveIntAttribute(R.attr.backgroundGradientLeftColor),
                              theme.resolveIntAttribute(R.attr.backgroundGradientMiddleColor),
                              theme.resolveIntAttribute(R.attr.backgroundGradientRightColor))
    val screenWidth = resources.displayMetrics.widthPixels
    val bgGradientBuilder = GradientBuilder(x2 = screenWidth.toFloat(), colors = bgColors)
    val bgGradientShader = bgGradientBuilder.buildLinearGradient()
    ui.bottomAppBar.backgroundGradient = bgGradientShader

    val paint = Paint()
    paint.style = Paint.Style.FILL
    paint.shader = bgGradientShader
    val bgGradientBitmap = Bitmap.createBitmap(screenWidth, 1, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bgGradientBitmap)
    canvas.drawRect(0f, 0f, screenWidth.toFloat(), 1f, paint)

    // Checkout button
    val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    ui.bottomAppBar.ui.cradleLayout.measure(wrapContent, wrapContent)
    val cradleWidth = ui.bottomAppBar.ui.cradleLayout.measuredWidth
    val cradleLeft = (screenWidth - cradleWidth) / 2f
    ui.bottomAppBar.ui.checkoutButton.backgroundGradient = bgGradientBuilder
        .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()

    // Add button
    var right = cradleLeft.toInt() + cradleWidth
    var left = right - ui.bottomAppBar.ui.addButton.layoutParams.width
    var leftColor = bgGradientBitmap.getPixel(left, 0)
    var rightColor = bgGradientBitmap.getPixel(right, 0)
    (ui.bottomAppBar.ui.addButton.background as GradientDrawable).colors =
        intArrayOf(leftColor, rightColor)

    // Add inventory button
    val marginEnd = (ui.addItemGroupButton.layoutParams as ViewGroup.MarginLayoutParams).marginEnd
    right = screenWidth - ui.itemGroupSelectorBackground.paddingEnd - marginEnd
    left = right - ui.addItemGroupButton.layoutParams.width
    leftColor = bgGradientBitmap.getPixel(left, 0)
    rightColor = bgGradientBitmap.getPixel(right, 0)
    val drawable = ui.addItemGroupButton.background.mutate() as GradientDrawable
    drawable.colors = intArrayOf(leftColor, rightColor)
}

fun MainActivityBinding.bottomSheetCallback() = object: BottomSheetBehavior.BottomSheetCallback() {
    private val Int.isSettlingOrDragging get() = this == BottomSheetBehavior.STATE_SETTLING ||
                                                 this == BottomSheetBehavior.STATE_DRAGGING
    private val Int.isExpanded get() = this == BottomSheetBehavior.STATE_EXPANDED
    private val Int.isCollapsed get() = this == BottomSheetBehavior.STATE_COLLAPSED
    private val Int.isExpandingOrExpanded get() = isExpanded || isSettlingOrDragging
    private val Int.isCollapsingOrCollapsed get() = isCollapsed || isSettlingOrDragging

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        dlog("state set to $newState")
        appTitle.isVisible = newState.isExpandingOrExpanded
        settingsButton.isVisible = newState.isExpandingOrExpanded
        itemGroupSelectorOptionsButton.isVisible = newState.isExpandingOrExpanded
        itemGroupSelector.isInvisible = newState.isCollapsed

        bottomAppBar.ui.cradleLayout.isVisible = newState.isCollapsingOrCollapsed
        bottomAppBar.ui.navigationView.isVisible = newState.isCollapsingOrCollapsed
        bottomAppBar.navIndicator.alpha = if (newState.isCollapsingOrCollapsed) 1f else 0f
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        fragmentContainer.children.forEach {
            val fragment = it.findFragment<Fragment>()
            if (fragment is ItemListFragment<*>)
                fragment.emptyListMessageView?.translationY =
                    if (slideOffset <= 0f) 0f
                    else slideOffset * -0.3f * it.height
        }

        dlog("slideOffset = $slideOffset")
        if (bottomNavigationDrawer.targetState == BottomSheetBehavior.STATE_HIDDEN)
            return
        val slide = abs(slideOffset)

        appTitle.alpha = slide
        settingsButton.alpha = slide
        itemGroupSelectorOptionsButton.alpha = slide
        itemGroupSelector.alpha = slide
        bottomAppBar.ui.navigationView.alpha = 1f - slide

        bottomAppBar.apply {
            ui.cradleLayout.apply {
                alpha = 1f - slide
                scaleX = 1f - 0.1f * slide
                scaleY = 1f - 0.1f * slide
                translationY = height * -0.9f * slide
            }
            navIndicator.alpha = 1f - slide
            interpolation = 1f - slide
        }
    }
}