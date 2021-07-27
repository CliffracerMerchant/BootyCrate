/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.animation.Animator
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.fragment.PreferencesFragment
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.doOnStart
import com.cliffracertech.bootycrate.utils.layoutTransition

/**
 * A MultiFragmentActivity with a fragment interface that enables implementing fragments to use its custom UI.
 *
 * MainActivity is a MultiFragmentActivity subclass with a custom UI including
 * a RecyclerViewActionBar, a BottomAppBar, and a checkout button and an add
 * button in the cradle of the BottomAppBar. In order for fragments to inform
 * MainActivity which of these UI elements should be displayed when they are
 * active, the fragment should implement MainActivity.FragmentInterface. While
 * it is not necessary for fragments to implement FragmentInterface, fragments
 * that do not will not be able to affect the visibility of the MainActivity
 * UI when they are displayed.
 */
@Suppress("LeakingThis")
open class MainActivity : MultiFragmentActivity() {
    lateinit var ui: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setThemeFromPreferences()
        ui = MainActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(ui.root)
        fragmentContainerId = ui.fragmentContainer.id
        navigationBar = ui.bottomNavigationBar
        super.onCreate(savedInstanceState)
        setupOnClickListeners()
        initAnimatorConfigs()
        window.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_gradient))
        initGradientStyle()
    }

    override fun onBackPressed() { ui.actionBar.ui.backButton.performClick() }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.settings_menu_item) {
            addSecondaryFragment(PreferencesFragment())
            true
        } else visibleFragment?.onOptionsItemSelected(item) ?: false

    private var currentFragment: Fragment? = null
    override fun onNewFragmentSelected(newFragment: Fragment) {
        currentFragment?.let {
            if (it is MainActivityFragment)
                it.onActiveStateChanged(isActive = false, ui)
        }
        val needToAnimate = currentFragment != null
        currentFragment = newFragment
        if (newFragment !is MainActivityFragment) return

        val needToAnimateCheckoutButton = needToAnimate && ui.bottomAppBar.isVisible
        ui.showBottomAppBar(newFragment.showsBottomAppBar(), needToAnimate,
                            primaryFragmentTransitionAnimatorConfig)
        val showsCheckoutButton = newFragment.showsCheckoutButton()
        if (showsCheckoutButton != null)
            showCheckoutButton(showsCheckoutButton, needToAnimateCheckoutButton)
        ui.bottomAppBar.moveIndicatorToNavBarItem(navigationBar.selectedItemId)
        newFragment.onActiveStateChanged(isActive = true, ui)
    }

    private var showingCheckoutButton: Boolean? = null
    // The cradle animation is stored here and started in the cradle
    // layout's layoutTransition's transition listener's transitionStart
    // override so that the animation is synced with the layout transition.
    private var pendingCradleAnim: Animator? = null
    private fun showCheckoutButton(show: Boolean, animate: Boolean = true) {
        if (showingCheckoutButton == show) return
        showingCheckoutButton = show
        if (!animate) ui.showCheckoutButton(show)
        else pendingCradleAnim =
            ui.showCheckoutButtonAnimation(show, primaryFragmentTransitionAnimatorConfig)
    }

    private fun setThemeFromPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var prefKey = getString(R.string.pref_view_models_need_cleared)
        val viewModelsNeedCleared = prefs.getBoolean(prefKey, false)
        if (viewModelsNeedCleared) viewModelStore.clear()

        prefKey = getString(R.string.pref_light_dark_mode_key)
        val themeDefault = getString(R.string.pref_theme_sys_default_title)
        val sysDarkThemeIsActive = Configuration.UI_MODE_NIGHT_YES ==
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        setTheme(when (prefs.getString(prefKey, themeDefault) ?: "") {
            getString(R.string.pref_theme_light_theme_title) -> R.style.LightTheme
            getString(R.string.pref_theme_dark_theme_title) ->  R.style.DarkTheme
            else -> if (sysDarkThemeIsActive) R.style.DarkTheme
                    else                      R.style.LightTheme
        })
    }

    private fun setupOnClickListeners() {
        ui.actionBar.ui.backButton.setOnClickListener {
            val fragment = visibleFragment as? MainActivityFragment
            if (fragment?.onBackPressed() != true)
                supportFragmentManager.popBackStack()
        }
        ui.actionBar.onDeleteButtonClickedListener = {
            onOptionsItemSelected(ui.actionBar.optionsMenu.findItem(R.id.delete_selected_menu_item))
        }
        ui.actionBar.setOnSortOptionClickedListener { item -> onOptionsItemSelected(item) }
        ui.actionBar.setOnOptionsItemClickedListener { item -> onOptionsItemSelected(item) }
    }

    private fun initAnimatorConfigs() {
        val transitionAnimConfig = AnimatorConfig(
            resources.getInteger(R.integer.primaryFragmentTransitionDuration).toLong(),
            AnimationUtils.loadInterpolator(this, R.anim.default_interpolator))
        primaryFragmentTransitionAnimatorConfig = transitionAnimConfig
        defaultSecondaryFragmentEnterAnimResId = R.animator.fragment_close_enter
        defaultSecondaryFragmentExitAnimResId = R.animator.fragment_close_exit
        ui.actionBar.animatorConfig = transitionAnimConfig
        ui.bottomAppBar.indicatorWidth = 3 * ui.bottomNavigationBar.itemIconSize
        ui.bottomAppBar.indicatorAnimatorConfig = transitionAnimConfig
            // This interpolator shows the indicator going around the bottom app bar cradle slightly better.
            .copy(interpolator = PathInterpolator(.4f,.6f,.6f,.4f))
        ui.checkoutButton.animatorConfig = transitionAnimConfig
        ui.cradleLayout.layoutTransition = layoutTransition(transitionAnimConfig)
        ui.cradleLayout.layoutTransition.doOnStart {
            pendingCradleAnim?.start()
            pendingCradleAnim = null
        }
    }

    /**
     * An interface that informs MainActivity how its Fragment implementor affects the main activity ui.
     *
     * MainActivityFragment can be implemented by a Fragment subclass to
     * inform the MainActivity how its UI should be displayed when the frag-
     * ment is in the foreground, and to provide a callback for back button
     * (either the hardware/software bottom back button or the action bar's
     * back button) presses. Fragments should always indicate their desired
     * bottom app bar and checkout button states through an override of the
     * appropriate function instead of changing their visibility manually to
     * ensure that the appropriate hide/show animations are played. The
     * function addSecondaryFragment allows implementing fragments to add a
     * secondary fragment (see MultiFragmentActivity documentation for an
     * explanation of primary/secondary fragments) themselves.
     */
    interface MainActivityFragment {
        /** Return whether the bottom app bar should be visible when the implementing fragment is.*/
        fun showsBottomAppBar() = true
        /** Return whether the checkout button should be visible when the implementing fragment
         * is, or null if the implementing fragment's showsBottomAppBar returns false.*/
        fun showsCheckoutButton(): Boolean? = null
        /** Return whether the implementing fragment consumed the back button press. */
        fun onBackPressed() = false
        /** Perform any additional actions on the @param activityUi that the fragment desires,
         * given its @param isActive state. */
        fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) { }

        fun addSecondaryFragment(fragment: Fragment) {
            val mainActivityFragment = this as? Fragment ?:
                throw IllegalStateException("Implementors of MainActivityFragment must inherit from androidx.fragment.app.Fragment")
            val mainActivity = mainActivityFragment.activity as? MainActivity ?:
                throw IllegalStateException("Implementors of MainActivityFragment must be hosted inside a MainActivity instance.")
            mainActivity.addSecondaryFragment(fragment)
        }
    }
}
