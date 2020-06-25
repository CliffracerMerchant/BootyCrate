package com.cliffracermerchant.bootycrate

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView

class PreferencesFragment : PreferenceFragmentCompat() {
    companion object { val instance = PreferencesFragment() }

    init { setHasOptionsMenu(true) }

    private lateinit var itemDecoration: AlternatingRowBackgroundDecoration

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val darkThemeActivePref = findPreference<SwitchPreferenceCompat>(getString(R.string.pref_dark_theme_active))
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.findItem(R.id.app_bar_search).isVisible = false
        menu.findItem(R.id.change_sorting_button).isVisible = false
        menu.findItem(R.id.delete_all_button).isVisible = false
    }
}