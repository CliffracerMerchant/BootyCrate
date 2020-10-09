/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.view.ViewPropertyAnimator

class ViewPropertyAnimatorSet {
    private val animators = mutableListOf<ViewPropertyAnimator>()

    fun add(anim: ViewPropertyAnimator) = animators.add(anim)

    fun start() {
        if (animators.isEmpty()) return
        animators.removeAt(animators.size - 1).withStartAction {
            for (anim in animators) anim.start()
        }.start()
    }
}

enum class SelectionState { Selected, NotSelected }
enum class ExpansionState { Expanded, Collapsed }