/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.fragment.UpdateListReminder
import com.cliffracertech.bootycrate.utils.mutablePreferenceFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val navigationState: ReadOnlyNavigationState
): ViewModel() {
    private val sortByCheckedKey = booleanPreferencesKey(
        context.getString(R.string.pref_sort_by_checked_key))

    private val _sortByChecked = context.dataStore.mutablePreferenceFlow(
        sortByCheckedKey, viewModelScope, false)

    val sortByChecked = _sortByChecked.asStateFlow()

    fun onSortByCheckedClick() {
        _sortByChecked.value = !_sortByChecked.value
    }

    fun onUpdateListReminderClick() {
        navigationState.requestNewFragment(UpdateListReminder.SettingsFragment())
    }
}