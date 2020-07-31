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
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
    private val shoppingListFragment = ShoppingListFragment()
    private val inventoryFragment = InventoryFragment()
    private val preferencesFragment = PreferencesFragment()
    private var showingInventory = false
    private var showingPreferences = false

    lateinit var inventoryViewModel: InventoryViewModel
    lateinit var shoppingListViewModel: ShoppingListViewModel
    lateinit var fab: FloatingActionButton
    lateinit var checkoutButton: MaterialButton

    private var blackColor: Int = 0
    private var cradleLayoutInitialWidth = -1

    private var checkoutButtonIsHidden = false
        set(value) { showCheckoutButton(value); field = value }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inventoryViewModel = ViewModelProvider(this).get(InventoryViewModel::class.java)
        shoppingListViewModel = ViewModelProvider(this).get(ShoppingListViewModel::class.java)

        val prefs = getDefaultSharedPreferences(this)
        val darkThemeActive = prefs.getBoolean(getString(R.string.pref_dark_theme_active), false)
        setTheme(if (darkThemeActive) R.style.DarkTheme
                 else                 R.style.LightTheme)

        setContentView(R.layout.activity_main)
        setSupportActionBar(action_bar)
        fab = floating_action_button
        checkoutButton = checkout_button
        blackColor = ContextCompat.getColor(this, android.R.color.black)

        bottom_navigation_bar.setOnNavigationItemSelectedListener { item ->
            item.isChecked = true
            toggleShoppingListInventoryFragments(switchingToInventory = item.itemId == R.id.inventory_button)
            true
        }
        supportFragmentManager.beginTransaction().
                add(R.id.fragment_container, preferencesFragment).
                add(R.id.fragment_container, inventoryFragment).
                add(R.id.fragment_container, shoppingListFragment).
                hide(preferencesFragment).hide(inventoryFragment).
                runOnCommit{ shoppingListFragment.enable() }.commit()
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
        supportActionBar?.setDisplayHomeAsUpEnabled(showing)
        bottom_app_bar.animate().translationYBy(if (showing) 250f else -250f).start()
        fab.animate().translationYBy(if (showing) 250f else -250f).start()
        supportFragmentManager.beginTransaction().
            setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right).
            hide(oldFragment).show(newFragment).commit()
    }

    private fun toggleShoppingListInventoryFragments(switchingToInventory: Boolean) {
        showingInventory = switchingToInventory
        checkoutButtonIsHidden = switchingToInventory
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

    private fun showCheckoutButton(showing: Boolean) {
        if (checkoutButtonIsHidden == showing) return
        if (cradleLayoutInitialWidth == -1) cradleLayoutInitialWidth = cradle_layout.width
        val cradleLayoutStartWidth = if (showing) cradle_layout.width else fab.width
        val cradleLayoutEndWidth = if (showing) fab.width else cradleLayoutInitialWidth
        val cradleLayoutWidthChange = cradleLayoutEndWidth - cradleLayoutStartWidth
        val anim = ValueAnimator.ofFloat(if (showing) 1f else 0f,
                                         if (showing) 0f else 1f)
        anim.addUpdateListener {
            checkout_button.scaleX = it.animatedValue as Float
            cradle_layout.layoutParams.width = cradleLayoutStartWidth +
                    (cradleLayoutWidthChange * it.animatedFraction).toInt()
            cradle_layout.requestLayout()
            bottom_app_bar.redrawCradle()
        }
        anim.start()
    }
}