package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.extraInfoEdit
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.nameEdit
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

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
        RecyclerView(context, attrs) {

    private val adapter = ShoppingListAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    private var expandedItemId: Long? = null
    private var expandedViewHolder: ShoppingListAdapter.ShoppingListItemViewHolder? = null
    val checkedItems = ShoppingListCheckedItems()

    var fragmentManager: FragmentManager? = null
    var snackBarAnchor: View? = null
    val selection = RecyclerViewSelection(adapter)

    var sort: Sort? get() = viewModel.sort
                    set(value) { viewModel.sort = value }
    var searchFilter: String? get() = viewModel.searchFilter
        set(value) { viewModel.searchFilter = value }

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. Field values (in the form of an EnumSet<Field>) are
     *  used as payloads in the adapter notifyItemChanged calls in order to iden-
     *  tify which field was changed.*/
    enum class Field { Name, ExtraInfo, IsChecked,
                       AmountOnList, AmountInCart,
                       LinkedTo, Color  }

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
        // Resetting the newly inserted item id here prevents the recycler view
        // from always expanding the item with that id until a new one is inserted
        viewModel.resetNewlyInsertedItemId()
        sort = initialSort
        viewModel.items.observe(owner, Observer { items -> listDiffer.submitList(items) })
        this.inventoryViewModel = inventoryViewModel
    }

    fun setExpandedItem(newExpandedVh: ShoppingListAdapter.ShoppingListItemViewHolder?,
                        animateCollapse: Boolean = true, animateExpand: Boolean = true) {

        if (expandedItemId != null && expandedViewHolder?.itemId == expandedItemId)
            expandedViewHolder?.view?.collapse(animateCollapse)
        expandedItemId = newExpandedVh?.itemId
        expandedViewHolder = newExpandedVh
        newExpandedVh?.view?.expand(animateExpand)
    }

    fun addNewItem() = viewModel.insert(ShoppingListItem())

    fun deleteItem(pos: Int) = deleteItems(pos)

    fun deleteItems(vararg positions: Int) {
        val expandedItemId = this.expandedItemId
        if (expandedItemId in positions.map{ adapter.getItemId(it) })
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

    fun undoDelete() {
        viewModel.undoDelete()
    }

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

    fun addItemsToInventory(vararg positions: Int) {
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        inventoryViewModel.insertFromShoppingListItems(*ids)
    }

    fun checkout() = viewModel.checkOut()

    /** ShoppingListAdapter is a subclass of RecyclerView.Adapter using its own
     *  RecyclerView.ViewHolder subclass ShoppingListItemViewHolder to represent shop-
     *  ping list items. Its override of onBindViewHolder(ViewHolder, Payload) makes use
     *  of ShoppingListRecyclerView.Field values to support partial binding. It also
     *  modifies the background color of the item views to reflect their selected / not
     *  selected status. */
    inner class ShoppingListAdapter(context: Context) :
            RecyclerView.Adapter<ShoppingListAdapter.ShoppingListItemViewHolder>() {
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
                ShoppingListItemViewHolder {
                val view = ShoppingListItemView(context)
                view.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
                return ShoppingListItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ShoppingListItemViewHolder, position: Int) =
            holder.bindTo(listDiffer.currentList[position])

        override fun onBindViewHolder(holder: ShoppingListItemViewHolder, position: Int,
                                      payloads: MutableList<Any>) {
            if (payloads.size == 0) return onBindViewHolder(holder, position)
            for (payload in payloads) {
                if (payload is Boolean) {
                    val startColor = if (payloads[0] as Boolean) 0 else selectedColor
                    val endColor =   if (payloads[0] as Boolean) selectedColor else 0
                    ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
                                          startColor, endColor).start()
                } else if (payload is EnumSet<*>) {
                    val item = listDiffer.currentList[position]
                    val changes = payload as EnumSet<Field>
                    if (changes.contains(Field.Name))         holder.itemView.nameEdit.setText(item.name)
                    if (changes.contains(Field.ExtraInfo))    holder.view.extraInfoEdit.setText(item.extraInfo)
                    if (changes.contains(Field.IsChecked))    holder.view.checkBox.isChecked = item.isChecked
                    if (changes.contains(Field.AmountOnList)) holder.view.amountOnListEdit.currentValue = item.amountOnList
                    if (changes.contains(Field.AmountInCart)) holder.view.amountInCartEdit.currentValue = item.amountInCart
                    if (changes.contains(Field.LinkedTo))     holder.view.updateLinkedStatus(item.linkedInventoryItemId)
                    if (changes.contains(Field.Color)) {
                            val checkBoxBg = (holder.view.checkBox.background as LayerDrawable).getDrawable(0)
                            val startColor = holder.view.itemColor ?: 0
                            holder.view.itemColor = item.color
                            val endColor = item.color
                            ObjectAnimator.ofArgb(checkBoxBg, "tint", startColor, endColor).start()
                    }
                }
            }
        }

        override fun getItemId(position: Int): Long = listDiffer.currentList[position].id

        inner class ShoppingListItemViewHolder(val view: ShoppingListItemView) :
                RecyclerView.ViewHolder(view) {
            val item: ShoppingListItem get() = listDiffer.currentList[adapterPosition]

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

                view.editButton.setOnClickListener { if (!view.isExpanded) setExpandedItem(this) }
                view.collapseButton.setOnClickListener { setExpandedItem(null) }

                view.nameEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateName(item.id, value)
                    val linkedId = item.linkedInventoryItemId
                    if (linkedId != null) inventoryViewModel.updateName(linkedId, value)
                }
                view.extraInfoEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateExtraInfo(item.id, value)
                    val linkedId = item.linkedInventoryItemId
                    if (linkedId != null) inventoryViewModel.updateExtraInfo(linkedId, value)
                }
                view.editColorButton.setOnClickListener {
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
                view.amountOnListEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmountOnList(item.id, value)
                }
                view.amountInCartEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmountInCart(item.id, value)
                }
                view.linkedToEdit.setOnClickListener {
                    val items = inventoryViewModel.items.value
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

                view.checkBox.isClickable = false
                view.checkBox.setOnClickListener { viewModel.updateIsChecked(item.id, view.checkBox.isChecked) }
                view.checkBox.setOnCheckedChangeListener { checkBox, checked ->
                    view.defaultOnCheckedChangeListener.onCheckedChanged(checkBox, checked)
                    Log.d("update", "overridden onCheckedChangeListener called")
                    if (checked) checkedItems.add(adapterPosition)
                    else         checkedItems.remove(adapterPosition)
                }
            }

            fun bindTo(item: ShoppingListItem) {
                view.update(item, itemId == expandedItemId)
                if (item.id == viewModel.newlyInsertedItemId) {
                    setExpandedItem(this, animateCollapse = true, animateExpand = false)
                    imm?.hideSoftInputFromWindow(nameEdit.windowToken, 0)
                    view.nameEdit.requestFocus()
                    imm?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
                    viewModel.resetNewlyInsertedItemId()
                }
                if (selection.contains(adapterPosition)) view.setBackgroundColor(selectedColor)
                else                                     view.background = null
            }

            private fun updateLinkedTo(newLinkedItem: InventoryItem?) {
                if (newLinkedItem == null || newLinkedItem.id == 0L) return
                viewModel.updateLinkedInventoryItemId(
                    listDiffer.currentList[adapterPosition].id, newLinkedItem)
            }
        }
    }

    internal inner class DiffUtilCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        private val listChanges = mutableMapOf<Long, EnumSet<Field>>()
        private val itemChanges = EnumSet.noneOf(Field::class.java)

        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ShoppingListItem,
                                        newItem: ShoppingListItem): Boolean {
            itemChanges.clear()
            if (newItem.name != oldItem.name)                 itemChanges.add(Field.Name)
            if (newItem.extraInfo != oldItem.extraInfo)       itemChanges.add(Field.ExtraInfo)
            if (newItem.color != oldItem.color)               itemChanges.add(Field.Color)
            if (newItem.isChecked != oldItem.isChecked)       itemChanges.add(Field.IsChecked)
            if (newItem.amountOnList != oldItem.amountOnList) itemChanges.add(Field.AmountOnList)
            if (newItem.amountInCart != oldItem.amountInCart) itemChanges.add(Field.AmountInCart)
            if (newItem.linkedInventoryItemId != oldItem.linkedInventoryItemId) itemChanges.add(Field.LinkedTo)
            if (!itemChanges.isEmpty()) listChanges[newItem.id] = EnumSet.copyOf(itemChanges)
            return itemChanges.isEmpty()
        }

        override fun getChangePayload(oldItem: ShoppingListItem,
                                      newItem: ShoppingListItem): Any? {
            return listChanges.remove(newItem.id)
        }
    }

    inner class ShoppingListCheckedItems() :
            RecyclerView.AdapterDataObserver() {
        private val hashSet = HashSet<Int>()
        private val _sizeLiveData = MutableLiveData(hashSet.size)

        val size: Int get() = hashSet.size
        val sizeLiveData: LiveData<Int> = _sizeLiveData
        val isEmpty: Boolean get() = hashSet.isEmpty()

        init { adapter.registerAdapterDataObserver(this) }

        fun add(pos: Int) {
            if (pos !in 0 until adapter.itemCount) return
            hashSet.add(pos)
            _sizeLiveData.value = size
            Log.d("update", "item checked, checked items size now " + hashSet.size)
        }

        fun remove(pos: Int) {
            if (pos !in 0 until adapter.itemCount) return
            hashSet.remove(pos)
            _sizeLiveData.value = size
            Log.d("update", "item unchecked, checked items size now " + hashSet.size)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {

            for (pos in positionStart until positionStart + itemCount)
                if (hashSet.contains(pos)) remove(pos)
            Log.d("update", "item removed, checked items size now " + hashSet.size)
            _sizeLiveData.value = size
        }
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
//            for (pos in positionStart until positionStart + itemCount)
//                if (listDiffer.currentList[pos].isChecked) hashSet.add(pos)
//            _sizeLiveData.value = size
            Log.d("update", "item inserted, checked items size now " + hashSet.size)
        }
    }
}