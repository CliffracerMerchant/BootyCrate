/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView

/** A fragment to display the BootyCrate app settings. */
class PreferencesFragment : PreferenceFragmentCompat() {

    init { setHasOptionsMenu(true) }

    private lateinit var itemDecoration: AlternatingRowBackgroundDecoration

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val darkThemeActivePref = findPreference<SwitchPreferenceCompat>(getString(R.string.pref_dark_theme_active))
        // An activity restart is necessary when the user changes
        // the theme to ensure that all fragments use the new theme.
        darkThemeActivePref?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }
        darkThemeActivePref?.isPersistent = true
    }

    override fun onAttach(context: Context) {
        itemDecoration = AlternatingRowBackgroundDecoration(context)
        super.onAttach(context)
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup,
                                      savedInstanceState: Bundle?): RecyclerView? {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.addItemDecoration(itemDecoration)
        return recyclerView
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) =
        menu.clear()

    /* For some reason after recreating the main activity to apply a theme change,
     * PreferencesFragment's theme is sometimes not changed with the rest of the
     * app. The item decoration will therefore use the wrong alternate background
     * color. Calling updateItemDecoration in the activity's onCreate can fix this. */
    fun updateItemDecoration(context: Context) = itemDecoration.update(context)
}