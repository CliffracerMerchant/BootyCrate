/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateItem
import com.cliffracertech.bootycrate.database.BootyCrateViewModel
import com.cliffracertech.bootycrate.utils.resolveIntAttribute
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

/**
 * A RecyclerView for displaying the contents of a BootyCrateViewModel.
 *
 * BootyCrateRecyclerView is an abstract RecyclerView subclass that tailors the
 * RecyclerView interface toward displaying the contents of a BootyCrateViewModel
 * and updating itself asynchronously using its custom ListAdapter derived
 * adapter type. To achieve this, its abstract properties diffUtilCallback,
 * adapter, and viewModel must be overridden in subclasses. diffUtilCallback
 * must be overridden with an appropriate DiffUtil.ItemCallback<T> for the
 * adapter. adapter must be overridden with a BootyCrateItemAdapter subclass
 * that implements onCreateViewHolder. viewModel must be overridden with a
 * concrete BootyCrateViewModel<T> subclass.
 *
 * After the viewModel property is overridden and initialized in subclasses,
 * the function observeViewModel must be called with a LifecycleOwner that
 * matches the BootyCrateRecyclerView's lifespan. If observeViewModel is not
 * called then the recycler view will always be empty. Once the viewModel
 * property is initialized properly, the properties sort and searchFilter,
 * which mirror these properties from the view model, can be changed to change
 * the sorting or text filter of the displayed items.
 *
 * BootyCrateRecyclerView utilizes a ItemTouchHelper with a SwipeToDeleteCallback
 * to allow the user to call deleteItem on the swiped item. When items are
 * deleted, a snack bar will appear informing the user of the amount of items
 * that were deleted, as well as providing an undo option. The snack bar will
 * be anchored to the view set as the public property snackBarAnchor, in case
 * this needs to be customized, or to the RecyclerView itself otherwise.
 */
@Suppress("LeakingThis")
abstract class BootyCrateRecyclerView<T: BootyCrateItem>(
    context: Context,
    attrs: AttributeSet
) : RecyclerView(context, attrs) {
    protected abstract val diffUtilCallback: DiffUtil.ItemCallback<T>
    abstract val adapter: Adapter<out ViewHolder>
    protected abstract val viewModel: BootyCrateViewModel<T>
    var snackBarAnchor: View? = null

    var sort get() = viewModel.sort
             set(value) { viewModel.sort = value }
    var searchFilter: String? get() = viewModel.searchFilter
                              set(value) { viewModel.searchFilter = value }

    init {
        val swipeCallback = SwipeToDeleteCallback(context) { pos ->
            viewModel.delete(LongArray(1) { adapter.getItemId(pos) })
            showDeletedItemsSnackBar(1)
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(this)
    }

    fun showDeletedItemsSnackBar(numDeletedItems: Int) {
        val text = context.getString(R.string.delete_snackbar_text, numDeletedItems)
        Snackbar.make(this, text, Snackbar.LENGTH_LONG)
            .setAnchorView(snackBarAnchor ?: this)
            .setAction(R.string.delete_snackbar_undo_text) { viewModel.undoDelete() }
            .setActionTextColor(context.theme.resolveIntAttribute(R.attr.colorAccent))
            .addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(a: Snackbar?, b: Int) { viewModel.emptyTrash() }
            }).show()
    }

    fun observeViewModel(owner: LifecycleOwner) {
        setAdapter(adapter)
        viewModel.items.observe(owner) { items -> adapter.submitList(items) }
    }

    /** A ListAdapter derived RecyclerView.Adapter for BootyCrateRecyclerView
     * that enforces the use of stable ids. */
    @Suppress("LeakingThis")
    abstract inner class Adapter<VHType: ViewHolder> : ListAdapter<T, VHType>(diffUtilCallback) {
        init { setHasStableIds(true) }

        final override fun setHasStableIds(hasStableIds: Boolean) =
            super.setHasStableIds(true)

        override fun getItemId(position: Int) = currentList[position].id
    }

    /**
     * A ViewHolder subclass that wraps an instance of BootyCrateItemView.
     *
     * BootyCrateRecyclerView.ViewHolder provides a simplified way of obtaining
     * the instance of the item that it represents through the property item, and
     * connects changes to the fields made by the user to view model update calls.
     */
    open inner class ViewHolder(view: BootyCrateItemView<T>) : RecyclerView.ViewHolder(view) {
        val item: T get() = adapter.currentList[adapterPosition]

        init { view.apply {
            ui.checkBox.onColorChangedListener = { color ->
                viewModel.updateColor(item.id, BootyCrateItem.Colors.indexOf(color))
            }
            ui.nameEdit.onTextChangedListener = { newName ->
                if (adapterPosition != -1) viewModel.updateName(item.id, newName)
            }
            ui.extraInfoEdit.onTextChangedListener = { newExtraInfo ->
                if (adapterPosition != -1) viewModel.updateExtraInfo(item.id, newExtraInfo)
            }
            ui.amountEdit.onValueChangedListener = { value ->
                if (adapterPosition != -1) viewModel.updateAmount(item.id, value)
            }
        }}
    }
}