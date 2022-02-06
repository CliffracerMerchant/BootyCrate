/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import java.util.*
import kotlin.collections.set

/**
 * A View to display a list of ShoppingListItem instances.
 *
 * The member callback onItemCheckBoxClick should be changed to a non-null
 * value to respond to clicks on an item's checkbox.
 */
class ShoppingListView(context: Context, attrs: AttributeSet) :
    ExpandableItemListView<ShoppingListItem>(context, attrs)
{
    override val diffUtilCallback = DiffUtilCallback()
    override val listAdapter = Adapter()

    var onItemCheckBoxClick: ((Long) -> Unit)? = null

    init {
        this.adapter = listAdapter
        itemAnimator.animatorConfig = AnimatorConfig(
            context.resources.getInteger(R.integer.shoppingListItemAnimationDuration).toLong(),
            AnimationUtils.loadInterpolator(context, R.anim.default_interpolator))
    }

    /**
     * An adapter to display the contents of a list of shopping list items.
     *
     * ShoppingListView.Adapter is a subclass of ExpandableItemListView.Adapter
     * using ShoppingListView.ViewHolder instances to represent shopping list
     * items. Its overrides of onBindViewHolder make use of the ShoppingListItem.Field
     * values passed by ShoppingListView.DiffUtilCallback to support partial
     * binding. Note that ShoppingListView.Adapter assumes that any change
     * payloads passed to it are of the type EnumSet<ShoppingListItem.Field>.
     * If a payload of another type is passed to it, an exception will be thrown.
     */
    inner class Adapter : ExpandableItemListView<ShoppingListItem>.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ShoppingListItemView(context, itemAnimator.animatorConfig))

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

                if (changes.contains(ShoppingListItem.Field.Name))
                    holder.view.setNameText(item.name)
                if (changes.contains(ShoppingListItem.Field.ExtraInfo))
                    holder.view.setExtraInfoText(item.extraInfo)
                if (changes.contains(ShoppingListItem.Field.Color))
                    ui.checkBox.colorIndex = item.color
                if (changes.contains(ShoppingListItem.Field.Amount))
                    ui.amountEdit.value = item.amount
                if (changes.contains(ShoppingListItem.Field.IsExpanded))
                    holder.view.setExpanded(item.isExpanded)
                if (changes.contains(ShoppingListItem.Field.IsSelected))
                    holder.view.isSelected = item.isSelected
                if (changes.contains(ShoppingListItem.Field.IsLinked))
                    holder.view.updateIsLinked(item.isLinked, animate = item.isExpanded)
                if (changes.contains(ShoppingListItem.Field.IsChecked)) {
                    ui.checkBox.initIsChecked(item.isChecked)
                    holder.view.setStrikeThroughEnabled(item.isChecked)
                }
            }
        }
    }

    /**
     * A ExpandableItemListView.ViewHolder that wraps an instance of ShoppingListItemView.
     *
     * ShoppingListView.ViewHolder is a subclass of ExpandableItemListView.ViewHolder
     * that holds an instance of ShoppingListItemView to display the data for a
     * ShoppingListItem.
     */
    inner class ViewHolder(view: ShoppingListItemView) :
        ExpandableItemListView<ShoppingListItem>.ViewHolder(view)
    {
        override val view get() = itemView as ShoppingListItemView

        init {
            view.ui.checkBox.onCheckedChangedListener = { _ ->
                onItemCheckBoxClick?.invoke(item.id)
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