/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

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
import androidx.core.content.ContextCompat
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
    private var showingInventory = false
    private var showingPreferences = false

    lateinit var inventoryViewModel: InventoryViewModel
    lateinit var shoppingListViewModel: ShoppingListViewModel
    lateinit var fab: FloatingActionButton
    lateinit var checkoutBtn: MaterialButton

    private lateinit var imm: InputMethodManager
    private var blackColor: Int = 0

    private var checkoutButtonIsVisible = true
        set(value) { showHideCheckoutButton(value, true); field = value }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inventoryViewModel = ViewModelProvider(this).get(InventoryViewModel::class.java)
        shoppingListViewModel = ViewModelProvider(this).get(ShoppingListViewModel::class.java)

        val prefs = getDefaultSharedPreferences(this)
        val darkThemeActive = prefs.getBoolean(getString(R.string.pref_dark_theme_active), false)
        setTheme(if (darkThemeActive) R.style.DarkTheme
                 else                 R.style.LightTheme)

        setContentView(R.layout.activity_main)
        setSupportActionBar(topActionBar)
        fab = floatingActionButton
        checkoutBtn = checkoutButton
        imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        blackColor = ContextCompat.getColor(this, android.R.color.black)

        bottomNavigationBar.setOnNavigationItemSelectedListener { item ->
            item.isChecked = true
            toggleShoppingListInventoryFragments(switchingToInventory = item.itemId == R.id.inventory_button)
            true
        }

        if (savedInstanceState != null) {
            shoppingListFragment = supportFragmentManager.getFragment(
                savedInstanceState, "shoppingListFragment") as ShoppingListFragment
            inventoryFragment = supportFragmentManager.getFragment(
                savedInstanceState, "inventoryFragment") as InventoryFragment
            preferencesFragment = supportFragmentManager.getFragment(
                savedInstanceState, "preferencesFragment") as PreferencesFragment
            //See preferencesFragment.updateItemDecoration definition for why this is necessary
            preferencesFragment.updateItemDecoration(this)
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

        if (showingInventory) showHideCheckoutButton(showing = false, animate = false)
        if (showingPreferences) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            bottomAppBar.translationY = 250f
            fab.translationY = 250f
            checkoutBtn.translationY = 250f
        }
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
        supportFragmentManager.beginTransaction().
            setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right).
            hide(oldFragment).show(newFragment).commit()
    }

    private fun toggleShoppingListInventoryFragments(switchingToInventory: Boolean) {
        showingInventory = switchingToInventory
        checkoutButtonIsVisible = !switchingToInventory
        imm.hideSoftInputFromWindow(bottomAppBar.windowToken, 0)
        if (switchingToInventory) {
            shoppingListFragment.disable()
            inventoryFragment.enable()
            supportFragmentManager.beginTransaction().
                setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right).
                hide(shoppingListFragment).show(inventoryFragment).commit()
        } else {
            inventoryFragment.disable()
            shoppingListFragment.enable()
            supportFragmentManager.beginTransaction().
                setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right).
                hide(inventoryFragment).show(shoppingListFragment).commit()
        }
    }

    private fun showHideCheckoutButton(showing: Boolean, animate: Boolean) {
        val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        cradleLayout.measure(wrapContentSpec, wrapContentSpec)
        val cradleLayoutFullWidth = cradleLayout.measuredWidth
        fab.measure(wrapContentSpec, wrapContentSpec)
        val fabWidth = fab.measuredWidth

        if (animate && checkoutButtonIsVisible != showing) {
            val cradleLayoutStartWidth = if (showing) fabWidth else cradleLayoutFullWidth
            val cradleLayoutEndWidth =   if (showing) cradleLayoutFullWidth else fabWidth
            val cradleLayoutWidthChange = cradleLayoutEndWidth - cradleLayoutStartWidth
            checkoutBtn.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            val anim = ValueAnimator.ofFloat(if (showing) 0f else 1f,
                                             if (showing) 1f else 0f)
            anim.addUpdateListener {
                checkoutBtn.scaleX = it.animatedValue as Float
                cradleLayout.layoutParams.width = cradleLayoutStartWidth +
                        (cradleLayoutWidthChange * it.animatedFraction).toInt()
                cradleLayout.requestLayout()
                bottomAppBar.background.invalidateSelf()
            }
            anim.doOnEnd { checkoutBtn.setLayerType(View.LAYER_TYPE_NONE, null) }
            anim.start()
        } else {
            checkoutBtn.scaleX = if (showing) 1f else 0f
            cradleLayout.layoutParams.width = if (showing) cradleLayoutFullWidth
                                               else         fabWidth
            cradleLayout.requestLayout()
            bottomAppBar.background.invalidateSelf()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showingPreferences", showingPreferences)
        outState.putBoolean("showingInventory", showingInventory)
        supportFragmentManager.putFragment(outState, "shoppingListFragment", shoppingListFragment)
        supportFragmentManager.putFragment(outState, "inventoryFragment", inventoryFragment)
        supportFragmentManager.putFragment(outState, "preferencesFragment", preferencesFragment)
    }
}