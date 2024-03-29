/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.AnimationUtils
import com.cliffracertech.bootycrate.R

/** A data class that contains a TimeInterpolator and a duration for syncing animations. */
data class AnimatorConfig(var duration: Long, var interpolator: TimeInterpolator) {
    companion object {
        /** Return an AnimatorConfig with an animation duration equal to the value
         * of R.integer.defaultAnimationDuration and an interpolator equal to the
         * value of R.anim.default_interpolator. */
        fun appDefault(context: Context) = AnimatorConfig(
            context.resources.getInteger(R.integer.defaultAnimationDuration).toLong(),
            AnimationUtils.loadInterpolator(context, R.anim.default_interpolator))
    }
}

/** Apply an AnimatorConfig to an Animator object and return the Animator. */
fun <T: Animator>T.applyConfig(config: AnimatorConfig?) = apply {
    if (config == null) return@apply
    duration = config.duration
    interpolator = config.interpolator
}

/** Apply an AnimatorConfig to a ViewPropertyAnimator object and return the ViewPropertyAnimator. */
fun ViewPropertyAnimator.applyConfig(config: AnimatorConfig?) = apply {
    if (config == null) return@apply
    duration = config.duration
    interpolator = config.interpolator
}

/** Apply an AnimatorConfig to a LayoutTransition and return the LayoutTransition. */
fun LayoutTransition.applyConfig(config: AnimatorConfig?) = apply {
    if (config == null) return@apply
    setInterpolator(LayoutTransition.CHANGE_APPEARING, config.interpolator)
    setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, config.interpolator)
    setInterpolator(LayoutTransition.APPEARING, config.interpolator)
    setInterpolator(LayoutTransition.DISAPPEARING, config.interpolator)
    setInterpolator(LayoutTransition.CHANGING, config.interpolator)
    setDuration(config.duration)
}

// The following ValueAnimator returning functions can be used similarly to object animators,
// but hopefully are more performant due to not using reflection to get the property setter
// (the performance difference is likely negligible, but these are similar in ease of use as
// ObjectAnimators, and allow for setting the duration and interpolator at the same time through
// the config parameter.
/** Return a ValueAnimator for an Int property with the update listener already set. */
fun intValueAnimator(
    setter: (Int) -> Unit,
    from: Int, to: Int,
    config: AnimatorConfig? = null,
): ValueAnimator = ValueAnimator.ofInt(from, to).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    if (config != null) applyConfig(config)
}

/** Return a ValueAnimator for a Float property with the update listener already set. */
fun floatValueAnimator(
    setter: (Float) -> Unit,
    from: Float, to: Float,
    config: AnimatorConfig? = null,
): ValueAnimator = ValueAnimator.ofFloat(from, to).apply {
    addUpdateListener { setter(it.animatedValue as Float) }
    if (config != null) applyConfig(config)
}

/** Return a ValueAnimator for an ARGB property with the update listener already set. */
fun argbValueAnimator(
    setter: (Int) -> Unit,
    from: Int, to: Int,
    config: AnimatorConfig? = null,
): ValueAnimator = ValueAnimator.ofArgb(from, to).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    if (config != null) applyConfig(config)
}

/** Return a delayless LayoutTransition with the given AnimatorConfig applied. */
fun layoutTransition(config: AnimatorConfig?) =
    LayoutTransition().applyConfig(config).apply {
        setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
        setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
        setStartDelay(LayoutTransition.APPEARING, 0)
        setStartDelay(LayoutTransition.DISAPPEARING, 0)
        setStartDelay(LayoutTransition.CHANGING, 0)
    }

/** Apply a onStart action to a LayoutTransition. */
fun LayoutTransition.doOnStart(onStart: () -> Unit) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) =
            onStart()
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) = Unit
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
