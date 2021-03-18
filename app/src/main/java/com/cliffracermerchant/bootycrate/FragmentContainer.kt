/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.doOnNextLayout
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

/**
 * A container for fragments that, when linked up with a navigation menu
 * instance, will automatically create and manage fragment instances for
 * each navigation bar menu item.
 *
 * FragmentContainer acts as a container for fragments that will automatic-
 * ally add a fragment instance for each menu item of a navigation bar
 * menu that it is connected to. The resource id of the navigation bar
 * must be set in XML using the attribute R.attr.navBarResId. Likewise,
 * the attribute R.attr.navBarMenuFragmentMapResId must reference a string
 * array that contains the names of the fragments (including package name)
 * to be associated with each corresponding menu item, or an empty string
 * if a menu item should not be associated with a fragment. After set up,
 * FragmentContainer will set itself as the OnNavigationItemSelectedListe-
 * ner for the navigation bar, and will automatically switch to the corre-
 * sponding fragment with an animation.
 *
 * FragmentContainer is intended to be used primarily with fragments that
 * need to be switched between often. To speed up this operation, the frag-
 * ments will have their views' visibilities set to View.INVISIBLE when
 * not in use. Because this increases memory consumption, it is not reco-
 * mmended to add more than a few commonly used fragments. If other, less
 * frequently used fragments need to be used temporarily, this can be
 * accomplished using the function addSecondaryFragment. The function pop-
 * BackStack will function similarly to FragmentManager popBackStack, but
 * will only pop secondary fragments and not primary fragments (the ones
 * added automatically that correspond to the navigation bar menu items).
 *
 * The menu item id of the currently selected primary fragment, or null
 * if a secondary fragment is displayed, can be queried using the property
 * visibleFragmentMenuItemId. The property visibleFragment will always
 * be equal to the topmost fragment, including secondary fragments. When a
 * new primary or secondary fragment becomes visible to the user, the call-
 * back onNewFragmentSelectedListener will be invoked.
 *
 * FragmentContainer uses its own slide left or right animations for its
 * primary fragments, although the duration and the interpolators used for
 * these animations can be set through the property primaryFragmentTransi-
 * tionAnimatorConfig. The animations used for the addition or popping of
 * secondary fragments can either be passed in directly to a call of add-
 * SecondaryFragment, or will default to the value of the properties
 * secondaryFragmentDefaultEnterAnimResId and secondaryFragmentDefaultExit-
 * AnimResId.
 * */
