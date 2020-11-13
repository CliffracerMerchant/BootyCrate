/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*

/** A layout to display the contents of a shopping list item.
 *
 *  ShoppingListItemView is a ExpandableSelectableItemView subclass that
 *  inflates a layout resource to display the data of a ShoppingListItem
 *  instance. Its update override updates the contained views with the informa-
 *  tion of the provided ShoppingListItem. It also overrides the setExpanded
 *  function with an implementation that shows or hides the shopping list
 *  item's extra details. */
class ShoppingListItemView(context: Context) :
    ExpandableSelectableItemView<ShoppingListItem>(context)
{
    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    val decreaseButtonIconController: AnimatedIconController
    val increaseButtonIconController: AnimatedIconController
    val editButtonIconController: AnimatedIconController
    val checkBoxBackgroundController: AnimatedIconController
    val checkBoxCheckmarkController: AnimatedIconController

    // This companion object stores resources common to all ShoppingListItemViews.
    private companion object SharedResources {
        private var isInitialized = false
        private lateinit var imm: InputMethodManager
        private lateinit var linkedItemDescriptionString: String
        private lateinit var unlinkedItemDescriptionString: String
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.shopping_list_item_layout, this, true)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT)
        if (!SharedResources.isInitialized) initSharedResources(context)

        decreaseButtonIconController = AnimatedImageViewController(shoppingListAmountEdit.decreaseButton)
        decreaseButtonIconController.addTransition(
            decreaseButtonIconController.addState("multiply"), decreaseButtonIconController.addState("minus"),
            ContextCompat.getDrawable(context, R.drawable.shopping_list_animated_multiply_to_minus_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.shopping_list_animated_minus_to_multiply_icon) as AnimatedVectorDrawable)
        increaseButtonIconController = AnimatedImageViewController(shoppingListAmountEdit.increaseButton)
        increaseButtonIconController.addTransition(
            increaseButtonIconController.addState("blank"), increaseButtonIconController.addState("plus"),
            ContextCompat.getDrawable(context, R.drawable.animated_blank_to_plus_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_plus_to_blank_icon) as AnimatedVectorDrawable)
        editButtonIconController = AnimatedImageViewController(editButton)
        editButtonIconController.addTransition(
            editButtonIconController.addState("edit"), editButtonIconController.addState("more_options"),
            ContextCompat.getDrawable(context, R.drawable.animated_edit_to_more_options_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_more_options_to_edit_icon) as AnimatedVectorDrawable)
        checkBoxBackgroundController = AnimatedDrawableLayer(checkBox.background as LayerDrawable, R.id.checkBoxBackground)
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
        checkBoxCheckmarkController = AnimatedDrawableLayer(checkBox.background as LayerDrawable, R.id.checkBoxCheckmark)
        checkBoxCheckmarkController.addTransition(
            checkBoxCheckmarkController.addState("unchecked"), checkBoxCheckmarkController.addState("checked"),
            ContextCompat.getDrawable(context, R.drawable.animated_checkbox_unchecked_to_checked_checkmark) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(context, R.drawable.animated_checkbox_checked_to_unchecked_checkmark) as AnimatedVectorDrawable)

        editButton.setOnClickListener {
            if (_isExpanded) //TODO: Implement more options menu
            else             expand()
        }
        collapseButton.setOnClickListener { collapse() }
        checkBox.setOnCheckedChangeListener { _, checked -> setVisualCheckedState(checked) }

        layoutTransition = delaylessLayoutTransition()
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun update(item: ShoppingListItem) {
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        val colorIndex = item.color.coerceIn(ViewModelItem.Colors.indices)
        checkBoxColor = ViewModelItem.Colors[colorIndex]
        shoppingListAmountEdit.initCurrentValue(item.amount)
        linkedToIndicator.text = if (item.linkedItemId != null) linkedItemDescriptionString
                                 else                           unlinkedItemDescriptionString

        checkBox.isChecked = item.isChecked
        setVisualCheckedState(checked = false, animate = false)
        super.update(item)
    }

    override fun setExpanded(expanded: Boolean) {
        if (!expanded && nameEdit.isFocused || extraInfoEdit.isFocused ||
            shoppingListAmountEdit.valueEdit.isFocused)
                imm.hideSoftInputFromWindow(windowToken, 0)

        _isExpanded = expanded
        nameEdit.isEditable = expanded
        shoppingListAmountEdit.isEditable = expanded
        extraInfoEdit.isEditable = expanded
        setAmountEditable(expanded)

        if (expanded) {
            editButtonIconController.setState("more_options")
            checkBoxBackgroundController.setState("edit_color")
            if (checkBox.isChecked)
                checkBoxCheckmarkController.setState("unchecked")
        } else {
            editButtonIconController.setState("edit")
            if (checkBox.isChecked) {
                checkBoxCheckmarkController.setState("checked")
                checkBoxBackgroundController.setState("checked")
            } else {
                checkBoxCheckmarkController.setState("unchecked")
                checkBoxBackgroundController.setState("unchecked")
            }
        }
        val newVisibility = if (expanded) View.VISIBLE
                            else          View.GONE
        shoppingListItemDetailsGroup.visibility = newVisibility
        if (extraInfoEdit.text.isNullOrBlank())
            extraInfoEdit.visibility = newVisibility
    }

    fun setVisualCheckedState(checked: Boolean, animate: Boolean = true) {
        nameEdit.setStrikeThroughEnabled(checked, animate)
        extraInfoEdit.setStrikeThroughEnabled(checked, animate)
        if (checked) {
            checkBoxCheckmarkController.setState("checked", animate)
            checkBoxBackgroundController.setState("checked", animate)
        } else {
            checkBoxCheckmarkController.setState("unchecked", animate)
            checkBoxBackgroundController.setState("unchecked", animate)
        }
    }

    var checkBoxColor get() = checkBoxBackgroundController.tint
                      set(value) = setCheckboxColor(value)
    private fun setCheckboxColor(color: Int?, animate: Boolean = true) {
        if (!animate) checkBoxBackgroundController.tint = color
        else {
            val anim = ValueAnimator.ofArgb(checkBoxBackgroundController.tint ?: 0, color ?: 0)
            anim.addUpdateListener { checkBoxBackgroundController.tint = anim.animatedValue as Int }
            anim.start()
        }
    }

    private fun setAmountEditable(makingEditable: Boolean = true) {
        decreaseButtonIconController.setState(if (makingEditable) "minus" else "multiply")
        increaseButtonIconController.setState(if (makingEditable) "plus" else "blank")

        if (makingEditable) decreaseButton.setOnClickListener { shoppingListAmountEdit.decrement() }
        else                decreaseButton.setOnClickListener(null)
        if (makingEditable) increaseButton.setOnClickListener { shoppingListAmountEdit.increment() }
        else                increaseButton.setOnClickListener(null)
        val increaseButtonFullWidth = increaseButton.drawable.intrinsicWidth +
                                      increaseButton.paddingStart + increaseButton.paddingEnd
        val increaseButtonWidth = if (makingEditable) increaseButtonFullWidth
                                  else               (increaseButtonFullWidth * 1f / 3f).toInt()

        increaseButton.layoutParams.width = increaseButtonWidth
        increaseButton.requestLayout()
    }

    private fun initSharedResources(context: Context) {
        imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        linkedItemDescriptionString = context.getString(R.string.linked_item_description)
        unlinkedItemDescriptionString = context.getString(R.string.unlinked_item_description)
        isInitialized = true
    }
}