package com.cliffracermerchant.stuffcrate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView


/**     AlternatingRowBackgroundDecoration is a simple subclass of
 *  RecyclerView.ItemDecoration that draws the 'colorBackgroundVariant' color
 *  of the current theme as a color background for every odd numbered row in
 *  its attached recycler view. The context of the attached recycler view
 *  should be passed in the constructor to ensure that the proper color is
 *  used. If the attached recycler view's theme changes, the alterate row back-
 *  ground color can be updated using the function update(context: Context). */
class AlternatingRowBackgroundDecoration(context: Context) :
    RecyclerView.ItemDecoration() {
    private val backgroundFill = Paint()

    init {
        backgroundFill.style = Paint.Style.FILL
        update(context)
    }

    fun update(context: Context) {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(R.attr.colorBackgroundVariant, typedValue, true))
            backgroundFill.color = typedValue.data
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.childCount < 2) return
        // startPos will be equal to the on screen position
        // of the first visible odd numbered item
        val firstChildAdapterPos = parent.getChildAdapterPosition(parent.getChildAt(0))
        val startPos = if (firstChildAdapterPos % 2 == 0) 1
        else 0
        for (i in startPos until parent.childCount step 2) {
            val child = parent.getChildAt(i)
            c.drawRect(child.left.toFloat(), child.top.toFloat(),
                       child.right.toFloat(), child.bottom.toFloat(),
                       backgroundFill
            )
        }
    }
}