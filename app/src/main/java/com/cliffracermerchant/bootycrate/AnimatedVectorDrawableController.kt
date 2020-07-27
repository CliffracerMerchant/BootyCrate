package com.cliffracermerchant.bootycrate

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.security.InvalidParameterException

class AnimatedVectorDrawableController(private val target: Any,
                                       private val aToBDrawable: AnimatedVectorDrawable,
                                       private val bToADrawable: AnimatedVectorDrawable,
                                       private val layerId: Int = -1) {
    private var aToBHasBeenAnimated = false
    private var bToAHasBeenAnimated = false
    private val targetIsFab = target is FloatingActionButton
    private val targetIsDrawableLayer = target is LayerDrawable

    private var _isInStateA = true

    var isInStateA get() = _isInStateA
                   set(value) { setState(value) }
    var tint: Int? = null
        set(value) { field = value
                     aToBDrawable.setTint(value ?: 0)
                     bToADrawable.setTint(value ?: 0) }

    init {
        if (!targetIsFab && !targetIsDrawableLayer && target !is View)
            throw InvalidParameterException("The target for an AnimatedVectorDrawable" +
                                            "Controller must be a FloatingActionButton, " +
                                            "a LayerDrawable, or a View descendant")
        setBackground(aToBDrawable)
    }

    fun setState(toStateA: Boolean, animate: Boolean = true) {
        _isInStateA = toStateA
        val newBg =
            if (animate)
                if (toStateA) {
                    bToAHasBeenAnimated = true
                    bToADrawable
                } else {
                    aToBHasBeenAnimated = true
                    aToBDrawable
                }
            else
                if (toStateA)
                    if (!aToBHasBeenAnimated) aToBDrawable
                    else                      bToADrawable
                else
                    if (!bToAHasBeenAnimated) bToADrawable
                    else                      aToBDrawable
        setBackground(newBg)
        if (animate) newBg.start()
    }

    fun toStateA(animate: Boolean = true) = setState(true, animate)
    fun toStateB(animate: Boolean = true) = setState(false, animate)
    fun toggleState(animate: Boolean = true) = setState(!_isInStateA, animate)

    private fun setBackground(newBackground: AnimatedVectorDrawable) {
        when { targetIsFab ->
                   (target as FloatingActionButton).setImageDrawable(newBackground)
               targetIsDrawableLayer ->
                   (target as LayerDrawable).setDrawableByLayerId(layerId, newBackground)
               else ->
                   (target as View).background = newBackground
        }
    }
}