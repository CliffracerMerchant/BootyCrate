/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R

/**
 * A RecyclerView.ItemTouchHelper.SimpleCallback to delete swiped items.
 *
 * SwipeToDeleteCallback is a RecyclerView.ItemTouchHelper.SimpleCallback sub-
 * class that implements swipe (left or right) to delete functionality. The
 * function callback @param deleteFunc is invoked when an item is swiped left
 * or right. The Int parameter will be the RecyclerView.Adapter position of the
 * swiped item
 */
class SwipeToDeleteCallback(context: Context, private val deleteFunc: (Int) -> Unit) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteBg = ColorDrawable(ContextCompat.getColor(context, android.R.color.holo_red_light))
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp)
    private val iconSize = deleteIcon?.intrinsicHeight ?: 0
    private val iconMargin = iconSize / 2

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder) = false

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        deleteBg.bounds.left = if (dX > 0) viewHolder.itemView.left
                               else        viewHolder.itemView.right + dX.toInt()
        deleteBg.bounds.top = viewHolder.itemView.top
        deleteBg.bounds.right = if (dX > 0) viewHolder.itemView.left + dX.toInt()
                                else        viewHolder.itemView.right
        deleteBg.bounds.bottom = viewHolder.itemView.bottom
        deleteBg.draw(c)

        if (deleteIcon == null) return
        deleteIcon.bounds.left = if (dX > 0) viewHolder.itemView.left + iconMargin
                                 else        viewHolder.itemView.right - iconSize - iconMargin
        deleteIcon.bounds.top = viewHolder.itemView.top + (viewHolder.itemView.height - iconSize) / 2
        deleteIcon.bounds.right = if (dX > 0) viewHolder.itemView.left + iconSize + iconMargin
                                  else        viewHolder.itemView.right - iconMargin
        deleteIcon.bounds.bottom = deleteIcon.bounds.top + iconSize
        deleteIcon.draw(c)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) =
        deleteFunc(viewHolder.adapterPosition)
}