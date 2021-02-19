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
 * An object that contains static AnimatorConfigs.Config instances to help synchronize animations.
 *
 * The duration and interpolator of animations are the most important factors
 * in ensuring that separate but related animations are synchronized. Applying
 * the provided AnimatorConfigs.Config instances wherever animations are used
 * within the app make it easier to achieve this. The provided configs are:
 * - AnimatorConfigs.translation: For translation or size changing of views
 * - AnimatorConfigs.fadeIn: For views that fade in.
 * - AnimatorConfigs.fadeOut: For views that fade out.
 * - AnimatorConfigs.transition: For views involved in large scale transitions,
 *       e.g. between fragments.
 * - AnimatorConfigs.shoppingListItem: Like the translation config, but for
 *       shopping list item views. It uses a slightly shorter duration due to
 *       the small size of shopping list item views making the default transla-
 *       tion duration look a little slow.
 *
 * The function init must be called with a context instance sometime before any
 * of the configs are accessed, or else an exception will be thrown.
 */
object AnimatorConfigs {
    /** Config instances contain a duration and an animation interpolator. */
    data class Config(val duration: Long, val interpolator: TimeInterpolator)

    fun initConfigs(context: Context) {
        val translationDuration = context.resources.getInteger(R.integer.viewTranslationDuration).toLong()
        val transitionDuration = context.resources.getInteger(R.integer.fragmentTransitionDuration).toLong()
        val shoppingListItemDuration = context.resources.getInteger(R.integer.shoppingListItemViewTranslationDuration).toLong()
        val translationInterpolator = AnimationUtils.loadInterpolator(context, R.anim.translation_interpolator)
        val fadeInInterpolator = AnimationUtils.loadInterpolator(context, R.anim.fade_in_interpolator)
        val fadeOutInterpolator = AnimationUtils.loadInterpolator(context, R.anim.fade_out_interpolator)
        translationPrivate = Config(translationDuration, translationInterpolator)
        fadeInPrivate = Config(translationDuration, fadeInInterpolator)
        fadeOutPrivate = Config(translationDuration, fadeOutInterpolator)
        transitionPrivate = Config(transitionDuration, translationInterpolator)
        shoppingListItemPrivate = Config(shoppingListItemDuration, translationInterpolator)
    }

    private lateinit var translationPrivate: Config
    private lateinit var fadeInPrivate: Config
    private lateinit var fadeOutPrivate: Config
    private lateinit var transitionPrivate: Config
    private lateinit var shoppingListItemPrivate: Config

    val translation get() = translationPrivate
    val fadeIn get() = fadeInPrivate
    val fadeOut get() = fadeOutPrivate
    val transition get() = transitionPrivate
    val shoppingListItem get() = shoppingListItemPrivate
}

/** Apply an AnimatorConfigs.Config to an Animator object and return the object. */
fun Animator.applyConfig(config: AnimatorConfigs.Config) = apply {
    duration = config.duration
    interpolator = config.interpolator
}

/** Apply an AnimatorConfigs.Config to a ViewPropertyAnimator object and return the object. */
fun ViewPropertyAnimator.applyConfig(config: AnimatorConfigs.Config) = apply {
    duration = config.duration
    interpolator = config.interpolator
}

// The following value animator returning functions can be used similarly to object animators,
// but hopefully are more performant due to not using reflection to get the property setter.
/** Return a valueAnimator for an Int property with the update listener already set. */
fun valueAnimatorOfInt(
    setter: (Int) -> Unit, fromValue: Int, toValue: Int,
    config: AnimatorConfigs.Config = AnimatorConfigs.translation,
): ValueAnimator = ValueAnimator.ofInt(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    applyConfig(config)
}

/** Return a valueAnimator for a Float property with the update listener already set. */
fun valueAnimatorOfFloat(
    setter: (Float) -> Unit, fromValue: Float, toValue: Float,
    config: AnimatorConfigs.Config = AnimatorConfigs.translation,
): ValueAnimator = ValueAnimator.ofFloat(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Float) }
    applyConfig(config)
}

/** Return a valueAnimator for an ARGB property with the update listener already set. */
fun valueAnimatorOfArgb(
    setter: (Int) -> Unit, fromValue: Int, toValue: Int,
    config: AnimatorConfigs.Config = AnimatorConfigs.translation,
): ValueAnimator = ValueAnimator.ofArgb(fromValue, toValue).apply {
    addUpdateListener { setter(it.animatedValue as Int) }
    applyConfig(config)
}

/**
 * Return a LayoutTransition whose durations and interpolators match the given AnimatorConfigs.Config.
 *
 * Note that the APPEARING and DISAPPEARING transitions will use the AnimatorConfigs.fadeIn
 * and AnimatorConfigs.fadeOut interpolators instead of the interpolator of the config parameter.
 */
fun layoutTransition(config: AnimatorConfigs.Config) = LayoutTransition().apply {
    setStartDelay(LayoutTransition.CHANGE_APPEARING, 0)
    setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
    setStartDelay(LayoutTransition.APPEARING, 0)
    setStartDelay(LayoutTransition.DISAPPEARING, 0)
    setStartDelay(LayoutTransition.CHANGING, 0)
    setInterpolator(LayoutTransition.CHANGE_APPEARING, config.interpolator)
    setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, config.interpolator)
    setInterpolator(LayoutTransition.APPEARING, AnimatorConfigs.fadeIn.interpolator)
    setInterpolator(LayoutTransition.DISAPPEARING, AnimatorConfigs.fadeOut.interpolator)
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
