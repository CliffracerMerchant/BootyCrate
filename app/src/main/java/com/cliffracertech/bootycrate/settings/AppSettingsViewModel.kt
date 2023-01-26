/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.utils.enumPreferenceState
import com.cliffracertech.bootycrate.utils.preferenceState
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore("preferences")

@Module @InstallIn(SingletonComponent::class)
class DataStoreModule {
    @Singleton @Provides
        fun provideDataStore(@ApplicationContext app: Context) = app.dataStore
}

/** Edit the DataStore preference pointed to by [key] to the new [value] in [scope]. */
fun <T> DataStore<Preferences>.edit(
    key: Preferences.Key<T>,
    value: T,
    scope: CoroutineScope,
) {
    scope.launch { edit { it[key] = value } }
}

/** Edit the DataStore preference pointed to by [key] to the new [enumValue] in [scope]. */
fun <T: Enum<T>> DataStore<Preferences>.edit(
    enumValue: T,
    key: Preferences.Key<Int>,
    scope: CoroutineScope,
) {
    scope.launch { edit { it[key] = enumValue.ordinal } }
}

object PrefKeys {
    /** An integer value that represents the version code of the app during its last launch. */
    const val lastLaunchVersionCode = "last_launch_version_code"

    /** An integer value that represents the ordinal of the desired
    * [AppTheme] enum value to use as the application's light/dark theme. */
    const val appTheme = "app_theme"

    /** A boolean value that represents whether or not shopping list items
    * will be sorted by their checked state after the primary sorting method. */
    const val sortByChecked = "sort_by_checked"

    /** An integer value that represents the ordinal of the desired
    * [ListItem.Sort] enum value to use to as the item sorting method. */
    const val itemSort = "item_sort"
}

enum class AppTheme { MatchSystem, Light, Dark;
    companion object {
        /** Return an Array<String> containing strings that describe the enum values. */
        @Composable fun valueStrings() = with(LocalContext.current) {
            remember { arrayOf(
                getString(R.string.pref_theme_match_system_title),
                getString(R.string.pref_theme_light_theme_title),
                getString(R.string.pref_theme_dark_theme_title)
            )}
        }
    }
}

@HiltViewModel class AppSettingsViewModel(
    private val dataStore: DataStore<Preferences>,
    coroutineScope: CoroutineScope?,
): ViewModel() {
    @Inject constructor(
        dataStore: DataStore<Preferences>
    ) : this(dataStore, null)

    private val scope = coroutineScope ?: viewModelScope
    private val appThemeKey = intPreferencesKey(PrefKeys.appTheme)
    private val sortByCheckedKey = booleanPreferencesKey(PrefKeys.sortByChecked)

    val appTheme by dataStore.enumPreferenceState(appThemeKey, scope, AppTheme.MatchSystem)

    fun onAppThemeClick(value: AppTheme) {
        if (value == appTheme) return
        dataStore.edit(value, appThemeKey, scope)
    }

    val sortByChecked by dataStore.preferenceState(sortByCheckedKey, false, scope)

    fun onSortByCheckedClick() =
        dataStore.edit(sortByCheckedKey, !sortByChecked, scope)

//    private val getExportPath = registerForActivityResult(
//        ActivityResultContracts.CreateDocument("application/octet-stream")
//    ) { uri ->
//        val context = this.context ?: return@registerForActivityResult
//        if (uri != null) database.backup(context, uri)
//    }
//
//    private val getImportPath = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
//        val activity = this.activity ?: return@registerForActivityResult
//        if (uri != null) importDatabaseFromUriDialog(uri, activity, database)
//    }
}