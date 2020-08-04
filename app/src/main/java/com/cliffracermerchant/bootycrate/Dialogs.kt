/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

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

fun colorPickerDialog(context: Context, fragmentManager: FragmentManager,
                      initialColor: Int = -8355712 /*Medium gray*/,
                      callback: (Int) -> Unit ) {
    val colors = context.resources.getIntArray(R.array.color_picker_presets)
    val colorPicker = ColorSheet().colorPicker(colors, initialColor,
                                               noColorOption = false)
                                               { color -> callback(color) }
    colorPicker.show(fragmentManager)
}

// AlertDialog seems to ignore the theme's alertDialogTheme value, making it
// necessary to pass alertDialogTheme's value in manually to the
// AlertDialog.builder constructor

fun selectInventoryItemDialog(context: Context, inventoryItems: List<InventoryItem>?,
                              initiallySelectedItemId: Long?, snackBarAnchor: View,
                              callback: (InventoryItem?) -> Unit) {
    if (inventoryItems == null || inventoryItems.isEmpty()) {
        val string = context.getString(R.string.empty_inventory_message)
        val snackBar = Snackbar.make(snackBarAnchor, string, Snackbar.LENGTH_LONG)
        snackBar.show()
        return
    }
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.alertDialogTheme, typedValue, true)
    val builder = AlertDialog.Builder(context, typedValue.data)
    builder.setTitle(context.getString(R.string.link_inventory_item_action_long_description))
    val recyclerView = InventoryRecyclerViewDialog(context, inventoryItems,
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

fun newInventoryItemDialog(context: Context, fragmentManager: FragmentManager,
                           callback: (InventoryItem?) -> Unit) {
    val newItem = InventoryItem()
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.alertDialogTheme, typedValue, true)
    val builder = AlertDialog.Builder(context, typedValue.data)
    builder.setTitle(context.getString(R.string.add_item_button_name))
    val itemView = InventoryItemView(context)
    val colorEdit = itemView.colorEdit.background as ColoredCircleDrawable
    itemView.update(newItem, isExpanded = true)
    itemView.colorEdit.setOnClickListener {
        colorPickerDialog(context, fragmentManager, colorEdit.color) { chosenColor ->
            colorEdit.color = chosenColor
        }
    }
    itemView.editButton.isVisible = false
    itemView.collapseButton.isVisible = false
    val dialogClickListener = DialogInterface.OnClickListener { _, button ->
        if (button == DialogInterface.BUTTON_POSITIVE) {
            newItem.name = itemView.nameEdit.text.toString()
            newItem.extraInfo = itemView.extraInfoEdit.text.toString()
            newItem.color = colorEdit.color
            newItem.amount = itemView.amountEdit.currentValue
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

fun newShoppingListItemDialog(context: Context, fragmentManager: FragmentManager,
                              callback: (ShoppingListItem?) -> Unit) {
    val newItem = ShoppingListItem()
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.alertDialogTheme, typedValue, true)
    val builder = AlertDialog.Builder(context, typedValue.data)
    builder.setTitle(context.getString(R.string.add_item_button_name))
    val itemView = InventoryItemView(context)
    val colorEdit = itemView.colorEdit.background as ColoredCircleDrawable
    colorEdit.color = newItem.color
    itemView.colorEdit.setOnClickListener {
        colorPickerDialog(context, fragmentManager, newItem.color) { chosenColor ->
            colorEdit.color = chosenColor
        }
    }
    itemView.editButton.isVisible = false
    itemView.collapseButton.isVisible = false
    itemView.nameEdit.isEditable = true
    itemView.extraInfoEdit.isEditable = true
    itemView.amountEdit.isEditable = true
    val dialogClickListener = DialogInterface.OnClickListener { _, button ->
        if (button == DialogInterface.BUTTON_POSITIVE) {
            newItem.name = itemView.nameEdit.text.toString()
            newItem.extraInfo = itemView.extraInfoEdit.text.toString()
            newItem.color = colorEdit.color
            newItem.amountOnList = itemView.amountEdit.currentValue
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