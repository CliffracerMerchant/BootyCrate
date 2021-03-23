/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

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
import com.cliffracermerchant.bootycrate.databinding.NewItemDialogBinding

/**
 * An abstract DialogFragment to create a new ViewModelItem.
 *
 * NewViewModelItemDialog is an abstract DialogFragment for creating new View-
 * ModelItems. By default it fills the newItemViewContainer ui element with
 * a ExpandableSelectableItemView instance. If this needs to be overridden in
 * a subclass, call the constructor with the useDefaultLayoutParameter set to
 * false. If this is done, the subclass must initialize the newItemView member
 * before onCreateDialog, or an exception will be thrown.
 *
 * The abstract function createItemFromView must be overridden in subclasses
 * with an implementation that returns an Entity instance that reflects the
 * information entered in the newItemView member. The open function resetNew-
 * ItemView should be overridden in subclasses if additional work is needed
 * to prepare the newItemView member if the user clicks the addAnotherButton.
 *
 * The dialog will display a warning when the current name and extra info
 * combination is already used by another item. It will not prevent the user
 * from adding the item anyway if desired. It will also display an error mes-
 * sage and will prevent the user from proceeding if they try to add an item
 * with no name.
 */
abstract class NewViewModelItemDialog<Entity: ExpandableSelectableItem>(
    context: Context,
    useDefaultLayout: Boolean = true
) : DialogFragment() {
    abstract val viewModel: ViewModel<Entity>
    private val inputMethodManager = inputMethodManager(context)
    private val cancelButton get(): Button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
    private val addAnotherButton: Button get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE)
    private val okButton: Button get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)

    protected val ui = NewItemDialogBinding.inflate(LayoutInflater.from(context))
    protected lateinit var newItemView: ExpandableSelectableItemView<Entity>

    init { if (useDefaultLayout) newItemView = ExpandableSelectableItemView(context) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel.resetNewItemName()
        ui.newItemViewContainer.addView(newItemView)
        newItemView.apply {
            setExpanded(true, animate = false)
            ui.amountEditSpacer.isVisible = false
            ui.checkBox.setColorIndex(0, animate = false)
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

    abstract fun createItemFromView(): Entity
}

/** Open a dialog to create a new shopping list item. */
class NewShoppingListItemDialog(context: Context) :
    NewViewModelItemDialog<ShoppingListItem>(context)
{
    override val viewModel: ShoppingListViewModel by activityViewModels()
    init { newItemView.ui.checkBox.inColorEditMode = true }

    override fun createItemFromView() = ShoppingListItem(
        name = newItemView.ui.nameEdit.text.toString(),
        extraInfo = newItemView.ui.extraInfoEdit.text.toString(),
        color = newItemView.ui.checkBox.colorIndex,
        amount = newItemView.ui.amountEdit.value)
}

/** Open a dialog to create a new inventory item. */
class NewInventoryItemDialog(context: Context) :
    NewViewModelItemDialog<InventoryItem>(context, useDefaultLayout = false)
{
    override val viewModel: InventoryViewModel by activityViewModels()
    private val newInventoryItemView = InventoryItemView(context, null)

    init {
        newItemView = newInventoryItemView
        newInventoryItemView.setExpanded(true, animate = false)
        newInventoryItemView.detailsUi.addToShoppingListTriggerEdit.apply { value = minValue }
    }

    override fun resetNewItemView() {
        super.resetNewItemView()
        newInventoryItemView.detailsUi.addToShoppingListCheckBox.isChecked = false
        newInventoryItemView.detailsUi.addToShoppingListTriggerEdit.apply { value = minValue }
    }

    override fun createItemFromView() = InventoryItem(
        name = newItemView.ui.nameEdit.text.toString(),
        extraInfo = newItemView.ui.extraInfoEdit.text.toString(),
        color = newItemView.ui.checkBox.colorIndex,
        amount = newItemView.ui.amountEdit.value,
        addToShoppingList = newInventoryItemView.detailsUi.addToShoppingListCheckBox.isChecked,
        addToShoppingListTrigger = newInventoryItemView.detailsUi.addToShoppingListTriggerEdit.value)
}