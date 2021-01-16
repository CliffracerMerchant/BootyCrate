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
    override var backgroundGradient get() = getDrawableLayer(backgroundLayer = true)?.gradient
                                    set(value) { getDrawableLayer(backgroundLayer = true)?.gradient = value }
    override var outlineGradient get() = getDrawableLayer(backgroundLayer = false)?.gradient
                                 set(value) { getDrawableLayer(backgroundLayer = false)?.gradient = value
                                              paint.shader = value }
    private val normalText = context.getString(R.string.checkout_description)
    private val confirmText = context.getString(R.string.checkout_confirm_description)
    private var disabledOverlayView: View? = null

    var isDisabled = false
        set(disabled) { field = disabled
                        disabledOverlayView?.visibility = if (disabled) View.VISIBLE
                                                          else          View.GONE }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CheckoutButton)
        val disabledOverlayViewId = a.getResourceId(R.styleable.CheckoutButton_disabledOverlayResId, 0)
        a.recycle()
        text = normalText
        val strokeWidth = outlineDrawable?.strokeWidth ?: 0f
        val background = GradientVectorDrawable(
            120f, 46f, context.getString(R.string.checkout_button_background_path_data))
        val outline = ClippedGradientVectorDrawable(120f, 46f,
            context.getString(R.string.checkout_button_outline_path_data),
            context.getString(R.string.checkout_button_outline_clip_path_data))

        outline.strokeWidth = strokeWidth
        outline.style = Paint.Style.STROKE
        this.background = LayerDrawable(arrayOf(background, outline))

        doOnNextLayout {
            disabledOverlayView = rootView.findViewById(disabledOverlayViewId)
            if (isEnabled) disabledOverlayView?.alpha = 0f
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (!isDisabled) return
        val disabledOverlay = disabledOverlayView ?: return
        disabledOverlay.visibility = when { visibility == View.GONE -> View.GONE
                                            isDisabled ->              View.VISIBLE
                                            else ->                    View.INVISIBLE }
    }

    private fun getDrawableLayer(backgroundLayer: Boolean) = try {
        val index = if (backgroundLayer) 0 else 1
        val background = this.background as? LayerDrawable
        (background?.getDrawable(index) as? GradientVectorDrawable)
    } catch (e: IndexOutOfBoundsException) { null }
}