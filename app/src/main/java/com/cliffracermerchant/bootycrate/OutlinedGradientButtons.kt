/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

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
import dagger.hilt.android.AndroidEntryPoint

/**
 * A button with a vector background, outline, and icon that are all tintable with gradients.
 *
 * OutlinedGradientButton is a button that uses GradientVectorDrawables
 * for its background, outline, and icon. The path data used for these
 * drawables are defined in XML using the attributes backgroundPathData,
 * outlinePathData, and iconPathData. The stroke width for the outline is
 * set using the attributes outlineStrokeWidth. The gradient shader used
 * for the background drawaable can be set through the property background-
 * Gradient, while the gradient for the outline, the icon, and any text is
 * set through the property foregroundGradient. In order for the path data
 * attributes to be interpreted correctly, the XML properties pathWidth
 * and pathHeight must also be set in XML.
 */
open class OutlinedGradientButton(context: Context, attrs: AttributeSet) :
    AppCompatButton(context, attrs)
{
    var backgroundGradient get() = backgroundDrawable.gradient
        set(gradient) { backgroundDrawable.gradient = gradient }
    var outlineGradient get() = outlineDrawable.gradient
        set(gradient) { outlineDrawable.gradient = gradient
                        iconDrawable.gradient = gradient
                        paint.shader = gradient }

    protected var backgroundDrawable: GradientVectorDrawable
    protected var outlineDrawable: GradientVectorDrawable
    protected var iconDrawable: GradientVectorDrawable

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.OutlinedGradientButton)
        val backgroundPathData = a.getString(R.styleable.OutlinedGradientButton_backgroundPathData) ?: ""
        val outlinePathData = a.getString(R.styleable.OutlinedGradientButton_outlinePathData) ?: ""
        val outlineStrokeWidth = a.getDimension(R.styleable.OutlinedGradientButton_outlineStrokeWidth, 0f)
        val iconPathData = a.getString(R.styleable.OutlinedGradientButton_iconPathData) ?: ""
        val iconStrokeWidth = a.getDimension(R.styleable.OutlinedGradientButton_iconStrokeWidth, 0f)
        // Apparently if more than one attr is retrieved with a manual
        // IntArray, they must be in numerical id order to work.
        // Log.d("", "R.attr.pathWidth = ${R.attr.pathWidth}, R.attr.pathHeight=${R.attr.pathHeight}")
        a = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.pathHeight, R.attr.pathWidth))
        val pathHeight = a.getFloat(0, 0f)
        @SuppressLint("ResourceType")
        val pathWidth = a.getFloat(1, 0f)
        a.recycle()

        backgroundDrawable = GradientVectorDrawable(pathWidth, pathHeight, backgroundPathData )
        outlineDrawable = GradientVectorDrawable(pathWidth, pathHeight, outlinePathData)
        iconDrawable = GradientVectorDrawable(pathWidth, pathHeight, iconPathData)
        outlineDrawable.strokeWidth = outlineStrokeWidth
        outlineDrawable.style = Paint.Style.STROKE
        if (iconStrokeWidth != 0f) {
            iconDrawable.strokeWidth = iconStrokeWidth
            iconDrawable.style = Paint.Style.STROKE
        }
        this.background = LayerDrawable(arrayOf(backgroundDrawable, outlineDrawable, iconDrawable))
    }
}

/**
 * An OutlinedGradientButton with disable/enable functionality.
 *
 * DisableableOutlinedGradientButton automatically creates a disabled draw-
 * able for itself based on the OutlinedGradientButton background and the
 * XML attributes disabledBackgroundTint and disabledOutlineAndTextTint.
 * The disabled drawable will look like the OutlinedGradientButton back-
 * ground, but with these alternative tint values and no gradient shaders.
 * When the button is enabled or disabled via the View property isEnabled,
 * the button will animate to or from its disabled state, using the value
 * of the property animatorConfig for the animation's duration and inter-
 * polator. Note that the disabled background will not reflect any changes
 * to the button's text that occur after initialization.
 *
 * If any additional changes are desired when the isEnabledState is
 * changed, they can be defined in a subclass override of View.setEnabled.
 */
@AndroidEntryPoint
open class DisableableOutlinedGradientButton(context: Context, attrs: AttributeSet) :
    OutlinedGradientButton(context, attrs)
{
    private var disabledOverlay: Drawable? = null
    var animatorConfig: AnimatorConfig? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DisableableOutlinedGradientButton)
        val disabledBackgroundTint = a.getColor(R.styleable.DisableableOutlinedGradientButton_disabledBackgroundTint, 0)
        val disabledOutlineAndTextTint = a.getColor(R.styleable.DisableableOutlinedGradientButton_disabledOutlineAndTextTint, 0)
        a.recycle()

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
                alpha = if (isEnabled) 0 else 255
                bounds = background.bounds
            }
            backgroundGradient = backupBackgroundGradient
            outlineGradient = backupOutlineGradient
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
 * CheckoutButton is a DisableableOutlinedGradientButton with extra func-
 * tionality that is intended to be used as the button to execute the shop-
 * ping list checkout function (see ShoppingListItemDao.checkout() for
 * more information). Its normal text reads checkout, but when tapped its
 * text will change to indicate to the user that it is in a confirmatory
 * state. An additional tap will then actually execute the callback set
 * via the property checkoutCallback. If the user does not tap the button
 * again before the value of R.integer.checkoutButtonConfirmationTimeout,
 * the button will reset to its normal state.
 *
 * Confirmation is requested due to the checkout function being irrevers-
 * ible, and because a double tap for users who know what they are doing
 * is faster than having to answer yes to a confirmatory alert dialog.
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