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

/**
 * A data class that contains a TimeInterpolator and a duration for syncing animations, as well as predefined static instances.
 *
 * The duration and interpolator of animations are the most important factors
 * in ensuring that separate but related animations are synchronized. Applying
 * the predefined AnimatorConfig instances wherever animations are used within
 * the app make it easier to achieve this. The provided configs are:
 * - AnimatorConfig.translation: For translation or size changing of views
 * - AnimatorConfig.fadeIn: For views that fade in.
 * - AnimatorConfig.fadeOut: For views that fade out.
 * - AnimatorConfig.transition: For views involved in large scale transitions,
 *       e.g. between fragments.
 * - AnimatorConfig.shoppingListItem: Like the translation config, but for
 *       shopping list item views. It uses a slightly shorter duration due to
 *       the small size of shopping list item views making the default transla-
 *       tion duration look a little slow.
 *
 * The function init must be called with a context instance sometime before any
 * of the configs are accessed, or else an exception will be thrown.
 */
data class AnimatorConfig(val duration: Long, val interpolator: TimeInterpolator) {
    companion object {
        fun initConfigs(context: Context) {
            val translationDuration = context.resources.getInteger(R.integer.viewTranslationDuration).toLong()
            val transitionDuration = context.resources.getInteger(R.integer.fragmentTransitionDuration).toLong()
            val shoppingListItemDuration = context.resources.getInteger(R.integer.shoppingListItemViewTranslationDuration).toLong()
            val translationInterpolator = AnimationUtils.loadInterpolator(context, R.anim.translation_interpolator)
            val fadeInInterpolator = AnimationUtils.loadInterpolator(context, R.anim.fade_in_interpolator)
            val fadeOutInterpolator = AnimationUtils.loadInterpolator(context, R.anim.fade_out_interpolator)
            translationPrivate = AnimatorConfig(translationDuration, translationInterpolator)
            fadeInPrivate = AnimatorConfig(translationDuration, fadeInInterpolator)
            fadeOutPrivate = AnimatorConfig(translationDuration, fadeOutInterpolator)
            transitionPrivate = AnimatorConfig(transitionDuration, translationInterpolator)
            shoppingListItemPrivate = AnimatorConfig(shoppingListItemDuration, translationInterpolator)
        }

        private lateinit var translationPrivate: AnimatorConfig
        private lateinit var fadeInPrivate: AnimatorConfig
        private lateinit var fadeOutPrivate: AnimatorConfig
        private lateinit var transitionPrivate: AnimatorConfig
        private lateinit var shoppingListItemPrivate: AnimatorConfig

        val translation get() = translationPrivate
        val fadeIn get() = fadeInPrivate
        val fadeOut get() = fadeOutPrivate
        val transition get() = transitionPrivate
        val shoppingListItem get() = shoppingListItemPrivate
    }
}

/** Apply an AnimatorConfig to an Animator object and return the object. */
fun <T: Animator>T.applyConfig(config: AnimatorConfig) = apply {
    duration = config.duration
    interpolator = config.interpolator
}

/** Apply an AnimatorConfig to a ViewPropertyAnimator object and return the object. */
fun ViewPropertyAnimator.applyConfig(config: AnimatorConfig) = apply {
    duration = config.duration
    interpolator = config.interpolator
}

// The following value animator returning functions can be used similarly to object animators,
// but hopefully are more performant due to not using reflection to get the property setter.
/** Return a valueAnimator for an Int property with the update listener already set. */
fun valueAnimatorOfInt(
    setter: (Int) -> Unit, fromValue: Int, toValue: Int,
    config: AnimatorConfig = AnimatorConfig.translation,
): ValueAnimator = ValueAnimator.ofInt(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    applyConfig(config)
}

/** Return a valueAnimator for a Float property with the update listener already set. */
fun valueAnimatorOfFloat(
    setter: (Float) -> Unit, fromValue: Float, toValue: Float,
    config: AnimatorConfig = AnimatorConfig.translation,
): ValueAnimator = ValueAnimator.ofFloat(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Float) }
    applyConfig(config)
}

/** Return a valueAnimator for an ARGB property with the update listener already set. */
fun valueAnimatorOfArgb(
    setter: (Int) -> Unit, fromValue: Int, toValue: Int,
    config: AnimatorConfig = AnimatorConfig.translation,
): ValueAnimator = ValueAnimator.ofArgb(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    applyConfig(config)
}

/**
 * Return a LayoutTransition whose durations and interpolators match the given AnimatorConfig.
 *
 * Note that the APPEARING and DISAPPEARING transitions will use the AnimatorConfig.fadeIn
 * and AnimatorConfig.fadeOut interpolators instead of the interpolator of the config parameter.
 */
fun layoutTransition(config: AnimatorConfig) = LayoutTransition().apply {
    setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
    setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
    setStartDelay(LayoutTransition.APPEARING, 0)
    setStartDelay(LayoutTransition.DISAPPEARING, 0)
    setStartDelay(LayoutTransition.CHANGING, 0)
    setInterpolator(LayoutTransition.CHANGE_APPEARING, config.interpolator)
    setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, config.interpolator)
    setInterpolator(LayoutTransition.APPEARING, AnimatorConfig.fadeIn.interpolator)
    setInterpolator(LayoutTransition.DISAPPEARING, AnimatorConfig.fadeOut.interpolator)
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
