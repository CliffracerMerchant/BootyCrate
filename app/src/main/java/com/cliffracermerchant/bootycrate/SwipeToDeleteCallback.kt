/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.lang.Integer.max

/** A RecyclerView.ItemTouchHelper.SimpleCallback to delete swiped items.
 *
 *  SwipeToDeleteCallback is a RecyclerView.ItemTouchHelper.SimpleCallback
 *  subclass that implements swipe (left or right) to delete functionality.
 *  @property deleteFunc The function callback invoked when an item is swiped
 *                       left or right. The Int parameter will be the Recycler-
 *                       View.Adapter position of the swiped item */
class SwipeToDeleteCallback(private val deleteFunc: (Int) -> Unit, context: Context) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteBg = ColorDrawable(ContextCompat.getColor(context, android.R.color.holo_red_light))
    private val deleteIcon = context.getDrawable(R.drawable.ic_delete_black_24dp)
    private val iconSize = deleteIcon?.intrinsicHeight ?: 0
    private val iconMargin = iconSize / 2

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder) = false

    /* onChildDrawOver has to be overridden instead of onChildDraw to make the
     * temporary swiping background color visible even when the RecyclerView
     * item backgrounds are opaque instead of transparent. */
    override fun onChildDrawOver(canvas: Canvas, recyclerView: RecyclerView,
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
        canvas.save()
        canvas.clipRect(deleteBg.bounds)
        deleteIcon.draw(canvas)
        canvas.restore()
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) =
        deleteFunc(viewHolder.adapterPosition)
}