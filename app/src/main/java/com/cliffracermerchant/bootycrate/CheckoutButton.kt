/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
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
    private var disabledOverlay: Drawable? = null
    private var checkoutButtonLastPressTimeStamp = 0L
    private val confirmTimeout = 2000L
    var checkoutCallback: (() -> Unit)? = null

    // isDisabled violates the usual rule of not naming boolean variables
    // in the negative to avoid conflicting with View.isEnabled.
    var isDisabled = false
        set(disabled) { if (field == disabled) return
                        field = disabled
                        if (disabled) exitConfirmatoryState()
                        val disabledOverlay = this.disabledOverlay ?: return
                        val startAlpha = if (disabled) 0 else 255
                        val endAlpha = if (disabled) 255 else 0
                        ObjectAnimator.ofInt(disabledOverlay, "alpha", startAlpha, endAlpha).apply {
                            addUpdateListener{ invalidate() }
                        }.start() }

    init {
        text = normalText
        val a = context.obtainStyledAttributes(attrs, R.styleable.CheckoutButton)
        val disabledBackgroundTint = a.getColor(R.styleable.CheckoutButton_disabledBackgroundTint, 0)
        val disabledOutlineAndTextTint = a.getColor(R.styleable.CheckoutButton_disabledOutlineAndTextTint, 0)
        a.recycle()

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

        doOnNextLayout {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val backupBackgroundGradient = backgroundGradient
            val backupOutlineGradient = outlineGradient
            backgroundGradient = null
            outlineGradient = null
            ((background as LayerDrawable).getDrawable(0) as GradientVectorDrawable).setTint(disabledBackgroundTint)
            ((background as LayerDrawable).getDrawable(1) as GradientVectorDrawable).setTint(disabledOutlineAndTextTint)
            setTextColor(disabledOutlineAndTextTint)

            draw(canvas)
            disabledOverlay = BitmapDrawable(context.resources, bitmap).apply {
                alpha = if (isDisabled) 255 else 0
                bounds = background.bounds }
            backgroundGradient = backupBackgroundGradient
            outlineGradient = backupOutlineGradient
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) disabledOverlay?.draw(canvas)
    }

    private fun exitConfirmatoryState() {
        checkoutButtonLastPressTimeStamp = 0
        text = normalText
    }
}