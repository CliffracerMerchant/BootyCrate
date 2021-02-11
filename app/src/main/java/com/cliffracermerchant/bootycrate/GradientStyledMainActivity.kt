/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnNextLayout
import kotlinx.android.synthetic.main.activity_main.*

/** A styled subclass of MainActivity.
 *
 *  Unfortunately many of the desired aspects of MainActivity's style (e.g. the
 *  menu item icons being tinted to match the gradient background of the top
 *  and bottom action bar) are impossible to accomplish in XML. GradientStyled-
 *  MainActivity performs additional operations to initialize its style. */
class GradientStyledMainActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenWidth = resources.displayMetrics.widthPixels
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
        val actionBarHeight = typedValue.getDimension(resources.displayMetrics)

        // Foreground colors
        var colors = IntArray(4)
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
        val alpha = if (usingDarkTheme) 125 else 150
        theme.resolveAttribute(R.attr.recyclerViewItemColor, typedValue, true)
        val blendColor = typedValue.data
        val dimmedColors = IntArray(4) { ColorUtils.compositeColors(ColorUtils.setAlphaComponent(colors[it], alpha), blendColor) }

        val gradientBuilder = GradientBuilder()
        val topFgGradient = gradientBuilder.setX1(screenWidth / 2f).setY1(actionBarHeight).
        setX2(screenWidth * 0.8f).setY2(actionBarHeight * 1.25f).
        setColors(colors).buildRadialGradient()
        val topBgGradient = gradientBuilder.setColors(dimmedColors).buildRadialGradient()
        val bottomBgGradient = gradientBuilder.setX1(screenWidth / 2f).setY1(actionBarHeight * 0.25f).buildRadialGradient()
        val bottomFgGradient = gradientBuilder.setColors(colors).buildRadialGradient()

        val topFgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val bottomFgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(topFgGradientBitmap)
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.shader = topFgGradient
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        canvas.setBitmap(bottomFgGradientBitmap)
        paint.shader = bottomFgGradient
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        topActionBar.backgroundGradient = topBgGradient
        topActionBar.borderGradient = topFgGradient
        topActionBar.ui.customTitle.shader = BitmapShader(topFgGradientBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        bottomAppBar.backgroundGradient = bottomBgGradient
        bottomAppBar.borderGradient = bottomFgGradient
        bottomAppBar.indicatorGradient = bottomFgGradient
        gradientBuilder.setX1(addButton.width / 2f)
        addButton.backgroundGradient = gradientBuilder.setColors(dimmedColors).buildRadialGradient()
        addButton.outlineGradient = gradientBuilder.setColors(colors).buildRadialGradient()

        coordinatorLayout.doOnNextLayout {
            val rect = Rect()
            checkoutButton.getGlobalVisibleRect(rect)
            gradientBuilder.setX1(screenWidth / 2f - bottomAppBar.cradleWidth / 2f)
            checkoutButton.backgroundGradient = gradientBuilder.setColors(dimmedColors).buildRadialGradient()
            checkoutButton.outlineGradient = gradientBuilder.setColors(colors).buildRadialGradient()

            // Back icon
            val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            topActionBar.ui.backButton.measure(wrapContent, wrapContent)
            topActionBar.ui.backButton.drawable.setTint(
                topFgGradientBitmap.getPixel(topActionBar.ui.backButton.measuredHeight / 2,
                                             topActionBar.height / 2))

            // Search view
            val searchView = topActionBar.ui.searchView
            val color = topFgGradientBitmap.getPixelAtCenter(searchView)
            var view = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
            view?.drawable?.setTint(color)
            view = searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
            view?.drawable?.setTint(color)
            val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            colors = intArrayOf(ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent1),
                                ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent2))
            searchEditText.paint.shader = LinearGradient(0f, searchEditText.height.toFloat(),
                                                         0f, 0f, colors, null, Shader.TileMode.CLAMP)

            // Change sort icon
            topActionBar.ui.changeSortButton.drawable.setTint(
                topFgGradientBitmap.getPixelAtCenter(topActionBar.ui.changeSortButton))

            // Overflow icon
            topActionBar.ui.menuButton.drawable.setTint(
                topFgGradientBitmap.getPixelAtCenter(topActionBar.ui.menuButton))

            // BottomNavigationView active colors
            val shoppingListButton = bottomNavigationBar.findViewById<View>(R.id.shopping_list_button)
            shoppingListButton.getDrawingRect(rect)
            val inactiveColor = bottomNavigationBar.itemIconTintList?.defaultColor ?: 0
            val activeColor = bottomFgGradientBitmap.getPixelAtCenter(shoppingListButton)
            val itemTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), IntArray(0)),
                intArrayOf(activeColor, inactiveColor))
            bottomNavigationBar.itemIconTintList = itemTintList
            bottomNavigationBar.itemTextColor = itemTintList
        }
    }
}