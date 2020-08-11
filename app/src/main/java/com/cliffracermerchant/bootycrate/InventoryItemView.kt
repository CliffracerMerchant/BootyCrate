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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
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
class InventoryItemView(context: Context) : ConstraintLayout(context) {
    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    private val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    private val editButtonIconController: TwoStateAnimatedIconController

    init {
        inflate(context, R.layout.inventory_item_layout, this)
        editButtonIconController = TwoStateAnimatedIconController.forView(editButton,
            context.getDrawable(R.drawable.animated_edit_to_more_options_icon) as AnimatedVectorDrawable,
            context.getDrawable(R.drawable.animated_more_options_to_edit_icon) as AnimatedVectorDrawable)
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
        val colorIndex = item.color.coerceIn(BootyCrateItem.Colors.indices)
        (colorEdit.background as ColoredCircleDrawable).color = BootyCrateItem.Colors[colorIndex]
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
        editButtonIconController.toStateB(animate)

        if (animate) expandCollapseAnimation(true).start()
        else inventoryItemDetailsInclude.visibility = View.VISIBLE
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
        editButtonIconController.toStateA(animate)

        if (animate) {
            val anim = expandCollapseAnimation(false)
            anim.doOnEnd { inventoryItemDetailsInclude.visibility = View.GONE }
            anim.start()
        } else inventoryItemDetailsInclude.visibility = View.GONE
    }

    private fun expandCollapseAnimation(expanding: Boolean) : ValueAnimator {
        inventoryItemDetailsInclude.visibility = View.VISIBLE

        val matchParentSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        inventoryItemDetailsInclude.measure(matchParentSpec, wrapContentSpec)
        val endHeight = height + (if (expanding) 1 else -1) *
                        inventoryItemDetailsInclude.measuredHeight

        val anim = ValueAnimator.ofInt(height, endHeight)
        anim.addUpdateListener {
            layoutParams.height = anim.animatedValue as Int
            requestLayout()
        }
        anim.duration = 200
        anim.interpolator = FastOutSlowInInterpolator()
        return anim
    }
}