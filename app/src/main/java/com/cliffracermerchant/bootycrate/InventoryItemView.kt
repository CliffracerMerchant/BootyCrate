/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
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
    private val pendingViewPropAnimations = ViewPropertyAnimatorSet()

    init {
        inflate(context, R.layout.inventory_item_layout, this)
        editButtonIconController = AnimatedImageViewController(editButton)
        editButtonIconController.addTransition(
            editButtonIconController.addState("edit"), editButtonIconController.addState("more_options"),
            ContextCompat.getDrawable(context, R.drawable.animated_edit_to_more_options_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_more_options_to_edit_icon) as AnimatedVectorDrawable)

        editButton.setOnClickListener {
            if (isExpanded) //TODO Implement more options menu
            else          expand()
        }
        collapseButton.setOnClickListener{ collapse() }
    }

    fun update(item: InventoryItem, isExpanded: Boolean = false) {
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        val colorIndex = item.color.coerceIn(ViewModelItem.Colors.indices)
        colorEdit.drawable.setTint(ViewModelItem.Colors[colorIndex])
        inventoryAmountEdit.initCurrentValue(item.amount)

        addToShoppingListCheckBox.isChecked = item.addToShoppingList
        addToShoppingListTriggerEdit.initCurrentValue(item.addToShoppingListTrigger)

        if (isExpanded) expand(false)
        else            collapse(false)
    }

    fun expand(animate: Boolean = true): Int {
        _isExpanded = true
        nameEdit.isEditable = true
        inventoryAmountEdit.isEditable = true
        extraInfoEdit.isEditable = true
        addToShoppingListTriggerEdit.isEditable = true
        editButtonIconController.setState("more_options", animate)

        if (extraInfoEdit.text.isNullOrBlank())
            setExtraInfoVisible(true, animate)
        val expandAnimAndHeightChange = setDetailsVisible(true, animate)

        if (animate) {
            expandAnimAndHeightChange!!.first.start()
            pendingViewPropAnimations.start()
            return expandAnimAndHeightChange.second
        }
        return 0
    }

    fun collapse(animate: Boolean = true): Int {
        if (nameEdit.isFocused || extraInfoEdit.isFocused ||
            inventoryAmountEdit.valueEdit.isFocused ||
            addToShoppingListTriggerEdit.valueEdit.isFocused)
                imm?.hideSoftInputFromWindow(windowToken, 0)

        _isExpanded = false
        nameEdit.isEditable = false
        inventoryAmountEdit.isEditable = false
        extraInfoEdit.isEditable = false
        addToShoppingListTriggerEdit.isEditable = false
        editButtonIconController.setState("edit", animate)

        if (extraInfoEdit.text.isNullOrBlank())
            setExtraInfoVisible(false, animate)
        val collapseAnimAndHeightChange = setDetailsVisible(false, animate)

        if (animate) {
            collapseAnimAndHeightChange!!.first.start()
            pendingViewPropAnimations.start()
            return collapseAnimAndHeightChange.second
        }
        return 0
    }

    private fun setExtraInfoVisible(makingVisible: Boolean = true, animate: Boolean = true) {
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        extraInfoEdit.measure(wrapContentSpec, wrapContentSpec)
        val nameEditTranslationY = if (makingVisible) 0f
        else extraInfoEdit.measuredHeight/ 2.2f
        if (!animate) {
            extraInfoEdit.alpha = if (makingVisible) 1f else 0f
            nameEdit.translationY = nameEditTranslationY
        } else {
            pendingViewPropAnimations.add(
                extraInfoEdit.animate().alpha(if (makingVisible) 1f else 0f).withLayer().setDuration(200))
            pendingViewPropAnimations.add(
                nameEdit.animate().translationY(nameEditTranslationY).withLayer().setDuration(200))
        }
    }

    private fun setDetailsVisible(
        makingVisible: Boolean = true,
        animate: Boolean = true
    ) : Pair<ObjectAnimator, Int>? {
        val startHeight = height
        inventoryItemDetailsGroup.visibility = if (makingVisible) View.VISIBLE
                                               else               View.GONE
        if (!animate) return null

        measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val heightChange = measuredHeight - startHeight
        layoutParams.height = startHeight
        val anim = ObjectAnimator.ofInt(this, "bottom", bottom + heightChange)
        anim.doOnEnd {
            if (!makingVisible) inventoryItemDetailsGroup.visibility = View.GONE
            layoutParams.height = startHeight + heightChange
            requestLayout()
        }
        anim.duration = 200
        return Pair(anim, heightChange)
    }
}