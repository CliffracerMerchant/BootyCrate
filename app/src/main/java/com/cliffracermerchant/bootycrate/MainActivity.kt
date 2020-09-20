/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.AnimatorInflater
import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*

/** The primary activity for BootyCrate
 *
 *  Instead of switching between activities, nearly everything in BootyCrate is
 *  accomplished in the ShoppingListFragment, InventoryFragment, or the Preferences-
 *  Fragment. Instances of these fragments are instantiated on app startup, and
 *  hidden/shown by the fragment manager as appropriate. The active fragment can be
 *  determined via the boolean members showingInventory and showingPreferences as
 *  follows:
 *  Active fragment = if (showingPreferences)    PreferencesFragment
 *                    else if (showingInventory) InventoryFragment
 *                    else                       ShoppingListFragment
 *  If showingPreferences is true, the value of showingInventory determines the
 *  fragment "under" the preferences (i.e. the one that will be returned to on a
 *  back button press or a navigate up).
 *
 *  Both ShoppingListFragment and InventoryFragment are expected to have an
 *  enable() and a disable() function to be called when they are shown or hidden,
 *  respectively. These functions should prepare the main activity's UI for that
 *  fragment, e.g. by setting the floating action button's on click listener or
 *  icon.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var shoppingListFragment: ShoppingListFragment
    private lateinit var inventoryFragment: InventoryFragment
    private lateinit var preferencesFragment: PreferencesFragment
    private lateinit var imm: InputMethodManager
    private var showingInventory = false
    private var showingPreferences = false
    private var checkoutButtonIsVisible = true
    private var shoppingListSize = -1
    private var shoppingListNumNewItems = 0

    lateinit var inventoryViewModel: InventoryViewModel
    lateinit var shoppingListViewModel: ShoppingListViewModel
    lateinit var fab: FloatingActionButton
    lateinit var checkoutBtn: MaterialButton

    private var cradleLayoutFullWidth = -1
    private var fabWidth = -1
    private var blackColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO: Use new state manager again once bug causing options menu to not
        // show up is fixed (https://issuetracker.google.com/issues/168357317)
        FragmentManager.enableNewStateManager(false)
        super.onCreate(savedInstanceState)

        val prefs = getDefaultSharedPreferences(this)
        /* The activity's ViewModelStore will by default retain instances of the
           app's viewmodels across activity restarts. In case this is not desired
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
        inventoryViewModel = ViewModelProvider(this).get(InventoryViewModel::class.java)
        shoppingListViewModel = ViewModelProvider(this).get(ShoppingListViewModel::class.java)

        prefKey = getString(R.string.pref_dark_theme_active)
        val darkThemeActive = prefs.getBoolean(prefKey, false)
        setTheme(if (darkThemeActive) R.style.DarkTheme
                 else                 R.style.LightTheme)

        setContentView(R.layout.activity_main)
        setSupportActionBar(topActionBar)
        fab = floatingActionButton
        checkoutBtn = checkoutButton
        imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        blackColor = ContextCompat.getColor(this, android.R.color.black)

        bottomNavigationBar.setOnNavigationItemSelectedListener { item ->
            if (item.isChecked) false // Selected item was already selected
            else {
                item.isChecked = true
                toggleShoppingListInventoryFragments(switchingToInventory = item.itemId == R.id.inventory_button)
                true
            }
        }
        initFragments(savedInstanceState)

        if (showingInventory) showCheckoutButton(showing = false, animate = false)
        if (showingPreferences) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            bottomAppBar.translationY = 250f
            fab.translationY = 250f
            checkoutBtn.translationY = 250f
        }
        bottomAppBar.prepareCradleLayout(cradleLayout)

        shoppingListViewModel.items.observe(this, Observer { newList ->
            updateShoppingListBadge(newList)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)
        // Setting the SearchView icon color manually is a temporary work-
        // around because setting it in the theme/style did not work.
        val searchView = menu.findItem(R.id.app_bar_search)?.actionView as SearchView?
        (searchView?.findViewById(androidx.appcompat.R.id.search_close_btn) as ImageView).
            setColorFilter(blackColor)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showingPreferences", showingPreferences)
        outState.putBoolean("showingInventory", showingInventory)
        supportFragmentManager.putFragment(outState, "shoppingListFragment", shoppingListFragment)
        supportFragmentManager.putFragment(outState, "inventoryFragment",    inventoryFragment)
        supportFragmentManager.putFragment(outState, "preferencesFragment",  preferencesFragment)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settings_menu_item) {
            togglePreferencesFragment(showing = true)
            return true
        }
        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (showingPreferences) {
            togglePreferencesFragment(showing = false)
            true
        } else false
    }

    override fun onBackPressed() { onSupportNavigateUp() }

    private fun togglePreferencesFragment(showing: Boolean) {
        val oldFragment = when { !showing ->         preferencesFragment
                                 showingInventory -> inventoryFragment
                                 else ->             shoppingListFragment }
        val newFragment = when { showing ->          preferencesFragment
                                 showingInventory -> inventoryFragment
                                 else ->             shoppingListFragment }
        showingPreferences = showing
        imm.hideSoftInputFromWindow(bottomAppBar.windowToken, 0)
        supportActionBar?.setDisplayHomeAsUpEnabled(showing)
        bottomAppBar.animate().translationY(if (showing) 250f else 0f).withLayer().start()
        fab.animate().translationY(if (showing) 250f else 0f).withLayer().start()
        checkoutBtn.animate().translationY(if (showing) 250f else 0f).withLayer().start()
        val exitAnimation = AnimatorInflater.loadAnimator(this, R.animator.fragment_close_exit)
        exitAnimation.setTarget(oldFragment.view)
        exitAnimation.doOnStart{ oldFragment.view?.setLayerType(View.LAYER_TYPE_HARDWARE, null) }
        exitAnimation.doOnEnd{ oldFragment.view?.setLayerType(View.LAYER_TYPE_NONE, null)
                               supportFragmentManager.beginTransaction().hide(oldFragment).commit() }
        supportFragmentManager.beginTransaction().runOnCommit{ exitAnimation.start() }.
            setCustomAnimations(R.animator.fragment_close_enter, 0).show(newFragment).commit()
    }

    private fun toggleShoppingListInventoryFragments(switchingToInventory: Boolean) {
        if (showingPreferences) return

        showingInventory = switchingToInventory
        showCheckoutButton(showing = !switchingToInventory)
        imm.hideSoftInputFromWindow(bottomAppBar.windowToken, 0)
        val oldFragment = if (switchingToInventory) shoppingListFragment
                          else                      inventoryFragment
        val newFragment = if (switchingToInventory) inventoryFragment
                          else                      shoppingListFragment

        val enterAndExitAnims = ViewPropertyAnimatorSet()
        var translationAmount = fragmentContainer.width * 1f
        if (switchingToInventory) translationAmount *= -1f

        inventoryFragment.view?.translationX = if (showingInventory) -translationAmount else 0f
        val inventoryFragmentAnim = inventoryFragment.view?.animate()?.
            withLayer()?.translationXBy(translationAmount)?.setDuration(300)

        shoppingListFragment.view?.translationX = if (showingInventory) 0f else -translationAmount
        val shoppingListFragmentAnim = shoppingListFragment.view?.animate()?.
            withLayer()?.translationXBy(translationAmount)?.setDuration(300)

        if (switchingToInventory) shoppingListFragmentAnim?.withEndAction {
            supportFragmentManager.beginTransaction().hide(shoppingListFragment).commit()
        }
        else inventoryFragmentAnim?.withEndAction {
            supportFragmentManager.beginTransaction().hide(inventoryFragment).commit()
        }
        if (shoppingListFragmentAnim != null) enterAndExitAnims.add(shoppingListFragmentAnim)
        if (inventoryFragmentAnim != null) enterAndExitAnims.add(inventoryFragmentAnim)
        oldFragment.disable()
        newFragment.enable()
        supportFragmentManager.beginTransaction().show(newFragment).
            runOnCommit{ enterAndExitAnims.start() }.commit()
    }

    private fun showCheckoutButton(showing: Boolean, animate: Boolean = true) {
        if (checkoutButtonIsVisible == showing) return

        if (cradleLayoutFullWidth == -1) {
            val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            cradleLayout.measure(wrapContentSpec, wrapContentSpec)
            cradleLayoutFullWidth = cradleLayout.measuredWidth
            fab.measure(wrapContentSpec, wrapContentSpec)
            fabWidth = fab.measuredWidth
        }
        val cradleLayoutStartWidth = if (showing) fabWidth else cradleLayoutFullWidth
        val cradleLayoutEndWidth =   if (showing) cradleLayoutFullWidth else fabWidth

        if (animate) {
            val cradleLayoutWidthChange = cradleLayoutEndWidth - cradleLayoutStartWidth

            val anim = ValueAnimator.ofInt(cradleLayoutStartWidth, cradleLayoutEndWidth)
            anim.addUpdateListener {
                fab.translationX = cradleLayoutWidthChange / 2f *
                        if (showing) (it.animatedFraction - 1)
                        else         it.animatedFraction
                checkoutBtn.scaleX = if (showing) it.animatedFraction
                                     else         (1 - it.animatedFraction)
                bottomAppBar.cradleWidth = it.animatedValue as Int
                bottomAppBar.background.invalidateSelf()
            }
            anim.doOnStart {
                fab.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                checkoutBtn.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                checkoutBtn.visibility = View.VISIBLE
                checkoutBtn.requestLayout()
            }
            anim.doOnEnd {
                fab.setLayerType(View.LAYER_TYPE_NONE, null)
                checkoutBtn.setLayerType(View.LAYER_TYPE_NONE, null)
                fab.translationX = 0f
                if (!showing) checkoutBtn.visibility = View.GONE
                checkoutBtn.requestLayout()
                bottomAppBar.background.invalidateSelf()
            }
            anim.start()
        } else {
            checkoutBtn.visibility = if (showing) View.VISIBLE else View.GONE
            checkoutBtn.scaleX = if (showing) 1f else 0f
            cradleLayout.requestLayout()
            bottomAppBar.cradleWidth = cradleLayoutEndWidth
            bottomAppBar.background.invalidateSelf()
        }
        checkoutButtonIsVisible = showing
    }

    private fun updateShoppingListBadge(newShoppingList: List<ShoppingListItem>) {
        if (shoppingListSize == -1) {
            if (newShoppingList.isNotEmpty())
                shoppingListSize = newShoppingList.size
        } else {
            val sizeChange = newShoppingList.size - shoppingListSize
            if (!showingPreferences && showingInventory && sizeChange > 0) {
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
        if (savedInstanceState != null) {
            shoppingListFragment = supportFragmentManager.getFragment(
                savedInstanceState, "shoppingListFragment") as ShoppingListFragment
            inventoryFragment = supportFragmentManager.getFragment(
                savedInstanceState, "inventoryFragment") as InventoryFragment
            preferencesFragment = supportFragmentManager.getFragment(
                savedInstanceState, "preferencesFragment") as PreferencesFragment
        } else {
            shoppingListFragment = ShoppingListFragment()
            inventoryFragment = InventoryFragment()
            preferencesFragment = PreferencesFragment()
        }

        showingInventory = savedInstanceState?.getBoolean("showingInventory") ?: false
        showingPreferences = savedInstanceState?.getBoolean("showingPreferences") ?: false

        val transaction = supportFragmentManager.beginTransaction()
        if (savedInstanceState == null) transaction.
        add(R.id.fragmentContainer, preferencesFragment, "preferences").
        add(R.id.fragmentContainer, inventoryFragment, "inventory").
        add(R.id.fragmentContainer, shoppingListFragment, "shoppingList")

        val hiddenFragment1 = if (showingInventory) shoppingListFragment
        else                  inventoryFragment
        val hiddenFragment2 = when { !showingPreferences -> preferencesFragment
            showingInventory ->    inventoryFragment
            else ->                shoppingListFragment }
        transaction.hide(hiddenFragment1).hide(hiddenFragment2).runOnCommit {
            if (!showingPreferences) if (showingInventory) inventoryFragment.enable()
            else                  shoppingListFragment.enable()
        }.commit()
    }
}