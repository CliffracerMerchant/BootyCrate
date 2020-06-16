package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var actionMode: ActionMode? = null
    private var enabledFabVerticalOffset: Float = 0f
    private var disabledFabVerticalOffset: Float = 0f
    lateinit var inventoryViewModel: InventoryViewModel
    lateinit var shoppingListViewModel: ShoppingListViewModel
    lateinit var fab: FloatingActionButton
    var menu: Menu? = null

    companion object { var selectedNavigationItemId: Int? = null }

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
        enabledFabVerticalOffset = bottom_app_bar.cradleVerticalOffset
        disabledFabVerticalOffset = -0.5f * bottom_app_bar.cradleVerticalOffset

        bottom_navigation_bar.itemIconTintList = null
        bottom_navigation_bar.setOnNavigationItemSelectedListener(onNavigationItemSelected)
        bottom_navigation_bar.selectedItemId = selectedNavigationItemId ?:
                                               R.id.inventory_navigation_button
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)
        this.menu = menu
        return true
    }

    private val onNavigationItemSelected =
            BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
        for (fragment in supportFragmentManager.fragments)
            supportFragmentManager.saveFragmentInstanceState(fragment)
        actionMode?.finish()
        floating_action_button.setOnClickListener(null)

        when (item.itemId) {
            R.id.inventory_navigation_button -> {
                if (selectedNavigationItemId == R.id.preferences_navigation_button) {
                    floating_action_button.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                    ObjectAnimator.ofFloat(bottom_app_bar, "cradleVerticalOffset",
                                           enabledFabVerticalOffset).setDuration(200).start()
                }
                selectedNavigationItemId = item.itemId
                supportFragmentManager.beginTransaction().
                    setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right).
                    replace(R.id.fragment_container, InventoryFragment.instance).commit()
            }
            R.id.shopping_list_navigation_button -> {
                if (selectedNavigationItemId == R.id.preferences_navigation_button) {
                    floating_action_button.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                    ObjectAnimator.ofFloat(bottom_app_bar, "cradleVerticalOffset",
                                           enabledFabVerticalOffset).setDuration(200).start()
                }
                selectedNavigationItemId = item.itemId
                supportFragmentManager.beginTransaction().
                setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right).
                replace(R.id.fragment_container, ShoppingListFragment.instance).commit()
            }
            R.id.preferences_navigation_button -> {
                floating_action_button.animate().scaleX(0f).scaleY(0f).setDuration(200).start()
                ObjectAnimator.ofFloat(bottom_app_bar, "cradleVerticalOffset",
                                       disabledFabVerticalOffset).setDuration(200).start()
                selectedNavigationItemId = item.itemId
                supportFragmentManager.beginTransaction().
                setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right).
                replace(R.id.fragment_container, PreferencesFragment.instance).commit()
            }
        }
        false
    }
}