/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * A view that can be used as a bottom navigation drawer when included in a CoordinatorLayout.
 *
 * BottomNavigationDrawer is a custom view that can act as a bottom navigation
 * drawer for an app when included in its top-level CoordinatorLayout. The
 * drawer manages its own BottomSheetBehavior instance, and does not need one
 * to be set in XML. It will read the value of the XML attribute behavior_peekHeight,
 * and use this value as its base peek height. If the system bottom gesture
 * overlaps with the BottomNavigationDrawer in its collapsed state, it will
 * attempt to increase its peek height above the base peek height so that the
 * effective touch target size matches the peek height set in XML, up to the
 * value of the XML attribute maxPeekHeight. The peek height can also be
 * accessed and manually adjusted through the property peekHeight.
 *
 * BottomNavigationDrawer provides a new bottom sheet API in place of the one
 * provided by BottomSheetBehavior. Rather than calling setState, the state is
 * changed through the functions expand, collapse, hide, and show. The
 * drawer's hideability is controlled through the property isHideable, which
 * can only be set in XML using the XML attribute by the same name. If set to
 * IsHideable.Yes or IsHideable.No, this will behave in the same way as the
 * BottomSheetBehavior property isHideable. If set to IsHideable.OnlyByApp,
 * the drawer still collapses when using the functions hide and show, but will
 * not be hideable by the user swiping down.
 *
 * If the drawer is not hidden, the functions expand and collapse will change
 * the current state to the corresponding BottomSheetBehavior states STATE_EXPANDED
 * and STATE_COLLAPSED, respectively. If isHideable is not equal to isHideable.No,
 * the functions hide and show will set the current state to the corresponding
 * BottomSheetBehavior states STATE_HIDDEN and STATE_COLLAPSED, respectively.
 *
 * BottomSheetCallbacks can be added to the drawer's BottomSheetBehavior using
 * the function addBottomSheetCallback. BottomNavigationDrawer also adds the
 * public val targetState, which will be equal to either BottomSheetBehavior.STATE_COLLAPSED,
 * STATE_EXPANDED, or STATE_HIDDEN depending on which state the drawer is
 * moving towards.
 */
class BottomNavigationDrawer(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val behavior = BottomSheetBehavior<BottomNavigationDrawer>()
    var targetState = BottomSheetBehavior.STATE_COLLAPSED
        private set

    enum class IsHideable { Yes, No, OnlyByApp }
    val isHideable: IsHideable

    var peekHeight get() = behavior.peekHeight
                   set(value) { behavior.peekHeight = value }

    init {
        var a = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.behavior_peekHeight))
        peekHeight = a.getDimensionPixelSize(0, 0)

        a = context.obtainStyledAttributes(attrs, R.styleable.BottomNavigationDrawer)
        val maxPeekHeight = a.getDimensionPixelSize(R.styleable.BottomNavigationDrawer_maxPeekHeight,
                                                    Integer.MAX_VALUE)
        isHideable = IsHideable.values()[a.getInt(R.styleable.BottomNavigationDrawer_isHideable, 0)]
        behavior.isHideable = isHideable == IsHideable.Yes
        a.recycle()

        behavior.addBottomSheetCallback(BottomNavDrawerSheetCallback())

        var systemBottomGestureHeight = 0
        var statusBarHeight = 0
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
            systemBottomGestureHeight = insets.bottom
            insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            statusBarHeight = insets.top
            windowInsets
        }

        doOnNextLayout {
            adjustBottomNavDrawerPeekHeight(context, systemBottomGestureHeight,
                                            statusBarHeight, maxPeekHeight)
            (layoutParams as CoordinatorLayout.LayoutParams).behavior = behavior
        }
    }

    fun expand() {
        if (behavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            targetState = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    fun collapse() {
        if (behavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            targetState = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    fun hide() {
        if (isHideable == IsHideable.No) return

        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        targetState = BottomSheetBehavior.STATE_HIDDEN
    }

    private var unhiding = false
    fun show() {
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        targetState = BottomSheetBehavior.STATE_COLLAPSED
        if (isHideable == IsHideable.OnlyByApp)
            unhiding = true
    }

    fun addBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback) =
        behavior.addBottomSheetCallback(callback)

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
    private fun adjustBottomNavDrawerPeekHeight(
        context: Context,
        gestureHeight: Int,
        statusBarHeight: Int,
        maxPeekHeight: Int
    ) {
        if (gestureHeight == 0) return

        val displaySize = Point()
        val display = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
                       else (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay)
                      ?: (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        display.getRealSize(displaySize)

        val collapsedBottom = context.resources.displayMetrics.heightPixels + statusBarHeight
        val gestureTop = displaySize.y - gestureHeight
        val overlap = (collapsedBottom - gestureTop).coerceAtLeast(0)

        behavior.peekHeight = (peekHeight + overlap).coerceAtMost(maxPeekHeight)
    }

    private inner class BottomNavDrawerSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED && unhiding) {
                behavior.isHideable = false
                unhiding = false
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) { }
    }

    val isExpanded get() = behavior.state == BottomSheetBehavior.STATE_EXPANDED
    val isCollapsed get() = behavior.state == BottomSheetBehavior.STATE_COLLAPSED
    val isDragging get() = behavior.state == BottomSheetBehavior.STATE_DRAGGING
    val isSettling get() = behavior.state == BottomSheetBehavior.STATE_SETTLING
    val isHidden get() = behavior.state == BottomSheetBehavior.STATE_HIDDEN
}