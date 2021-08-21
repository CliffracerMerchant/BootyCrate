/*
 * Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracertech.bootycrate.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import com.cliffracertech.bootycrate.databinding.BottomNavigationDrawerBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomNavigationDrawer(context: Context, attrs: AttributeSet) : AppBarLayout(context, attrs) {
    val ui = BottomNavigationDrawerBinding.inflate(LayoutInflater.from(context), this)

    init {

//        behavior.expandedOffset = (resources.displayMetrics.heightPixels * 3f / 4f).toInt()
//        behavior.isDraggable = true
//        behavior.isHideable = false
//        setExpanded(true)
//        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        doOnNextLayout {
            val behavior = BottomSheetBehavior.from(this)
            (layoutParams as CoordinatorLayout.LayoutParams).apply {
                behavior.peekHeight = ui.bottomAppBar.layoutParams.height
                behavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {

                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        ui.navBarAppTitleSwitcher.secondViewFraction = slideOffset
                        ui.bottomAppBar.cradleInterpolation = 1f - slideOffset
                        ui.cradleLayout.alpha = 1f - slideOffset
                        ui.cradleLayout.scaleX = 1f - 0.1f * slideOffset
                        ui.cradleLayout.scaleY = 1f - 0.1f * slideOffset
                        ui.cradleLayout.translationY = ui.cradleLayout.height * -0.8f * slideOffset
                    }
                })
                this.behavior = behavior
            }
        }
    }
}