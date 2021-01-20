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

/** An OutlinedGradientButton with a custom shape, disable/enable functionality, and a double tap to use functionality.
 *
 *  CheckoutButton is an OutlinedGradientButton with extra functionality that
 *  is intended to be used as the button to execute the shopping list checkout
 *  function (see ShoppingListItemDao.checkout() for more information). Its
 *  normal text reads checkout, but when tapped its text will change to indi-
 *  cate to the user that it is in a confirmatory state. An additional tap will
 *  then actually execute the callback set via the property checkoutCallback.
 *  Confirmation is requested due to the checkout function being irreversible,
 *  and because a double tap for users who know what they are doing is faster
 *  than having to answer yes to a confirmatory alert dialog. If the user does
 *  not tap the button again before the value of the constant property confirm-
 *  Timeout, the button will reset to its normal state.
 *
 *  In case the checkout functionality should be unavailable, such as when the
 *  shopping list is empty, the isDisabled property can be set to true to make
 *  the checkout button ignore onClick events until it is enabled again. If the
 *  XML property disabledOverlayResId is set to the ID of another view, then
 *  this view will be used as the disabled overlay, and will be faded in or out
 *  as the isDisabled state is changed. The disabled overlay view should there-
 *  fore look like the checkout button but in a disabled state. This method of
 *  a view overlay (rather than a simpler color changing animation) is used due
 *  to the difficulty of using color changing animations when gradient shaders
 *  are in use (there is no way to change the color of an existing Shader
 *  object, meaning that a new one with different colors would have to be made
 *  every frame of the animation, likely leading to a stuttering animation). */
class CheckoutButton(context: Context, attrs: AttributeSet) :
    OutlinedGradientButton(context, attrs)
{
    private val normalText = context.getString(R.string.checkout_description)
    private val confirmText = context.getString(R.string.checkout_confirm_description)
    private var disabledOverlayView: View? = null
    private var checkoutButtonLastPressTimeStamp = 0L
    private val confirmTimeout = 2000L
    var checkoutCallback: (() -> Unit)? = null

    // isDisabled violates the usual rule of not naming boolean variables
    // in the negative to avoid conflicting with View.isEnabled.
    var isDisabled = false
        set(disabled) { field = disabled
                        if (disabled) exitConfirmatoryState()
                        disabledOverlayView?.visibility = if (disabled) View.VISIBLE
                                                          else          View.GONE }

    init {
        text = normalText
        val a = context.obtainStyledAttributes(attrs, R.styleable.CheckoutButton)
        val disabledOverlayViewId = a.getResourceId(R.styleable.CheckoutButton_disabledOverlayResId, 0)
        a.recycle()
        doOnNextLayout {
            disabledOverlayView = rootView.findViewById(disabledOverlayViewId)
            if (!isDisabled) disabledOverlayView?.visibility = View.GONE
        }

        val strokeWidth = outlineDrawable.strokeWidth
        outlineDrawable = ClippedGradientVectorDrawable(120f, 46f,
            context.getString(R.string.checkout_button_outline_path_data),
            context.getString(R.string.checkout_button_outline_clip_path_data))
        outlineDrawable.strokeWidth = strokeWidth
        outlineDrawable.style = Paint.Style.STROKE
        (background as LayerDrawable).setId(1, 1)
        (background as LayerDrawable).setDrawableByLayerId(1, outlineDrawable)

        setOnClickListener {
            if (isDisabled) return@setOnClickListener
            val currentTime = System.currentTimeMillis()
            if (currentTime < checkoutButtonLastPressTimeStamp + confirmTimeout) {
                exitConfirmatoryState()
                checkoutCallback?.invoke()
            } else {
                text = confirmText
                checkoutButtonLastPressTimeStamp = currentTime
                handler.removeCallbacks(::exitConfirmatoryState)
                handler.postDelayed(::exitConfirmatoryState, confirmTimeout)
            }
        }
    }

    private fun exitConfirmatoryState() {
        checkoutButtonLastPressTimeStamp = 0
        text = normalText
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