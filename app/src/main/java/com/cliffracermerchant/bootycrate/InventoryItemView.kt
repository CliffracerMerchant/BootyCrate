/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.editButton
import kotlinx.android.synthetic.main.inventory_item_layout.view.extraInfoEdit
import kotlinx.android.synthetic.main.inventory_item_layout.view.nameEdit

/** A layout to display the contents of an inventory item.
 *
 *  InventoryItemView is a ConstraintLayout subclass that inflates a layout to
 *  display the data of an InventoryItem instance. Its update(InventoryItem)
 *  function updates the contained views with the information of the Inventory-
 *  Item instance. Its expand and collapse functions allow for an optional anim-
 *  ation. */
class InventoryItemView(context: Context) :
    ConstraintLayout(ContextThemeWrapper(context, R.style.RecyclerViewItemStyle))
{
    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    private val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    private val editButtonIconController: AnimatedIconController

    init {
        inflate(context, R.layout.inventory_item_layout, this)
        editButtonIconController = AnimatedIconController.forView(editButton)
        editButtonIconController.addTransition(
            editButtonIconController.addState("edit"), editButtonIconController.addState("more_options"),
            ContextCompat.getDrawable(context, R.drawable.animated_edit_to_more_options_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_more_options_to_edit_icon) as AnimatedVectorDrawable)
        colorEdit.background = ColoredCircleDrawable(colorEdit.layoutParams.width.toFloat(),
                                                     0, nameEdit.currentTextColor)

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
        val colorIndex = item.color.coerceIn(ViewModelItem.Colors.indices)
        (colorEdit.background as ColoredCircleDrawable).color = ViewModelItem.Colors[colorIndex]
        inventoryAmountEdit.initCurrentValue(item.amount)

        autoAddToShoppingListCheckBox.isChecked = item.autoAddToShoppingList
        autoAddToShoppingListTriggerEdit.initCurrentValue(item.autoAddToShoppingListTrigger)

        if (isExpanded) expand(false)
        else            collapse(false)
    }

    fun expand(animate: Boolean = true) {
        _isExpanded = true
        nameEdit.isEditable = true
        inventoryAmountEdit.isEditable = true
        extraInfoEdit.isEditable = true
        autoAddToShoppingListTriggerEdit.isEditable = true
        editButtonIconController.setState("more_options", animate)

        if (animate)
            expandCollapseAnimation(true, extraInfoEdit.text.isNullOrBlank()).start()
        else {
            inventoryItemDetailsInclude.visibility = View.VISIBLE
            extraInfoEdit.visibility = View.VISIBLE
        }
    }

    fun collapse(animate: Boolean = true) {
        if (nameEdit.isFocused || extraInfoEdit.isFocused ||
            inventoryAmountEdit.valueEdit.isFocused ||
            autoAddToShoppingListTriggerEdit.valueEdit.isFocused)
                imm?.hideSoftInputFromWindow(windowToken, 0)

        _isExpanded = false
        nameEdit.isEditable = false
        inventoryAmountEdit.isEditable = false
        extraInfoEdit.isEditable = false
        autoAddToShoppingListTriggerEdit.isEditable = false
        editButtonIconController.setState("edit", animate)

        val extraInfoNeedsCollapsed = extraInfoEdit.text.isNullOrBlank()
        if (animate) {
            val anim = expandCollapseAnimation(false, extraInfoNeedsCollapsed)
            anim.doOnEnd {
                inventoryItemDetailsInclude.visibility = View.GONE
                if (extraInfoNeedsCollapsed)
                    extraInfoEdit.visibility = View.GONE
            }
            anim.start()
        } else {
            if (extraInfoNeedsCollapsed)
                extraInfoEdit.visibility = View.GONE
            inventoryItemDetailsInclude.visibility = View.GONE
        }
    }

    private fun expandCollapseAnimation(expanding: Boolean, animatingExtraInfo: Boolean): ValueAnimator {
        inventoryItemDetailsInclude.visibility = View.VISIBLE

        val matchParentSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        inventoryItemDetailsInclude.measure(matchParentSpec, wrapContentSpec)
        val endHeight = height + (if (expanding) 1 else -1) *
                        inventoryItemDetailsInclude.measuredHeight

        var extraInfoStartHeight = 0
        var extraInfoHeightChange = 0
        if (animatingExtraInfo) {
            extraInfoEdit.visibility = View.VISIBLE
            extraInfoEdit.measure(wrapContentSpec, wrapContentSpec)
            extraInfoStartHeight = if (expanding) 0 else extraInfoEdit.measuredHeight
            extraInfoHeightChange = extraInfoEdit.measuredHeight * (if (expanding) 1 else -1)
        }

        val anim = ValueAnimator.ofInt(height, endHeight)
        anim.addUpdateListener {
            layoutParams.height = anim.animatedValue as Int
            if (animatingExtraInfo) {
                extraInfoEdit.layoutParams.height = extraInfoStartHeight +
                        (anim.animatedFraction * extraInfoHeightChange).toInt()
                extraInfoEdit.requestLayout()
            }
            else requestLayout()
        }
        anim.duration = 200
        anim.interpolator = FastOutSlowInInterpolator()
        return anim
    }
}