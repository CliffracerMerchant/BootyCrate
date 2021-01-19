/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*

/** The primary activity for BootyCrate
 *
 *  Instead of switching between activities, nearly everything in BootyCrate is
 *  accomplished in the ShoppingListFragment, InventoryFragment, or the Preferences-
 *  Fragment. Instances of ShoppingListFragment and InventoryFragment are created
 *  on app startup, and hidden/shown by the fragment manager as appropriate. The
 *  currently shown fragment can be determined via the boolean members showing-
 *  Inventory and showingPreferences as follows:
 *  Shown fragment = if (showingPreferences)    PreferencesFragment
 *                   else if (showingInventory) InventoryFragment
 *                   else                       ShoppingListFragment
 *  If showingPreferences is true, the value of showingInventory determines the
 *  fragment "under" the preferences (i.e. the one that will be returned to on a
 *  back button press or a navigate up). */
open class MainActivity : AppCompatActivity() {
    private lateinit var shoppingListFragment: ShoppingListFragment
    private lateinit var inventoryFragment: InventoryFragment
    private lateinit var imm: InputMethodManager
    private var showingInventory = false
    private var showingPreferences = false
    val activeFragment get() = if (showingInventory) inventoryFragment
                               else                  shoppingListFragment

    private var checkoutButtonIsVisible = true
    private var shoppingListSize = -1
    private var shoppingListNumNewItems = 0
    private var pendingCradleAnim: Animator? = null

    lateinit var shoppingListViewModel: ShoppingListViewModel
    lateinit var inventoryViewModel: InventoryViewModel
    lateinit var addButton: OutlinedGradientButton
    lateinit var checkoutButton: CheckoutButton
    lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getDefaultSharedPreferences(this)
     /* The activity's ViewModelStore will by default retain instances of the
        app's view models across activity restarts. In case this is not desired
        (e.g. when the database was replaced with an external one, and the view-
        models therefore need to be reset), setting the shared preference whose
        key is equal to the value of R.string.pref_viewmodels_need_cleared to
        true will cause MainActivity to call viewModelStore.clear() */
        var prefKey = getString(R.string.pref_viewmodels_need_cleared)
        if (prefs.getBoolean(prefKey, false)) {
            viewModelStore.clear()
            val editor = prefs.edit()
            editor.putBoolean(prefKey, false)
            editor.apply()
        }
        shoppingListViewModel = ViewModelProvider(this).get(ShoppingListViewModel::class.java)
        inventoryViewModel = ViewModelProvider(this).get(InventoryViewModel::class.java)

        prefKey = getString(R.string.pref_dark_theme_active)
        setTheme(if (prefs.getBoolean(prefKey, false)) R.style.DarkTheme
                 else                                  R.style.LightTheme)

