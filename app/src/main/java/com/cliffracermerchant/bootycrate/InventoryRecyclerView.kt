package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.sasikanth.colorsheet.ColorSheet
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
 *  displayed data on a background thread, it also contains an AsyncListDiffer
 *  member.
 *     Adding or removing inventory items is accomplished using the functions
 *  addItem, deleteItem, and deleteItems. When items are deleted, a snackbar
 *  will appear informing the user of the amount of items that were deleted, as
 *  well as providing an undo option. */

class InventoryRecyclerView(context: Context, attrs: AttributeSet) :
        RecyclerView(context, attrs) {

    private val adapter = InventoryAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var viewModel: InventoryViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private var expandedItemId: Long? = null
    private var expandedViewHolder: InventoryAdapter.InventoryItemViewHolder? = null
    var fragmentManager: FragmentManager? = null
    var snackBarAnchor: View? = null
    val selection = RecyclerViewSelection(adapter)
    var sort: Sort? get() = viewModel.sort
                    set(value) { viewModel.sort = value }
    var searchFilter: String? get() = viewModel.searchFilter
                              set(value) { viewModel.searchFilter = value }

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. Field values are used as payloads in the adapter
     *  notifyItemChanged calls in order to identify which field was changed.*/
    enum class Field { Name, Amount, ExtraInfo, AutoAddToShoppingList,
                       AutoAddToShoppingListTrigger, Color }

    init {
        layoutManager = LinearLayoutManager(context)
        setItemViewCacheSize(10)
        setHasFixedSize(true)
        ItemTouchHelper(SwipeToDeleteCallback(::deleteItem, context)).attachToRecyclerView(this)
        addItemDecoration(AlternatingRowBackgroundDecoration(context))
        setAdapter(adapter)
    }

    fun setViewModels(owner: LifecycleOwner,
                      inventoryViewModel: InventoryViewModel,
                      shoppingListViewModel: ShoppingListViewModel,
                      initialSort: Sort) {
        viewModel = inventoryViewModel
        // Resetting the newly inserted item id here prevents the recycler view
        // from always expanding the item with that id until a new one is inserted
        viewModel.resetNewlyInsertedItemId()
        sort = initialSort
        viewModel.items.observe(owner, Observer { items -> listDiffer.submitList(items) })
        this.shoppingListViewModel = shoppingListViewModel
    }

    fun setExpandedItem(newExpandedVh: InventoryAdapter.InventoryItemViewHolder?,
                        animateCollapse: Boolean = true, animateExpand: Boolean = true) {
        if (expandedItemId != null && expandedViewHolder?.itemId == expandedItemId)
            expandedViewHolder?.view?.collapse(animateCollapse)
        expandedItemId = newExpandedVh?.itemId
        expandedViewHolder = newExpandedVh
        newExpandedVh?.view?.expand(animateExpand)
    }

    fun addNewItem() = viewModel.insert(InventoryItem())

    fun deleteItem(pos: Int) = deleteItems(pos)

    fun deleteItems(vararg positions: Int) {
        val expandedItemId = this.expandedItemId
        if (expandedItemId != null && expandedItemId in positions.map { adapter.getItemId(it) })
            setExpandedItem(null)
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
        builder.setMessage(context.getString(R.string.delete_all_inventory_items_confirmation_message))
        builder.show()
    }

    fun addItemsToShoppingList(vararg positions: Int) {
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        shoppingListViewModel.insertFromInventoryItems(*ids)
    }

    fun checkAutoAddToShoppingList(position: Int) {
        val item = listDiffer.currentList[position]
        if (!item.autoAddToShoppingList) return
        if (item.amount < item.autoAddToShoppingListTrigger) {
            val minAmount = item.autoAddToShoppingListTrigger - item.amount
            shoppingListViewModel.autoAddFromInventoryItem(item.id, minAmount)
        }
    }

    /** InventoryAdapter is a subclass of RecyclerView.Adapter using its own
     *  RecyclerView.ViewHolder subclass InventoryItemViewHolder to represent
     *  inventory items. Its override of onBindViewHolder makes use of
     *  InventoryRecyclerView.Field values to support partial binding. It also
     *  modifies the background color of the item views to reflect their sel-
     *  ected / not selected status. */
    inner class InventoryAdapter(context: Context) :
        RecyclerView.Adapter<InventoryAdapter.InventoryItemViewHolder>() {

        private val selectedColor: Int
        private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

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
                    val startColor = if (payloads[0] as Boolean) 0 else selectedColor
                    val endColor =   if (payloads[0] as Boolean) selectedColor else 0
                    ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
                                          startColor, endColor).start()
                } else if (payload is Field) {
                    val item = listDiffer.currentList[position]
                    when (payload) {
                        Field.Name -> holder.itemView.nameEdit.setText(item.name)
                        Field.Amount -> {
                            holder.itemView.amountEdit.currentValue = item.amount
                            checkAutoAddToShoppingList(holder.adapterPosition)
                        }
                        Field.ExtraInfo -> holder.itemView.extraInfoEdit.setText(item.extraInfo)
                        Field.AutoAddToShoppingList -> {
                            holder.itemView.autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
                            checkAutoAddToShoppingList(holder.adapterPosition)
                        }
                        Field.AutoAddToShoppingListTrigger -> {
                            holder.itemView.autoAddToShoppingListTriggerEdit.currentValue = item.autoAddToShoppingListTrigger
                            checkAutoAddToShoppingList(holder.adapterPosition)
                        }
                        Field.Color -> {
                            val colorEditBg = holder.itemView.colorEdit.background as ColoredCircleDrawable
                            val startColor = colorEditBg.color
                            val endColor = item.color
                            ObjectAnimator.ofArgb(colorEditBg, "color", startColor, endColor).start()
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
        inner class InventoryItemViewHolder(val view: InventoryItemView) :
                RecyclerView.ViewHolder(view) {
            val item: InventoryItem get() = listDiffer.currentList[adapterPosition]

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

                view.editButton.setOnClickListener { setExpandedItem(this) }
                view.collapseButton.setOnClickListener { setExpandedItem(null) }

                view.colorEdit.setOnClickListener {
                    val colors = resources.getIntArray(R.array.color_picker_presets)
                    val selectedColor = if (item.color != 0) item.color
                                        else                 ColorSheet.NO_COLOR
                    val colorPicker = ColorSheet().colorPicker(colors, selectedColor, true) { color ->
                        val itemColor = if (color == ColorSheet.NO_COLOR) 0
                                        else color
                        viewModel.updateColor(item.id, itemColor)
                    }
                    colorPicker.show(fragmentManager!!)
                }

                view.nameEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateName(item.id, value)
                    shoppingListViewModel.updateNameFromLinkedInventoryItem(item.id, value)
                }
                view.amountEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmount(item.id, value)
                }
                view.extraInfoEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateExtraInfo(item.id, value)
                    shoppingListViewModel.updateExtraInfoFromLinkedInventoryItem(item.id, value)
                }
                view.autoAddToShoppingListCheckBox.setOnCheckedChangeListener { _, checked ->
                    viewModel.updateAutoAddToShoppingList(item.id, checked)
                }
                view.autoAddToShoppingListTriggerEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAutoAddToShoppingListTrigger(item.id, value)
                }
            }

            fun bindTo(item: InventoryItem) {
                view.update(item, itemId == expandedItemId)
                if (item.id == viewModel.newlyInsertedItemId) {
                    setExpandedItem(this, animateCollapse = true, animateExpand = false)
                    imm?.hideSoftInputFromWindow(nameEdit.windowToken, 0)
                    view.nameEdit.requestFocus()
                    imm?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
                }
                if (selection.contains(adapterPosition)) view.setBackgroundColor(selectedColor)
                else                                     view.background = null
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
                oldItem.extraInfo != newItem.extraInfo -> Field.ExtraInfo
                oldItem.color != newItem.color -> Field.Color
                oldItem.amount != newItem.amount -> Field.Amount
                oldItem.autoAddToShoppingList != newItem.autoAddToShoppingList -> Field.AutoAddToShoppingList
                oldItem.autoAddToShoppingListTrigger != newItem.autoAddToShoppingListTrigger -> Field.AutoAddToShoppingListTrigger
                else -> super.getChangePayload(oldItem, newItem)
            }
        }
    }
}