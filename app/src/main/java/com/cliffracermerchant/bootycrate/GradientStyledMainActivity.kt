/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomnavigation.BottomNavigationItemView

/**
 * A styled subclass of MainActivity.
 *
 * Unfortunately many of the desired aspects of MainActivity's style (e.g.
 * the menu item icons being tinted to match the gradient background of
 * the top and bottom action bar) are impossible to accomplish in XML.
 * GradientStyledMainActivity performs additional operations to initialize
 * its style. Its foreground gradient is made by creating a linear gradi-
 * ent using the values of the XML attributes foregroundGradientColorLeft,
 * foregroundGradientColorMiddle, and foregroundGradientColorRight. The
 * background gradient is made from the colors colorAccent, colorInBetween-
 * PrimaryAccent, and colorPrimary. It is assumed that
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
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_gradient))
        initGradients()
    }

    private fun initGradients() {
        val screenWidth = resources.displayMetrics.widthPixels
        val actionBarHeight = theme.resolveIntAttribute(R.attr.actionBarSize).toFloat()

        val fgColors = intArrayOf(theme.resolveIntAttribute(R.attr.foregroundGradientColorLeft),
                                  theme.resolveIntAttribute(R.attr.foregroundGradientColorMiddle),
                                  theme.resolveIntAttribute(R.attr.foregroundGradientColorRight))
        val bgColors = intArrayOf(theme.resolveIntAttribute(R.attr.colorAccent),
                                  theme.resolveIntAttribute(R.attr.colorInBetweenPrimaryAccent),
                                  theme.resolveIntAttribute(R.attr.colorPrimary))

        val fgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val bgGradientBitmap = Bitmap.createBitmap(screenWidth, actionBarHeight.toInt(), Bitmap.Config.ARGB_8888)
        val paint = Paint()
        paint.style = Paint.Style.FILL

        val fgGradientBuilder = GradientBuilder(x2 = screenWidth.toFloat(), colors = fgColors)
        val fgGradientShader = fgGradientBuilder.buildLinearGradient()
        paint.shader = fgGradientShader
        val canvas = Canvas(fgGradientBitmap)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        val bgGradientBuilder = fgGradientBuilder.copy(colors  = bgColors)
        val bgGradientShader = bgGradientBuilder.buildLinearGradient()
        paint.shader = bgGradientShader
        canvas.setBitmap(bgGradientBitmap)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), actionBarHeight, paint)

        styleActionBarContents(fgGradientShader, fgGradientBitmap)
        ui.bottomAppBar.backgroundPaint.shader = bgGradientShader
        ui.bottomAppBar.indicatorPaint.shader = fgGradientShader
        styleBottomAppBar(screenWidth, fgGradientBitmap, fgGradientBuilder, bgGradientBuilder)
    }

    private fun styleActionBarContents(fgGradientShader: Shader, fgGradientBitmap: Bitmap) {
        val buttonWidth = ui.actionBar.ui.backButton.drawable.intrinsicWidth
        var x = buttonWidth / 2
        ui.actionBar.ui.backButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
        ui.actionBar.ui.titleSwitcher.setShader(fgGradientShader)

        x = resources.displayMetrics.widthPixels - buttonWidth / 2
        ui.actionBar.ui.menuButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
        x -= buttonWidth
        ui.actionBar.ui.changeSortButton.drawable.setTint(fgGradientBitmap.getPixel(x, 0))
        x -= buttonWidth
        ui.actionBar.ui.searchButton.drawable?.setTint(fgGradientBitmap.getPixel(x, 0))
    }
    val handler = Handler(Looper.getMainLooper())
    private fun styleBottomAppBar(screenWidth: Int, fgGradientBitmap: Bitmap,
                                  fgGradientBuilder: GradientBuilder,
                                  bgGradientBuilder: GradientBuilder) {
        // Checkout button
        val wrapContent = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ui.cradleLayout.measure(wrapContent, wrapContent)
        val cradleWidth = ui.cradleLayout.measuredWidth
        val cradleLeft = (screenWidth - cradleWidth) / 2f
        ui.checkoutButton.foregroundGradient = fgGradientBuilder
                .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()
        ui.checkoutButton.backgroundGradient = bgGradientBuilder
                .setX1(-cradleLeft).setX2(screenWidth - cradleLeft).buildLinearGradient()

        // Add button
        val addButtonWidth = ui.addButton.layoutParams.width
        val addButtonLeft = cradleLeft + cradleWidth - addButtonWidth * 1f
        ui.addButton.foregroundGradient = fgGradientBuilder
                .setX1(-addButtonLeft).setX2(screenWidth - addButtonLeft).buildLinearGradient()
        ui.addButton.backgroundGradient = bgGradientBuilder
                .setX1(-addButtonLeft).setX2(screenWidth - addButtonLeft).buildLinearGradient()

        val menuSize = ui.bottomNavigationBar.menu.size()
        for (i in 0 until menuSize) {
            val center = ((i + 0.5f) / menuSize * screenWidth).toInt()
            val tint = ColorStateList.valueOf(fgGradientBitmap.getPixel(center, 0))
            ui.bottomNavigationBar.getIconAt(i).imageTintList = tint
            ui.bottomNavigationBar.setTextTintList(i, tint)
        }
        ui.bottomNavigationBar.invalidate()
    }

    private fun ActionBarTitle.setShader(shader: Shader?) {
        titleView.paint.shader = shader
        actionModeTitleView.paint.shader = shader
        searchQueryView.paint.shader = shader
        (searchQueryView.background as? GradientVectorDrawable)?.gradient = shader
    }
}