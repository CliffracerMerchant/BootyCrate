/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class FragmentContainer(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private val navBarResId: Int
    private val navBarMenuFragmentMapResId: Int
    private val backStackFragments = Stack<Fragment>()
    private val navBarMenuItemFragmentMap = mutableMapOf<Int, Fragment>()
    private lateinit var navBar: BottomNavigationView
    private var _visibleFragmentMenuItemId = -1

    val visibleFragmentMenuItemId get() = if (!backStackFragments.isEmpty()) null
                                          else _visibleFragmentMenuItemId
    val visibleFragment get() = if (backStackFragments.isEmpty())
                                    navBarMenuItemFragmentMap[_visibleFragmentMenuItemId]
                                else backStackFragments.peek()
    var onNewFragmentSelectedListener: ((Fragment) -> Unit)? = null

    var secondaryFragmentDefaultEnterAnimResId = 0
    var secondaryFragmentDefaultExitAnimResId = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FragmentContainer)
        navBarResId = a.getResourceIdOrThrow(R.styleable.FragmentContainer_navBarResId)
        navBarMenuFragmentMapResId = a.getResourceIdOrThrow(R.styleable.FragmentContainer_navBarMenuFragmentMapResId)
        secondaryFragmentDefaultEnterAnimResId = a.getResourceId(
            R.styleable.FragmentContainer_secondaryFragmentDefaultEnterAnimResId, 0)
        secondaryFragmentDefaultExitAnimResId = a.getResourceId(
            R.styleable.FragmentContainer_secondaryFragmentDefaultExitAnimResId, 0)
        a.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        navBar = (parent as ViewGroup).findViewById(navBarResId)
        val fragmentNames = resources.getStringArray(navBarMenuFragmentMapResId)
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
                        navBarMenuItemFragmentMap.get(menuItem.itemId)?.view?.visibility = View.INVISIBLE
                }
            }.apply {
                for (idAndFragment in navBarMenuItemFragmentMap)
                    add(id, idAndFragment.value)
            }.commit()

        _visibleFragmentMenuItemId = navBar.selectedItemId
        navBar.setOnNavigationItemSelectedListener(::switchToNewFragment)
    }

    private fun switchToNewFragment(menuItem: MenuItem): Boolean {
        if (_visibleFragmentMenuItemId == menuItem.itemId) return false
        if (!navBarMenuItemFragmentMap.containsKey(menuItem.itemId)) return false

        val oldFragmentMenuItem = navBar.menu.findItem(_visibleFragmentMenuItemId)
        val leftToRight = oldFragmentMenuItem.order < menuItem.order
        val translationAmount = resources.displayMetrics.widthPixels / 2f

        navBarMenuItemFragmentMap.getValue(_visibleFragmentMenuItemId).view?.apply {
            animate().alpha(0f)
                .translationX(translationAmount * if (leftToRight) -1f else 1f)
                .applyConfig(AnimatorConfig.transition)
                .withEndAction { visibility = View.INVISIBLE }
                .start()
        }
        val newFragment = navBarMenuItemFragmentMap.getValue(menuItem.itemId)
        newFragment.view?.apply {
            alpha = 0f
            isVisible = true
            translationX = translationAmount * if (leftToRight) 1f else -1f
            animate().alpha(1f).translationX(0f).applyConfig(AnimatorConfig.transition).start()
        }

        _visibleFragmentMenuItemId = menuItem.itemId
        onNewFragmentSelectedListener?.invoke(newFragment)
        return true
    }

    @Suppress("NAME_SHADOWING")
    fun addSecondaryFragment(
        fragment: Fragment,
        enterAnimResId: Int? = null,
        exitAnimResId: Int? = null
    ) {
        fragment.view?.alpha = 0f
        val enterAnimResId = enterAnimResId ?: secondaryFragmentDefaultEnterAnimResId
        val exitAnimResId = exitAnimResId ?: secondaryFragmentDefaultExitAnimResId
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