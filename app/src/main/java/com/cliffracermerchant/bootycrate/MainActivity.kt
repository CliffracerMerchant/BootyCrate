/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
class MainActivity : AppCompatActivity() {
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
    private var pendingBabAnim: Animator? = null
    private val handler = Handler()
    private var menuIconInitializationErrors = 0

    lateinit var shoppingListViewModel: ShoppingListViewModel
    lateinit var inventoryViewModel: InventoryViewModel
    lateinit var fab: RoundGradientButton
    lateinit var checkoutBtn: TintableForegroundButton
    lateinit var menu: Menu

    lateinit var topFgGradientBitmap: Bitmap
    lateinit var bottomFgGradientBitmap: Bitmap
    lateinit var topBgGradient: Shader
    lateinit var topFgGradient: Shader
    lateinit var bottomBgGradient: Shader
    lateinit var bottomFgGradient: Shader

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
        fab = floatingActionButton
        checkoutBtn = checkoutButton
        imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        initGradients()

        cradleLayout.layoutTransition = delaylessLayoutTransition()
        cradleLayout.layoutTransition.doOnStart { _, _, _, _ ->
            pendingBabAnim?.start()
            pendingBabAnim = null
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
        initOptionsMenuIcons()
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
                                    withEndAction { oldFragmentView.visibility = View.GONE }?.
                                    start()

        activeFragment.isActive = true
        val newFragmentView = activeFragment.view
        newFragmentView?.translationX = newFragmentTranslationStart
        newFragmentView?.visibility = View.VISIBLE
        newFragmentView?.animate()?.translationX(0f)?.setDuration(300)?.start()//withLayer()
    }

    private fun showBottomAppBar(show: Boolean = true) {
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val views = arrayOf<View>(bottomAppBar, fab, checkoutBtn)

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
        checkoutBtn.visibility = if (showing) View.VISIBLE else View.GONE

        val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        cradleLayout.measure(wrapContentSpec, wrapContentSpec)
        val cradleEndWidth = if (showing) cradleLayout.measuredWidth
                             else         fab.layoutParams.width

        if (!animate) {
            bottomAppBar.cradleWidth = cradleEndWidth
            return
        }
        // Settings the checkout button's clip bounds prevents the
        // right corners of the checkout button from sticking out
        // underneath the FAB during the show / hide animation.
        val checkoutBtnClipBounds = Rect(0, 0, 0, checkoutBtn.background.intrinsicHeight)
        ObjectAnimator.ofInt(bottomAppBar, "cradleWidth", cradleEndWidth).apply {
            interpolator = cradleLayout.layoutTransition.getInterpolator(LayoutTransition.CHANGE_APPEARING)
            duration = cradleLayout.layoutTransition.getDuration(LayoutTransition.CHANGE_APPEARING)
            addUpdateListener {
                checkoutBtnClipBounds.right = bottomAppBar.cradleWidth - fab.measuredWidth / 2
                checkoutBtn.clipBounds = checkoutBtnClipBounds
            }
            doOnEnd { checkoutBtn.clipBounds = null }
            // The anim is stored here and started in the cradle layout's
            // layoutTransition's transition listener's transitionStart override
            // so that the animation is synced with the layout transition.
            pendingBabAnim = this
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

    private fun initGradients() {
        val screenWidth = resources.displayMetrics.widthPixels
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
        val actionBarHeight = typedValue.getDimension(resources.displayMetrics)

        // Foreground colors
        val colors = IntArray(4)
        colors[0] = ContextCompat.getColor(this, R.color.colorPrimary)
        colors[1] = ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent1)
        colors[2] = ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent2)
        colors[3] = ContextCompat.getColor(this, R.color.colorAccent)

        // Background colors
        // Colors are more easily visible on dark backgrounds compared to light ones,
        // so the alpha value is lower when a dark theme is used and higher when a
        // light theme is used. This will make the background gradient visible on
        // both types of themes without being overpowering.
        theme.resolveAttribute(R.attr.isDarkTheme, typedValue, true)
        val usingDarkTheme = typedValue.data == -1
        val alpha = if (usingDarkTheme) 90 else 125
        theme.resolveAttribute(R.attr.recyclerViewItemColor, typedValue, true)
        val blendColor = ContextCompat.getColor(this, R.color.colorRecyclerViewItemDark)
        val dimmedColors = IntArray(4) { ColorUtils.compositeColors(ColorUtils.setAlphaComponent(colors[it], alpha), blendColor) }

        val gradientBuilder = GradientBuilder()
        topFgGradient = gradientBuilder.setX1(screenWidth / 2f).setY1(actionBarHeight * 1.25f).
                                        setX2(screenWidth * 0.9f).setY2(actionBarHeight * 1.5f).
                                        setColors(colors).buildRadialGradient()
        topBgGradient = gradientBuilder.setColors(dimmedColors).buildRadialGradient()
        bottomBgGradient = gradientBuilder.setX1(screenWidth / 2f).setY1(0f).buildRadialGradient()
        bottomFgGradient = gradientBuilder.setColors(colors).buildRadialGradient()

        topFgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        bottomFgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(topFgGradientBitmap)
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.shader = topFgGradient
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        canvas.setBitmap(bottomFgGradientBitmap)
        paint.shader = bottomFgGradient
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        topActionBar.initGradients(topBgGradient, topFgGradient)
        customTitle.paint.shader = topFgGradient

        bottomAppBar.setBackgroundGradient(bottomBgGradient)
        bottomAppBar.setBorderGradient(bottomFgGradient)
        bottomAppBar.setIndicatorGradient(bottomFgGradient)
        gradientBuilder.setX1(fab.width / 2f).setY1(fab.height / 2f)
        fab.setForegroundGradient(gradientBuilder.buildRadialGradient())
        fab.setBackgroundGradient(gradientBuilder.setColors(dimmedColors).buildRadialGradient())
    }

