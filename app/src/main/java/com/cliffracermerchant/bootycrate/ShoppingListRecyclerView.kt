/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*
import java.util.*

/** A RecyclerView to display the data provided by a ShoppingListViewModel.
 *
 *  ShoppingListRecyclerView is a ExpandableSelectableRecyclerView subclass
 *  specialized for displaying the contents of a shopping list. Several of
 *  ShoppingListRecyclerView's necessary fields can not be obtained when it
 *  is inflated from XML, such as its viewmodels. To finish initialization with
 *  these required members, the function finishInit MUST be called during run-
 *  time, but before any sort of data access is attempted. ShoppingListRecycler-
 *  View's version of finishInit will call ExpandableSelectableRecyclerView's
 *  version to prevent the implementing activity or fragment from needing to
 *  call both.
 *
 *  Adding or removing shopping list items is accomplished using the ViewModel-
 *  RecyclerView and ExpandableSelectableRecyclerView functions and the new
 *  function addNewItem. ShoppingListRecyclerView also provides a function to
 *  allow the user to "checkout." For more information about the functionality
 *  of checkout, see the ShoppingListItemDao documentation.*/
class ShoppingListRecyclerView(context: Context, attrs: AttributeSet) :
        ExpandableSelectableRecyclerView<ShoppingListItem>(context, attrs) {
    override val diffUtilCallback = ShoppingListDiffUtilCallback()
    override val adapter = ShoppingListAdapter()
    override val collectionNameResId = R.string.shopping_list_item_collection_name
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    protected lateinit var fragmentManager: FragmentManager
    val checkedItems = ShoppingListCheckedItems()

    fun finishInit(
        owner: LifecycleOwner,
        shoppingListViewModel: ShoppingListViewModel,
        inventoryViewModel: InventoryViewModel,
        fragmentManager: FragmentManager,
        initialSort: ViewModelItem.Sort? = null
    ) {
        this.shoppingListViewModel = shoppingListViewModel
        this.inventoryViewModel = inventoryViewModel
        this.fragmentManager = fragmentManager
        finishInit(owner, shoppingListViewModel, initialSort)
    }

    fun addNewItem() = newShoppingListItemDialog(context, fragmentManager) { newItem ->
        shoppingListViewModel.add(newItem)
    }

    fun checkout() {
        checkedItems.removeAllIds()
        shoppingListViewModel.checkout()
    }

    override fun deleteItems(ids: LongArray) {
        checkedItems.removeAllIds(ids)
        super.deleteItems(ids)
    }

    /** A RecyclerView.Adapter to display the contents of a list of shopping list items.
     *
     *  ShoppingListAdapter is a subclass of BootyCrateAdapter using Shopping-
     *  ListItemViewHolder instances to represent shopping list items. Its
     *  overrides of onBindViewHolder make use of the ShoppingListItem.Field
     *  values passed by ShoppingListItemDiffUtilCallback to support partial
     *  binding. */
    inner class ShoppingListAdapter : ExpandableSelectableItemAdapter<ShoppingListItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ShoppingListItemViewHolder {
            val view = ShoppingListItemView(context)
            return ShoppingListItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ShoppingListItemViewHolder, position: Int) {
            holder.view.update(holder.item)
            super.onBindViewHolder(holder, position)
        }

        override fun onBindViewHolder(
            holder: ShoppingListItemViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.size == 0)
                return onBindViewHolder(holder, position)
            val unhandledChanges = mutableListOf<Any>()

            for (payload in payloads) {
                if (payload is EnumSet<*>) {
                    val item = getItem(position)
                    val changes = payload as EnumSet<ShoppingListItem.Field>

                    if (changes.contains(ShoppingListItem.Field.Name) &&
                        holder.view.nameEdit.text.toString() != item.name)
                            holder.view.nameEdit.setText(item.name)
                    if (changes.contains(ShoppingListItem.Field.ExtraInfo) &&
                        holder.view.extraInfoEdit.text.toString() != item.extraInfo)
                            holder.view.extraInfoEdit.setText(item.extraInfo)
                    if (changes.contains(ShoppingListItem.Field.Color) &&
                        holder.view.checkBoxColor != item.color)
                            holder.view.checkBoxColor = item.color
                    if (changes.contains(ShoppingListItem.Field.Amount) &&
                        holder.view.shoppingListAmountEdit.currentValue != item.amount)
                            holder.view.shoppingListAmountEdit.currentValue = item.amount
                    if (changes.contains(ShoppingListItem.Field.IsExpanded))
                        holder.updateForExpansionState(item.isExpanded)
                    if (changes.contains(ShoppingListItem.Field.IsSelected))
                        holder.view.setSelectedState(item.isSelected)
                    if (changes.contains(ShoppingListItem.Field.IsChecked)) {
                        holder.view.checkBox.isChecked = item.isChecked
                        if (item.isChecked) checkedItems.add(position)
                        else                checkedItems.remove(position)
                    }
                }
                else unhandledChanges.add(payload)
            }
            if (unhandledChanges.isNotEmpty())
                super.onBindViewHolder(holder, position, unhandledChanges)
        }
    }

    /** A BootyCrateViewHolder that wraps an instance of ShoppingListItemView.
     *
     *  ShoppingListItemViewHolder is a subclass of BootyCrateViewHolder that
     *  holds an instance of ShoppingListItemView to display the data for a
     *  ShoppingListItem. Besides its use of this custom item view, its diff-
     *  erences from BootyCrateViewHolder are:
     * - It sets the on click listeners of each of the sub views in the Shop-
     *   pingListItemView to permit the user to select/deselect items, and to
     *   edit the displayed data when allowed.
     * - Its override of the expand details button's onClickListener calls the
     *   RecyclerViewExpandedItem.set function on itself. Its override of
     *   ExpandableViewHolder.onExpansionStateChanged calls the corresponding
     *   expand or collapse functions on its ShoppingListItemView instance. */
    inner class ShoppingListItemViewHolder(val view: ShoppingListItemView) :
            ExpandableSelectableItemViewHolder(view) {

        private val checkBoxExpandedOnClick = OnClickListener {
            colorPickerDialog(fragmentManager, item.color) { pickedColor ->
                shoppingListViewModel.updateColor(item.id, pickedColor)
            }
        }
        private val checkBoxCollapsedOnClick = OnClickListener {
            shoppingListViewModel.updateIsChecked(item.id, view.checkBox.isChecked)
        }
        private val checkBoxExpandedOnCheckedChange = CompoundButton.OnCheckedChangeListener { checkBox, isChecked ->
            checkBox.isChecked = !isChecked
        }
        private val checkBoxCollapsedOnCheckedChange = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            view.setVisualCheckedState(isChecked)
        }

        init {
            // Click & long click listeners
            val onClick = OnClickListener { if (!selection.isEmpty) selection.toggle(itemId) }
            view.setOnClickListener(onClick)
            view.nameEdit.setOnClickListener(onClick)
            view.extraInfoEdit.setOnClickListener(onClick)
            view.shoppingListAmountEdit.valueEdit.setOnClickListener(onClick)

            val onLongClick = OnLongClickListener { selection.toggle(itemId); true }
            view.setOnLongClickListener(onLongClick)
            view.nameEdit.setOnLongClickListener(onLongClick)
            view.extraInfoEdit.setOnLongClickListener(onLongClick)
            view.shoppingListAmountEdit.valueEdit.setOnLongClickListener(onLongClick)

            view.checkBox.setOnClickListener {
                if (!view.isExpanded)
                    shoppingListViewModel.updateIsChecked(item.id, view.checkBox.isChecked)
            }
            view.editButton.setOnClickListener {
                if (!view.isExpanded) setExpandedItem(adapterPosition)
            }
            view.collapseButton.setOnClickListener { setExpandedItem(null) }

            // Data change listeners
            view.nameEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                shoppingListViewModel.updateName(item.id, value)
                val linkedId = item.linkedItemId
                if (linkedId != null)
                    inventoryViewModel.updateName(linkedId, value)
            }
            view.extraInfoEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                shoppingListViewModel.updateExtraInfo(item.id, value)
                val linkedId = item.linkedItemId
                if (linkedId != null)
                    inventoryViewModel.updateExtraInfo(linkedId, value)
            }
            view.shoppingListAmountEdit.liveData.observeForever { value ->
                if (adapterPosition == -1) return@observeForever
                shoppingListViewModel.updateAmount(item.id, value)
            }
        }

        fun updateForExpansionState(isExpanded: Boolean) {
            if (isExpanded) {
                view.checkBox.setOnClickListener(checkBoxExpandedOnClick)
                view.checkBox.setOnCheckedChangeListener(checkBoxExpandedOnCheckedChange)
            } else { //!isExpanded
                view.checkBox.setOnClickListener(checkBoxCollapsedOnClick)
                view.checkBox.setOnCheckedChangeListener(checkBoxCollapsedOnCheckedChange)
            }
            view.setExpanded(isExpanded)
        }
    }

    /** A utility to keep track of checked items in the shopping list.
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
    inner class ShoppingListCheckedItems : AdapterDataObserver() {
        private val hashSet = HashSet<Long>()
        private val _sizeLiveData = MutableLiveData(hashSet.size)

        val size: Int get() = hashSet.size
        val sizeLiveData: LiveData<Int> = _sizeLiveData
        val isEmpty get() = hashSet.isEmpty()

        init { adapter.registerAdapterDataObserver(this) }

        fun add(pos: Int) {
            if (pos !in 0 until adapter.itemCount) return
            hashSet.add(adapter.getItemId(pos))
            _sizeLiveData.value = size
        }

        fun remove(pos: Int) {
            if (pos !in 0 until adapter.itemCount) return
            hashSet.remove(adapter.getItemId(pos))
            _sizeLiveData.value = size
        }

        fun removeAllIds(ids: LongArray? = null) {
            if (ids != null)
                for (id in ids)
                    hashSet.remove(id)
            else hashSet.clear()
            _sizeLiveData.value = size
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            for (pos in positionStart until positionStart + itemCount) {
                val item = adapter.currentList[pos]
                if (item.isChecked) hashSet.add(item.id)
            }
            _sizeLiveData.value = size
        }

    }

    /** Computes a diff between two shopping list items.
     *
     *  ShoppingListRecyclerView.DiffUtilCallback uses the ids of shopping list
     *  items to determine if they are the same or not. If they are the same,
     *  changes are logged by setting the appropriate bit of an instance of
     *  EnumSet<ShoppingListItem.Field>. The change payload for modified items
     *  will then be the enum set containing all of the Fields that were
     *  changed. */
    class ShoppingListDiffUtilCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        private val listChanges = mutableMapOf<Long, EnumSet<ShoppingListItem.Field>>()
        private val itemChanges = EnumSet.noneOf(ShoppingListItem.Field::class.java)

        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ShoppingListItem,
                                        newItem: ShoppingListItem): Boolean {
            itemChanges.clear()
            if (newItem.name != oldItem.name)             itemChanges.add(ShoppingListItem.Field.Name)
            if (newItem.extraInfo != oldItem.extraInfo)   itemChanges.add(ShoppingListItem.Field.ExtraInfo)
            if (newItem.color != oldItem.color)           itemChanges.add(ShoppingListItem.Field.Color)
            if (newItem.amount != oldItem.amount)         itemChanges.add(ShoppingListItem.Field.Amount)
            if (newItem.isExpanded != oldItem.isExpanded) itemChanges.add(ShoppingListItem.Field.IsExpanded)
            if (newItem.isSelected != oldItem.isSelected) itemChanges.add(ShoppingListItem.Field.IsSelected)
            if (newItem.isChecked != oldItem.isChecked)   itemChanges.add(ShoppingListItem.Field.IsChecked)

            if (!itemChanges.isEmpty())
                listChanges[newItem.id] = EnumSet.copyOf(itemChanges)
            return itemChanges.isEmpty()
        }

        override fun getChangePayload(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            listChanges.remove(newItem.id)
    }
}