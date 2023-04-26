/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.app.Application
import androidx.compose.animation.core.spring
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@HiltAndroidApp
class BootyCrateApplication : Application()

open class ViewModel(
    val coroutineScope: CoroutineScope
) : androidx.lifecycle.ViewModel() {
    override fun onCleared() { coroutineScope.cancel() }

    companion object {
        fun viewModelScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

const val springStiffness = 700f / 1f
fun <T> defaultSpring() = spring<T>(stiffness = springStiffness)
const val tweenDuration = 200