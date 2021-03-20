/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.util.*

/**
 * A RecyclerView for displaying the contents of a ViewModel<Entity>.
 *
 * ViewModelRecyclerView is an abstract RecyclerView subclass that tailors the
 * RecyclerView interface toward displaying the contents of a ViewModel<
 * Entity> and updating itself asynchronously using its custom ListAdapter-
 * derived adapter type. To achieve this, its abstract properties diffUtilCall-
 * back, adapter, and viewModel must be overridden in subclasses. diffUtilCall-
 * back must be overridden with an appropriate DiffUtil.ItemCallback<Entity>
 * for the adapter. adapter must be overridden with a ViewModelAdapter subclass
 * that implements onCreateViewHolder. viewModel must be overridden with a
 * concrete ViewModel<Entity> subclass. collectionName, used in user facing
 * strings regarding the item collection, should be overridden with a string
 * that describes the collection of items (e.g. inventory for a collection of
 * inventory items).
 *
 * After the viewModel property is overridden and initialized in subclasses,
 * the function observeViewModel must be called with a LifecycleOwner that
 * matches the ViewModelRecyclerView's lifespan. If observeViewModel is not
 * called then the recycler view will always be empty. Once the viewModel prop-
 * erty is initialized properly, the properties sort and searchFilter, which
 * mirror these properties from the view model, can be changed to change the
 * sorting or text filter of the displayed items.
 *
 * To utilize ViewModel<Entity>'s support for treating new items differently,
 * ViewModelRecyclerView has an open function onNewItemInsertion. onNewItem-
 * Insertion smooth scrolls to the new item by default, but can be overridden
 * in subclasses for additional functionality.
 *
 * ViewModelRecyclerView utilizes a ItemTouchHelper with a SwipeToDeleteCall-
 * back to allow the user to call deleteItem on the swiped item. When items are
 * deleted, a snack bar will appear informing the user of the amount of items
 * that were deleted, as well as providing an undo option. The snack bar will
 * be anchored to the view set as the public property snackBarAnchor, in case
 * this needs to be customized, or to the RecyclerView itself otherwise.
 */
@Suppress("LeakingThis")
abstract class ViewModelRecyclerView<Entity: ViewModelItem>(
    context: Context,
    attrs: AttributeSet
) : RecyclerView(context, attrs) {

    open val collectionName = ""
    protected abstract val diffUtilCallback: DiffUtil.ItemCallback<Entity>
    abstract val adapter: ViewModelAdapter<out ViewModelItemViewHolder>
    protected abstract val viewModel: ViewModel<Entity>
    var snackBarAnchor: View? = null

    var sort get() = viewModel.sort
             set(value) { viewModel.sort = value }
    var searchFilter: String? get() = viewModel.searchFilter
                              set(value) { viewModel.searchFilter = value }

    init {
        val swipeCallback = SwipeToDeleteCallback(context) { pos ->
            viewModel.delete(LongArray(1) { adapter.getItemId(pos) })
            val text = context.getString(R.string.delete_snackbar_text, 1)
            Snackbar.make(this, text, Snackbar.LENGTH_LONG)
                .setAnchorView(snackBarAnchor ?: this)
                .setAction(R.string.delete_snackbar_undo_text) { viewModel.undoDelete() }
                .addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(a: Snackbar?, b: Int) = viewModel.emptyTrash()
                }).show()
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(this)
    }

    fun observeViewModel(owner: LifecycleOwner) {
        setAdapter(adapter)
        viewModel.items.observe(owner) { items -> adapter.submitList(items) }
    }

    open fun onNewItemInsertion(item: Entity, vh: ViewModelItemViewHolder) =
        smoothScrollToPosition(vh.adapterPosition)

    /**
     * A ListAdapter derived RecyclerView.Adapter for ViewModelRecyclerView.
     *
     * ViewModelAdapter enforces the use of stable ids and calls onNewItemInsertion
     * for newly inserted items. It is abstract because it does not implement
     * onCreateViewHolder.
     */
    @Suppress("LeakingThis")
    abstract inner class ViewModelAdapter<VHType: ViewModelItemViewHolder> :
            ListAdapter<Entity, VHType>(diffUtilCallback) {
        init { setHasStableIds(true) }

        final override fun setHasStableIds(hasStableIds: Boolean) =
            super.setHasStableIds(true)

        override fun getItemId(position: Int) = currentList[position].id

        override fun onBindViewHolder(holder: VHType, position: Int) {
            if (getItemId(holder.adapterPosition) == viewModel.newlyAddedItemId) {
                onNewItemInsertion(currentList[holder.adapterPosition], holder)
                viewModel.resetNewlyAddedItemId()
            }
        }
    }

    /**
     * A ViewHolder subclass that wraps an instance of ViewModelItemView.
     *
     * ViewModelItemViewHolder provides a simplified way of obtaining the instance
     * of the item that it represents through the property item, and connects
     * changes to the fields made by the user to view model update calls.
     */
    open inner class ViewModelItemViewHolder(view: ViewModelItemView<Entity>) :
            RecyclerView.ViewHolder(view) {
        val item: Entity get() = adapter.currentList[adapterPosition]

        init {
            view.apply {
                ui.checkBox.onColorChangedListener = { color ->
                    viewModel.updateColor(item.id, ViewModelItem.Colors.indexOf(color))
                }
                ui.nameEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateName(item.id, value)
                }
                ui.extraInfoEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateExtraInfo(item.id, value)
                }
                ui.amountEdit.onValueChangedListener = { value ->
                    if (adapterPosition != -1)
                        viewModel.updateAmount(item.id, value)
                }
            }
        }
    }
}