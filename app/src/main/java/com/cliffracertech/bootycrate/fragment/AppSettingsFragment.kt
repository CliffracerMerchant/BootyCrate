/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.MainActivity
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.utils.AboutAppDialog
import com.cliffracertech.bootycrate.utils.PrivacyPolicyDialog
import com.cliffracertech.bootycrate.utils.importDatabaseFromUriDialog
import com.cliffracertech.bootycrate.utils.mutablePreferenceFlow
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import kotlinx.coroutines.flow.MutableStateFlow

/** A fragment to display the BootyCrate app settings. */
class AppSettingsFragment : PreferenceFragmentCompat(), MainActivity.MainActivityFragment {

    private var sortByCheckedSwitch: SwitchPreferenceCompat? = null
    private var sortByChecked: MutableStateFlow<Boolean>? = null

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            val key = booleanPreferencesKey(getString(R.string.pref_sort_by_checked_key))
            sortByChecked = it.applicationContext.dataStore.mutablePreferenceFlow(
                key, viewLifecycleOwner.lifecycleScope, false)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
//            getString(R.string.pref_theme_gradient_screen) -> { }
            getString(R.string.pref_sort_by_checked_key) ->
                sortByChecked?.apply { value = !value }
            getString(R.string.pref_update_list_reminder_enabled_key) ->
                addSecondaryFragment(UpdateListReminder.SettingsFragment())
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

    override fun showsBottomAppBar() = false

    private val getExportPath = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        val context = this.context ?: return@registerForActivityResult
        if (uri != null) BootyCrateDatabase.backup(context, uri)
    }

    private val getImportPath = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val activity = this.activity ?: return@registerForActivityResult
        if (uri != null) importDatabaseFromUriDialog(uri, activity)
    }
}