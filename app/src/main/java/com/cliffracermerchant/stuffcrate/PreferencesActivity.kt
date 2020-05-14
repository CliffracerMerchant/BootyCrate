package com.cliffracermerchant.stuffcrate

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.recyclerview.widget.RecyclerView


class PreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getDefaultSharedPreferences(this)
        val darkThemeActive = prefs.getBoolean(getString(R.string.pref_dark_theme_active), false)
        setTheme(if (darkThemeActive) R.style.DarkTheme
                 else                 R.style.LightTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.preferences_layout)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportFragmentManager.beginTransaction().replace(
            R.id.preferences_menu_item,PreferencesFragment()).commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onBackPressed() {
        /* Normally using the back button to return to the main activity as
         * opposed to the navigate up button will cause the main activity to
         * become visible again before it checks for a theme change. It would
         * therefore be temporarily visible with the old theme before it can
         * update itself to use the new theme. Making the back button function
         * the same as the navigate up button will prevent this. */
        onNavigateUp()
    }

    class PreferencesFragment : PreferenceFragmentCompat() {
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
}