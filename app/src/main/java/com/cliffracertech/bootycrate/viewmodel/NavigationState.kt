/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import com.cliffracertech.bootycrate.BootyCrateApplication
import com.cliffracertech.bootycrate.R
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * A state holder representing the navigation state for a single activity
 * multiple fragment application that uses a NavigationBarView subclass.
 *
 * MutableNavigationState contains the StateFlow selectedNavItemId. A single
 * activity should notify the MutableNavigationState of changes to this value
 * using the function notifyNavItemSelected when the navigation view's selected
 * item changes. The activity should also collect the emissions of the
 * SharedFlow requestedFragments and add the fragments accordingly.
 *
 * Entities other than the application's single activity that need to read the
 * navigation view's selected item should use the read-only NavigationState
 * instead, which provides read-only access to selectedNavItemId and the
 * function requestNewFragment, but without the ability to manipulate the
 * underlying value of selectedNavItemId directly.
 *
 * Meta-note: Ideally MutableNavigationState would act as a model level single
 * source of truth regarding an application's navigation state. Unfortunately
 * this isn't really possible within the Android framework due to the fact that
 * an activity's fragment manager, which ultimately has control over the
 * application's navigation state, is inseparable from its FragmentActivity.
 * This leads to the undesirable requirement for the application's sole
 * FragmentActivity needing to remember to call notifyNavItemSelected.
 * Nevertheless, so long as the activity correctly calls this function when it
 * should, NavigationState will allow other entities to treat it as the single
 * source of truth regarding the application's navigation state.
 */
@ActivityRetainedScoped
open class MutableNavigationState @Inject constructor() {
    private val _selectedNavItemId = MutableStateFlow(-1)
    val selectedNavItemId = _selectedNavItemId.asStateFlow()

    fun notifyNavItemSelected(itemId: Int) {
        _selectedNavItemId.value = itemId
    }
}

/** A read-only state holder for an activity's selected navigation view item. */
@ActivityRetainedScoped
class NavigationState @Inject constructor(
    mutableState: MutableNavigationState
) {
    val selectedNavItemId = mutableState.selectedNavItemId
}

/**
 * A state holder representing the navigation state for a MainActivity instance.
 *
 * MainActivityNavigationState wraps an instance of NavigationState and adds
 * the Flow activeFragment, which represents the current visible fragment in
 * the MainActivity instance from among the possible values of ShoppingListFragment,
 * InventoryFragment, or another fragment which covers these two. Changes in
 * the application's back stack size should be reported to MainActivityNavigationState
 * using the function notifyBackStackSizeChanged.
 */
@ActivityRetainedScoped
class MainActivityNavigationState @Inject constructor(
    state: NavigationState
) {
    private val backStackSize = MutableStateFlow(0)

    fun notifyBackStackSizeChanged(backStackSize: Int) {
        this.backStackSize.value = backStackSize
    }

    /** An enum class whose values describe the possible navigation destinations
     * for an instance of MainActivity: the shopping list, the inventory, or
     * some other destination that covers the shopping list or inventory (e.g.
     * an app settings screen). */
    enum class Screen { ShoppingList, Inventory, Other;
        val isShoppingList get() = this == ShoppingList
        val isInventory get() = this == Inventory
        val isOther get() = this == Other
    }

    /** The current visible fragment in the MainActivity instance. */
    val visibleScreen = combine(
        state.selectedNavItemId,
        backStackSize
    ) { navItemId, backStackSize -> when {
        backStackSize > 0 ->              Screen.Other
        navItemId == R.id.shoppingList -> Screen.ShoppingList
        navItemId == R.id.inventory ->    Screen.Inventory
        else ->                           Screen.Other
        // The drop(1) is to avoid the navViewSelectedItemId's initial state of -1,
        // which leads to an activeFragment value of Fragment.Other instead of the
        // intended initial value of Fragment.ShoppingList
    }}.drop(1).stateIn(BootyCrateApplication.coroutineScope,
                       SharingStarted.Eagerly, Screen.ShoppingList)
}

/** A state holder for a search query entry. */
@ActivityRetainedScoped
class SearchQueryState @Inject constructor() {
    val query = MutableStateFlow<String?>(null)
}