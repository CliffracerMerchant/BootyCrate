/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

/** A fragment to display the BootyCrate app settings. */
class PreferencesFragment : PreferenceFragmentCompat(), MainActivity.FragmentInterface {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>(getString(R.string.pref_light_dark_mode))?.apply {
            setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
//            getString(R.string.pref_theme_gradient_screen) -> { }
            getString(R.string.pref_sort_by_checked) -> {
                val sortByChecked = (preference as SwitchPreferenceCompat).isChecked
                val viewModel: ShoppingListViewModel by activityViewModels()
                viewModel.sortByChecked = sortByChecked
            }
            getString(R.string.pref_about_app) ->
                AboutAppDialog().show(childFragmentManager, null)
            getString(R.string.pref_open_source_libraries_used) -> {
                val context = this.context ?: return false
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            } else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

    override fun showsOptionsMenu() = false
    override fun showsBottomAppBar() = false
    // Since the bottom app bar will be hidden we might as well
    // prevent the checkout button from needing to be animated
    override fun showsCheckoutButton() = true
    override fun onBackPressed() = false

//    private val getExportPath = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
//        val context = this.context ?: return@registerForActivityResult
//        if (uri != null) BootyCrateDatabase.backup(context, uri)
//    }
//
//    private val getImportPath = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
//        if (uri != null) Dialog.importDatabaseFromUri(uri, activity)
//    }
}