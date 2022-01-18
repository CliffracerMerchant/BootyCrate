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
import androidx.lifecycle.ViewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.applyConfig
import com.cliffracertech.bootycrate.utils.recollectWhenStarted
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Module @InstallIn(ActivityRetainedComponent::class)
class NavigationManager {
    val activeFragment = MutableStateFlow<Fragment?>(null)
}

@Module @InstallIn(ActivityRetainedComponent::class)
class ReadOnlyNavigationManager @Inject constructor(
    manager: NavigationManager
) {
    val activeFragment = manager.activeFragment.asStateFlow()
}

@HiltViewModel
class BottomNavViewActivityViewModel @Inject constructor(
    private val manager: NavigationManager
) : ViewModel() {
    fun onActiveFragmentChanged(newActiveFragment: Fragment?) {
        manager.activeFragment.value = newActiveFragment
    }

    private val _selectedPrimaryNavItemId = MutableStateFlow(-1)
    val selectedPrimaryNavItemId = _selectedPrimaryNavItemId.asStateFlow()

    fun onPrimaryNavItemClick(navItemId: Int) {
        _selectedPrimaryNavItemId.value = navItemId
    }
}

/**
 * An activity that, when linked up with a navigation bar instance, will
 * automatically create and manage fragment instances for each navigation bar
 * menu item.
 *
 * BottomNavViewActivity will automatically add a fragment instance for each
 * menu item of the navigation view menu that it is connected to. These
 * automatically generated fragments are called primary fragments. These
 * primary fragments are intended to be used for fragments that need to be
 * switched between often, and for which repeated destruction and recreation is
 * undesirable. To facilitate this, the primary fragments are never replaced,
 * but instead will have their views' visibilities set to View.GONE when not in
 * use. Because this increases memory consumption, it is not recommended to add
 * more than a few commonly used fragments. If other, less frequently used
 * fragments need to be used temporarily, this can be accomplished using the
 * function addSecondaryFragment.
 *
 * In order for the primary fragment auto-generation to succeed, the property
 * navigationView must be initialized in a subclass before BottomNavViewActivity's
 * onCreate is called, the property fragmentContainerId must be set to the id
 * of the container that the fragments will be added to, and the resource
 * pointed to by the id R.array.bottom_nav_view_activity_fragments must be a
 * string array that contains the names of the primary fragments, including
 * package name. This being the case, BottomNavViewActivity will attempt to
 * associate each primary fragment in the order they are listed with a
 * navigation bar menu item, skipping disabled menu items. If the navigation
 * menu does not have at least as many enabled menu items as the number of
 * primary fragments, an indexOutOfBoundsException will be thrown. If primary
 * fragment auto-generation succeeds, BottomNavViewActivity will set itself as
 * the OnItemSelectedListener for the navigation bar, and will automatically
 * switch to the corresponding fragment with an animation.
 *
 * BottomNavViewActivity uses its own slide left or right animations for its
 * primary fragments, although the duration and the interpolators used for
 * these animations can be set through the property primaryFragmentTransitionAnimatorConfig.
 * The animations used for the addition or popping of secondary fragments can
 * either be passed in directly to a call of addSecondaryFragment, or will
 * default to the value of the properties secondaryFragmentDefaultEnterAnimResId
 * and secondaryFragmentDefaultExitAnimResId.
 *
 * The property visibleFragment will always be equal to the topmost fragment,
 * including secondary fragments. When a new primary or secondary fragment
 * becomes visible to the user, the open function onNewFragmentSelected will be
 * called. To listen to changes in the active fragment, obtain an instance of
 * ReadOnlyNavigationManager scoped to the instance of BottomNavViewActivity
 * being used and collect the StateFlow property activeFragment.
 */
abstract class BottomNavViewActivity : AppCompatActivity() {
    private val viewModel: BottomNavViewActivityViewModel by viewModels()
    protected var fragmentContainerId = 0
    protected lateinit var navigationView: BottomNavigationView
    private val navBarMenuItemFragmentMap = mutableMapOf<Int, Fragment>()

