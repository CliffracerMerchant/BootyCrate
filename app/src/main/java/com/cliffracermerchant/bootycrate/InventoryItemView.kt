package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.bottomappbar.BottomAppBar
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.extraInfoEdit


/**     InventoryItemView is a ConstraintLayout that displays the data of an
 *  InventoryItem instance. Its update(InventoryItem) function updates the con-
 *  tained views with the information of the InventoryItem instance. Its expand
 *  and collapse functions allow for an optional animation. */
class InventoryItemView(context: Context) : ConstraintLayout(context) {
    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    private val editButtonIconController: AnimatedVectorDrawableController

    /* This companion object stores the heights of the layout when expanded/
       collapsed. This prevents the expand/collapse animations from having to
       calculate these values every time the animation is started. */
    private companion object {
        var collapsedHeight = 0
        var expandedHeight = 0
    }

    init {
        inflate(context, R.layout.inventory_item_layout, this)
        editButtonIconController = AnimatedVectorDrawableController.forView(editButton,
            context.getDrawable(R.drawable.animated_edit_to_more_options_icon) as AnimatedVectorDrawable,
            context.getDrawable(R.drawable.animated_more_options_to_edit_icon) as AnimatedVectorDrawable)

        editButton.setOnClickListener {
            if (isExpanded) //TODO Implement more options menu
            else          expand()
        }
        collapseButton.setOnClickListener{ collapse() }

        // If the layout's children are clipped, they will suddenly appear or
        // disappear without an animation during the expand collapse animation
        clipChildren = false
    }

    fun update(item: InventoryItem, isExpanded: Boolean = false) {
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        colorEdit.background = ColoredCircleDrawable(colorEdit.layoutParams.width.toFloat(),
                                                     item.color, nameEdit.currentTextColor)
        amountEdit.initCurrentValue(item.amount)
        autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
        autoAddToShoppingListTriggerEdit.initCurrentValue(item.autoAddToShoppingListTrigger)
        if (isExpanded) expand(false)
        else            collapse(false)
    }

    fun expand(animate: Boolean = true) {
        _isExpanded = true
        nameEdit.isEditable = true
        amountEdit.isEditable = true
        extraInfoEdit.isEditable = true
        autoAddToShoppingListTriggerEdit.isEditable = true
        editButtonIconController.toStateB(animate)

        if (animate) expandCollapseAnimation(true).start()
        else inventoryItemDetailsInclude.visibility = View.VISIBLE
    }

    fun collapse(animate: Boolean = true) {
        if (nameEdit.isFocused || extraInfoEdit.isFocused ||
            amountEdit.valueEdit.isFocused ||
            autoAddToShoppingListTriggerEdit.valueEdit.isFocused)
                imm?.hideSoftInputFromWindow(windowToken, 0)

        _isExpanded = false
        nameEdit.isEditable = false
        amountEdit.isEditable = false
        extraInfoEdit.isEditable = false
        autoAddToShoppingListTriggerEdit.isEditable = false
        editButtonIconController.toStateA(animate)

        if (animate) {
            val anim = expandCollapseAnimation(false)
            anim.doOnEnd { inventoryItemDetailsInclude.visibility = View.GONE }
            anim.start()
        } else inventoryItemDetailsInclude.visibility = View.GONE
    }

    private fun expandCollapseAnimation(expanding: Boolean) : ValueAnimator {
        if (collapsedHeight == 0) initExpandedCollapsedHeights(expanding)
        inventoryItemDetailsInclude.visibility = View.VISIBLE

        val startHeight = if (expanding) collapsedHeight else expandedHeight
        val endHeight =   if (expanding) expandedHeight  else collapsedHeight

        val anim = ValueAnimator.ofInt(startHeight, endHeight)
        anim.addUpdateListener {
            layoutParams.height = anim.animatedValue as Int
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