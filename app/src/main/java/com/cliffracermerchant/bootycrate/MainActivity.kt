/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.cliffracermerchant.bootycrate.databinding.MainActivityBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A FragmentContainer hosting activity with a custom UI.
 *
 * MainActivity is a MultiFragmentActivity subclass with a custom UI inclu-
 * ding a RecyclerViewActionBar, a BottomAppBar, and a checkout button and
 * an add button in the cradle of the BottomAppBar. In order for fragments
 * to inform MainActivity which of these UI elements should be displayed
 * when they are active, the fragment should implement MainActivity.Frag-
 * mentInterface. While it is not necessary for fragments to implement
 * FragmentInterface, fragments that do not will not be able to affect the
 * visibility of the MainActivity UI when they are displayed.
 */
@Suppress("LeakingThis")
@AndroidEntryPoint
open class MainActivity : MultiFragmentActivity() {
    lateinit var ui: MainActivityBinding
    @Inject @TransitionAnimatorConfig lateinit var transitionAnimConfig: AnimatorConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getDefaultSharedPreferences(this)
        val prefKey = getString(R.string.pref_light_dark_mode)
        val themeDefault = getString(R.string.sys_default_theme_description)
        val sysDarkThemeIsActive = UI_MODE_NIGHT_YES == (resources.configuration.uiMode and
                                                         Configuration.UI_MODE_NIGHT_MASK)
        setTheme(when (prefs.getString(prefKey, themeDefault) ?: "") {
            getString(R.string.light_theme_description) -> R.style.LightTheme
            getString(R.string.dark_theme_description) ->  R.style.DarkTheme
            else -> if (sysDarkThemeIsActive) R.style.DarkTheme
                    else                      R.style.LightTheme
        })
        ui = MainActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(ui.root)
        fragmentContainerId = ui.fragmentContainer.id
        navigationBar = ui.bottomNavigationBar
        super.onCreate(savedInstanceState)
        setupOnClickListeners()
        initAnimatorConfigs()
    }

    override fun onBackPressed() { ui.topActionBar.ui.backButton.performClick() }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.settings_menu_item) {
            addSecondaryFragment(PreferencesFragment())
            true
        }
        else visibleFragment?.onOptionsItemSelected(item) ?: false

    private var currentFragment: Fragment? = null
    override fun onNewFragmentSelected(newFragment: Fragment) {
        currentFragment?.let {
            if (it is FragmentInterface)
                it.onActiveStateChanged(isActive = false, ui)
        }
        ui.topActionBar.ui.backButton.isVisible = !showingPrimaryFragment
        val needToAnimate = currentFragment != null
        currentFragment = newFragment

        if (newFragment !is FragmentInterface) return
        newFragment.onActiveStateChanged(isActive = true, ui = ui)
        ui.topActionBar.optionsMenuVisible = newFragment.showsOptionsMenu()
        showBottomAppBar(show = newFragment.showsBottomAppBar(), animate = needToAnimate)
        if (newFragment.showsBottomAppBar())
            showCheckoutButton(showing = newFragment.showsCheckoutButton(), animate = needToAnimate)
        inputMethodManager(this)?.hideSoftInputFromWindow(ui.bottomAppBar.windowToken, 0)

        if (newFragment.showsBottomAppBar())
            ui.bottomAppBar.moveIndicatorToNavBarItem(navigationBar.selectedItemId)
    }

    private val showingBottomAppBar get() = ui.bottomAppBar.translationY == 0f
    private fun showBottomAppBar(show: Boolean = true, animate: Boolean = true) {
        if (showingBottomAppBar == show) return
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val views = arrayOf(ui.bottomAppBar, ui.addButton, ui.checkoutButton)

        if (!animate) {
            if (!show) ui.bottomAppBar.doOnNextLayout {
                val translationAmount = screenHeight - ui.cradleLayout.top
                for (view in views) view.translationY = translationAmount
            }
            return
        }

        val translationAmount = screenHeight - ui.cradleLayout.top
        val translationStart = if (show) translationAmount else 0f
        val translationEnd = if (show) 0f else translationAmount
        for (view in views) {
            view.translationY = translationStart
            view.animate().withLayer().applyConfig(transitionAnimConfig)
                .translationY(translationEnd).start()
        }
    }

    private var pendingCradleAnim: Animator? = null
    private fun showCheckoutButton(showing: Boolean, animate: Boolean = true) {
        if (ui.checkoutButton.isVisible == showing) return
        ui.checkoutButton.isVisible = showing

        val cradleEndWidth = if (!showing) ui.addButton.layoutParams.width else {
            val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            ui.cradleLayout.measure(wrapContentSpec, wrapContentSpec)
            ui.cradleLayout.measuredWidth
        }
        // These z values seem not to stick when set in XML, so we have to set them here
        // every time to ensure that the addButton remains on top of the checkout button.
        ui.addButton.elevation = 5f
        ui.checkoutButton.elevation = -10f
        if (!animate) {
            ui.bottomAppBar.cradleWidth = cradleEndWidth
            return
        }
        // Settings the checkout button's clip bounds prevents the right corners of the check-
        // out button from sticking out underneath the FAB during the show / hide animation.
        val clipBounds = Rect(0, 0, 0, ui.checkoutButton.height)
        ValueAnimator.ofInt(ui.bottomAppBar.cradleWidth, cradleEndWidth).apply {
            applyConfig(transitionAnimConfig)
            addUpdateListener {
                ui.bottomAppBar.cradleWidth = it.animatedValue as Int
                clipBounds.right = ui.bottomAppBar.cradleWidth - ui.addButton.measuredWidth / 2
                ui.checkoutButton.clipBounds = clipBounds
            }
            doOnEnd { ui.checkoutButton.clipBounds = null }
            // The anim is stored here and started in the cradle layout's
            // layoutTransition's transition listener's transitionStart override
            // so that the animation is synced with the layout transition.
            pendingCradleAnim = this
        }
    }

    private fun setupOnClickListeners() {
        ui.topActionBar.ui.backButton.setOnClickListener {
            val fragment = visibleFragment as? FragmentInterface
            if (fragment?.onBackPressed() == false)
                supportFragmentManager.popBackStack()
        }
        ui.topActionBar.onDeleteButtonClickedListener = {
            onOptionsItemSelected(ui.topActionBar.optionsMenu.findItem(R.id.delete_selected_menu_item))
        }
        ui.topActionBar.setOnSortOptionClickedListener { item -> onOptionsItemSelected(item) }
        ui.topActionBar.setOnOptionsItemClickedListener { item -> onOptionsItemSelected(item) }
    }

    private fun initAnimatorConfigs() {
        defaultSecondaryFragmentEnterAnimResId = R.animator.fragment_close_enter
        defaultSecondaryFragmentExitAnimResId = R.animator.fragment_close_exit
        ui.bottomAppBar.indicatorWidth = 3 * ui.bottomNavigationBar.itemIconSize
        ui.cradleLayout.layoutTransition = layoutTransition(transitionAnimConfig)
        ui.cradleLayout.layoutTransition.doOnStart {
            pendingCradleAnim?.start()
            pendingCradleAnim = null
        }
    }

    /**
     * An interface that informs MainActivity how its Fragment implementor affects the main activity ui.
     *
     * Fragment interface can be implemented by a Fragment subclass to inform the
     * MainActivity as to how to alter its ui to suit the fragment, and to provide
     * callbacks for certain user actions that might be forwarded to the fragment
     * (e.g. a back button or options menu item press).
     * */
    interface FragmentInterface {
        /** Return whether the top action bar's options menu (including its search view and change
         * sort button) should be visible when the implementing fragment is. */
        fun showsOptionsMenu(): Boolean
        /** Return whether the bottom app bar should be visible when the implementing fragment is.*/
        fun showsBottomAppBar(): Boolean
        /** Return whether the checkout button should be visible when the implementing fragment is.*/
        fun showsCheckoutButton(): Boolean
        /** Return whether the implementing fragment consumed the back button press. Note that
         * this function is also called when the top action bar's back button is pressed. */
        fun onBackPressed(): Boolean
        /** Perform any additional actions on @param ui that the fragment desires, given its @param isActive state. */
        fun onActiveStateChanged(isActive: Boolean, ui: MainActivityBinding) { }
    }
}
