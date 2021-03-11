/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import com.cliffracermerchant.bootycrate.databinding.MainActivityBinding

interface MainActivityFragment {
    val name: String
    fun showsOptionsMenu(): Boolean
    fun showsBottomAppBar(): Boolean
    fun showsCheckoutButton(): Boolean
    fun onActiveStateChanged(isActive: Boolean, ui: MainActivityBinding)
    fun inactiveToActiveAnimatorResId(): Int
    fun oldFragmentActiveToInactiveAnimatorResId(): Int
    fun onBackPressed()
}

/**
 * A Fragment subclass with API modifications.
 *
 * The main fragments in BootyCrate are designed to be added to the fragment
 * manager at the same time, and have their views' visibilities set to View.-
 * VISIBLE or View.INVISIBLE when the user switches between them. androidx.-
 * fragment.app.Fragment's API obviously lacks callbacks for when such a
 * switch occurs. MainActivityFragment adds the functions onActiveStateChanged
 * and setOptionsMenuItemsVisible to the API to make them easier to use in this
 * way.
 *
 * onActiveStateChanged is called on both main fragments (the inventory frag-
 * ment and the shopping list fragment) when the active fragment is toggled.
 * onActiveStateChanged(false) will be called on the fragment being switched
 * away from, and onActiveStateChanged(true) will be called on the fragment
 * being switched to, in that order. The current active / inactive state of
 * the fragment can be queried and set via the property isActive.
 *
 * onPrepareOptionsMenu is only called when the user clicks on the options
 * menu overflow button, and is therefore not an appropriate place to hide
 * menu items that are displayed in the action bar instead of in the overflow
 * menu. The open function setOptionsMenuItemsVisible(showing = false) is
 * called when the fragment is set to inactive, and is likewise called with
 * showing = true when the fragment is set to active. Subclasses can override
 * this function with their own implementation to customize how the options
 * menu appears when they are visible.
 */
//@AndroidEntryPoint
//open class MainActivityFragment(isActive: Boolean = false): Fragment(), MainActivityFragmentInterface {
//    protected var mainActivityUi: MainActivityBinding? = null
//
//    var isActive: Boolean = isActive
//        set(value) { field = value
//                     onActiveStateChanged(value) }
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        val activity = context as? MainActivity
//            ?: throw IllegalStateException("The parent activity for a MainActivityFragment must be an instance of MainActivity.")
//        mainActivityUi = activity.ui
//    }
//
//    override fun onDetach() {
//        super.onDetach()
//        mainActivityUi = null
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        isActive = savedInstanceState?.getBoolean("wasActiveFragment") ?: isActive
//        if (!isActive) view.visibility = View.INVISIBLE
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putBoolean("wasActiveFragment", isActive)
//    }
//
//    protected open fun onActiveStateChanged(active: Boolean) =
//        setOptionsMenuItemsVisible(active)
//
//    protected open fun setOptionsMenuItemsVisible(showing: Boolean) { }
//}