/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.databinding.BottomNavigationDrawerBinding
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomNavigationDrawer(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val behavior = BottomSheetBehavior<BottomNavigationDrawer>()

    val ui = BottomNavigationDrawerBinding.inflate(LayoutInflater.from(context), this)
    val peekHeight get() = behavior.peekHeight

    init {
        var systemBottomGestureHeight = 0
        var statusBarHeight = 0

        val rootView = context.asFragmentActivity().window.decorView
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
            systemBottomGestureHeight = insets.bottom

            insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            statusBarHeight = insets.top

            windowInsets
        }
        doOnNextLayout {
            behavior.addBottomSheetCallback(BottomNavDrawerSheetCallback())
            behavior.peekHeight = ui.bottomAppBar.height
            setBottomNavDrawerPeekHeight(context, systemBottomGestureHeight, statusBarHeight)
            (layoutParams as CoordinatorLayout.LayoutParams).behavior = behavior
        }
    }

    fun expand() { behavior.state = BottomSheetBehavior.STATE_EXPANDED }
    fun collapse() { behavior.state = BottomSheetBehavior.STATE_COLLAPSED }

    fun hide() {
        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private var showing = false
    fun show() {
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        showing = true
    }

    /**
     * Adjust the peek height of the bottom navigation drawer to prevent
     * interference with the system bottom gesture, if needed.
     *
     * If the touch target of the collapsed bottom navigation drawer overlaps
     * with the system gesture inset, we want to increase the peek height of
     * the bottom nav drawer as much as we can without showing the nav drawer
     * contents to reduce the likelihood of the bottom sheet gesture
     * interfering with the system home gesture.
     */
    private fun setBottomNavDrawerPeekHeight(
        context: Context,
        gestureHeight: Int,
        statusBarHeight: Int
    ) {
        if (gestureHeight == 0) return

        val displaySize = Point()
        val display = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
                       else (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay)
                      ?: (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        display.getRealSize(displaySize)

        val defaultBottom = context.resources.displayMetrics.heightPixels + statusBarHeight
        val gestureTop = displaySize.y - gestureHeight
        val overlap = (defaultBottom - gestureTop).coerceAtLeast(0)

        val contentTop = ui.bottomNavDrawerContentsLayout.run { top + paddingTop }
        behavior.peekHeight = (ui.bottomAppBar.height + overlap).coerceAtMost(contentTop)
    }

    private inner class BottomNavDrawerSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED && showing) {
                behavior.isHideable = false
                showing = false
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            ui.bottomNavigationView.isVisible = slideOffset != 1f
            ui.bottomAppBar.cradle.layout.isVisible = slideOffset != 1f
            ui.appTitle.isVisible = slideOffset != 0f
            ui.settingsButton.isVisible = slideOffset != 0f

            ui.bottomNavigationView.alpha = 1f - slideOffset
            ui.bottomAppBar.cradle.layout.alpha = 1f - slideOffset
            ui.bottomAppBar.navIndicator.alpha = 1f - slideOffset
            ui.bottomAppBar.cradle.layout.alpha = 1f - slideOffset
            ui.appTitle.alpha = slideOffset
            ui.settingsButton.alpha = slideOffset

            ui.bottomAppBar.cradle.interpolation = 1f - slideOffset
            ui.bottomAppBar.cradle.layout.scaleX = 1f - 0.1f * slideOffset
            ui.bottomAppBar.cradle.layout.scaleY = 1f - 0.1f * slideOffset
            ui.bottomAppBar.cradle.layout.translationY = ui.bottomAppBar.cradle.layout.height * -0.9f * slideOffset

            ui.bottomAppBar.invalidate()
        }
    }
}