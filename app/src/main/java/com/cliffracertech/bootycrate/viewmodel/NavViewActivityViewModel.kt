/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * A state holder representing the navigation state for a single activity
 * multiple fragment application that uses a NavigationBarView subclass.
 *
 * NavigationState contains the MutableStateFlows navViewSelectedItemId and
 * backStackSize. A single activity should modify the values for these
 * StateFlows when the navigation view's selected item or the fragment back
 * stack size changes. The activity should also collect the SharedFlow
 * requestedFragment's emissions and add fragments accordingly.
 *
 * Other users of NavigationState besides the application's single activity
 * should use ReadOnlyNavigationState instead, which provides read only
 * access to NavigationState's fields.
 */
@ActivityRetainedScoped
open class NavigationState @Inject constructor() {
    val navViewSelectedItemId = MutableStateFlow(-1)
    val backStackSize = MutableStateFlow(0)
    val requestedFragments = MutableSharedFlow<Fragment?>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}

/**
 * A read only instance of NavigationState.
 *
 * Entities other than a single activity application's sole activity that need
 * to access NavigationState's fields should use a ReadOnlyNavigationState
 * instead. navViewSelectedItemId and backStackSize are StateFlows whose values
 * represent the activity's NavigationBarView's selected menu item's id and the
 * size of the activity's fragment manager's back stack, respectively. The
 * function requestNewFragment can be called with an instance of a Fragment to
 * request the fragment's addition to the activity's fragment container.
 */
@ActivityRetainedScoped
class ReadOnlyNavigationState @Inject constructor(
    private val mutableState: NavigationState
) {
    val navViewSelectedItemId = mutableState.navViewSelectedItemId.asStateFlow()
    val backStackSize = mutableState.backStackSize.asStateFlow()

    fun requestNewFragment(fragment: Fragment) {
        mutableState.requestedFragments.tryEmit(fragment)
    }
}

@HiltViewModel
class NavViewActivityViewModel @Inject constructor(
    private val state: NavigationState
) : ViewModel() {

    val selectedNavViewId = state.navViewSelectedItemId.asStateFlow()

    val requestedFragments = state.requestedFragments.asSharedFlow()

    fun onPrimaryNavItemClick(navItemId: Int) {
        state.navViewSelectedItemId.value = navItemId
    }

    fun onBackStackSizeChanged(backStackSize: Int) {
        state.backStackSize.value = backStackSize
    }
}

