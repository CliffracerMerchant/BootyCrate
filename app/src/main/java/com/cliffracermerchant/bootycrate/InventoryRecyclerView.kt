package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*

/**     InventoryRecyclerView is a RecyclerView subclass specialized for dis-
 *  playing the contents of an inventory. Because it is intended to be inflated
 *  from an XML layout before a valid InventoryViewModel can exist, an instance
 *  of InventoryViewModel must be passed along with an AndroidX LifecycleOwner
 *  to an instance of InventoryRecyclerView after it is created using the
 *  setViewModel function. If this is not done, a kotlin.UninitializedProperty-
 *  AccessException will be thrown when any type of data access is attempted.
 *  In order to allow it to calculate changes to the displayed data on a back-
 *  ground thread, it implements the Observer<List<InventoryItem>> interface
 *  and contains an AsyncListDiffer member.
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
    private var expandedItemAdapterPos: Int? = null
    //private var bottomBar: BottomAppBar? = null
    val selection = RecyclerViewSelection(adapter)

    /** The enum class Field identifies user facing fields
     *  that are potentially editable by the user. */
    enum class Field { Name, Amount, ExtraInfo,
                       AutoAddToShoppingList,
                       AutoAddToShoppingListTrigger }

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

    fun setViewModel(owner: LifecycleOwner, viewModel: InventoryViewModel) {
        this.viewModel = viewModel
        viewModel.getAll().observe(owner, this)
    }

    fun addNewItem() = viewModel.insert(InventoryItem(
        context.getString(R.string.inventory_item_default_name)))

    fun deleteItem(pos: Int) = deleteItems(pos)

    fun deleteItems(vararg positions: Int) {
        val expandedPos = expandedItemAdapterPos
        if (expandedPos != null && expandedPos in positions)
            expandedItemAdapterPos = null
        // no need to actually collapse the view since it is about to be removed
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        viewModel.delete(*ids)
        val text = context.getString(R.string.delete_snackbar_text, ids.size)
        val snackBar = Snackbar.make(/*bottomBar ?: */this, text, Snackbar.LENGTH_LONG)
        snackBar.anchorView = /*bottomBar ?: */this
        snackBar.setAction(R.string.delete_snackbar_undo_text) { undoDelete() }
        snackBar.show()
    }

    fun undoDelete() = viewModel.undoDelete()

    override fun onChanged(items: List<InventoryItem>) = listDiffer.submitList(items)

    /** InventoryAdapter is a subclass of SelectionEnabledAdapter using its own
     *  RecyclerView.ViewHolder subclass InventoryItemViewHolder to represent
     *  inventory items in an inventory. Its override of onBindViewHolder(View-
     *  Holder, Payload) makes use of InventoryRecyclerView.ItemChange instan-
     *  ces to support partial binding. It also modifies the background color
     *  of the item views to reflect their selected / not selected status. */
    inner class InventoryAdapter(context: Context) :
        RecyclerView.Adapter<InventoryAdapter.InventoryItemViewHolder>() {

        private val selectedColor: Int

        init {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
            selectedColor = typedValue.data
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
                    val startColor = if (payload) 0 else selectedColor
                    val endColor = if (payload) selectedColor else 0
                    val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
                    valueAnimator.duration = 300
                    valueAnimator.addUpdateListener { anim ->
                        holder.itemView.setBackgroundColor(ColorUtils.blendARGB(
                            startColor, endColor, anim.animatedValue as Float))
                    }
                    valueAnimator.start()
                } else if (payload is ItemChange) {
                    when (payload.editable) {
                        Field.Name -> {
                            assert(payload.value is String)
                            holder.itemView.nameEdit.setText(payload.value as String)
                        }
                        Field.Amount -> {
                            assert(payload.value is Int)
                            holder.itemView.amountEdit.currentValue = payload.value as Int
                        }
                        Field.ExtraInfo -> {
                            assert(payload.value is String)
                            holder.itemView.extraInfoEdit.setText(payload.value as String)
                        }
                        Field.AutoAddToShoppingList -> {
                            assert(payload.value is Boolean)
                            holder.itemView.autoAddToShoppingListCheckBox.isChecked =
                                payload.value as Boolean
                        }
                        Field.AutoAddToShoppingListTrigger -> {
                            assert(payload.value is Int)
                            holder.itemView.autoAddToShoppingListTriggerEdit.currentValue =
                                payload.value as Int
                        }
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
        inner class InventoryItemViewHolder(view: InventoryItemView) :
                RecyclerView.ViewHolder(view) {
            private val view = itemView as InventoryItemView

            init {
                val onClick = OnClickListener {
                    if (!selection.isEmpty) selection.toggle(adapterPosition)
                }
                view.setOnClickListener(onClick)
                view.nameEdit.setOnClickListener(onClick)
                view.amountEdit.valueEdit.setOnClickListener(onClick)
                view.extraInfoEdit.setOnClickListener(onClick)

                val onLongClick = OnLongClickListener { editor ->
                    selection.toggle(adapterPosition); true
                }
                view.setOnLongClickListener(onLongClick)
                view.nameEdit.setOnLongClickListener(onLongClick)
                view.amountEdit.valueEdit.setOnLongClickListener(onLongClick)
                view.extraInfoEdit.setOnLongClickListener(onLongClick)

                view.expandDetailsButton.setOnClickListener {
                    val oldExpandedItemAdapterPos = expandedItemAdapterPos
                    if (oldExpandedItemAdapterPos != null) {
                        val alreadyExpandedVH = findViewHolderForAdapterPosition(
                            oldExpandedItemAdapterPos) as InventoryItemViewHolder?
                        alreadyExpandedVH?.view?.collapse()
                    }
                    view.expand(listDiffer.currentList[adapterPosition])
                    expandedItemAdapterPos = adapterPosition
                }
                view.collapseDetailsButton.setOnClickListener {
                    view.collapse()
                    expandedItemAdapterPos = null
                }

                view.nameEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateName(listDiffer.currentList[adapterPosition], value)
                }
                view.amountEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmount(listDiffer.currentList[adapterPosition], value)
                }
                view.extraInfoEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateExtraInfo(listDiffer.currentList[adapterPosition], value)
                }
                view.autoAddToShoppingListCheckBox.setOnCheckedChangeListener { _, checked ->
                    viewModel.updateAutoAddToShoppingList(
                        listDiffer.currentList[adapterPosition], checked)
                }
                view.autoAddToShoppingListTriggerEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAutoAddToShoppingListTrigger(
                        listDiffer.currentList[adapterPosition], value)
                }
            }

            fun bindTo(item: InventoryItem) {
                val expanded = expandedItemAdapterPos == adapterPosition
                view.update(item, expanded)
                if (selection.contains(adapterPosition)) view.setBackgroundColor(selectedColor)
                else                                     view.background = null
            }
        }
    }

    internal inner class DiffUtilCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
            val expandedAdapterPos = expandedItemAdapterPos
            val range = 0 until listDiffer.currentList.size
            val expandedItemId = if (expandedAdapterPos != null && expandedAdapterPos in range)
                                    listDiffer.currentList[expandedAdapterPos].id
                                 else null
            // If the new item is not expanded, then a quicker compare
            // using only the values visible to the user can be used
            return if (newItem.id != expandedItemId)
                oldItem.id == newItem.id &&
                oldItem.name == newItem.name &&
                oldItem.amount == newItem.amount &&
                oldItem.extraInfo == newItem.extraInfo
            else oldItem == newItem
        }

        override fun getChangePayload(oldItem: InventoryItem, newItem: InventoryItem): Any? {
            if (oldItem.name != newItem.name)
                return ItemChange(Field.Name, newItem.name)
            if (oldItem.amount != newItem.amount)
                return ItemChange(Field.Amount, newItem.amount)
            if (oldItem.extraInfo != newItem.extraInfo)
                return ItemChange(Field.ExtraInfo, newItem.extraInfo)
            if (oldItem.autoAddToShoppingList != newItem.autoAddToShoppingList)
                return ItemChange(Field.AutoAddToShoppingList, newItem.autoAddToShoppingList)
            if (oldItem.autoAddToShoppingListTrigger != newItem.autoAddToShoppingListTrigger)
                return ItemChange(Field.AutoAddToShoppingListTrigger,
                                  newItem.autoAddToShoppingListTrigger)
            return super.getChangePayload(oldItem, newItem)
        }
    }
}