/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.cliffracermerchant.bootycrate.databinding.MainActivityBinding
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext

/**
 * The primary activity for BootyCrate
 *
 * Instead of switching between activities, nearly everything in BootyCrate is
 * accomplished in the ShoppingListFragment, InventoryFragment, or the Preferences-
 * Fragment. Instances of ShoppingListFragment and InventoryFragment are created
 * on app startup, and hidden/shown by the fragment manager as appropriate. The
 * currently shown fragment can be determined via the boolean members showing-
 * Inventory and showingPreferences as follows:
 * Shown fragment = if (showingPreferences)    PreferencesFragment
 *                  else if (showingInventory) InventoryFragment
 *                  else                       ShoppingListFragment
 * If showingPreferences is true, the value of showingInventory determines the
 * fragment "under" the preferences (i.e. the one that will be returned to on a
 * back button press or a navigate up).
 */
@Suppress("LeakingThis")
@AndroidEntryPoint
open class MainActivity : AppCompatActivity() {
    private var shoppingListSize = -1
    private var shoppingListNumNewItems = 0
    private var pendingCradleAnim: Animator? = null

    val shoppingListViewModel: ShoppingListViewModel by viewModels()
    val inventoryViewModel: InventoryViewModel by viewModels()
    lateinit var ui: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getDefaultSharedPreferences(this)
     /* The activity's ViewModelStore will by default retain instances of the
        app's view models across activity restarts. In case this is not desired
        (e.g. when the database was replaced with an external one, and the view-
        models therefore need to be reset), setting the shared preference whose
        key is equal to the value of R.string.pref_viewmodels_need_cleared to
        true will cause MainActivity to call viewModelStore.clear() */
        var prefKey = getString(R.string.pref_view_models_need_cleared)
        if (prefs.getBoolean(prefKey, false)) {
            viewModelStore.clear()
            val editor = prefs.edit()
            editor.putBoolean(prefKey, false)
            editor.apply()
        }

        prefKey = getString(R.string.pref_light_dark_mode)
        val themeDefault = getString(R.string.sys_default_theme_description)
        setTheme(when (prefs.getString(prefKey, themeDefault) ?: "") {
            getString(R.string.light_theme_description) -> R.style.LightTheme
            getString(R.string.dark_theme_description) ->  R.style.DarkTheme
            else -> if (sysDarkThemeIsActive) R.style.DarkTheme
                    else                      R.style.LightTheme
        })
        ui = MainActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(ui.root)

        ui.topActionBar.ui.backButton.setOnClickListener {
            val visibleFragment = ui.fragmentContainer.visibleFragment as? FragmentInterface
            if (visibleFragment?.onBackPressed() == false)
                ui.fragmentContainer.popBackStack()
        }
        ui.topActionBar.onDeleteButtonClickedListener = {
            onOptionsItemSelected(ui.topActionBar.optionsMenu.findItem(R.id.delete_selected_menu_item))
        }
        ui.topActionBar.setOnSortOptionClickedListener { item -> onOptionsItemSelected(item) }
        ui.topActionBar.setOnOptionsItemClickedListener { item -> onOptionsItemSelected(item) }
        ui.fragmentContainer.onNewFragmentSelectedListener = ::updateUiForNewFragment

        ui.bottomAppBar.indicatorAnimatorConfig = AnimatorConfig.transition
        ui.bottomAppBar.indicatorWidth = 3 * ui.bottomNavigationBar.itemIconSize
        ui.bottomAppBar.doOnNextLayout {
            ui.bottomAppBar.moveIndicatorToNavBarItem(
                ui.bottomNavigationBar.selectedItemId, animate = false)
        }

        ui.cradleLayout.layoutTransition = layoutTransition(AnimatorConfig.transition)
        ui.cradleLayout.layoutTransition.doOnStart {
            pendingCradleAnim?.start()
            pendingCradleAnim = null
        }

