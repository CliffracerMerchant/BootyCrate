/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView


/** A zebra stripes style RecyclerView.ItemDecoration.
 *
 *  AlternatingRowBackgroundDecoration is a subclass of RecyclerView.ItemDecoration
 *  that draws the 'colorBackgroundVariant' color of the current theme as a color
 *  background for every odd numbered row in its attached recycler view. */
class ItemSpacingDecoration(context: Context) :
    RecyclerView.ItemDecoration() {
    private val spacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f,
                                                    context.resources.displayMetrics).toInt()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = spacing / 2
        outRect.bottom = spacing / 2
        outRect.left = spacing
        outRect.right = spacing
    }
}