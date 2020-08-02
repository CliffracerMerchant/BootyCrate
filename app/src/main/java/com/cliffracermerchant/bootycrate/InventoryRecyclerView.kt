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
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
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
import java.util.*

/** A RecyclerView to display the data provided by an InventoryViewModel.
 *
 *  InventoryRecyclerView is a RecyclerView subclass specialized for displaying
 *  the contents of an inventory. Several of InventoryRecyclerView's necessary
 *  fields can not be obtained when it is inflated from XML, such as its view-
 *  models. To finish initialization with these required members, the function
 *  finishInit MUST be called after runtime but before any sort of data access
 *  is attempted. An initial sort can be passed during this finishInit call,
 *  and can thereafter be modified (along with a search filter) using the pub-
 *  lic properties sort and searchFilter.
 *
 *  In order to allow it to calculate changes to the displayed data on a back-
 *  ground thread, InventoryRecyclerView contains an AsyncListDiffer member.
 *  Its custom DiffUtilCallback dispatches an EnumSet<InventoryRecyclerView.
 *  Field> to indicate which fields of the inventory item need to be updated.
 *
 *  Adding or removing inventory items is accomplished using the functions add-
 *  Item, deleteItem, and deleteItems. An ItemTouchHelper with a SwipeToDelete-
 *  Callback is used to allow the user to call deleteItem on items that are
 *  swiped left or right. When items are deleted, a snackbar will appear infor-
 *  ming the user of the amount of items that were deleted, as well as provi-
 *  ding an undo option.
 *
 *  The snackbar that appears after deleting items will be anchored to the view
 *  set as the public property snackBarAnchor, in case this needs to be custom-
 *  ized, or to the InventoryRecyclerView otherwise. */
