/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracertech.bootycrate.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatButton
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.valueAnimatorOfInt

/**
 * A button with a vector background and icon that are both tintable with gradients.
 *
 * GradientButton is a button that uses a GradientVectorDrawable for its
 * background. The path data used for the background is defined in XML using
 * the attribute backgroundPathData. The gradient shader used for the
 * background drawable can be set through the property backgroundGradient.
 * In order for the path data attribute to be interpreted correctly, the XML
 * properties pathWidth and pathHeight must also be set in XML.
 */
open class GradientButton(context: Context, attrs: AttributeSet) : AppCompatButton(context, attrs) {
    var backgroundGradient get() = backgroundDrawable.gradient
        set(gradient) { backgroundDrawable.gradient = gradient }
    var foregroundGradient get() = iconDrawable.gradient
        set(gradient) { iconDrawable.gradient = gradient
            paint.shader = gradient }

    protected var backgroundDrawable: GradientVectorDrawable
    protected var iconDrawable: GradientVectorDrawable

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.GradientButton)
        val backgroundPathData = a.getString(R.styleable.GradientButton_backgroundPathData) ?: ""
        val iconPathData = a.getString(R.styleable.GradientButton_iconPathData) ?: ""
        val iconStrokeWidth = a.getDimension(R.styleable.GradientButton_iconStrokeWidth, 0f)
        // Apparently if more than one attr is retrieved with a manual
        // IntArray, they must be in numerical id order to work.
        // Log.d("", "R.attr.pathWidth = ${R.attr.pathWidth}, R.attr.pathHeight=${R.attr.pathHeight}")
        a = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.pathHeight, R.attr.pathWidth))
        val pathHeight = a.getFloat(0, 0f)
        @SuppressLint("ResourceType")
        val pathWidth = a.getFloat(1, 0f)
        a.recycle()

        backgroundDrawable = GradientVectorDrawable(pathWidth, pathHeight, backgroundPathData )
        iconDrawable = GradientVectorDrawable(pathWidth, pathHeight, iconPathData)
        if (iconStrokeWidth != 0f) {
            iconDrawable.strokeWidth = iconStrokeWidth
            iconDrawable.style = Paint.Style.STROKE
        }
        this.background = LayerDrawable(arrayOf(backgroundDrawable, iconDrawable))
    }
}

/**
 * A GradientButton with disable/enable functionality.
 *
 * DisableableGradientButton's override of View.setEnabled will set its
 * background's alpha value to the value of the property disabledAlpha
 * to indicate its disabled state. The value of disabledAlpha can be
 * changed programmatically, or through the XML attribute of the same
 * name. If any additional changes are desired when the isEnabledState is
 * changed, they can be defined in a subclass override of View.setEnabled.
 */
open class DisableableGradientButton(context: Context, attrs: AttributeSet) :
    GradientButton(context, attrs)
{
    var disabledAlpha: Int
    var animatorConfig: AnimatorConfig? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DisableableGradientButton)
        disabledAlpha = a.getInt(R.styleable.DisableableGradientButton_disabledAlpha, 0)
        a.recycle()
    }

    @CallSuper override fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        super.setEnabled(enabled)
        val anim = valueAnimatorOfInt(setter = background::setAlpha,
                                      fromValue = if (enabled) disabledAlpha else 255,
                                      toValue = if (enabled) 255 else disabledAlpha,
                                      config = animatorConfig)
        anim.addUpdateListener{ invalidate() }
        anim.start()
    }
}

/**
 * An button with a custom shape and a double tap to use functionality.
 *
 * CheckoutButton is a DisableableGradientButton with extra functionality
 * that is intended to be used as the button to execute the shopping list
 * checkout function (see ShoppingListItemDao.checkout() for more informa-
 * tion). Its normal text reads checkout, but when tapped its text will
 * change to indicate to the user that it is in a confirmatory state. An
 * additional tap will then actually execute the callback set via the pro-
 * perty checkoutCallback. If the user does not tap the button again
 * before the value of R.integer.checkoutButtonConfirmationTimeout, the
 * button will reset to its normal state.
 *
 * Confirmation is requested due to the checkout function being irrevers-
 * ible, and because a double tap for users who know what they are doing
 * is faster than having to answer yes to a confirmatory alert dialog.
 */
class CheckoutButton(context: Context, attrs: AttributeSet) :
    DisableableGradientButton(context, attrs)
{
    private val normalText = context.getString(R.string.checkout_description)
    private val confirmText = context.getString(R.string.checkout_confirm_description)
    private var checkoutButtonLastPressTimeStamp = 0L
    private val confirmTimeout = resources.getInteger(R.integer.checkoutButtonConfirmationTimeout).toLong()
    var checkoutCallback: (() -> Unit)? = null

    init {
        text = normalText
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