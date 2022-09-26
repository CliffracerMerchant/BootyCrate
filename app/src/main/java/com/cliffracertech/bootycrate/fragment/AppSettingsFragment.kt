/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.*
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.dialog.AboutAppDialog
import com.cliffracertech.bootycrate.dialog.PrivacyPolicyDialog
import com.cliffracertech.bootycrate.dialog.importDatabaseFromUriDialog
import com.cliffracertech.bootycrate.model.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.utils.*
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
): ViewModel() {
    private val sortByCheckedKey = booleanPreferencesKey(
        context.getString(R.string.pref_sort_by_checked_key))

    private val _sortByChecked = context.dataStore.mutablePreferenceFlow(
        sortByCheckedKey, viewModelScope, false)

    val sortByChecked = _sortByChecked.asStateFlow()

    fun onSortByCheckedClick() {
        _sortByChecked.value = !_sortByChecked.value
    }
}

/** A fragment to display the BootyCrate app settings. */
@AndroidEntryPoint
class AppSettingsFragment: PreferenceFragmentCompat() {
    @Inject lateinit var database: BootyCrateDatabase
    private var sortByCheckedSwitch: SwitchPreferenceCompat? = null
    private val viewModel: AppSettingsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>(getString(R.string.pref_light_dark_mode_key))?.apply {
            setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }
        }
        sortByCheckedSwitch = findPreference(getString(R.string.pref_sort_by_checked_key))
        sortByCheckedSwitch?.isPersistent = false
        recollectWhenStarted(viewModel.sortByChecked) {
            sortByCheckedSwitch?.isChecked = it
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
//            getString(R.string.pref_theme_gradient_screen) -> { }
            getString(R.string.pref_sort_by_checked_key) ->
                viewModel.onSortByCheckedClick()
            getString(R.string.pref_update_list_reminder_enabled_key) ->
                replaceSelfWith(UpdateListReminder.SettingsFragment())
            getString(R.string.pref_export_database_key) ->
                getExportPath.launch(getString(R.string.exported_database_default_name))
            getString(R.string.pref_import_database_key) ->
                getImportPath.launch(arrayOf("*/*"))
            getString(R.string.pref_about_app_key) ->
                AboutAppDialog().show(childFragmentManager, null)
            getString(R.string.pref_privacy_policy_key) ->
                PrivacyPolicyDialog().show(childFragmentManager, null)
            getString(R.string.pref_open_source_libraries_used_key) -> {
                val context = this.context ?: return false
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            } else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

    private val getExportPath = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val context = this.context ?: return@registerForActivityResult
        if (uri != null) database.backup(context, uri)
    }

    private val getImportPath = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val activity = this.activity ?: return@registerForActivityResult
        if (uri != null) importDatabaseFromUriDialog(uri, activity, database)
    }
}