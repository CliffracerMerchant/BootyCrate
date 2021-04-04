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
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnNextLayout

/**
 * A styled subclass of MainActivity.
 *
 * Unfortunately many of the desired aspects of MainActivity's style (e.g. the
 * menu item icons being tinted to match the gradient background of the top
 * and bottom action bar) are impossible to accomplish in XML. GradientStyled-
 * MainActivity performs additional operations to initialize its style. Its
 * foreground gradient is made by creating a radial gradient using the values
 * of the XML attributes colorAccent, colorInBetweenPrimaryAccent, and color-
 * Primary, setting the alpha component of this gradient to the value of the
 * attribute foregroundGradientBlendAlpha, and then overlaying it onto a solid
 * background with a color equal to the value of the attribute foregroundGrad-
 * ientBlendColor. The same process is repeated for the background gradient
 * using the values of backgroundGradientBlendAlpha and backgroundGradient-
 * BlendColor.
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

    private val fgColors = IntArray(3)
    private val bgColors = IntArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setGradientColors()
        initGradients()
    }

    private fun setGradientColors() {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        fgColors[0] = typedValue.data
        theme.resolveAttribute(R.attr.colorInBetweenPrimaryAccent, typedValue, true)
        fgColors[1] = typedValue.data
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        fgColors[2] = typedValue.data

        theme.resolveAttribute(R.attr.backgroundGradientBlendAlpha, typedValue, true)
        val bgBlendAlpha = typedValue.data
        theme.resolveAttribute(R.attr.foregroundGradientBlendAlpha, typedValue, true)
        val fgBlendAlpha = typedValue.data
        theme.resolveAttribute(R.attr.backgroundGradientBlendColor, typedValue, true)
        val bgBlendColor = typedValue.data
        theme.resolveAttribute(R.attr.foregroundGradientBlendColor, typedValue, true)
        val fgBlendColor = typedValue.data
        for (i in fgColors.indices) {
            var colorWithTransparency = ColorUtils.setAlphaComponent(fgColors[i], bgBlendAlpha)
            bgColors[i] = ColorUtils.compositeColors(colorWithTransparency, bgBlendColor)
            colorWithTransparency = ColorUtils.setAlphaComponent(fgColors[i], fgBlendAlpha)
            fgColors[i] = ColorUtils.compositeColors(colorWithTransparency, fgBlendColor)
        }
    }

    private fun initGradients() {
        val screenWidth = resources.displayMetrics.widthPixels
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
        val actionBarHeight = typedValue.getDimension(resources.displayMetrics)

        topFgGradientBuilder.setX1(screenWidth / 2f).setY1(actionBarHeight)
                            .setX2(screenWidth * 0.85f).setY2(actionBarHeight * 1.5f)
        topBgGradientBuilder = topFgGradientBuilder.copy()
        bottomFgGradientBuilder = topFgGradientBuilder.copy(y1 = 0f)
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

        styleActionBar(topFgGradientBitmap)
        styleBottomAppBar(screenWidth, bottomFgGradientBitmap)
    }

    private fun styleActionBar(topFgGradientBitmap: Bitmap) {
        if (!ui.actionBar.isLaidOut)
            ui.actionBar.doOnNextLayout { styleActionBar(topFgGradientBitmap) }
        ui.actionBar.backgroundGradient = topBgGradient
        ui.actionBar.borderGradient = topFgGradient

        // For some reason, using getPixelAtCenter with the backButton does
        // not work here, even though it should be laid out by this point.
        val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ui.actionBar.ui.backButton.measure(wrapContent, wrapContent)
        ui.actionBar.ui.backButton.drawable.setTint(
            topFgGradientBitmap.getPixel(ui.actionBar.ui.backButton.measuredWidth / 2,
                                         ui.actionBar.height / 2))
        ui.actionBar.ui.titleSwitcher.setShader(topFgGradient)
        ui.actionBar.ui.searchButton.drawable?.setTint(
            topFgGradientBitmap.getPixelAtCenter(ui.actionBar.ui.searchButton))
        ui.actionBar.ui.changeSortButton.drawable.setTint(
            topFgGradientBitmap.getPixelAtCenter(ui.actionBar.ui.changeSortButton))
        ui.actionBar.ui.menuButton.drawable.setTint(
            topFgGradientBitmap.getPixelAtCenter(ui.actionBar.ui.menuButton))
    }

    private fun styleBottomAppBar(screenWidth: Int, bottomFgGradientBitmap: Bitmap) {
        if (!ui.bottomAppBar.isLaidOut)
            ui.bottomAppBar.doOnNextLayout { styleBottomAppBar(screenWidth, bottomFgGradientBitmap) }
        ui.bottomAppBar.backgroundGradient = bottomBgGradient
        ui.bottomAppBar.borderGradient = bottomFgGradient
        ui.bottomAppBar.indicatorGradient = bottomFgGradient

        // Checkout button
        val rect = Rect()
        ui.checkoutButton.getGlobalVisibleRect(rect)
        ui.checkoutButton.outlineGradient = bottomFgGradientBuilder
            .copy(x1 = ui.checkoutButton.width * 3f / 4f).buildRadialGradient()
        ui.checkoutButton.backgroundGradient = bottomBgGradientBuilder
            .copy(x1 = ui.checkoutButton.width * 3f / 4f).buildRadialGradient()

        // Add button
        ui.addButton.outlineGradient = bottomFgGradientBuilder.copy(
            x1 = ui.addButton.width / 2f, y1 = ui.addButton.height / 4f,
            x2 = bottomFgGradientBuilder.y2 * 0.75f,
            y2 = bottomFgGradientBuilder.y2 * 0.75f
        ).buildRadialGradient()
        ui.addButton.backgroundGradient = bottomBgGradientBuilder.copy(
            x1 = ui.addButton.width / 2f, y1 = ui.addButton.height / 4f
        ).buildRadialGradient()

        // BottomNavigationView active colors
        val shoppingListButton = ui.bottomNavigationBar.findViewById<View>(R.id.shopping_list_button)
        val activeColor = bottomFgGradientBitmap.getPixelAtCenter(shoppingListButton)
        ui.bottomNavigationBar.itemIconTintList = ColorStateList.valueOf(activeColor)
        ui.bottomNavigationBar.itemTextColor = ui.bottomNavigationBar.itemIconTintList
    }

    fun ActionBarTitle.setShader(shader: Shader?) {
        titleView.paint.shader = shader
        actionModeTitleView.paint.shader = shader
        searchQueryView.paint.shader = shader
        (searchQueryView.background as? GradientVectorDrawable)?.gradient = shader
    }
}