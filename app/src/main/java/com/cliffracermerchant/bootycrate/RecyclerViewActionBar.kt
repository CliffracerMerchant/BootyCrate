/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.cliffracermerchant.bootycrate.databinding.RecyclerViewActionBarBinding

/** A toolbar tailored towards interacting with a recycler view.
 *
 *  RecyclerViewActionBar acts as an entirely custom (i.e. it eschews the
 *  Android setSupportActionBar API in favor of its own) action bar with an
 *  interface tailored towards activities or fragments that primarily show a
 *  recycler view. Through its binding property ui, the ui elements available
 *  are:
 *      - backButton, similar to the home as up indicator, hidden by default
 *      - customTitle, a TextSwitcher that can be used as an activity or frag-
 *        ment title, or an action mode title. The attribute android.R.attr.-
 *        text is used as the default text for the title.
 *      - searchView, a SearchView
 *      - changeSortButton, a button whose default on click action opens the
 *        changeSortMenu, but can also have isActivated set to true to change
 *        to a delete icon and call the property onDeleteButtonClickedListener
 *        instead.
 *      - menuButton, which opens the optionsMenu member.
 *  The contents of the changeSortMenu and the optionsMenu can be set in XML
 *  with the attributes R.attr.changeSortMenuResId and R.attr.optionsMenuResId.
 *  The callbacks for the menu items being clicked can be set through the func-
 *  tions setOnSortOptionClickedListener and setOnOptionsItemClickedListener.
 *  If the default Android action bar menu item callback functionality (every
 *  click being routed through onOptionsItemSelected) is desired, the functions
 *  can be passed a lambda that manually calls onOptionsItemSelected for the
 *  activity or fragment being used. */
open class RecyclerViewActionBar(context: Context, attrs: AttributeSet) :
        ConstraintLayout(context, attrs) {
    val ui = RecyclerViewActionBarBinding.inflate(LayoutInflater.from(context), this)

    var onDeleteButtonClickedListener: (() -> Unit)? = null
    fun setOnSortOptionClickedListener(listener: (MenuItem) -> Boolean) =
        changeSortPopupMenu.setOnMenuItemClickListener(listener)
    fun setOnOptionsItemClickedListener(listener: (MenuItem) -> Boolean) =
        optionsPopupMenu.setOnMenuItemClickListener(listener)

    private val optionsPopupMenu = PopupMenu(context, ui.menuButton)
    private val changeSortPopupMenu = PopupMenu(context, ui.changeSortButton)
    val optionsMenu get() = optionsPopupMenu.menu
    val changeSortMenu get() = changeSortPopupMenu.menu

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerViewActionBar)
        val changeSortMenuResId = a.getResourceId(R.styleable.RecyclerViewActionBar_changeSortMenuResId, 0)
        val optionsMenuResId = a.getResourceId(R.styleable.RecyclerViewActionBar_optionsMenuResId, 0)
        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text))
        ui.customTitle.setCurrentText(a.getString(0))
        a.recycle()
        changeSortPopupMenu.menuInflater.inflate(changeSortMenuResId, changeSortMenu)
        optionsPopupMenu.menuInflater.inflate(optionsMenuResId, optionsMenu)

        layoutTransition = defaultLayoutTransition()
        ui.changeSortButton.setOnClickListener {
            if (!ui.changeSortButton.isActivated) changeSortPopupMenu.show()
            else onDeleteButtonClickedListener?.invoke()
        }
        ui.menuButton.setOnClickListener{ optionsPopupMenu.show() }
    }
}

/** A RecyclerViewActionBar that has a bottom border and allows setting a gradient as a background and / or border.
 *
 *  GradientActionBar acts as an RecyclerViewActionBar, except that a gradient
 *  (in the form of a Shader) can be set as the background or as the paint to
 *  use for its border. Setting a gradient background this way (as opposed to,
 *  e.g. a ShapeDrawable with a gradient fill) allows for more customization
 *  (e.g. a radial gradient with different x and y radii).
 *
 *  The border width is derived from the attr borderWidth. The background and
 *  border gradients can be set independently of each other through the prop-
 *  erties backgroundGradient and borderGradient. */
class GradientActionBar(context: Context, attrs: AttributeSet) : RecyclerViewActionBar(context, attrs) {
    val backgroundDrawable: GradientVectorDrawable
    val borderDrawable: GradientVectorDrawable

    var backgroundGradient: Shader? = null
        set(value) { field = value; backgroundDrawable.gradient = value }
    var borderGradient: Shader? = null
        set(value) { field = value; borderDrawable.gradient = value }

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.GradientActionBar)
        val borderWidth = a.getDimension(R.styleable.GradientActionBar_bottomBorderWidth, 0f)
        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.layout_height))
        val height = a.getDimensionPixelSize(0, 0)
        a.recycle()

        val width = context.resources.displayMetrics.widthPixels
        val backgroundPathData = "L $width,0 L $width,$height L 0,$height Z"
        backgroundDrawable = GradientVectorDrawable(width * 1f, height * 1f, backgroundPathData)

        val borderPathData = "M 0,${height - borderWidth / 2} L $width,${height - borderWidth / 2}"
        borderDrawable = GradientVectorDrawable(width * 1f, height * 1f, borderPathData)
        borderDrawable.style = Paint.Style.STROKE
        borderDrawable.strokeWidth = borderWidth

        backgroundDrawable.gradient = backgroundGradient
        borderDrawable.gradient = borderGradient
        background = LayerDrawable(arrayOf(backgroundDrawable, borderDrawable))
    }
}

/** A TextSwitcher that comes with two text views, passes xml attributes on to these text views,
 * and preconfigures short fade in and out animations.*/
class PrefilledTextSwitcher(context: Context, attrs: AttributeSet) : TextSwitcher(context, attrs) {
    val text get() = (currentView as TextView).text.toString()
    init {
        addView(TextView(context, attrs))
        addView(TextView(context, attrs))
        setInAnimation(context, R.anim.short_fade_in)
        setOutAnimation(context, R.anim.short_fade_out)

        val a = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.fontFamily))
        val fontId = a.getResourceId(0, 0)
        a.recycle()
        val font = ResourcesCompat.getFont(context, fontId)
        (currentView as TextView).typeface = font
        (nextView as TextView).typeface = font
    }
}