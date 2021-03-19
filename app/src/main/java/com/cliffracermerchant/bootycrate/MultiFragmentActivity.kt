/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * An activity that, when linked up with a navigation bar instance, will
 * automatically create and manage fragment instances for each navigation
 * bar menu item.
 *
 * MultiFragmentActivity will automatically add a fragment instance for
 * each menu item of the navigation bar menu that it is connected to.
 * These automatically generated fragments are known to MultiFragmentAct-
 * ivity as primary fragments. These primary fragments are intended to be
 * used for fragments that need to be switched between often, and for
 * which repeated destruction and recreation is undesirable. To facilitate
 * this, the primary fragments are never replaced, but instead will have
 * their views' visibilities set to View.GONE when not in use. Because
 * this increases memory consumption, it is not recommended to add more
 * than a few commonly used fragments. If other, less frequently used frag-
 * ments need to be used temporarily, this can be accomplished using the
 * function addSecondaryFragment.
 *
 * In order for the primary fragment auto-generation to succeed, the prop-
 * erty navigationBar must be overridden in a subclass before MultiFrag-
 * mentActivity's onCreate is called, the property fragmentContainerId
 * must be set to the id of the container that the primary fragments will
 * be added to, and the resource pointed to by the id
 * R.array.multi_fragment_activity_fragments must be a string array that
 * contains the names of the primary fragments, including package name.
 * This being the case, MultiFragmentActivity will attempt to associate
 * each primary fragment in the order they are listed with a navigation
 * bar menu item, skipping disabled menu items. If the navigation menu
 * does not have at least as many enabled menu items as the number of pri-
 * mary fragments, an indexOutOfBoundsException will be thrown. If primary
 * fragment auto-generation succeeds, MultiFragmentActivity will set
 * itself as the OnNavigationItemSelectedListener for the navigation bar,
 * and will automatically switch to the corresponding fragment with an ani-
 * mation.
 *
 * FragmentContainer uses its own slide left or right animations for its
 * primary fragments, although the duration and the interpolators used for
 * these animations can be set through the property primaryFragmentTransi-
 * tionAnimatorConfig. The animations used for the addition or popping of
 * secondary fragments can either be passed in directly to a call of add-
 * SecondaryFragment, or will default to the value of the properties
 * secondaryFragmentDefaultEnterAnimResId and secondaryFragmentDefaultExit-
 * AnimResId.
 *
 * The property visibleFragment will always be equal to the topmost frag-
 * ment, including secondary fragments. When a new primary or secondary
 * fragment becomes visible to the user, the open function onNewFragment-
 * Selected will be called. Override onNewFragmentSelected in subclasses
 * if special behavior when the visible fragment changes is desired.
 * */
abstract class MultiFragmentActivity : AppCompatActivity() {
    protected var fragmentContainerId = 0
    protected lateinit var navigationBar: BottomNavigationView
    private val navBarMenuItemFragmentMap = mutableMapOf<Int, Fragment>()

    val visibleFragment get() =
        if (showingPrimaryFragment) selectedPrimaryFragment
        else supportFragmentManager.run { findFragmentByTag(getBackStackEntryAt(backStackEntryCount - 1).name) }

    val showingPrimaryFragment get() = supportFragmentManager.backStackEntryCount == 0
    val selectedPrimaryFragment get() = navBarMenuItemFragmentMap.getValue(navigationBar.selectedItemId)

    var primaryFragmentTransitionAnimatorConfig: AnimatorConfig? = null
    var defaultSecondaryFragmentEnterAnimResId: Int = 0
    var defaultSecondaryFragmentExitAnimResId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOrRestoreFragments(savedInstanceState)

