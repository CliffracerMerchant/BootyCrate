/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.AnimationUtils

/** A data class that contains a TimeInterpolator and a duration for syncing animations. */
data class AnimatorConfig(var duration: Long, var interpolator: TimeInterpolator) {
    companion object {
        fun translation(context: Context) = AnimatorConfig(
            context.resources.getInteger(R.integer.animationDefaultDuration).toLong(),
            AnimationUtils.loadInterpolator(context, R.anim.default_interpolator))
    }
}

/** Apply an AnimatorConfig to an Animator object and return the object. */
fun <T: Animator>T.applyConfig(config: AnimatorConfig?) = apply {
    if (config == null) return@apply
    duration = config.duration
    interpolator = config.interpolator
}

/** Apply an AnimatorConfig to a ViewPropertyAnimator object and return the object. */
fun ViewPropertyAnimator.applyConfig(config: AnimatorConfig?) = apply {
    if (config == null) return@apply
    duration = config.duration
    interpolator = config.interpolator
}

// The following value animator returning functions can be used similarly to object animators,
// but hopefully are more performant due to not using reflection to get the property setter.
/** Return a valueAnimator for an Int property with the update listener already set. */
fun valueAnimatorOfInt(
    setter: (Int) -> Unit,
    fromValue: Int, toValue: Int,
    config: AnimatorConfig? = null,
): ValueAnimator = ValueAnimator.ofInt(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    if (config != null) applyConfig(config)
}

/** Return a valueAnimator for a Float property with the update listener already set. */
fun valueAnimatorOfFloat(
    setter: (Float) -> Unit,
    fromValue: Float, toValue: Float,
    config: AnimatorConfig? = null,
): ValueAnimator = ValueAnimator.ofFloat(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Float) }
    if (config != null) applyConfig(config)
}

/** Return a valueAnimator for an ARGB property with the update listener already set. */
fun valueAnimatorOfArgb(
    setter: (Int) -> Unit,
    fromValue: Int, toValue: Int,
    config: AnimatorConfig? = null,
): ValueAnimator = ValueAnimator.ofArgb(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    if (config != null) applyConfig(config)
}

/** Return a LayoutTransition whose durations and interpolators match the given AnimatorConfig. */
fun layoutTransition(config: AnimatorConfig) = LayoutTransition().apply {
    setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
    setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
    setStartDelay(LayoutTransition.APPEARING, 0)
    setStartDelay(LayoutTransition.DISAPPEARING, 0)
    setStartDelay(LayoutTransition.CHANGING, 0)
    setInterpolator(LayoutTransition.CHANGE_APPEARING, config.interpolator)
    setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, config.interpolator)
    setInterpolator(LayoutTransition.APPEARING, config.interpolator)
    setInterpolator(LayoutTransition.DISAPPEARING, config.interpolator)
    setInterpolator(LayoutTransition.CHANGING, config.interpolator)
    setDuration(config.duration)
}

/** Apply a onStart action to a LayoutTransition. */
fun LayoutTransition.doOnStart(onStart: () -> Unit) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) =
            onStart()
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) { }
    })
}

/** Apply a onEnd action to a LayoutTransition. */
fun LayoutTransition.doOnEnd(onEnd: () -> Unit) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) { }
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) =
            onEnd()
    })
}
