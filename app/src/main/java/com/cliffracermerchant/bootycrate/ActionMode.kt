/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.ActionBar

/** An reimplementation of androidx.appcompat.view.ActionMode that reuses the action bar already in use.
 *
 *  ActionMode is intended to be a replacement for an androidx.appcompat.view.-
 *  ActionMode that reuses the action bar instead of replacing it. This can be
 *  useful when, for example, a toolbar with custom painting is being used as
 *  the action bar so that the look of the custom toolbar is preserved when the
 *  action mode is started.
 *
 *  The ActionMode requires the activity of the fragment using it's action bar,
 *  the options menu, and optionally a custom title view. These are passed to
 *  the ActionMode using the init function. If this is not called, then start-
 *  ing the ActionMode will do nothing. Once initialized, calling start will
 *  start the ActionMode, display the home as up indicator on the action bar,
 *  and replace the text of the custom title view if used, or the action bar's
 *  title otherwise, with the current value of the property title. The original
 *  title will be restored when the ActionMode is finished. Updating the prop-
 *  erty title while the ActionMode is already started will also update the
 *  displayed title. The function finish can be called when desired to end the
 *  ActionMode
 *
 *  Sub classes should override onStart and onFinish to make the desired
 *  changes to the UI while the ActionMode is started. Because the action bar's
 *  original menu is used, the implementing activity or fragment will have to
 *  respond to action item clicks in their implementation of onOptionsItemSel-
 *  ected. Unfortunately there is no way for ActionMode to determine what will
 *  happen when the homeAsUpIndicator is pressed, so the implementing activity
 *  or fragment will also have to handle this event in an override of onSupport-
 *  NavigateUp if they wish for the ActionMode to end when this occurs. */
open class ActionMode {
    private var actionBar: ActionBar? = null
    private var menu: Menu? = null
    private var titleView: TextView? = null

    val isStarted get() = _isStarted
    private var _isStarted = false
    private var oldTitle: String? = null
    var title: String? = null
        set(value) { field = value
                     if (!_isStarted) return
                     val titleView = this.titleView
                     if (titleView != null) titleView.text = title
                     else                   actionBar?.title = title }

    fun init(actionBar: ActionBar, menu: Menu, titleView: TextView? = null) {
        this.actionBar = actionBar
        this.menu = menu
        this.titleView = titleView
    }

    fun start() {
        if (_isStarted) return
        val actionBar = this.actionBar ?: return
        val menu = this.menu ?: return
        val titleView = this.titleView

        _isStarted = true
        oldTitle = (titleView?.text ?: actionBar.title).toString()
        if (titleView != null) titleView.text = title
        else                   actionBar.title = title
        actionBar.setDisplayHomeAsUpEnabled(true)
        onStart(actionBar, menu, titleView)
    }

    fun finish() {
        if (!_isStarted) return
        val actionBar = this.actionBar ?: return
        val titleView = this.titleView
        val menu = this.menu ?: return

        _isStarted = false
        if (titleView != null) titleView.text = oldTitle
        else                   actionBar.title = oldTitle
        oldTitle = null
        actionBar.setDisplayHomeAsUpEnabled(false)
        onFinish(actionBar, menu, titleView)
    }

    open fun onStart(actionBar: ActionBar, menu: Menu, titleView: TextView?) { }
    open fun onFinish(actionBar: ActionBar, menu: Menu, titleView: TextView?) { }
}