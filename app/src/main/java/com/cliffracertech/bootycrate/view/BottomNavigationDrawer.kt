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
import androidx.core.view.*
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.BottomNavigationDrawerBinding
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomNavigationDrawer(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val behavior = BottomSheetBehavior<BottomNavigationDrawer>()
    private val basePeekHeight: Int

    val ui = BottomNavigationDrawerBinding.inflate(LayoutInflater.from(context), this)
    var peekHeight get() = behavior.peekHeight
                   set(value) { behavior.peekHeight = value }

    var onStateChangedListener: ((View, Int) -> Unit)? = null
    var onSlideListener: ((View, Float) -> Unit)? = null

    init {
        val a = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.behavior_peekHeight))
        val defaultPeekHeight = ui.appTitle.layoutParams.run { height + marginTop }
        basePeekHeight = a.getDimensionPixelSize(0, defaultPeekHeight)
        a.recycle()

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
        behavior.peekHeight = (basePeekHeight + overlap).coerceAtMost(contentTop)
    }

    private inner class BottomNavDrawerSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED && showing) {
                behavior.isHideable = false
                showing = false
            }
            onStateChangedListener?.invoke(bottomSheet, newState)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            ui.appTitle.isVisible = slideOffset != 0f
            ui.settingsButton.isVisible = slideOffset != 0f
            ui.appTitle.alpha = slideOffset
            ui.settingsButton.alpha = slideOffset
            onSlideListener?.invoke(bottomSheet, slideOffset)
        }
    }
}