        switchToNewPrimaryFragment(navigationBar.menu.findItem(navigationBar.selectedItemId))
        if (!showingPrimaryFragment) onNewFragmentSelected(visibleFragment!!)
        navigationBar.setOnNavigationItemSelectedListener(::switchToNewPrimaryFragment)
        supportFragmentManager.addOnBackStackChangedListener {
            onNewFragmentSelected(visibleFragment!!)
        }
    }

    /** Attempt to switch to a new active fragment corresponding to the @param
     * menuItem, and @return whether or not the switch was successful. */
    private fun switchToNewPrimaryFragment(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == navigationBar.selectedItemId) return false
        if (!navBarMenuItemFragmentMap.containsKey(menuItem.itemId)) return false
        val newFragment = navBarMenuItemFragmentMap[menuItem.itemId]

        val oldFragmentMenuItem = navigationBar.menu.findItem(navigationBar.selectedItemId)
        menuItem.isChecked = true
        onNewFragmentSelected(newFragment!!)

        val leftToRight = oldFragmentMenuItem.order < menuItem.order
        val oldFragment = navBarMenuItemFragmentMap.getValue(oldFragmentMenuItem.itemId)
        oldFragment.view?.apply {
            val animResId = if (leftToRight) R.animator.slide_out_left
                            else             R.animator.slide_out_right
            val anim = AnimatorInflater.loadAnimator(context, animResId)
            anim.setTarget(this)
            ((anim as AnimatorSet).childAnimations[0] as ObjectAnimator)
                .setFloatValues(0f, width / 2f * if (leftToRight) -1f else 1f)
            anim.doOnEnd { visibility = View.GONE }
            anim.start()
        }
        newFragment.view?.apply {
            alpha = 0f
            isVisible = true
            val animResId = if (leftToRight) R.animator.slide_in_right
                            else             R.animator.slide_in_left
            val anim = AnimatorInflater.loadAnimator(context, animResId)
            anim.setTarget(this)
            ((anim as AnimatorSet).childAnimations[0] as ObjectAnimator)
                .setFloatValues(width / 2f * if (leftToRight) 1f else -1f, 0f)
            anim.start()
        }
        return true
    }

    @Suppress("NAME_SHADOWING")
    fun addSecondaryFragment(fragment: Fragment, enterAnimResId: Int? = null, exitAnimResId: Int? = null) {
        val enterAnimResId = enterAnimResId ?: defaultSecondaryFragmentEnterAnimResId
        val exitAnimResId = exitAnimResId ?: defaultSecondaryFragmentExitAnimResId
        val tag = supportFragmentManager.backStackEntryCount.toString()
        fragment.tag
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(enterAnimResId, exitAnimResId, enterAnimResId, exitAnimResId)
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
        outState.putBoolean("wasShowingPrimaryFragment", showingPrimaryFragment)
    }

    open fun onNewFragmentSelected(newFragment: Fragment) { }

    private fun addOrRestoreFragments(savedInstanceState: Bundle?) {
        val fragmentNames = resources.getStringArray(R.array.multi_fragment_activity_fragments)
        var menuItemIndex = 0
        var assignedFragments = 0
        try { while (assignedFragments < fragmentNames.size) {
            val name = fragmentNames[assignedFragments]
            val menuItem = navigationBar.menu.getItem(menuItemIndex++)
            if (!menuItem.isEnabled) continue
            val fragment = supportFragmentManager.run {
                if (savedInstanceState == null)
                    fragmentFactory.instantiate(ClassLoader.getSystemClassLoader(), name)
                else getFragment(savedInstanceState, menuItem.itemId.toString())
                    ?: throw IllegalStateException("The saved instance state must contain a " +
                                                   "fragment for each navigation menu item id")
            }
            navBarMenuItemFragmentMap[menuItem.itemId] = fragment
            assignedFragments++
        }}
        catch (e: IndexOutOfBoundsException) { throw IndexOutOfBoundsException(
            "The navigation menu of the navigation bar must have at least as many non-empty menu" +
            "items as the number of fragments named in R.array.multi_fragment_activity_fragments.")
        }
        val transaction = supportFragmentManager.beginTransaction()
            .runOnCommit { initPrimaryFragmentVisibility(savedInstanceState) }
        if (savedInstanceState == null)
            for (idAndFragment in navBarMenuItemFragmentMap)
                transaction.add(fragmentContainerId, idAndFragment.value)
        transaction.commit()
    }

    private fun initPrimaryFragmentVisibility(savedInstanceState: Bundle?) {
        val wasShowingPrimaryFragment =
            savedInstanceState?.getBoolean("wasShowingPrimaryFragment") ?: true
        navBarMenuItemFragmentMap.forEach { menuItemIdAndFragment ->
            val menuItemId = menuItemIdAndFragment.key
            val fragment = menuItemIdAndFragment.value
            if (navigationBar.selectedItemId != menuItemId || !wasShowingPrimaryFragment)
                fragment.view?.visibility = View.INVISIBLE
        }
    }
}