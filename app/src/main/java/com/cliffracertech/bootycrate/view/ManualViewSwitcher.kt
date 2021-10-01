/*
 * Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracertech.bootycrate.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.doOnNextLayout

/** A container for two views that allows setting the alpha of both views at once through
 * the property secondViewFraction, or the function setSecondViewFraction.
 *
 * ManualViewSwitcher is intended to contain two views, and it enforces this behavior
 * at inflation; removing one of its two views after inflation will result in a crash
 * if the property secondViewFraction is accessed or set. */
class ManualViewSwitcher(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    var secondViewFraction get() = getChildAt(1).alpha
                           set(value) = setSecondViewFraction(value)

    @JvmName("setSecondViewFraction1")
    fun setSecondViewFraction(fraction: Float) {
        getChildAt(1).alpha = fraction
        getChildAt(0).alpha = 1f - fraction
    }

    init { doOnNextLayout {
        if (childCount != 2) throw IllegalStateException(
            "ManualViewSwitcher must have exactly two children")
        setSecondViewFraction(0f)
    }}
}