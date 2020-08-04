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
import android.animation.ValueAnimator
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

/** A RecyclerView to display the data provided by a ShoppingListViewModel.
 *
 *  ShoppingListRecyclerView is a RecyclerView subclass specialized for display-
 *  ing the contents of a shopping list. Several of ShoppingListRecyclerView's
 *  necessary fields can not be obtained when it is inflated from XML, such as
 *  its viewmodels. To finish initialization with these required members, the
 *  function finishInit MUST be called after runtime, but before any sort of
 *  data access is attempted. An initial sort can be passed during this finish-
 *  Init call, and can thereafter be modified (along with a search filter)
 *  using the public properties sort and searchFilter.
 *
 *  In order to allow it to calculate changes to the displayed data on a back-
 *  ground thread, ShoppingListRecyclerView contains an AsyncListDiffer member.
 *  Its custom DiffUtilCallback dispatches an EnumSet<ShoppingListRecyclerView.
 *  Field> to indicate which fields of the inventory item need to be updated.
 *
 *  Adding or removing shopping list items is accomplished using the functions
 *  addItem, deleteItem, and deleteItems. An ItemTouchHelper with a SwipeTo-
 *  DeleteCallback is used to allow the user to call deleteItem on items that
 *  are swiped left or right. When items are deleted, a snackbar will appear
 *  informing the user of the amount of items that were deleted, as well as
 *  providing an undo option.
 *
 *  The snackbar that appears after deleting items will be anchored to the view
 *  set as the public property snackBarAnchor, in case this needs to be custom-
 *  ized, or to the ShoppingListRecyclerView otherwise. */
