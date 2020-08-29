/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*
/** A layout to display the contents of a shopping list item.
 *
 *  ShoppingListItemView is a ConstraintLayout subclass that inflates a layout
 *  to display the data of a ShoppingListItem instance. Its update function
 *  updates the contained views with the information of the provided Shopping-
 *  ListItem. Its expand and collapse functions allow for an optional animation. */
class ShoppingListItemView(context: Context) :
    ConstraintLayout(ContextThemeWrapper(context, R.style.RecyclerViewItemStyle))
{
    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    val decreaseButtonIconController: AnimatedIconController
    val increaseButtonIconController: AnimatedIconController
    val editButtonIconController: AnimatedIconController
    val checkBoxBackgroundController: AnimatedIconController
    val checkBoxCheckmarkController: AnimatedIconController
    var itemColor: Int? = null

    // This companion object stores resources common to all ShoppingListItemViews.
    private companion object SharedResources {
        private var isInitialized = false
        private lateinit var imm: InputMethodManager
        private lateinit var linkedItemDescriptionString: String
        private lateinit var unlinkedItemDescriptionString: String
        private lateinit var linkNowActionString: String
        private lateinit var changeLinkActionString: String
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.shopping_list_item_layout, this, true)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT)
        if (!SharedResources.isInitialized) initSharedResources(context)

        decreaseButtonIconController = AnimatedIconController.forView(shoppingListAmountEdit.decreaseButton)
        decreaseButtonIconController.addTransition(
            decreaseButtonIconController.addState("multiply"), decreaseButtonIconController.addState("minus"),
            ContextCompat.getDrawable(context, R.drawable.shopping_list_animated_multiply_to_minus_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.shopping_list_animated_minus_to_multiply_icon) as AnimatedVectorDrawable)
        increaseButtonIconController = AnimatedIconController.forView(shoppingListAmountEdit.increaseButton)
        increaseButtonIconController.addTransition(
            increaseButtonIconController.addState("blank"), increaseButtonIconController.addState("plus"),
            ContextCompat.getDrawable(context, R.drawable.animated_blank_to_plus_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_plus_to_blank_icon) as AnimatedVectorDrawable)
        editButtonIconController = AnimatedIconController.forView(editButton)
        editButtonIconController.addTransition(
            editButtonIconController.addState("edit"), editButtonIconController.addState("more_options"),
            ContextCompat.getDrawable(context, R.drawable.animated_edit_to_more_options_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_more_options_to_edit_icon) as AnimatedVectorDrawable)
        checkBoxBackgroundController = AnimatedIconController.forDrawableLayer(
            checkBox.background as LayerDrawable, R.id.checkBoxBackground)
        val checkBoxBackgroundUncheckedIndex = checkBoxBackgroundController.addState("unchecked")
        val checkBoxBackgroundCheckedIndex = checkBoxBackgroundController.addState("checked")
        val checkBoxBackgroundColorEditIndex = checkBoxBackgroundController.addState("edit_color")
        checkBoxBackgroundController.addTransition(
            checkBoxBackgroundUncheckedIndex, checkBoxBackgroundCheckedIndex,
            ContextCompat.getDrawable(context, R.drawable.animated_checkbox_unchecked_to_checked_background) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_checkbox_checked_to_unchecked_background) as AnimatedVectorDrawable)
        checkBoxBackgroundController.addTransition(
            checkBoxBackgroundUncheckedIndex, checkBoxBackgroundColorEditIndex,
            ContextCompat.getDrawable(context, R.drawable.animated_checkbox_unchecked_background_to_circle) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_circle_to_checkbox_unchecked_background) as AnimatedVectorDrawable)
        checkBoxBackgroundController.addTransition(
            checkBoxBackgroundCheckedIndex, checkBoxBackgroundColorEditIndex,
            ContextCompat.getDrawable(context, R.drawable.animated_checkbox_checked_background_to_circle) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_circle_to_checkbox_checked_background) as AnimatedVectorDrawable)
        checkBoxCheckmarkController = AnimatedIconController.forDrawableLayer(
            checkBox.background as LayerDrawable, R.id.checkBoxCheckmark)
        checkBoxCheckmarkController.addTransition(
            checkBoxCheckmarkController.addState("unchecked"), checkBoxCheckmarkController.addState("checked"),
            ContextCompat.getDrawable(context, R.drawable.animated_checkbox_unchecked_to_checked_checkmark) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_checkbox_checked_to_unchecked_checkmark) as AnimatedVectorDrawable)

        editButton.setOnClickListener {
            if (_isExpanded) //TODO: Implement more options menu
            else            expand()
        }
        collapseButton.setOnClickListener { collapse() }
        checkBox.setOnCheckedChangeListener { _, checked -> defaultOnCheckedChange(checked) }
        linkedToEdit.paintFlags = linkedToEdit.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    fun update(item: ShoppingListItem, isExpanded: Boolean = false) {
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        itemColor = item.color
        val colorIndex = item.color.coerceIn(ViewModelItem.Colors.indices)
        checkBoxBackgroundController.tint = ViewModelItem.Colors[colorIndex]
        shoppingListAmountEdit.initCurrentValue(item.amount)

        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = item.isChecked
        defaultOnCheckedChange(checked = item.isChecked, animate = false)
        checkBox.setOnCheckedChangeListener { _, checked -> defaultOnCheckedChange(checked) }
        updateLinkedStatus(item.linkedInventoryItemId)
        if (isExpanded) expand(false)
        else            collapse(false)
    }

    fun updateLinkedStatus(newLinkedId: Long?) {
        if (newLinkedId != null) {
            linkedToIndicator.text = linkedItemDescriptionString
            linkedToEdit.text = changeLinkActionString
        } else {
            linkedToIndicator.text = unlinkedItemDescriptionString
            linkedToEdit.text = linkNowActionString
        }
    }

    fun expand(animate: Boolean = true) {
        _isExpanded = true
        nameEdit.isEditable = true
        shoppingListAmountEdit.isEditable = true
        extraInfoEdit.isEditable = true
        decreaseButtonIconController.setState("minus", animate)
        increaseButtonIconController.setState("plus", animate)
        editButtonIconController.setState("more_options", animate)
        if (checkBox.isChecked) checkBoxCheckmarkController.setState("unchecked", animate)
        checkBoxBackgroundController.setState("edit_color", animate)

        if (animate)
            expandCollapseAnimation(true, extraInfoEdit.text.isNullOrBlank()).start()
        else {
            extraInfoEdit.visibility = View.VISIBLE
            shoppingListItemDetailsInclude.visibility = View.VISIBLE
            shoppingListAmountEdit.increaseButton.apply { layoutParams.width = background.intrinsicWidth }
        }
    }

    fun collapse(animate: Boolean = true) {
        if (nameEdit.isFocused || extraInfoEdit.isFocused ||
            shoppingListAmountEdit.valueEdit.isFocused)
                imm.hideSoftInputFromWindow(windowToken, 0)

        _isExpanded = false
        nameEdit.isEditable = false
        shoppingListAmountEdit.isEditable = false
        extraInfoEdit.isEditable = false
        decreaseButtonIconController.setState("multiply", animate)
        increaseButtonIconController.setState("blank", animate)
        editButtonIconController.setState("edit", animate)

        if (checkBox.isChecked) {
            checkBoxCheckmarkController.setState("checked", animate)
            checkBoxBackgroundController.setState("checked", animate)
        }
        else checkBoxBackgroundController.setState("unchecked", animate)

        val extraInfoNeedsCollapsed = extraInfoEdit.text.isNullOrBlank()
        if (animate) {
            val anim = expandCollapseAnimation(false, extraInfoNeedsCollapsed)
            anim.doOnEnd {
                shoppingListItemDetailsInclude.visibility = View.GONE
                if (extraInfoNeedsCollapsed)
                    extraInfoEdit.visibility = View.GONE
            }
            anim.start()
        } else {
            if (extraInfoNeedsCollapsed)
                extraInfoEdit.visibility = View.GONE
            shoppingListItemDetailsInclude.visibility = View.GONE
            shoppingListAmountEdit.increaseButton.apply { layoutParams.width = background.intrinsicWidth / 2 }
        }
    }

    fun defaultOnCheckedChange(checked: Boolean, animate: Boolean = true) {
        nameEdit.setStrikethroughEnabled(checked, animate)
        extraInfoEdit.setStrikethroughEnabled(checked, animate)
        if (checked) {
            checkBoxCheckmarkController.setState("checked", animate)
            checkBoxBackgroundController.setState("checked", animate)
        } else {
            checkBoxCheckmarkController.setState("unchecked", animate)
            checkBoxBackgroundController.setState("unchecked", animate)
        }
    }

    private fun expandCollapseAnimation(expanding: Boolean, animatingExtraInfo: Boolean) : ValueAnimator {
        shoppingListItemDetailsInclude.visibility = View.VISIBLE
        val matchParentSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        shoppingListItemDetailsInclude.measure(matchParentSpec, wrapContentSpec)
        val endHeight = height + (if (expanding) 1 else -1) *
                        shoppingListItemDetailsInclude.measuredHeight

        val increaseButtonStartWidth = increaseButton.width
        val increaseButtonWidthChange = if (expanding) increaseButton.width
                                        else           -increaseButton.width / 2

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
            shoppingListAmountEdit.increaseButton.layoutParams.width = increaseButtonStartWidth +
                    (anim.animatedFraction * increaseButtonWidthChange).toInt()
            shoppingListAmountEdit.increaseButton.requestLayout()
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

    private fun initSharedResources(context: Context) {
        imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        linkedItemDescriptionString = context.getString(R.string.linked_shopping_list_item_description)
        unlinkedItemDescriptionString = context.getString(R.string.unlinked_shopping_list_item_description)
        linkNowActionString = context.getString(R.string.shopping_list_item_link_now_action_description)
        changeLinkActionString = context.getString(R.string.shopping_list_item_change_link_action_description)
        isInitialized = true
    }
}