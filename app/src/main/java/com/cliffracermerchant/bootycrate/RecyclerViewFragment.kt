/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

/** An fragment to display a SelectableExpandableRecyclerView to the user.
 *
 *  RecyclerViewFragment is an abstract fragment whose main purpose is to dis-
 *  play an instance of a SelectableExpandableRecyclerView to the user. It has
 *  an abstract property recyclerView that must be overridden in subclasses
 *  with a concrete implementation of SelectableExpandableRecyclerView. Because
 *  RecyclerViewFragment's implementation of onViewCreated references its abs-
 *  tract recyclerView property, it is important that subclasses override the
 *  recyclerView property and initialize it before calling super.onViewCreated,
 *  or an exception will occur.
 *
 *  RecyclerViewFragment starts or finishes an action mode instance according
 *  to the size of the RecyclerView's selection. The ActionMode.Callback used
 *  with this action mode is the value of its property actionModeCallback. This
 *  property defaults to an instance of its own ActionModeCallback, but is open
 *  in case subclasses need to override the callback with their own.
 *
 *  RecyclerViewFragment manages the visual state of the floating action button
 *  and its onClickListeners according to whether an action mode is ongoing.
 *  While it automatically switches between two visual states for the FAB, sub-
 *  classes should set the drawables for the fabIconController member so that
 *  these visual states are not empty. It also switches the onClickListener of
 *  the FAB between the values of the abstract properties fabRegularOnClickList-
 *  ener and fabActionModeOnClickListener. Override these properties in sub-
 *  classes with the desired functionality. */
abstract class RecyclerViewFragment<Entity: ViewModelItem>: HideableFragment() {
    protected lateinit var mainActivity: MainActivity

    abstract val recyclerView: SelectableExpandableRecyclerView<Entity>

    protected var actionMode: ActionMode? = null
    protected open val actionModeCallback = ActionModeCallback()

    protected lateinit var fabIconController: AnimatedIconController
    protected abstract val fabRegularOnClickListener: View.OnClickListener?
    protected abstract val fabActionModeOnClickListener: View.OnClickListener?

    private val savedSelectionStateIdsKey: String get() = "${getString(recyclerView.collectionNameResId)}_selection_state_ids"
    private val savedSelectionStatePositionsKey: String get() = "${getString(recyclerView.collectionNameResId)}_selection_state_pos"
    private val wasActiveFragmentKey: String get() = "${getString(recyclerView.collectionNameResId)}_was_active"

    init { setHasOptionsMenu(true) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        fabIconController = AnimatedFabIconController(mainActivity.fab)
        recyclerView.snackBarAnchor = mainActivity.bottomAppBar
        recyclerView.selection.sizeLiveData.observe(viewLifecycleOwner, actionModeCallback)

        fabIconController.addTransition(fabIconController.addState("add"), fabIconController.addState("delete"),
            ContextCompat.getDrawable(mainActivity, R.drawable.fab_animated_add_to_delete_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(mainActivity, R.drawable.fab_animated_delete_to_add_icon) as AnimatedVectorDrawable)

        if (savedInstanceState == null) return

        val wasActiveFragment = savedInstanceState.getBoolean(wasActiveFragmentKey)
        if (wasActiveFragment) onHiddenChanged(false)
        else actionModeCallback.clearSelectionOnExit = false

        val selectionStateIds = savedInstanceState.getLongArray(savedSelectionStateIdsKey) ?: return
        val selectionStatePositions = savedInstanceState.getIntArray(savedSelectionStatePositionsKey) ?: return
        recyclerView.selection.restoreState(selectionStateIds, selectionStatePositions)
    }

    override fun onAboutToBeHidden() {
        super.onAboutToBeHidden()
        actionModeCallback.clearSelectionOnExit = false
        actionMode?.finish()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            fabIconController.setState("add", animate = false)
            mainActivity.fab.setOnClickListener(fabRegularOnClickListener)
            actionModeCallback.clearSelectionOnExit = true
            actionModeCallback.onChanged(recyclerView.selection.size)
        }
    }

    override fun setOptionsMenuItemsVisible(showing: Boolean) {
        if (!showing) return
        val menu = this.menu ?: return

        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
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
            else ->                          R.id.color_option
        }).isChecked = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) return false
        return when (item.itemId) {
            R.id.delete_all_menu_item -> {
                recyclerView.deleteAll(); true
            } R.id.export_menu_item -> { exportAsDialog(
                    context = mainActivity,
                    items = recyclerView.adapter.currentList,
                    insertBlankLineBetweenColors = recyclerView.sort == ViewModelItem.Sort.Color,
                    snackBarAnchor = mainActivity.bottomAppBar)
                true
            } R.id.color_option -> {
                recyclerView.sort = ViewModelItem.Sort.Color
                item.isChecked = true; true
            } R.id.name_ascending_option -> {
                recyclerView.sort = ViewModelItem.Sort.NameAsc
                item.isChecked = true; true
            } R.id.name_descending_option -> {
                recyclerView.sort = ViewModelItem.Sort.NameDesc
                item.isChecked = true; true
            } R.id.amount_ascending_option -> {
                recyclerView.sort = ViewModelItem.Sort.AmountAsc
                item.isChecked = true; true
            } R.id.amount_descending_option -> {
                recyclerView.sort = ViewModelItem.Sort.AmountDesc
                item.isChecked = true; true
            } else -> false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(wasActiveFragmentKey, !isHidden)

        if (recyclerView.selection.isEmpty) return
        val savedSelectionState = recyclerView.selection.currentState()
        outState.putLongArray(savedSelectionStateIdsKey, savedSelectionState.first)
        outState.putIntArray(savedSelectionStatePositionsKey, savedSelectionState.second)
    }

    /** The default ActionMode.Callback used by RecyclerViewFragment.
     *
     *  ActionModeCallback implements Observer<Int>, and is intended to be used as an
     *  observer of the recyclerView member's selection.sizeLiveData. With the member
     *  clearSelectionOnExit set to the default value of true, the callback will clear
     *  the recycler view selection when it exits.
     *
     *  The default action mode callback handles the visual state change and on-
     *  ClickListeners for the FAB in onCreateActionMode and onDestroyAction-
     *  Mode. Subclasses may wish to override ActionModeCallback with implemen-
     *  tations of onActionItemClicked and onPrepareActionMode. */
    open inner class ActionModeCallback : ActionMode.Callback, Observer<Int> {
        var clearSelectionOnExit = true

        override fun onChanged(newSize: Int) {
            if (newSize == 0) actionMode?.finish()
            else if (newSize > 0) {
                actionMode = actionMode ?: mainActivity.startSupportActionMode(this)
                actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =  false

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater?.inflate(R.menu.action_bar_menu, menu)
            menu.findItem(R.id.app_bar_search).isVisible = false
            menu.findItem(R.id.change_sorting_menu_item).isVisible = false
            mainActivity.fab.setOnClickListener(fabActionModeOnClickListener)
            fabIconController.setState("delete")
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu?) = false

        override fun onDestroyActionMode(mode: ActionMode) {
            if (clearSelectionOnExit) recyclerView.selection.clear()
            mainActivity.fab.setOnClickListener(fabRegularOnClickListener)
            fabIconController.setState("add")
            actionMode = null
        }
    }
}