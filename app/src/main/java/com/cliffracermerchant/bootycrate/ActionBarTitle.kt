/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ViewFlipper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged

/**
 * A preconfigured ViewFlipper that allows animated switching between a
 * title, action mode title, and a search view.
 *
 * ActionBarTitle is a ViewFlipper that adds two TextViews and an EditText
 * to be used as an app/activity/fragment title, an action mode title, and
 * a search query entry respectively, and preconfigures short fade in and
 * out animations. XML attributes are passed on to these views so that
 * their attributes can be configured from an XML layout.
 */
class ActionBarTitle(context: Context, attrs: AttributeSet) : ViewFlipper(context, attrs) {

    private val imm = inputMethodManager(context)
    private val titlePos = 0
    private val actionModeTitlePos = 1
    private val searchViewPos = 2
    val titleView = AppCompatTextView(context, attrs)
    val actionModeTitleView = AppCompatTextView(context, attrs)
    val searchQueryView = EditText(context, attrs)

    var title: CharSequence get() = titleView.text
                            set(value) = setTitle(value)
    var actionModeTitle: CharSequence get() = actionModeTitleView.text
                                      set(value) = setActionModeTitle(value)
    var searchQuery: CharSequence get() = searchQueryView.text.toString()
                                  set(value) = setSearchQuery(value)

    var onSearchQueryChangedListener: ((CharSequence?) -> Unit)? = null

    val showingTitle get() = displayedChild == titlePos
    val showingActionModeTitle get() = displayedChild == actionModeTitlePos
    val showingSearchView get() = displayedChild == searchViewPos

    init {
        addView(titleView, titlePos)
        addView(actionModeTitleView, actionModeTitlePos)
        addView(searchQueryView, searchViewPos)
        setInAnimation(context, R.anim.fade_in)
        setOutAnimation(context, R.anim.fade_out)
        val a = context.obtainStyledAttributes(attrs, R.styleable.ActionBarTitle)
        var fontId = a.getResourceId(R.styleable.ActionBarTitle_titleFont, 0)
        titleView.typeface = try { ResourcesCompat.getFont(context, fontId) }
                          catch(e: Resources.NotFoundException) { null }

        fontId = a.getResourceId(R.styleable.ActionBarTitle_searchViewFont, 0)
        searchQueryView.typeface = try { ResourcesCompat.getFont(context, fontId) }
                               catch(e: Resources.NotFoundException) { null }

        fontId = a.getResourceId(R.styleable.ActionBarTitle_actionModeTitleFont, 0)
        actionModeTitleView.typeface = try { ResourcesCompat.getFont(context, fontId) }
                                    catch(e: Resources.NotFoundException) { null }
        a.recycle()

        searchQueryView.doAfterTextChanged { text -> onSearchQueryChangedListener?.invoke(text) }
    }

    fun showTitle() { if (showingTitle) return
                      displayedChild = titlePos
                      imm?.hideSoftInputFromWindow(windowToken, 0) }
    fun showActionModeTitle() { if (showingActionModeTitle) return
                                displayedChild = actionModeTitlePos
                                imm?.hideSoftInputFromWindow(windowToken, 0) }
    fun showSearchQuery() { if (showingSearchView) return
                            displayedChild = searchViewPos
                            searchQueryView.requestFocus()
                            imm?.showSoftInput(searchQueryView, InputMethodManager.SHOW_IMPLICIT) }

    fun setTitle(title: CharSequence, switchTo: Boolean = false) {
        titleView.text = title
        if (switchTo) showTitle()
    }

    fun setActionModeTitle(title: CharSequence, switchTo: Boolean = false) {
        actionModeTitleView.text = title
        if (switchTo) showActionModeTitle()
    }

    fun setSearchQuery(query: CharSequence, switchTo: Boolean = false) {
        searchQueryView.setText(query)
        if (!switchTo) return
        showSearchQuery()
    }
}