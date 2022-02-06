/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.*
import com.cliffracertech.bootycrate.databinding.NewItemDialogBinding
import com.cliffracertech.bootycrate.recyclerview.ExpandableItemView
import com.cliffracertech.bootycrate.recyclerview.InventoryItemView
import com.cliffracertech.bootycrate.recyclerview.ItemGroupPicker
import com.cliffracertech.bootycrate.utils.SoftKeyboard
import com.cliffracertech.bootycrate.utils.repeatWhenStarted
import com.cliffracertech.bootycrate.viewmodel.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * An abstract DialogFragment to create a new ListItem.
 *
 * NewListItemDialog is an abstract DialogFragment for creating new ListItems.
 * By default it fills the newItemViewContainer ui element with a ExpandableSelectableItemView
 * instance. If this needs to be overridden in a subclass, call the constructor
 * with the useDefaultLayoutParameter set to false. If this is done, the
 * subclass must initialize the newItemView member before onCreateDialog, or an
 * exception will be thrown.
 *
 * The abstract function createItemFromView must be overridden in subclasses
 * with an implementation that returns a T instance that reflects the
 * information entered in the newItemView member. The open function resetNewItemView
 * should be overridden in subclasses if additional work is needed to prepare the
 * newItemView member if the user clicks the addAnotherButton.
 *
 * The dialog will display an error message and prevent the user from
 * proceeding if they try to add an item with no name. If the name and extra
 * info combination matches an item already in the collection, a warning
 * message will be displayed with text equal to the value of the abstract
 * property itemWithNameAlreadyExistsInCollectionWarningMessage. If the name
 * and extra info combination exists in a neighboring collection, a different
 * warning with text equal to the value of the abstract property
 * itemWithNameAlreadyExistsInOtherCollectionWarningMessage will be displayed
 * instead. These values must be overridden in subclasses with the message that
 * should be displayed in either case.
 */
abstract class NewListItemDialog<T: ListItem>(
    context: Context,
    useDefaultLayout: Boolean = true
) : DialogFragment() {
    protected abstract val viewModel: NewItemDialogViewModel<T>

    private val addAnotherButton: Button get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE)
    private val okButton: Button get() = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)

    protected var ui = NewItemDialogBinding.inflate(layoutInflater)
    protected lateinit var newItemView: ExpandableItemView<T>
    protected var targetGroupId: Long? = null
        private set

    abstract val itemWithNameAlreadyExistsInCollectionWarningMessage: String
    abstract val itemWithNameAlreadyExistsInOtherCollectionWarningMessage: String
    private val itemGroupPickerPrompt = context.getString(R.string.select_an_item_group_message)
    private val itemHasNoGroupIdErrorMessage = context.getString(R.string.new_item_has_no_item_group_error_message)

    init {
        if (useDefaultLayout)
            newItemView = ExpandableItemView(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel.newItemName = ""
        viewModel.newItemExtraInfo = ""
        ui.newItemViewContainer.addView(newItemView)
        newItemView.apply {
            setExpanded(true, animate = false)
            ui.checkBox.initColorIndex(0)
            ui.editButton.visibility = View.GONE
            ui.extraInfoEdit.doOnTextChanged { text, _, _, _ ->
                viewModel.newItemExtraInfo = text.toString()
            }
            updateContentDescriptions(getString(R.string.new_item_description))
        }
        newItemView.ui.nameEdit.doOnTextChanged { text, _, _, _ ->
            viewModel.newItemName = text.toString()
            if (shownWarningMessage == WarningMessage.ItemHasNoName && text?.isNotBlank() == true)
                clearWarningMessageAndEnableButtons()
        }

        return themedAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_button_description)
            .setNeutralButton(android.R.string.cancel) { _, _ -> }
            .setNegativeButton(R.string.add_another_item_button_description) { _, _ -> }
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setView(ui.root)
            .create().apply {
                setOnShowListener {
                    okButton.setOnClickListener { if (addItem()) dismiss() }
                    addAnotherButton.setOnClickListener { if (addItem()) resetNewItemView() }
                    // Showing the soft input seems not to work when done as
                    // a fragment is appearing. Showing the soft input after
                    // a small delay seems to be a workaround.
                    SoftKeyboard.showWithDelay(newItemView.ui.nameEdit)
                }
            }
    }

    // Hypothetically onCreateView should not need to be called because onCreateDialog is
    // overridden. Unfortunately we need to observe the viewModel's newItemNameIsAlreadyUsed
    // live data using the viewLifecycleOwner, which can only be done in or after
    // onViewCreated, which will not be called unless onCreateView returns a non-null View.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?) = ui.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.repeatWhenStarted {
            launch { viewModel.newItemNameIsAlreadyUsed.collect {
                showWarningMessage(when (it) {
                    NewItemDialogViewModel.NameIsAlreadyUsed.TrueForCurrentList ->
                        WarningMessage.NameAlreadyInUseInCollection
                    NewItemDialogViewModel.NameIsAlreadyUsed.TrueForSiblingList ->
                        WarningMessage.NameAlreadyInUseInOtherCollection
                    else ->//NewItemDialogViewModel.NameIsAlreadyUsed.False
                        WarningMessage.None
                })
            }}
            initSelectedItemGroupPicker()
        }
    }

    /** Initialize the targetGroupId field with the id of the only selected
     * item group if there is only one selected group, or show the item group
     * picker to the user otherwise to allow them to specify which item group
     * to add the item to.*/
    private fun initSelectedItemGroupPicker() {
        val selectedGroups = viewModel.selectedItemGroups
        if (selectedGroups.size == 1) {
            targetGroupId = selectedGroups.first().id
            return
        }
        val container = ui.itemGroupPickerStub.inflate()
        val itemGroupPicker = container.findViewById<ItemGroupPicker>(R.id.itemGroupPicker)
        if (itemGroupPicker != null) {
            itemGroupPicker.submitList(selectedGroups)
            itemGroupPicker.onChosenGroupIdChanged = {
                targetGroupId = it
                if (it != null && shownWarningMessage == WarningMessage.ItemHasNoGroupId)
                    clearWarningMessageAndEnableButtons()
            }
        }
        val message = container.findViewById<TextView>(R.id.itemGroupPickerPrompt)
        message?.text = itemGroupPickerPrompt
    }

    open fun resetNewItemView(): Unit = with(newItemView) {
        ui.nameEdit.text?.clear()
        ui.extraInfoEdit.text?.clear()
        ui.amountEdit.value = 1
        // We'll leave the color edit set to whichever color it was on previously,
        // in case the user wants to add items with like colors consecutively.
        ui.nameEdit.requestFocus()
        SoftKeyboard.show(ui.nameEdit)
    }

    private fun addItem(): Boolean {
        val groupId = targetGroupId
        return when {
            newItemView.ui.nameEdit.text?.isBlank() == true -> {
                showWarningMessage(WarningMessage.ItemHasNoName)
                addAnotherButton.isEnabled = false
                okButton.isEnabled = false
                false
            } groupId == null -> {
                showWarningMessage(WarningMessage.ItemHasNoGroupId)
                addAnotherButton.isEnabled = false
                okButton.isEnabled = false
                SoftKeyboard.hide(ui.root)
                false
            } else -> {
                viewModel.onAddItemRequest(createItemFromView(), groupId)
                true
            }
        }
    }

    abstract fun createItemFromView(): T

    enum class WarningMessage {
        NameAlreadyInUseInCollection,
        NameAlreadyInUseInOtherCollection,
        ItemHasNoName,
        ItemHasNoGroupId,
        None
    }

    private var _shownWarningMessage = WarningMessage.None
    protected val shownWarningMessage get() = _shownWarningMessage
    private fun showWarningMessage(warning: WarningMessage) {
        if (_shownWarningMessage == warning) return
        _shownWarningMessage = warning

        if (warning == WarningMessage.None)
            ui.warningMessage.visibility = View.GONE
        else {
            val context = this.context ?: return

            ui.warningMessage.text = when (warning) {
                WarningMessage.NameAlreadyInUseInCollection ->
                    itemWithNameAlreadyExistsInCollectionWarningMessage
                WarningMessage.NameAlreadyInUseInOtherCollection ->
                    itemWithNameAlreadyExistsInOtherCollectionWarningMessage
                WarningMessage.ItemHasNoGroupId ->
                    itemHasNoGroupIdErrorMessage
                else -> context.getString(R.string.new_item_no_name_error)
            }
            val iconResId = if (warning == WarningMessage.ItemHasNoName ||
                warning == WarningMessage.ItemHasNoGroupId)
                R.drawable.ic_baseline_error_24
            else R.drawable.ic_round_warning_24
            val icon = ContextCompat.getDrawable(context, iconResId)
            ui.warningMessage.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)

            ui.warningMessage.visibility = View.VISIBLE
        }
    }

    private fun clearWarningMessageAndEnableButtons() {
        showWarningMessage(WarningMessage.None)
        addAnotherButton.isEnabled = true
        okButton.isEnabled = true
    }
}



