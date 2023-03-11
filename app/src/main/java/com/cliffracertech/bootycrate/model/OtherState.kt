/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.cliffracertech.bootycrate.model.NavigationState.*
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
    sealed class Screen(val stackIndex: Int) {
        val isRootScreen get() = this is RootScreen
        val isAdditionalScreen get() = this is AdditionalScreen
        val isShoppingList get() = this is RootScreen.ShoppingList
        val isInventory get() = this is RootScreen.Inventory
        val isAppSettings get() = this is AdditionalScreen && this.type.isAppSettings
    }

    /** RootScreen's values describe the possible root [Screen]s for the
     * app, i.e. the possible [Screen]s when the back stack is empty. The
     * [leftToRightIndex] is the left to right ordering of the screen among
     * all [RootScreen]s, and can be used to determine the appropriate
     * transition animation to play. */
    sealed class RootScreen(
        val leftToRightIndex: Int
    ): Screen(stackIndex = 0) {
        object ShoppingList : RootScreen(0)
        object Inventory : RootScreen(1)
    }

    /** AdditionalScreenType's values describe the types of additional
     * [Screen]s that can appear on top of one of the [RootScreen]s.*/
    enum class AdditionalScreenType { AppSettings;
        val isAppSettings get() = this == AppSettings
    }

    /** AdditionalScreen describes a given Screen that can appear over
     * one of the [RootScreen]s. The type of screen is described by the
     * value of [type]. [Screen]'s property [stackIndex] can be used in
     * determining the appropriate transition animation to play. */
    class AdditionalScreen(
        val type: AdditionalScreenType,
        stackIndex: Int,
    ): Screen(stackIndex)

    var rootScreen by mutableStateOf<RootScreen>(RootScreen.ShoppingList)
        private set
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

    /** Add a new [AdditionalScreen] to the back stack. This is a no-op
     * if the top-most [AdditionalScreen] on the back stack's is the same
     * [AdditionalScreenType] as [screenType]. */
    fun addToStack(screenType: AdditionalScreenType) {
        if (additionalScreenStack.lastOrNull()?.type != screenType)
            additionalScreenStack.add(
                AdditionalScreen(screenType, additionalScreenStack.size + 1))
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

// SelectionState is not ActivityRetainedScoped because we want to allow
// multiple instances (one for the shopping list and one for the inventory).
class SelectionState @Inject constructor() {
    val selectedIds: SnapshotStateMap<Long, Unit> = mutableStateMapOf()

    fun toggle(id: Long) {
        if (selectedIds[id] != null)
            selectedIds[id] = Unit
        else selectedIds.remove(id)
    }

    fun clear() = selectedIds.clear()
}

@ActivityRetainedScoped
class NewItemDialogVisibilityState @Inject constructor() {
    var showingNewShoppingListItemDialog by mutableStateOf(false)
    var showingNewInventoryItemDialog by mutableStateOf(false)
}