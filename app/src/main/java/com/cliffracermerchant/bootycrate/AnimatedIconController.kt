/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.IllegalArgumentException

/** A wrapper to manage the AnimatedVectorDrawable backgrounds of a view.
 *
 *  AnimatedIconController manages the background of a target view to enable
 *  easy animated switching between two states. Because it is intended to work
 *  with as many types of drawable attributes as possible (e.g. a view's back-
 *  ground, an ImageView's image resource, or a layer of a LayerDrawable),
 *  AnimatedIconController is abstract. Subclasses must implement the abstract
 *  methods
 *
 *  Any number of states can be added using the addState function. An optional
 *  string label can be passed to addState if referring to states by a label
 *  is preferred over referring to them by index. The label of a newly added
 *  state will be returned from addState. If only two states are desired, a
 *  useful idiom to add both states and the transitions between them is:
 *      addTransition(addState("state1label"), addState("state2label"),
 *                    1to2animatedVectorDrawable, 2to1animatedVectorDrawable)
 *
 *  The state of the managed background is altered using the function setState(
 *  targetState), where targetState is either the index or label of the desired
 *  state. setState also allow an optional second parameter to enable or
 *  disable animating between states.
 *
 *  If a tint for the icon being controlled is desired, the public property
 *  tint allows this to be set for all transition drawables at once. */
abstract class AnimatedIconController {

    private var _currentStateIndex: Int = -1
    val currentStateIndex get() = _currentStateIndex
    val currentStateLabel get() = states[_currentStateIndex].label

    private var _tint: Int? = null
    var tint: Int? get() = _tint
                   set(value) { _tint = value; setTint(value) }

    /* Due to AnimatedVectorDrawable.reset() not being present in lower API
     * levels, TwoStateAnimatedIconController keeps track of whether or not each
     * drawable has been animated at least once so that it can set the drawable
     * properly for a non-animated state change. */
    data class State(val index: Int, val label: String)
    data class Transition(var drawable: AnimatedVectorDrawable,
                          var hasBeenAnimated: Boolean = false)

    private val states = mutableListOf<State>()
    private val transitions = hashMapOf<Pair<Int, Int>, Transition>()

    fun addState(label: String = "") : Int {
        val newIndex = states.size
        states.add(State(newIndex, label))
        return newIndex
    }

    fun addTransition(
        fromStateIndex: Int, toStateIndex: Int,
        fromToTransition: AnimatedVectorDrawable,
        toFromTransition: AnimatedVectorDrawable
    ) {
        transitions[Pair(fromStateIndex, toStateIndex)] = Transition(fromToTransition)
        transitions[Pair(toStateIndex, fromStateIndex)] = Transition(toFromTransition)
        if (currentStateIndex == -1) {
            _currentStateIndex = fromStateIndex
            setDrawable(fromToTransition)
        }
    }

    fun addTransition(
        fromStateLabel: String, toStateLabel: String,
        fromToTransition: AnimatedVectorDrawable,
        toFromTransition: AnimatedVectorDrawable
    ) =
        addTransition(findStateWithLabel(fromStateLabel).index,
                      findStateWithLabel(toStateLabel).index,
                      fromToTransition, toFromTransition)

    fun setState(targetStateIndex: Int, animate: Boolean = true) {
        if (currentStateIndex == targetStateIndex) return
        val fromToTransition = transitions[Pair(currentStateIndex, targetStateIndex)] ?:
            throw IllegalArgumentException("No transition between states $currentStateIndex and $targetStateIndex has been added.")
        val newBg = if (animate || fromToTransition.hasBeenAnimated)
                        fromToTransition.drawable
                    else transitions[Pair(targetStateIndex, currentStateIndex)]?.drawable ?:
                        throw IllegalArgumentException("No transition between states $targetStateIndex and $currentStateIndex has been added.")

        val tint = _tint
        if (tint != null) newBg.setTint(tint)
        setDrawable(newBg)
        if (animate) {
            fromToTransition.hasBeenAnimated = true
            newBg.start()
        }
        _currentStateIndex = targetStateIndex
    }

    fun setState(targetStateLabel: String, animate: Boolean = true) =
        setState(findStateWithLabel(targetStateLabel).index, animate)

    private fun findStateWithLabel(label: String) =
        states.find { it.label == label } ?:
        throw IllegalArgumentException("State with label $label not found.")

    protected abstract fun setDrawable(newDrawable: AnimatedVectorDrawable)

    protected abstract fun getDrawable(): AnimatedVectorDrawable

    fun setTint(newTint: Int?, animate: Boolean = true) {
        val drawable = getDrawable()
        if (!animate) if (newTint == null) drawable.setTintList(null)
                      else                 drawable.setTint(newTint)
        else {
            val startTint = tint ?: 0
            val endTint = newTint ?: 0
            val anim = ObjectAnimator.ofArgb(drawable, "tint", startTint, endTint)
            if (newTint == null) anim.doOnEnd { drawable.setTintList(null) }
            anim.start()
        }
        _tint = newTint
    }
}

class AnimatedFabIconController(private val fab: FloatingActionButton) : AnimatedIconController() {
    override fun getDrawable(): AnimatedVectorDrawable = fab.drawable as AnimatedVectorDrawable
    override fun setDrawable(newDrawable: AnimatedVectorDrawable) = fab.setImageDrawable(newDrawable)
}

class AnimatedImageViewController(private val imageView: ImageView) : AnimatedIconController() {
    override fun getDrawable(): AnimatedVectorDrawable = imageView.drawable as AnimatedVectorDrawable
    override fun setDrawable(newDrawable: AnimatedVectorDrawable) = imageView.setImageDrawable(newDrawable)
}

class AnimatedDrawableLayer(private val layerDrawable: LayerDrawable, private val layerId: Int) :
        AnimatedIconController() {
    override fun getDrawable(): AnimatedVectorDrawable = layerDrawable.findDrawableByLayerId(layerId) as AnimatedVectorDrawable
    override fun setDrawable(newDrawable: AnimatedVectorDrawable) { layerDrawable.setDrawableByLayerId(layerId, newDrawable) }
}

