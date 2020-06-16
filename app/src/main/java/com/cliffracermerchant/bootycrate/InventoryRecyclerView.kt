package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*

/**     InventoryRecyclerView is a RecyclerView subclass specialized for dis-
 *  playing the contents of an inventory. Because it is intended to be inflated
 *  from an XML layout before a valid InventoryViewModel can exist, instances
 *  of InventoryViewModel and ShoppingListViewModel must be passed along with
 *  an AndroidX LifecycleOwner to an instance of InventoryRecyclerView after it
 *  is created using the setViewModel function. If this is not done, a
 *  kotlin.UninitializedPropertyAccessException will be thrown when any type of
 *  data access is attempted. In order to allow it to calculate changes to the
 *  displayed data on a background thread, it implements the Observer<List<
 *  InventoryItem>> interface and contains an AsyncListDiffer member.
 *     Adding or removing inventory items is accomplished using the functions
 *  addItem, deleteItem, and deleteItems. When items are deleted, a snackbar
 *  will appear informing the user of the amount of items that were deleted, as
 *  well as providing an undo option. */

class InventoryRecyclerView(context: Context, attrs: AttributeSet) :
        RecyclerView(context, attrs),
        Observer<List<InventoryItem>> {

    private val adapter = InventoryAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var viewModel: InventoryViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private var expandedItemAdapterPos: Int? = null
    var snackBarAnchor: BottomAppBar? = null
    val selection = RecyclerViewSelection(adapter)

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. Field values are used as payloads in the adapter
     *  notifyItemChanged calls in order to identify which field was changed.*/
    enum class Field { Name, Amount, ExtraInfo,
                       AutoAddToShoppingList,
                       AutoAddToShoppingListTrigger }

    init {
        layoutManager = LinearLayoutManager(context)
        setItemViewCacheSize(10)
        setHasFixedSize(true)
        ItemTouchHelper(SwipeToDeleteCallback(::deleteItem, context)).attachToRecyclerView(this)
        setAdapter(adapter)
    }

    fun setViewModels(owner: LifecycleOwner,
                      inventoryViewModel: InventoryViewModel,
                      shoppingListViewModel: ShoppingListViewModel) {
        this.viewModel = inventoryViewModel
        inventoryViewModel.getAll().observe(owner, this)
        this.shoppingListViewModel = shoppingListViewModel
    }

    fun addNewItem() = viewModel.insert(InventoryItem(
        context.getString(R.string.inventory_item_default_name)))

    fun deleteItem(pos: Int) = deleteItems(pos)

    fun deleteItems(vararg positions: Int) {
        val expandedPos = expandedItemAdapterPos
        if (expandedPos != null && expandedPos in positions)
            expandedItemAdapterPos = null
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        viewModel.delete(*ids)
        val text = context.getString(R.string.delete_snackbar_text, ids.size)
        val snackBar = Snackbar.make(this, text, Snackbar.LENGTH_LONG)
        snackBar.anchorView = snackBarAnchor ?: this
        snackBar.setAction(R.string.delete_snackbar_undo_text) { undoDelete() }
        snackBar.show()
    }

    fun addItemToShoppingList(position: Int) = addItemsToShoppingList(position)

    fun addItemsToShoppingList(vararg positions: Int) {
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        shoppingListViewModel.insertFromInventoryItems(*ids)
    }

    fun undoDelete() = viewModel.undoDelete()

    override fun onChanged(items: List<InventoryItem>) = listDiffer.submitList(items)

    /** InventoryAdapter is a subclass of RecyclerView.Adapter using its own
     *  RecyclerView.ViewHolder subclass InventoryItemViewHolder to represent
     *  inventory items. Its override of onBindViewHolder makes use of
     *  InventoryRecyclerView.Field values to support partial binding. It also
     *  modifies the background color of the item views to reflect their sel-
     *  ected / not selected status. */
    inner class InventoryAdapter(context: Context) :
        RecyclerView.Adapter<InventoryAdapter.InventoryItemViewHolder>() {

        private val selectedColor: Int
        private val normalBgColor: Int
        private val altBgColor: Int

        init {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
            selectedColor = typedValue.data
            context.theme.resolveAttribute(R.attr.colorBackgroundVariant, typedValue, true)
            altBgColor = typedValue.data
            context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
            normalBgColor = typedValue.data
            setHasStableIds(true)
        }

        override fun getItemCount(): Int = listDiffer.currentList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :
                InventoryItemViewHolder {
            val view = InventoryItemView(context)
            view.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                             ViewGroup.LayoutParams.WRAP_CONTENT)
            return InventoryItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: InventoryItemViewHolder, position: Int) =
            holder.bindTo(listDiffer.currentList[position])

        override fun onBindViewHolder(holder: InventoryItemViewHolder, position: Int,
                                      payloads: MutableList<Any>) {
            if (payloads.size == 0) return onBindViewHolder(holder, position)
            for (payload in payloads) {
                if (payload is Boolean) {
                    val startColor = (holder.itemView.background as ColorDrawable).color
                    val endColor = when { payload ->                         selectedColor
                                          holder.adapterPosition % 2 == 0 -> normalBgColor
                                          else ->                            altBgColor }
                    ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
                                          startColor, endColor).start()
                } else if (payload is Field) {
                    val item = listDiffer.currentList[position]
                    when (payload) {
                        Field.Name -> holder.itemView.nameEdit.setText(item.name)
                        Field.Amount -> holder.itemView.amountEdit.currentValue = item.amount
                        Field.ExtraInfo -> holder.itemView.extraInfoEdit.setText(item.extraInfo)
                        Field.AutoAddToShoppingList -> holder.itemView.autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
                        Field.AutoAddToShoppingListTrigger -> holder.itemView.autoAddToShoppingListTriggerEdit.currentValue = item.autoAddToShoppingListTrigger
                    }
                }
            }
        }

        override fun getItemId(position: Int): Long = listDiffer.currentList[position].id

        /** InventoryItemViewHolder is a subclass of RecyclerView.ViewHolder
         *  that holds an instance of InventoryItemLayout to display the data
         *  for an InventoryItem. Besides its use of this custom item view, its
         *  differences from RecyclerView.ViewHolder are:
         * - It sets the on click listeners of each of the sub views in the
         *   InventoryItemLayout to permit the user to select/deselect items,
         *   and to edit the displayed data when allowed.
         * - Its override of the expand details button onClickListener enforces
         *   a one expanded item at a time rule, collapsing any other expanded
         *   view holder before the clicked one is expanded.
         * - its bindTo function checks the selected / not selected status of
         *   an item and updates its background color accordingly. */
        inner class InventoryItemViewHolder(private val view: InventoryItemView) :
                RecyclerView.ViewHolder(view) {

            init {
                val onClick = OnClickListener {
                    if (!selection.isEmpty) selection.toggle(adapterPosition)
                }
                view.setOnClickListener(onClick)
                view.nameEdit.setOnClickListener(onClick)
                view.amountEdit.valueEdit.setOnClickListener(onClick)
                view.extraInfoEdit.setOnClickListener(onClick)

                val onLongClick = OnLongClickListener {
                    selection.toggle(adapterPosition); true
                }
                view.setOnLongClickListener(onLongClick)
                view.nameEdit.setOnLongClickListener(onLongClick)
                view.amountEdit.valueEdit.setOnLongClickListener(onLongClick)
                view.extraInfoEdit.setOnLongClickListener(onLongClick)

                view.editButton.setOnClickListener {
                    if (!view.expanded) {
                        val oldExpandedItemAdapterPos = expandedItemAdapterPos
                        if (oldExpandedItemAdapterPos != null) {
                            val alreadyExpandedVH = findViewHolderForAdapterPosition(
                                oldExpandedItemAdapterPos) as InventoryItemViewHolder?
                            alreadyExpandedVH?.view?.collapse()
                        }
                        view.expand()
                        expandedItemAdapterPos = adapterPosition
                    }
                }
                view.collapseButton.setOnClickListener {
                    view.collapse()
                    expandedItemAdapterPos = null
                }

                view.nameEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    val id = listDiffer.currentList[adapterPosition].id
                    viewModel.updateName(id, value)
                    shoppingListViewModel.updateNameFromLinkedInventoryItem(id, value)
                }
                view.amountEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmount(listDiffer.currentList[adapterPosition].id, value)
                }
                view.extraInfoEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    val id = listDiffer.currentList[adapterPosition].id
                    viewModel.updateExtraInfo(id, value)
                    shoppingListViewModel.updateExtraInfoFromLinkedInventoryItem(id, value)
                }
                view.autoAddToShoppingListCheckBox.setOnCheckedChangeListener { _, checked ->
                    val id = listDiffer.currentList[adapterPosition].id
                    viewModel.updateAutoAddToShoppingList(id, checked)
                }
                view.autoAddToShoppingListTriggerEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    val id = listDiffer.currentList[adapterPosition].id
                    viewModel.updateAutoAddToShoppingListTrigger(id, value)
                }
            }

            fun bindTo(item: InventoryItem) {
                val expanded = expandedItemAdapterPos == adapterPosition
                view.update(item, expanded)
                view.setBackgroundColor(when {
                        selection.contains(adapterPosition) -> selectedColor
                        adapterPosition % 2 == 1 ->            altBgColor
                        else ->                                normalBgColor })
            }
        }
    }

    internal inner class DiffUtilCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean =
            oldItem == newItem

        override fun getChangePayload(oldItem: InventoryItem, newItem: InventoryItem): Any? {
            return when {
                oldItem.name != newItem.name -> Field.Name
                oldItem.amount != newItem.amount -> Field.Amount
                oldItem.extraInfo != newItem.extraInfo -> Field.ExtraInfo
                oldItem.autoAddToShoppingList != newItem.autoAddToShoppingList -> Field.AutoAddToShoppingList
                oldItem.autoAddToShoppingListTrigger != newItem.autoAddToShoppingListTrigger -> Field.AutoAddToShoppingListTrigger
                else -> super.getChangePayload(oldItem, newItem)
            }
        }
    }
}