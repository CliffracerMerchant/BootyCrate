/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dlog
import com.cliffracertech.bootycrate.utils.*
import com.cliffracertech.bootycrate.viewmodel.NavViewActivityViewModel
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * An activity that, when linked up with a NavigationBarView instance, will
 * automatically create and manage fragment instances for each navigation view
 * menu item.
 *
 * NavViewActivity will automatically add a fragment instance for each menu
 * item of the NavigationBarView that it is connected to. These automatically
 * generated fragments are called primary fragments. These primary fragments
 * are intended to be used for fragments that need to be switched between often,
 * and for which repeated destruction and recreation is undesirable. To
 * facilitate this, the primary fragments are never replaced, but instead will
 * have their views' visibilities set to View.GONE when not in use. Because
 * this increases memory consumption, it is not recommended to add more than a
 * few commonly used fragments. If other fragments need to be used temporarily,
 * this can be accomplished using the function addSecondaryFragment.
 *
 * In order for the primary fragment auto-generation to succeed, the property
 * navigationView must be initialized in a subclass before NavViewActivity's
 * onCreate is called, the property fragmentContainerId must be set to the id
 * of the container that the fragments will be added to, and the resource
 * pointed to by the id R.array.nav_view_activity_fragments must be a string
 * array that contains the names of the primary fragments, including package
 * name. This being the case, NavViewActivity will attempt to associate each
 * primary fragment in the order they are listed with a navigation bar menu
 * item, skipping disabled menu items. If the navigation menu does not have at
 * least as many enabled menu items as the number of primary fragments, an
 * indexOutOfBoundsException will be thrown. If primary fragment auto-
 * generation succeeds, NavViewActivity will set itself as the NavigationBarView's
 * OnItemSelectedListener, and will automatically switch to the corresponding
 * fragment with an animation.
 *
 * NavViewActivity uses its own slide left or right animations for its primary
 * fragments, although the duration and the interpolators used for these
 * animations can be set through the property primaryFragmentTransitionAnimatorConfig.
 * The animations used for the addition or popping of secondary fragments can
 * either be passed in directly to a call of addSecondaryFragment, or will
 * default to the value of the properties secondaryFragmentDefaultEnterAnimResId
 * and secondaryFragmentDefaultExitAnimResId.
 *
 * The property visibleFragment will always be equal to the topmost fragment,
 * including secondary fragments. To listen to changes in the selected
 * navigation menu item or the backstack size, obtain an instance of
 * NavigationState scoped to the instance of NavViewActivity being used and
 * collect the StateFlow properties navViewSelectedId and backStackSize,
 * respectively.
 */
abstract class NavViewActivity : AppCompatActivity() {
    private val viewModel: NavViewActivityViewModel by viewModels()
    protected var fragmentContainerId = 0
    protected lateinit var navigationView: NavigationBarView
    private val navBarMenuItemFragmentMap = mutableMapOf<Int, Fragment>()

    val visibleFragment get() =
        if (showingPrimaryFragment) selectedPrimaryFragment
        else supportFragmentManager.run {
            findFragmentByTag(getBackStackEntryAt(backStackEntryCount - 1).name)
        }

    private val showingPrimaryFragment get() =
        supportFragmentManager.backStackEntryCount == 0
    private val selectedPrimaryFragment get() =
        navBarMenuItemFragmentMap.getValue(navigationView.selectedItemId)