    private fun initOptionsMenuIcons() {
     /* Because the action bar's items are set to have instances of GradientVectorDrawable
        as icons, and because GradientVectorDrawable needs to know the on screen position
        of the view that uses it in order to offset the gradient used as a background by
        the opposite amount, the views that contain the menu items must be initialized
        when this work is done. Unfortunately this is sometimes not the case when onCreate-
        OptionsMenu, which calls this function, is called. According to this SO answer
        https://stackoverflow.com/a/33337827/9653167 this happens because the view init-
        ialization is added to the message queue, and consequently is not performed imm-
        ediately. Posting the following work to the message queue should result in it
        being performed after the menu view initialization is finished.

        EDIT: This method sometimes still does not work (only sometimes during activity
        recreations?). Using menuIconInitializationErrors as a counter, the initialization
        work is attempted up to three times, and is aborted thereafter to prevent the
        execution from getting stuck in a loop.*/
        var finished = false
        handler.post {
            // Home as up icon
            ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_black_24dp)?.apply {
                setTint(topFgGradientBitmap.getPixel(topActionBar.height / 2, topActionBar.height / 2))
                supportActionBar?.setHomeAsUpIndicator(this)
                topActionBar.collapseIcon = this
            }

            // Search view
            var menuItem = menu.findItem(R.id.app_bar_search)
            val searchView = menuItem.actionView as SearchView
            val searchEditText = searchView.findViewById<EditText>(
                androidx.appcompat.R.id.search_src_text) ?: return@post
            val colors = intArrayOf(ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent1),
                                    ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent2))
            searchEditText.paint.shader = LinearGradient(0f, searchEditText.height.toFloat(),
                                                         0f, 0f, colors, null, Shader.TileMode.CLAMP)

            val searchButton = topActionBar.findViewById<View>(R.id.app_bar_search) ?: return@post
            menuItem.icon.setTint(topFgGradientBitmap.getPixelAtViewCenter(searchButton))

            val searchClose = searchView.findViewById<ImageView>(
                androidx.appcompat.R.id.search_close_btn) ?: return@post
            searchClose.drawable.setTint(topFgGradientBitmap.getPixelAtViewCenter(searchButton))


            // Change sort icon
            val changeSortButton = topActionBar.findViewById<View>(
                R.id.change_sorting_menu_item) ?: return@post
            menuItem = menu.findItem(R.id.change_sorting_menu_item)
            menuItem.icon.setTint(topFgGradientBitmap.getPixelAtViewCenter(changeSortButton))


            // Delete icon
            // When the action mode is active, the delete button should
            // appear where the change sort button is normally, so we'll
            // just use the change sort button as the parent.
            menuItem = menu.findItem(R.id.delete_selected_menu_item)
            menuItem.icon.setTint(topFgGradientBitmap.getPixelAtViewCenter(changeSortButton))


            // Overflow icon
            // It's hard to get the view that holds the overflow icon, so we just
            // shift the change_sorting_menu_item rect over by its width, which
            // should give a decent approximation of the overflow icon's position.
            topActionBar.overflowIcon?.setTint(topFgGradientBitmap.getPixel(
                topActionBar.width - topActionBar.height / 2, topActionBar.height / 2))

            finished = true
        }
        if (!finished && ++menuIconInitializationErrors < 3)
            handler.post { initOptionsMenuIcons() }
        else menuIconInitializationErrors = 0
    }
}
