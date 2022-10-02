/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.text.InputType
import android.util.AttributeSet
import android.view.ViewPropertyAnimator
import android.view.inputmethod.EditorInfo
import android.widget.ViewFlipper
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.SoftKeyboard

/**
 * A preconfigured ViewFlipper that allows animated switching between a
 * fragment title, action mode title, and a search view.
 *
 * ActionBarTitle is a ViewFlipper that adds two TextViews and an EditText
 * to be used as an app/activity/fragment title, an action mode title, and
 * a search query entry, respectively, and preconfigures short fade in and
 * out animations. XML attributes are passed on to these views so that
 * their attributes can be configured from an XML layout.
 *
 * The fragment title, action mode title, and search query view will have
 * their ids set to the values of R.id.actionBarTitle_fragmentTitle,
 * R.id.actionBarTitle_actionModeTitle, and R.id.actionBarTitle_searchQuery
 * respectively. The TextViews themselves can also be accessed through the
 * properties fragmentTitleView, actionModeTitleView, and searchQueryView.
 *
 * ActionBarTitle will not animate a change in the action mode title or in
 * the search query when they are already visible on screen due to the fact
 * that they are likely to change by only one character at a time (e.g.
 * from '2 items selected' to '3 items selected' or 'Search quer' to
 * 'Search query'). It will crossfade changes in the app/activity/fragment
 * title if it is visible when the change is made.
 *
 * searchQueryView uses an custom underline background to indicate that it is
 * a blank field to the user. In order to ensure that this custom background
 * uses the same text color as the search query itself, it is recommended to
 * set the search query's text color using the function setSearchQueryTextColor.
 */
class ActionBarTitle(context: Context, attrs: AttributeSet) : ViewFlipper(context, attrs) {

    private val fragmentTitlePos = 0
    private val actionModeTitlePos = 1
    private val searchViewPos = 2
    val fragmentTitleView = AppCompatTextView(context, attrs)
    val actionModeTitleView = AppCompatTextView(context, attrs)
    val searchQueryView = AppCompatEditText(context, attrs)

    var title: CharSequence get() = fragmentTitleView.text
                            set(value) = setTitle(value)
    var actionModeTitle: CharSequence get() = actionModeTitleView.text
                                      set(value) = setActionModeTitle(value)
    var searchQuery: CharSequence get() = searchQueryView.text.toString()
                                  set(value) = setSearchQuery(value)

    var onSearchQueryChange: ((CharSequence?) -> Unit)? = null

    val showingFragmentTitle get() = displayedChild == fragmentTitlePos
    val showingActionModeTitle get() = displayedChild == actionModeTitlePos
    val showingSearchView get() = displayedChild == searchViewPos

    init {
        addView(fragmentTitleView, fragmentTitlePos)
        addView(actionModeTitleView, actionModeTitlePos)
        addView(searchQueryView, searchViewPos)
        setInAnimation(context, R.anim.fade_in)
        setOutAnimation(context, R.anim.fade_out)

        fragmentTitleView.id = R.id.actionBarTitle_fragmentTitle
        actionModeTitleView.id = R.id.actionBarTitle_actionModeTitle
        searchQueryView.apply {
            id = R.id.actionBarTitle_searchQuery
            contentDescription = context.getString(R.string.search_query_description)
            maxLines = 1
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
            isFocusableInTouchMode = true
            searchQueryView.background = ContextCompat.getDrawable(context, R.drawable.search_query)
        }
        val a = context.obtainStyledAttributes(attrs, R.styleable.ActionBarTitle)

        var fontId = a.getResourceId(R.styleable.ActionBarTitle_titleFont, 0)
        fragmentTitleView.typeface = try { ResourcesCompat.getFont(context, fontId) }
                                     catch(e: Resources.NotFoundException) { null }

        fontId = a.getResourceId(R.styleable.ActionBarTitle_searchQueryFont, 0)
        searchQueryView.typeface = try { ResourcesCompat.getFont(context, fontId) }
                                   catch(e: Resources.NotFoundException) { null }

        fontId = a.getResourceId(R.styleable.ActionBarTitle_actionModeTitleFont, 0)
        actionModeTitleView.typeface = try { ResourcesCompat.getFont(context, fontId) }
                                       catch(e: Resources.NotFoundException) { null }
        a.recycle()

        // For some reason, if saveFromParentEnabled == true the title
        // will be "restored" to a blank string across activity restarts.
        isSaveFromParentEnabled = false
        searchQueryView.doAfterTextChanged {
            onSearchQueryChange?.invoke(it)
        }
    }

