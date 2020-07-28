package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
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

class MainActivity : AppCompatActivity() {
    private var darkGrayColor: Int = 0
    private var lightGrayColor: Int = 0
    private var blackColor: Int = 0
    private var yellowColor: Int = 0
    private var cradleLayoutInitialWidth = -1
    var checkoutButtonIsEnabled = false
        set(value) {
            if (checkoutButtonIsEnabled == value) return

            val bgColorAnim = ValueAnimator.ofArgb(if (value) lightGrayColor else yellowColor,
                                                   if (value) yellowColor else lightGrayColor)
            bgColorAnim.addUpdateListener {
                checkout_button.backgroundTintList = ColorStateList.valueOf(it.animatedValue as Int)
            }
            bgColorAnim.duration = 200
            bgColorAnim.start()
             val textColorAnim = ObjectAnimator.ofArgb(checkout_button, "textColor",
                                                       if (value) blackColor else darkGrayColor)
            textColorAnim.duration = 200
            textColorAnim.start()
            field = value
        }
    private var checkoutButtonIsHidden = false
        set(value) {
            if (checkoutButtonIsHidden == value) return
            if (cradleLayoutInitialWidth == -1) cradleLayoutInitialWidth = cradle_layout.width
            val cradleLayoutStartWidth = if (value) cradle_layout.width else fab.width
            val cradleLayoutEndWidth = if (value) fab.width else cradleLayoutInitialWidth
            val cradleLayoutWidthChange = cradleLayoutEndWidth - cradleLayoutStartWidth
            val anim = ValueAnimator.ofFloat(if (value) 1f else 0f,
                                             if (value) 0f else 1f)
            anim.addUpdateListener {
                checkout_button.scaleX = it.animatedValue as Float
                cradle_layout.layoutParams.width = cradleLayoutStartWidth +
                        (cradleLayoutWidthChange * it.animatedFraction).toInt()
                cradle_layout.requestLayout()
                bottom_app_bar.redrawCradle()
            }
            anim.start()
            field = value
        }
    private var checkoutButtonWidth = 0
    private var menu: Menu? = null
    lateinit var inventoryViewModel: InventoryViewModel
    lateinit var shoppingListViewModel: ShoppingListViewModel
    lateinit var fab: FloatingActionButton
    lateinit var checkoutButton: MaterialButton

    enum class FragmentId { ShoppingList, Inventory, Preferences }

    val shoppingListFragment = ShoppingListFragment()
    val inventoryFragment = InventoryFragment()
    val preferencesFragment = PreferencesFragment()

    var showingInventory = false
    var showingPreferences = false
//    }

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
        checkoutButtonWidth = checkout_button.width
        darkGrayColor = ContextCompat.getColor(this, R.color.colorTextLightSecondary)
        lightGrayColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        blackColor = ContextCompat.getColor(this, android.R.color.black)
        yellowColor = ContextCompat.getColor(this, R.color.checkoutButtonEnabledColor)
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
        this.menu = menu
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
            shoppingListFragment.enable()
            inventoryFragment.disable()
            supportFragmentManager.beginTransaction().
                setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right).
                hide(inventoryFragment).show(shoppingListFragment).commit()
        }
    }
}