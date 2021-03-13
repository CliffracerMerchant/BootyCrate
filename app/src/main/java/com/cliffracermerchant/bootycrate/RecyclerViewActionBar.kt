/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.*
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.cliffracermerchant.bootycrate.databinding.RecyclerViewActionBarBinding

/**
 * A toolbar tailored towards interacting with a recycler view.
 *
 * RecyclerViewActionBar acts as an entirely custom (i.e. it eschews the
 * Android setSupportActionBar API in favor of its own) action bar with an
 * interface tailored towards activities or fragments that primarily show a
 * recycler view. Through its binding property ui, the ui elements available
 * are:
 *     - backButton, similar to the home as up indicator, hidden by default
 *     - customTitle, a TextSwitcher that can be used as an activity or frag-
 *       ment title, or an action mode title. The attribute android.R.attr.-
 *       text is used as the default text for the title.
 *     - searchView, a SearchView
 *     - changeSortButton, a button whose default on click action opens the
 *       changeSortMenu, but can also have isActivated set to true to change
 *       to a delete icon and call the property onDeleteButtonClickedListener
 *       instead.
 *     - menuButton, which opens the optionsMenu member.
 * The contents of the changeSortMenu and the optionsMenu can be set in XML
 * with the attributes R.attr.changeSortMenuResId and R.attr.optionsMenuResId.
 * The callbacks for the menu items being clicked can be set through the func-
 * tions setOnSortOptionClickedListener and setOnOptionsItemClickedListener.
 * If the default Android action bar menu item callback functionality (every
 * click being routed through onOptionsItemSelected) is desired, the functions
 * can be passed a lambda that manually calls onOptionsItemSelected for the
 * activity or fragment being used.
 */
@Suppress("LeakingThis")
open class RecyclerViewActionBar(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs)
{
    val ui = RecyclerViewActionBarBinding.inflate(LayoutInflater.from(context), this)

    var onDeleteButtonClickedListener: (() -> Unit)? = null
    fun setOnSortOptionClickedListener(listener: (MenuItem) -> Boolean) =
        changeSortPopupMenu.setOnMenuItemClickListener(listener)
    fun setOnOptionsItemClickedListener(listener: (MenuItem) -> Boolean) =
        optionsPopupMenu.setOnMenuItemClickListener(listener)

    private val changeSortPopupMenu = PopupMenu(context, ui.changeSortButton)
    private val optionsPopupMenu = PopupMenu(context, ui.menuButton)
    val changeSortMenu get() = changeSortPopupMenu.menu
    val optionsMenu get() = optionsPopupMenu.menu
    var optionsMenuVisible: Boolean = true
        set(value) { field = value
                     ui.searchView.isVisible = value
                     ui.changeSortButton.isVisible = value
                     ui.menuButton.isVisible = value }


    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerViewActionBar)
        val changeSortMenuResId = a.getResourceId(R.styleable.RecyclerViewActionBar_changeSortMenuResId, 0)
        val optionsMenuResId = a.getResourceId(R.styleable.RecyclerViewActionBar_optionsMenuResId, 0)
        a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text))
        ui.customTitle.setCurrentText(a.getString(0))
        a.recycle()
        changeSortPopupMenu.menuInflater.inflate(changeSortMenuResId, changeSortMenu)
        optionsPopupMenu.menuInflater.inflate(optionsMenuResId, optionsMenu)

        layoutTransition = layoutTransition(AnimatorConfig.translation)
        ui.changeSortButton.setOnClickListener {
            if (!ui.changeSortButton.isActivated) changeSortPopupMenu.show()
            else onDeleteButtonClickedListener?.invoke()
        }
        ui.menuButton.setOnClickListener{ optionsPopupMenu.show() }
    }

    /**
     * An reimplementation of ActionMode that uses an instance of RecyclerViewActionBar.
     *
     * RecyclerViewActionBar.ActionMode is intended to be a replacement for an
     * Android ActionMode that reuses a RecyclerViewActionBar instead of over-
     * laying the support action bar. It requires an instance of RecyclerView-
     * ActionBar, which can be passed in during the constructor, or set through
     * the property actionBar. The property is nullable and public so that frag-
     * ments that use a RecyclerViewActionMode can null the property during
     * their onDestroyView.
     *
     * Once the property actionBar is set, calling start will start the action
     * mode, display the back button on the action bar, switch the action bar's
     * changeSortButton to a delete icon, and switch the title to the action
     * mode's title, accessed through the property title. The function finish can
     * be called when desired to end the ActionMode and switch the custom title
     * back to its original text.
     *
     * Sub-classes should override onStart and onFinish to make the desired
     * changes to the action bar UI while the action mode is started. Because the
     * action bar's original options menu is used, the implementing activity or
     * fragment will have to respond to action item clicks in the action bar's
     * onOptionsItemSelectedListener.
     *
     * Due to the fact that the same action bar can be used for multiple Recycler-
     * ViewActionModes, and because the actionBar backButton can be used for mul-
     * tiple purpose, RecyclerViewActionBar.ActionMode does not set the onClick-
     * Listener of the action bar's backButton despite making it visible. It is
     * up to the implementing activity or fragment to make the backButton finish
     * the action mode.
     */
    open class ActionMode(actionBar: RecyclerViewActionBar? = null) {
        var actionBar: RecyclerViewActionBar? = null
        val isStarted get() = _isStarted
        var title: String? = null
            set(value) { field = value
                if (_isStarted) actionBar?.ui?.customTitle?.setCurrentText(title) }

        private var _isStarted = false
        private var titleBackup: String? = null

        init { this.actionBar = actionBar }

        fun start() = startOrFinish(starting = true)

        fun finish() = startOrFinish(starting = false)

        private fun startOrFinish(starting: Boolean) {
            if (_isStarted == starting) return
            val actionBar = actionBar ?: return
            _isStarted = starting
            actionBar.ui.backButton.isVisible = starting
            if (starting) {
                titleBackup = actionBar.ui.customTitle.text
                actionBar.ui.customTitle.setText(title)
                actionBar.ui.backButton.alpha = 0f
                actionBar.ui.backButton.isVisible = true
                //actionBar.ui.backButton.animate().alpha(1f).withLayer().start()
                onStart(actionBar)
            } else {
                actionBar.ui.customTitle.setText(titleBackup)
                titleBackup = null
                actionBar.ui.backButton.isVisible = false
                onFinish(actionBar)
            }
        }

        open fun onStart(actionBar: RecyclerViewActionBar) { }
        open fun onFinish(actionBar: RecyclerViewActionBar) { }
    }
}

