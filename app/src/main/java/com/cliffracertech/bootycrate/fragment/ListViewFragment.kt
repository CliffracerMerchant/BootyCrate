/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.animation.LayoutTransition
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.MainActivity
import com.cliffracertech.bootycrate.database.*
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemListView
import com.cliffracertech.bootycrate.utils.recollectWhenStarted
import com.cliffracertech.bootycrate.utils.setPadding
import com.cliffracertech.bootycrate.viewmodel.ItemListViewModel
import com.google.android.material.snackbar.Snackbar
import com.kennyc.view.MultiStateView
import java.util.*

/**
 * A fragment to display an ExpandableItemListView to the user.
 *
 * ListViewFragment is an abstract fragment whose main purpose is to display an
 * instance of a ExpandableItemListView to the user. It has two abstract
 * properties, viewModel and listView, that must be overridden in subclasses
 * with concrete implementations of ItemListViewModel and ExpandableItemListView,
 * respectively. Because ListViewFragment's implementation of onViewCreated
 * references its abstract listView property, it is important that subclasses
 * override the listView property and initialize it before calling
 * super.onViewCreated, or an exception will occur.
 *
 * The value of the open property collectionName should be overridden in
 * subclasses with a value that describes what the collection of items
 * should be called in user facing strings.
 */
abstract class ListViewFragment<T: ListItem> :
    Fragment(), MainActivity.MainActivityFragment
{
    protected abstract val viewModel: ItemListViewModel<T>
    protected abstract val listView: ExpandableItemListView<T>?
    private var multiStateView: MultiStateView? = null
    protected open val collectionName = ""
    private var bottomAppBar: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        multiStateView = view as? MultiStateView
        multiStateView?.layoutTransition = LayoutTransition()
        (multiStateView?.getView(MultiStateView.ViewState.EMPTY) as? TextView)
            ?.text = getString(R.string.empty_list_message, collectionName)

        val content = multiStateView?.getView(MultiStateView.ViewState.CONTENT)
        (content as? ExpandableItemListView<*>)?.apply {
            onItemClick = viewModel::onItemClick
            onItemLongClick = viewModel::onItemLongClick
            onItemColorIndexChangeRequest = viewModel::onChangeItemColorIndexRequest
            onItemRenameRequest = viewModel::onRenameItemRequest
            onItemExtraInfoChangeRequest = viewModel::onChangeItemExtraInfoRequest
            onItemAmountChangeRequest = viewModel::onChangeItemAmountRequest
            onItemEditButtonClick = viewModel::onItemEditButtonClick
            onItemSwipe = viewModel::onItemSwipe
        }

        viewLifecycleOwner.recollectWhenStarted(viewModel.uiState) {
            val view = multiStateView ?: return@recollectWhenStarted
            when (it) {
                is ItemListViewModel.UiState.Loading ->
                    view.viewState = MultiStateView.ViewState.LOADING
                is ItemListViewModel.UiState.EmptyContent ->
                    view.viewState = MultiStateView.ViewState.EMPTY
                is ItemListViewModel.UiState.EmptySearchResults ->
                    view.viewState = MultiStateView.ViewState.ERROR
                is ItemListViewModel.UiState.Content<*> -> {
                    listView?.submitList(it.items as List<T>)
                    view.viewState = MultiStateView.ViewState.CONTENT
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        multiStateView = null
    }

    override fun onDetach() {
        super.onDetach()
        bottomAppBar = null
    }

    /** Open a ShareDialog.
     * @return whether the dialog was successfully started. */
    private fun shareList(): Boolean {
        val context = this.context ?: return false
        val selectionIsEmpty = viewModel.selectedItemCount.value == 0

        val contentState = viewModel.uiState as? ItemListViewModel.UiState.Content<T>
        val items = if (selectionIsEmpty) contentState?.items
                    else contentState?.items?.filter { it.isSelected }

        if (items.isNullOrEmpty()) {
            val anchor = bottomAppBar ?: view ?: return false
            val message = context.getString(R.string.empty_list_message, collectionName)
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

    @CallSuper override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
        bottomAppBar = if (isActive) activityUi.bottomAppBar
                       else          null
        listView?.apply {
            val bottomSheetPeekHeight = activityUi.bottomNavigationDrawer.peekHeight
            if (paddingBottom != bottomSheetPeekHeight)
                setPadding(bottom = bottomSheetPeekHeight)
        }
    }
}