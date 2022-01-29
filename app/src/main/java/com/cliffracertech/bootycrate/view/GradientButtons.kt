/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracertech.bootycrate.view

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatButton
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.intValueAnimator

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
        val a = context.obtainStyledAttributes(attrs, R.styleable.GradientButton)
        val backgroundPathData = a.getString(R.styleable.GradientButton_backgroundPathData) ?: ""
        val iconPathData = a.getString(R.styleable.GradientButton_iconPathData) ?: ""
        val iconStrokeWidth = a.getDimension(R.styleable.GradientButton_iconStrokeWidth, 0f)
        val pathWidth = a.getFloat(R.styleable.GradientButton_pathWidth, 0f)
        val pathHeight = a.getFloat(R.styleable.GradientButton_pathHeight, 0f)
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
 * A GradientButton with an automatic and animated changing of
 * its alpha property to indicate its disabled/enabled state.
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
        if (!isLaidOut) return
        val anim = intValueAnimator(background::setAlpha,
                                    if (enabled) disabledAlpha else 255,
                                    if (enabled) 255 else disabledAlpha,
                                    animatorConfig)
        anim.addUpdateListener{ background.invalidateSelf() }
        anim.start()
    }
}

/**
 * A DisableableGradientButton with a double tap to use functionality.
 *
 * DoubleTapToConfirmButton acts as a DisableableGradientButton that implements
 * a double-tap to use functionality, intended for actions that are too trivial
 * to use with a confirmatory dialog, but that still want to allow confirmation
 * before executing an action. Its text will start as the value of the XML
 * attribute android:text, but when tapped will change to indicate to the user
 * that it is in a confirmatory state. An additional tap will then actually
 * execute the callback set via the property checkoutCallback. If the user does
 * not tap the button again before the value of R.integer.checkoutButtonConfirmationTimeout,
 * the button will reset to its normal state.
 */
class DoubleTapToConfirmButton(context: Context, attrs: AttributeSet) :
    DisableableGradientButton(context, attrs)
{
    private val normalText: String
    private val confirmText: String
    private var lastPressTimeStamp = 0L
    private val confirmTimeout = resources.getInteger(R.integer.checkoutButtonConfirmationTimeout).toLong()
    var onConfirm: (() -> Unit)? = null

    init {
        var a = context.obtainStyledAttributes(R.styleable.DoubleTapToConfirmButton)
        confirmText = a.getString(R.styleable.DoubleTapToConfirmButton_confirmatoryText) ?: "Confirm?"
        a.recycle()
        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text))
        normalText = a.getString(0) ?: ""
        a.recycle()
        text = normalText

        setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime < lastPressTimeStamp + confirmTimeout) {
                exitConfirmatoryState()
                onConfirm?.invoke()
            } else {
                text = confirmText
                lastPressTimeStamp = currentTime
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
        lastPressTimeStamp = 0
        text = normalText
    }
}