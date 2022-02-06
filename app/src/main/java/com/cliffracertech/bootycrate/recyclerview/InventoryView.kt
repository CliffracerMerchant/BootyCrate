/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.cliffracertech.bootycrate.model.database.InventoryItem
import java.util.*

/**
 * A View to display a list of InventoryItem instances.
 *
 * The members onItemAutoAddToShoppingListCheckboxClick and
 * onItemAutoAddToShoppingListAmountChangeRequest should be set to a non-null
 * value to respond when either of these events occur for an item.
 */
class InventoryView(context: Context, attrs: AttributeSet) :
    ExpandableItemListView<InventoryItem>(context, attrs)
{
    override val diffUtilCallback = DiffUtilCallback()
    override val listAdapter = Adapter()

    var onItemAutoAddToShoppingListCheckboxClick: ((Long) -> Unit)? = null
    var onItemAutoAddToShoppingListAmountChangeRequest: ((Long, Int) -> Unit)? = null

    init { this.adapter = listAdapter }

    /**
     * A RecyclerView.Adapter to display the contents of a list of inventory items.
     *
     * InventoryItemRecyclerView.Adapter is a subclass of ExpandableSelectableRecyclerView.Adapter
     * using InventoryItemRecyclerView.ViewHolder instances to represent inventory
     * items. Its overrides of onBindViewHolder make use of the InventoryItem.Field
     * values passed by InventoryItemRecyclerView.DiffUtilCallback to support partial
     * binding. Note that the adapetr assumes that any change payloads passed to
     * it are of the type EnumSet<InventoryItem.Field>. If a payload of another
     * type is passed to it, an exception will be thrown.
     */
    inner class Adapter : ExpandableItemListView<InventoryItem>.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(InventoryItemView(context, itemAnimator.animatorConfig))

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
                val changes = payload as EnumSet<InventoryItem.Field>
                val ui = holder.view.ui
                val detailsUi = holder.view.detailsUi

                if (changes.contains(InventoryItem.Field.Name))
                    holder.view.setNameText(item.name)
                if (changes.contains(InventoryItem.Field.ExtraInfo))
                    holder.view.setExtraInfoText(item.extraInfo)
                if (changes.contains(InventoryItem.Field.Color)) {
                    ui.checkBox.colorIndex = item.color
                    detailsUi.autoAddToShoppingListCheckBox.colorIndex = item.color
                }
                if (changes.contains(InventoryItem.Field.Amount))
                    ui.amountEdit.value = item.amount
                if (changes.contains(InventoryItem.Field.IsExpanded))
                    holder.view.setExpanded(item.isExpanded)
                if (changes.contains(InventoryItem.Field.IsSelected))
                    holder.view.isSelected = item.isSelected
                if (changes.contains(InventoryItem.Field.IsLinked))
                    holder.view.updateIsLinked(item.isLinked, animate = item.isExpanded)
                if (changes.contains(InventoryItem.Field.AutoAddToShoppingList))
                    detailsUi.autoAddToShoppingListCheckBox.initIsChecked(item.autoAddToShoppingList)
                if (changes.contains(InventoryItem.Field.AutoAddToShoppingListAmount))
                    detailsUi.autoAddToShoppingListAmountEdit.value = item.autoAddToShoppingListAmount
            }
        }
    }

    /**
     * A ExpandableItemList.ViewHolder that wraps an instance of InventoryItemView.
     *
     * InventoryView.ViewHolder is a subclass of ExpandableItemList.ViewHolder that
     * holds an instance of InventoryItemView to display the data for an InventoryItem.
     */
    inner class ViewHolder(view: InventoryItemView) :
        ExpandableItemListView<InventoryItem>.ViewHolder(view) {

        override val view get() = itemView as InventoryItemView

        init {
            view.detailsUi.autoAddToShoppingListCheckBox.onCheckedChangedListener = { _ ->
                onItemAutoAddToShoppingListCheckboxClick?.invoke(item.id)
            }
            view.detailsUi.autoAddToShoppingListAmountEdit.onValueChangedListener = { value ->
                onItemAutoAddToShoppingListAmountChangeRequest?.invoke(item.id, value)
            }
        }
    }

    /**
     * Computes a diff between two inventory item lists.
     *
     * InventoryItemRecyclerView.DiffUtilCallback uses the ids of inventory
     * items to determine if they are the same or not. If they are the same,
     * the change payload will be an instance of EnumSet<InventoryItem.Field>
     * contains InventoryItem.Field values for all of the fields that were
     * changed.
     */
    class DiffUtilCallback : DiffUtil.ItemCallback<InventoryItem>() {
        private val listChanges = mutableMapOf<Long, EnumSet<InventoryItem.Field>>()
        private val itemChanges = EnumSet.noneOf(InventoryItem.Field::class.java)

        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
            itemChanges.apply {
                clear()
                if (newItem.name != oldItem.name)             add(InventoryItem.Field.Name)
                if (newItem.extraInfo != oldItem.extraInfo)   add(InventoryItem.Field.ExtraInfo)
                if (newItem.color != oldItem.color)           add(InventoryItem.Field.Color)
                if (newItem.amount != oldItem.amount)         add(InventoryItem.Field.Amount)
                if (newItem.isExpanded != oldItem.isExpanded) add(InventoryItem.Field.IsExpanded)
                if (newItem.isSelected != oldItem.isSelected) add(InventoryItem.Field.IsSelected)
                if (newItem.isLinked != oldItem.isLinked)     add(InventoryItem.Field.IsLinked)
                if (newItem.autoAddToShoppingList != oldItem.autoAddToShoppingList)
                    add(InventoryItem.Field.AutoAddToShoppingList)
                if (newItem.autoAddToShoppingListAmount != oldItem.autoAddToShoppingListAmount)
                    add(InventoryItem.Field.AutoAddToShoppingListAmount)

                if (!isEmpty())
                    listChanges[newItem.id] = EnumSet.copyOf(this)
            }.isEmpty()

        override fun getChangePayload(oldItem: InventoryItem, newItem: InventoryItem) =
            listChanges.remove(newItem.id)
    }
}