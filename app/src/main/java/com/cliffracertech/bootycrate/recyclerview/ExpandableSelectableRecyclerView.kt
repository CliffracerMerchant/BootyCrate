/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import com.cliffracertech.bootycrate.database.BootyCrateItem
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.SoftKeyboard

/**
 * A BootyCrateRecyclerView subclass that enables multi-selection and expansion of items.
 *
 * ExpandableSelectableRecyclerView extends BootyCrateRecyclerView via the use
 * of an ExpandableSelectableItemViewModel along with the new function setExpandedItem.
 * Its selection property provides an interface for manipulating the recycler
 * view selection (see the documentation for the inner class Selection for more
 * details) as well as the function setExpandedItem to change or set to null
 * the currently expanded item. It also utilizes its own custom view holder to
 * enforce the use of an ExpandableSelectableItemView, and a custom adapter
 * that in turn enforces the use of ExpandableSelectableRecyclerView.ViewHolder.
 */
@Suppress("LeakingThis")
abstract class ExpandableSelectableRecyclerView<T: BootyCrateItem>(
    context: Context,
    attrs: AttributeSet
) : BootyCrateRecyclerView<T>(context, attrs) {
    protected val itemAnimator = ExpandableItemAnimator(AnimatorConfig.appDefault(context))

    val selection = Selection()

    init {
        addItemDecoration(ItemSpacingDecoration(context))
        setHasFixedSize(true)
        itemAnimator.expandCollapseAnimationFinishedListener = { _, _ ->
            if (pendingExpandedItem != -1) {
                setExpandedItem(pendingExpandedItem)
                pendingExpandedItem = -1
            }
        }
        setItemAnimator(itemAnimator)
    }

    private var pendingExpandedItem: Int? = -1
    fun setExpandedItem(pos: Int?) {
        // This check makes sure that another expand collapse animation isn't
        // already playing to prevent visual bugs. Unfortunately this can also
        // make the UI feel unresponsive if the user if expanding and collapsing
        // items quickly. To compromise, if the user tries to expand or collapse
        // another item while an animation is ongoing, the new expand collapse
        // is queued and occurs after the ongoing one is finished.
        if (itemAnimator.expandCollapseAnimationInProgress)
            pendingExpandedItem = pos
        else {
            val currentExpandedPos = itemAnimator.expandedItemPos
            if (currentExpandedPos != null) {
                val vh = findViewHolderForAdapterPosition(currentExpandedPos)
                if ((vh as ExpandableSelectableRecyclerView<*>.ViewHolder).hasFocusedChild()) {
                    requestFocus()
                    SoftKeyboard.hide(this)
                }
            }
            viewModel.setExpandedItem(if (pos == null) null
                                      else adapter.currentList[pos].id)
            itemAnimator.notifyExpandedItemChanged(pos)
        }
    }

    /**
     * A class that provides an interface for manipulating the selection of the parent recycler view.
     *
     * Selection is a memberless class whose purpose is to make the manipulation
     * of the recycler view selection more idiomatic (e.g. recyclerView.selection.add()
     * instead of recyclerView.addToSelection()). The size of the selection can be
     * queried through the properties size and isEmpty, as well as sizeLiveData in
     * case an observable selection size is desired.
     *
     * The contents of the selection are modified for single items through the
     * self-explanatory functions add, remove, and toggle, which accept the stable
     * id of the item being operated on. The function clear will erase the
     * selection entirely.
     */
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

    /** An abstract (due to not implementing onCreateViewHolder) subclass of
     * BootyCrateRecyclerView.Adapter that enforces the use of
     * ExpandableSelectableRecyclerView.ViewHolder. */
    abstract inner class Adapter<VHType: ViewHolder> : BootyCrateRecyclerView<T>.Adapter<VHType>() {
        override fun onBindViewHolder(holder: VHType, position: Int) {
            if (holder.item.isExpanded)
                itemAnimator.notifyExpandedItemChanged(position) }
    }

    /**
     * A ViewHolder subclass that wraps an instance of ExpandableSelectableItemView.
     *
     * ExpandableSelectableRecyclerView.ViewHolder updates the onClickListeners of
     * the wrapped item view to enable the selection and expansion of the items, and
     * adds the open function hasFocusedChild. ExpandableSelectableRecyclerView uses
     * the return value of hasFocusedChild to decide whether or not to attempt to
     * hide the soft keyboard when an item is collapsed. If a subclass adds new
     * focusable children, it should override hasFocusedChild to check if these
     * children are focused.
     */
    open inner class ViewHolder(view: ExpandableSelectableItemView<T>) :
        BootyCrateRecyclerView<T>.ViewHolder(view)
    {
        init {
            val onClick = OnClickListener { if (!selection.isEmpty) selection.toggle(itemId) }
            val onLongClick = OnLongClickListener { selection.toggle(itemId); true }

            view.apply {
                startAnimationsImmediately = false
                setOnClickListener(onClick)
                ui.nameEdit.setOnClickListener(onClick)
                ui.extraInfoEdit.setOnClickListener(onClick)
                ui.amountEdit.ui.valueEdit.setOnClickListener(onClick)

                setOnLongClickListener(onLongClick)
                ui.nameEdit.setOnLongClickListener(onLongClick)
                ui.extraInfoEdit.setOnLongClickListener(onLongClick)
                ui.amountEdit.ui.valueEdit.setOnLongClickListener(onLongClick)
                ui.editButton.setOnClickListener {
                    setExpandedItem(if (!view.isExpanded) adapterPosition else null)
                }
            }
        }

        open fun hasFocusedChild() = (itemView as ExpandableSelectableItemView<*>).ui.run {
            nameEdit.isFocused || extraInfoEdit.isFocused || amountEdit.ui.valueEdit.isFocused
        }
    }
}