    fun showTitle() {
        if (showingFragmentTitle) return
        displayedChild = fragmentTitlePos
        SoftKeyboard.hide(this)
    }

    fun showActionModeTitle() {
        if (showingActionModeTitle) return
        displayedChild = actionModeTitlePos
        actionModeTitleView.isVisible = true
        actionModeTitleView.alpha = 1f
        titleCrossfade?.apply {
            cancel()
            actionModeTitleView.text = savedActionModeTitle
            actionModeTitleView.typeface = savedActionModeFont
        }
        SoftKeyboard.hide(this)
    }

    fun showSearchQuery(showSoftInput: Boolean = true) {
        if (showingSearchView) return
        displayedChild = searchViewPos
        searchQueryView.requestFocus()
        if (showSoftInput)
            SoftKeyboard.show(searchQueryView)
    }

    private var titleCrossfade: ViewPropertyAnimator? = null
    private var savedActionModeTitle: String? = null
    private var savedActionModeFont: Typeface? = null
    fun setTitle(title: CharSequence, switchTo: Boolean = false) {
        if (title != this.title) {
            if (!showingFragmentTitle)
                fragmentTitleView.text = title
            else {
                savedActionModeTitle = actionModeTitle.toString()
                savedActionModeFont = actionModeTitleView.typeface
                actionModeTitleView.text = fragmentTitleView.text
                actionModeTitleView.typeface = fragmentTitleView.typeface
                actionModeTitleView.alpha = 1f
                actionModeTitleView.isVisible = true
                fragmentTitleView.alpha = 0f
                fragmentTitleView.text = title

                // A reference to the animator is kept in case it needs to be canceled early.
                titleCrossfade = actionModeTitleView.animate()
                    .alpha(0f).withLayer().withEndAction {
                        titleCrossfade = null
                        actionModeTitleView.text = savedActionModeTitle
                        actionModeTitleView.typeface = savedActionModeFont
                    }.apply { start() }
                fragmentTitleView.animate().alpha(1f).withLayer().start()
            }
        }
        if (switchTo) showTitle()
    }

    fun setActionModeTitle(title: CharSequence, switchTo: Boolean = false) {
        actionModeTitleView.text = title
        if (switchTo) showActionModeTitle()
    }

    fun setSearchQuery(query: CharSequence?, switchTo: Boolean = false) {
        if (searchQueryView.text != query)
            searchQueryView.setTextKeepState(query)
        if (switchTo) showSearchQuery()
    }

    fun setSearchQueryTextColor(color: Int) {
        searchQueryView.setTextColor(color)
        searchQueryView.background?.setTint(color)
    }
}

@Composable fun RowScope.ActionBarTitle(
    title: String,
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    onSearchQueryChanged: (String) -> Unit,
) {
    Crossfade(// This outer crossfade is for when the search query appears/disappears.
        targetState = searchQuery != null,
        modifier = modifier,
    ) { searchQueryIsNotNull ->
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            // lastSearchQuery is used so that when the search query changes from a
            // non-null non-blank value to null, the search query will be recomposed
            // with the value of lastSearchQuery instead of null during the search
            // query's fade out animation. This allows the last non-null search
            // query text to fade out with the rest of the search query (i.e. the
            // underline) instead of abruptly disappearing.
            var lastSearchQuery by rememberSaveable { mutableStateOf("") }
            if (searchQueryIsNotNull) {
                val text = searchQuery ?: lastSearchQuery
                AutoFocusSearchQuery(text, onSearchQueryChanged)
                @Suppress("UNUSED_VALUE")
                if (searchQuery != null)
                    lastSearchQuery = searchQuery
            } else Crossfade(title) { // This inner crossfade is for when the title changes.
                Text(it, style = MaterialTheme.typography.h5, maxLines = 1)
            }
        }
    }
}

/**
 * A search query that auto-focuses when first composed,
 * and displays an underline as a background.
 *
 * @param query The current value of the search query
 * @param onQueryChanged The callback to be invoked when user input changes the query
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable fun AutoFocusSearchQuery(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        textStyle = MaterialTheme.typography.h6
            .copy(color = MaterialTheme.colors.onPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, ),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) keyboardController?.show()
            },
        singleLine = true,
    ) { innerTextField ->
        Box {
            innerTextField()
            androidx.compose.material.Divider(
                Modifier.align(Alignment.BottomStart),
                LocalContentColor.current, (1.5).dp)
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}