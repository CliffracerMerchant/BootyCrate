/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

/**
 * A state holder that contains the application's navigation state (described
 * as a value of [Screen]) in its member [visibleScreen]. A new [RootScreen]
 * (i.e. one of the possible [Screen]s when the back stack is empty) can be
 * chosen via the method [navigateToRootScreen]. An [AdditionalScreen] can be
 * added to the back stack via the method [addToStack], while the top-most
 * [AdditionalScreen] can be removed from the stack via the method [popStack].
 */
@ActivityRetainedScoped
class NavigationState @Inject constructor() {

    /** A navigation destination for the app. */
    sealed class Screen

    /** RootScreen's values describe the possible root [Screen]s for the
     * app, i.e. the possible [Screen]s when the back stack is empty. */
    sealed class RootScreen: Screen() {
        object ShoppingList : RootScreen()
        object Inventory : RootScreen()
    }

    /** AdditionalScreen's value describe the additional [Screen]s
     * that can appear on top of one of the [RootScreen]s.*/
    sealed class AdditionalScreen: Screen() {
        object AppSettings : AdditionalScreen()
    }

    private var rootScreen by mutableStateOf<RootScreen>(RootScreen.ShoppingList)
    private val additionalScreenStack = mutableStateListOf<AdditionalScreen>()

    /** The current visible [Screen] in the MainActivity instance. */
    val visibleScreen by derivedStateOf {
        if (additionalScreenStack.isNotEmpty())
            additionalScreenStack.last()
        else rootScreen
    }
    val visibleScreenFlow = snapshotFlow { visibleScreen }

    /** Choose a new [RootScreen] for the app. This is a no-op if the back
     * stack is not empty (i.e. if [visibleScreen] is a value of [AdditionalScreen]
     * rather than [RootScreen]. */
    fun navigateToRootScreen(screen: RootScreen) {
        if (additionalScreenStack.isEmpty())
            rootScreen = screen
    }

    /** Add a new [AdditionalScreen] to the back stack. This is a no-op if the
     * top-most [AdditionalScreen] on the back stack is the same as [screen]. */
    fun addToStack(screen: AdditionalScreen) {
        if (additionalScreenStack.last() != screen)
            additionalScreenStack.add(screen)
    }

    /** Pop the top element off of the back stack. The return
     * value indicates whether or not the top element was removed. */
    fun popStack(): Boolean {
        if (additionalScreenStack.isNotEmpty()) {
            additionalScreenStack.removeLast()
            return true
        }
        return false
    }
}

@ActivityRetainedScoped
class SearchQueryState @Inject constructor() {
    var query by mutableStateOf<String?>(null)
}

@ActivityRetainedScoped
class NewItemDialogVisibilityState @Inject constructor() {
    var showingNewShoppingListItemDialog by mutableStateOf(false)
    var showingNewInventoryItemDialog by mutableStateOf(false)
}