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
 * ExpandableSelectableRecyclerView extends BootyCrateRecyclerView by using a
 * ExpandableItemAnimator instance to visually animate changes in items'
 * expanded states, and by adding an interface for item selection through the
 * property selection (see the documentation for the inner class Selection for
 * more details). It also utilizes its own custom view holder to enforce the
 * use of an ExpandableSelectableItemView, and a custom adapter that in turn
 * enforces the use of ExpandableSelectableRecyclerView.ViewHolder.
 */
@Suppress("LeakingThis")
abstract class ExpandableSelectableRecyclerView<T: BootyCrateItem>(
    context: Context,
    attrs: AttributeSet
) : BootyCrateRecyclerView<T>(context, attrs) {
    protected val itemAnimator = ExpandableItemAnimator(AnimatorConfig.appDefault(context))
    val selection = Selection()
    private var needToHideSoftKeyboard = false

    init {
        addItemDecoration(ItemSpacingDecoration(context))
        setHasFixedSize(true)
        itemAnimator.onItemCollapseStartedListener = { viewHolder ->
            val vh = viewHolder as ExpandableSelectableRecyclerView<*>.ViewHolder
            needToHideSoftKeyboard = vh.view.focusedChild != null
        }
        itemAnimator.onItemCollapseEndedListener = { viewHolder ->
            if (needToHideSoftKeyboard) {
                clearFocus()
                SoftKeyboard.hide(this)
            }
        }
        setItemAnimator(itemAnimator)
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
        val sizeLiveData get() = viewModel.selectedItemCount
        val size get() = sizeLiveData.value ?: 0
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

        override fun onBindViewHolder(holder: VHType, position: Int) =
            holder.view.update(holder.item)

        override fun onViewDetachedFromWindow(holder: VHType) {
            super.onViewDetachedFromWindow(holder)
            if (holder.view.focusedChild != null) {
                clearFocus()
                SoftKeyboard.hide(holder.itemView)
            }
        }
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
        open val view get() = itemView as ExpandableSelectableItemView<T>

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
                    viewModel.setExpandedItem(if (isExpanded) null
                                              else adapter.currentList[adapterPosition].id)
                }
            }
        }
    }
}