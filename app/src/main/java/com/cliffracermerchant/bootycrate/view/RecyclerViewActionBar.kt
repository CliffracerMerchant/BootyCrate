/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.cliffracermerchant.bootycrate.R
import com.cliffracermerchant.bootycrate.databinding.RecyclerViewActionBarBinding
import com.cliffracermerchant.bootycrate.utils.AnimatorConfig
import com.cliffracermerchant.bootycrate.utils.applyConfig
import com.cliffracermerchant.bootycrate.utils.layoutTransition
import java.util.*

/**
 * A toolbar tailored towards interacting with a recycler view.
 *
 * RecyclerViewActionBar acts as an entirely custom (i.e. it eschews the
 * Android setSupportActionBar API in favor of its own) action bar with an
 * interface tailored towards activities or fragments that primarily show
 * a recycler view. Through its binding property ui, the ui elements avail-
 * able are:
 *     - backButton, similar to the home as up indicator, hidden by default
 *     - actionBarTitle, an ActionBarTitle that is used as an activity or
 *       fragment title, an action mode title, or a search query entry.
 *       The attribute android.R.attr.text is used as the default text for
 *       the title.
 *     - searchButton, a button that changes the actionBarTitle to its
 *       search query entry mode.
 *     - changeSortButton, a button that opens the changeSortMenu, but can
 *       also have isActivated set to true to change to a delete icon and
 *       call the property onDeleteButtonClickedListener instead.
 *     - menuButton, which opens the optionsMenu member.
 * If the activity or fragment using RecyclerViewActionBar wants to show
 * only the title, the property optionsMenuVisible can be set to false to
 * hide the search button, change sort button, and the options menu button
 * all at once.
 *
 * The contents of the changeSortMenu and the optionsMenu can be set in
 * XML with the attributes R.attr.changeSortMenuResId and R.attr.options-
 * MenuResId. The callbacks for the menu items being clicked can be set
 * through the functions setOnSortOptionClickedListener and setOnOptions-
 * ItemClickedListener. If the default Android action bar menu item call-
 * back functionality (every click being routed through onOptionsItemSel-
 * ected) is desired, the functions can be passed a lambda that manually
 * calls onOptionsItemSelected for the activity or fragment being used.
 *
 * RecyclerViewActionBar uses its own implementation of an action mode in
 * its inner class ActionMode. An action mode can be started by calling
 * the function startActionMode with an implementation of the ActionMode-
 * Callback interface. If another action mode was already started when a
 * new one is started, the old action mode will be finished. The current
 * action mode, or null if there isn't one, can be queried through the
 * property actionMode.
 *
 * The text entered in the search query view, or null if the search query
 * view is not shown, can be queried or set through the property active-
 * SearchQuery. Changes in the search query entry can be listened to
 * through by setting the property onSearchQueryChangedListener.
 *
 * If multiple changes to the action bar UI are desired at once (e.g. when
 * transitioning between displayed fragments), the function transition
 * should be called with parameters that describe the desired state of the
 * UI. This will ensure that any given combination of UI states is anima-
 * ted between smoothly.
 */
@Suppress("LeakingThis")
open class RecyclerViewActionBar(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs)
{
    val ui = RecyclerViewActionBarBinding.inflate(LayoutInflater.from(context), this)
    var animatorConfig: AnimatorConfig? = null
        set(value) { field = value; layoutTransition.applyConfig(value) }

    private val changeSortPopupMenu = PopupMenu(context, ui.changeSortButton)
    private val optionsPopupMenu = PopupMenu(context, ui.menuButton)
    val changeSortMenu get() = changeSortPopupMenu.menu
    val optionsMenu get() = optionsPopupMenu.menu

    var onDeleteButtonClickedListener: (() -> Unit)? = null
    fun setOnSortOptionClickedListener(listener: (MenuItem) -> Boolean) =
        changeSortPopupMenu.setOnMenuItemClickListener(listener)
    fun setOnOptionsItemClickedListener(listener: (MenuItem) -> Boolean) =
        optionsPopupMenu.setOnMenuItemClickListener(listener)

