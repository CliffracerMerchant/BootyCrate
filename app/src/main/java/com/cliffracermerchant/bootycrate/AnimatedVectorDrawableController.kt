package com.cliffracermerchant.bootycrate

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.security.InvalidParameterException

class AnimatedVectorDrawableController private constructor(
    private val target: Any,
    private val aToBDrawable: AnimatedVectorDrawable,
    private val bToADrawable: AnimatedVectorDrawable,
    private var targetLayerId: Int = -1) {
    private var aToBHasBeenAnimated = false
    private var bToAHasBeenAnimated = false
    private var _isInStateA = true
    private val targetIsFab = target is FloatingActionButton
    private val targetIsDrawableLayer = target is LayerDrawable

    var isInStateA get() = _isInStateA
        set(value) { setState(value) }
    var tint: Int? = null
        set(value) { field = value
                     aToBDrawable.setTint(value ?: 0)
                     bToADrawable.setTint(value ?: 0) }

    companion object {
        fun forFloatingActionButton(fab: FloatingActionButton,
                                    aToBDrawable: AnimatedVectorDrawable,
                                    bToADrawable: AnimatedVectorDrawable) =
            AnimatedVectorDrawableController(fab, aToBDrawable, bToADrawable)

        fun forView(view: View,
                    aToBDrawable: AnimatedVectorDrawable,
                    bToADrawable: AnimatedVectorDrawable) =
            AnimatedVectorDrawableController(view, aToBDrawable, bToADrawable)

        fun forDrawableLayer(layerDrawable: LayerDrawable, targetLayerId: Int,
                             aToBDrawable: AnimatedVectorDrawable,
                             bToADrawable: AnimatedVectorDrawable) =
            AnimatedVectorDrawableController(layerDrawable, aToBDrawable, bToADrawable, targetLayerId)
    }

    init { setBackground(aToBDrawable) }

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
                   (target as LayerDrawable).setDrawableByLayerId(targetLayerId, newBackground)
               else ->
                   (target as View).background = newBackground
        }
    }
}