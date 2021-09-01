/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.intValueAnimator

/**
 * A RecyclerView.ItemTouchHelper.SimpleCallback to delete swiped items.
 *
 * SwipeToDeleteCallback is a RecyclerView.ItemTouchHelper.SimpleCallback sub-
 * class that implements swipe (left or right) to delete functionality. The
 * function callback @param deleteFunc is invoked when an item is swiped left
 * or right. The Int parameter will be the RecyclerView.Adapter position of the
 * swiped item.
 *
 * The recycler view that the SwipeToDeleteCallback will be attached to is
 * required during construction so that it can create a helper ItemDecoration
 * that will fade the delete background out after an item is swiped. A float
 * value can optionally be passed in that will be used as the corner radius
 * of the red delete background used.
 */
class SwipeToDeleteCallback(
    context: Context,
    recyclerView: RecyclerView,
    private val cornerRadius: Float = 0f,
    private val deleteFunc: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp)!!
    private val iconSize = deleteIcon.intrinsicHeight
    private val iconMargin = iconSize / 2
    private val deleteBgBounds = RectF()
    private val deleteBgPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.holo_red_light)
    }

    init { recyclerView.addItemDecoration(DeleteBackgroundFadeOutDecoration()) }

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
        // This function seems not to be called after the item is actually deleted,
        // even before its fade out delete animation is completed. To fade the delete
        // background out we need to pass the drawing work over to the companion item
        // decoration after the item is deleted. The fade out animation also seems not
        // to work when the last item in the recycler view is swiped for some reason.
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (dX == 0f || deleteBgPaint.alpha < 255 ||
            viewHolder.adapterPosition == -1) return

        deleteBgBounds.left = if (dX > 0) viewHolder.itemView.left.toFloat()
                              else        viewHolder.itemView.right + dX - 2 * cornerRadius
        deleteBgBounds.top = viewHolder.itemView.top.toFloat()
        deleteBgBounds.right = if (dX > 0) viewHolder.itemView.left + dX + 2 * cornerRadius
                               else        viewHolder.itemView.right.toFloat()
        deleteBgBounds.bottom = viewHolder.itemView.bottom.toFloat()
        c.drawRoundRect(deleteBgBounds, cornerRadius, cornerRadius, deleteBgPaint)

        deleteIcon.bounds.left = if (dX > 0) viewHolder.itemView.left + iconMargin
                                 else        viewHolder.itemView.right - iconSize - iconMargin
        deleteIcon.bounds.top = viewHolder.itemView.top + (viewHolder.itemView.height - iconSize) / 2
        deleteIcon.bounds.right = if (dX > 0) viewHolder.itemView.left + iconSize + iconMargin
                                  else        viewHolder.itemView.right - iconMargin
        deleteIcon.bounds.bottom = deleteIcon.bounds.top + iconSize
        deleteIcon.draw(c)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        deleteBgPaint.alpha = 255
        deleteIcon.alpha = 255
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        deleteFunc(viewHolder.adapterPosition)
        intValueAnimator(deleteBgPaint::setAlpha, 255, 0).start()
    }

    inner class DeleteBackgroundFadeOutDecoration() : RecyclerView.ItemDecoration() {
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            if (deleteBgPaint.alpha == 255) return
            c.drawRoundRect(deleteBgBounds, cornerRadius, cornerRadius, deleteBgPaint)
            deleteIcon.alpha = deleteBgPaint.alpha
            deleteIcon.draw(c)
        }
    }
}