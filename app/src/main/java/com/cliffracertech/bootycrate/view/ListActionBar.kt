/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.ListActionBarBinding
import com.cliffracertech.bootycrate.utils.*
import com.cliffracertech.bootycrate.viewmodel.*

/**
 * A toolbar tailored towards interacting with a list of items.
 *
 * ListActionBar acts as an entirely custom (i.e. it eschews the Android
 * setSupportActionBar API in favor of its own) action bar with an interface
 * tailored towards activities or fragments that primarily show a list of items.
 * Through its binding property ui, the UI elements available are:
 *     - backButton, similar to the home as up indicator. The backButton's
 *       visibility is set through the method setBackButtonIsVisible.
 *     - titleSwitcher, an ActionBarTitle that is used as an activity or
 *       fragment title, an action mode title, or a search query entry. The
 *       value of the theme attribute actionBarTitleStyle is used as the style
 *       for the title switcher. The state of the titleSwitcher is set with the
 *       method setTitleState with an instance of TitleState.
 *     - searchButton, a button whose icon can morph between a search icon and
 *       a close icon. The state of the search button is set with the method
 *       setSearchButtonState with an instance of SearchButtonState.
 *     - changeSortButton, a button that opens the changeSortMenu, but can also
 *       morph to a delete icon and call the property onDeleteButtonClickedListener
 *       instead. The state of the changeSortButton is set with the method
 *       setChangeSortButtonState with an instance of ChangeSortButtonState.
 *     - menuButton, which opens the optionsMenu member. The visibility of the
 *       menuButton is set with the method set setMenuButtonVisible.
 *
 * Clicks on the backButton, searchButton, and the delete button (i.e. the
 * changeSort button when its state is set to ChangeSortButtonState.MorphedToDelete)
 * can be listened to through the callbacks onBackButtonClick, onSearchButtonClick,
 * and onDeleteButtonClick, respectively. Changes in the search query entry can
 * be listened to through by setting the property onSearchQueryChange.
 *
 * The contents of the changeSortMenu and the optionsMenu can be set in
 * XML with the attributes R.attr.changeSortMenuResId and R.attr.optionsMenuResId.
 * The callbacks for the menu items being clicked can be set through the
 * functions setOnSortOptionClickedListener and setOnOptionsItemClickedListener.
 * If the default Android action bar menu item callback functionality (every
 * click being routed through onOptionsItemSelected) is desired, the functions
 * can be passed a lambda that manually calls onOptionsItemSelected for the
 * activity or fragment being used.
 *
 * The duration and interpolators of the buttons' appearance/disappearance
 * animations can be set through the property animatorConfig.
 */
@Suppress("LeakingThis")
open class ListActionBar(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs)
{
    private val ui = ListActionBarBinding.inflate(LayoutInflater.from(context), this)
    private val backButtonHiddenTransX: Float
    var animatorConfig: AnimatorConfig? = null
        set(value) {
            field = value
            layoutTransition.applyConfig(value)
        }

    private val changeSortPopupMenu = PopupMenu(context, ui.changeSortButton)
    private val optionsPopupMenu = PopupMenu(context, ui.menuButton)
    private val changeSortMenu get() = changeSortPopupMenu.menu
    val optionsMenu get() = optionsPopupMenu.menu

    /** Called when the back button is clicked. */
    var onBackButtonClick: (() -> Unit)? = null
    /** Called when the search button is clicked. */
    var onSearchButtonClick: (() -> Unit)? = null
    /** Called when the search query is changed. */
    var onSearchQueryChange get() = ui.titleSwitcher.onSearchQueryChange
                            set(value) { ui.titleSwitcher.onSearchQueryChange = value }
    /** Called when the delete button is clicked. */
    var onDeleteButtonClick: (() -> Unit)? = null
    /** Called when a sort option is clicked. */
    fun setOnSortOptionClick(listener: (MenuItem) -> Boolean) =
        changeSortPopupMenu.setOnMenuItemClickListener(listener)
    /** Called when an options menu item is clicked. */
    fun setOnOptionsItemClick(listener: (MenuItem) -> Boolean) =
        optionsPopupMenu.setOnMenuItemClickListener(listener)

