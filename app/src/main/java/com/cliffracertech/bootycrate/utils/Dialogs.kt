/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.cliffracertech.bootycrate.database.*
import com.cliffracertech.bootycrate.databinding.NewItemDialogBinding
import com.cliffracertech.bootycrate.utils.inputMethodManager
import com.cliffracertech.bootycrate.utils.resolveIntAttribute
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Open a dialog to display an about app screen. */
class AboutAppDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        themedAlertDialogBuilder(requireContext()).setView(R.layout.about_app_dialog).create()
}

/** Return a MaterialAlertDialogBuilder with the context theme's materialAlertDialogTheme style applied. */
fun themedAlertDialogBuilder(context: Context) = MaterialAlertDialogBuilder(
    context, context.theme.resolveIntAttribute(R.attr.materialAlertDialogTheme))

/**
 * An abstract DialogFragment to create a new BootyCrateItem.
 *
 * NewBootyCrateItemDialog is an abstract DialogFragment for creating new
 * BootyCrateItems. By default it fills the newItemViewContainer ui element
 * with a ExpandableSelectableItemView instance. If this needs to be overridden
 * in a subclass, call the constructor with the useDefaultLayoutParameter set
 * to false. If this is done, the subclass must initialize the newItemView
 * member before onCreateDialog, or an exception will be thrown.
 *
 * The abstract function createItemFromView must be overridden in subclasses
 * with an implementation that returns an T instance that reflects the
 * information entered in the newItemView member. The open function resetNewItemView
 * should be overridden in subclasses if additional work is needed to prepare the
 * newItemView member if the user clicks the addAnotherButton.
 *
 * The dialog will display a warning when the current name and extra info
 * combination is already used by another item. It will not prevent the user
 * from adding the item anyway if desired. It will also display an error
 * message and will prevent the user from proceeding if they try to add an item
 * with no name.
 */
abstract class NewBootyCrateItemDialog<T: BootyCrateItem>(
    context: Context,
    useDefaultLayout: Boolean = true
) : DialogFragment() {
    abstract val viewModel: BootyCrateViewModel<T>
    private val inputMethodManager = inputMethodManager(context)
    private val addAnotherButton: Button get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE)
    private val okButton: Button get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)

    protected val ui = NewItemDialogBinding.inflate(LayoutInflater.from(context))
    protected lateinit var newItemView: ExpandableSelectableItemView<T>

    init { if (useDefaultLayout) newItemView = ExpandableSelectableItemView(context) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel.resetNewItemName()
        ui.newItemViewContainer.addView(newItemView)
        newItemView.apply {
            setExpanded(true, animate = false)
            ui.amountEditSpacer.isVisible = false
            ui.checkBox.initColorIndex(0)
            setSelectedState(false, animate = false)
            ui.editButton.visibility = View.GONE
            ui.extraInfoEdit.doOnTextChanged { text, _, _, _ ->
                viewModel.newItemExtraInfo = text.toString()
            }
        }
        newItemView.ui.nameEdit.doOnTextChanged { text, _, _, _ ->
            viewModel.newItemName = text.toString()
            if (text?.isNotBlank() == true && ui.noNameError.isVisible) {
                ui.noNameError.isVisible = false
                addAnotherButton.isEnabled = true
                okButton.isEnabled = true
            }
        }
        viewModel.newItemNameIsAlreadyUsed.observe(this) { nameIsAlreadyUsed ->
            ui.duplicateNameWarning.isVisible = nameIsAlreadyUsed
        }
        return themedAlertDialogBuilder(requireContext())
            .setBackgroundInsetStart(0)
            .setBackgroundInsetEnd(0)
            .setTitle(R.string.add_item_button_name)
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .setNegativeButton(R.string.add_another_item_button_description) { _, _ -> }
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setView(ui.root)
            .create().apply {
                setOnShowListener {
                    okButton.setOnClickListener { if (addItem()) dismiss() }
                    // Override the add another button's default on click listener
                    // to prevent it from closing the dialog when clicked
                    addAnotherButton.setOnClickListener { if (addItem()) resetNewItemView() }
                    newItemView.ui.nameEdit.requestFocus()
                    inputMethodManager?.showSoftInput(newItemView.ui.nameEdit,
                                                      InputMethodManager.SHOW_IMPLICIT)
                }
            }
    }

    open fun resetNewItemView(): Unit = with(newItemView) {
        ui.nameEdit.text?.clear()
        ui.extraInfoEdit.text?.clear()
        ui.amountEdit.value = 1
        // We'll leave the color edit set to whichever color it was on previously,
        // in case the user wants to add items with like colors consecutively.
        ui.nameEdit.requestFocus()
        inputMethodManager?.showSoftInput(ui.nameEdit, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun addItem() =
        if (newItemView.ui.nameEdit.text?.isBlank() == true) {
            ui.noNameError.isVisible = true
            addAnotherButton.isEnabled = false
            okButton.isEnabled = false
            false
        } else {
            viewModel.add(createItemFromView())
            true
        }

    abstract fun createItemFromView(): T
}

/** Open a dialog to create a new shopping list item. */
class NewShoppingListItemDialog(context: Context) :
    NewBootyCrateItemDialog<ShoppingListItem>(context)
{
    override val viewModel: ShoppingListViewModel by activityViewModels()
    init { newItemView.ui.checkBox.setInColorEditMode(true, animate = false) }

    override fun createItemFromView() = ShoppingListItem(
        name = newItemView.ui.nameEdit.text.toString(),
        extraInfo = newItemView.ui.extraInfoEdit.text.toString(),
        color = newItemView.ui.checkBox.colorIndex,
        amount = newItemView.ui.amountEdit.value)
}

/** Open a dialog to create a new inventory item. */
class NewInventoryItemDialog(context: Context) :
    NewBootyCrateItemDialog<InventoryItem>(context, useDefaultLayout = false)
{
    override val viewModel: InventoryViewModel by activityViewModels()
    private val newInventoryItemView = InventoryItemView(context, null)

    init {
        newItemView = newInventoryItemView.apply {
            setExpanded(true, animate = false)
            detailsUi.autoAddToShoppingListAmountEdit.apply { value = minValue }
            detailsUi.autoAddToShoppingListCheckBox.initColorIndex(0)
            ui.checkBox.onColorChangedListener = {
                detailsUi.autoAddToShoppingListCheckBox.colorIndex =
                    ui.checkBox.colorIndex
            }
        }
    }

    override fun resetNewItemView() {
        super.resetNewItemView()
        newInventoryItemView.detailsUi.autoAddToShoppingListCheckBox.isChecked = false
        newInventoryItemView.detailsUi.autoAddToShoppingListAmountEdit.apply { value = minValue }
    }

    override fun createItemFromView() = InventoryItem(
        name = newItemView.ui.nameEdit.text.toString(),
        extraInfo = newItemView.ui.extraInfoEdit.text.toString(),
        color = newItemView.ui.checkBox.colorIndex,
        amount = newItemView.ui.amountEdit.value,
        autoAddToShoppingList = newInventoryItemView.detailsUi.autoAddToShoppingListCheckBox.isChecked,
        autoAddToShoppingListAmount = newInventoryItemView.detailsUi.autoAddToShoppingListAmountEdit.value)
}