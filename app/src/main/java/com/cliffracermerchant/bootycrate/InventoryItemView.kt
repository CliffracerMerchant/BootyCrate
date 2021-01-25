/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import com.cliffracermerchant.bootycrate.databinding.InventoryItemDetailsBinding
import com.cliffracermerchant.bootycrate.databinding.InventoryItemBinding
import kotlinx.android.synthetic.main.integer_edit.view.*

/** A layout to display the contents of an inventory item.
 *
 *  InventoryItemView is a ExpandableSelectableItemView subclass that inflates
 *  a layout resource to display the data of an InventoryItem instance. Its
 *  update override updates the contained views with the information of the
 *  InventoryItem instance. It also overrides the setExpanded function with an
 *  implementation that shows or hides the inventory item's extra details. */
class InventoryItemView(context: Context, attrs: AttributeSet? = null) :
    ExpandableSelectableItemView<InventoryItem>(context, attrs)
{
    private val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    val ui = InventoryItemBinding.inflate(LayoutInflater.from(context), this)
    val detailsUi = InventoryItemDetailsBinding.bind(ui.root)

    var color get() = ui.colorEdit.imageTintList?.defaultColor ?: 0
              set(value) = setColor(value)
    var colorIndex get() = ViewModelItem.Colors.indexOf(color)
                   set(value) = setColorIndex(value)

    init {
        ui.editButton.setOnClickListener {
            if (isExpanded) //TODO Implement more options menu
            else            expand()
        }
        detailsUi.collapseButton.setOnClickListener{ collapse() }

        layoutTransition = delaylessLayoutTransition()
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun update(item: InventoryItem) {
        ui.nameEdit.setText(item.name)
        ui.extraInfoEdit.setText(item.extraInfo)
        setColorIndex(item.color, animate = false)
        ui.inventoryAmountEdit.initValue(item.amount)
        detailsUi.addToShoppingListCheckBox.isChecked = item.addToShoppingList
        detailsUi.addToShoppingListTriggerEdit.initValue(item.addToShoppingListTrigger)
        super.update(item)
    }

    override fun setExpanded(expanded: Boolean) {
        super.setExpanded(expanded)
        if (!expanded && ui.nameEdit.isFocused || ui.extraInfoEdit.isFocused ||
            ui.inventoryAmountEdit.valueEdit.isFocused ||
            detailsUi.addToShoppingListTriggerEdit.valueEdit.isFocused)
                imm?.hideSoftInputFromWindow(windowToken, 0)

        ui.nameEdit.isEditable = expanded
        ui.inventoryAmountEdit.valueIsDirectlyEditable = expanded
        ui.extraInfoEdit.isEditable = expanded
        detailsUi.addToShoppingListTriggerEdit.valueIsDirectlyEditable = expanded
        ui.editButton.isActivated = expanded

        val newVisibility = if (expanded) View.VISIBLE
                            else          View.GONE
        detailsUi.inventoryItemDetailsGroup.visibility = newVisibility
        if (ui.extraInfoEdit.text.isNullOrBlank())
            ui.extraInfoEdit.visibility = newVisibility

        // For some reason, expanding an inventory item whose extra info is not blank
        // causes a flicker. It appears that changing the layout params of a view and
        // requesting a layout for it stops this flicker from occurring. Given that
        // the bug seems to originate from somewhere deep in Android code, having
        // this useless layout parameter changing and requested layout is an easy and
        // performance negligible way to prevent the bug.
        ui.spacer.layoutParams.width = if (expanded) 1 else 0
        ui.spacer.requestLayout()
    }

    fun setColor(color: Int, animate: Boolean = true) {
        if (!animate) {
            ui.colorEdit.imageTintList = ColorStateList.valueOf(color)
            return
        }
        ObjectAnimator.ofArgb(ui.colorEdit, "tint", color).apply {
            duration = 200L
            addUpdateListener {
                ui.colorEdit.imageTintList = ColorStateList.valueOf(it.animatedValue as Int)
            }
        }.start()
    }

    fun setColorIndex(colorIndex: Int, animate: Boolean = true) {
        val index = colorIndex.coerceIn(ViewModelItem.Colors.indices)
        setColor(ViewModelItem.Colors[index], animate)
    }
}