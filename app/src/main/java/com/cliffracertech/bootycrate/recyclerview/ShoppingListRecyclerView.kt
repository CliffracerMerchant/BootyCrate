/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.database.shoppingListViewModel
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import java.util.*
import kotlin.collections.set

/**
 * A RecyclerView to display the data provided by a ShoppingListViewModel.
 *
 * ShoppingListRecyclerView is a ExpandableSelectableRecyclerView subclass
 * specialized for displaying the contents of a shopping list. ShoppingListRecyclerView
 * adds a sortByChecked property, which mirrors the ShoppingListViewModel
 * property, for convenience. sortByChecked should not be changed the
 * property viewModel is initialized, or an exception will be thrown.
 */
class ShoppingListRecyclerView(context: Context, attrs: AttributeSet) :
    ExpandableSelectableRecyclerView<ShoppingListItem>(context, attrs)
{
    override val diffUtilCallback = DiffUtilCallback()
    override val adapter = Adapter()
    override val viewModel = shoppingListViewModel(context)

    var sortByChecked get() = viewModel.sortByChecked
        set(value) { viewModel.sortByChecked = value }

    init {
        itemAnimator.animatorConfig = AnimatorConfig(
            context.resources.getInteger(R.integer.shoppingListItemAnimationDuration).toLong(),
            AnimationUtils.loadInterpolator(context, R.anim.default_interpolator))
        itemAnimator.registerAdapterDataObserver(adapter)
    }

    /**
     * An adapter to display the contents of a list of shopping list items.
     *
     * ShoppingListRecyclerView.Adapter is a subclass of ExpandableSelectableRecyclerView.Adapter
     * using ShoppingListRecyclerView.ViewHolder instances to represent shopping list
     * items. Its overrides of onBindViewHolder make use of the ShoppingListItem.Field
     * values passed by ShoppingListRecyclerView.DiffUtilCallback to support partial
     * binding. Note that ShoppingListAdapter assumes that any change payloads passed
     * to it are of the type EnumSet<ShoppingListItem.Field>. If a payload of another
     * type is passed to it, an exception will be thrown.
     */
    inner class Adapter : ExpandableSelectableRecyclerView<ShoppingListItem>.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ShoppingListItemView(context, itemAnimator.animatorConfig))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.view.update(holder.item)
            super.onBindViewHolder(holder, position)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.size == 0)
                return onBindViewHolder(holder, position)

            for (payload in payloads) {
                val item = getItem(position)
                @Suppress("UNCHECKED_CAST")
                val changes = payload as EnumSet<ShoppingListItem.Field>
                val ui = holder.view.ui

                if (changes.contains(ShoppingListItem.Field.Name) &&
                    ui.nameEdit.text.toString() != item.name)
                        ui.nameEdit.setText(item.name)
                if (changes.contains(ShoppingListItem.Field.ExtraInfo) &&
                    ui.extraInfoEdit.text.toString() != item.extraInfo)
                        holder.view.setExtraInfoText(item.extraInfo)
                if (changes.contains(ShoppingListItem.Field.Color) &&
                    ui.checkBox.colorIndex != item.color)
                        ui.checkBox.colorIndex = item.color
                if (changes.contains(ShoppingListItem.Field.Amount) &&
                    ui.amountEdit.value != item.amount)
                        ui.amountEdit.value = item.amount
                if (changes.contains(ShoppingListItem.Field.IsExpanded) &&
                    holder.view.isExpanded != item.isExpanded)
                        holder.view.setExpanded(item.isExpanded)
                if (changes.contains(ShoppingListItem.Field.IsSelected) &&
                    holder.view.isInSelectedState != item.isSelected)
                        holder.view.setSelectedState(item.isSelected)
                if (changes.contains(ShoppingListItem.Field.IsLinked) &&
                    ui.linkIndicator.isVisible != item.isLinked)
                        holder.view.updateIsLinked(item.isLinked, animate = false)
                if (changes.contains(ShoppingListItem.Field.IsChecked) &&
                    ui.checkBox.isChecked != item.isChecked)
                        ui.checkBox.isChecked = item.isChecked
            }
        }
    }

    /**
     * A ExpandableSelectableItemViewHolder that wraps an instance of ShoppingListItemView.
     *
     * ShoppingListRecyclerView.ViewHolder is a subclass of ExpandableSelectableItemViewHolder
     * that holds an instance of ShoppingListItemView to display the data for a
     * ShoppingListItem.
     */
    inner class ViewHolder(val view: ShoppingListItemView) :
        ExpandableSelectableRecyclerView<ShoppingListItem>.ViewHolder(view)
    {
        init {
            view.ui.checkBox.onCheckedChangedListener = { checked ->
                viewModel.updateIsChecked(item.id, checked)
                view.setStrikeThroughEnabled(checked)
            }
        }
    }

    /**
     * Computes a diff between two shopping list item lists.
     *
     * DiffUtilCallback uses the ids of shopping list items to determine if they
     * are the same or not. If they are the same, the change payload will be an
     * instance of EnumSet<ShoppingListItem.Field> that contains the
     * ShoppingListItem.Field values for all of the fields that were changed.
     */
    class DiffUtilCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        private val listChanges = mutableMapOf<Long, EnumSet<ShoppingListItem.Field>>()
        private val itemChanges = EnumSet.noneOf(ShoppingListItem.Field::class.java)

        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            itemChanges.apply {
                clear()
                if (newItem.name != oldItem.name)             add(ShoppingListItem.Field.Name)
                if (newItem.extraInfo != oldItem.extraInfo)   add(ShoppingListItem.Field.ExtraInfo)
                if (newItem.color != oldItem.color)           add(ShoppingListItem.Field.Color)
                if (newItem.amount != oldItem.amount)         add(ShoppingListItem.Field.Amount)
                if (newItem.isExpanded != oldItem.isExpanded) add(ShoppingListItem.Field.IsExpanded)
                if (newItem.isSelected != oldItem.isSelected) add(ShoppingListItem.Field.IsSelected)
                if (newItem.isLinked != oldItem.isLinked)     add(ShoppingListItem.Field.IsLinked)
                if (newItem.isChecked != oldItem.isChecked)   add(ShoppingListItem.Field.IsChecked)

                if (!isEmpty())
                    listChanges[newItem.id] = EnumSet.copyOf(this)
            }.isEmpty()

        override fun getChangePayload(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            listChanges.remove(newItem.id)
    }
}