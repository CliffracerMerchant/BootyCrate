/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

/**
 * A fragment to display the BootyCrate app settings.
 *
 * Note: PreferencesFragment is intended to hide the action bar menu when it
 * is shown, and show it again when it is hidden. For this to work properly,
 * the function initOptionsMenu(menu: Menu) must be called with an instance of
 * the app's action bar menu. See the comment before the implementation of
 * initOptionsMenu for more information.
 */
class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>(getString(R.string.pref_app_theme))?.apply {
            isPersistent = true
            setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.setGroupVisible(R.id.all_action_bar_items_group, false)
        menu.setGroupVisible(R.id.other_action_bar_menu_items, false)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            getString(R.string.pref_sort_by_checked) -> {
                val sortByChecked = (preference as SwitchPreferenceCompat).isChecked
                (activity as? MainActivity)?.shoppingListViewModel?.sortByChecked = sortByChecked
            }
            getString(R.string.pref_about_app) ->
                AboutAppDialog().show(parentFragmentManager, null)
            getString(R.string.pref_open_source_libraries_used) -> {
                val context = this.context ?: return false
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            } else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

//    private val getExportPath = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
//        val context = this.context ?: return@registerForActivityResult
//        if (uri != null) BootyCrateDatabase.backup(context, uri)
//    }
//
//    private val getImportPath = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
//        if (uri != null) Dialog.importDatabaseFromUri(uri, activity)
//    }
}