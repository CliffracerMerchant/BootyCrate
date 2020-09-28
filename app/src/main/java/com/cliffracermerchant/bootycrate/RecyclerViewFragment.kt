/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

/** An fragment to display a BootyCrateRecyclerView to the user.
 *
 *  RecyclerViewFragment is an abstract fragment whose main purpose is to dis-
 *  play an instance of a BootyCrateRecyclerView to the user. It has an
 *  abstract property recyclerView that must be overridden in subclasses with a
 *  concrete implementation of BootyCrateRecyclerView. Because RecyclerViewFrag-
 *  ment's implementation of onViewCreated references its abstract recyclerView
 *  property, it is important that subclasses override the recyclerView pro-
 *  perty and initialize it before calling super.onViewCreated, or an exception
 *  will occur.
 *
 *  RecyclerViewFragment starts or finishes an action mode instance according
 *  to the size of the RecyclerView's selection. The ActionMode.Callback used
 *  with this action mode is the value of its property actionModeCallback. This
 *  property defaults to an instance of its own ActionModeCallback, but is open
 *  in case subclasses need to override or replace the callback with their own.
 *
 *  RecyclerViewFragment manages the visual state of the floating action button
 *  and its onClickListeners according to whether an action mode is ongoing.
 *  While it automatically switches between two visual states for the FAB, sub-
 *  classes should set the drawables for the fabIconController member so that
 *  these visual states are not empty. It also switches the onClickListener of
 *  the FAB between the values of the abstract properties fabRegularOnClickList-
 *  ener and fabActionModeOnClickListener. Override these properties in sub-
 *  classes with the desired functionality.
 *
 *  The interface through which MainActivity interacts with RecyclerViewFrag-
 *  ments consists of the functions enable and disable. Enable will be called
 *  by the MainActivity when the fragment is becoming the active one (i.e.
 *  visible to the user), and disable will be called when the fragment is being
 *  hidden. Both of these functions can be overridden in subclasses (though the
 *  default implementation should always be called in overrides) in case fur-
 *  ther enable or disable functionality is required. */
abstract class RecyclerViewFragment<Entity: ViewModelItem>: Fragment() {
    protected lateinit var mainActivity: MainActivity
    protected lateinit var menu: Menu
    abstract val recyclerView: SelectableExpandableRecyclerView<Entity>

    private var actionMode: ActionMode? = null
    protected open val actionModeCallback = ActionModeCallback()

    protected lateinit var fabIconController: AnimatedIconController
    protected abstract val fabRegularOnClickListener: View.OnClickListener?
    protected abstract val fabActionModeOnClickListener: View.OnClickListener?

    private var savedSelectionState: List<Pair<Long, Int>>? = null

    init { setHasOptionsMenu(true) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        fabIconController = AnimatedFabIconController(mainActivity.fab)
        recyclerView.snackBarAnchor = mainActivity.bottomAppBar

        recyclerView.selection.sizeLiveData.observe(viewLifecycleOwner) { newSize ->
            Log.d("checkeditems", "selection size = $newSize")
            if (newSize == 0) actionMode?.finish()
            else if (newSize > 0) {
                actionMode = actionMode ?: mainActivity.startSupportActionMode(actionModeCallback)
                actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        }
    }

    open fun enable() {
        fabIconController.setState("add", animate = false)
        mainActivity.fab.setOnClickListener(fabRegularOnClickListener)
        savedSelectionState?.let { recyclerView.selection.restoreState(it) }
        savedSelectionState = null
    }

    open fun disable() {
        savedSelectionState = recyclerView.selection.currentState()
        mainActivity.fab.setOnClickListener(null)
        mainActivity.checkoutBtn.setOnClickListener(null)
        actionMode?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menu.size() == 0) inflater.inflate(R.menu.action_bar_menu, menu)
        this.menu = menu
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?): Boolean {
                recyclerView.searchFilter = newText
                return true
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) =
        initOptionsMenuSort(menu)

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
            } else -> mainActivity.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        disable()
        super.onSaveInstanceState(outState)
    }

    private fun initOptionsMenuSort(menu: Menu) {
        menu.findItem(when (recyclerView.sort) {
            ViewModelItem.Sort.Color ->      R.id.color_option
            ViewModelItem.Sort.NameAsc ->    R.id.name_ascending_option
            ViewModelItem.Sort.NameDesc ->   R.id.name_descending_option
            ViewModelItem.Sort.AmountAsc ->  R.id.amount_ascending_option
            ViewModelItem.Sort.AmountDesc -> R.id.amount_descending_option
            else ->                           R.id.color_option }).isChecked = true
    }

    /** The default ActionMode.Callback used by RecyclerViewFragment.
     *
     *  The default action mode callback handles the visual state change and on-
     *  ClickListeners for the FAB in onCreateActionMode and onDestroyAction-
     *  Mode. Subclasses may wish to override ActionModeCallback with implemen-
     *  tations of onActionItemClicked and onPrepareActionMode.*/
    open inner class ActionModeCallback : ActionMode.Callback {

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater?.inflate(R.menu.action_bar_menu, menu)
            mainActivity.fab.setOnClickListener(fabActionModeOnClickListener)
            fabIconController.setState("delete")
            initOptionsMenuSort(menu)
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu?) = false

        override fun onDestroyActionMode(mode: ActionMode) {
            recyclerView.selection.clear()
            mainActivity.fab.setOnClickListener(fabRegularOnClickListener)
            fabIconController.setState("add")
            initOptionsMenuSort(this@RecyclerViewFragment.menu)
            actionMode = null
        }
    }
}