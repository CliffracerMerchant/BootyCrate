/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
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
    val initialColor = ViewModelItem.Colors[initialColorIndex.coerceIn(ViewModelItem.Colors.indices)]
    val colorPicker = ColorSheet().colorPicker(ViewModelItem.Colors, initialColor,
                                               noColorOption = false) { color ->
        val colorIndex = ViewModelItem.Colors.indexOf(color)
        callback(if (colorIndex != -1) colorIndex else 0)
    }
    colorPicker.show(fragmentManager)
}

fun newShoppingListItemDialog(
    context: Context,
    fragmentManager: FragmentManager,
    callback: (ShoppingListItem) -> Unit
) {
    val itemView = InventoryItemView(context)
    itemView.background = null
    itemView.editButton.visibility = View.GONE
    itemView.inventoryItemDetailsGroup.visibility = View.GONE
    val colorEdit = itemView.colorEdit.drawable
    var colorIndex = 0
    colorEdit.setTint(ViewModelItem.Colors[colorIndex])
    itemView.colorEdit.setOnClickListener {
        colorPickerDialog(fragmentManager, ViewModelItem.Colors[0]) { chosenColorIndex ->
            ObjectAnimator.ofArgb(colorEdit, "tint", ViewModelItem.Colors[colorIndex],
                                  ViewModelItem.Colors[chosenColorIndex]).start()
            colorIndex = chosenColorIndex
        }
    }
    itemView.nameEdit.isEditable = true
    itemView.extraInfoEdit.isEditable = true
    itemView.inventoryAmountEdit.valueIsDirectlyEditable = true
    itemView.inventoryAmountEdit.minValue = 1

    val dialog = themedAlertDialogBuilder(context).
        setTitle(context.getString(R.string.add_item_button_name)).
        setPositiveButton(android.R.string.ok) { _, _ ->
            callback(ShoppingListItem(name = itemView.nameEdit.text.toString(),
                                      extraInfo = itemView.extraInfoEdit.text.toString(),
                                      color = colorIndex,
                                      amount = itemView.inventoryAmountEdit.currentValue))
        }.setNegativeButton(android.R.string.cancel) { _, _ -> }.
        setView(itemView).create()
    // The dialog dimming is disabled here to prevent
    // flickering if the color sheet dialog is opened on top.
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
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
    callback: (InventoryItem) -> Unit
) {
    val itemView = InventoryItemView(context)
    itemView.background = null
    itemView.expand()
    itemView.editButton.visibility = View.GONE
    // Setting collapseButton's visibility to GONE or INVISIBLE doesn't seem to work for some reason
    itemView.collapseButton.alpha = 0f
    itemView.collapseButton.setOnClickListener {}
    itemView.addToShoppingListTriggerEdit.currentValue = itemView.addToShoppingListTriggerEdit.minValue
    val colorEdit = itemView.colorEdit.drawable
    var colorIndex = 0
    colorEdit.setTint(ViewModelItem.Colors[colorIndex])
    itemView.colorEdit.setOnClickListener {
        colorPickerDialog(fragmentManager, ViewModelItem.Colors[0]) { chosenColorIndex ->
            ObjectAnimator.ofArgb(colorEdit, "tint", ViewModelItem.Colors[colorIndex],
                                  ViewModelItem.Colors[chosenColorIndex]).start()
            colorIndex = chosenColorIndex
        }
    }

    val dialog = themedAlertDialogBuilder(context).
        setTitle(context.getString(R.string.add_item_button_name)).
        setPositiveButton(android.R.string.ok) { _, _ ->
            callback(InventoryItem(name = itemView.nameEdit.text.toString(),
                                   extraInfo = itemView.extraInfoEdit.text.toString(),
                                   color = colorIndex,
                                   amount = itemView.inventoryAmountEdit.currentValue,
                                   addToShoppingList = itemView.addToShoppingListCheckBox.isChecked,
                                   addToShoppingListTrigger = itemView.addToShoppingListTriggerEdit.currentValue))
        }.setNegativeButton(android.R.string.cancel) { _, _ -> }.
        setView(itemView).create()
    // The dialog dimming is disabled here to prevent
    // flickering if the color sheet dialog is opened on top.
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    dialog.setOnShowListener {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        itemView.nameEdit.requestFocus()
        imm?.showSoftInput(itemView.nameEdit, InputMethodManager.SHOW_IMPLICIT)
    }
    dialog.show()
}

fun <Entity: ViewModelItem>exportAsDialog(
    context: Context,
    items: List<Entity>,
    insertBlankLineBetweenColors: Boolean,
    snackBarAnchor: View
) {
    val titleText = context.getString(R.string.export_dialog_title)
    val titleView = themedAlertDialogTitle(context, titleText)

    themedAlertDialogBuilder(context).
        setCustomTitle(titleView).
        setItems(R.array.export_options) { _, chosenOption ->
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.type = "HTTP.PLAIN_TEXT_TYPE"
            var message = ""

            if (insertBlankLineBetweenColors) {
                var currentColorIndex = items.first().color
                for (item in items) {
                    if (item.color != currentColorIndex) {
                        message += "\n"
                        currentColorIndex = item.color
                    }
                    message += item.toString() + "\n"
                }
            } else for (item in items)
                message += item.toString() + "\n"

            when (chosenOption) {
                0 /* Text message */-> {
                    intent.putExtra("sms_body", message)
                    intent.data = Uri.parse("smsto:")
                } 1 /* Email */-> {
                    intent.data = Uri.parse("mailto:")
                    val subject = context.getString(R.string.shopping_list_navigation_item_name)
                    intent.putExtra(Intent.EXTRA_SUBJECT, subject)
                    intent.putExtra(Intent.EXTRA_TEXT, message)
                }
            }
            if (intent.resolveActivity(context.packageManager) != null)
                context.startActivity(intent)
            else {
                val snackbar = Snackbar.make(snackBarAnchor, R.string.export_error_message,
                                             Snackbar.LENGTH_SHORT)
                snackbar.anchorView = snackBarAnchor
                snackbar.show()
            }
        }.show()
}

/** Returns an AlertDialog.Builder that uses the current theme's alertDialogTheme.
 *
 *  AlertDialog seems to ignore the theme's alertDialogTheme value, making it
 *  necessary to pass it's value in manually to the AlertDialog.builder constructor.*/
fun themedAlertDialogBuilder(context: Context): AlertDialog.Builder {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.alertDialogTheme, typedValue, true)
    return AlertDialog.Builder(context, typedValue.data)
}

/** Returns a themed TextView for use as a custom title for an AlertDialog.
 *
 *  Changing the style of AlertDialog's default title seems to not work when
 *  done through styles (e.g. with the android:windowTitleStyle attribute).
 *  themedAlertDialogTitle returns a TextView with a padding, text size, text
 *  color, and background color suited for use as a custom title for an Alert-
 *  Dialog (using AlertDialog.setCustomTitle() or AlertDialog.Builder.setCus-
 *  tomTitle(). The text of the returned text view is not set by default. */
fun themedAlertDialogTitle(context: Context, title: String): TextView {
    val titleView = TextView(context)
    val dm = context.resources.displayMetrics
    titleView.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6f, dm)
    titleView.setPadding(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, dm).toInt())

    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.recyclerViewItemColor, typedValue, true)
    val titleBackgroundColor = typedValue.data
    titleView.setBackgroundColor(titleBackgroundColor)
    context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    titleView.setTextColor(typedValue.data)
    titleView.text = title
    return titleView
}