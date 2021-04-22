/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.utils.dpToPixels

/** A RecyclerView.ItemDecoration to add spacing between items. */
class ItemSpacingDecoration(context: Context) :
    RecyclerView.ItemDecoration() {
    private val spacing = context.resources.dpToPixels(8f).toInt()

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