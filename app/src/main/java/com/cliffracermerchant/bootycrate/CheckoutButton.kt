/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.view.doOnNextLayout

class CheckoutButton(context: Context, attrs: AttributeSet) :
    OutlinedGradientButton(context, attrs)
{
    private val normalText = context.getString(R.string.checkout_description)
    private val confirmText = context.getString(R.string.checkout_confirm_description)
    private var disabledOverlayView: View? = null

    // isDisabled violates the usual rule of not naming boolean variables
    // in the negative to avoid conflicting with View.isEnabled.
    var isDisabled = false
        set(disabled) { field = disabled
                        disabledOverlayView?.visibility = if (disabled) View.VISIBLE
                                                          else          View.GONE }

    init {
        text = normalText
        val a = context.obtainStyledAttributes(attrs, R.styleable.CheckoutButton)
        val disabledOverlayViewId = a.getResourceId(R.styleable.CheckoutButton_disabledOverlayResId, 0)
        a.recycle()
        doOnNextLayout {
            disabledOverlayView = rootView.findViewById(disabledOverlayViewId)
            if (!isDisabled) disabledOverlayView?.alpha = 0f
        }

        val strokeWidth = outlineDrawable.strokeWidth
        _outlineDrawable = ClippedGradientVectorDrawable(120f, 46f,
            context.getString(R.string.checkout_button_outline_path_data),
            context.getString(R.string.checkout_button_outline_clip_path_data))
        outlineDrawable.strokeWidth = strokeWidth
        outlineDrawable.style = Paint.Style.STROKE
        (this.background as LayerDrawable).setId(1, 1)
        (this.background as LayerDrawable).setDrawableByLayerId(1, outlineDrawable)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (!isDisabled) return
        val disabledOverlay = disabledOverlayView ?: return
        disabledOverlay.visibility = when { visibility == View.GONE -> View.GONE
                                            isDisabled ->              View.VISIBLE
                                            else ->                    View.INVISIBLE }
    }
}