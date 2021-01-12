/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlinx.android.synthetic.main.activity_main.*

/** A styled subclass of MainActivity.
 *
 *  Unfortunately many of the desired aspects of MainActivity's style (e.g. the
 *  menu item icons being tinted to match the gradient background of the top
 *  and bottom action bar) are impossible to accomplish in XML. GradientStyled-
 *  MainActivity performs additional operations to initialize its style. */
class GradientStyledMainActivity : MainActivity() {
    private val handler = Handler()
    private var menuIconInitializationErrors = 0

    lateinit var topFgGradientBitmap: Bitmap
    lateinit var bottomFgGradientBitmap: Bitmap
    lateinit var topBgGradient: Shader
    lateinit var topFgGradient: Shader
    lateinit var bottomBgGradient: Shader
    lateinit var bottomFgGradient: Shader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGradients()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return if (super.onCreateOptionsMenu(menu)) {
            initOptionsMenuIcons()
            true
        }
        else false
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
        topFgGradient = gradientBuilder.setX1(screenWidth / 2f).setY1(actionBarHeight).
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

        bottomAppBar.backgroundGradient = bottomBgGradient
        bottomAppBar.borderGradient = bottomFgGradient
        bottomAppBar.indicatorGradient = bottomFgGradient
        gradientBuilder.setX1(addButton.width / 2f)
        addButton.initGradients(gradientBuilder.setColors(dimmedColors).buildRadialGradient(),
                                gradientBuilder.setColors(colors).buildRadialGradient())

        val rect = Rect()
        checkoutBtn.getGlobalVisibleRect(rect)
        gradientBuilder.setX1(screenWidth / 2f - bottomAppBar.cradleWidth / 2f)
        checkoutBtn.initGradients(gradientBuilder.setColors(dimmedColors).buildRadialGradient(),
                                  gradientBuilder.setColors(colors).buildRadialGradient())
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
            val rect = Rect()

            // Home as up icon
            ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_black_24dp)?.apply {
                setTint(topFgGradientBitmap.getPixel(topActionBar.height / 2, topActionBar.height / 2))
                supportActionBar?.setHomeAsUpIndicator(this)
                topActionBar.collapseIcon = this
            }

            // Change sort icon
            val changeSortButton = topActionBar.findViewById<View>(
                R.id.change_sorting_menu_item) ?: return@post
            changeSortButton.getDrawingRect(rect)
            var menuItem = menu.findItem(R.id.change_sorting_menu_item) ?: return@post
            var color = topFgGradientBitmap.getPixel(rect.centerX(), rect.centerY())
            menuItem.icon.setTint(color)

            // Search view
            menuItem = menu.findItem(R.id.app_bar_search) ?: return@post
            val searchView = menuItem.actionView as SearchView
            // For some reason (maybe because the search action view is collapsed?),
            // using the search button's position to get the color does not work.
            // Instead we'll use the changeSortButton's position and offset by its
            // width to approximate the searchButton's on screen position.
            color = topFgGradientBitmap.getPixel(rect.centerX() + changeSortButton.width, rect.centerY())
            menuItem.icon.setTint(color)
            val searchClose = searchView.findViewById<ImageView>(
                androidx.appcompat.R.id.search_close_btn) ?: return@post
            searchClose.drawable.setTint(color)
            val searchEditText = searchView.findViewById<EditText>(
                androidx.appcompat.R.id.search_src_text) ?: return@post
            val colors = intArrayOf(
                ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent1),
                ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent2))
            searchEditText.paint.shader = LinearGradient(0f, searchEditText.height.toFloat(),
                0f, 0f, colors, null, Shader.TileMode.CLAMP)

            // Delete icon
            // When the action mode is active, the delete button should appear
            // where the change sort button is normally, so we'll just use the
            // change sort button's center instead of the delete button's.
            menuItem = menu.findItem(R.id.delete_selected_menu_item) ?: return@post
            menuItem.icon.setTint(color)

            // Overflow icon
            // It's hard to get the view that holds the overflow icon, so we just approximate its center.
            color = topFgGradientBitmap.getPixel(
                topActionBar.width - topActionBar.height / 2, topActionBar.height / 2)
            topActionBar.overflowIcon?.setTint(color)

            // BottomNavigationView active colors
            val shoppingListButton = bottomNavigationBar.findViewById<View>(R.id.shopping_list_button)
            shoppingListButton.getDrawingRect(rect)
            val inactiveColor = bottomNavigationBar.itemIconTintList?.defaultColor ?: 0
            val activeColor = bottomFgGradientBitmap.getPixel(rect.centerX(), rect.centerY())
            val itemTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), IntArray(0)),
                intArrayOf(activeColor, inactiveColor))
            bottomNavigationBar.itemIconTintList = itemTintList
            bottomNavigationBar.itemTextColor = itemTintList

            finished = true
        }
        if (!finished && ++menuIconInitializationErrors < 3)
            handler.post { initOptionsMenuIcons() }
        else menuIconInitializationErrors = 0
    }
}