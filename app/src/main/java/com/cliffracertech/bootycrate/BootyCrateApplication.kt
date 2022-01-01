/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.app.Application
import android.util.Log
import com.cliffracertech.bootycrate.database.ListItem
import com.cliffracertech.bootycrate.utils.SoftKeyboard

class BootyCrateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SoftKeyboard.init(this)
        ListItem.initColors(this)
    }
}

fun dlog(msg: String) = Log.d("BootyCrate", msg)