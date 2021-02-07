/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.kennyc.view.MultiStateView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*

/** An fragment to display a SelectableExpandableRecyclerView to the user.
 *
 *  RecyclerViewFragment is an abstract fragment whose main purpose is to dis-
 *  play an instance of a ExpandableSelectableRecyclerView to the user. It has
 *  an abstract property recyclerView that must be overridden in subclasses
 *  with a concrete implementation of ExpandableSelectableRecyclerView. Because
 *  RecyclerViewFragment's implementation of onViewCreated references its abs-
 *  tract recyclerView property, it is important that subclasses override the
 *  recyclerView property and initialize it before calling super.onViewCreated,
 *  or an exception will occur.
 *
 *  The open property actionMode is the ActionMode instance used when the recy-
 *  clerView has a selection. Subclasses can override this property with their
 *  own instance of ActionMode if they wish to specialize this behavior. */
@Suppress("LeakingThis")
abstract class RecyclerViewFragment<Entity: ExpandableSelectableItem>(isActive: Boolean = false) :
        MainActivityFragment(isActive) {

    protected lateinit var activity: MainActivity
    abstract val recyclerView: ExpandableSelectableRecyclerView<Entity>
    open val actionMode = RecyclerViewActionMode()
    protected lateinit var sortModePrefKey: String
    private var searchIsActive = false
        set(value) {
            field = value
            val stateView = view as? MultiStateView ?: return
            val emptyMessage = stateView.getView(MultiStateView.ViewState.EMPTY) as? TextView ?: return
            emptyMessage.text = if (value) activity.getString(R.string.no_search_results_message)
                                else activity.getString(R.string.empty_recycler_view_message,
                                                        recyclerView.collectionName)
        }

    init { setHasOptionsMenu(true) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity = requireActivity() as MainActivity
        sortModePrefKey = activity.getString(R.string.pref_sort, recyclerView.collectionName)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sortStr = prefs.getString(sortModePrefKey, ViewModelItem.Sort.Color.toString())
        recyclerView.sort = ViewModelItem.Sort.fromString(sortStr)
        recyclerView.snackBarAnchor = activity.bottomAppBar
        recyclerView.selection.itemsLiveData.observe(viewLifecycleOwner, actionMode)
        recyclerView.viewModel.items.observe(viewLifecycleOwner) { items ->
            val stateView = view as? MultiStateView ?: return@observe
            stateView.viewState = if (items.isNotEmpty()) MultiStateView.ViewState.CONTENT
                                  else                    MultiStateView.ViewState.EMPTY
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActiveStateChanged(active: Boolean) {
        super.onActiveStateChanged(active)
        if (!active) actionMode.finish()
    }

    override fun setOptionsMenuItemsVisible(showing: Boolean) {
        if (!showing) return
        val menu = this.menu ?: return
        val actionBar = activity.supportActionBar
        actionBar?.run {
            actionMode.init(this, menu, activity.topActionBar.customTitle)
            if (recyclerView.selection.isNotEmpty)
                actionMode.onChanged(recyclerView.selection.items!!)
        }

        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.setOnQueryTextFocusChangeListener { v, hasFocus -> searchIsActive = hasFocus }
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?): Boolean {
                recyclerView.searchFilter = newText
                return true
            }})
        menu.findItem(when (recyclerView.sort) {
            ViewModelItem.Sort.Color ->      R.id.color_option
            ViewModelItem.Sort.NameAsc ->    R.id.name_ascending_option
            ViewModelItem.Sort.NameDesc ->   R.id.name_descending_option
            ViewModelItem.Sort.AmountAsc ->  R.id.amount_ascending_option
            ViewModelItem.Sort.AmountDesc -> R.id.amount_descending_option
        }).isChecked = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.isChecked || !isActive) return false
        return when (item.itemId) {
            R.id.delete_selected_menu_item -> {
                recyclerView.deleteSelectedItems()
                true
            } R.id.share_menu_item -> {
                Dialog.shareList(items = if (recyclerView.selection.size != 0)
                                             recyclerView.selection.items!!
                                         else recyclerView.adapter.currentList)
                true
            } R.id.select_all_menu_item -> {
                recyclerView.selection.addAll()
                true
            } R.id.color_option -> {
                recyclerView.sort = ViewModelItem.Sort.Color
                item.isChecked = true
                saveSortingOption(); true
            } R.id.name_ascending_option -> {
                recyclerView.sort = ViewModelItem.Sort.NameAsc
                item.isChecked = true
                saveSortingOption(); true
            } R.id.name_descending_option -> {
                recyclerView.sort = ViewModelItem.Sort.NameDesc
                item.isChecked = true
                saveSortingOption(); true
            } R.id.amount_ascending_option -> {
                recyclerView.sort = ViewModelItem.Sort.AmountAsc
                item.isChecked = true
                saveSortingOption(); true
            } R.id.amount_descending_option -> {
                recyclerView.sort = ViewModelItem.Sort.AmountDesc
                item.isChecked = true
                saveSortingOption(); true
            } else -> false
        }
    }

    private fun saveSortingOption() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = prefs.edit()
        editor.putString(sortModePrefKey, recyclerView.sort.toString())
        editor.apply()
    }

    /** The default ActionMode used by RecyclerViewFragment.
     *
     *  RecyclerViewActionMode implements Observer<Int>, and is intended to be used
     *  as an observer of the recyclerView.selection.sizeLiveData. When the number of
     *  selected items increases above zero and the RecyclerViewFragment is the
     *  active one, the action mode will be started. If the number of selected items
     *  ever decreases to zero, the action mode will be ended.
     *
     *  Note that finish will not clear the selection in case ending the action mode
     *  but not the selection is desired. To clear the selection and end the action
     *  mode, either clear the selection manually, which will end the action mode, or
     *  call finishAndClearSelection. */
    open inner class RecyclerViewActionMode() : ActionMode(), Observer<List<Entity>> {

        override fun onChanged(newList: List<Entity>) {
            if (newList.isEmpty()) actionMode.finish()
            else if (newList.isNotEmpty() && isActive) {
                actionMode.title = activity.getString(R.string.action_mode_title, newList.size)
                actionMode.start()
            }
        }

        override fun onStart(actionBar: ActionBar, menu: Menu, titleView: TextView?) {
            menu.findItem(R.id.app_bar_search)?.isVisible = false
            menu.findItem(R.id.change_sorting_menu_item)?.isVisible = false
            menu.findItem(R.id.delete_selected_menu_item)?.isVisible = true
        }

        override fun onFinish(actionBar: ActionBar, menu: Menu, titleView: TextView?) {
            menu.findItem(R.id.app_bar_search)?.isVisible = true
            menu.findItem(R.id.change_sorting_menu_item)?.isVisible = true
            menu.findItem(R.id.delete_selected_menu_item)?.isVisible = false
        }

        fun finishAndClearSelection() = recyclerView.selection.clear()
    }
}