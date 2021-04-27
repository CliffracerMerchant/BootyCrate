/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.cliffracertech.bootycrate.AboutAppDialog
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.ShoppingListViewModel
import com.cliffracertech.bootycrate.activity.MainActivity
import com.cliffracertech.bootycrate.activity.MultiFragmentActivity
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

/** A fragment to display the BootyCrate app settings. */
class PreferencesFragment : PreferenceFragmentCompat(), MainActivity.MainActivityFragment {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>(getString(R.string.pref_light_dark_mode_key))?.apply {
            setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The view's id is set so that it can be tested easier. Unfortunately
        // I couldn't find any other way to check if the preferences fragment
        // was showing.
        view.id = R.id.settings_menu_item
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
//            getString(R.string.pref_theme_gradient_screen) -> { }
            getString(R.string.pref_sort_by_checked_key) -> {
                val sortByChecked = (preference as SwitchPreferenceCompat).isChecked
                val viewModel: ShoppingListViewModel by activityViewModels()
                viewModel.sortByChecked = sortByChecked
            } getString(R.string.pref_update_list_reminder_enabled_key) -> {
                val activity = activity as? MultiFragmentActivity ?: return false
                activity.addSecondaryFragment(UpdateListReminderFragment())
            } getString(R.string.pref_about_app_key) ->
                AboutAppDialog().show(childFragmentManager, null)
            getString(R.string.pref_open_source_libraries_used_key) -> {
                val context = this.context ?: return false
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            } else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

    override fun showsBottomAppBar() = false
    override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
        if (isActive) activityUi.actionBar.transition(
            backButtonVisible = true,
            searchButtonVisible = false,
            changeSortButtonVisible = false,
            menuButtonVisible = false)
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