    init {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT)
        val a = context.obtainStyledAttributes(attrs, R.styleable.ListActionBar)
        val changeSortMenuResId = a.getResourceIdOrThrow(R.styleable.ListActionBar_changeSortMenuResId)
        val optionsMenuResId = a.getResourceIdOrThrow(R.styleable.ListActionBar_optionsMenuResId)
        val contentsTint = try { a.getColorOrThrow(R.styleable.ListActionBar_contentsTint) }
                           catch(e: IllegalArgumentException) { null }
        a.recycle()

        contentsTint?.let {
            ui.backButton.drawable.setTint(it)
            ui.titleSwitcher.fragmentTitleView.setTextColor(it)
            ui.titleSwitcher.actionModeTitleView.setTextColor(it)
            ui.titleSwitcher.setSearchQueryTextColor(it)
            ui.searchButton.drawable.setTint(it)
            ui.changeSortButton.drawable.setTint(it)
            ui.menuButton.drawable.setTint(it)
        }

        changeSortPopupMenu.menuInflater.inflate(changeSortMenuResId, changeSortMenu)
        optionsPopupMenu.menuInflater.inflate(optionsMenuResId, optionsMenu)

        layoutTransition = layoutTransition(config = null)
        ui.backButton.setOnClickListener { onBackButtonClick?.invoke() }
        ui.searchButton.setOnClickListener { onSearchButtonClick?.invoke() }
        ui.changeSortButton.setOnClickListener {
            if (!ui.changeSortButton.isActivated)
                changeSortPopupMenu.show()
            else onDeleteButtonClick?.invoke()
        }
        ui.menuButton.setOnClickListener { optionsPopupMenu.show() }

        // The 4dp adjustment here represents the padding inherent to the back button's
        // vector drawable. It is added to the back button's actual paddingRight value
        // to get the apparent visual padding of the icon.
        backButtonHiddenTransX = ui.backButton.paddingRight + context.dpToPixels(4f) -
                                 ui.backButton.layoutParams.width
    }

    fun setBackButtonIsVisible(visible: Boolean) {
        if (ui.backButtonSpacer.isVisible == visible) return
        val endTransX = if (visible) 0f else backButtonHiddenTransX

        if (!isVisible) {
            ui.backButton.isInvisible = !visible
            ui.backButton.translationX = endTransX
        } else {
            ui.backButton.isVisible = true
            ui.backButton.animate()
                .withLayer().applyConfig(animatorConfig)
                .translationX(endTransX).withEndAction {
                    ui.backButton.isInvisible = !visible
                }.start()
        }
        ui.backButtonSpacer.isVisible = visible
    }

    fun setTitleState(state: TitleState) = when(state) {
        is TitleState.ActionMode ->  ui.titleSwitcher.setActionModeTitle(state.title, switchTo = true)
        is TitleState.SearchQuery -> ui.titleSwitcher.setSearchQuery(state.title, switchTo = true)
        is TitleState.NormalTitle -> ui.titleSwitcher.setTitle(state.title, switchTo = true)
    }

    fun setSearchButtonState(state: SearchButtonState) {
        ui.searchButton.isVisible = !state.isInvisible
        ui.searchButton.isActivated = state.isMorphedToClose
    }

    fun setChangeSortButtonState(state: ChangeSortButtonState, animate: Boolean = true) {
        ui.changeSortButton.isActivated = state.isMorphedToDelete
        if (!animate) {
            val drawable = ui.changeSortButton.drawable as? StateListDrawable
            drawable?.jumpToCurrentState()
        }
        ui.changeSortButton.contentDescription = context.getString(
            if (isActivated) R.string.change_sorting_description
            else             R.string.delete_button_description)
        ui.changeSortButton.isVisible = !state.isInvisible
        if (state is ChangeSortButtonState.Visible)
            changeSortMenu.getItemOrNull(state.selectedIndex)?.isChecked = true
    }

    fun setMenuButtonVisible(visible: Boolean) {
        // The layout transition for some reason does not fade the menu button
        // in and out when its visibility is set, so the fade in/out has to be
        // done manually.
        ui.menuButton.animate().alpha(if (visible) 1f else 0f)
            .withLayer().applyConfig(animatorConfig).start()
        // But the layout transition suddenly decides to work if the button's
        // visibility is set to gone or invisible at the end of the animation,
        // leading to the button fading out, jumping back to 1.0 alpha and
        // fading out again... To workaround this we'll just leave the button
        // at 0 alpha.
    }
}