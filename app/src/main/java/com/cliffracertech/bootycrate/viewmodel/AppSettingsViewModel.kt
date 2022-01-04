/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.utils.mutablePreferenceFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An implementation of ItemListViewModel<ShoppingListItem>.
 *
 * ShoppingListViewModel adds a new sortByChecked option, which will sort
 * ShoppingListItems by their checked state in addition to the sorting method
 * described by the sort property. It also adds functions to respond to clicks
 * on the checkbox of items, and to respond to a request to checkout.
 */
class AppSettingsViewModel(app: Application) :
    AndroidViewModel(app)
{
    private val sortByCheckedKey = booleanPreferencesKey(app.getString(R.string.pref_sort_by_checked_key))
    private val _sortByChecked = app.dataStore.mutablePreferenceFlow(
        key = sortByCheckedKey, scope = viewModelScope, defaultValue = false)
    val sortByChecked = _sortByChecked.asStateFlow()

    fun onSortByCheckedSwitchClicked() {
        _sortByChecked.value = !_sortByChecked.value
    }
}