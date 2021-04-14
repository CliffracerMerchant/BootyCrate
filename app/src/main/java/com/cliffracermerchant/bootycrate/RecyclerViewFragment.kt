/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.cliffracermerchant.bootycrate.databinding.MainActivityBinding
import com.kennyc.view.MultiStateView
import java.util.*

/**
 * An fragment to display a ExpandableSelectableRecyclerView to the user.
 *
 * RecyclerViewFragment is an abstract fragment whose main purpose is to
 * display an instance of a ExpandableSelectableRecyclerView to the user.
 * It has two abstract properties, viewModel and recyclerView, that must
 * be overridden in subclasses with concrete implementations of Expandable-
 * SelectableViewModel and ExpandableSelectableRecyclerView, respectively.
 * Because RecyclerViewFragment's implementation of onViewCreated referen-
 * ces its abstract recyclerView property, it is important that subclasses
 * override the recyclerView property and initialize it before calling
 * super.onViewCreated, or an exception will occur.
 *
 * The value of the open property actionModeCallback will be used as the
 * callback when an action mode is started. Subclasses can override it
 * with a descendant of SelectionActionModeCallback if they wish to per-
 * form work when the action action mde starts or finishes.
 */
@Suppress("LeakingThis")
abstract class RecyclerViewFragment<Entity: ExpandableSelectableItem> :
    Fragment(), MainActivity.MainActivityFragment
{
    protected abstract val viewModel: ExpandableSelectableItemViewModel<Entity>
    protected abstract val recyclerView: ExpandableSelectableRecyclerView<Entity>?
    protected open val actionModeCallback = SelectionActionModeCallback()
    private lateinit var sortModePrefKey: String

    private var actionBar: RecyclerViewActionBar? = null
    private val actionModeIsStarted get() = actionBar?.actionMode?.callback == actionModeCallback
    private val searchIsActive get() = actionBar?.activeSearchQuery != null

    init { setHasOptionsMenu(true) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        sortModePrefKey = getString(R.string.pref_sort, recyclerView!!.collectionName)
        val sortStr = prefs.getString(sortModePrefKey, ViewModelItem.Sort.Color.toString())
        recyclerView?.apply {
            sort = ViewModelItem.Sort.fromString(sortStr)
            selection.itemsLiveData.observe(viewLifecycleOwner, ::onSelectionChanged)
            observeViewModel(viewLifecycleOwner)
        }

        val multiStateView = view as? MultiStateView
        val emptyTextView = multiStateView?.getView(MultiStateView.ViewState.EMPTY) as? TextView
        emptyTextView?.text = getString(R.string.empty_recycler_view_message, recyclerView?.collectionName)

        viewModel.items.observe(viewLifecycleOwner) { items ->
            val stateView = view as? MultiStateView ?: return@observe
            stateView.viewState = when { items.isNotEmpty() -> MultiStateView.ViewState.CONTENT
                                         searchIsActive ->     MultiStateView.ViewState.ERROR
                                         else ->               MultiStateView.ViewState.EMPTY }
        }

        val isActive = this.isActiveTemp ?: return
        val activityUi = this.activityUiTemp ?: return
        onActiveStateChanged(isActive, activityUi)
        isActiveTemp = null
        activityUiTemp = null
    }

    override fun onDetach() {
        super.onDetach()
        actionBar = null
        recyclerView?.snackBarAnchor = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.delete_selected_menu_item -> deleteSelectedItems()
        R.id.share_menu_item -> shareList()
        R.id.select_all_menu_item -> {  recyclerView?.selection?.addAll(); true }
        R.id.color_option -> { saveSortingOption(ViewModelItem.Sort.Color, item) }
        R.id.name_ascending_option -> { saveSortingOption(ViewModelItem.Sort.NameAsc, item) }
        R.id.name_descending_option -> { saveSortingOption(ViewModelItem.Sort.NameDesc, item) }
        R.id.amount_ascending_option -> { saveSortingOption(ViewModelItem.Sort.AmountAsc, item) }
        R.id.amount_descending_option -> { saveSortingOption(ViewModelItem.Sort.AmountDesc, item) }
        else -> false
    }

    /** Open a ShareDialog.
     * @return whether the dialog was successfully started. */
    private fun shareList(): Boolean {
        val context = this.context ?: return false
        val recyclerView = this.recyclerView ?: return false
        val selectionIsEmpty = viewModel.selectedItems.value?.isEmpty() ?: true
        val items = if (!selectionIsEmpty) viewModel.selectedItems.value ?: emptyList()
                    else                   viewModel.items.value ?: emptyList()
        if (items.isEmpty()) return false

        val locale = context.resources.configuration.locale
        val collectionName = recyclerView.collectionName.toLowerCase(locale)

        val stringResId = if (selectionIsEmpty) R.string.share_whole_list_title
                          else                  R.string.share_selected_items_title
        val messageTitle = context.getString(stringResId, collectionName)

        var message = ""
        for (i in 0 until items.size - 1)
            message += items[i].toString() + "\n"
        if (items.isNotEmpty())
            message += items.last().toString()

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, messageTitle))
        return true
    }

    private fun deleteSelectedItems(): Boolean {
        val recyclerView = this.recyclerView ?: return false
        val size = viewModel.selectedItems.value?.size ?: 0
        viewModel.deleteSelected()
        recyclerView.showDeletedItemsSnackBar(size)
        return true
    }

    /** Set the recyclerView's sort to @param sort, check the @param
     * sortMenuItem, and save the sort to sharedPreferences.
     * @return whether the option was successfully saved to preferences. */
    private fun saveSortingOption(sort: ViewModelItem.Sort, sortMenuItem: MenuItem) : Boolean {
        recyclerView?.sort = sort
        sortMenuItem.isChecked = true
        val context = this.context ?: return false
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(sortModePrefKey, recyclerView?.sort.toString()).apply()
        return true
    }

    private fun onSelectionChanged(newList: List<Entity>) {
        if (newList.isNotEmpty()) {
            if (!actionModeIsStarted)
                actionBar?.startActionMode(actionModeCallback)
            else actionBar?.actionMode?.title = actionModeTitle(newList.size)
        } else actionBar?.actionMode?.finish()
    }

    override fun onBackPressed() = when {
        actionModeIsStarted  -> { recyclerView?.selection?.clear(); true }
        searchIsActive       -> { actionBar?.activeSearchQuery = null; true }
        else                 -> false
    }

    private var isActiveTemp: Boolean? = null
    private var activityUiTemp: MainActivityBinding? = null
    @CallSuper override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
        val recyclerView = this.recyclerView
        if (recyclerView == null) {
            // If recyclerView is null, the view probably hasn't been created yet. In
            // this case, we'll store the parameter values and call onActiveStateChanged
            // manually with the stored parameters at the end of onViewCreated.
            isActiveTemp = isActive
            activityUiTemp = activityUi
            return
        }
        if (!isActive) actionBar = null
        else {
            recyclerView.snackBarAnchor = activityUi.bottomAppBar
            actionBar = activityUi.actionBar
            activityUi.actionBar.onSearchQueryChangedListener = { newText ->
                recyclerView.searchFilter = newText.toString()
            }

            val actionModeCallback = if (recyclerView.selection.isEmpty) null
                                     else this.actionModeCallback
            val activeSearchQuery = if (viewModel.searchFilter.isNullOrBlank()) null
                                    else viewModel.searchFilter
            activityUi.actionBar.transition(
                activeActionModeCallback = actionModeCallback,
                activeSearchQuery = activeSearchQuery)

            activityUi.actionBar.changeSortMenu.findItem(when (recyclerView.sort) {
                ViewModelItem.Sort.Color ->      R.id.color_option
                ViewModelItem.Sort.NameAsc ->    R.id.name_ascending_option
                ViewModelItem.Sort.NameDesc ->   R.id.name_descending_option
                ViewModelItem.Sort.AmountAsc ->  R.id.amount_ascending_option
                ViewModelItem.Sort.AmountDesc -> R.id.amount_descending_option
            })?.isChecked = true
        }
    }

    open inner class SelectionActionModeCallback : RecyclerViewActionBar.ActionModeCallback {
        @CallSuper override fun onStart(
            actionMode: RecyclerViewActionBar.ActionMode,
            actionBar: RecyclerViewActionBar
        ) {
            val selectionSize = recyclerView?.selection?.size ?: 0
            actionMode.title = actionModeTitle(selectionSize)
        }
    }

    private fun actionModeTitle(selectionSize: Int) =
        activity?.getString(R.string.action_mode_title, selectionSize) ?: ""
}