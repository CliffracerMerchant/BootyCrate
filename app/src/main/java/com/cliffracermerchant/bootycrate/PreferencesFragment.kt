/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat

/** A fragment to display the BootyCrate app settings.
 *
 *  Note: PreferencesFragment is intended to hide the action bar menu when it
 *  is shown, and show it again when it is hidden. For this to work properly,
 *  the function initOptionsMenu(menu: Menu) must be called with an instance of
 *  the app's action bar menu. See the comment before the implementation of
 *  initOptionsMenu for more information. */
class PreferencesFragment : PreferenceFragmentCompat() {
    private var menu: Menu? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val darkThemeActivePref = findPreference<SwitchPreferenceCompat>(getString(R.string.pref_dark_theme_active))
        darkThemeActivePref?.isPersistent = true
        // An activity restart is necessary when the user changes
        // the theme to ensure that all fragments use the new theme.
        darkThemeActivePref?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }

        findPreference<Preference>(getString(R.string.pref_export_database))?.setOnPreferenceClickListener {
            getExportPath.launch(getString(R.string.exported_database_default_name))
            true
        }

        findPreference<Preference>(getString(R.string.pref_import_database))?.setOnPreferenceClickListener {
            getImportPath.launch(arrayOf("*/*"))
            true
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        menu?.setGroupVisible(R.id.all_action_bar_items_group, hidden)
        // See R.menu.action_bar_menu source for an explanation of "other_action_bar_menu_items"
        menu?.setGroupVisible(R.id.other_action_bar_menu_items, hidden)
    }

    /* Ideally the PreferenceFragment's menu property would be initialized in an
       override of onCreateOptionsMenu, so that PreferenceFragment's onHidden-
       Changed override could properly hide the options menu when it is shown.
       Unfortunately onCreateOptionsMenu seems to not be called for some reason,
       even when setHasOptionsMenu(true) is called in the PreferenceFragment's
       init block. initOptionsMenu() must therefore be called manually by the outer
       activity, probably in its onCreateOptionsMenu override, so that the options
       menu is properly hidden when the instance of PreferencesFragment is shown. */
    fun initOptionsMenu(menu: Menu) { this.menu = menu }

    private val getExportPath = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        val context = this.context ?: return@registerForActivityResult
        if (uri != null) BootyCrateDatabase.backup(context, uri)
    }

    private val getImportPath = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val context = this.context ?: return@registerForActivityResult
        if (uri != null) {
            val dialogBuilder = themedAlertDialogBuilder(context)
            dialogBuilder.setMessage(R.string.import_database_question_message)
            dialogBuilder.setNeutralButton(android.R.string.cancel) { _, _ -> }
            dialogBuilder.setNegativeButton(R.string.import_database_question_merge_option) { _, _ ->
                BootyCrateDatabase.mergeWithBackup(context, uri)
            }
            dialogBuilder.setPositiveButton(R.string.import_database_question_overwrite_option) { _, _ ->
                val dialogBuilder = themedAlertDialogBuilder(context)
                dialogBuilder.setMessage(R.string.import_database_overwrite_confirmation_message)
                dialogBuilder.setNegativeButton(android.R.string.no) { _, _ -> }
                dialogBuilder.setPositiveButton(android.R.string.yes) { _, _ ->
                    BootyCrateDatabase.replaceWithBackup(context, uri)
                    // The pref pref_viewmodels_need_cleared needs to be set to true so that
                    // when the MainActivity is recreated, it will clear its ViewModelStore
                    // and use the DAOs of the new database instead of the old one.
                    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val editor = prefs.edit()
                    editor.putBoolean(context.getString(R.string.pref_viewmodels_need_cleared), true)
                    editor.apply()
                    activity?.recreate()
                }
                dialogBuilder.show()
            }
            dialogBuilder.show()
        }
    }
}