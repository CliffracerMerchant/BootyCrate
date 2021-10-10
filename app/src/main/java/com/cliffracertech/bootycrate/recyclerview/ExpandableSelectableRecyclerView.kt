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
 * expanded states. It utilizes its own custom view holder to enforce the use
 * of ExpandableSelectableItemViews, and a custom adapter that in turn enforces
 * the use of ExpandableSelectableRecyclerView.ViewHolder.
 */
@Suppress("LeakingThis")
abstract class ExpandableSelectableRecyclerView<T: BootyCrateItem>(
    context: Context,
    attrs: AttributeSet
) : BootyCrateRecyclerView<T>(context, attrs) {
    protected val itemAnimator = ExpandableItemAnimator(AnimatorConfig.appDefault(context))
    private var needToHideSoftKeyboard = false
    private var expandCollapseAnimRunning = false
    private var queuedEditButtonPressPos = -1
    private var editButtonLastPressTimestamp = 0L

    init {
        itemAnimator.onAnimStartedListener = { viewHolder, expanding ->
            expandCollapseAnimRunning = true
            if (!expanding) {
                val vh = viewHolder as ExpandableSelectableRecyclerView<*>.ViewHolder
                vh.view.hasFocus()
                needToHideSoftKeyboard = vh.view.focusedChild != null
            }
        }
        // While allowing all edit button presses to be responded to would be ideal,
        // rapid edit button presses seem to cause flickering in the item views
        // (perhaps due to layouts occurring when the item view is still in the
        // middle of an expand collapse animation). If all edit buttons presses are
        // ignored when an expand/collapse animation is playing, this causes the
        // item expand/collapse function to seem sluggish due to ignoring most of
        // the user's inputs if they occur rapidly. To compromise, an edit button
        // press when an expand/collapse animation is occurring will be queued, and
        // acted out when the current expand/collapse animation is finished if the
        // queued edit button press occurred less than half the duration of the
        // expand/collapse animations ago.
        itemAnimator.onAnimEndedListener = { _, expanding ->
            expandCollapseAnimRunning = false
            if (!expanding && needToHideSoftKeyboard) {
                clearFocus()
                SoftKeyboard.hide(this)
            }
            if (queuedEditButtonPressPos != -1) {
                val now = System.currentTimeMillis()
                val allowableMargin = itemAnimator.changeDuration / 2
                if ((editButtonLastPressTimestamp + allowableMargin) >= now) {
                    val vh = findViewHolderForAdapterPosition(queuedEditButtonPressPos)
                    val view = vh?.itemView as? ExpandableSelectableItemView<*>
                    view?.ui?.editButton?.performClick()
                }
                queuedEditButtonPressPos = -1
            }
        }
        setItemAnimator(itemAnimator)
    }

    private val selectionIsEmpty get() = (viewModel.selectedItemCount.value ?: 0) == 0
    private fun toggleSelected(id: Long) = viewModel.toggleIsSelected(id)
    private fun clearSelection() = viewModel.clearSelection()

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
        @Suppress("UNCHECKED_CAST")
        open val view get() = itemView as ExpandableSelectableItemView<T>

        init {
            val onClick = OnClickListener { if (!selectionIsEmpty) toggleSelected(itemId) }
            val onLongClick = OnLongClickListener { toggleSelected(itemId); true }

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
                    if (!expandCollapseAnimRunning)
                        viewModel.setExpandedItem(if (isExpanded) null
                                                  else            item.id)
                    else {
                        editButtonLastPressTimestamp = System.currentTimeMillis()
                        queuedEditButtonPressPos = adapterPosition
                    }
                }
            }
        }
    }
}