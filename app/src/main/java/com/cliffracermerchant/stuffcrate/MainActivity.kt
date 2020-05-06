package com.cliffracermerchant.stuffcrate

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getDefaultSharedPreferences(this)
        val darkThemeActive = prefs.getBoolean(getString(R.string.pref_dark_theme_active), false)
        setTheme(if (darkThemeActive) R.style.DarkTheme
                 else                 R.style.LightTheme)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val viewModel = ViewModelProvider(this).get(InventoryViewModel::class.java)
        recyclerView.setViewModel(this, viewModel)
        recyclerView.adapter.selection.sizeLiveData.observe(this, Observer { newSize ->
            if (newSize == 0 && actionMode != null) actionMode?.finish()
            else if (newSize > 0) {
                if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback)
                actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        })

        add_item_button.setOnClickListener { recyclerView.addNewItem() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.preferencesLayout)
            startActivity(Intent(this, PreferencesActivity::class.java))
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    private val actionModeCallback = object: ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = true

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

        override fun onDestroyActionMode(mode: ActionMode?) {
            recyclerView.adapter.selection.clear()
            actionMode = null
        }

    }
}
