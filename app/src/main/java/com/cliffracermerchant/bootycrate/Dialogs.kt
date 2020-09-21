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
import androidx.core.view.isVisible
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
    builder.setPositiveButton(android.R.string.ok) { _, _ -> callback(recyclerView.selectedItem) }
    builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
    builder.setView(recyclerView)
    builder.show()
}

fun newShoppingListItemDialog(
    context: Context,
    fragmentManager: FragmentManager,
    callback: (ShoppingListItem) -> Unit
) {
    val builder = themedAlertDialogBuilder(context)
    builder.setTitle(context.getString(R.string.add_item_button_name))
    val itemView = InventoryItemView(context)
    itemView.background = null
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
    itemView.editButton.isVisible = false
    itemView.collapseButton.isVisible = false
    itemView.nameEdit.isEditable = true
    itemView.extraInfoEdit.isEditable = true
    itemView.inventoryAmountEdit.isEditable = true
    itemView.inventoryAmountEdit.minValue = 1
    builder.setPositiveButton(android.R.string.ok) { _, _ ->
        callback(ShoppingListItem(name = itemView.nameEdit.text.toString(),
                                  extraInfo = itemView.extraInfoEdit.text.toString(),
                                  color = colorIndex,
                                  amount = itemView.inventoryAmountEdit.currentValue))
    }
    builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
    builder.setView(itemView)
    val dialog = builder.create()
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
    val builder = themedAlertDialogBuilder(context)
    builder.setTitle(context.getString(R.string.add_item_button_name))
    val itemView = InventoryItemView(context)
    itemView.background = null
    itemView.collapseButton.visibility = View.GONE
    val colorEdit = itemView.colorEdit.drawable
    var colorIndex = 0
    colorEdit.setTint(ViewModelItem.Colors[colorIndex])
    itemView.expand(animate = false)
    itemView.colorEdit.setOnClickListener {
        colorPickerDialog(fragmentManager, ViewModelItem.Colors[0]) { chosenColorIndex ->
            ObjectAnimator.ofArgb(colorEdit, "tint", ViewModelItem.Colors[colorIndex],
                                  ViewModelItem.Colors[chosenColorIndex]).start()
            colorIndex = chosenColorIndex
        }
    }
    itemView.editButton.isVisible = false
    itemView.collapseButton.isVisible = false

    builder.setPositiveButton(android.R.string.ok) { _, _ ->
        callback(InventoryItem(name = itemView.nameEdit.text.toString(),
                               extraInfo = itemView.extraInfoEdit.text.toString(),
                               color = colorIndex,
                               amount = itemView.inventoryAmountEdit.currentValue,
                               addToShoppingList = itemView.addToShoppingListCheckBox.isChecked,
                               addToShoppingListTrigger = itemView.addToShoppingListTriggerEdit.currentValue))
    }
    builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
    builder.setView(itemView)
    val dialog = builder.create()
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
    val dialogBuilder = themedAlertDialogBuilder(context)
    dialogBuilder.setTitle(R.string.export_dialog_title)
    dialogBuilder.setItems(R.array.export_options) { _, chosenOption ->
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            //
            type = "HTTP.PLAIN_TEXT_TYPE"
        }
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
    }
    val title = themedAlertDialogTitle(context)
    title.text = context.getString(R.string.export_dialog_title)
    dialogBuilder.setCustomTitle(title)
    dialogBuilder.show()
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
fun themedAlertDialogTitle(context: Context): TextView {
    val title = TextView(context)
    val dm = context.resources.displayMetrics
    title.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6f, dm)
    title.setPadding(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, dm).toInt())

    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.recyclerViewItemColor, typedValue, true)
    val titleBackgroundColor = typedValue.data
    title.setBackgroundColor(titleBackgroundColor)
    context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    title.setTextColor(typedValue.data)

    return title
}