class InventoryRecyclerView(context: Context, attrs: AttributeSet) :
        RecyclerView(context, attrs) {

    private val adapter = InventoryAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var viewModel: InventoryViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    /* To simplify the user experience, only one inventory item view is allowed to
     * be expanded at once. Keeping track of both the expanded item ID as well as
     * its ViewHolder allows InventoryRecyclerView to use expandedViewHolder when
     * its contained item id == expandedItemId (meaning that it hasn't been
     * rebound to a new item), thereby preventing a potentially costly
     * findViewHolderForItemId call. */
    private var expandedItemId: Long? = null
    private var expandedViewHolder: InventoryAdapter.InventoryItemViewHolder? = null
    val selection = RecyclerViewSelection(adapter)

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

    fun finishInit(owner: LifecycleOwner,
                   inventoryViewModel: InventoryViewModel,
                   shoppingListViewModel: ShoppingListViewModel,
                   fragmentManager: FragmentManager,
                   initialSort: Sort? = null) {
        viewModel = inventoryViewModel
        // Resetting the newly inserted item id here prevents the recycler view
        // from always expanding the item with that id until a new one is inserted
        viewModel.resetNewlyInsertedItemId()
        if (initialSort != null) sort = initialSort
        viewModel.items.observe(owner, Observer { items -> listDiffer.submitList(items) })
        this.shoppingListViewModel = shoppingListViewModel
        this.fragmentManager = fragmentManager
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

    /** A RecyclerView.Adapter to display the contents of a list of inventory items.
     *
     *  InventoryAdapter is a subclass of RecyclerView.Adapter using its own
     *  RecyclerView.ViewHolder subclass InventoryItemViewHolder to represent
     *  inventory items. Its override of onBindViewHolder(ViewHolder, Payload)
     *  makes use of InventoryRecyclerView.Field values to support partial
     *  binding. It also modifies the background color of the item views to
     *  reflect their selected / not selected status. */
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
                } else if (payload is EnumSet<*>) {
                    val item = listDiffer.currentList[position]
                    val changes = payload as EnumSet<Field>
                    if (changes.contains(Field.Name))
                        holder.itemView.nameEdit.setText(item.name)
                    if (changes.contains(Field.Amount)) {
                        holder.itemView.amountEdit.currentValue = item.amount
                        checkAutoAddToShoppingList(holder.adapterPosition) }
                    if (changes.contains(Field.ExtraInfo))
                        holder.itemView.extraInfoEdit.setText(item.extraInfo)
                    if (changes.contains(Field.AutoAddToShoppingList)) {
                        holder.itemView.autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
                        checkAutoAddToShoppingList(holder.adapterPosition) }
                    if (changes.contains(Field.AutoAddToShoppingListTrigger)) {
                        holder.itemView.autoAddToShoppingListTriggerEdit.currentValue = item.autoAddToShoppingListTrigger
                        checkAutoAddToShoppingList(holder.adapterPosition) }
                    if (changes.contains(Field.Color)) {
                        val colorEditBg = holder.itemView.colorEdit.background as ColoredCircleDrawable
                        val startColor = colorEditBg.color
                        val endColor = item.color
                        ObjectAnimator.ofArgb(colorEditBg, "color", startColor, endColor).start() }
                }
            }
        }

        override fun getItemId(position: Int): Long = listDiffer.currentList[position].id

        /** A RecyclerView.ViewHolder that wraps an instance of InventoryItemView.
         *
         *  InventoryItemViewHolder is a subclass of RecyclerView.ViewHolder
         *  that holds an instance of InventoryItemView to display the data for
         *  an InventoryItem. Besides its use of this custom item view, its
         *  differences from RecyclerView.ViewHolder are:
         * - It sets the on click listeners of each of the sub views in the
         *   InventoryItemView to permit the user to select/deselect items, and
         *   to edit the displayed data when allowed.
         * - Its override of the expand details button onClickListener calls
         *   InventoryRecyclerView.setExpandedItem on itself to enforce the one
         *   expanded item at a time rule.
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
                    colorPicker.show(fragmentManager)
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
                    imm?.hideSoftInputFromWindow(view.nameEdit.windowToken, 0)
                    view.nameEdit.requestFocus()
                    imm?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
                }
                if (selection.contains(adapterPosition)) view.setBackgroundColor(selectedColor)
                else                                     view.background = null
            }
        }
    }

    /** Computes a diff between two inventory items.
     *
     *  InventoryRecyclerView.DiffUtilCallback uses the ids of inventory items
     *  to determine if they are the same or not. If they are the same, changes
     *  are logged by setting the appropriate bit of an instance of EnumSet<
     *  InventoryRecyclerView.Field>. The change payload for modified items
     *  will then be the enum set containing all of the Fields that were
     *  changed. */
    internal inner class DiffUtilCallback : DiffUtil.ItemCallback<InventoryItem>() {
        private val listChanges = mutableMapOf<Long, EnumSet<Field>>()
        private val itemChanges = EnumSet.noneOf(Field::class.java)

        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryItem,
                                        newItem: InventoryItem): Boolean {
            itemChanges.clear()
            if (newItem.name != oldItem.name)           itemChanges.add(Field.Name)
            if (newItem.extraInfo != oldItem.extraInfo) itemChanges.add(Field.ExtraInfo)
            if (newItem.color != oldItem.color)         itemChanges.add(Field.Color)
            if (newItem.amount != oldItem.amount)       itemChanges.add(Field.Amount)
            if (newItem.autoAddToShoppingList != oldItem.autoAddToShoppingList) itemChanges.add(Field.AutoAddToShoppingList)
            if (newItem.autoAddToShoppingListTrigger != oldItem.autoAddToShoppingListTrigger) itemChanges.add(Field.AutoAddToShoppingListTrigger)

            if (!itemChanges.isEmpty()) listChanges[newItem.id] = EnumSet.copyOf(itemChanges)
            return itemChanges.isEmpty()
        }

        override fun getChangePayload(oldItem: InventoryItem,
                                      newItem: InventoryItem) =
            listChanges.remove(newItem.id)
    }
}