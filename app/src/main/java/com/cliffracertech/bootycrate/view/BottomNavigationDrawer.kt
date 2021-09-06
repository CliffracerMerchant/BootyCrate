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
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import com.cliffracertech.bootycrate.databinding.BottomNavigationDrawerBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomNavigationDrawer(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    val ui = BottomNavigationDrawerBinding.inflate(LayoutInflater.from(context), this)

    init {
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
                        ui.bottomAppBar.indicatorAlpha = 1f - slideOffset
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