/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
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
 *
 *     - backButton, similar to the home as up indicator. The backButton's
 *       visibility is set through the method setBackButtonIsVisible. Clicks on
 *       the back button can be listened to by setting the property onBackButtonClick.
 *
 *     - titleSwitcher, an ActionBarTitle that is used as an activity or
 *       fragment title, an action mode title, or a search query entry. The
 *       value of the theme attribute actionBarTitleStyle is used as the style
 *       for the title switcher. The state of the titleSwitcher is set with the
 *       method setTitleState with an instance of TitleState. When the title is
 *       in its search query mode, changes in the search query entry can be
 *       listened to through by setting the property onSearchQueryChange.
 *
 *     - searchButton, a button whose icon can morph between a search icon and
 *       a close icon. The state of the search button is set with the method
 *       setSearchButtonState with an instance of SearchButtonState. Clicks on
 *       the search button can be listened to through the property onSearchButtonClick.
 *
 *     - changeSortButton, a button that opens the changeSortMenu, but can also
 *       morph to a delete icon instead. The state of the changeSortButton is
 *       set with the method setChangeSortButtonState with an instance of
 *       ChangeSortButtonState. The contents of the changeSortMenu are set in
 *       XML using the attribute R.attr.changeSortMenuResId. Changing the
 *       contents of the changeSortMenu at runtime is not currently supported.
 *       Change sort menu item clicks can be listened to by calling the function
 *       setOnSortOptionClick with an appropriate PopupMenu.OnMenuItemClickListener,
 *       while clicks on the delete button can be listened to by setting the
 *       property onDeleteButtonClick.
 *
 *     - menuButton, which opens the optionsMenu member. The visibility of the
 *       menu button is set at runtime using the function setMenuButtonVisible.
 *       The contents of the optionsMenu is set using the XML attribute
 *       R.attr.optionsMenuResId or at runtime using the function
 *       setOptionsMenuContents with a list of string resource ids to use as
 *       the titles for each menu item. In this case the item ids for the menu
 *       items will match the ids for the string resources used for their
 *       titles. Menu item clicks can be listened to through by calling the
 *       function setOnOptionsItemClick with an appropriate
 *       PopupMenu.OnMenuItemClickListener.
 *
 * The duration and interpolators used for the internal animations that are
 * played when the visibility of UI elements is changed can be set through the
 * property animatorConfig.
 */
class ListActionBar(context: Context, attrs: AttributeSet) :
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
    private val optionsMenu get() = optionsPopupMenu.menu

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
    fun setOnSortOptionClick(listener: PopupMenu.OnMenuItemClickListener) =
        changeSortPopupMenu.setOnMenuItemClickListener(listener)
    /** Called when an options menu item is clicked. */
    fun setOnOptionsItemClick(listener: PopupMenu.OnMenuItemClickListener) =
        optionsPopupMenu.setOnMenuItemClickListener(listener)

    init {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT)
        val a = context.obtainStyledAttributes(attrs, R.styleable.ListActionBar)
        val changeSortMenuResId = a.getResourceIdOrThrow(R.styleable.ListActionBar_changeSortMenuResId)
        val optionsMenuResId = a.getResourceId(R.styleable.ListActionBar_optionsMenuResId, -1)
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
        if (optionsMenuResId != -1)
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
        is TitleState.ActionMode ->
            ui.titleSwitcher.setActionModeTitle(state.titleRes.resolve(context), switchTo = true)
        is TitleState.SearchQuery ->
            ui.titleSwitcher.setSearchQuery(state.searchQuery, switchTo = true)
        is TitleState.NormalTitle ->
            ui.titleSwitcher.setTitle(state.titleRes.resolve(context), switchTo = true)
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

    /** Set the options menu contents using the provided list of StringResources.
     * The id, title, and order of each MenuItem will be equal to its corresponding
     * StringResource's stringResId property, its resolved string, and its position
     * in the list, respectively.
     */
    fun setOptionsMenuContents(stringResources: List<Int>) {
        optionsMenu.clear()
        stringResources.forEachIndexed { index, resId ->
            optionsMenu.add(Menu.NONE, resId, index, context.getString(resId))
        }
    }
}