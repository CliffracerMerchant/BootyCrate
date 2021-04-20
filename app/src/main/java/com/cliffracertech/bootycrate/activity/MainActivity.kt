/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.fragment.PreferencesFragment
import com.cliffracertech.bootycrate.utils.*

/**
 * A MultiFragmentActivity with a fragment interface that enables implementing fragments to use its custom UI.
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
        initGradients()
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
        showBottomAppBar(show = newFragment.showsBottomAppBar(), animate = needToAnimate)
        val showsCheckoutButton = newFragment.showsCheckoutButton()
        if (showsCheckoutButton != null)
            showCheckoutButton(show = showsCheckoutButton, animate = needToAnimateCheckoutButton)
        ui.bottomAppBar.moveIndicatorToNavBarItem(navigationBar.selectedItemId)
        newFragment.onActiveStateChanged(isActive = true, ui)
    }

    private fun showBottomAppBar(show: Boolean = true, animate: Boolean = true) {
        if (ui.bottomAppBar.isVisible == show) return
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()

        if (!animate) ui.bottomAppBar.isVisible = show
        else {
            val translationAmount = screenHeight - ui.cradleLayout.top - ui.bottomAppBar.top
            val translationStart = if (show) translationAmount else 0f
            val translationEnd = if (show) 0f else translationAmount
            ui.bottomAppBar.translationY = translationStart
            ui.bottomAppBar.isVisible = true
            ui.bottomAppBar.animate().translationY(translationEnd)
                .applyConfig(primaryFragmentTransitionAnimatorConfig)
                .withEndAction { if (!show) ui.bottomAppBar.isVisible = false }.start()
        }
    }

    private var showingCheckoutButton: Boolean? = null
    private var pendingCradleAnim: Animator? = null
    private fun showCheckoutButton(show: Boolean, animate: Boolean = true) {
        if (showingCheckoutButton == show) return
        showingCheckoutButton = show
        ui.checkoutButton.isVisible = show

        val cradleEndWidth = if (!show) ui.addButton.layoutParams.width else {
            val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            ui.cradleLayout.measure(wrapContent, wrapContent)
            ui.cradleLayout.measuredWidth
        }
        if (!animate) ui.bottomAppBar.cradleWidth = cradleEndWidth
        else {
            // Settings the checkout button's clip bounds prevents the right corners of the check-
            // out button from sticking out underneath the FAB during the show / hide animation.
            val clipBounds = Rect(0, 0, 0, ui.checkoutButton.height)
            ValueAnimator.ofInt(ui.bottomAppBar.cradleWidth, cradleEndWidth).apply {
                applyConfig(primaryFragmentTransitionAnimatorConfig)
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
    }

    private fun setThemeFromPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prefKey = getString(R.string.pref_light_dark_mode)
        val themeDefault = getString(R.string.sys_default_theme_description)
        val sysDarkThemeIsActive = Configuration.UI_MODE_NIGHT_YES ==
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        setTheme(when (prefs.getString(prefKey, themeDefault) ?: "") {
            getString(R.string.light_theme_description) -> R.style.LightTheme
            getString(R.string.dark_theme_description) ->  R.style.DarkTheme
            else -> if (sysDarkThemeIsActive) R.style.DarkTheme
                    else                      R.style.LightTheme
        })
    }

    private fun setupOnClickListeners() {
        ui.actionBar.ui.backButton.setOnClickListener {
            val fragment = visibleFragment as? MainActivityFragment
            if (fragment?.onBackPressed() == false)
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
            resources.getInteger(R.integer.fragmentTransitionLongDuration).toLong(),
            AnimationUtils.loadInterpolator(this, R.anim.default_interpolator))
        primaryFragmentTransitionAnimatorConfig = transitionAnimConfig
        defaultSecondaryFragmentEnterAnimResId = R.animator.fragment_close_enter
        defaultSecondaryFragmentExitAnimResId = R.animator.fragment_close_exit
        ui.actionBar.animatorConfig = transitionAnimConfig
        ui.bottomAppBar.indicatorWidth = 3 * ui.bottomNavigationBar.itemIconSize
        ui.bottomAppBar.indicatorAnimatorConfig = transitionAnimConfig
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
     * inform the MainActivity how its ui should be displayed when the frag-
     * ment is in the foreground, and to provide a callback for back button
     * (either the hardware/software bottom back button or the action bar's
     * back button) presses. Fragments should always indicate their desired
     * bottom app bar and checkout button states through an override of the
     * appropriate function instead of changing their visibility manually to
     * ensure that the appropriate hide/show animations are played.
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
        fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {}
    }

    /**
     * Unfortunately many of the desired aspects of MainActivity's style (e.g. the
     * checkout and add buttons background gradient matching the bottom app bar's
     * background gradient) are impossible to accomplish in XML. initGradients
     * performs additional operations to initialize its style. The background
     * gradient used for the bottom app bar is made by creating a linear gradient
     * using the values of the XML attributes backgroundGradientColorLeft,
     * backgroundGradientColorMiddle, and backgroundGradientColorRight. It is
     * assumed that the action bar will have had its background set correctly in
     * XML to match the background gradient.
     */
    private fun initGradients() {
        window.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_gradient))

        val screenWidth = resources.displayMetrics.widthPixels
        val actionBarHeight = theme.resolveIntAttribute(R.attr.actionBarSize)

        val fgColor = theme.resolveIntAttribute(R.attr.topBottomBarForegroundColor)
        val bgColors = intArrayOf(theme.resolveIntAttribute(R.attr.backgroundGradientColorLeft),
                                  theme.resolveIntAttribute(R.attr.backgroundGradientColorMiddle),
                                  theme.resolveIntAttribute(R.attr.backgroundGradientColorRight))

        val gradientBuilder = GradientBuilder(x2 = screenWidth.toFloat(), colors = bgColors)
        val gradientShader = gradientBuilder.buildLinearGradient()

        val gradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(gradientBitmap)
        val paint = Paint().apply { shader = gradientShader }
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight.toFloat(), paint)

        ui.bottomAppBar.backgroundGradient = gradientShader
        ui.bottomAppBar.indicatorColor = fgColor

        // Checkout button
        val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ui.cradleLayout.measure(wrapContent, wrapContent)
        val cradleWidth = ui.cradleLayout.measuredWidth
        val cradleLeft = (screenWidth - cradleWidth) / 2f
        ui.checkoutButton.setTextColor(fgColor)
        ui.checkoutButton.backgroundGradient = gradientBuilder
            .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()

        // Add button
        val addButtonWidth = ui.addButton.layoutParams.width
        val addButtonLeft = cradleLeft + cradleWidth - addButtonWidth * 1f
        ui.addButton.imageTintList = ColorStateList.valueOf(fgColor)
        // Setting the background for the add button in XML doesn't work for some reason?
        ui.addButton.background = ContextCompat.getDrawable(this, R.drawable.add_button)
        (ui.addButton.background as? GradientDrawable)?.colors = intArrayOf(
            gradientBitmap.getPixel(addButtonLeft.toInt(), 0),
            gradientBitmap.getPixel(addButtonLeft.toInt() + addButtonWidth, 0))
    }
}
