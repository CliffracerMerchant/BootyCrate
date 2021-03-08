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
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.cliffracermerchant.bootycrate.databinding.MainActivityBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    private lateinit var shoppingListFragment: ShoppingListFragment
    private lateinit var inventoryFragment: InventoryFragment
    private var showingInventory = false
    private var showingPreferences = false
    val activeFragment get() = if (showingInventory) inventoryFragment
                               else                  shoppingListFragment

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

        ui.cradleLayout.layoutTransition = layoutTransition(AnimatorConfig.transition)
        ui.cradleLayout.layoutTransition.doOnStart {
            pendingCradleAnim?.start()
            pendingCradleAnim = null
        }

        ui.bottomAppBar.indicatorWidth = 3 * ui.bottomNavigationBar.itemIconSize
        ui.bottomNavigationBar.setOnNavigationItemSelectedListener(onNavigationItemSelected)

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                showBottomAppBar()
                showingPreferences = false
                ui.topActionBar.ui.backButton.isVisible = false
                activeFragment.isActive = true
            }
        }
        initFragments(savedInstanceState)
        val navButton = findViewById<View>(if (showingInventory) R.id.inventory_button
                                           else                  R.id.shopping_list_button)
        navButton.doOnNextLayout {
            ui.bottomAppBar.indicatorXPos = (it.width - ui.bottomAppBar.indicatorWidth) / 2 + it.left
        }
        if (showingInventory)
            showCheckoutButton(showing = false, animate = false)
        ui.bottomAppBar.prepareCradleLayout(ui.cradleLayout)

        shoppingListViewModel.items.observe(this) { newList ->
            updateShoppingListBadge(newList)
        }

        ui.topActionBar.ui.backButton.setOnClickListener { onSupportNavigateUp() }
        onCreateOptionsMenu(ui.topActionBar.optionsMenu)
        ui.topActionBar.onDeleteButtonClickedListener = {
            onOptionsItemSelected(ui.topActionBar.optionsMenu.findItem(R.id.delete_selected_menu_item))
        }
        ui.topActionBar.setOnSortOptionClickedListener { item ->
            onOptionsItemSelected(item)
        }
        ui.topActionBar.setOnOptionsItemClickedListener { item ->
            onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showingInventory", showingInventory)
        supportFragmentManager.putFragment(outState, "shoppingListFragment", shoppingListFragment)
        supportFragmentManager.putFragment(outState, "inventoryFragment",    inventoryFragment)
        outState.putBoolean("showingPreferences", showingPreferences)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settings_menu_item) {
            showPreferencesFragment()
            return true
        }
        return activeFragment.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp() = when {
        showingPreferences -> {
            supportFragmentManager.popBackStack()
            true
        } activeFragment.searchIsActive -> {
            ui.topActionBar.ui.searchView.findViewById<ImageView>(
                androidx.appcompat.R.id.search_close_btn)
                .apply { performClick(); performClick() }
            true
        } activeFragment.actionMode.isStarted -> {
            activeFragment.actionMode.finishAndClearSelection()
            true
        } else -> false
    }

    override fun onBackPressed() {
        if (showingPreferences) supportFragmentManager.popBackStack()
        else                    super.onBackPressed()
    }

    private fun showPreferencesFragment(animate: Boolean = true) {
        showingPreferences = true
        inputMethodManager(this)?.hideSoftInputFromWindow(ui.bottomAppBar.windowToken, 0)
        showBottomAppBar(false)
        activeFragment.isActive = false
        ui.topActionBar.ui.backButton.isVisible = true

        val enterAnimResId = if (animate) R.animator.fragment_close_enter else 0
        supportFragmentManager.beginTransaction().
            setCustomAnimations(enterAnimResId, R.animator.fragment_close_exit,
                                enterAnimResId, R.animator.fragment_close_exit).
            hide(activeFragment).
            add(R.id.fragmentContainer, PreferencesFragment()).
            addToBackStack(null).commit()
    }

    private fun switchToInventory() = toggleMainFragments(switchingToInventory = true)
    private fun switchToShoppingList() = toggleMainFragments(switchingToInventory = false)
    private fun toggleMainFragments(switchingToInventory: Boolean) {
        if (showingPreferences) return

        val oldFragment = activeFragment
        showingInventory = switchingToInventory
        showCheckoutButton(showing = !showingInventory)
        inputMethodManager(this)?.hideSoftInputFromWindow(ui.bottomAppBar.windowToken, 0)

        val newFragmentTranslationStart = ui.fragmentContainer.width * if (showingInventory) 1f else -1f
        val fragmentTranslationAmount = ui.fragmentContainer.width * if (showingInventory) -1f else 1f

        oldFragment.isActive = false
        val oldFragmentView = oldFragment.view
        oldFragmentView?.animate()
            ?.translationXBy(fragmentTranslationAmount)
            ?.applyConfig(AnimatorConfig.transition)
            ?.withEndAction { oldFragmentView.visibility = View.INVISIBLE }
            ?.start()

        activeFragment.isActive = true
        val newFragmentView = activeFragment.view
        newFragmentView?.translationX = newFragmentTranslationStart
        newFragmentView?.visibility = View.VISIBLE
        newFragmentView?.animate()
            ?.applyConfig(AnimatorConfig.transition)
            ?.translationX(0f)?.start()
    }

    fun showBottomAppBar(show: Boolean = true) {
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
        android.R.anim.accelerate_decelerate_interpolator
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
        if (shoppingListSize == -1) {
            if (newShoppingList.isNotEmpty())
                shoppingListSize = newShoppingList.size
        } else {
            val sizeChange = newShoppingList.size - shoppingListSize
            if (activeFragment == inventoryFragment && sizeChange > 0) {
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

    private fun initFragments(savedInstanceState: Bundle?) {
        showingInventory = savedInstanceState?.getBoolean("showingInventory") ?: false
        showingPreferences = savedInstanceState?.getBoolean("showingPreferences") ?: false

        if (savedInstanceState != null) {
            shoppingListFragment = supportFragmentManager.getFragment(
                savedInstanceState, "shoppingListFragment") as ShoppingListFragment
            inventoryFragment = supportFragmentManager.getFragment(
                savedInstanceState, "inventoryFragment") as InventoryFragment

            if (showingPreferences) {
                showBottomAppBar(false)
                ui.topActionBar.ui.backButton.isVisible = true
            }

        } else {
            shoppingListFragment = ShoppingListFragment(isActive = !showingInventory)
            inventoryFragment = InventoryFragment(isActive = showingInventory)
            supportFragmentManager.beginTransaction().
                add(R.id.fragmentContainer, shoppingListFragment, "shoppingList").
                add(R.id.fragmentContainer, inventoryFragment, "inventory").
                commit()
        }
    }

    private val onNavigationItemSelected = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        if (item.isChecked) false // Selected item was already selected
        else {
            item.isChecked = true
            toggleMainFragments(switchingToInventory = item.itemId == R.id.inventory_button)

            val newIcon = findViewById<View>(
                if (item.itemId == R.id.inventory_button) R.id.inventory_button
                else                                      R.id.shopping_list_button)
            val indicatorNewXPos = (newIcon.width - ui.bottomAppBar.indicatorWidth) / 2 + newIcon.left
            ValueAnimator.ofInt(ui.bottomAppBar.indicatorXPos, indicatorNewXPos).apply {
                addUpdateListener { ui.bottomAppBar.indicatorXPos = it.animatedValue as Int }
                applyConfig(AnimatorConfig.transition)
            }.start()
            true
        }
    }

    val sysDarkThemeIsActive get() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
}

@Module @InstallIn(ActivityComponent::class)
object MainActivityBindingModule {
    @Provides fun provideMainActivityBinding(@ActivityContext context: Context) =
        (context as MainActivity).ui
}
