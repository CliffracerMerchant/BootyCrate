/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.databinding.MainActivityBinding

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
fun MainActivityBinding.showCheckoutButtonAnimation(
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