/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.annotation.CallSuper
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.model.NewItemDialogVisibilityState
import com.cliffracertech.bootycrate.model.database.DatabaseListItem
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.InventoryItemNameValidator
import com.cliffracertech.bootycrate.model.database.ItemDao
import com.cliffracertech.bootycrate.model.database.ItemGroup
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ListItemGroupValidator
import com.cliffracertech.bootycrate.model.database.ListItemNameValidator
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItemNameValidator
import com.cliffracertech.bootycrate.model.database.Validator
import com.cliffracertech.bootycrate.utils.collectAsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * An abstract [ViewModel] to provide state and callbacks for a dialog to add
 * new [ListItem] subclasses.
 *
 * The properties [itemColorIndex], [itemName], [itemExtraInfo], and [itemAmount]
 * should be updated with the proposed values for the new [ListItem]. The read-
 * only property [itemGroupId] describes the [ItemGroup.id] of the [ItemGroup]
 * that the new item will be added to. In the event that there is only, this
 * value will automatically change to the id of the only selected [ItemGroup].
 * Otherwise, the list of selected [ItemGroup]s provided by the property
 * [selectedItemGroups] should be displayed to the user, and clicks on a
 * selected [ItemGroup] should be connected to a [onItemGroupClick] call.
 *
 * The property [messages] will be equal to a list of [Validator.Message]s that
 * describe informational messages, warnings, or error messages regarding the
 * state of the new item dialog, while the property [confirmButtonsEnabled]
 * describes whether or not the add another and ok buttons should be enabled.
 * Clicks on these buttons should be connected to the callbacks [onAddAnotherClick]
 * and [onOkClick], respectively.
 *
 * Subclasses need to override [createItem] to return an instance of the [ListItem]
 * subclass T, and [onDismissRequest]. They might also override [resetFields]
 * to reset their properties that represent the state of a potential new item.
 */
abstract class NewItemDialogViewModel<T: ListItem>(
    private val nameValidator: ListItemNameValidator<T>,
    private val itemDao: ItemDao,
    itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?
) : ViewModel() {
    protected val scope = coroutineScope ?: viewModelScope
    private val itemGroupIdValidator = ListItemGroupValidator(itemGroupDao, scope)

    val itemGroupId by itemGroupIdValidator::value
    var itemColorIndex by mutableStateOf(0)
    var itemName by nameValidator::name
    var itemExtraInfo by nameValidator::extraInfo
    var itemAmount by mutableStateOf(1)

    val selectedItemGroups by
        itemGroupDao.getSelectedGroups()
            .map { it.toImmutableList() }
            .collectAsState(null, scope)

    fun onItemGroupClick(group: ItemGroup) {
        itemGroupIdValidator.value = group.id
    }

    val messages by combine(
            itemGroupIdValidator.message,
            nameValidator.message,
        ) { itemGroupIdMessage, nameMessage ->
            listOfNotNull(itemGroupIdMessage, nameMessage)
        }.map { it.toImmutableList() }
        .collectAsState(emptyList<Validator.Message>().toImmutableList(), scope)

    /** Return an instance of T given the values of [itemGroupId], [name],
     * [extraInfo], the members [itemColorIndex] and [itemAmount], as well as
     * any additional item state properties that subclasses add. Due to the
     * fact that the item's name, extra info, and [ItemGroup] id must be
     * validated, it is important to use the provided values rather than the
     * member properties of the same name (which might have changed to a
     * invalid value since they were last validated). */
    protected abstract suspend fun createItem(
        itemGroupId: Long,
        name: String,
        extraInfo: String
    ): DatabaseListItem

    private suspend fun addItem(): Boolean {
        val itemGroupId = itemGroupIdValidator.validate() ?: return false
        val itemNameAndExtraInfo = nameValidator.validate() ?: return false
        val item = createItem(
            itemGroupId,
            itemNameAndExtraInfo.first,
            itemNameAndExtraInfo.second)
        itemDao.add(item)
        return true
    }

    @CallSuper open fun resetFields() {
        nameValidator.clear()
        itemAmount = 1
        // itemGroupId and newItemColorIndex are intentionally not reset
        // so that users can add multiple items to the same group and/or
        // with the same color consecutively
    }

    /** Whether or not the ok and add another buttons of the dialog should be enabled. */
    val confirmButtonsEnabled by derivedStateOf {
        messages.find { it.isError } == null
    }

    fun onOkClick() {
        if (!confirmButtonsEnabled) return
        scope.launch {
            addItem()
            onDismissRequest()
        }
    }

    fun onAddAnotherClick() {
        if (!confirmButtonsEnabled) return
        scope.launch {
            addItem()
            resetFields()
        }
    }

    abstract fun onDismissRequest()
}

/** A view model to provide data for a dialog to add new [ShoppingListItem]s. */
@HiltViewModel
class NewShoppingListItemDialogViewModel(
    itemDao: ItemDao,
    itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?,
    private val visibilityState: NewItemDialogVisibilityState,
) : NewItemDialogViewModel<ShoppingListItem>(
    ShoppingListItemNameValidator(itemDao),
    itemDao, itemGroupDao, coroutineScope
) {
    @Inject constructor(
        itemDao: ItemDao,
        itemGroupDao: ItemGroupDao,
        visibilityState: NewItemDialogVisibilityState
    ) : this(itemDao, itemGroupDao, null, visibilityState)

    var itemIsChecked by mutableStateOf(false)

    override fun resetFields() {
        super.resetFields()
        itemIsChecked = false
    }

    override suspend fun createItem(
        itemGroupId: Long,
        name: String,
        extraInfo: String
    ) = ShoppingListItem(
        name = name,
        extraInfo = extraInfo,
        color = itemColorIndex,
        amount = itemAmount,
        isChecked = itemIsChecked
    ).toDbListItem(itemGroupId)

    override fun onDismissRequest() {
        visibilityState.showingNewShoppingListItemDialog = false
    }
}

/** A view model to provide data for a dialog to add new [InventoryItem]s. */
@HiltViewModel
class NewInventoryItemDialogViewModel(
    itemDao: ItemDao,
    itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?,
    private val visibilityState: NewItemDialogVisibilityState,
) : NewItemDialogViewModel<InventoryItem>(
    InventoryItemNameValidator(itemDao),
    itemDao, itemGroupDao, coroutineScope
) {
    @Inject constructor(
        itemDao: ItemDao,
        itemGroupDao: ItemGroupDao,
        visibilityState: NewItemDialogVisibilityState
    ) : this(itemDao, itemGroupDao, null, visibilityState)

    var itemAutoAddToShoppingList by mutableStateOf(false)
    var itemAutoAddToShoppingListAmount by mutableStateOf(1)

    override fun resetFields() {
        super.resetFields()
        itemAutoAddToShoppingList = false
        itemAutoAddToShoppingListAmount = 1
    }

    override suspend fun createItem(
        itemGroupId: Long,
        name: String,
        extraInfo: String
    ) = InventoryItem(
        name = name,
        extraInfo = extraInfo,
        color = itemColorIndex,
        amount = itemAmount,
        autoAddToShoppingList = itemAutoAddToShoppingList,
        autoAddToShoppingListAmount = itemAutoAddToShoppingListAmount
    ).toDbListItem(itemGroupId)

    override fun onDismissRequest() {
        visibilityState.showingNewInventoryItemDialog = false
    }
}