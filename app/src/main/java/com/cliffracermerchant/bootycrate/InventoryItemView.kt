package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*


/**     InventoryItemView is a ConstraintLayout that displays the data of an
 *  InventoryItem instance. Its update(InventoryItem) function updates the con-
 *  tained views with the information of the InventoryItem instance. It uses
 *  lazy initialization of the extra fields displayed when expanded, and only
 *  updates them when the details are expanded. Its expand and collapse func-
 *  tions also allow for an optional animation. */
class InventoryItemView(context: Context) : ConstraintLayout(context) {
    val expanded get() = expandedPrivate
    private var expandedPrivate = false
    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

    /* This companion object stores the heights of the layout when expanded/
       collapsed. This prevents the expand/collapse animations from having to
       calculate these values every time the animation is started. */
    private companion object {
        var collapsedHeight = 0
        var expandedHeight = 0
    }

    init {
        inflate(context, R.layout.inventory_item_layout, this)
        // It is recommended to override the expandDetailsButton's onClickList-
        // ener with one that passes in an instance of the contained Inventory-
        // Item, as it is otherwise unable to fill out the new fields exposed
        // when expanded.
        expandDetailsButton.setOnClickListener { expand() }
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

    fun expand(item: InventoryItem? = null, animate: Boolean = true) {
        expandedPrivate = true
        if (item != null) {
            autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
            autoAddToShoppingListTriggerEdit.currentValue = item.autoAddToShoppingListTrigger
        }
        nameEdit.isEditable = true
        amountEdit.isEditable = true
        extraInfoEdit.isEditable = true
        autoAddToShoppingListTriggerEdit.isEditable = true

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
        nameEdit.isEditable = false
        amountEdit.isEditable = false
        extraInfoEdit.isEditable = false
        autoAddToShoppingListTriggerEdit.isEditable = false

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
            nameEdit.isFocused ->             nameEdit.clearFocus()
            amountEdit.valueEdit.isFocused -> valueEdit.clearFocus()
            extraInfoEdit.isFocused ->        extraInfoEdit.clearFocus()
        }
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