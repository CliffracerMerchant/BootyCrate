/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Application
import android.content.Context
import android.view.animation.AnimationUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Qualifier

@HiltAndroidApp
class BootyCrateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ViewModelItem.initColors(this)
    }
}

@Qualifier @Retention annotation class ViewTranslationAnimatorConfig
@Qualifier @Retention annotation class TransitionAnimatorConfig

@Module @InstallIn(ActivityComponent::class)
object AnimatorConfigProvider {
    @Provides @ViewTranslationAnimatorConfig
    fun provideViewTranslationAnimatorConfig(@ActivityContext context: Context) = AnimatorConfig(
        context.resources.getInteger(R.integer.animationDefaultDuration).toLong(),
        AnimationUtils.loadInterpolator(context, R.anim.default_interpolator))

    @Provides @TransitionAnimatorConfig
    fun provideTransitionAnimatorConfig(@ActivityContext  context: Context) = AnimatorConfig(
        context.resources.getInteger(R.integer.fragmentTransitionLongDuration).toLong(),
        AnimationUtils.loadInterpolator(context, R.anim.default_interpolator))
}