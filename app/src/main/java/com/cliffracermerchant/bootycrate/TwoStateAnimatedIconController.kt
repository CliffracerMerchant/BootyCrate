/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

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
    private val aToBDrawable: AnimatedVectorDrawable,
    private val bToADrawable: AnimatedVectorDrawable,
    private var targetLayerId: Int = -1) {
    // Due to AnimatedVectorDrawable.reset() not being
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
            TwoStateAnimatedIconController(fab, aToBDrawable, bToADrawable)

        fun forView(view: View,
                    aToBDrawable: AnimatedVectorDrawable,
                    bToADrawable: AnimatedVectorDrawable) =
            TwoStateAnimatedIconController(view, aToBDrawable, bToADrawable)

        fun forDrawableLayer(layerDrawable: LayerDrawable, targetLayerId: Int,
                             aToBDrawable: AnimatedVectorDrawable,
                             bToADrawable: AnimatedVectorDrawable) =
            TwoStateAnimatedIconController(layerDrawable, aToBDrawable, bToADrawable, targetLayerId)
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