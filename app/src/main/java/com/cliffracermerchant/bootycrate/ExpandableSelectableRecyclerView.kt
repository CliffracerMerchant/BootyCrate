/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

/** A ViewModelRecyclerView subclass that enables multi-selection and expansion of items.
 *
 *  ExpandableSelectableRecyclerView extends ViewModelRecyclerView via the use
 *  of an ExpandableSelectableItemViewModel along with the new functions delete-
 *  SelectedItems. Its member selection provides an interface for manipulating
 *  the recycler view selection (see the documentation for the inner class Sel-
 *  ection for more details) as well as the function setExpandedItem to change
 *  or set to null the currently expanded item. It also utilizes its own custom
 *  view holder to enforce the use of an ExpandableSelectableItemView, and a
 *  custom adapter that in turn enforces the use of ExpandableSelectableItem-
 *  ViewHolder.
 *
 *  Like its parent class, ExpandableSelectableRecyclerView requires the func-
 *  tion finishInit to be called in order to provide it with an instance of
 *  ExpandableSelectableItemViewModel. ExpandableSelectableRecyclerView's
 *  finishInit function, while not an override of ViewModelRecyclerView's ver-
 *  sion due to requiring a different function signature, is designed to call
 *  ViewModelRecyclerView's version to prevent the implementing activity or
 *  fragment from needing to remember to call both. */
abstract class ExpandableSelectableRecyclerView<Entity: ExpandableSelectableItem>(
    context: Context,
    attrs: AttributeSet
) : ViewModelRecyclerView<Entity>(context, attrs) {

    abstract override val adapter: ExpandableSelectableItemAdapter<out ExpandableSelectableItemViewHolder>
    private lateinit var viewModel: ExpandableSelectableItemViewModel<Entity>
    private val itemAnimator = ExpandableItemAnimator(this)

    val selection = Selection()

    init {
        addItemDecoration(ItemSpacingDecoration(context))
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        setItemAnimator(itemAnimator)
    }

    fun finishInit(owner: LifecycleOwner, viewModel: ExpandableSelectableItemViewModel<Entity>) {
        this.viewModel = viewModel
        adapter.registerAdapterDataObserver(itemAnimator.observer)
        super.finishInit(owner, viewModel)
    }

    open fun deleteSelectedItems() {
        val size = selection.size
        viewModel.deleteSelected()
        val text = context.getString(R.string.delete_snackbar_text, size)
        Snackbar.make(this, text, Snackbar.LENGTH_LONG).
             setAnchorView(snackBarAnchor ?: this).
             setAction(R.string.delete_snackbar_undo_text) { undoDelete() }.
             addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
             override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                 viewModel.emptyTrash()
             }}).show()
    }

    fun setExpandedItem(pos: Int?) {
        viewModel.setExpandedId(if (pos == null) null
                                else adapter.currentList[pos].id)
        itemAnimator.notifyExpandedItemChanged(pos)
    }

    /** A class that provides an interface for manipulating the selection of the parent recycler view.
     *
     *  Selection is a memberless class whose purpose is to make the manipula-
     *  tion of the recycler view selection more idiomatic (e.g. recyclerView.-
     *  selection.add() instead of recyclerView.addToSelection()). The size of
     *  the selection can be queried through the properties size and isEmpty,
     *  as well as sizeLiveData in case an observable selection size is desired.
     *
     *  The contents of the selection are modified for single items through the
     *  self-explanatory functions add, remove, and toggle, which accept the
     *  stable id of the item being operated on. The function clear will erase
     *  the selection entirely. */
    inner class Selection {
        val itemsLiveData get() = viewModel.selectedItems
        val items get() = itemsLiveData.value
        val size get() = items?.size ?: 0
        val isEmpty get() = size == 0
        val isNotEmpty get() = size != 0

        fun addAll() = viewModel.selectAll()
        fun add(id: Long) = viewModel.updateIsSelected(id, true)
        fun remove(id: Long) = viewModel.updateIsSelected(id, false)
        fun toggle(id: Long) = viewModel.toggleIsSelected(id)
        fun clear() = viewModel.clearSelection()
    }

    /** A subclass of ViewModelAdapter that enforces the use of ExpandableSelectableItemViewHolder.
     *
     *  ExpandableSelectableItemAdapter does not implement onCreateViewHolder, and is
     *  therefore abstract.*/
    abstract inner class ExpandableSelectableItemAdapter<VHType: ExpandableSelectableItemViewHolder> :
            ViewModelAdapter<VHType>() {
        override fun onBindViewHolder(holder: VHType, position: Int) {
            if (holder.item.isExpanded)
                itemAnimator.notifyExpandedItemChanged(position)
            super.onBindViewHolder(holder, position)
        }
    }

    /** A view holder that ensures that the view passed in the constructor is an instance of ExpandableSelectableItemView. */
    open inner class ExpandableSelectableItemViewHolder(
        view: ExpandableSelectableItemView<Entity>
    ) : ViewModelItemViewHolder(view)
}