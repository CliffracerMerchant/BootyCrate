/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import androidx.core.view.isVisible

/**
 * An reimplementation of ActionMode that uses an instance of RecyclerViewActionBar.
 *
 * RecyclerViewActionMode is intended to be a replacement for an Android
 * ActionMode that reuses a RecyclerViewActionBar instead of overlaying the
 * support action bar. It requires an instance of RecyclerViewActionBar, which
 * can be passed in during the constructor, or set through the property action-
 * Bar. The property is nullable and public so that fragments that use a Recy-
 * clerViewActionMode can null the property during their onDestroyView.
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
 * tiple purpose, RecyclerViewActionMode does not set the onClickListener of
 * the action bar's backButton despite making it visible. It is up to the
 * implementing activity or fragment to make the backButton finish the action
 * mode.
 */
open class RecyclerViewActionMode(actionBar: RecyclerViewActionBar? = null) {
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