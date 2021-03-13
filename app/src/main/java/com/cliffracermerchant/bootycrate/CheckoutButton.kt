/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet

/**
 * An button with a custom shape and a double tap to use functionality.
 *
 * CheckoutButton is a DisableableOutlinedGradientButton with extra function-
 * ality that is intended to be used as the button to execute the shopping
 * list checkout function (see ShoppingListItemDao.checkout() for more infor-
 * mation). Its normal text reads checkout, but when tapped its text will
 * change to indicate to the user that it is in a confirmatory state. An add-
 * itional tap will then actually execute the callback set via the property
 * checkoutCallback. Confirmation is requested due to the checkout function
 * being irreversible, and because a double tap for users who know what they
 * are doing is faster than having to answer yes to a confirmatory alert
 * dialog. If the user does not tap the button again before the value of
 * R.integer.checkoutButtonConfirmationTimeout, the button will reset to its
 * normal state.
 */
class CheckoutButton(context: Context, attrs: AttributeSet) :
    DisableableOutlinedGradientButton(context, attrs)
{
    private val normalText = context.getString(R.string.checkout_description)
    private val confirmText = context.getString(R.string.checkout_confirm_description)
    private var checkoutButtonLastPressTimeStamp = 0L
    private val confirmTimeout =
        resources.getInteger(R.integer.checkoutButtonConfirmationTimeout).toLong()
    var checkoutCallback: (() -> Unit)? = null

    init {
        text = normalText

        val strokeWidth = outlineDrawable.strokeWidth
        outlineDrawable = ClippedGradientVectorDrawable(120f, 46f,
            context.getString(R.string.checkout_button_outline_path_data),
            context.getString(R.string.checkout_button_outline_clip_path_data))
        outlineDrawable.strokeWidth = strokeWidth
        outlineDrawable.style = Paint.Style.STROKE
        (background as LayerDrawable).setId(1, 1)
        (background as LayerDrawable).setDrawableByLayerId(1, outlineDrawable)

        setOnClickListener {
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

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) exitConfirmatoryState()
    }

    private fun exitConfirmatoryState() {
        checkoutButtonLastPressTimeStamp = 0
        text = normalText
    }
}