        shoppingListViewModel.items.observe(this) { newList -> updateShoppingListBadge(newList) }
    }

    override fun onBackPressed() { ui.topActionBar.ui.backButton.performClick() }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.settings_menu_item) {
            ui.fragmentContainer.addSecondaryFragment(PreferencesFragment())
            true
        }
        else ui.fragmentContainer.visibleFragment?.onOptionsItemSelected(item) ?: false

    private var currentFragment: Fragment? = null
    private fun updateUiForNewFragment(newFragment: Fragment) {
        currentFragment?.let {
            if (it is FragmentInterface)
                it.onActiveStateChanged(isActive = false, ui)
        }
        currentFragment = newFragment

        if (newFragment !is FragmentInterface) return
        newFragment.onActiveStateChanged(isActive = true, ui = ui)
        ui.topActionBar.optionsMenuVisible = newFragment.showsOptionsMenu()
        showBottomAppBar(show = newFragment.showsBottomAppBar())
        if (newFragment.showsBottomAppBar())
            showCheckoutButton(showing = newFragment.showsCheckoutButton())
        inputMethodManager(this)?.hideSoftInputFromWindow(ui.bottomAppBar.windowToken, 0)

        val newMenuItemId = ui.fragmentContainer.visibleFragmentMenuItemId
        if (newFragment.showsBottomAppBar() && newMenuItemId != null)
            ui.bottomAppBar.moveIndicatorToNavBarItem(newMenuItemId)
    }

    private var showingBottomAppBar = true
    private fun showBottomAppBar(show: Boolean = true) {
        if (showingBottomAppBar == show) return
        showingBottomAppBar = show
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val views = arrayOf(ui.bottomAppBar, ui.addButton, ui.checkoutButton)

        if (!show && ui.bottomAppBar.height == 0) {
            ui.bottomAppBar.doOnNextLayout {
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
            view.animate().withLayer()
                .applyConfig(AnimatorConfig.transition)
                .translationY(translationEnd).start()
        }
    }

    private fun showCheckoutButton(showing: Boolean, animate: Boolean = true) {
        if (ui.checkoutButton.isVisible == showing) return
        ui.checkoutButton.isVisible = showing

        val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ui.cradleLayout.measure(wrapContentSpec, wrapContentSpec)
        val cradleEndWidth = if (showing) ui.cradleLayout.measuredWidth
                             else         ui.addButton.layoutParams.width

        // These z values seem not to stick when set in XML, so we have to
        // set them here every time to ensure that the addButton remains on
        // top of the others.
        ui.addButton.elevation = 5f
        ui.checkoutButton.elevation = -10f
        if (!animate) {
            ui.bottomAppBar.cradleWidth = cradleEndWidth
            return
        }
        // Settings the checkout button's clip bounds prevents the
        // right corners of the checkout button from sticking out
        // underneath the FAB during the show / hide animation.
        val clipBounds = Rect(0, 0, 0, ui.checkoutButton.height)
        ValueAnimator.ofInt(ui.bottomAppBar.cradleWidth, cradleEndWidth).apply {
            applyConfig(AnimatorConfig.transition)
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

    private fun updateShoppingListBadge(newShoppingList: List<ShoppingListItem>) {
        if (shoppingListSize == -1)
            if (newShoppingList.isNotEmpty())
                shoppingListSize = newShoppingList.size
        else {
            val sizeChange = newShoppingList.size - shoppingListSize
            if (ui.fragmentContainer.visibleFragment is InventoryFragment && sizeChange > 0) {
                shoppingListNumNewItems += sizeChange
                ui.shoppingListBadge.text = getString(R.string.shopping_list_badge_text,
                                                   shoppingListNumNewItems)
                ui.shoppingListBadge.clearAnimation()
                ui.shoppingListBadge.alpha = 1f
                ui.shoppingListBadge.animate().alpha(0f).setDuration(1000).setStartDelay(1500).
                    withLayer().withEndAction { shoppingListNumNewItems = 0 }.start()
            }
            shoppingListSize = newShoppingList.size
        }
    }

    private val sysDarkThemeIsActive get() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES

    /**
     * An interface that informs MainActivity how its Fragment implementor affects the main activity ui.
     *
     * Fragment interface can be implemented by a Fragment subclass to inform the
     * MainActivity as to how to alter its ui to suit the fragment, and to provide
     * callbacks for certain user actions that might be forwarded to the fragment
     * (e.g. a back button or options menu item press).
     * */
    interface FragmentInterface {
        /** Return whether the top action bar's options menu (including its search view and change sort button) should be visible when the implementing fragment is. */
        fun showsOptionsMenu(): Boolean
        /** Return whether the bottom app bar should be visible when the implementing fragment is.*/
        fun showsBottomAppBar(): Boolean
        /** Return whether the checkout button should be visible when the implementing fragment is.*/
        fun showsCheckoutButton(): Boolean
        /** Return whether the implementing fragment consumed the back button press. Note that this function is also called when the top action bar's back button is pressed. */
        fun onBackPressed(): Boolean
        /** Perform any additional actions on @param ui that the fragment desires, given its @param isActive state. */
        fun onActiveStateChanged(isActive: Boolean, ui: MainActivityBinding)
    }

    @Module @InstallIn(ActivityComponent::class)
    object MainActivityBindingModule {
        @Provides fun provideMainActivityBinding(@ActivityContext context: Context) =
            (context as MainActivity).ui
    }
}
