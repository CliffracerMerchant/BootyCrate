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

/**
 * A styled subclass of MainActivity.
 *
 * Unfortunately many of the desired aspects of MainActivity's style (e.g. the
 * menu item icons being tinted to match the gradient background of the top
 * and bottom action bar) are impossible to accomplish in XML. GradientStyled-
 * MainActivity performs additional operations to initialize its style.
 */
class GradientStyledMainActivity : MainActivity() {
    private val topFgGradientBuilder = GradientBuilder()
    private lateinit var topBgGradientBuilder: GradientBuilder
    private lateinit var bottomFgGradientBuilder: GradientBuilder
    private lateinit var bottomBgGradientBuilder: GradientBuilder

    private var topFgGradient: Shader? = null
    private var topBgGradient: Shader? = null
    private var bottomFgGradient: Shader? = null
    private var bottomBgGradient: Shader? = null

    private val fgColors = IntArray(4)
    private val bgColors = IntArray(4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setGradientColors()
        initGradients()
    }

    private fun setGradientColors() {
        fgColors[0] = ContextCompat.getColor(this, R.color.colorPrimary)
        fgColors[1] = ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent1)
        fgColors[2] = ContextCompat.getColor(this, R.color.colorInBetweenPrimaryAccent2)
        fgColors[3] = ContextCompat.getColor(this, R.color.colorAccent)

        // Colors are more easily visible on dark backgrounds compared to light ones,
        // so the alpha value is lower when a dark theme is used and higher when a
        // light theme is used. This will make the background gradient visible on
        // both types of themes without being overpowering.
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.isDarkTheme, typedValue, true)
        val usingDarkTheme = typedValue.data == -1
        val blendAlpha = if (usingDarkTheme) 100 else 150
        theme.resolveAttribute(R.attr.recyclerViewItemColor, typedValue, true)
        val blendColor = typedValue.data
        for (i in bgColors.indices)
            bgColors[i] = ColorUtils.compositeColors(ColorUtils.setAlphaComponent(fgColors[i], blendAlpha), blendColor)
    }

    private fun initGradients() {
        val screenWidth = resources.displayMetrics.widthPixels
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
        val actionBarHeight = typedValue.getDimension(resources.displayMetrics)

        topFgGradientBuilder.setX1(screenWidth / 2f).setY1(actionBarHeight)
                            .setX2(screenWidth * 0.8f).setY2(actionBarHeight * 1.25f)
        topBgGradientBuilder = topFgGradientBuilder.copy()
        bottomFgGradientBuilder = topFgGradientBuilder.copy(x1 = screenWidth / 2f, y1 = actionBarHeight * 0.25f)
        bottomBgGradientBuilder = bottomFgGradientBuilder.copy()

        topFgGradient = topFgGradientBuilder.setColors(fgColors).buildRadialGradient()
        topBgGradient = topBgGradientBuilder.setColors(bgColors).buildRadialGradient()
        bottomFgGradient = bottomFgGradientBuilder.setColors(fgColors).buildRadialGradient()
        bottomBgGradient = bottomBgGradientBuilder.setColors(bgColors).buildRadialGradient()

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

        ui.topActionBar.backgroundGradient = topBgGradient
        ui.topActionBar.borderGradient = topFgGradient

        ui.bottomAppBar.backgroundGradient = bottomBgGradient
        ui.bottomAppBar.borderGradient = bottomFgGradient
        ui.bottomAppBar.indicatorGradient = bottomFgGradient

        if (ui.coordinatorLayout.isLaidOut)
            initGradientsForTopAndBottomBarChildren(screenWidth, topFgGradientBitmap, bottomFgGradientBitmap)
        else ui.coordinatorLayout.doOnNextLayout {
            initGradientsForTopAndBottomBarChildren(screenWidth, topFgGradientBitmap, bottomFgGradientBitmap)
        }
    }

    private fun initGradientsForTopAndBottomBarChildren(
        screenWidth: Int,
        topFgGradientBitmap: Bitmap,
        bottomFgGradientBitmap: Bitmap
    ) {
        ui.addButton.outlineGradient = bottomFgGradientBuilder.copy()
            .setX1(ui.addButton.width / 2f).buildRadialGradient()
        ui.addButton.backgroundGradient = bottomBgGradientBuilder.copy()
            .setX1(ui.addButton.width / 2f).buildRadialGradient()

        val rect = Rect()
        ui.checkoutButton.getGlobalVisibleRect(rect)
        ui.checkoutButton.outlineGradient = bottomFgGradientBuilder.copy()
            .setX1(screenWidth / 2f - ui.bottomAppBar.cradleWidth / 2f).buildRadialGradient()
        ui.checkoutButton.backgroundGradient = bottomBgGradientBuilder.copy()
            .setX1(screenWidth / 2f - ui.bottomAppBar.cradleWidth / 2f).buildRadialGradient()

        // Back icon
        // For some reason, using getPixelAtCenter with the backButton does
        // not work here, even though it should be laid out by this point.
        val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ui.topActionBar.ui.backButton.measure(wrapContent, wrapContent)
        ui.topActionBar.ui.backButton.drawable.setTint(
            topFgGradientBitmap.getPixel(ui.topActionBar.ui.backButton.measuredWidth / 2,
                                         ui.topActionBar.height / 2))

        // Custom title
        // Because the custom title moves its shader around when it is moved,
        // and we don't want the top action bar's shader to be moved around
        // with it, the custom title's shader needs to be a separate instance.
        ui.topActionBar.ui.customTitle.shader = topFgGradientBuilder.buildRadialGradient()
        //ui.topActionBar.ui.customTitle.invalidate()

        // Search view
        val searchView = ui.topActionBar.ui.searchView
        val color = topFgGradientBitmap.getPixelAtCenter(searchView)
        var view = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        view?.drawable?.setTint(color)
        view = searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
        view?.drawable?.setTint(color)
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.paint.shader = ui.topActionBar.ui.customTitle.shader

        // Change sort icon
        ui.topActionBar.ui.changeSortButton.drawable.setTint(
            topFgGradientBitmap.getPixelAtCenter(ui.topActionBar.ui.changeSortButton))

        // Overflow icon
        ui.topActionBar.ui.menuButton.drawable.setTint(
            topFgGradientBitmap.getPixelAtCenter(ui.topActionBar.ui.menuButton))

        // BottomNavigationView active colors
        val shoppingListButton = ui.bottomNavigationBar.findViewById<View>(R.id.shopping_list_button)
        val inactiveColor = ui.bottomNavigationBar.itemIconTintList?.defaultColor ?: 0
        val activeColor = bottomFgGradientBitmap.getPixelAtCenter(shoppingListButton)
        val itemTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), IntArray(0)),
                    intArrayOf(activeColor, inactiveColor))
        ui.bottomNavigationBar.itemIconTintList = itemTintList
        ui.bottomNavigationBar.itemTextColor = itemTintList
    }
}