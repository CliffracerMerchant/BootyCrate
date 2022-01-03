/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.*
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.ListItem

/**
 * A view to display a list of ListItem instances.
 *
 * ItemListView is an abstract RecyclerView subclass intended to display a list
 * of ListItem instances. The list of items is displayed using the function
 * submitList. Item interactions, e.g. a click, will be responded to by calling
 * the appropriate callback for the event. These callbacks all use the item
 * being interacted with's id as their first parameter. These callbacks are
 * initialized to null, and therefore must be changed to a non-null value to
 * have these events be responded to.
 *
 * ItemListView utilizes a ItemTouchHelper with a SwipeToDeleteCallback
 * to allow the user to delete swiped items. When items are swiped, the
 * callback onItemSwiped will be invoked if not null with the swiped item's id.
 *
 * For subclasses, the abstract properties diffUtilCallback and listAdapter
 * must be overridden. diffUtilCallback must be overridden with an appropriate
 * DiffUtil.ItemCallback<T> for the adapter. listAdapter must be overridden
 * with a ItemListView.Adapter subclass that implements onCreateViewHolder.
 */
@Suppress("LeakingThis")
abstract class ItemListView<T: ListItem>(
    context: Context,
    attrs: AttributeSet
) : RecyclerView(context, attrs) {
    protected abstract val diffUtilCallback: DiffUtil.ItemCallback<T>
    abstract val listAdapter: Adapter<out ViewHolder>

    /** Called when an item's view is clicked. */
    var onItemClick: ((Long) -> Unit)? = null
    /** Called when an item's view is long clicked. */
    var onItemLongClick: ((Long) -> Unit)? = null
    /** Called when a new color option has been chosen in the item's inner TintableCheckbox. */
    var onItemColorChangeRequest: ((Long, Int) -> Unit)? = null
    /** Called when a new name has been requested for the item through its inner name TextFieldEdit. */
    var onItemRenameRequest: ((Long, String) -> Unit)? = null
    /** Called when a new extra info line has been requested for the item through its inner extra info TextFieldEdit. */
    var onItemExtraInfoChangeRequest: ((Long, String) -> Unit)? = null
    /** Called when a new amount has been requested for the item through its inner amount IntegerEdit. */
    var onItemAmountChangeRequest: ((Long, Int) -> Unit)? = null
    /** Called when an item's view is swiped left or right. */
    var onItemSwipe: ((Long) -> Unit)? = null

    init {
        val cornerRadius = resources.getDimension(R.dimen.recycler_view_item_rounded_corner_radius)
        val swipeCallback = SwipeToDeleteCallback(context, this, cornerRadius) { pos ->
            onItemSwipe?.invoke(listAdapter.getItemId(pos))
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(this)
        val spacing = resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing)
        addItemDecoration(ItemSpacingDecoration(spacing))
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
    }

    fun submitList(items: List<T>) = listAdapter.submitList(items)

    /** A ListAdapter derived RecyclerView.Adapter for ItemList that enforces the use of stable ids. */
    @Suppress("LeakingThis")
    abstract inner class Adapter<VHType: ViewHolder> : ListAdapter<T, VHType>(diffUtilCallback) {
        init { setHasStableIds(true) }

        final override fun setHasStableIds(hasStableIds: Boolean) =
            super.setHasStableIds(true)

        override fun getItemId(position: Int) = currentList[position].id
    }

    /**
     * A ViewHolder subclass that wraps an instance of ListItemView.
     *
     * ItemList.ViewHolder provides a simplified way of obtaining the instance of
     * the item that it represents through the property item, and connects user
     * changes to its editable fields to their appropriate callbacks.
     */
    open inner class ViewHolder(view: ListItemView<T>) : RecyclerView.ViewHolder(view) {
        val item: T get() = listAdapter.currentList[adapterPosition]

        init { view.apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            val onClick = OnClickListener { onItemClick?.invoke(item.id) }
            val onLongClick = OnLongClickListener { onItemLongClick?.invoke(item.id); true }
            setOnClickListener(onClick)
            ui.nameEdit.setOnClickListener(onClick)
            ui.extraInfoEdit.setOnClickListener(onClick)
            ui.amountEdit.ui.valueEdit.setOnClickListener(onClick)

            setOnLongClickListener(onLongClick)
            ui.nameEdit.setOnLongClickListener(onLongClick)
            ui.extraInfoEdit.setOnLongClickListener(onLongClick)
            ui.amountEdit.ui.valueEdit.setOnLongClickListener(onLongClick)

            ui.checkBox.onColorChangedListener = { color ->
                onItemColorChangeRequest?.invoke(item.id, color)
            }
            ui.nameEdit.onTextChangedListener = { newName ->
                onItemRenameRequest?.invoke(item.id, newName)
            }
            ui.extraInfoEdit.onTextChangedListener = { newExtraInfo ->
                onItemRenameRequest?.invoke(item.id, newExtraInfo)
            }
            ui.amountEdit.onValueChangedListener = { value ->
                onItemAmountChangeRequest?.invoke(item.id, value)
            }
        }}
    }
}