        setContentView(R.layout.activity_main)
        setSupportActionBar(topActionBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        addButton = add_button
        checkoutButton = checkout_button
        imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        cradleLayout.layoutTransition = delaylessLayoutTransition()
        cradleLayout.layoutTransition.doOnStart { _, _, _, _ ->
            // These z values seem not to stick when set in XML, so we have to
            // set them here every time to ensure that the addButton remains on
            // top of the others.
            addButton.z = 1f
            checkoutButton.z = -2f
            disabled_checkout_button.z = -1f
            pendingCradleAnim?.start()
            pendingCradleAnim = null
        }

        bottomAppBar.indicatorWidth = 3 * bottomNavigationBar.itemIconSize
        bottomNavigationBar.setOnNavigationItemSelectedListener(onNavigationItemSelected)

        Dialog.init(activity = this, snackBarParent = coordinatorLayout)
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                showBottomAppBar()
                showingPreferences = false
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                activeFragment.isActive = true
            }
        }
        initFragments(savedInstanceState)
        val navButton = findViewById<View>(if (showingInventory) R.id.inventory_button
                                           else                  R.id.shopping_list_button)
        navButton.doOnNextLayout {
            bottomAppBar.indicatorXPos = (it.width - bottomAppBar.indicatorWidth) / 2 + it.left
        }
        if (showingInventory)
            showCheckoutButton(showing = false, animate = false)
        bottomAppBar.prepareCradleLayout(cradleLayout)

        shoppingListViewModel.items.observe(this) { newList ->
            updateShoppingListBadge(newList)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)
        super.onCreateOptionsMenu(menu)
        this.menu = menu
        shoppingListFragment.initOptionsMenu(menu)
        inventoryFragment.initOptionsMenu(menu)
        return true
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
        return false
    }

    override fun onSupportNavigateUp() = when {
        showingPreferences -> {
            supportFragmentManager.popBackStack()
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
        imm.hideSoftInputFromWindow(bottomAppBar.windowToken, 0)
        showBottomAppBar(false)
        activeFragment.isActive = false
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        imm.hideSoftInputFromWindow(bottomAppBar.windowToken, 0)

        val newFragmentTranslationStart = fragmentContainer.width * if (showingInventory) 1f else -1f
        val fragmentTranslationAmount = fragmentContainer.width * if (showingInventory) -1f else 1f

        oldFragment.isActive = false
        val oldFragmentView = oldFragment.view
        oldFragmentView?.animate()?.translationXBy(fragmentTranslationAmount)?.
                                    setDuration(300)?.//withLayer()?.
                                    withEndAction { oldFragmentView.visibility = View.INVISIBLE }?.
                                    start()

        activeFragment.isActive = true
        val newFragmentView = activeFragment.view
        newFragmentView?.translationX = newFragmentTranslationStart
        newFragmentView?.visibility = View.VISIBLE
        newFragmentView?.animate()?.translationX(0f)?.setDuration(300)?.start()//withLayer()
    }

    private fun showBottomAppBar(show: Boolean = true) {
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val views = arrayOf<View>(bottomAppBar, addButton, checkoutButton)

        if (!show && bottomAppBar.height == 0) {
            bottomAppBar.doOnNextLayout {
                val translationAmount = screenHeight - cradleLayout.top
                for (view in views) view.translationY = translationAmount
            }
            return
        }
        val translationAmount = screenHeight - cradleLayout.top
        val translationStart = if (show) translationAmount else 0f
        val translationEnd =   if (show) 0f else translationAmount
        for (view in views) {
            view.translationY = translationStart
            view.animate().withLayer().translationY(translationEnd).start()
        }
    }

    private fun showCheckoutButton(showing: Boolean, animate: Boolean = true) {
        if (checkoutButtonIsVisible == showing) return

        checkoutButtonIsVisible = showing
        checkoutButton.visibility = if (showing) View.VISIBLE else View.GONE

        val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        cradleLayout.measure(wrapContentSpec, wrapContentSpec)
        val cradleEndWidth = if (showing) cradleLayout.measuredWidth
                             else         addButton.layoutParams.width

        if (!animate) {
            bottomAppBar.cradleWidth = cradleEndWidth
            return
        }
        // Settings the checkout button's clip bounds prevents the
        // right corners of the checkout button from sticking out
        // underneath the FAB during the show / hide animation.
        val checkoutBtnClipBounds = Rect(0, 0, 0, checkoutButton.height)
        ObjectAnimator.ofInt(bottomAppBar, "cradleWidth", cradleEndWidth).apply {
            interpolator = cradleLayout.layoutTransition.getInterpolator(LayoutTransition.CHANGE_APPEARING)
            duration = cradleLayout.layoutTransition.getDuration(LayoutTransition.CHANGE_APPEARING)
            addUpdateListener {
                checkoutBtnClipBounds.right = bottomAppBar.cradleWidth - addButton.measuredWidth / 2
                checkoutButton.clipBounds = checkoutBtnClipBounds
                disabled_checkout_button.clipBounds = checkoutBtnClipBounds
            }
            doOnEnd { checkoutButton.clipBounds = null
                      disabled_checkout_button.clipBounds = null }
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
                shoppingListBadge.text = getString(R.string.shopping_list_badge_text,
                                                   shoppingListNumNewItems)
                shoppingListBadge.clearAnimation()
                shoppingListBadge.alpha = 1f
                shoppingListBadge.animate().alpha(0f).setDuration(1000).setStartDelay(1500).
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
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
            val indicatorNewXPos = (newIcon.width - bottomAppBar.indicatorWidth) / 2 + newIcon.left
            ObjectAnimator.ofInt(bottomAppBar, "indicatorXPos", indicatorNewXPos).apply {
                duration = cradleLayout.layoutTransition.getDuration(LayoutTransition.CHANGE_APPEARING)
            }.start()
            true
        }
    }
}