/**
 * A RecyclerViewActionBar that has a bottom border and allows setting a gradient as a background and / or border.
 *
 * GradientActionBar acts as an RecyclerViewActionBar, except that a gradient
 * (in the form of a Shader) can be set as the background or as the paint to
 * use for its border. Setting a gradient background this way (as opposed to,
 * e.g. a ShapeDrawable with a gradient fill) allows for more customization
 * (e.g. a radial gradient with different x and y radii).
 *
 * The border width is derived from the attr borderWidth. The background and
 * border gradients can be set independently of each other through the prop-
 * erties backgroundGradient and borderGradient.
 */
class GradientActionBar(context: Context, attrs: AttributeSet) : RecyclerViewActionBar(context, attrs) {
    private val backgroundDrawable: GradientVectorDrawable
    private val borderDrawable: GradientVectorDrawable

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
        ui.searchView.setOnSearchClickListener {
            ui.backButton.isVisible = true
            ui.customTitle.isVisible = false
        }
        ui.searchView.setOnCloseListener {
            ui.backButton.isVisible = false
            ui.customTitle.isVisible = true
            false
        }
    }
}

/**
 * A preconfigured TextSwitcher that comes with two text views and a property that allows setting a paint shader for both TextViews at once.
 *
 * ShaderTextSwitcher is a TextSwitcher that adds two TextViews, passes xml
 * attributes on to these text views, preconfigures short fade in and out anim-
 * ations, and allows setting a Shader object to the shader property. Setting
 * this property will apply the shader to both member TextViews. If the Shader-
 * TextSwitcher is moved during a layout, the shader will be offset to create
 * the appearance that the shader is set in global coordinates, and the Text-
 * Views are moving relative to it, instead of the shader moving with the Text-
 * Views.
 */
class ShaderTextSwitcher(context: Context, attrs: AttributeSet) : TextSwitcher(context, attrs) {
    val text get() = (currentView as TextView).text.toString()
    var shader: Shader? get() = (currentView as TextView).paint.shader
        set(value) { val textView = currentView as TextView
                     val textTop = top.toFloat() + textView.baseline +
                                   textView.paint.fontMetrics.top
                     value?.translateBy(left * -1f, -textTop)
                     (currentView as TextView).paint.shader = value
                     (nextView as TextView).paint.shader = value }

    init {
        addView(TextView(context, attrs))
        addView(TextView(context, attrs))
        setInAnimation(context, R.anim.fade_in)
        setOutAnimation(context, R.anim.fade_out)

        val a = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.fontFamily))
        val fontId = a.getResourceId(0, 0)
        a.recycle()
        val font = ResourcesCompat.getFont(context, fontId)
        (currentView as TextView).typeface = font
        (nextView as TextView).typeface = font

        addOnLayoutChangeListener { _, left, top, _, _, _, _, _, _ ->
            val textView = currentView as TextView
            if (textView.paint.shader == null) return@addOnLayoutChangeListener
            val textTop = top.toFloat() + textView.baseline + textView.paint.fontMetrics.top
            textView.paint.shader.translateBy(left * -1f, -textTop)
        }
    }
}