/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BootyCrateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ViewModelItem.initColors(this)
    }
}