/** Open a dialog to create a new shopping list item. */
class NewShoppingListItemDialog(context: Context) :
    NewListItemDialog<ShoppingListItem>(context)
{
    override val viewModel: NewShoppingListItemDialogViewModel by activityViewModels()

    override val itemWithNameAlreadyExistsInCollectionWarningMessage =
        context.getString(R.string.new_shopping_list_item_duplicate_name_warning)
    override val itemWithNameAlreadyExistsInOtherCollectionWarningMessage =
        context.getString(R.string.new_shopping_list_item_will_not_be_linked_warning,
                          context.getString(R.string.add_to_shopping_list_description))

    init { newItemView.ui.checkBox.setInColorEditMode(true, animate = false) }

    override fun createItemFromView() = ShoppingListItem(
        name = newItemView.ui.nameEdit.text.toString(),
        extraInfo = newItemView.ui.extraInfoEdit.text.toString(),
        color = newItemView.ui.checkBox.colorIndex,
        amount = newItemView.ui.amountEdit.value)
}



/** Open a dialog to create a new inventory item. */
class NewInventoryItemDialog(context: Context) :
    NewListItemDialog<InventoryItem>(context, useDefaultLayout = false)
{
    override val viewModel: NewInventoryItemDialogViewModel by activityViewModels()
    private val newInventoryItemView = InventoryItemView(context, null)

    override val itemWithNameAlreadyExistsInCollectionWarningMessage =
        context.getString(R.string.new_inventory_item_duplicate_name_warning)
    override val itemWithNameAlreadyExistsInOtherCollectionWarningMessage =
        context.getString(R.string.new_inventory_item_will_not_be_linked_warning,
                          context.getString(R.string.add_to_inventory_description))

    init {
        newItemView = newInventoryItemView.apply {
            detailsUi.autoAddToShoppingListAmountEdit.apply { value = minValue }
            detailsUi.autoAddToShoppingListCheckBox.initColorIndex(0)
            ui.checkBox.onColorIndexChangedListener = {
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

