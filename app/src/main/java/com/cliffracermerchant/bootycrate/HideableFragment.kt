/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.view.Menu
import androidx.fragment.app.Fragment

/** A Fragment subclass that adds several functions to the fragment API.
 *
 *  The main fragments in BootyCrate are designed to be alternatively shown and
 *  hidden when they are used, rather than replaced. Unfortunately androidx.-
 *  fragment.app.Fragment's API is somewhat limited when used in this way.
 *  HideableFragment adds the functions onAboutToBeHidden and setOptionsMenu-
 *  ItemsVisible to the API to make them easier to use in this way.
 *
 *  In a activity containing multiple fragments that are hidden or shown so
 *  that only one is visible to the user, a typical use case might be temporar-
 *  ily showing both fragments in order for a transition animation to be shown,
 *  and then hiding the exiting fragment. Unfortunately this means that onHid-
 *  denChanged will be called for the entering fragment right away, but will be
 *  called for the exiting fragment only after the transition animation is fin-
 *  ished. An onAboutToBeHidden override will allow the fragment to do work
 *  before the other fragment is shown (e.g. to save some sort of state that
 *  will be overwritten by the entering fragment).
 *
 *  onCreateOptionsMenu overrides in a fragment seem to only be called for visi-
 *  ble fragments, but not hidden ones. Furthermore, onPrepareOptionsMenu is
 *  only called when the user clicks on the options menu overflow button, and
 *  is therefore not an appropriate place to hide menu items that are displayed
 *  in the action bar instead of in the overflow menu. The open function set-
 *  OptionsMenuItemsVisible(showing = false) is called when the fragment is
 *  about to be hidden, and is likewise called with showing = true when the
 *  fragment is shown. Subclasses can override this function with their own
 *  implementation (still calling the original implementation via super.set-
 *  OptionsMenuIemsVisible()) to customize how the options menu appears when
 *  they are visible.
 *
 *  Because onCreateOptionsMenu is typically called after the activities and /
 *  or fragments containing the menu are visible, the function initOptionsMenu
 *  should be called with an instance of the menu when it is ready. It is up
 *  to the activities and / or fragment manager subclasses that use Hideable-
 *  Fragments to call initOptionsMenu and onAboutToBeHidden at the appropriate
 *  times. */
open class HideableFragment: Fragment() {
    protected var menu: Menu? = null

    open fun onAboutToBeHidden() {
        setOptionsMenuItemsVisible(false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) setOptionsMenuItemsVisible(true)
    }

    fun initOptionsMenu(menu: Menu) {
        if (this.menu != null) return
        this.menu = menu
        setOptionsMenuItemsVisible(isVisible)
    }

    protected open fun setOptionsMenuItemsVisible(showing: Boolean) { }
}