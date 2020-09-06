/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

/** A RecyclerView for displaying the contents of a BootyCrate ViewModel<Entity>.
 *
 *  ViewModelRecyclerView is an abstract RecyclerView subclass that tailors the
 *  RecyclerView interface toward displaying the contents of a ViewModel<
 *  Entity> and updating itself asynchronously using its custom ListAdapter-
 *  derived adapter type. To achieve this, its abstract properties diffUtilCall-
 *  back, adapter, and viewModel must be overridden in subclasses. diffUtilCall-
 *  back must be overridden with an appropriate DiffUtil.ItemCallback<Entity>
 *  for the adapter. adapter must be overridden with a ViewModelAdapter sub-
 *  class that implements onCreateViewHolder. viewModel must be overridden with
 *  a concrete ViewModel<Entity> subclass that implements its abstract methods.
 *
 *  Because AndroidViewModels are created after views inflated from xml are
 *  created, it is necessary to finish initialization with these required mem-
 *  bers. To do this, the function finishInit must be called with an Lifecycle-
 *  Owner and a ViewModel<Entity> instance during runtime, but before any sort
 *  of data access is attempted. An initial sort can also be passed during this
 *  finishInit call.
 *
 *  ViewModelRecyclerView has two public properties, sort and searchFilter,
 *  that mirror these properties from the view model from which it obtains its
 *  data. Changing these properties will therefore change the sorting or text
 *  filter of the RecyclerView items.
 *
 *  To utilize ViewModel<Entity>'s support for treating new items differently,
 *  ViewModelRecyclerView has an open function onNewItemInsertion. onNewItem-
 *  Insertion smooth scrolls to the new item by default, but can be overridden
 *  in subclasses to change this.
 *
 *  ViewModelRecyclerView provides public functions to execute most of View-
 *  Model<Entity>'s public functions such as add, delete, deleteAll, and undo-
 *  Delete. It also utilizes a ItemTouchHelper with a SwipeToDeleteCallback to
 *  allow the user to call deleteItem on the swiped item. It is worth noting
 *  that due to being Entity agnostic, the addItem function requires an already
 *  created instance of Entity to add to the ViewModel data. Subclasses may
 *  wish to provide an alternative that inserts a default constructed Entity
 *  instance.
 *
 *  When items are deleted, a snackbar will appear informing the user of the
 *  amount of items that were deleted, as well as providing an undo option. The
 *  snackbar will be anchored to the view set as the public property snackBar-
 *  Anchor, in case this needs to be customized, or to the RecyclerView itself
 *  otherwise. */
abstract class ViewModelRecyclerView<Entity: ViewModelItem>(
    context: Context,
    attrs: AttributeSet
) : RecyclerView(context, attrs) {

    abstract val diffUtilCallback: DiffUtil.ItemCallback<Entity>
    abstract val adapter: ViewModelAdapter<out ViewModelItemViewHolder>
    private lateinit var viewModel: ViewModel<Entity>
    var snackBarAnchor: View? = null

    var sort: ViewModelItem.Sort? get() = viewModel.sort
                                   set(value) { viewModel.sort = value }
    var searchFilter: String? get() = viewModel.searchFilter
                              set(value) { viewModel.searchFilter = value }

    init {
        ItemTouchHelper(SwipeToDeleteCallback(::deleteItem, context)).attachToRecyclerView(this)
    }

    fun finishInit(
        owner: LifecycleOwner,
        viewModel: ViewModel<Entity>,
        initialSort: ViewModelItem.Sort? = null
    ) {
        setAdapter(adapter)
        this.viewModel = viewModel
        if (initialSort != null) sort = initialSort
        viewModel.items.observe(owner, Observer { items -> adapter.submitList(items) })
    }

    open fun addItem(item: Entity) = viewModel.add(item)

    open fun deleteItem(pos: Int) = deleteItems(LongArray(1) { adapter.getItemId(pos) })

    open fun deleteItems(ids: LongArray) {
        viewModel.delete(ids)
        val text = context.getString(R.string.delete_snackbar_text, ids.size)
        val snackBar = Snackbar.make(this, text, Snackbar.LENGTH_LONG)
        snackBar.anchorView = snackBarAnchor ?: this
        snackBar.setAction(R.string.delete_snackbar_undo_text) { undoDelete() }
        snackBar.addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                viewModel.emptyTrash()
        }})
        snackBar.show()
    }

    open fun undoDelete() { viewModel.undoDelete() }

    open fun deleteAll() {
        val builder = themedAlertDialogBuilder(context)
        val dialogClickListener = DialogInterface.OnClickListener { _, button ->
            if (button == DialogInterface.BUTTON_POSITIVE) {
                viewModel.deleteAll()
                viewModel.emptyTrash()
            }
        }
        builder.setPositiveButton(context.getString(android.R.string.yes), dialogClickListener)
        builder.setNegativeButton(context.getString(android.R.string.cancel), dialogClickListener)
        builder.setMessage(context.getString(R.string.delete_all_inventory_items_confirmation_message))
        builder.show()
    }

    open fun onNewItemInsertion(item: Entity, vh: ViewModelItemViewHolder) =
        smoothScrollToPosition(vh.adapterPosition)

    /** A ListAdapter derived RecyclerView.Adapter for ViewModelRecyclerView.
     *
     *  ViewModelAdapter is an abstract (because it does not implement onCreate-
     *  ViewHolder) ListAdapter subclass that enforces the use of stable ids,
     *  and is responsible for calling onNewItemInsertion during the binding of
     *  a new item. t*/
    abstract inner class ViewModelAdapter<VHType: ViewModelItemViewHolder> :
            ListAdapter<Entity, VHType>(diffUtilCallback) {
        init { setHasStableIds(true) }

        final override fun setHasStableIds(hasStableIds: Boolean) =
            super.setHasStableIds(true)

        override fun onBindViewHolder(holder: VHType, position: Int) {
            if (getItemId(holder.adapterPosition) == viewModel.newlyAddedItemId) {
                onNewItemInsertion(currentList[holder.adapterPosition], holder)
                viewModel.resetNewlyAddedItemId()
            }
        }

        override fun getItemId(position: Int) = currentList[position].id
    }

   /** A ViewHolder subclass that provides a simplified way of obtaining the
    *  instance of the item that it represents through the property item. */
    open inner class ViewModelItemViewHolder(view: View) : ViewHolder(view) {
        val item: Entity get() = adapter.currentList[adapterPosition]
    }
}