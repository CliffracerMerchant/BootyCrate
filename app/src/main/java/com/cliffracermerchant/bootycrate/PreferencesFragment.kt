package com.cliffracermerchant.bootycrate

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class PreferencesFragment : PreferenceFragmentCompat() {
    companion object { val instance = PreferencesFragment() }

    private lateinit var itemDecoration: AlternatingRowBackgroundDecoration

    private val darkThemeChangeListener =
        Preference.OnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val darkThemeActivePref = findPreference<SwitchPreferenceCompat>(
                                getString(R.string.pref_dark_theme_active))
        darkThemeActivePref?.onPreferenceChangeListener = darkThemeChangeListener
        darkThemeActivePref?.isPersistent = true
    }

    override fun onAttach(context: Context) {
        itemDecoration = AlternatingRowBackgroundDecoration(context)
        super.onAttach(/*themeWrapper*/context)
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup,
                                      savedInstanceState: Bundle?): RecyclerView? {
        val recyclerView = super.onCreateRecyclerView(inflater, parent,
                                                      savedInstanceState)
        recyclerView.addItemDecoration(itemDecoration)
        return recyclerView
    }
}