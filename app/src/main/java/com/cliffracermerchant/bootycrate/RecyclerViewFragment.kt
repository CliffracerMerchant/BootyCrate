/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.cliffracermerchant.bootycrate.databinding.MainActivityBinding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.kennyc.view.MultiStateView

/**
 * An fragment to display a SelectableExpandableRecyclerView to the user.
 *
 * RecyclerViewFragment is an abstract fragment whose main purpose is to dis-
 * play an instance of a ExpandableSelectableRecyclerView to the user. It has
 * two abstract properties, viewModel and recyclerView, that must be overrid-
 * den in subclasses with concrete implementations of ExpandableSelectableView-
 * Model and ExpandableSelectableRecyclerView respectively. Because Recycler-
 * ViewFragment's implementation of onViewCreated references its abstract
 * recyclerView property, it is important that subclasses override the recy-
 * clerView property and initialize it before calling super.onViewCreated,
 * or an exception will occur.
 *
 * The open property actionMode is the ActionMode instance used when the recy-
 * clerView has a selection. Subclasses can override this property with their
 * own instance of ActionMode if they wish to specialize this behavior.
 */
@Suppress("LeakingThis")
abstract class RecyclerViewFragment<Entity: ExpandableSelectableItem>:
    Fragment(), MainActivity.FragmentInterface
{
    protected abstract val viewModel: ExpandableSelectableItemViewModel<Entity>
    protected abstract var recyclerView: ExpandableSelectableRecyclerView<Entity>?
    protected open val actionMode = ActionMode()

    private var searchView: SearchView? = null
    private var searchIsActive = false
        set(value) {
            field = value
            val stateView = view as? MultiStateView ?: return
            val emptyMessage = stateView.getView(MultiStateView.ViewState.EMPTY) as? TextView ?: return
            emptyMessage.text = getString(if (value) R.string.no_search_results_message
                                          else R.string.empty_recycler_view_message,
                                               recyclerView?.collectionName)
        }
    private lateinit var sortModePrefKey: String
    private val searchWasActivePrefKey = "searchWasActive"

    init { setHasOptionsMenu(true) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        sortModePrefKey = getString(R.string.pref_sort, recyclerView!!.collectionName)
        val sortStr = prefs.getString(sortModePrefKey, ViewModelItem.Sort.Color.toString())
        recyclerView!!.apply {
            sort = ViewModelItem.Sort.fromString(sortStr)
            selection.itemsLiveData.observe(viewLifecycleOwner, actionMode)
            observeViewModel(viewLifecycleOwner)
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            val stateView = view as? MultiStateView ?: return@observe
            stateView.viewState = if (items.isNotEmpty()) MultiStateView.ViewState.CONTENT
                                  else                    MultiStateView.ViewState.EMPTY
        }

        searchIsActive = savedInstanceState?.getBoolean(searchWasActivePrefKey, false) ?: false

        val isActive = this.isActiveTemp ?: return
        val ui = this.uiTemp ?: return
        onActiveStateChanged(isActive, ui)
        isActiveTemp = null
        uiTemp = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
    }

    override fun onDetach() {
        super.onDetach()
        searchView = null
        recyclerView?.snackBarAnchor = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) return false
        return when (item.itemId) {
            R.id.delete_selected_menu_item -> {
                val view = this.view ?: return false
                val size = viewModel.selectedItems.value?.size ?: 0
                viewModel.deleteSelected()
                val text = getString(R.string.delete_snackbar_text, size)
                Snackbar.make(view, text, Snackbar.LENGTH_LONG)
                    .setAnchorView(recyclerView?.snackBarAnchor)
                    .setAction(R.string.delete_snackbar_undo_text) { viewModel.undoDelete() }
                    .addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onDismissed(a: Snackbar?, b: Int) = viewModel.emptyTrash()
                    }).show()
                true
            } R.id.share_menu_item -> {
                val recyclerView = this.recyclerView ?: return false
                val items = if (viewModel.selectedItems.value?.isNotEmpty() == true)
                                viewModel.selectedItems.value ?: emptyList()
                            else viewModel.items.value ?: emptyList()
                val anchor = recyclerView.snackBarAnchor ?: recyclerView
                ShareDialog(recyclerView.collectionName, items, anchor)
                    .show(childFragmentManager, null)
                true
            } R.id.select_all_menu_item -> {
                recyclerView?.selection?.addAll()
                true
            } R.id.color_option -> {
                recyclerView?.sort = ViewModelItem.Sort.Color
                item.isChecked = true
                saveSortingOption(); true
            } R.id.name_ascending_option -> {
                recyclerView?.sort = ViewModelItem.Sort.NameAsc
                item.isChecked = true
                saveSortingOption(); true
            } R.id.name_descending_option -> {
                recyclerView?.sort = ViewModelItem.Sort.NameDesc
                item.isChecked = true
                saveSortingOption(); true
            } R.id.amount_ascending_option -> {
                recyclerView?.sort = ViewModelItem.Sort.AmountAsc
                item.isChecked = true
                saveSortingOption(); true
            } R.id.amount_descending_option -> {
                recyclerView?.sort = ViewModelItem.Sort.AmountDesc
                item.isChecked = true
                saveSortingOption(); true
            } else -> false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(searchWasActivePrefKey, searchIsActive)
    }

    private fun saveSortingOption() {
        val context = this.context ?: return
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(sortModePrefKey, recyclerView?.sort.toString())
            .apply()
    }

    override fun showsOptionsMenu() = true
    override fun showsBottomAppBar() = true
    override fun showsCheckoutButton() = false
    override fun onBackPressed() = when {
        actionMode.isStarted -> { actionMode.finishAndClearSelection(); true }
        searchIsActive       -> { searchView?.isIconified = true; true }
        else                 -> false
    }

    private var isActiveTemp: Boolean? = null
    private var uiTemp: MainActivityBinding? = null
    @CallSuper override fun onActiveStateChanged(isActive: Boolean, ui: MainActivityBinding) {
        val recyclerView = this.recyclerView
        if (recyclerView == null) {
            // If recyclerView is null, the view probably hasn't been created yet. In
            // this case, we'll store the parameter values and call onActiveStateChanged
            // manually with the stored parameters at the end of onViewCreated.
            isActiveTemp = isActive
            uiTemp = ui
            return
        }

        if (!isActive) {
            actionMode.finish()
            actionMode.actionBar = null
            return
        }

        recyclerView.snackBarAnchor = ui.bottomAppBar
        actionMode.actionBar = ui.topActionBar
        searchView = ui.topActionBar.ui.searchView
        if (searchIsActive) ui.topActionBar.ui.searchView.performClick()

        if (recyclerView.selection.isNotEmpty)
            actionMode.onChanged(recyclerView.selection.items!!)

        ui.topActionBar.ui.searchView.apply {
            setOnCloseListener { searchIsActive = false; false }
            setOnSearchClickListener { searchIsActive = true }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    recyclerView.searchFilter = newText
                    return true
                }
            })
        }
        ui.topActionBar.changeSortMenu.findItem(when (recyclerView.sort) {
            ViewModelItem.Sort.Color ->      R.id.color_option
            ViewModelItem.Sort.NameAsc ->    R.id.name_ascending_option
            ViewModelItem.Sort.NameDesc ->   R.id.name_descending_option
            ViewModelItem.Sort.AmountAsc ->  R.id.amount_ascending_option
            ViewModelItem.Sort.AmountDesc -> R.id.amount_descending_option
        })?.isChecked = true
    }

    /**
     * The default ActionMode used by RecyclerViewFragment.
     *
     * RecyclerViewActionMode implements Observer<Int>, and is intended to
     * be used as an observer of the recyclerView.selection.sizeLiveData.
     * When the number of selected items increases above zero the action
     * mode will be started. If the number of selected items ever
     * decreases to zero, the action mode will be ended.
     *
     * Note that finish will not clear the selection in case ending the
     * action mode but not the selection is desired. To clear the selec-
     * tion and end the action mode, either clear the selection manually,
     * which will end the action mode, or call finishAndClearSelection.
     */
    open inner class ActionMode : RecyclerViewActionBar.ActionMode(), Observer<List<Entity>> {

        override fun onChanged(newList: List<Entity>) {
            if (newList.isEmpty() && actionMode.isStarted) actionMode.finish()
            else if (newList.isNotEmpty()) {
                actionMode.title = activity?.getString(R.string.action_mode_title, newList.size)
                actionMode.start()
            }
        }

        override fun onStart(actionBar: RecyclerViewActionBar) {
            actionBar.ui.searchView.isVisible = false
            actionBar.ui.changeSortButton.isActivated = true
        }

        override fun onFinish(actionBar: RecyclerViewActionBar) {
            actionBar.ui.searchView.isVisible = true
            actionBar.ui.changeSortButton.isActivated = false
        }

        fun finishAndClearSelection() = recyclerView?.selection?.clear()
    }
}