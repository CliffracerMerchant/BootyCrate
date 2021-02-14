/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.app.Application
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped

@HiltAndroidApp
class BootyCrateApplication: Application()

@Module @InstallIn(ActivityComponent::class)
object ImmModule {
    @Provides fun provideImm(@ApplicationContext context: Context) =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}

@Module @InstallIn(ActivityComponent::class)
object FragmentManagerModule {
    @Provides fun provideFragmentManager(@ActivityScoped activity: AppCompatActivity) =
        activity.supportFragmentManager
}