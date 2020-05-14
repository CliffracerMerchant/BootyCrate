package com.cliffracermerchant.stuffcrate

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private var actionMode: ActionMode? = null
    private var deleteIcon: Drawable? = null// = getDrawable(R.drawable.ic_delete_black_24dp)
    private var addIcon: Drawable? = null// = getDrawable(android.R.drawable.ic_input_add)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getDefaultSharedPreferences(this)
        val darkThemeActive = prefs.getBoolean(getString(R.string.pref_dark_theme_active), false)
        setTheme(if (darkThemeActive) R.style.DarkTheme
                 else                 R.style.LightTheme)

        deleteIcon = getDrawable(R.drawable.ic_delete_black_24dp)
        addIcon = getDrawable(android.R.drawable.ic_input_add)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_dashboard_black_24dp)

        val viewModel = ViewModelProvider(this).get(InventoryViewModel::class.java)
        recyclerView.setViewModel(this, viewModel)
        recyclerView.adapter.selection.sizeLiveData.observe(this, Observer { newSize ->
            if (newSize == 0 && actionMode != null) actionMode?.finish()
            else if (newSize > 0) {
                if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback)
                actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        })

        floatingActionButton.setOnClickListener { recyclerView.addNewItem() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.preferences_menu_item) onPreferencesMenuItemClicked()
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    private val actionModeCallback = object: ActionMode.Callback {
        private var addToShoppingListButton: MenuItem? = null
        private var moveToOtherInventoryButton: MenuItem? = null
        private var search: MenuItem? = null

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.preferences_menu_item) onPreferencesMenuItemClicked()
            return true
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.action_bar_menu, menu)
            addToShoppingListButton = menu?.findItem(R.id.add_to_shopping_list_button)
            moveToOtherInventoryButton = menu?.findItem(R.id.move_to_other_inventory)
            search = menu?.findItem(R.id.app_bar_search)
            addToShoppingListButton?.isVisible = true
            moveToOtherInventoryButton?.isVisible = true
            search?.isVisible = false
            floatingActionButton.setOnClickListener{ recyclerView.deleteItems(*recyclerView.selectionState().toLongArray()) }
            floatingActionButton.setImageDrawable(deleteIcon)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = true

        override fun onDestroyActionMode(mode: ActionMode?) {
            recyclerView.adapter.selection.clear()
            //addToShoppingListButton?.isVisible = false
            //moveToOtherInventoryButton?.isVisible = false
            //search?.isVisible = true
            floatingActionButton.setOnClickListener { recyclerView.addNewItem() }
            floatingActionButton.setImageDrawable(addIcon)
            actionMode = null
        }
    }

    private fun onPreferencesMenuItemClicked() =
        startActivity(Intent(this, PreferencesActivity::class.java))
}