class FragmentContainer(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private val backStackFragments = Stack<Fragment>()
    private val navBarMenuItemFragmentMap = mutableMapOf<Int, Fragment>()
    private lateinit var navBar: BottomNavigationView
    private var _visibleFragmentMenuItem: MenuItem? = null

    val visibleFragmentMenuItemId get() = if (!backStackFragments.isEmpty()) null
                                          else navBar.selectedItemId
    val visibleFragment get() = if (backStackFragments.isEmpty())
                                    navBarMenuItemFragmentMap[_visibleFragmentMenuItem?.itemId]
                                else backStackFragments.peek()
    var onNewFragmentSelectedListener: ((Fragment) -> Unit)? = null

    var primaryFragmentTransitionAnimatorConfig: AnimatorConfig? = null
    var secondaryFragmentDefaultEnterAnimResId = 0
    var secondaryFragmentDefaultExitAnimResId = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FragmentContainer)
        val navBarResId = a.getResourceIdOrThrow(R.styleable.FragmentContainer_navBarResId)
        val navBarMenuFragmentMapResId = a.getResourceIdOrThrow(
            R.styleable.FragmentContainer_navBarMenuFragmentMapResId)
        secondaryFragmentDefaultEnterAnimResId = a.getResourceId(
            R.styleable.FragmentContainer_secondaryFragmentDefaultEnterAnimResId, 0)
        secondaryFragmentDefaultExitAnimResId = a.getResourceId(
            R.styleable.FragmentContainer_secondaryFragmentDefaultExitAnimResId, 0)
        a.recycle()
        doOnNextLayout { initFragments(navBarResId, navBarMenuFragmentMapResId) }
    }

    private fun initFragments(navBarResId: Int, fragmentMapResId: Int) {
        navBar = (parent as ViewGroup).findViewById(navBarResId)
        val fragmentNames = resources.getStringArray(fragmentMapResId)
        val fragmentManager = fragmentActivityFrom(context).supportFragmentManager
        try { navBar.menu.forEachIndexed { i, menuItem ->
            if (fragmentNames[i].isNotEmpty()) {
                val fragment = fragmentManager.fragmentFactory.instantiate(
                    ClassLoader.getSystemClassLoader(), fragmentNames[i])
                navBarMenuItemFragmentMap[menuItem.itemId] = fragment
            }
        }}
        catch (e: IndexOutOfBoundsException) { throw IndexOutOfBoundsException(
            "The string array pointed to by R.attr.navBarMenuFragmentMap must be the same length " +
            "as the navigation menu of the navigation bar pointed to by R.attr.navBarResId.")
        }
        fragmentManager.beginTransaction()
            .runOnCommit {
                navBar.menu.forEach { menuItem ->
                    if (navBar.selectedItemId != menuItem.itemId)
                        navBarMenuItemFragmentMap[menuItem.itemId]?.view?.visibility = View.INVISIBLE
                }
            }.apply {
                for (idAndFragment in navBarMenuItemFragmentMap)
                    add(id, idAndFragment.value)
            }.commit()

        val selectedMenuItem = navBar.menu.findItem(navBar.selectedItemId)
        switchToNewFragment(selectedMenuItem)
        navBar.setOnNavigationItemSelectedListener(::switchToNewFragment)
    }

    /** Attempt to switch to a new active fragment corresponding to the @param menuItem,
     * and @return whether or not the switch was successful. */
    private fun switchToNewFragment(menuItem: MenuItem): Boolean {
        if (_visibleFragmentMenuItem == menuItem) return false
        if (!navBarMenuItemFragmentMap.containsKey(menuItem.itemId)) return false

        val oldFragmentMenuItem = _visibleFragmentMenuItem
        _visibleFragmentMenuItem = menuItem
        menuItem.isChecked = true
        val newFragment = navBarMenuItemFragmentMap.getValue(menuItem.itemId)
        onNewFragmentSelectedListener?.invoke(newFragment)
        // If there was no old fragment, then the container is being initialized and no animation is necessary.
        if (oldFragmentMenuItem == null) return true

        val leftToRight = oldFragmentMenuItem.order < menuItem.order
        navBarMenuItemFragmentMap.getValue(oldFragmentMenuItem.itemId).view?.apply {
            val animResId = if (leftToRight) R.animator.slide_out_left
                            else             R.animator.slide_out_right
            val anim = AnimatorInflater.loadAnimator(context, animResId)
            anim.setTarget(this)
            ((anim as AnimatorSet).childAnimations[0] as ObjectAnimator)
                .setFloatValues(0f, width / 2f * if (leftToRight) -1f else 1f)
            anim.doOnEnd { visibility = View.INVISIBLE}
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

    fun addSecondaryFragment(
        fragment: Fragment,
        customEnterAnimResId: Int? = null,
        customExitAnimResId: Int? = null
    ) {
        fragment.view?.alpha = 0f
        val enterAnimResId = customEnterAnimResId ?: secondaryFragmentDefaultEnterAnimResId
        val exitAnimResId = customExitAnimResId ?: secondaryFragmentDefaultExitAnimResId
        fragmentActivityFrom(context).supportFragmentManager.beginTransaction()
            .setCustomAnimations(enterAnimResId, exitAnimResId, enterAnimResId, exitAnimResId)
            .apply {
                visibleFragment?.let { hide(it) }
                backStackFragments.add(fragment)
            }.add(id, fragment)
            .addToBackStack(null).commit()
        onNewFragmentSelectedListener?.invoke(fragment)

    }

    fun popBackStack() {
        if (backStackFragments.isEmpty()) return
        val fragmentManager = fragmentActivityFrom(context).supportFragmentManager
        fragmentManager.popBackStack()
        backStackFragments.pop()
        visibleFragment?.let { onNewFragmentSelectedListener?.invoke(it) }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()
    }
}