class ShoppingListRecyclerView(context: Context, attrs: AttributeSet) :
        RecyclerView(context, attrs) {

    private val adapter = ShoppingListAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    /* To simplify the user experience, only one shopping list item view is allowed
     * to be expanded at once. Keeping track of both the expanded item ID as well
     * as its ViewHolder allows ShoppingListRecyclerView to use expandedViewHolder
     * when its contained item id == expandedItemId (meaning that it hasn't been
     * rebound to a new item), thereby preventing a potentially costly
     * findViewHolderForItemId call. */
    private var expandedItemId: Long? = null
    private var expandedViewHolder: ShoppingListAdapter.ShoppingListItemViewHolder? = null
    val selection = RecyclerViewSelection(adapter)
    val checkedItems = ShoppingListCheckedItems()

    lateinit var fragmentManager: FragmentManager
    var snackBarAnchor: View? = null

    var sort: Sort? get() = viewModel.sort
                    set(value) { viewModel.sort = value }
    var searchFilter: String? get() = viewModel.searchFilter
        set(value) { viewModel.searchFilter = value }

    /** The enum class Field identifies user facing fields that are potentially
     *  editable by the user. Field values (in the form of an EnumSet<Field>)
     *  are used as a payload in the adapter notifyItemChanged calls in order
     *  to identify which fields were changed.*/
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

    fun finishInit(owner: LifecycleOwner,
                   shoppingListViewModel: ShoppingListViewModel,
                   inventoryViewModel: InventoryViewModel,
                   fragmentManager: FragmentManager,
                   initialSort: Sort? = null) {
        viewModel = shoppingListViewModel
        // Resetting the newly inserted item id here prevents the recycler view
        // from always expanding the item with that id until a new one is inserted
        viewModel.resetNewlyInsertedItemId()
        if (initialSort != null) sort = initialSort
        viewModel.items.observe(owner, Observer { items -> listDiffer.submitList(items) })
        this.inventoryViewModel = inventoryViewModel
        this.fragmentManager = fragmentManager
    }

    fun setExpandedItem(newExpandedVh: ShoppingListAdapter.ShoppingListItemViewHolder?,
                        animateCollapse: Boolean = true, animateExpand: Boolean = true) {

        if (expandedItemId != null && expandedViewHolder?.itemId == expandedItemId)
            expandedViewHolder?.view?.collapse(animateCollapse)
        expandedItemId = newExpandedVh?.itemId
        expandedViewHolder = newExpandedVh
        newExpandedVh?.view?.expand(animateExpand)
    }

    fun addNewItem() = newShoppingListItemDialog(context, fragmentManager) { newItem ->
        if (newItem != null) viewModel.insert(newItem)
    }//viewModel.insert(ShoppingListItem())

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

    fun addItemsToInventory(vararg positions: Int) {
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        inventoryViewModel.insertFromShoppingListItems(*ids)
    }

    fun checkout() = viewModel.checkOut()

    /** A RecyclerView.Adapter to display the contents of a list of shopping list items.
     *
     *  ShoppingListAdapter is a subclass of RecyclerView.Adapter using its own
     *  RecyclerView.ViewHolder subclass ShoppingListItemViewHolder to repre-
     *  sent shopping list items. Its override of onBindViewHolder(ViewHolder,
     *  Payload) makes use of ShoppingListRecyclerView.Field values to support
     *  partial binding. It also modifies the background color of the item
     *  views to reflect their selected / not selected status. */
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
                        holder.view.itemColor = item.color
                        val startColor = holder.view.itemColor ?: 0
                        val endColor = item.color
                        val anim = ValueAnimator.ofArgb(startColor, endColor)
                        anim.addUpdateListener { holder.view.checkBoxBackgroundController.tint = anim.animatedValue as Int }
                        anim.start()
                    }
                }
            }
        }

        override fun getItemId(position: Int): Long = listDiffer.currentList[position].id

        /** A RecyclerView.ViewHolder that wraps an instance of ShoppingListItemView.
         *
         *  ShoppingListItemViewHolder is a subclass of RecyclerView.ViewHolder
         *  that holds an instance of ShoppingListItemView to display the data
         *  for a ShoppingListItem. Besides its use of this custom item view,
         *  its differences from RecyclerView.ViewHolder are:
         * - It sets the on click listeners of each of the sub views in the
         *   ShoppingListItemView to permit the user to select/deselect items,
         *   and to edit the displayed data when allowed.
         * - Its override of the expand details button onClickListener calls
         *   ShoppingListRecyclerView.setExpandedItem on itself to enforce the
         *   one expanded item at a time rule.
         * - its bindTo function checks the selected / not selected status of
         *   an item and updates its background color accordingly. */
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
                    colorPickerDialog(context, fragmentManager) { pickedColor ->
                        viewModel.updateColor(item.id, pickedColor) }
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
                    selectInventoryItemDialog(context = context,
                                              inventoryItems = inventoryViewModel.items.value,
                                              initiallySelectedItemId = item.linkedInventoryItemId,
                                              snackBarAnchor = snackBarAnchor ?: itemView,
                                              callback = ::updateLinkedTo)
                }

                view.checkBox.isClickable = false
                view.checkBox.setOnClickListener { viewModel.updateIsChecked(item.id, view.checkBox.isChecked) }
                /* ShoppingListItemView's default onCheckedChangeListener has to be "overridden"
                 * here by replacing it with another implementation that calls ShoppingListItem-
                 * View.defaultOnCheckedChange so that checkedItems can keep track of which items
                 * are checked while still keeping the original onCheckedChange functionality. */
                view.checkBox.setOnCheckedChangeListener { _, checked ->
                    view.defaultOnCheckedChange(checked)
                    if (checked) checkedItems.add(adapterPosition)
                    else         checkedItems.remove(adapterPosition)
                }
            }

            fun bindTo(item: ShoppingListItem) {
                view.update(item, itemId == expandedItemId)
                if (item.id == viewModel.newlyInsertedItemId) {
                    setExpandedItem(this, animateCollapse = true, animateExpand = false)
                    smoothScrollToPosition(adapterPosition)
                }
                if (selection.contains(adapterPosition))
                    view.setBackgroundColor(selectedColor)
                else view.background = null
            }

            private fun updateLinkedTo(newLinkedItem: InventoryItem?) {
                if (newLinkedItem == null || newLinkedItem.id == 0L) return
                viewModel.updateLinkedInventoryItemId(item.id, newLinkedItem)
            }
        }
    }

    /** Computes a diff between two shopping list items.
     *
     *  ShoppingListRecyclerView.DiffUtilCallback uses the ids of shopping list
     *  items to determine if they are the same or not. If they are the same,
     *  changes are logged by setting the appropriate bit of an instance of
     *  EnumSet<ShoppingListRecyclerView.Field>. The change payload for modi-
     *  fied items will then be the enum set containing all of the Fields that
     *  were changed. */
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

            if (!itemChanges.isEmpty())
                listChanges[newItem.id] = EnumSet.copyOf(itemChanges)
            return itemChanges.isEmpty()
        }

        override fun getChangePayload(oldItem: ShoppingListItem,
                                      newItem: ShoppingListItem) =
            listChanges.remove(newItem.id)
    }

    /** A wrapper around a HashSet<Int> to keep track of checked items in the shopping list.
     *
     *  ShoppingListCheckedItems functions similarly to a RecyclerViewSelection
     *  in that it keeps track of a set of items, in this case ones that are
     *  checked. Items that are inserted already checked and items that are
     *  removed while checked should be automatically added or removed by the
     *  RecyclerView.AdapterDataObserver overrides. Changes to the checked
     *  status of an already existing item must be recorded via use of the add
     *  or remove functions.
     *
     *  A LiveData<Int> member is provided in sizeLiveData to allow external
     *  entities to respond to changes in the number of checked shopping list
     *  items. */
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
        }

        fun remove(pos: Int) {
            if (pos !in 0 until adapter.itemCount) return
            hashSet.remove(pos)
            _sizeLiveData.value = size
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {

            for (pos in positionStart until positionStart + itemCount)
                if (hashSet.contains(pos)) remove(pos)
            _sizeLiveData.value = size
        }
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            for (pos in positionStart until positionStart + itemCount)
                if (listDiffer.currentList[pos].isChecked) hashSet.add(pos)
            _sizeLiveData.value = size
        }
    }
}