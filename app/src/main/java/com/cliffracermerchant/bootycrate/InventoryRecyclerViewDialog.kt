/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_popup_layout.view.*

/** A RecyclerView for selecting a single item in the user's inventory in a popup.
 *
 *  InventoryRecyclerViewDialog is an alteration of InventoryRecyclerView
 *  specialized for selecting a single item within the inventory from a popup
 *  window. The selected item (or null is no item is selected) can queried from
 *  the property selectedItem. */
class InventoryRecyclerViewDialog(context: Context,
                                  private val items: List<InventoryItem>,
                                  private val initiallySelectedItemId: Long? = null) :
        RecyclerView(context) {
    private val adapter = PopupInventoryAdapter(context)
    private var selectedItemPos: Int? = null
    val selectedItem: InventoryItem? get() { val pos = selectedItemPos
                                             return if (pos != null) items[pos]
                                                    else             null}

    init {
        layoutManager = LinearLayoutManager(context)
        setItemViewCacheSize(10)
        setHasFixedSize(true)

        val itemDecoration = AlternatingRowBackgroundDecoration(context)
        addItemDecoration(itemDecoration)
        setAdapter(adapter)
    }

    inner class PopupInventoryAdapter(context: Context) :
            RecyclerView.Adapter<PopupInventoryAdapter.InventoryItemViewHolder>() {
        private val layoutInflater = LayoutInflater.from(context)
        private val multiplyIcon = context.getDrawable(R.drawable.shopping_list_animated_multiply_to_minus_icon)
        private val selectedColor: Int

        init {
            setHasStableIds(true)
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
            selectedColor = typedValue.data
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :
                InventoryItemViewHolder {
            val view = layoutInflater.inflate(R.layout.inventory_item_popup_layout, parent, false)
            return InventoryItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: InventoryItemViewHolder, position: Int) =
            holder.bindTo(items[position])

        override fun onBindViewHolder(holder: InventoryItemViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.size != 1 || payloads[0] !is Boolean)
                return onBindViewHolder(holder, position)
            val startColor = if (payloads[0] as Boolean) 0 else selectedColor
            val endColor =   if (payloads[0] as Boolean) selectedColor else 0
            ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
                startColor, endColor).start()
        }

        override fun getItemId(position: Int): Long = items[position].id

        inner class InventoryItemViewHolder(private val view: View) :
                RecyclerView.ViewHolder(view) {
            init {
                view.amountEdit.decreaseButton.background = multiplyIcon
                view.amountEdit.increaseButton.background = null

                val onClick = OnClickListener {
                    val oldSelectedItemPos = selectedItemPos
                    selectedItemPos = adapterPosition
                    if (oldSelectedItemPos != null)
                        notifyItemChanged(oldSelectedItemPos, false)
                    notifyItemChanged(adapterPosition, true)
                }
                view.setOnClickListener(onClick)
                view.nameEdit.setOnClickListener(onClick)
                view.amountEdit.valueEdit.setOnClickListener(onClick)
                view.extraInfoEdit.setOnClickListener(onClick)
                view.amountEdit.decreaseButton.setOnClickListener(onClick)
                view.amountEdit.increaseButton.setOnClickListener(onClick)

                val onLongClick = OnLongClickListener { view ->
                    onClick.onClick(view); true
                }
                view.setOnLongClickListener(onLongClick)
                view.nameEdit.setOnLongClickListener(onLongClick)
                view.amountEdit.valueEdit.setOnLongClickListener(onLongClick)
                view.extraInfoEdit.setOnLongClickListener(onLongClick)
                view.amountEdit.decreaseButton.setOnLongClickListener(onLongClick)
                view.amountEdit.increaseButton.setOnLongClickListener(onLongClick)
            }

            fun bindTo(item: InventoryItem) {
                view.nameEdit.text = item.name
                view.amountEdit.currentValue = item.amount
                view.extraInfoEdit.text = item.extraInfo
                if (item.id == initiallySelectedItemId) selectedItemPos = adapterPosition
                if (selectedItemPos == adapterPosition) itemView.setBackgroundColor(selectedColor)
                else                                    itemView.background = null
            }
        }
    }
}