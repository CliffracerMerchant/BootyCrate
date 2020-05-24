package com.cliffracermerchant.bootycrate

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * SwipeToDeleteItemTouchHelperCallback is a simple
 * RecyclerView.ItemTouchHelper.SimpleCallback subclass that implements swipe
 * (left or right) to delete functionality.
 *
 * @property deleteFunc The function callback invoked when an item is swiped
 *                      left or right. The Int parameter will be the Recycler-
 *                      View.Adapter position of the swiped item
 */
class SwipeToDeleteItemTouchHelperCallback(private val deleteFunc: (Int) -> Unit) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        deleteFunc(viewHolder.adapterPosition)
    }
}