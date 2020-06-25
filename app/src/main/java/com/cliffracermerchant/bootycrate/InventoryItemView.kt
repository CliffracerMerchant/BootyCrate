package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.extraInfoEdit


/**     InventoryItemView is a ConstraintLayout that displays the data of an
 *  InventoryItem instance. Its update(InventoryItem) function updates the con-
 *  tained views with the information of the InventoryItem instance. Its expand
 *  and collapse functions allow for an optional animation. */
class InventoryItemView(context: Context) : ConstraintLayout(context) {
    val expanded get() = expandedPrivate
    private var expandedPrivate = false
    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    private val editToMoreOptionsIcon = context.getDrawable(R.drawable.animated_edit_to_more_options_icon)
    private val moreOptionsToEditIcon = context.getDrawable(R.drawable.animated_more_options_to_edit_icon)

    /* This companion object stores the heights of the layout when expanded/
       collapsed. This prevents the expand/collapse animations from having to
       calculate these values every time the animation is started. */
    private companion object {
        var collapsedHeight = 0
        var expandedHeight = 0
    }

    init {
        inflate(context, R.layout.inventory_item_layout, this)
        editButton.setOnClickListener {
            if (expanded) //TODO Implement more options menu
            else          expand()
        }
        collapseButton.setOnClickListener{ collapse() }
        // If the layout's children are clipped, they will suddenly appear or
        // disappear without an animation during the expand collapse animation
        clipChildren = false
    }

    fun update(item: InventoryItem, expanded: Boolean) {
        nameEdit.setText(item.name)
        amountEdit.setCurrentValueWithoutDataUpdate(item.amount)
        extraInfoEdit.setText(item.extraInfo)
        autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
        autoAddToShoppingListTriggerEdit.setCurrentValueWithoutDataUpdate(item.autoAddToShoppingListTrigger)
        colorEdit.background = ColoredCircleDrawable(colorEdit.layoutParams.width.toFloat(),
                                                     item.color, nameEdit.currentTextColor)
        if (expanded) expand()
    }

    fun expand(animate: Boolean = true) {
        expandedPrivate = true

        nameEdit.isEditable = true
        amountEdit.isEditable = true
        extraInfoEdit.isEditable = true
        autoAddToShoppingListTriggerEdit.isEditable = true

        if (animate) {
            editButton.background = editToMoreOptionsIcon
            (editButton.background as AnimatedVectorDrawable).start()
            expandCollapseAnimation(true).start()
        } else editButton.background = moreOptionsToEditIcon
    }

    fun collapse(animate: Boolean = true) {
        expandedPrivate = false
        nameEdit.isEditable = false
        amountEdit.isEditable = false
        extraInfoEdit.isEditable = false
        autoAddToShoppingListTriggerEdit.isEditable = false

        if (animate) {
            editButton.background = moreOptionsToEditIcon
            (editButton.background as AnimatedVectorDrawable).start()
            val anim = expandCollapseAnimation(false)
            anim.doOnEnd { inventoryItemDetailsInclude.visibility = View.GONE }
            anim.start()
        } else {
            editButton.background = editToMoreOptionsIcon
            inventoryItemDetailsInclude.visibility = View.GONE
        }
        /*when {
            nameEdit.isFocused ->             nameEdit.clearFocus()
            amountEdit.valueEdit.isFocused -> valueEdit.clearFocus()
            extraInfoEdit.isFocused ->        extraInfoEdit.clearFocus()
        }*/
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun expandCollapseAnimation(expanding: Boolean) : ValueAnimator {
        if (collapsedHeight == 0) initExpandedCollapsedHeights(expanding)
        inventoryItemDetailsInclude.visibility = View.VISIBLE

        val startHeight = if (expanding) collapsedHeight else expandedHeight
        val endHeight =   if (expanding) expandedHeight  else collapsedHeight
        val change = endHeight - startHeight

        val anim = ValueAnimator.ofInt(startHeight, endHeight)
        anim.addUpdateListener {
            layoutParams.height = startHeight + (anim.animatedFraction * change).toInt()
            requestLayout()
        }
        anim.duration = 200
        anim.interpolator = FastOutSlowInInterpolator()
        return anim
    }

    private fun initExpandedCollapsedHeights(expanding: Boolean) {
        val matchParentSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(matchParentSpec, wrapContentSpec)

        if (expanding) {
            collapsedHeight = measuredHeight
            inventoryItemDetailsInclude.visibility = View.VISIBLE
            measure(matchParentSpec, wrapContentSpec)
            expandedHeight = measuredHeight
        } else {
            expandedHeight = measuredHeight
            inventoryItemDetailsInclude.visibility = View.GONE
            measure(matchParentSpec, wrapContentSpec)
            collapsedHeight = measuredHeight
        }
    }
}