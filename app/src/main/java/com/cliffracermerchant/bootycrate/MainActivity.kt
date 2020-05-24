package com.cliffracermerchant.bootycrate

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.*
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomappbar.BottomAppBar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private var actionMode: ActionMode? = null
    private var deleteIcon: Drawable? = null
    private var addIcon: Drawable? = null

    // This is used to make sure that the selection state
    // is not restored on a fresh restart of the app
    companion object { var ranOnce = false }

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

        val viewModel = ViewModelProvider(this).get(InventoryViewModel::class.java)
        recyclerView.setViewModel(this, viewModel)
        recyclerView.selection.sizeLiveData.observe(this, Observer { newSize ->
            if (newSize == 0 && actionMode != null) actionMode?.finish()
            else if (newSize > 0) {
                if (actionMode == null) actionMode = startSupportActionMode(actionModeCallback)
                actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        })

        floating_action_button.setImageDrawable(addIcon)
        floating_action_button.setOnClickListener { recyclerView.addNewItem() }
        bottom_navigation_bar.itemIconTintList = null

        if (!ranOnce) { ranOnce = true; return }
        val selectionStateFile = File(cacheDir, "selection_state")
        if (selectionStateFile.exists()) {
            val selectionStateString = selectionStateFile.readText().split(',')
            // size - 1 is to leave off the trailing comma
            val selectionState = IntArray(selectionStateString.size - 1) { i ->
                selectionStateString[i].toInt()
            }
            recyclerView.selection.restoreState(selectionState)
        }
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
        val selectionStateFile = File(cacheDir, "selection_state")
        val writer = selectionStateFile.writer()
        for (id in recyclerView.selection.saveState())
            writer.write("$id,")
        writer.close()
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val selectionStateFile = File(cacheDir, "selection_state")
        if (selectionStateFile.exists()) {
            val selectionStateString = selectionStateFile.readText().split(',')
            for (idString in selectionStateString) Log.d("savestate", idString)
            val selectionState = IntArray(selectionStateString.size) { idString ->
                idString//.toInt()
            }
            recyclerView.selection.restoreState(selectionState)
            selectionStateFile.delete()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
            floating_action_button.setOnClickListener{ recyclerView.deleteItems(*recyclerView.selection.saveState()) }
            floating_action_button.setImageDrawable(deleteIcon)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = true

        override fun onDestroyActionMode(mode: ActionMode?) {
            recyclerView.selection.clear()
            floating_action_button.setOnClickListener { recyclerView.addNewItem() }
            floating_action_button.setImageDrawable(addIcon)
            actionMode = null
        }
    }

    private fun onPreferencesMenuItemClicked() =
        startActivity(Intent(this, PreferencesActivity::class.java))
}