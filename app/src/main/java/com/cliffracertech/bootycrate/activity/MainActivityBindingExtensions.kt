/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.*
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.GradientBuilder
import com.cliffracertech.bootycrate.utils.applyConfig
import com.cliffracertech.bootycrate.utils.resolveIntAttribute
import com.cliffracertech.bootycrate.view.ActionBarTitle
import com.cliffracertech.bootycrate.view.GradientVectorDrawable

/** Show the bottom app bar according to the value of the parameter show,
 * animating if the parameter animate == true, and using the AnimatorConfig
 * provided in the parameter animatorConfig for the animations duration
 * and interpolator. */
fun MainActivityBinding.showBottomAppBar(
    show: Boolean = true,
    animate: Boolean = true,
    animatorConfig: AnimatorConfig? = null
) {
    if (bottomAppBar.isVisible == show) return
    val screenHeight = bottomAppBar.resources.displayMetrics.heightPixels.toFloat()

    if (!animate) bottomAppBar.isVisible = show
    else {
        val translationAmount = screenHeight - cradleLayout.top - bottomAppBar.top
        val translationStart = if (show) translationAmount else 0f
        val translationEnd = if (show) 0f else translationAmount
        bottomAppBar.translationY = translationStart
        bottomAppBar.isVisible = true
        val anim = bottomAppBar.animate().translationY(translationEnd).applyConfig(animatorConfig)
        if (!show) anim.withEndAction {
            if (bottomAppBar.y > screenHeight)
                bottomAppBar.isVisible = false
        }
        anim.start()
    }
}

private fun MainActivityBinding.cradleWidth(showingCheckoutButton: Boolean) =
    if (!showingCheckoutButton)
        addButton.layoutParams.width
    else {
        val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        cradleLayout.measure(wrapContent, wrapContent)
        cradleLayout.measuredWidth
    }

fun MainActivityBinding.showCheckoutButton(show: Boolean) {
    checkoutButton.isVisible = show
    bottomAppBar.cradleWidth = cradleWidth(show)
}

/** Provide an animation to show or hide the checkout button, according
 * to the parameter show, using the values of the parameter animatorConfig
 * as the animation's duration and interpolator if not null. */
fun MainActivityBinding. showCheckoutButtonAnimation(
    showing: Boolean,
    animatorConfig: AnimatorConfig? = null
): Animator {
    checkoutButton.isVisible = showing
    val cradleEndWidth = cradleWidth(showing)

    // Setting the checkout button's clip bounds prevents the right corners of the
    // checkout button from sticking out underneath the add button during the animation.
    val clipBounds = Rect(0, 0, 0, checkoutButton.height)
    return ValueAnimator.ofInt(bottomAppBar.cradleWidth, cradleEndWidth).apply {
        applyConfig(animatorConfig)
        addUpdateListener {
            bottomAppBar.cradleWidth = it.animatedValue as Int
            clipBounds.right = bottomAppBar.cradleWidth - addButton.measuredWidth / 2
            checkoutButton.clipBounds = clipBounds
        }
        doOnEnd { checkoutButton.clipBounds = null }
    }
}

/**
 * Style the contents of the actionBar and bottomAppBar members to match their background gradients.
 *
 * Unfortunately many of the desired aspects of MainActivity's style (e.g. the
 * menu item icons being tinted to match the gradient background of the top and
 * bottom action bar) are impossible to accomplish in XML. initGradientStyle
 * performs additional operations to initialize the desired style. Its
 * foreground gradient (used to tint the text and icon drawables in the action
 * bar and bottom app bar) is made by creating a linear gradient using the
 * values of the XML attributes foregroundGradientColorLeft, foregroundGradientColorMiddle,
 * and foregroundGradientColorRight. The background gradient (smaller subsets
 * of which are used as the background used for the checkout and add buttons is
 * made from the colors backgroundGradientColorLeft, backgroundGradientColorMiddle,
 * and backgroundGradientColorRight.
 */
