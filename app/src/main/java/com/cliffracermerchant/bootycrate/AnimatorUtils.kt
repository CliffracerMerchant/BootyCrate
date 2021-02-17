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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils

object AnimatorUtils {
    data class Config(val duration: Long, val interpolator: TimeInterpolator) {
        fun apply(anim: Animator) {
            anim.duration = duration
            anim.interpolator = interpolator
        }
    }

    fun initConfigs(context: Context) {
        val viewAnimationDuration = context.resources.getInteger(R.integer.viewAnimationDuration).toLong()
        val fragmentTransitionDuration = context.resources.getInteger(R.integer.fragmentTransitionDuration).toLong()
        val accelerateDecelerateInterpolator = AnimationUtils.loadInterpolator(
            context, android.R.anim.accelerate_decelerate_interpolator)
        val decelerateInterpolator = AnimationUtils.loadInterpolator(
            context, android.R.anim.decelerate_interpolator)
        val accelerateInterpolator = AnimationUtils.loadInterpolator(
            context, android.R.anim.accelerate_interpolator)
        viewTranslationConfigPrivate = Config(viewAnimationDuration, accelerateDecelerateInterpolator)
        viewFadeInConfigPrivate = Config(viewAnimationDuration, accelerateInterpolator)
        viewFadeOutConfigPrivate = Config(viewAnimationDuration, decelerateInterpolator)
        fragmentTransitionConfigPrivate = Config(fragmentTransitionDuration, AccelerateDecelerateInterpolator())
        shoppingListItemViewConfigPrivate = Config((viewAnimationDuration * 3 / 4), accelerateDecelerateInterpolator)
    }

    private lateinit var viewTranslationConfigPrivate: Config
    private lateinit var viewFadeInConfigPrivate: Config
    private lateinit var viewFadeOutConfigPrivate: Config
    private lateinit var fragmentTransitionConfigPrivate: Config
    private lateinit var shoppingListItemViewConfigPrivate: Config
    val viewTranslationConfig get() = viewTranslationConfigPrivate
    val viewFadeInConfig get() = viewFadeInConfigPrivate
    val viewFadeOutConfig get() = viewFadeOutConfigPrivate
    val fragmentTransitionConfig get() = fragmentTransitionConfigPrivate
    val shoppingListItemViewConfig get() = shoppingListItemViewConfigPrivate
}

fun ViewPropertyAnimator.applyConfig(config: AnimatorUtils.Config) = apply {
    duration = config.duration
    interpolator = config.interpolator
}

// The following value animator returning functions can be used similarly to object animators,
// but hopefully are more performant due to not using reflection to get the property setter.
/** Return a valueAnimator for an Int property with the update listener already set. */
fun valueAnimatorOfInt(
    setter: (Int) -> Unit,
    initialValue: Int,
    endValue: Int,
    config: AnimatorUtils.Config = AnimatorUtils.viewTranslationConfig,
): ValueAnimator = ValueAnimator.ofInt(initialValue, endValue).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    config.apply(this)
}

/** Return a valueAnimator for a Float property with the update listener already set. */
fun valueAnimatorOfFloat(
    setter: (Float) -> Unit,
    initialValue: Float,
    endValue: Float,
    config: AnimatorUtils.Config = AnimatorUtils.viewTranslationConfig,
): ValueAnimator = ValueAnimator.ofFloat(initialValue, endValue).apply {
    addUpdateListener { setter(it.animatedValue as Float) }
    config.apply(this)
}

/** Return a valueAnimator for an ARGB property with the update listener already set. */
fun valueAnimatorOfArgb(
    setter: (Int) -> Unit,
    initialValue: Int,
    endValue: Int,
    config: AnimatorUtils.Config = AnimatorUtils.viewTranslationConfig,
): ValueAnimator = ValueAnimator.ofArgb(initialValue, endValue).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    config.apply(this)
}

fun layoutTransition(config: AnimatorUtils.Config) = LayoutTransition().apply {
    enableTransitionType(LayoutTransition.CHANGING)
    setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
    setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
    setStartDelay(LayoutTransition.APPEARING, 0)
    setStartDelay(LayoutTransition.DISAPPEARING, 0)
    setStartDelay(LayoutTransition.CHANGING, 0)
    setInterpolator(LayoutTransition.CHANGE_APPEARING, config.interpolator)
    setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, config.interpolator)
    setInterpolator(LayoutTransition.APPEARING, AnimatorUtils.viewFadeInConfig.interpolator)
    setInterpolator(LayoutTransition.DISAPPEARING, AnimatorUtils.viewFadeOutConfig.interpolator)
    setInterpolator(LayoutTransition.CHANGING, config.interpolator)
    setDuration(config.duration)
}

fun LayoutTransition.doOnStart(onStart: (transition: LayoutTransition,
                                         container: ViewGroup, view: View,
                                         transitionType: Int) -> Unit = {_, _, _, _ -> }) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) =
            onStart(a, b, c, d)
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) { }
    })
}

fun LayoutTransition.doOnEnd(onEnd: (transition: LayoutTransition,
                                     container: ViewGroup, view: View,
                                     transitionType: Int) -> Unit = {_, _, _, _ -> }) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) { }
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) =
            onEnd(a, b, c, d)
    })
}