    var primaryFragmentTransitionAnimatorConfig: AnimatorConfig? = null
    protected var defaultSecondaryFragmentEnterAnimResId: Int = 0
    protected var defaultSecondaryFragmentExitAnimResId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createOrRestoreFragments(savedInstanceState)
        supportFragmentManager.addOnBackStackChangedListener {
            viewModel.onBackStackSizeChanged(supportFragmentManager.backStackEntryCount)
            // The hidden primary fragments seem to have their
            // visibility reset at this point for some reason.
            if (!showingPrimaryFragment)
                for (fragment in navBarMenuItemFragmentMap.values)
                    fragment.view?.isVisible = false
        }
        navigationView.setOnItemSelectedListener{
            viewModel.onPrimaryNavItemClick(it.itemId)
            false
        }
        repeatWhenStarted {
            launch { viewModel.selectedNavViewId.collect(::selectPrimaryFragmentAt) }
            launch { viewModel.requestedFragments.collect {
                if (it != null) addSecondaryFragment(it)
            }}
        }
    }

    // See the documentation for checkQueuedMenuItemPress for an
    // explanation of queuedMenuItem and menuItemLastPressTimestamp
    private var queuedNavItemPress: Int? = null
    private var navItemLastPressTimestamp = 0L
    private var primaryTransitionInProgress = false
    /** Attempt to switch to a new primary fragment corresponding to the @param navItemId. */
    private fun selectPrimaryFragmentAt(navItemId: Int) {
        if (navItemId == navigationView.selectedItemId)
            return
        if (primaryTransitionInProgress) {
            queuedNavItemPress = navItemId
            navItemLastPressTimestamp = System.currentTimeMillis()
            return
        }

        val newFragment = navBarMenuItemFragmentMap[navItemId] ?: return
        val newMenuItem = navigationView.menu.findItem(navItemId)
        val oldMenuItem = navigationView.menu.findItem(navigationView.selectedItemId)
        val oldFragment = navBarMenuItemFragmentMap.getValue(oldMenuItem.itemId)
        newMenuItem.isChecked = true

        if (!showingPrimaryFragment) {
            // If there is a secondary fragment on top of the primary fragments,
            // then we can skip the animations since they won't be seen anyways.
            oldFragment.view?.isVisible = false
            newFragment.view?.isVisible = true
            return
        }

        val leftToRight = oldMenuItem.order < newMenuItem.order
        primaryTransitionInProgress = true
        oldFragment.view?.apply {
            val endTranslation = width * if (leftToRight) -1f else 1f
            animate().translationX(endTranslation).withLayer()
                .applyConfig(primaryFragmentTransitionAnimatorConfig)
                .withEndAction {
                    visibility = View.GONE
                    primaryTransitionInProgress = false
                    checkQueuedMenuItemPress()
                }.start()
        }
        newFragment.view?.apply {
            translationX = width * if (leftToRight) 1f else -1f
            isVisible = true
            animate().translationX(0f).withLayer()
                .applyConfig(primaryFragmentTransitionAnimatorConfig)
                .start()
        }
    }

    /** To prevent visual bugs due to new animations starting before the old
     * ones are finished while still allowing the UI to feel responsive,
     * NavViewActivity queues navigation menu item presses that occur during
     * the last half of the transition animation, and plays them when the
     * transition animation is finished. */
    private fun checkQueuedMenuItemPress() {
        queuedNavItemPress?.let {
            val now = System.currentTimeMillis()
            val animDuration = primaryFragmentTransitionAnimatorConfig?.duration ?: 300L
            val allowableMargin = animDuration / 2
            if ((navItemLastPressTimestamp + allowableMargin) >= now)
                viewModel.onPrimaryNavItemClick(it)
        }
        this.queuedNavItemPress = null
    }

    fun addSecondaryFragment(
        fragment: Fragment,
        enterAnimResId: Int? = null,
        exitAnimResId: Int? = null
    ) {
        val resolvedEnterAnimResId = enterAnimResId ?: defaultSecondaryFragmentEnterAnimResId
        val resolvedExitAnimResId = exitAnimResId ?: defaultSecondaryFragmentExitAnimResId

        val tag = supportFragmentManager.backStackEntryCount.toString()
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(resolvedEnterAnimResId, resolvedExitAnimResId,
                                 resolvedEnterAnimResId, resolvedExitAnimResId)
        if (showingPrimaryFragment)
            transaction.hide(selectedPrimaryFragment)
        else visibleFragment?.let { transaction.remove(it) }
        transaction.add(fragmentContainerId, fragment, tag)
        transaction.addToBackStack(tag).commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        for (idAndFragment in navBarMenuItemFragmentMap)
            supportFragmentManager.putFragment(
                outState, idAndFragment.key.toString(), idAndFragment.value)
    }

    private fun createOrRestoreFragments(savedInstanceState: Bundle?) {
        val fragmentNames = resources.getStringArray(R.array.bottom_nav_view_activity_fragments)
        var menuItemIndex = 0
        var assignedFragments = 0
        try { while (assignedFragments < fragmentNames.size) {
            val name = fragmentNames[assignedFragments]
            val menuItem = navigationView.menu.getItem(menuItemIndex++)
            if (!menuItem.isEnabled) continue

            val fragment = supportFragmentManager.run {
                if (savedInstanceState == null)
                    fragmentFactory.instantiate(ClassLoader.getSystemClassLoader(), name)
                else getFragment(savedInstanceState, menuItem.itemId.toString())
                    ?: throw IllegalStateException("The saved instance state must contain a " +
                                                   "fragment for each navigation menu item.")
            }
            navBarMenuItemFragmentMap[menuItem.itemId] = fragment
            assignedFragments++
        }}
        catch (e: IndexOutOfBoundsException) { throw IndexOutOfBoundsException(
            "The navigation menu of the navigation bar must have at least as many enabled menu" +
            "items as the number of fragments named in R.array.bottom_nav_view_activity_fragments.")
        }
        val transaction = supportFragmentManager.beginTransaction()
            .runOnCommit(::initPrimaryFragmentVisibility)
        if (savedInstanceState == null)
            for (idAndFragment in navBarMenuItemFragmentMap)
                transaction.add(fragmentContainerId, idAndFragment.value)
        transaction.commit()
    }

    private fun initPrimaryFragmentVisibility() {
        if (viewModel.selectedNavViewId.value == -1) {
            val firstItemId = navigationView.menu.getItemOrNull(0)?.itemId
            firstItemId?.let { viewModel.onPrimaryNavItemClick(it) }
        }
        else navigationView.menu.findItem(viewModel.selectedNavViewId.value)?.isChecked = true
        navBarMenuItemFragmentMap.forEach { menuItemIdAndFragment ->
            val menuItemId = menuItemIdAndFragment.key
            val fragment = menuItemIdAndFragment.value
            if (menuItemId != navigationView.selectedItemId || !showingPrimaryFragment)
                // Even though the inactive fragments' views' visibilities are later
                // set to GONE, setting them to INVISIBLE this first time ensures
                // that the transition animation plays correctly the first time.
                fragment.view?.visibility = View.INVISIBLE
        }
    }
}