fun MainActivityBinding.initGradientStyle(activity: Activity) {
    val ui = this
    activity.run {
        val screenWidth = resources.displayMetrics.widthPixels
        val actionBarHeight = theme.resolveIntAttribute(R.attr.actionBarSize).toFloat()

        val fgColors = intArrayOf(theme.resolveIntAttribute(R.attr.foregroundGradientColorLeft),
                                  theme.resolveIntAttribute(R.attr.foregroundGradientColorMiddle),
                                  theme.resolveIntAttribute(R.attr.foregroundGradientColorRight))
        val bgColors = intArrayOf(theme.resolveIntAttribute(R.attr.backgroundGradientColorLeft),
                                  theme.resolveIntAttribute(R.attr.backgroundGradientColorMiddle),
                                  theme.resolveIntAttribute(R.attr.backgroundGradientColorRight))

        val fgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val bgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val paint = Paint()
        paint.style = Paint.Style.FILL

        val fgGradientBuilder = GradientBuilder(x2 = screenWidth.toFloat(), colors = fgColors)
        val fgGradientShader = fgGradientBuilder.buildLinearGradient()
        paint.shader = fgGradientShader
        val canvas = Canvas(fgGradientBitmap)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        val bgGradientBuilder = fgGradientBuilder.copy(colors  = bgColors)
        val bgGradientShader = bgGradientBuilder.buildLinearGradient()
        paint.shader = bgGradientShader
        canvas.setBitmap(bgGradientBitmap)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        styleActionBarContents(screenWidth, fgGradientShader, fgGradientBitmap)
        ui.bottomAppBar.backgroundGradient = bgGradientShader
        ui.bottomAppBar.indicatorGradient = fgGradientShader
        styleBottomAppBarContents(screenWidth, fgGradientBitmap, fgGradientBuilder, bgGradientBuilder)
    }
}

private fun MainActivityBinding.styleActionBarContents(screenWidth: Int,
                                                       fgGradientShader: Shader,
                                                       fgGradientBitmap: Bitmap) {
    val buttonWidth = actionBar.ui.backButton.drawable.intrinsicWidth
    var x = buttonWidth / 2
    actionBar.ui.backButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
    actionBar.ui.titleSwitcher.setShader(fgGradientShader)

    x = screenWidth - buttonWidth / 2
    actionBar.ui.menuButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
    x -= buttonWidth
    actionBar.ui.changeSortButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
    x -= buttonWidth
    actionBar.ui.searchButton.drawable?.setTint(fgGradientBitmap.getPixel(x, 0))
}

private fun MainActivityBinding.styleBottomAppBarContents(screenWidth: Int, fgGradientBitmap: Bitmap,
                                                          fgGradientBuilder: GradientBuilder,
                                                          bgGradientBuilder: GradientBuilder) {
    // Checkout button
    val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    cradleLayout.measure(wrapContent, wrapContent)
    val cradleWidth = cradleLayout.measuredWidth
    val cradleLeft = (screenWidth - cradleWidth) / 2f
    checkoutButton.foregroundGradient = fgGradientBuilder
        .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()
    checkoutButton.backgroundGradient = bgGradientBuilder
        .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()

    // Add button
    val addButtonWidth = addButton.layoutParams.width
    val addButtonLeft = cradleLeft + cradleWidth - addButtonWidth * 1f
    addButton.foregroundGradient = fgGradientBuilder
        .setX1(-addButtonLeft).setX2(screenWidth - addButtonLeft).buildLinearGradient()
    addButton.backgroundGradient = bgGradientBuilder
        .setX1(-addButtonLeft).setX2(screenWidth - addButtonLeft).buildLinearGradient()

    // Bottom navigation view
    val menuSize = bottomNavigationBar.menu.size()
    for (i in 0 until menuSize) {
        val center = ((i + 0.5f) / menuSize * screenWidth).toInt()
        val tint = ColorStateList.valueOf(fgGradientBitmap.getPixel(center, 0))
        bottomNavigationBar.getIconAt(i).imageTintList = tint
        bottomNavigationBar.setTextTintList(i, tint)
    }
    bottomNavigationBar.invalidate()
}

private fun ActionBarTitle.setShader(shader: Shader?) {
    fragmentTitleView.paint.shader = shader
    actionModeTitleView.paint.shader = shader
    searchQueryView.paint.shader = shader
    (searchQueryView.background as? GradientVectorDrawable)?.gradient = shader
}