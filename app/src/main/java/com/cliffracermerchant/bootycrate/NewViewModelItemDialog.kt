/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Transformations
import com.cliffracermerchant.bootycrate.databinding.NewItemDialogBinding
import com.cliffracermerchant.bootycrate.databinding.ViewModelItemBinding

//abstract class NewViewModelItemDialog<Entity: ExpandableSelectableItem>(
//    context: Context,
//    private val viewModel: ViewModel<Entity>
//) : DialogFragment() {
//    protected val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
//    protected val cancelButton get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
//    protected val addAnotherButton get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE)
//    protected val okButton get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
//    protected val ui = NewItemDialogBinding.inflate(LayoutInflater.from(context))
//    protected open val newItemViewLayoutResId = R.layout.view_model_item
//    protected open val newItemView: ExpandableSelectableItemView<Entity>
//    protected open val newItemUi = ViewmodelItemBinding.bind(ui.root)
//
//    init {
//        ui.newItemViewStub.layoutResource = newItemViewLayoutResId
//        newItemView = ui.newItemViewStub.inflate() as ExpandableSelectableItemView<Entity>
//    }
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
////        ui.newItemView.apply {
////            background = null
////            newItemUi.editButton.visibility = View.GONE
////            setColorIndex(0, animate = false)
////            ui.extraInfoEdit.doOnTextChanged { text, _, _, _ ->
////                viewModel.newItemExtraInfo = text.toString()
////            }
////            requestLayout()
////        }
////        ui.newItemView.ui.nameEdit.doOnTextChanged { text, _, _, _ ->
////            viewModel.newItemName = text.toString()
////            if (text?.isNotBlank() == true &&
////                ui.noNameError.visibility == View.VISIBLE)
////            {
////                ui.noNameError.visibility = View.INVISIBLE
////                addAnotherButton.isEnabled = true
////                okButton.isEnabled = true
////            }
////        }
//        Transformations.distinctUntilChanged(viewModel.newItemNameIsAlreadyUsed).observe(this) { nameIsAlreadyUsed ->
//            ui.duplicateNameWarning.visibility = if (nameIsAlreadyUsed) View.VISIBLE
//            else                   View.INVISIBLE
//        }
//        return Dialog.themedAlertBuilder()
//            .setBackgroundInsetStart(0)
//            .setBackgroundInsetEnd(0)
//            .setTitle(R.string.add_item_button_name)
//            .setNeutralButton(android.R.string.cancel) { _, _ -> }
//            .setNegativeButton(R.string.add_another_item_button_description) { _, _ -> }
//            .setPositiveButton(android.R.string.ok) { _, _ -> }
//            .setView(ui.root)
//            .create().apply {
//                setOnDismissListener { viewModel.resetNewItemName() }
////                setOnShowListener {
////                    okButton.setOnClickListener { if (addItem()) dismiss() }
////                    // Override the add another button's default on click listener
////                    // to prevent it from closing the dialog when clicked
////                    addAnotherButton.setOnClickListener {
////                        if (addItem()) ui.newItemView.apply {
////                            ui.nameEdit.text?.clear()
////                            ui.extraInfoEdit.text?.clear()
////                            ui.amountEdit.apply { value = minValue }
////                            detailsUi.addToShoppingListCheckBox.isChecked = false
////                            detailsUi.addToShoppingListTriggerEdit.apply { value = minValue }
////                            // We'll leave the color edit set to whichever color it was on previously,
////                            // in case the user wants to add items with like colors consecutively.
////                            ui.nameEdit.requestFocus()
////                            imm?.showSoftInput(ui.nameEdit, InputMethodManager.SHOW_IMPLICIT)
////                        }
////                    }
////                    ui.newItemView.ui.nameEdit.requestFocus()
////                    imm?.showSoftInput(ui.newItemView.ui.nameEdit, InputMethodManager.SHOW_IMPLICIT)
////                }
//            }
//    }
//
//    private fun addItem() =
//
//        if (newItemView.ui.nameEdit.text?.isBlank() == true) {
//            ui.noNameError.visibility = View.VISIBLE
//            (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
//            (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
//            false
//        } else {
//            viewModel.add(createItemFromView())
//            true
//        }
//
//    abstract fun createItemFromView(): Entity
//}
//
///** Open a dialog to create a new shopping list item. */
//class NewShoppingListItemDialog(context: Context, viewModel: ShoppingListViewModel) :
//    NewViewModelItemDialog<ShoppingListItem>(context, viewModel)
//{
//    init { newItemView.apply {
//        //detailsUi.inventoryItemDetailsGroup.visibility = View.GONE
//        ui.nameEdit.setEditable(true, animate = false)
//        ui.extraInfoEdit.setEditable(true, animate = false)
//        ui.amountEdit.minValue = 1
//    }}
//
//    override fun createItemFromView() = ShoppingListItem(
//        name = newItemView.ui.nameEdit.text.toString(),
//        extraInfo = newItemView.ui.extraInfoEdit.text.toString(),
//        color = newItemView.colorIndex,
//        amount = newItemView.ui.amountEdit.value)
//}
//
///** Open a dialog to create a new inventory item. */
//class NewInventoryItemDialog(context: Context, viewModel: InventoryViewModel) :
//    NewViewModelItemDialog<InventoryItem>(context, viewModel)
//{
//    init { newItemView.apply {
//        expand()
//       //detailsUi.addToShoppingListTriggerEdit.apply { value = minValue }
//    }}
//
//    override fun createItemFromView() = InventoryItem(
//        name = newItemView.ui.nameEdit.text.toString(),
//        extraInfo = newItemView.ui.extraInfoEdit.text.toString(),
//        color = newItemView.colorIndex,
//        amount = newItemView.ui.amountEdit.value,
//        addToShoppingList = false,//newItemView.detailsUi.addToShoppingListCheckBox.isChecked,
//        addToShoppingListTrigger = 0)//newItemView.detailsUi.addToShoppingListTriggerEdit.value)
//}