/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.fragment.app.Fragment

/** A Fragment subclass with API modifications.
 *
 *  The main fragments in BootyCrate are designed to be added to the fragment
 *  manager at the same time, and have their views' visibilities set to View.-
 *  VISIBLE or View.GONE when they switched between. androidx.fragment.app.Frag-
 *  ment's API obviously lacks callbacks for when such a switch occurs. MainAct-
 *  ivityFragment adds the functions onActiveStateChanged and setOptionsMenu-
 *  ItemsVisible to the API to make them easier to use in this way.
 *
 *  onActiveStateChanged is called on both main fragments (the inventory frag-
 *  ment and the shopping list fragment) when the active fragment is toggled.
 *  onActiveStateChanged(false) will be called on the fragment being switched
 *  away from, and onActiveStateChanged(true) will be called on the fragment
 *  being switched to, in that order. The current active / inactive state of
 *  the fragment can be queried and set via the property isActive.
 *
 *  Because onCreateOptionsMenu is typically called after the activities and /
 *  or fragments containing the menu are visible, the function initOptionsMenu
 *  should be called with an instance of the menu when it is ready. It is up
 *  to the activities and / or fragment manager subclasses that use MainActivi-
 *  tyFragments to call initOptionsMenu and onActiveStateChanged at the appro-
 *  priate times.
 *
 *  onPrepareOptionsMenu is only called when the user clicks on the options
 *  menu overflow button, and is therefore not an appropriate place to hide
 *  menu items that are displayed in the action bar instead of in the overflow
 *  menu. The open function setOptionsMenuItemsVisible(showing = false) is
 *  called when the fragment is set to inactive, and is likewise called with
 *  showing = true when the fragment is set to active. Subclasses can override
 *  this function with their own implementation (still calling the original
 *  implementation via super.setOptionsMenuItemsVisible()) to customize how the
 *  options menu appears when they are visible. setOptionsMenuItemsVisible does
 *  not need to be called manually, as the default implementation of onActive-
 *  StateChanged will call it, and initOptionsMenu will call it when the
 *  options menu is initialized. */
open class MainActivityFragment(isActive: Boolean = false): Fragment() {
    protected var menu: Menu? = null
    var isActive: Boolean = isActive
        set(value) { field = value
                     onActiveStateChanged(value) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isActive = savedInstanceState?.getBoolean("wasActiveFragment") ?: isActive
        onActiveStateChanged(isActive)
        if (!isActive) view.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("wasActiveFragment", isActive)
    }

    protected open fun onActiveStateChanged(active: Boolean) =
        setOptionsMenuItemsVisible(active)

    fun initOptionsMenu(menu: Menu) {
        if (this.menu != null) return
        this.menu = menu
        setOptionsMenuItemsVisible(isVisible)
    }

    protected open fun setOptionsMenuItemsVisible(showing: Boolean) { }
}