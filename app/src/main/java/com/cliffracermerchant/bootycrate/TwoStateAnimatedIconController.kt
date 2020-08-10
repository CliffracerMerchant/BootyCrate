/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton

/** A wrapper to manage the AnimatedVectorDrawable backgrounds of a view.
 *
 *  TwoStateAnimatedIconController manages the background of a target view to
 *  enable easy animated switching between two states. Instances are created
 *  using the static factory methods forFloatingActionButton, forView, or
 *  forDrawableLayer.
 *
 *  The state of the managed background is altered using the functions setState(
 *  toStateA: Boolean), toStateA(), toStateB(), and toggleState(), or using the
 *  public boolean property isInStateA. All of the functions also allow an optional
 *  second parameter to enable or disable animating between states. */
class TwoStateAnimatedIconController private constructor(
    private val target: Any,
    aToBDrawable: AnimatedVectorDrawable? = null,
    bToADrawable: AnimatedVectorDrawable? = null,
    private var targetLayerId: Int = -1) {
    /* Due to AnimatedVectorDrawable.reset() not being present in lower API
     * levels, TwoStateAnimatedIconController keeps track of whether or not each
     * drawable has been animated at least once so that it can set the drawable
     * properly for a non-animated state change. */
    private var aToBHasBeenAnimated = false
    private var bToAHasBeenAnimated = false
    private lateinit var aToBDrawable: AnimatedVectorDrawable
    private lateinit var bToADrawable: AnimatedVectorDrawable
    private var _isInStateA = true
    private val targetIsFab = target is FloatingActionButton
    private val targetIsDrawableLayer = target is LayerDrawable

    var isInStateA get() = _isInStateA
                   set(value) { setState(value) }
    var tint: Int = 0
        set(value) { field = value
                     aToBDrawable.setTint(value)
                     bToADrawable.setTint(value) }

    companion object {
        fun forFloatingActionButton(fab: FloatingActionButton,
                                    aToBDrawable: AnimatedVectorDrawable? = null,
                                    bToADrawable: AnimatedVectorDrawable? = null) =
            TwoStateAnimatedIconController(fab, aToBDrawable, bToADrawable)

        fun forView(view: View,
                    aToBDrawable: AnimatedVectorDrawable? = null,
                    bToADrawable: AnimatedVectorDrawable? = null) =
            TwoStateAnimatedIconController(view, aToBDrawable, bToADrawable)

        fun forDrawableLayer(layerDrawable: LayerDrawable, targetLayerId: Int,
                             aToBDrawable: AnimatedVectorDrawable? = null,
                             bToADrawable: AnimatedVectorDrawable? = null) =
            TwoStateAnimatedIconController(layerDrawable, aToBDrawable, bToADrawable, targetLayerId)
    }

    init {
        if (aToBDrawable != null) this.aToBDrawable = aToBDrawable
        if (bToADrawable != null) this.bToADrawable = bToADrawable
    }

    fun setState(toStateA: Boolean, animate: Boolean = true) {
        _isInStateA = toStateA
        val newBg = if (animate)
                        if (toStateA) {
                            bToAHasBeenAnimated = true
                            bToADrawable
                        } else {
                            aToBHasBeenAnimated = true
                            aToBDrawable
                        }
                    else if (toStateA)
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

    fun setAtoBDrawable(drawable: AnimatedVectorDrawable) {
        aToBDrawable = drawable
        aToBHasBeenAnimated = false
        if (isInStateA && !bToAHasBeenAnimated) setBackground(drawable)
    }

    fun setBtoADrawable(drawable: AnimatedVectorDrawable) {
        bToADrawable = drawable
        bToAHasBeenAnimated = false
        if (!isInStateA && !aToBHasBeenAnimated) setBackground(drawable)
    }

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