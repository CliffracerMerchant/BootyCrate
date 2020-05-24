package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*

/**     InventoryItemLayout is a ConstraintLayout that displays the data of an
 *  InventoryItem instance. Its update(InventoryItem) function updates the con-
 *  tained views with the information of the InventoryItem instance. It uses
 *  lazy initialization of the extra fields displayed when expanded, and only
 *  updates them when the details are expanded. Its expand and collapse func-
 *  tions also allow for an optional animation. */
class InventoryItemLayout(context: Context) : ConstraintLayout(context) {
    val expanded get() = expandedPrivate
    private var expandedPrivate = false

    /* This companion object stores the heights of the layout when expanded/
       collapsed. This prevents the expand/collapse animations from having to
       calculate these values every time the animation is started. */
    private companion object {
        var collapsedHeight = 0
        var expandedHeight = 0
    }

    init {
        inflate(context, R.layout.inventory_item_layout, this)
        /* expandDetailsButton's onClickListener is not set here due to it
         * requiring an instance of InventoryItem to initialize the extra
         * fields displayed when expanded. */
        collapseDetailsButton.setOnClickListener { collapse() }
        // If the layout's children are clipped, they will suddenly appear or
        // disappear without an animation during the expand collapse animation
        clipChildren = false
    }

    fun update(item: InventoryItem, expanded: Boolean) {
        nameEdit.setText(item.name)
        amountEdit.currentValue = item.amount
        extraInfoEdit.setText(item.extraInfo)
        if (expanded) expand(item)
    }

    fun expand(item: InventoryItem, animate: Boolean = true) {
        expandedPrivate = true
        autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
        autoAddToShoppingListTriggerEdit.currentValue = item.autoAddToShoppingListTrigger

        //nameEdit.isEnabled = true
        //nameEdit.isFocusableInTouchMode = true
        nameEdit.isClickable = true

        amountEdit.editable = true

        //extraInfoEdit.isEnabled = true
        //extraInfoEdit.isFocusableInTouchMode = true
        extraInfoEdit.isClickable = true
        autoAddToShoppingListTriggerEdit.editable = true

        expandDetailsButton.visibility = View.INVISIBLE

        if (animate) {
            val anim = expandCollapseAnimation(true)
            anim.addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    collapseDetailsButton.visibility = View.VISIBLE
                }
            })
            anim.start()
        } else collapseDetailsButton.visibility = View.VISIBLE
    }

    fun collapse(animate: Boolean = true) {
        expandedPrivate = false
        //nameEdit.isFocusableInTouchMode = false
        nameEdit.isClickable = false
        //nameEdit.isEnabled = false
        amountEdit.editable = false
        //extraInfoEdit.isFocusableInTouchMode = false
        //extraInfoEdit.isEnabled = false
        extraInfoEdit.isClickable = false
        autoAddToShoppingListTriggerEdit.editable = false
        collapseDetailsButton.visibility = View.INVISIBLE

        if (animate) {
            val anim = expandCollapseAnimation(false)
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    expandDetailsButton.visibility = View.VISIBLE
                    inventoryItemDetailsInclude.visibility = View.GONE
                }
            })
            anim.start()
        } else {
            expandDetailsButton.visibility = View.VISIBLE
            inventoryItemDetailsInclude.visibility = View.GONE
        }
        when {
            nameEdit.isFocused -> nameEdit.clearFocus()
            amountEdit.valueEdit.isFocused -> valueEdit.clearFocus()
            extraInfoEdit.isFocused -> extraInfoEdit.clearFocus()
        }
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