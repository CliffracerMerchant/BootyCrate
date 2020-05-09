package com.cliffracermerchant.stuffcrate

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
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
 *  playing the contents of an InventoryManager inventory. InventoryRecycler-
 *  View is relatively simple class, due to much of its functionality being in
 *  its use of an InventoryAdapter. Because it is intended to be inflated from
 *  an XML layout before a valid InventoryViewModel can exist, an instance of
 *  InventoryViewModel must be passed along with an AndroidX LifecycleOwner to
 *  an instance of InventoryRecyclerView after it is created using the
 *  setViewModel function. If this is not done, the adapter will throw a
 *  kotlin.UninitializedPropertyAccessException when any type of data access is
 *  attempted.
 *     Adding or removing inventory items is accomplished using the functions
 *  addItem, deleteItem, and deleteItems. When items are deleted, a snackbar
 *  will appear informing the user of the amount of items that were deleted, as
 *  well as providing an undo option.
 *      The selection state of the adapter (in the form of a HashSet<Long>)
 *  containing the ids of each selected item) can be exported using
 *  selectionState(), and restored using restoreSelectionState(HashSet<Long>).*/
class InventoryRecyclerView(context: Context, attributes: AttributeSet) :
        RecyclerView(context, attributes) {

    val adapter = InventoryAdapter(context)

    init {
        layoutManager = LinearLayoutManager(context)
        setItemViewCacheSize(10)
        setHasFixedSize(true)
        ItemTouchHelper(SwipeToDeleteItemTouchHelperCallback(::deleteItem)).attachToRecyclerView(this)

        val itemDecoration = AlternatingRowBackgroundDecoration(context)
        addItemDecoration(itemDecoration)
        setAdapter(adapter)
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

    fun setViewModel(owner: LifecycleOwner, viewModel: InventoryViewModel) =
        adapter.setViewModel(owner, viewModel)

    fun addNewItem() = adapter.addNewItem()

    fun deleteItem(position: Int) = deleteItems(position)

    fun deleteItems(vararg positions: Int) {
        adapter.deleteItems(*positions)
        val text = context.getString(R.string.delete_snackbar_text, positions.size)
        val snackBar = Snackbar.make(this, text, Snackbar.LENGTH_LONG)
        snackBar.setAction(R.string.delete_snackbar_undo_text) { adapter.undoDelete() }
        snackBar.show()
    }

    fun selectionState() = adapter.selection.saveState()

    fun restoreSelectionState(selectionState: HashSet<Long>) = adapter.selection.restoreState(selectionState)

    enum class Editable {
        Expanded, Name, Amount, ExtraInfo,
        AutoAddToShoppingList, AutoAddToShoppingListTrigger
    }

    class ItemChange(editable: Editable, value: Any) {
        private val pair = Pair<Editable, Any>(editable, value)
        val editable = pair.first
        val value = pair.second
    }

    /**     InventoryAdapter is a subclass of SelectionEnabledAdapter using its
     *  own RecyclerView.ViewHolder subclasses to represent inventory items in
     *  an inventory. It also implements the Observer<List<InventoryItem>>
     *  interface as well as containing an internal AsyncListDiffer member that
     *  allows it to calculate changes made to the displayed data on a back-
     *  ground thread. */
    inner class InventoryAdapter(context: Context) :
        SelectionEnabledAdapter<InventoryAdapter.InventoryItemViewHolder>(),
        Observer<List<InventoryItem>> {
        private val inflater = LayoutInflater.from(context)
        private val listDiffer = AsyncListDiffer(this, DiffUtilCallback())
        private val selectedColor: Int
        private val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        private val newItemName = context.getString(R.string.inventory_item_default_name)
        private var expandedItemAdapterPos: Int? = null
        private lateinit var viewModel: InventoryViewModel

        init {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
            selectedColor = typedValue.data
        }

        fun addNewItem() = viewModel.insert(InventoryItem(newItemName))

        fun deleteItems(vararg positions: Int) {
            val ids = LongArray(positions.size) {
                assert(positions[it] in 0 until itemCount)
                listDiffer.currentList[positions[it]].id
            }
            viewModel.delete(*ids)
        }

        fun undoDelete() = viewModel.undoDelete()

        fun setViewModel(owner: LifecycleOwner, viewModel: InventoryViewModel) {
            this.viewModel = viewModel
            viewModel.getAll().observe(owner, this)
        }

        override fun getItemCount(): Int = listDiffer.currentList.size

        override fun onChanged(items: List<InventoryItem>) = listDiffer.submitList(items)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryItemViewHolder {
            val view = inflater.inflate(R.layout.inventory_item_layout, parent, false)
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
                        Editable.Expanded -> {
                            assert(payload.value is Boolean)
                            if (payload.value as Boolean) holder.expand()
                            else { //collapsing
                                holder.collapse()
                                expandedItemAdapterPos = null
                            }
                        }
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

        open inner class InventoryItemViewHolder(view: View) :
                RecyclerView.ViewHolder(view) {

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

                //imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
                //imm.hideSoftInputFromWindow(editor.windowToken, 0)

                view.autoAddToShoppingListCheckBox.setOnCheckedChangeListener { _, checked ->
                    viewModel.updateAutoAddToShoppingList(
                        listDiffer.currentList[adapterPosition], checked)
                }

                view.autoAddToShoppingListTriggerEdit.liveData.observeForever { value ->
                    if (adapterPosition != -1) {
                        val item = listDiffer.currentList[adapterPosition]
                        viewModel.updateAutoAddToShoppingListTrigger(item, value)
                    }
                }

                view.expandDetailsButton.setOnClickListener {
                    val oldExpandedItemAdapterPos = expandedItemAdapterPos
                    if (oldExpandedItemAdapterPos != null)
                        notifyItemChanged(oldExpandedItemAdapterPos,
                                          ItemChange(Editable.Expanded, false))
                    notifyItemChanged(adapterPosition, ItemChange(Editable.Expanded, true))
                }

                view.collapseDetailsButton.setOnClickListener {
                    notifyItemChanged(adapterPosition, ItemChange(Editable.Expanded, false))
                }
            }

            fun bindTo(item: InventoryItem) {
                itemView.nameEdit.setText(item.name)
                itemView.amountEdit.currentValue = item.amount
                //itemView.extraInfoEdit.setText(item.extraInfo)
                itemView.extraInfoEdit.setText("extra info")
                if (expandedItemAdapterPos == adapterPosition) expand()
                val selected = selection.contains(adapterPosition)
                if (selected) itemView.setBackgroundColor(selectedColor)
                else itemView.background = null
            }

            fun expand() {
                val item = listDiffer.currentList[adapterPosition]
                itemView.autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
                itemView.autoAddToShoppingListTriggerEdit.currentValue = item.autoAddToShoppingListTrigger
                itemView.amountEdit.editable = true
                itemView.nameEdit.inputType = InputType.TYPE_CLASS_TEXT
                itemView.extraInfoEdit.inputType = InputType.TYPE_CLASS_TEXT
                itemView.autoAddToShoppingListTriggerEdit.editable = true

                val anim = expandCollapseAnimation(true)
                anim.addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        itemView.expandDetailsButton.visibility = View.INVISIBLE
                    }
                    override fun onAnimationEnd(animation: Animator?) {
                        expandedItemAdapterPos = adapterPosition
                        itemView.collapseDetailsButton.visibility = View.VISIBLE
                    }
                })
                anim.start()
            }

            fun collapse() {
                itemView.amountEdit.editable = false
                itemView.nameEdit.inputType = InputType.TYPE_NULL
                itemView.extraInfoEdit.inputType = InputType.TYPE_NULL
                itemView.autoAddToShoppingListTriggerEdit.editable = false

                val anim = expandCollapseAnimation(false)
                anim.addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        itemView.collapseDetailsButton.visibility = View.INVISIBLE
                    }
                    override fun onAnimationEnd(animation: Animator?) {
                        itemView.expandDetailsButton.visibility = View.VISIBLE
                        itemView.inventoryItemDetailsInclude.visibility = View.GONE
                    }
                })
                anim.start()
            }

            private fun expandCollapseAnimation(expanding: Boolean) : ValueAnimator {
                val matchParentSpec = MeasureSpec.makeMeasureSpec(itemView.width,
                                                                  MeasureSpec.EXACTLY)
                val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                itemView.measure(matchParentSpec, wrapContentSpec)
                val startHeight = itemView.measuredHeight
                val heightChange: Int
                if (expanding) {
                    itemView.inventoryItemDetailsInclude.visibility = View.VISIBLE
                    itemView.measure(matchParentSpec, wrapContentSpec)
                    heightChange = itemView.measuredHeight - startHeight
                }
                else heightChange = -itemView.inventoryItemDetailsInclude.measuredHeight
                //val heightChange = endHeight - startHeight

                val anim = ValueAnimator.ofInt(startHeight, startHeight + heightChange)
                anim.addUpdateListener {
                    itemView.layoutParams.height =
                        startHeight + (anim.animatedFraction * heightChange).toInt()
                    itemView.requestLayout()
                }
                anim.duration = 200
                return anim
            }
        }

        internal inner class DiffUtilCallback : DiffUtil.ItemCallback<InventoryItem>() {
            override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
                // If the new item is not expanded, then a quicker compare
                // using only the values visible to the user can be used
                if (expandedItemAdapterPos != null &&
                    newItem.id != listDiffer.currentList[expandedItemAdapterPos!!].id)
                        return oldItem.id == newItem.id &&
                               oldItem.name == newItem.name &&
                               oldItem.amount == newItem.amount &&
                               oldItem.extraInfo == newItem.extraInfo
                return oldItem == newItem
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
}