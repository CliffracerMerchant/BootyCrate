/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.cliffracertech.bootycrate.*
import com.cliffracertech.bootycrate.activity.MainActivity
import com.cliffracertech.bootycrate.activity.*
import com.cliffracertech.bootycrate.database.*
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.recyclerview.ExpandableSelectableRecyclerView
import com.cliffracertech.bootycrate.view.RecyclerViewActionBar
import com.google.android.material.snackbar.Snackbar
import com.kennyc.view.MultiStateView
import java.util.*

/**
 * A fragment to display an ExpandableSelectableRecyclerView to the user.
 *
 * RecyclerViewFragment is an abstract fragment whose main purpose is to
 * display an instance of a ExpandableSelectableRecyclerView to the user. It
 * has two abstract properties, viewModel and recyclerView, that must be
 * overridden in subclasses with concrete implementations of
 * ExpandableSelectableViewModel and ExpandableSelectableRecyclerView,
 * respectively. Because RecyclerViewFragment's implementation of onViewCreated
 * references its abstract recyclerView property, it is important that
 * subclasses override the recyclerView property and initialize it before
 * calling super.onViewCreated, or an exception will occur.
 *
 * The value of the open property collectionName should be overridden in
 * subclasses with a value that describes what the collection of items
 * should be called in user facing strings.
 *
 * The value of the open property actionModeCallback will be used as the
 * callback when an action mode is started. Subclasses can override it with a
 * with a descendant of SelectionActionModeCallback if they wish to perform
 * work when the action action mode starts or finishes.
 */
@Suppress("LeakingThis")
abstract class RecyclerViewFragment<T: BootyCrateItem> :
    Fragment(), MainActivity.MainActivityFragment
{
    protected abstract val viewModel: BootyCrateViewModel<T>
    protected abstract val recyclerView: ExpandableSelectableRecyclerView<T>?
    protected open val collectionName = ""
    protected open val actionModeCallback = SelectionActionModeCallback()
    private lateinit var sortModePrefKey: String

    private var actionBar: RecyclerViewActionBar? = null
    private val actionModeIsStarted get() = actionBar?.actionMode?.callback == actionModeCallback
    private val searchIsActive get() = actionBar?.activeSearchQuery != null

    init { setHasOptionsMenu(true) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        sortModePrefKey = getString(R.string.pref_sort, collectionName)
        val sortStr = prefs.getString(sortModePrefKey, BootyCrateItemSort.Color.toString())
        recyclerView?.apply {
            sort = BootyCrateItemSort.fromString(sortStr)
            selection.sizeLiveData.observe(viewLifecycleOwner, ::onSelectionSizeChanged)
            observeViewModel(viewLifecycleOwner)
        }

        val multiStateView = view as? MultiStateView
        val emptyTextView = multiStateView?.getView(MultiStateView.ViewState.EMPTY) as? TextView
        emptyTextView?.text = getString(R.string.empty_recycler_view_message, collectionName)

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
        R.id.color_option -> { saveSortingOption(BootyCrateItemSort.Color, item) }
        R.id.name_ascending_option -> { saveSortingOption(BootyCrateItemSort.NameAsc, item) }
        R.id.name_descending_option -> { saveSortingOption(BootyCrateItemSort.NameDesc, item) }
        R.id.amount_ascending_option -> { saveSortingOption(BootyCrateItemSort.AmountAsc, item) }
        R.id.amount_descending_option -> { saveSortingOption(BootyCrateItemSort.AmountDesc, item) }
        else -> false
    }

    /** Open a ShareDialog.
     * @return whether the dialog was successfully started. */
    private fun shareList(): Boolean {
        val context = this.context ?: return false
        val selectionIsEmpty = (viewModel.selectedItemCount.value ?: 0) == 0
        val items = if (selectionIsEmpty) viewModel.items.value
                    else viewModel.items.value?.filter { it.isSelected }
        if (items?.isEmpty() != false) {
            val anchor = recyclerView?.snackBarAnchor ?: view ?: return false
            val message = context.getString(R.string.empty_recycler_view_message, collectionName)
            Snackbar.make(context, anchor, message, Snackbar.LENGTH_LONG)
                .setAnchorView(anchor).show()
            return false
        }

        val collectionName = collectionName.lowercase(Locale.getDefault())
        val stringResId = if (selectionIsEmpty) R.string.share_whole_list_title
                          else                  R.string.share_selected_items_title
        val messageTitle = context.getString(stringResId, collectionName)

        var message = ""
        for (i in 0 until items.size - 1)
            message += items[i].toUserFacingString() + "\n"
        message += items.last().toUserFacingString()

        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, message)
        intent.type = "text/plain"
        context.startActivity(Intent.createChooser(intent, messageTitle))
        return true
    }

    private fun deleteSelectedItems(): Boolean {
        val recyclerView = this.recyclerView ?: return false
        val size = viewModel.selectedItemCount.value ?: 0
        viewModel.deleteSelected()
        recyclerView.showDeletedItemsSnackBar(size)
        return true
    }

    /** Set the recyclerView's sort to @param sort, check the @param
     * sortMenuItem, and save the sort to sharedPreferences.
     * @return whether the option was successfully saved to preferences. */
    private fun saveSortingOption(sort: BootyCrateItemSort, sortMenuItem: MenuItem) : Boolean {
        recyclerView?.sort = sort
        sortMenuItem.isChecked = true
        val context = this.context ?: return false
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(sortModePrefKey, recyclerView?.sort.toString()).apply()
        return true
    }

    private fun onSelectionSizeChanged(newSize: Int) {
        if (newSize > 0) {
            if (!actionModeIsStarted)
                actionBar?.startActionMode(actionModeCallback)
            else actionBar?.actionMode?.title = actionModeTitle(newSize)
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
        if (!isActive) {
            actionBar = null
            activityUi.actionBar.onSearchQueryChangedListener = null
        } else {
            recyclerView.snackBarAnchor = activityUi.bottomAppBar
            actionBar = activityUi.actionBar
            activityUi.actionBar.onSearchQueryChangedListener = { newText ->
                recyclerView.searchFilter = newText.toString()
            }

            val actionModeCallback = if (recyclerView.selection.isEmpty) null
                                     else this.actionModeCallback
            val activeSearchQuery = if (viewModel.searchFilter.isBlank()) null
                                    else viewModel.searchFilter
            activityUi.actionBar.transition(
                activeActionModeCallback = actionModeCallback,
                activeSearchQuery = activeSearchQuery)

            activityUi.actionBar.changeSortMenu.findItem(when (recyclerView.sort) {
                BootyCrateItemSort.Color -> R.id.color_option
                BootyCrateItemSort.NameAsc -> R.id.name_ascending_option
                BootyCrateItemSort.NameDesc -> R.id.name_descending_option
                BootyCrateItemSort.AmountAsc -> R.id.amount_ascending_option
                BootyCrateItemSort.AmountDesc -> R.id.amount_descending_option
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