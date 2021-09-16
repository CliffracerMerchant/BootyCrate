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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.databinding.BottomNavigationDrawerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomNavigationDrawer(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
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
                        ui.bottomNavigationBar.isVisible = slideOffset != 1f
                        ui.bottomNavigationBar.alpha = 1f - slideOffset
                        ui.cradleLayout.isVisible = slideOffset != 1f
                        ui.cradleLayout.alpha = 1f - slideOffset
                        ui.appTitle.isVisible = slideOffset != 0f
                        ui.appTitle.alpha = slideOffset
                        ui.settingsButton.isVisible = slideOffset != 0f
                        ui.settingsButton.alpha = slideOffset

                        ui.bottomAppBar.interpolation = 1f - slideOffset
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