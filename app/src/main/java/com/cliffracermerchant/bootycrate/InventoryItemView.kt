/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.collapseButton
import kotlinx.android.synthetic.main.inventory_item_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.editButton
import kotlinx.android.synthetic.main.inventory_item_layout.view.extraInfoEdit
import kotlinx.android.synthetic.main.inventory_item_layout.view.nameEdit

/** A layout to display the contents of an inventory item.
 *
 *  InventoryItemView is a ExpandableSelectableItemView subclass that inflates
 *  a layout resource to display the data of an InventoryItem instance. Its
 *  update override updates the contained views with the information of the
 *  InventoryItem instance. It also overrides the setExpanded function with an
 *  implementation that shows or hides the inventory item's extra details. */
class InventoryItemView(context: Context) :
    ExpandableSelectableItemView<InventoryItem>(context)
{
    private val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

    var color get() = colorEdit.imageTintList?.defaultColor ?: 0
              set(value) = setColor(value)
    var colorIndex get() = ViewModelItem.Colors.indexOf(color)
                   set(value) = setColorIndex(value)

    init {
        inflate(context, R.layout.inventory_item_layout, this)
        editButton.setOnClickListener {
            if (isExpanded) //TODO Implement more options menu
            else            expand()
        }
        collapseButton.setOnClickListener{ collapse() }

        layoutTransition = delaylessLayoutTransition()
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun update(item: InventoryItem) {
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        setColorIndex(item.color, animate = false)
        inventoryAmountEdit.initValue(item.amount)
        addToShoppingListCheckBox.isChecked = item.addToShoppingList
        addToShoppingListTriggerEdit.initValue(item.addToShoppingListTrigger)
        super.update(item)
    }

    override fun setExpanded(expanded: Boolean) {
        super.setExpanded(expanded)
        if (!expanded && nameEdit.isFocused || extraInfoEdit.isFocused ||
            inventoryAmountEdit.valueEdit.isFocused ||
            addToShoppingListTriggerEdit.valueEdit.isFocused)
                imm?.hideSoftInputFromWindow(windowToken, 0)

        nameEdit.isEditable = expanded
        inventoryAmountEdit.valueIsDirectlyEditable = expanded
        extraInfoEdit.isEditable = expanded
        addToShoppingListTriggerEdit.valueIsDirectlyEditable = expanded
        editButton.isActivated = expanded

        val newVisibility = if (expanded) View.VISIBLE
                            else          View.GONE
        inventoryItemDetailsGroup.visibility = newVisibility
        if (extraInfoEdit.text.isNullOrBlank())
            extraInfoEdit.visibility = newVisibility

        // For some reason, expanding an inventory item whose extra info is not blank
        // causes a flicker. It appears that changing the layout params of a view and
        // requesting a layout for it stops this flicker from occurring. Given that
        // the bug seems to originate from somewhere deep in Android code, having
        // this useless layout parameter changing and requested layout is an easy and
        // performance negligible way to prevent the bug.
        spacer.layoutParams.width = if (expanded) 1 else 0
        spacer.requestLayout()
    }

    fun setColor(color: Int, animate: Boolean = true) {
        if (!animate) {
            colorEdit.imageTintList = ColorStateList.valueOf(color)
            return
        }
        ValueAnimator.ofArgb(colorEdit.imageTintList?.defaultColor ?: 0, color).run {
            addUpdateListener {
                colorEdit.imageTintList = ColorStateList.valueOf(it.animatedValue as Int)
            }
            duration = 200L
            start()
        }}
    fun setColorIndex(colorIndex: Int, animate: Boolean = true) {
        val index = colorIndex.coerceIn(ViewModelItem.Colors.indices)
        setColor(ViewModelItem.Colors[index], animate)
    }
}