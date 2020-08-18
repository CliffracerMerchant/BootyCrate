/* Copyright 2020 Nicholas Hochstetler
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0, or in the file
 * LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_details_layout.view.collapseButton
import kotlinx.android.synthetic.main.inventory_item_layout.view.*
import kotlinx.android.synthetic.main.inventory_item_layout.view.editButton
import kotlinx.android.synthetic.main.inventory_item_layout.view.extraInfoEdit
import kotlinx.android.synthetic.main.inventory_item_layout.view.nameEdit

fun colorPickerDialog(
    fragmentManager: FragmentManager,
    initialColorIndex: Int = 0,
    callback: (Int) -> Unit
) {
    val initialColor = BootyCrateItem.Colors[initialColorIndex.coerceIn(BootyCrateItem.Colors.indices)]
    val colorPicker = ColorSheet().colorPicker(BootyCrateItem.Colors, initialColor,
                                               noColorOption = false) { color ->
        val colorIndex = BootyCrateItem.Colors.indexOf(color)
        callback(if (colorIndex != -1) colorIndex else 0)
    }
    colorPicker.show(fragmentManager)
}

fun selectInventoryItemDialog(
    context: Context,
    inventoryItems: List<InventoryItem>?,
    initiallySelectedItemId: Long?,
    snackBarAnchor: View,
    callback: (InventoryItem?) -> Unit
) {
    if (inventoryItems == null || inventoryItems.isEmpty()) {
        val string = context.getString(R.string.empty_inventory_message)
        val snackBar = Snackbar.make(snackBarAnchor, string, Snackbar.LENGTH_LONG)
        snackBar.show()
        return
    }
    val builder = themedAlertDialogBuilder(context)
    builder.setTitle(context.getString(R.string.link_inventory_item_action_long_description))
    val recyclerView = PopupInventoryRecyclerView(context, inventoryItems,
                                                   initiallySelectedItemId)
    val dialogClickListener = DialogInterface.OnClickListener { _, button ->
        if (button == DialogInterface.BUTTON_POSITIVE)
            callback(recyclerView.selectedItem)
    }
    builder.setPositiveButton(context.getString(android.R.string.ok), dialogClickListener)
    builder.setNegativeButton(context.getString(android.R.string.cancel), dialogClickListener)
    builder.setView(recyclerView)
    builder.show()
}

fun newShoppingListItemDialog(
    context: Context,
    fragmentManager: FragmentManager,
    callback: (ShoppingListItem?) -> Unit
) {
    val newItem = ShoppingListItem()
    val builder = themedAlertDialogBuilder(context)
    builder.setTitle(context.getString(R.string.add_item_button_name))
    val itemView = InventoryItemView(context)
    val colorEdit = itemView.colorEdit.background as ColoredCircleDrawable
    colorEdit.color = newItem.color
    itemView.colorEdit.setOnClickListener {
        colorPickerDialog(fragmentManager, newItem.color)
                         { chosenColor -> colorEdit.setColor(chosenColor) }
    }
    itemView.editButton.isVisible = false
    itemView.collapseButton.isVisible = false
    itemView.nameEdit.isEditable = true
    itemView.extraInfoEdit.isEditable = true
    itemView.inventoryAmountEdit.isEditable = true
    itemView.inventoryAmountEdit.minValue = 1
    val dialogClickListener = DialogInterface.OnClickListener { _, button ->
        if (button == DialogInterface.BUTTON_POSITIVE) {
            newItem.name = itemView.nameEdit.text.toString()
            newItem.extraInfo = itemView.extraInfoEdit.text.toString()
            newItem.color = colorEdit.color
            newItem.amount = itemView.inventoryAmountEdit.currentValue
            callback(newItem)
        }
        else callback(null)
    }
    builder.setPositiveButton(context.getString(android.R.string.ok), dialogClickListener)
    builder.setNegativeButton(context.getString(android.R.string.cancel), dialogClickListener)
    builder.setView(itemView)
    val dialog = builder.create()
    dialog.setOnShowListener {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        itemView.nameEdit.requestFocus()
        imm?.showSoftInput(itemView.nameEdit, InputMethodManager.SHOW_IMPLICIT)
    }
    dialog.show()
}

fun newInventoryItemDialog(
    context: Context,
    fragmentManager: FragmentManager,
    callback: (InventoryItem?) -> Unit
) {
    val newItem = InventoryItem()
    val builder = themedAlertDialogBuilder(context)
    builder.setTitle(context.getString(R.string.add_item_button_name))
    val itemView = InventoryItemView(context)
    val colorEdit = itemView.colorEdit.background as ColoredCircleDrawable
    itemView.update(newItem, isExpanded = true)
    itemView.colorEdit.setOnClickListener {
        colorPickerDialog(fragmentManager, colorEdit.color)
                         { chosenColor -> colorEdit.setColor(chosenColor) }
    }
    itemView.editButton.isVisible = false
    itemView.collapseButton.isVisible = false
    val dialogClickListener = DialogInterface.OnClickListener { _, button ->
        if (button == DialogInterface.BUTTON_POSITIVE) {
            newItem.name = itemView.nameEdit.text.toString()
            newItem.extraInfo = itemView.extraInfoEdit.text.toString()
            newItem.color = colorEdit.color
            newItem.amount = itemView.inventoryAmountEdit.currentValue
            newItem.autoAddToShoppingList = itemView.autoAddToShoppingListCheckBox.isChecked
            newItem.autoAddToShoppingListTrigger = itemView.autoAddToShoppingListTriggerEdit.currentValue
            callback(newItem)
        }
        else callback(null)
    }
    builder.setPositiveButton(context.getString(android.R.string.ok), dialogClickListener)
    builder.setNegativeButton(context.getString(android.R.string.cancel), dialogClickListener)
    builder.setView(itemView)
    val dialog = builder.create()
    dialog.setOnShowListener {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        itemView.nameEdit.requestFocus()
        imm?.showSoftInput(itemView.nameEdit, InputMethodManager.SHOW_IMPLICIT)
    }
    dialog.show()
}

// AlertDialog seems to ignore the theme's alertDialogTheme value, making it
// necessary to pass it's value in manually to the AlertDialog.builder constructor
internal fun themedAlertDialogBuilder(context: Context): AlertDialog.Builder {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.alertDialogTheme, typedValue, true)
    return AlertDialog.Builder(context, typedValue.data)
}