    val visibleFragment get() =
        if (showingPrimaryFragment) selectedPrimaryFragment
        else supportFragmentManager.run {
            findFragmentByTag(getBackStackEntryAt(backStackEntryCount - 1).name)
        }

    val showingPrimaryFragment get() =
        supportFragmentManager.backStackEntryCount == 0
    val selectedPrimaryFragment get() =
        navBarMenuItemFragmentMap.getValue(navigationView.selectedItemId)

    var primaryFragmentTransitionAnimatorConfig: AnimatorConfig? = null
    protected var defaultSecondaryFragmentEnterAnimResId: Int = 0
    protected var defaultSecondaryFragmentExitAnimResId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createOrRestoreFragments(savedInstanceState)
        supportFragmentManager.addOnBackStackChangedListener {
            viewModel.onActiveFragmentChanged(visibleFragment)
            // The hidden primary fragments seem to have their
            // visibility reset at this point for some reason.
            if (!showingPrimaryFragment)
                for (fragment in navBarMenuItemFragmentMap.values)
                    fragment.view?.isVisible = false
        }
        recollectWhenStarted(viewModel.selectedPrimaryNavItemId) {
            onNavBarItemSelected(it)
        }
    }

    // See the documentation for checkQueuedMenuItemPress for an
    // explanation of queuedMenuItem and menuItemLastPressTimestamp
    private var queuedNavItemPress: Int? = null
    private var navItemLastPressTimestamp = 0L
    private var primaryTransitionInProgress = false
    /** Attempt to switch to a new primary fragment corresponding to the @param
     * navItemId, and @return whether or not the switch was successful. */
    private fun onNavBarItemSelected(navItemId: Int) {
        if (navItemId == navigationView.selectedItemId)
            return

        if (primaryTransitionInProgress) {
            queuedNavItemPress = navItemId
            navItemLastPressTimestamp = System.currentTimeMillis()
            return
        }

        val newFragment = navBarMenuItemFragmentMap[navItemId] ?: return
        val newMenuItem = navigationView.menu.findItem(navItemId)
        val oldFragmentMenuItem = navigationView.menu.findItem(navigationView.selectedItemId)
        val oldFragment = navBarMenuItemFragmentMap.getValue(oldFragmentMenuItem.itemId)
        navigationView.menu.findItem(navItemId)?.isChecked = true

        if (!showingPrimaryFragment) {
            // If there is a secondary fragment on top of the primary fragments,
            // then we can skip the animations since they won't be seen anyways.
            oldFragment.view?.isVisible = false
            newFragment.view?.isVisible = true
            return
        }

        val leftToRight = oldFragmentMenuItem.order < newMenuItem.order
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
            translationX =  width * if (leftToRight) 1f else -1f
            isVisible = true
            animate().translationX(0f).withLayer()
                .applyConfig(primaryFragmentTransitionAnimatorConfig)
                .start()
        }
    }

    /** To prevent visual bugs due to new animations starting before the old
     * ones are finished while still allowing the UI to feel responsive,
     * BottomNavViewActivity queues navigation menu item presses that occur
     * during the last half of the transition animation, and plays them when
     * the transition animation is finished. */
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
        if (showingPrimaryFragment) {
            transaction.hide(selectedPrimaryFragment)
            transaction.add(fragmentContainerId, fragment, tag)
        }
        else transaction.replace(fragmentContainerId, fragment, tag)
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
        navigationView.selectedItemId = viewModel.selectedPrimaryNavItemId.value
        navBarMenuItemFragmentMap.forEach { menuItemIdAndFragment ->
            val menuItemId = menuItemIdAndFragment.key
            val fragment = menuItemIdAndFragment.value
            if (menuItemId != navigationView.selectedItemId || !showingPrimaryFragment)
                // Even though the inactive fragments' views' visibilities are later
                // set to GONE, setting them to INVISIBLE this first time ensures
                // that the transition animation plays correctly the first time.
                fragment.view?.visibility = View.INVISIBLE
        }
        navigationView.setOnItemSelectedListener{
            viewModel.onPrimaryNavItemClick(it.itemId)
            true
        }
    }
}