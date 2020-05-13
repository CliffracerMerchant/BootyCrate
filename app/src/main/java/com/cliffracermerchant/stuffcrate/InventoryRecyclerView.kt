package com.cliffracermerchant.stuffcrate

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
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
 *  well as providing an undo option.
 *      The selection state of the adapter (in the form of a HashSet<Long>)
 *  containing the ids of each selected item) can be exported using
 *  selectionState(), and restored using restoreSelectionState(HashSet<Long>). */

class InventoryRecyclerView(context: Context, attributes: AttributeSet) :
        RecyclerView(context, attributes),
        Observer<List<InventoryItem>> {

    val adapter = InventoryAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var viewModel: InventoryViewModel
    private var expandedItemAdapterPos: Int? = null

    /** The enum class Editable identifies user facing fields
     *  that are potentially editable by the user. */
    enum class Editable { Name, Amount, ExtraInfo,
                          AutoAddToShoppingList,
                          AutoAddToShoppingListTrigger }

    /** ItemChange is a simple pair of an Editable value identifying a changed
     *  field, and the new value of that field. It is used as a payload in the
     *  RecyclerView.Adapter notifyItemChanged calls in order to support par-
     *  tial binding. */
    class ItemChange(editable: Editable, value: Any) {
        private val pair = Pair(editable, value)
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
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

    fun setViewModel(owner: LifecycleOwner, viewModel: InventoryViewModel) {
        this.viewModel = viewModel
        viewModel.getAll().observe(owner, this)
    }

    fun addNewItem() = viewModel.insert(InventoryItem(
        context.getString(R.string.inventory_item_default_name)))

    fun deleteItem(position: Int) = deleteItems(position)

    fun deleteItems(vararg positions: Int) {
        val ids = LongArray(positions.size) {
            assert(positions[it] in 0 until listDiffer.currentList.size)
            listDiffer.currentList[positions[it]].id
        }
        viewModel.delete(*ids)
        val text = context.getString(R.string.delete_snackbar_text, positions.size)
        val snackBar = Snackbar.make(this, text, Snackbar.LENGTH_LONG)
        snackBar.setAction(R.string.delete_snackbar_undo_text) { undoDelete() }
        snackBar.show()
    }

    fun undoDelete() = viewModel.undoDelete()

    fun selectionState() = adapter.selection.saveState()

    fun restoreSelectionState(selectionState: HashSet<Long>) =
        adapter.selection.restoreState(selectionState)

    override fun onChanged(items: List<InventoryItem>) = listDiffer.submitList(items)

    /**     InventoryAdapter is a subclass of SelectionEnabledAdapter using its
     *  own RecyclerView.ViewHolder subclass InventoryItemViewHolder to repre-
     *  sent inventory items in an inventory. Its override of onBindViewHolder(
     *  ViewHolder, Payload) makes use of InventoryRecyclerView.ItemChange
     *  instances to support partial binding. It also modifies the background
     *  color of the item views to refect their selected / not selected status. */
    inner class InventoryAdapter(context: Context) :
        SelectionEnabledAdapter<InventoryAdapter.InventoryItemViewHolder>() {

        private val selectedColor: Int
        private val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        init {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
            selectedColor = typedValue.data
        }

        override fun getItemCount(): Int = listDiffer.currentList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :
                InventoryItemViewHolder {
            val view = InventoryItemLayout(context)
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
                    valueAnimator.addUpdateListener { valueAnimator ->
                        holder.itemView.setBackgroundColor(ColorUtils.blendARGB(
                            startColor, endColor, valueAnimator.animatedValue as Float))
                    }
                    valueAnimator.start()
                } else if (payload is ItemChange) {
                    when (payload.editable) {
                        Editable.Name -> {
                            assert(payload.value is String)
                            holder.itemView.nameEdit.setText(payload.value as String)
                        }
                        Editable.Amount -> {
                            assert(payload.value is Int)
                            holder.itemView.amountEdit.currentValue = payload.value as Int
                        }
                        Editable.ExtraInfo -> {
                            assert(payload.value is String)
                            holder.itemView.extraInfoEdit.setText(payload.value as String)
                        }
                        Editable.AutoAddToShoppingList -> {
                            assert(payload.value is Boolean)
                            holder.itemView.autoAddToShoppingListCheckBox.isChecked =
                                payload.value as Boolean
                        }
                        Editable.AutoAddToShoppingListTrigger -> {
                            assert(payload.value is Int)
                            holder.itemView.autoAddToShoppingListTriggerEdit.currentValue =
                                payload.value as Int
                        }
                    }
                }
            }
        }

        override fun getItemId(position: Int) = listDiffer.currentList[position].id

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
        inner class InventoryItemViewHolder(view: InventoryItemLayout) :
                RecyclerView.ViewHolder(view) {
            val view = itemView as InventoryItemLayout

            init {
                view.setOnClickListener {
                    if (!selection.isEmpty) selection.toggle(adapterPosition)
                }
                val onClick = OnClickListener { editor ->
                    if (adapterPosition == expandedItemAdapterPos) {
                        (editor as EditText).requestFocus()
                        imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
                    } else if (!selection.isEmpty) selection.toggle(adapterPosition)
                }
                view.nameEdit.setOnClickListener(onClick)
                view.extraInfoEdit.setOnClickListener(onClick)

                view.setOnLongClickListener {
                    selection.toggle(adapterPosition); true
                }
                val onLongClick = OnLongClickListener { editor ->
                    if (adapterPosition == expandedItemAdapterPos)
                        (editor as EditText).requestFocus()
                    else selection.toggle(adapterPosition); true
                }
                view.nameEdit.setOnLongClickListener(onLongClick)
                view.extraInfoEdit.setOnLongClickListener(onLongClick)

                view.nameEdit.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE)
                        imm.hideSoftInputFromWindow(view.nameEdit.windowToken, 0)
                    actionId == EditorInfo.IME_ACTION_DONE
                }

                view.amountEdit.liveData.observeForever { value ->
                    if (adapterPosition != -1)
                        viewModel.updateAmount(listDiffer.currentList[adapterPosition], value)
                }

                view.autoAddToShoppingListCheckBox.setOnCheckedChangeListener { _, checked ->
                    viewModel.updateAutoAddToShoppingList(
                        listDiffer.currentList[adapterPosition], checked
                    )
                }

                view.autoAddToShoppingListTriggerEdit.liveData.observeForever { value ->
                    if (adapterPosition != -1) {
                        val item = listDiffer.currentList[adapterPosition]
                        viewModel.updateAutoAddToShoppingListTrigger(item, value)
                    }
                }

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
            }

            fun bindTo(item: InventoryItem) {
                val expanded = expandedItemAdapterPos == adapterPosition
                view.update(item, expanded)
                val selected = selection.contains(adapterPosition)
                if (selected) view.setBackgroundColor(selectedColor)
                else          view.background = null
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
                return ItemChange(Editable.Name, newItem.name)
            if (oldItem.amount != newItem.amount)
                return ItemChange(Editable.Amount, newItem.amount)
            if (oldItem.extraInfo != newItem.extraInfo)
                return ItemChange(Editable.ExtraInfo, newItem.extraInfo)
            if (oldItem.autoAddToShoppingList != newItem.autoAddToShoppingList)
                return ItemChange(Editable.AutoAddToShoppingList, newItem.autoAddToShoppingList)
            if (oldItem.autoAddToShoppingListTrigger != newItem.autoAddToShoppingListTrigger)
                return ItemChange(Editable.AutoAddToShoppingListTrigger,
                    newItem.autoAddToShoppingListTrigger)
            return super.getChangePayload(oldItem, newItem)
        }
    }
}