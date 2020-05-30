package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*

class ShoppingListRecyclerView(context: Context, attrs: AttributeSet) :
        RecyclerView(context, attrs),
        Observer<List<ShoppingListItem>> {

    private val adapter = ShoppingListAdapter(context)
    private val listDiffer = AsyncListDiffer(adapter, DiffUtilCallback())
    private lateinit var viewModel: ShoppingListViewModel
    private var bottomBar: BottomAppBar? = null
    val selection = RecyclerViewSelection(adapter)


    /** The enum class Field identifies user facing fields
     *  that are potentially editable by the user. */
    enum class Field { Name, ExtraInfo, AmountInCart, Amount  }

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

    fun setViewModel(owner: LifecycleOwner, viewModel: ShoppingListViewModel) {
        this.viewModel = viewModel
        viewModel.getAll().observe(owner, this)
    }

    fun addNewItem() = viewModel.insert(ShoppingListItem(
        context.getString(R.string.shopping_list_item_default_name)))

    fun deleteItem(pos: Int) = deleteItems(pos)

    fun deleteItems(vararg positions: Int) {
        val ids = LongArray(positions.size) { listDiffer.currentList[positions[it]].id }
        viewModel.delete(*ids)
        val text = context.getString(R.string.delete_snackbar_text, ids.size)
        val snackBar = Snackbar.make(bottomBar ?: this, text, Snackbar.LENGTH_LONG)
        snackBar.anchorView = bottomBar ?: this
        snackBar.setAction(R.string.delete_snackbar_undo_text) { undoDelete() }
        snackBar.show()
    }

    fun undoDelete() = viewModel.undoDelete()

    override fun onChanged(items: List<ShoppingListItem>) = listDiffer.submitList(items)

    inner class ShoppingListAdapter(context: Context) :
            RecyclerView.Adapter<ShoppingListAdapter.ShoppingListItemViewHolder>() {
        private val layoutInflater = LayoutInflater.from(context)
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
            return ShoppingListItemViewHolder(layoutInflater.inflate(
                    R.layout.shopping_list_item_layout, parent, false))
        }

        override fun onBindViewHolder(holder: ShoppingListItemViewHolder, position: Int) =
            holder.bindTo(listDiffer.currentList[position])

        override fun onBindViewHolder(holder: ShoppingListItemViewHolder,
                                      position: Int,
                                      payloads: MutableList<Any>) {
            if (payloads.size == 0) return onBindViewHolder(holder, position)
            for (payload in payloads) {
                if (payload is Boolean) {
                    val startColor = if (payload) 0 else selectedColor
                    val endColor = if (payload) selectedColor else 0
                    /*val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
                    valueAnimator.duration = 300
                    valueAnimator.addUpdateListener { anim ->
                        holder.itemView.setBackgroundColor(ColorUtils.blendARGB(
                            startColor, endColor, anim.animatedValue as Float))
                    }
                    valueAnimator.start()*/
                    ObjectAnimator.ofArgb(holder.itemView, "backgroundColor",
                                          startColor, endColor).start()
                } else if (payload is ItemChange) {
                    when (payload.editable) {
                        Field.Name -> {
                            assert(payload.value is String)
                            holder.itemView.nameEdit.setText(payload.value as String)
                        }
                        Field.ExtraInfo -> {
                            assert(payload.value is String)
                            holder.itemView.extraInfoEdit.setText(payload.value as String)
                        }
                        Field.AmountInCart -> {
                            assert(payload.value is Int)
                            holder.itemView.amountInCartEdit.currentValue = payload.value as Int
                        }
                        Field.Amount -> {
                            assert(payload.value is Int)
                            holder.itemView.amountEdit.currentValue = payload.value as Int
                        }
                    }
                }
            }
        }

        override fun getItemId(position: Int): Long = listDiffer.currentList[position].id

        inner class ShoppingListItemViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

            init {
                // Click & long click listeners
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

                view.nameEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateName(listDiffer.currentList[adapterPosition], value)
                }
                view.extraInfoEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateExtraInfo(listDiffer.currentList[adapterPosition], value)
                }
                view.amountInCartEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmountInCart(listDiffer.currentList[adapterPosition], value)
                }
                view.amountEdit.liveData.observeForever { value ->
                    if (adapterPosition == -1) return@observeForever
                    viewModel.updateAmount(listDiffer.currentList[adapterPosition], value)
                }
            }

            fun bindTo(item: ShoppingListItem) {
                itemView.nameEdit.setText(item.name)
                itemView.extraInfoEdit.setText(item.extraInfo)
                itemView.amountInCartEdit.currentValue = item.amountInCart
                itemView.amountEdit.currentValue = item.amount
                if (selection.contains(adapterPosition)) itemView.setBackgroundColor(selectedColor)
                else                                     itemView.background = null
            }
        }
    }

    internal inner class DiffUtilCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem) =
            oldItem == newItem

        override fun getChangePayload(oldItem: ShoppingListItem, newItem: ShoppingListItem): Any? {
            if (oldItem.name != newItem.name)
                return ItemChange(Field.Name, newItem.name)
            if (oldItem.extraInfo != newItem.extraInfo)
                return ItemChange(Field.ExtraInfo, newItem.extraInfo)
            if (oldItem.amountInCart != newItem.amountInCart)
                return ItemChange(Field.AmountInCart, newItem.amountInCart)
            if (oldItem.amount != newItem.amount)
                return ItemChange(Field.Amount, newItem.amount)
            return super.getChangePayload(oldItem, newItem)
        }
    }
}