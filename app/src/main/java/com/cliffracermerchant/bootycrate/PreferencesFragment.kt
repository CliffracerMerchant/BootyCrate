/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

/** A fragment to display the BootyCrate app settings. */
class PreferencesFragment : PreferenceFragmentCompat() {

    init { setHasOptionsMenu(true) }

    private lateinit var itemDecoration: AlternatingRowBackgroundDecoration

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val darkThemeActivePref = findPreference<SwitchPreferenceCompat>(getString(R.string.pref_dark_theme_active))
        // An activity restart is necessary when the user changes the theme to
        // ensure that all fragments use the new theme.
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
        val recyclerView = super.onCreateRecyclerView(inflater, parent,
                                                      savedInstanceState)
        recyclerView.addItemDecoration(itemDecoration)
        return recyclerView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = menu.clear()
}