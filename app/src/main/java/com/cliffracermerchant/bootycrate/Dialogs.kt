/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

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
import kotlinx.android.synthetic.main.inventory_item_layout.view.*

object Dialog {
    private lateinit var fragmentManager: FragmentManager
    fun initFragmentManager(instance: FragmentManager) { fragmentManager = instance }

    /** Displays a color picker dialog to choose from one of ViewModelItem's twelve
     *  colors. Note that the initial color parameter and the return value are the
     *  indices of the chosen color, not the Android color value for the color. */
    fun colorPicker(initialColorIndex: Int = 0, callback: (Int) -> Unit) {
        val initialColor = ViewModelItem.Colors[initialColorIndex.coerceIn(ViewModelItem.Colors.indices)]
        val colorPicker = ColorSheet().colorPicker(ViewModelItem.Colors, initialColor,
                                                   noColorOption = false) { color ->
            val colorIndex = ViewModelItem.Colors.indexOf(color)
            callback(if (colorIndex != -1) colorIndex else 0)
        }
        colorPicker.show(fragmentManager)
    }

    fun newShoppingListItem(context: Context, callback: (ShoppingListItem) -> Unit) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        val itemView = InventoryItemView(context)
        itemView.apply {
            background = null
            editButton.visibility = View.GONE
            inventoryItemDetailsGroup.visibility = View.GONE
            setColorIndex(0, animate = false)
            colorEdit.setOnClickListener {
                colorPicker(ViewModelItem.Colors[0]) { chosenColorIndex ->
                    itemView.colorIndex = chosenColorIndex
                }
            }
            nameEdit.isEditable = true
            extraInfoEdit.isEditable = true
            inventoryAmountEdit.valueIsDirectlyEditable = true
            inventoryAmountEdit.minValue = 1
        }
        val dialog = themedAlertBuilder(context).
            setTitle(context.getString(R.string.add_item_button_name)).
            setNeutralButton(android.R.string.cancel) { _, _ -> }.
            setNegativeButton(context.getString(R.string.add_another_item_button_description)) { _, _ ->}.
            setPositiveButton(android.R.string.ok) { _, _ ->
                callback(shoppingListItemFromItemView(itemView))
            }.setView(itemView).create()
        dialog.apply {
            // The dialog dimming is disabled here to prevent
            // flickering if the color sheet dialog is opened on top.
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setOnShowListener {
                // Override the add another button's default on click listener
                // to prevent it from closing the dialog when clicked
                getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                    callback(shoppingListItemFromItemView(itemView))
                    itemView.apply {
                        nameEdit.text?.clear()
                        extraInfoEdit.text?.clear()
                        inventoryAmountEdit.apply { value = minValue }
                        // We'll leave the color edit set to whichever color it was on previously,
                        // in case the user wants to items with like colors consecutively.
                        nameEdit.requestFocus()
                        imm?.showSoftInput(nameEdit, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
                itemView.nameEdit.requestFocus()
                imm?.showSoftInput(itemView.nameEdit, InputMethodManager.SHOW_IMPLICIT)
            }
            dialog.show()
        }
    }

    fun newInventoryItem(context: Context, callback: (InventoryItem) -> Unit) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        val itemView = InventoryItemView(context)
        itemView.apply {
            setSelectedState(selected = false, animate = false)
            background = null
            expand()
            editButton.visibility = View.GONE
            // Setting collapseButton's visibility to GONE or INVISIBLE doesn't seem to work for some reason
            collapseButton.alpha = 0f
            collapseButton.setOnClickListener {}
            addToShoppingListTriggerEdit.value = itemView.addToShoppingListTriggerEdit.minValue
            setColorIndex(0, animate = false)
            colorEdit.setOnClickListener {
                colorPicker(ViewModelItem.Colors[0]) { chosenColorIndex ->
                    itemView.colorIndex = chosenColorIndex
                }
            }
        }
        val dialog = themedAlertBuilder(context).
            setTitle(context.getString(R.string.add_item_button_name)).
            setNeutralButton(android.R.string.cancel) { _, _ -> }.
            setNegativeButton(R.string.add_another_item_button_description) { _, _ ->}.
            setPositiveButton(android.R.string.ok) { _, _ ->
                callback(inventoryItemFromItemView(itemView))
            }.setView(itemView).create()
        dialog.apply {
            // The dialog dimming is disabled here to prevent
            // flickering if the color sheet dialog is opened on top.
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            setOnShowListener {
                // Override the add another button's default on click listener
                // to prevent it from closing the dialog when clicked
                getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                    callback(inventoryItemFromItemView(itemView))
                    itemView.apply {
                        nameEdit.text?.clear()
                        extraInfoEdit.text?.clear()
                        inventoryAmountEdit.apply { value = minValue }
                        addToShoppingListCheckBox.isChecked = false
                        addToShoppingListTriggerEdit.apply { value = minValue }
                        // We'll leave the color edit set to whichever color it was on previously,
                        // in case the user wants to items with like colors consecutively.
                        nameEdit.requestFocus()
                        imm?.showSoftInput(nameEdit, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
                itemView.nameEdit.requestFocus()
                imm?.showSoftInput(itemView.nameEdit, InputMethodManager.SHOW_IMPLICIT)
            }
            show()
        }
    }

    fun <Entity: ViewModelItem>exportAs(
        context: Context,
        items: List<Entity>,
        insertBlankLineBetweenColors: Boolean,
        snackBarAnchor: View
    ) {
        val titleText = context.getString(R.string.export_dialog_title)
        val titleView = themedAlertTitle(context, titleText)

        themedAlertBuilder(context).
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
    fun themedAlertBuilder(context: Context): AlertDialog.Builder {
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
    fun themedAlertTitle(context: Context, title: String): TextView {
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
}