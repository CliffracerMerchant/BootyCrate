/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val pendingViewPropAnimations = ViewPropertyAnimatorSet()
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

        checkBox.isChecked = item.isChecked
        if (!item.isChecked) defaultOnCheckedChange(checked = item.isChecked, animate = false)
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

    fun expand(animate: Boolean = true): Int {
        _isExpanded = true
        nameEdit.isEditable = true
        shoppingListAmountEdit.isEditable = true
        extraInfoEdit.isEditable = true
        setAmountEditable(true, animate)
        if (extraInfoEdit.text.isNullOrBlank())
            setExtraInfoVisible(true, animate)
        val expandAnimAndHeightChange = setDetailsVisible(true, animate)

        editButtonIconController.setState("more_options", animate)
        if (checkBox.isChecked) checkBoxCheckmarkController.setState("unchecked", animate)
        checkBoxBackgroundController.setState("edit_color", animate)

        if (animate) {
            expandAnimAndHeightChange!!.first.start()
            pendingViewPropAnimations.start()
            return expandAnimAndHeightChange.second
        }
        return 0
    }

    fun collapse(animate: Boolean = true): Int {
        if (nameEdit.isFocused || extraInfoEdit.isFocused ||
            shoppingListAmountEdit.valueEdit.isFocused)
                imm.hideSoftInputFromWindow(windowToken, 0)

        _isExpanded = false
        nameEdit.isEditable = false
        shoppingListAmountEdit.isEditable = false
        extraInfoEdit.isEditable = false

        setAmountEditable(false, animate)
        if (extraInfoEdit.text.isNullOrBlank())
            setExtraInfoVisible(false, animate)
        val collapseAnimAndHeightChange = setDetailsVisible(false, animate)

        editButtonIconController.setState("edit", animate)
        if (checkBox.isChecked) {
            checkBoxCheckmarkController.setState("checked", animate)
            checkBoxBackgroundController.setState("checked", animate)
        }
        else checkBoxBackgroundController.setState("unchecked", animate)

        if (animate) {
            collapseAnimAndHeightChange!!.first.start()
            pendingViewPropAnimations.start()
            return collapseAnimAndHeightChange.second
        }
        return -50
    }

    fun defaultOnCheckedChange(checked: Boolean, animate: Boolean = true) {
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

    private fun setAmountEditable(makingEditable: Boolean = true, animate: Boolean = true) {
        decreaseButtonIconController.setState(if (makingEditable) "minus" else "multiply", animate)
        increaseButtonIconController.setState(if (makingEditable) "plus" else "blank", animate)

        if (makingEditable) decreaseButton.setOnClickListener { shoppingListAmountEdit.decrement() }
        else                decreaseButton.setOnClickListener(null)
        if (makingEditable) increaseButton.setOnClickListener { shoppingListAmountEdit.increment() }
        else                increaseButton.setOnClickListener(null)
        val increaseButtonFullWidth = increaseButton.drawable.intrinsicWidth +
                                      increaseButton.paddingStart + increaseButton.paddingEnd
        val increaseButtonWidth = if (makingEditable) increaseButtonFullWidth
                                  else               (increaseButtonFullWidth * 1f / 3f).toInt()

        if (!animate) {
            increaseButton.layoutParams.width = increaseButtonWidth
            increaseButton.requestLayout()
        } else {
            // The decrease button does not use a graphical layer here because its icon
            // should also be animating at the same time as the translation animation
            val translationStart = if (!makingEditable) 0f
                                   else increaseButtonFullWidth * 2f/3f
            val translationEnd = if (makingEditable) 0f
                                 else increaseButtonFullWidth * 2f/3f

            decreaseButton.translationX = translationStart
            valueEdit.translationX = translationStart
            if (makingEditable) {
                increaseButton.layoutParams.width = increaseButtonFullWidth
                increaseButton.requestLayout()
            }
            pendingViewPropAnimations.add(
                decreaseButton.animate().translationX(translationEnd).setDuration(200)
                    .withEndAction { decreaseButton.translationX = 0f })
            pendingViewPropAnimations.add(
                valueEdit.animate().translationX(translationEnd).withLayer().setDuration(200).withEndAction {
                    valueEdit.translationX = 0f
                    if (!makingEditable) {
                        increaseButton.layoutParams.width = increaseButtonWidth
                        increaseButton.requestLayout()
                    }
                })
        }
    }


    private fun setExtraInfoVisible(makingVisible: Boolean = true, animate: Boolean = true) {
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        extraInfoEdit.measure(wrapContentSpec, wrapContentSpec)
        val nameEditTranslationY = if (makingVisible) 0f
                                   else extraInfoEdit.measuredHeight/ 2.5f
        if (!animate) {
            extraInfoEdit.alpha = if (makingVisible) 1f else 0f
            nameEdit.translationY = nameEditTranslationY
        }
        else {
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
        shoppingListItemDetailsGroup.visibility = if (makingVisible) View.VISIBLE
                                                  else               View.GONE
        if (!animate) return null

        measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val heightChange = measuredHeight - startHeight
        layoutParams.height = startHeight
        val anim = ObjectAnimator.ofInt(this, "bottom", bottom + heightChange)
        anim.doOnEnd {
            if (!makingVisible) shoppingListItemDetailsGroup.visibility = View.GONE
            layoutParams.height = startHeight + heightChange
            requestLayout()
        }
        anim.duration = 200
        return Pair(anim, heightChange)
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