/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.LayoutTransition
import android.view.View
import android.view.ViewGroup

fun LayoutTransition.doOnStart(onStart: (transition: LayoutTransition,
                              container: ViewGroup, view: View,
                              transitionType: Int) -> Unit = {_, _, _, _ -> }) {
    addTransitionListener(object: LayoutTransition.TransitionListener {
        override fun startTransition(
            transition: LayoutTransition,
            container: ViewGroup,
            view: View,
            transitionType: Int
        ) {
            onStart(transition, container, view, transitionType)
        }
        override fun endTransition(a: LayoutTransition, b: ViewGroup, c: View, d: Int) { }
    })
}

enum class SelectionState { Selected, NotSelected }
enum class ExpansionState { Expanded, Collapsed }