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

/** A fragment to display the BootyCrate app settings. */
class PreferencesFragment : PreferenceFragmentCompat() {

    init { setHasOptionsMenu(true) }

    private lateinit var itemDecoration: AlternatingRowBackgroundDecoration

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
            //val activity = this.activity ?: return@setOnPreferenceClickListener false
            //exporter.chooseDatabaseExportLocation(activity)
            getExportPath.launch(getString(R.string.exported_database_default_name))
            true
        }

        findPreference<Preference>(getString(R.string.pref_import_database))?.setOnPreferenceClickListener {
            getImportPath.launch(arrayOf("*/*"))
            //val activity = this.activity ?: return@setOnPreferenceClickListener false
            //exporter.chooseDatabaseImportLocation(activity)
            true
        }
    }

    override fun onAttach(context: Context) {
        itemDecoration = AlternatingRowBackgroundDecoration(context)
        super.onAttach(context)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) = menu.clear()

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