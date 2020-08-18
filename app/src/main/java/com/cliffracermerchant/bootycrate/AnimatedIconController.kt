/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.core.animation.doOnEnd
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.IllegalArgumentException

/** A wrapper to manage the AnimatedVectorDrawable backgrounds of a view.
 *
 *  AnimatedIconController manages the background of a target view to enable
 *  easy animated switching between two states. Instances are created using the
 *  static factory methods forFloatingActionButton, forView, or forDrawable-
 *  Layer.
 *
 *  Any number of states can be added using the addState function. An optional
 *  string label can be passed to addState if referring to states by a label
 *  is preferred over referring to them by index. The label of a newly added
 *  state will be returned from addState. If other states have already been
 *  added, transitions to and from the state being added to other states can
 *  optionally be passed in to addState in the form of a triple where:
 *      triple.first = index of the already added state
 *      triple.second = AnimatedVectorDrawable to transition from the
 *                      state being added to the already added state
 *      triple.third = AnimatedVectorDrawable to transition from the
 *                     already added state to the state being added
 *  If only two states are desired, a useful idiom to add both states and the
 *  transitions between them is:
 *      addTransition(addState("state1label"), addState("state2label"),
 *                    1to2animatedVectorDrawable, 2to1animatedVectorDrawable)
 *
 *  The state of the managed background is altered using the function setState(
 *  targetState), where targetState is either the index or label of the desired
 *  state. setState also allow an optional second parameter to enable or
 *  disable animating between states.
 *
 *  If a tint for the icon being controlled is desired, the public property
 *  tint allows this to be set for all transition drawables at once. Setting
 *  this property for an AnimatedIconController that manages a floating action
 *  button will not do anything until the state is changed, due to the FAB's
 *  API lacking a way to retrieve the current image drawable.*/
class AnimatedIconController private constructor(
    private val target: Any,
    private var targetLayerId: Int = -1) {
    /* Due to AnimatedVectorDrawable.reset() not being present in lower API
     * levels, TwoStateAnimatedIconController keeps track of whether or not each
     * drawable has been animated at least once so that it can set the drawable
     * properly for a non-animated state change. */
    private val targetIsFab = target is FloatingActionButton
    private val targetIsDrawableLayer = target is LayerDrawable

    private var _currentStateIndex: Int = -1
    val currentStateIndex get() = _currentStateIndex
    val currentStateLabel get() = states[_currentStateIndex].label

    private var _tint: Int? = null
    var tint: Int? get() = _tint
                   set(value) { _tint = value
                                setTint(value) }

    data class State(val index: Int, val label: String)
    data class Transition(var drawable: AnimatedVectorDrawable,
                          var hasBeenAnimated: Boolean = false)

    private val states = mutableListOf<State>()
    private val transitions = hashMapOf<Pair<Int, Int>, Transition>()

    companion object {
        fun forFloatingActionButton(fab: FloatingActionButton) = AnimatedIconController(fab)
        fun forView(view: View) = AnimatedIconController(view)
        fun forDrawableLayer(layerDrawable: LayerDrawable, targetLayerId: Int) =
            AnimatedIconController(layerDrawable, targetLayerId)
    }

    fun addState(label: String = "", vararg transitions: Triple<Int, AnimatedVectorDrawable, AnimatedVectorDrawable>): Int {
        val index = states.size
        states.add(State(index, label))
        for (transition in transitions) {
            this.transitions[Pair(index, transition.first)] = Transition(transition.second)
            this.transitions[Pair(transition.first, index)] = Transition(transition.third)
        }
        return index
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
            setBackground(fromToTransition)
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
        setBackground(newBg)
        if (animate) {
            fromToTransition.hasBeenAnimated = true
            newBg.start()
        }
        _currentStateIndex = targetStateIndex
    }

    fun setState(targetStateLabel: String, animate: Boolean = true) =
        setState(findStateWithLabel(targetStateLabel).index, animate)

    fun setTint(newTint: Int?, animate: Boolean = true) {
        val drawable = when {
            targetIsFab -> (target as FloatingActionButton).background
            targetIsDrawableLayer ->
                (target as LayerDrawable).findDrawableByLayerId(targetLayerId)
            else -> (target as View).background
        }
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

    private fun findStateWithLabel(label: String) =
        states.find { it.label == label } ?:
            throw IllegalArgumentException("State with label $label not found.")

    private fun setBackground(newBackground: AnimatedVectorDrawable) {
        val tint = this.tint
        if (tint != null) newBackground.setTint(tint)
        when { targetIsFab ->
            (target as FloatingActionButton).setImageDrawable(newBackground)
            targetIsDrawableLayer ->
                (target as LayerDrawable).setDrawableByLayerId(targetLayerId, newBackground)
            else -> (target as View).background = newBackground
        }
    }
}