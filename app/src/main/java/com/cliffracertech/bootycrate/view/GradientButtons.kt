/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracertech.bootycrate.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.doOnNextLayout
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.valueAnimatorOfInt

/**
 * A button with a vector background and icon that are both tintable with gradients.
 *
 * OutlinedGradientButton is a button that uses GradientVectorDrawables
 * for its background and icon. The path data used for these drawables are
 * defined in XML using the attributes backgroundPathData and iconPathData.
 * The gradient shader used for the background drawable can be set through
 * the property backgroundGradient, while the gradient for the icon and
 * any text is set through the property foregroundGradient. The stroke
 * width of the icon drawable can be set through the property iconStroke-
 * Width; the icon will be drawn in Paint.FILL mode instead if the stroke
 * width is 0. In order for the path data attributes to be interpreted cor-
 * rectly, the XML properties pathWidth and pathHeight must also be set in
 * XML.
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
 * DisableableGradientButton automatically creates a disabled drawable for
 * itself based on the OutlinedGradientButton background and the XML attri-
 * butes disabledBackgroundTint and disabledIconAndTextTint. The disabled
 * drawable will look like the GradientButton background, but with these
 * alternative tint values and no gradient shaders. When the button is
 * enabled or disabled via the View property isEnabled, the button will
 * animate to or from its disabled state, using the value of the property
 * animatorConfig for the animation's duration and interpolator. Note that
 * the disabled background will not reflect any changes to the button's
 * text or icon that occur after initialization.
 *
 * If any additional changes are desired when the isEnabledState is
 * changed, they can be defined in a subclass override of View.setEnabled.
 */
open class DisableableGradientButton(context: Context, attrs: AttributeSet) :
    GradientButton(context, attrs)
{
    private var disabledOverlay: Drawable? = null
    var animatorConfig: AnimatorConfig? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DisableableGradientButton)
        val disabledBackgroundTint = a.getColor(R.styleable.DisableableGradientButton_disabledBackgroundTint, 0)
        val disabledIconAndTextTint = a.getColor(R.styleable.DisableableGradientButton_disabledIconAndTextTint, 0)
        a.recycle()

        doOnNextLayout {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val backupBackgroundGradient = backgroundGradient
            val backupForegroundGradient = foregroundGradient
            backgroundGradient = null
            foregroundGradient = null
            ((background as LayerDrawable).getDrawable(0) as GradientVectorDrawable).setTint(disabledBackgroundTint)
            ((background as LayerDrawable).getDrawable(1) as GradientVectorDrawable).setTint(disabledIconAndTextTint)
            setTextColor(disabledIconAndTextTint)

            draw(canvas)
            disabledOverlay = BitmapDrawable(context.resources, bitmap).apply {
                alpha = if (isEnabled) 0 else 255
                bounds = background.bounds
            }
            backgroundGradient = backupBackgroundGradient
            foregroundGradient = backupForegroundGradient
        }
    }

    @CallSuper override fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        super.setEnabled(enabled)
        val disabledOverlay = this.disabledOverlay ?: return
        val anim = valueAnimatorOfInt(setter = disabledOverlay::setAlpha,
            fromValue = disabledOverlay.alpha,
            toValue = if (enabled) 0 else 255,
            config = animatorConfig)
        anim.addUpdateListener{ invalidate() }
        anim.start()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) disabledOverlay?.draw(canvas)
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
    private val confirmTimeout =
        resources.getInteger(R.integer.checkoutButtonConfirmationTimeout).toLong()
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