    val actionMode get() = _actionMode
    private var _actionMode: ActionMode? = null
    fun startActionMode(callback: ActionModeCallback) {
        _actionMode?.finish(updateActionBarUi = false)
        _actionMode = ActionMode(callback).apply { start() }
    }

    var activeSearchQuery get() = if (!ui.titleSwitcher.showingSearchView) null
                                  else ui.titleSwitcher.searchQuery
                          set(value) = setSearchQueryPrivate(value)
    var onSearchQueryChangedListener get() = ui.titleSwitcher.onSearchQueryChangedListener
                                     set(value) { ui.titleSwitcher.onSearchQueryChangedListener = value }

    init {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT)
        val a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerViewActionBar)
        val changeSortMenuResId = a.getResourceId(R.styleable.RecyclerViewActionBar_changeSortMenuResId, 0)
        val optionsMenuResId = a.getResourceId(R.styleable.RecyclerViewActionBar_optionsMenuResId, 0)
        a.recycle()
        changeSortPopupMenu.menuInflater.inflate(changeSortMenuResId, changeSortMenu)
        optionsPopupMenu.menuInflater.inflate(optionsMenuResId, optionsMenu)

        layoutTransition = layoutTransition(config = null)
        ui.searchButton.setOnClickListener {
            setSearchQueryPrivate(if (activeSearchQuery == null) "" else null)
        }
        ui.changeSortButton.setOnClickListener {
            if (!ui.changeSortButton.isActivated)
                changeSortPopupMenu.show()
            else onDeleteButtonClickedListener?.invoke()
        }
        ui.menuButton.setOnClickListener{ optionsPopupMenu.show() }
    }

    fun setBackButtonVisible(visible: Boolean, animate: Boolean = true) {
        if (ui.backButtonSpacer.isVisible == visible) return
        if (!animate) ui.backButton.isVisible = visible
        else ui.backButton.apply {
            alpha = if (visible) 0f else 1f
            isVisible = true
            animate().withLayer().alpha(if (visible) 1f else 0f).applyConfig(animatorConfig).start()
        }
        ui.backButtonSpacer.isVisible = visible
    }

    /**
     * Transition the action bar's visual state to match the one described by the parameters.
     *
     * Using transitionTo rather than making UI changes manually is recom-
     * mended when making multiple changes at once. For example, if a frag-
     * ment ends it action mode as another fragment begins its own, the title
     * will be briefly visible in between. Using the transition function will
     * ensure that any combination of UI states is animated between smoothly.
     */
    fun transition(
        backButtonVisible: Boolean = false,
        activeActionModeCallback: ActionModeCallback? = null,
        activeSearchQuery: CharSequence? = null,
        searchButtonVisible: Boolean = true,
        changeSortButtonVisible: Boolean = true,
        menuButtonVisible: Boolean = true
    ) {
        if (activeActionModeCallback == null && activeSearchQuery == null)
            setBackButtonVisible(backButtonVisible)

        if (searchButtonVisible != ui.searchButton.isVisible && activeActionModeCallback == null)
            ui.searchButton.isVisible = searchButtonVisible

        if (activeActionModeCallback != null && !ui.changeSortButton.isVisible)
            ui.changeSortButton.isVisible = true
        else if (changeSortButtonVisible != ui.changeSortButton.isVisible)
            ui.changeSortButton.isVisible = changeSortButtonVisible

        if (menuButtonVisible != ui.menuButton.isVisible)
            ui.menuButton.isVisible = menuButtonVisible

        ui.titleSwitcher.setSearchQuery(activeSearchQuery ?: "", switchTo = false)
        activeActionModeCallback?.let {
            startActionMode(it)
            ui.searchButton.isActivated = false
        }
        if (activeActionModeCallback == null) {
            actionMode?.apply {
                finish(updateActionBarUi = false)
                ui.changeSortButton.isActivated = false
                if (activeSearchQuery == null) {
                    ui.titleSwitcher.showTitle()
                    ui.changeSortButton.isActivated = false
                }
            }
            setSearchQueryPrivate(activeSearchQuery, showSoftInput = false,
                                  hideBackButtonWhenDone = false)
        }
    }

    private fun setSearchQueryPrivate(
            query: CharSequence?,
            showSoftInput: Boolean = true,
            hideBackButtonWhenDone: Boolean = true) {
        val searchWasActive = activeSearchQuery != null
        if (hideBackButtonWhenDone || query != null)
            setBackButtonVisible(query != null)
        if (query != null) {
            ui.titleSwitcher.searchQuery = query
            if (!searchWasActive) {
                ui.titleSwitcher.showSearchQuery(showSoftInput)
                ui.searchButton.isActivated = true
            }
        } else {
            ui.titleSwitcher.setSearchQuery("")
            ui.titleSwitcher.showTitle()
            ui.searchButton.isActivated = false
        }
    }

    /**
     * An reimplementation of ActionMode that uses an instance of RecyclerViewActionBar.
     *
     * RecyclerViewActionBar.ActionMode is intended to be a replacement
     * for an Android ActionMode that reuses a RecyclerViewActionBar
     * instead of overlaying the support action bar. An implementation of
     * the ActionModeCallback interface is required to be passed in to the
     * constructor.
     *
     * Once the action mode is created with a callback, calling start will
     * start the action mode, display the back button on the action bar,
     * switch the action bar's changeSortButton to a delete icon, and
     * switch the title to the action mode's title, accessed through the
     * property title. The function finish can be called when desired to
     * end the ActionMode and switch the title back to the app/activity/
     * fragment title. If another action mode is going to be started imme-
     * diately after this one is finished, finish should be called with
     * the parameter updateActionBarUi set to false so that the new action
     * mode won't immediately undo the ui changes caused by the first one
     * ending and cause flickering.

     * Due to the fact that the same action bar can be used for multiple
     * ActionModes, and because the actionBar backButton can be used for
     * multiple purpose, ActionMode does not set the onClickListener of
     * the action bar's backButton despite making it visible. It is up to
     * the implementing activity or fragment to make the backButton finish
     * the action mode if this is desired.
     *
     * Implementors of ActionModeCallback should make any desired changes
     * to the action bar menu in their implementation of onStart and onFin-
     * ish. Because the action bar's original options menu is used, the
     * implementing activity or fragment will have to respond to action
     * item clicks in the action bar's onOptionsItemSelectedListener.
     */
    inner class ActionMode(val callback: ActionModeCallback) {
        var title get() = ui.titleSwitcher.actionModeTitle
                  set(value) { ui.titleSwitcher.actionModeTitle = value }

        fun start() {
            ui.titleSwitcher.setActionModeTitle(title, switchTo = true)
            if (!ui.backButtonSpacer.isVisible)
                setBackButtonVisible(true)
            if (ui.searchButton.isVisible)
                ui.searchButton.isVisible = false
            if (!ui.changeSortButton.isActivated)
                ui.changeSortButton.isActivated = true
            callback.onStart(this, this@RecyclerViewActionBar)
        }

        fun finish(updateActionBarUi: Boolean = true) {
            if (updateActionBarUi) {
                // The layout transition should take care of the search button fade in animation but doesn't for some reason.
                if (!ui.searchButton.isVisible) {
                    ui.searchButton.alpha = 0f
                    ui.searchButton.isVisible = true
                    ui.searchButton.animate().alpha(1f).withLayer().applyConfig(animatorConfig).start()
                }
                ui.changeSortButton.isActivated = false
                if (ui.titleSwitcher.searchQuery.isNotEmpty())
                    ui.titleSwitcher.showSearchQuery()
                else {
                    ui.titleSwitcher.showTitle()
                    setBackButtonVisible(false)
                }
            }
            callback.onFinish(this, this@RecyclerViewActionBar)
            _actionMode = null
        }
    }

    /** An interface to describe what happens when a RecyclerViewActionBar.ActionMode starts or finishes. */
    interface ActionModeCallback {
        fun onStart(actionMode: ActionMode, actionBar: RecyclerViewActionBar) { }
        fun onFinish(actionMode: ActionMode, actionBar: RecyclerViewActionBar) { }
    }
}