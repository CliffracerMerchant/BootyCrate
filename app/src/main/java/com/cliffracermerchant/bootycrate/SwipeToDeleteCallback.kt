package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.lang.Integer.max

/** SwipeToDeleteCallback is a RecyclerView.ItemTouchHelper.SimpleCallback
 *  subclass that implements swipe (left or right) to delete functionality.
 *  @property deleteFunc The function callback invoked when an item is swiped
 *                       left or right. The Int parameter will be the Recycler-
 *                       View.Adapter position of the swiped item */
class SwipeToDeleteCallback(private val deleteFunc: (Int) -> Unit, context: Context) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteBg = ColorDrawable(ContextCompat.getColor(context, android.R.color.holo_red_light))
    private val deleteIcon = context.getDrawable(R.drawable.ic_delete_black_24dp)
    private val iconSize = (deleteIcon?.intrinsicHeight ?: 0 * 3 / 2)
    private val iconMargin = iconSize / 2

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder) = false

    override fun onChildDraw(canvas: Canvas, recyclerView: RecyclerView,
                             viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int,
                             isCurrentlyActive: Boolean) {
        deleteBg.bounds.left = if (dX > 0) viewHolder.itemView.left
                               else        viewHolder.itemView.right + dX.toInt()
        deleteBg.bounds.top = viewHolder.itemView.top
        deleteBg.bounds.right = if (dX > 0) viewHolder.itemView.left + dX.toInt()
                                else        viewHolder.itemView.right
        deleteBg.bounds.bottom = viewHolder.itemView.bottom
        deleteBg.draw(canvas)

        if (deleteIcon == null) return
        deleteIcon.bounds.left = if (dX > 0) viewHolder.itemView.left + iconMargin
                                 else        viewHolder.itemView.right - iconSize - iconMargin
        deleteIcon.bounds.top = viewHolder.itemView.top + (viewHolder.itemView.height - iconSize) / 2
        deleteIcon.bounds.right = if (dX > 0) viewHolder.itemView.left + iconSize + iconMargin
                                  else        viewHolder.itemView.right - iconMargin
        deleteIcon.bounds.bottom = deleteIcon.bounds.top + iconSize
        deleteIcon.draw(canvas)
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        deleteFunc(viewHolder.adapterPosition)
    }
}