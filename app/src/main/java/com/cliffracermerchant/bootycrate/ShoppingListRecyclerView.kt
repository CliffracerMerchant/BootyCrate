package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*

class ShoppingListRecyclerView(context: Context, attrs: AttributeSet) :
        RecyclerView(context, attrs),
        Observer<List<ShoppingListItem>> {

    private val adapter = ShoppingListAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    var bottomBar: BottomAppBar? = null
    val selection = RecyclerViewSelection(adapter)

    /** The enum class Field identifies user facing fields
     *  that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, AmountInCart, Amount, LinkedTo  }

    /** ItemChange is a simple pair of a Field value identifying a changed
     *  field, and the new value of that field. It is used as a payload in the
     *  RecyclerView.Adapter notifyItemChanged calls in order to support par-
     *  tial binding. */
    class ItemChange(field: Field, value: Any) {
        private val pair = Pair(field, value)
        val editable = pair.first
        val value = pair.second
    }

    init {
        layoutManager = LinearLayoutManager(context)
        setItemViewCacheSize(10)
        setHasFixedSize(true)
        ItemTouchHelper(SwipeToDeleteItemTouchHelperCallback(::deleteItem)).
        attachToRecyclerView(this)

        val itemDecoration = AlternatingRowBackgroundDecoration(context)
        addItemDecoration(itemDecoration)
        setAdapter(adapter)
    }

    fun setViewModels(owner: LifecycleOwner,
                     shoppingListViewModel: ShoppingListViewModel,
                     inventoryViewModel: InventoryViewModel) {
        this.shoppingListViewModel = shoppingListViewModel
        shoppingListViewModel.getAll().observe(owner, this)
        this.inventoryViewModel = inventoryViewModel
    }

    fun addNewItem() = shoppingListViewModel.insert(ShoppingListItem(
        context.getString(R.string.shopping_list_item_default_name)))

    fun deleteItem(pos: Int) = deleteItems(pos)

    fun deleteItems(vararg positions: Int) {
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        shoppingListViewModel.delete(*ids)
        val text = context.getString(R.string.delete_snackbar_text, ids.size)
        val snackBar = Snackbar.make(this, text, Snackbar.LENGTH_LONG)
        snackBar.anchorView = bottomBar ?: this
        snackBar.setAction(R.string.delete_snackbar_undo_text) { undoDelete() }
        snackBar.show()
    }

    fun undoDelete() = shoppingListViewModel.undoDelete()

    override fun onChanged(items: List<ShoppingListItem>) = listDiffer.submitList(items)

    /** */
    inner class ShoppingListAdapter(context: Context) :
            RecyclerView.Adapter<ShoppingListAdapter.ShoppingListItemViewHolder>() {
        private val selectedColor: Int

        init {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
            selectedColor = typedValue.data
            setHasStableIds(true)
        }

        override fun getItemCount(): Int = listDiffer.currentList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :
                ShoppingListItemViewHolder {
            val view = ShoppingListItemView(context)
            view.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                             ViewGroup.LayoutParams.WRAP_CONTENT)
            return ShoppingListItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ShoppingListItemViewHolder, position: Int) =
            holder.bindTo(listDiffer.currentList[position])

        override fun onBindViewHolder(holder: ShoppingListItemViewHolder,
                                      position: Int,
                                      payloads: MutableList<Any>) {
            if (payloads.size == 0) return onBindViewHolder(holder, position)
            for (payload in payloads) {
                if (payload is Boolean) {
                    val startColor = if (payload) 0 else selectedColor
                    val endColor = if (payload) selectedColor else 0
                    ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
                                          startColor, endColor).start()
                } else if (payload is ItemChange) {
                    when (payload.editable) {
                        Field.Name -> {
                            assert(payload.value is String)
                            holder.itemView.nameEdit.setText(payload.value as String)
                        }
                        Field.ExtraInfo -> {
                            assert(payload.value is String)
                            holder.itemView.extraInfoEdit.setText(payload.value as String)
                        }
                        Field.AmountInCart -> {
                            assert(payload.value is Int)
                            holder.itemView.amountInCartEdit.currentValue = payload.value as Int
                        }
                        Field.Amount -> {
                            assert(payload.value is Int)
                            holder.itemView.amountOnListEdit.currentValue = payload.value as Int
                        }
                        Field.LinkedTo -> {
                            assert(payload.value is Long)
                            holder.view.updateLinkedStatus(payload.value as Long)
                        }
                    }
                }
            }
        }

        override fun getItemId(position: Int): Long = listDiffer.currentList[position].id

        inner class ShoppingListItemViewHolder(val view: ShoppingListItemView) :
            RecyclerView.ViewHolder(view) {

            init {
                // Click & long click listeners
                val onClick = OnClickListener {
                    if (!selection.isEmpty) selection.toggle(adapterPosition)
                }
                view.setOnClickListener(onClick)
                view.nameEdit.setOnClickListener(onClick)
                view.amountInCartEdit.valueEdit.setOnClickListener(onClick)
                view.amountOnListEdit.valueEdit.setOnClickListener(onClick)

                val onLongClick = OnLongClickListener {
                    selection.toggle(adapterPosition); true
                }
                view.setOnLongClickListener(onLongClick)
                view.nameEdit.setOnLongClickListener(onLongClick)
                view.amountInCartEdit.valueEdit.setOnLongClickListener(onLongClick)
                view.amountOnListEdit.valueEdit.setOnLongClickListener(onLongClick)

                view.nameEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    shoppingListViewModel.updateName(listDiffer.currentList[adapterPosition], value)
                }
                view.amountInCartEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    shoppingListViewModel.updateAmountInCart(listDiffer.currentList[adapterPosition], value)
                }
                view.amountOnListEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    shoppingListViewModel.updateAmount(listDiffer.currentList[adapterPosition], value)
                }
                view.extraInfoEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    shoppingListViewModel.updateExtraInfo(listDiffer.currentList[adapterPosition], value)
                }

                view.linkedToEdit.setOnClickListener {
                    val items = inventoryViewModel.getAll().value
                    if (items == null || items.isEmpty()) {
                        val string = context.getString(R.string.empty_inventory_message)
                        val snackBar = Snackbar.make(view, string, Snackbar.LENGTH_LONG)
                        snackBar.anchorView = bottomBar ?: view
                        snackBar.show()
                        return@setOnClickListener
                    }
                    // AlertDialog seems to ignore the theme's alertDialogTheme value,
                    // making this workaround necessary
                    val typedValue = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.alertDialogTheme, typedValue, true)
                    val builder = AlertDialog.Builder(context, typedValue.data)
                    builder.setTitle(context.getString(R.string.link_inventory_item_action_long_description))
                    val recyclerView = InventoryRecyclerViewDialog(context, items)
                    val dialogClickListener = DialogInterface.OnClickListener { _, button ->
                        if (button == DialogInterface.BUTTON_POSITIVE)
                            updateLinkedTo(recyclerView.selectedItemId())
                    }
                    builder.setPositiveButton(context.getString(android.R.string.ok), dialogClickListener)
                    builder.setNegativeButton(context.getString(android.R.string.cancel), dialogClickListener)
                    builder.setView(recyclerView)
                    builder.show()
                }
            }

            fun bindTo(item: ShoppingListItem) {
                view.update(item)
                if (selection.contains(adapterPosition)) itemView.setBackgroundColor(selectedColor)
                else                                     itemView.background = null
            }

            private fun updateLinkedTo(newLinkedItemId: Long?) {
                if (newLinkedItemId == null || newLinkedItemId == 0.toLong()) return
                shoppingListViewModel.updateLinkedInventoryItemId(
                    listDiffer.currentList[adapterPosition], newLinkedItemId)
            }
        }
    }

    internal inner class DiffUtilCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem == newItem

        override fun getChangePayload(oldItem: ShoppingListItem, newItem: ShoppingListItem): Any? {
            if (oldItem.name != newItem.name)
                return ItemChange(Field.Name, newItem.name)
            if (oldItem.extraInfo != newItem.extraInfo)
                return ItemChange(Field.ExtraInfo, newItem.extraInfo)
            if (oldItem.amountInCart != newItem.amountInCart)
                return ItemChange(Field.AmountInCart, newItem.amountInCart)
            if (oldItem.amount != newItem.amount)
                return ItemChange(Field.Amount, newItem.amount)
            if (oldItem.linkedInventoryItemId != newItem.linkedInventoryItemId)
                return ItemChange(Field.LinkedTo, newItem.linkedInventoryItemId ?: 0)
            return super.getChangePayload(oldItem, newItem)
        }
    }
}