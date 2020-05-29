package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var actionMode: ActionMode? = null
    lateinit var fab: FloatingActionButton
    lateinit var bab: BottomAppBar

    enum class FragmentID { InventoryView, Preferences }

    companion object {
        var activeFragmentID: FragmentID? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getDefaultSharedPreferences(this)
        val darkThemeActive = prefs.getBoolean(getString(R.string.pref_dark_theme_active), false)
        setTheme(if (darkThemeActive) R.style.DarkTheme
                 else                 R.style.LightTheme)

        setContentView(R.layout.activity_main)
        setSupportActionBar(action_bar)
        fab = floating_action_button
        bab = bottom_app_bar
        val currentFragmentId = when (activeFragmentID) {
            FragmentID.Preferences -> R.id.preferences_navigation_button
            else ->                   R.id.inventory_navigation_button
        }
        bottom_navigation_bar.itemIconTintList = null
        bottom_navigation_bar.setOnNavigationItemSelectedListener(onNavigationItemSelected)
        bottom_navigation_bar.selectedItemId = currentFragmentId
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.preferences_menu_item) { }
            //startActivity(Intent(this, PreferencesActivity::class.java))
        return super.onOptionsItemSelected(item)
    }

    private val onNavigationItemSelected =
            BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
        when (item.itemId) {
            R.id.inventory_navigation_button -> {
                actionMode?.finish()
                activeFragmentID = FragmentID.InventoryView
                supportFragmentManager.beginTransaction().replace(
                    R.id.fragment_container, InventoryViewFragment.instance).commit()
            }
            R.id.shopping_list_navigation_button -> {

            }
            R.id.preferences_navigation_button -> {
                if (activeFragmentID == FragmentID.InventoryView)
                    supportFragmentManager.saveFragmentInstanceState(InventoryViewFragment.instance)
                actionMode?.finish()
                activeFragmentID = FragmentID.Preferences
                supportFragmentManager.beginTransaction().replace(
                    R.id.fragment_container, PreferencesFragment.instance).commit()
                floating_action_button.setOnClickListener(null)
            }
        }
        false
    }
}