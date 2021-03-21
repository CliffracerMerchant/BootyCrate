/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.core.view.doOnNextLayout

/**
 * An OutlinedGradientButton with disable/enable functionality.
 *
 * DisableableOutlinedGradientButton automatically creates a disabled drawable
 * for itself based on the OutlinedGradientButton background and the XML attri-
 * butes disabledBackgroundTint and disabledOutlineAndTextTint. The disabled
 * drawable will look like the OutlinedGradientButton background, but with
 * these alternative tint values and no gradient shaders. When the button is
 * enabled or disabled via the View property isEnabled the button will animate
 * to or from its disabled state. Note that the disabled background will not
 * reflect any changes to the button's text that occur after initialization.
 *
 * If any additional changes are desired when the isEnabledState is changed,
 * they can be defined in a subclass override of View.setEnabled.
 */
open class DisableableOutlinedGradientButton(context: Context, attrs: AttributeSet) :
    OutlinedGradientButton(context, attrs)
{
    private var disabledOverlay: Drawable? = null

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
                                      config = AnimatorConfig.translation(context))
        anim.addUpdateListener{ invalidate() }
        anim.start()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) disabledOverlay?.draw(canvas)
    }
}