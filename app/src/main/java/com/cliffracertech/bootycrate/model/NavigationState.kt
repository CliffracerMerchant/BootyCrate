/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

/**
 * A state holder that contains the application's navigation state (described
 * as a value of [Screen]) in its member [visibleScreen]. A new destination
 * can be set via the method [navigateTo].
 */
@ActivityRetainedScoped
class NavigationState @Inject constructor() {
    /** An enum whose values describe the possible navigation destinations
     * for an instance of MainActivity: the shopping list, the inventory,
     * or the app settings screen. */
    enum class Screen { ShoppingList, Inventory, AppSettings;
        val isShoppingList get() = this == ShoppingList
        val isInventory get() = this == Inventory
        val isAppSettings get() = this == AppSettings
    }

    /** The current visible [Screen] in the MainActivity instance. */
    var visibleScreen by mutableStateOf(Screen.ShoppingList)
    val visibleScreenFlow = snapshotFlow { visibleScreen }

    fun navigateTo(screen: Screen) {
        visibleScreen = screen
    }
}

@ActivityRetainedScoped
class SearchQueryState @Inject constructor() {
    var query by mutableStateOf<String?>(null)
}