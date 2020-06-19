package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*

/**     ShoppingListRecyclerView is a RecyclerView subclass specialized for
 *  displaying the contents of a shopping list. Because it is intended to be
 *  inflated from an XML layout before a valid InventoryViewModel can exist,
 *  instances of ShoppingListViewModel and InventoryViewModel must be passed
 *  along with an AndroidX LifecycleOwner to an instance of ShoppingListRecy-
 *  clerView after it is created using the setViewModel function. If this is
 *  not done, a kotlin.UninitializedPropertyAccessException will be thrown when
 *  any type of data access is attempted. In order to allow it to calculate
 *  changes to the displayed data on a background thread, it implements the
 *  Observer<List<InventoryItem>> interface and contains an AsyncListDiffer
 *  member.
 *      Adding or removing shopping list items is accomplished using the func-
 *  tions addItem, deleteItem, and deleteItems. When items are deleted, a
 *  snackbar will appear informing the user of the amount of items that were
 *  deleted, as well as providing an undo option. */
class ShoppingListRecyclerView(context: Context, attrs: AttributeSet) :
        RecyclerView(context, attrs),
        Observer<List<ShoppingListItem>> {

    private val adapter = ShoppingListAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    var snackBarAnchor: BottomAppBar? = null
    val selection = RecyclerViewSelection(adapter)
    var sort: Sort? get() = viewModel.sort
                    set(value) { viewModel.sort = value }
    var searchFilter: String? get() = viewModel.searchFilter
        set(value) { viewModel.searchFilter = value }

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. Field values are used as payloads in the adapter
     *  notifyItemChanged calls in order to identify which field was changed.*/
    enum class Field { Name, ExtraInfo, Amount, AmountInCart, LinkedTo  }

    init {
        layoutManager = LinearLayoutManager(context)
        setItemViewCacheSize(10)
        setHasFixedSize(true)
        ItemTouchHelper(SwipeToDeleteCallback(::deleteItem, context)).attachToRecyclerView(this)
        addItemDecoration(AlternatingRowBackgroundDecoration(context))
        setAdapter(adapter)
    }

    fun setViewModels(owner: LifecycleOwner,
                     shoppingListViewModel: ShoppingListViewModel,
                     inventoryViewModel: InventoryViewModel,
                     initialSort: Sort) {
        viewModel = shoppingListViewModel
        sort = initialSort
        shoppingListViewModel.getAll().observe(owner, this)
        this.inventoryViewModel = inventoryViewModel
    }

    fun addNewItem() = viewModel.insert(ShoppingListItem(
        context.getString(R.string.shopping_list_item_default_name)))

    fun deleteItem(pos: Int) = deleteItems(pos)

    fun deleteItems(vararg positions: Int) {
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        viewModel.delete(*ids)
        val text = context.getString(R.string.delete_snackbar_text, ids.size)
        val snackBar = Snackbar.make(this, text, Snackbar.LENGTH_LONG)
        snackBar.anchorView = snackBarAnchor ?: this
        snackBar.setAction(R.string.delete_snackbar_undo_text) { undoDelete() }
        snackBar.addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                viewModel.emptyTrash()
            }
        })
        snackBar.show()
    }

    fun undoDelete() = viewModel.undoDelete()

    fun deleteAll() {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.alertDialogTheme, typedValue, true)
        val builder = AlertDialog.Builder(context, typedValue.data)
        val dialogClickListener = DialogInterface.OnClickListener { _, button ->
            if (button == DialogInterface.BUTTON_POSITIVE) {
                viewModel.deleteAll()
                viewModel.emptyTrash()
            }
        }
        builder.setPositiveButton(context.getString(android.R.string.yes), dialogClickListener)
        builder.setNegativeButton(context.getString(android.R.string.cancel), dialogClickListener)
        builder.setMessage(context.getString(R.string.delete_all_shopping_list_items_confirmation_message))
        builder.show()
    }

    override fun onChanged(items: List<ShoppingListItem>) = listDiffer.submitList(items)

    /** ShoppingListAdapter is a subclass of RecyclerView.Adapter using its own
     *  RecyclerView.ViewHolder subclass ShoppingListItemViewHolder to repre-
     *  sent shopping list items. Its override of onBindViewHolder(View-
     *  Holder, Payload) makes use of ShoppingListRecyclerView.Field values to
     *  support partial binding. It also modifies the background color of the
     *  item views to reflect their selected / not selected status. */
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
                    val startColor = if (payloads[0] as Boolean) 0 else selectedColor
                    val endColor =   if (payloads[0] as Boolean) selectedColor else 0
                    ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
                                          startColor, endColor).start()
                } else if (payload is Field) {
                    val item = listDiffer.currentList[position]
                    when (payload) {
                        Field.Name -> holder.itemView.nameEdit.setText(item.name)
                        Field.ExtraInfo -> holder.itemView.extraInfoEdit.setText(item.extraInfo)
                        Field.Amount ->holder.itemView.amountOnListEdit.currentValue = item.amount
                        Field.AmountInCart -> holder.itemView.amountInCartEdit.currentValue = item.amountInCart
                        Field.LinkedTo -> holder.view.updateLinkedStatus(item.linkedInventoryItemId)
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
                    val item = listDiffer.currentList[adapterPosition]
                    viewModel.updateName(item.id, value)
                    val linkedId = item.linkedInventoryItemId
                    if (linkedId != null) inventoryViewModel.updateName(linkedId, value)
                }
                view.amountInCartEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmountInCart(listDiffer.currentList[adapterPosition].id, value)
                }
                view.amountOnListEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmount(listDiffer.currentList[adapterPosition].id, value)
                }
                view.extraInfoEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    val item = listDiffer.currentList[adapterPosition]
                    viewModel.updateExtraInfo(item.id, value)
                    val linkedId = item.linkedInventoryItemId
                    if (linkedId != null) inventoryViewModel.updateExtraInfo(linkedId, value)
                }

                view.linkedToEdit.setOnClickListener {
                    val items = inventoryViewModel.getAll().value
                    if (items == null || items.isEmpty()) {
                        val string = context.getString(R.string.empty_inventory_message)
                        val snackBar = Snackbar.make(view, string, Snackbar.LENGTH_LONG)
                        snackBar.anchorView = snackBarAnchor ?: view
                        snackBar.show()
                        return@setOnClickListener
                    }
                    // AlertDialog seems to ignore the theme's alertDialogTheme value,
                    // making this workaround necessary
                    val typedValue = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.alertDialogTheme, typedValue, true)
                    val builder = AlertDialog.Builder(context, typedValue.data)
                    builder.setTitle(context.getString(R.string.link_inventory_item_action_long_description))
                    val recyclerView = InventoryRecyclerViewDialog(context, items,
                            listDiffer.currentList[adapterPosition].linkedInventoryItemId)
                    val dialogClickListener = DialogInterface.OnClickListener { _, button ->
                        if (button == DialogInterface.BUTTON_POSITIVE)
                            updateLinkedTo(recyclerView.selectedItem())
                    }
                    builder.setPositiveButton(context.getString(android.R.string.ok), dialogClickListener)
                    builder.setNegativeButton(context.getString(android.R.string.cancel), dialogClickListener)
                    builder.setView(recyclerView)
                    builder.show()
                }
            }

            fun bindTo(item: ShoppingListItem) {
                view.update(item)
                if (selection.contains(adapterPosition)) view.setBackgroundColor(selectedColor)
                else                                     view.background = null
            }

            private fun updateLinkedTo(newLinkedItem: InventoryItem?) {
                if (newLinkedItem == null || newLinkedItem.id == 0.toLong()) return
                viewModel.updateLinkedInventoryItemId(
                    listDiffer.currentList[adapterPosition].id, newLinkedItem)
            }
        }
    }

    internal inner class DiffUtilCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem == newItem

        override fun getChangePayload(oldItem: ShoppingListItem, newItem: ShoppingListItem): Any? {
            return when {
                oldItem.name != newItem.name -> Field.Name
                oldItem.extraInfo != newItem.extraInfo -> Field.ExtraInfo
                oldItem.amount != newItem.amount -> Field.Amount
                oldItem.amountInCart != newItem.amountInCart -> Field.AmountInCart
                oldItem.linkedInventoryItemId != newItem.linkedInventoryItemId -> Field.LinkedTo
                else -> super.getChangePayload(oldItem, newItem)
            }
        }
    }
}