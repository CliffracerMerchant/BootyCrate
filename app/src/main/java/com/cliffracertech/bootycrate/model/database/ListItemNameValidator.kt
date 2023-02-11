/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import androidx.annotation.CallSuper
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.StringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * An abstract value validator.
 *
 * Validator can be used to validate generic non-null values and return
 * messages explaining why the value is not valid if necessary. The initial
 * value of the property [proposedValue] will be equal to [initialValue], and
 * can be set using [setProposedValue]. The property [proposedValueHasBeenChanged]
 * is visible to sub-classes, and can be used in case different error messages
 * are desired before or after [proposedValue] has been changed at least once.
 * For example, a [String] property would mostly likely be initialized to a
 * blank [String]. If blank [String]s are invalid, [proposedValueHasBeenChanged]
 * can be utilized to only show a 'value must not be blank' error message after
 * it has been changed at least once. This would prevent the message from being
 * immediately displayed before the user has had a chance to change the value.
 *
 * The abstract suspend function [messageFor] must be overridden in subclasses
 * to return a [StringResource] that becomes the message explaining why the
 * current value is not valid when resolved, or null if the name is valid. The
 * [Flow]`<StringResource?>` property [message] can be collected to obtain the
 * current message given the current value of [proposedValue].
 *
 * When naming is finished, the suspend function [validate] should be called.
 * The provided value will be checked one last time, and then whether or not
 * the value is valid will be returned. [validate] also resets [proposedValue]
 * to [initialValue] if it returns true so that the validator can be reused.
 * Assuming that the most recent [proposedValue] is valid because [message]'s
 * current value is null is unsafe because [message] might not have had time
 * to update to a non-null message after [proposedValue] is changed to an
 * invalid value.
 */
abstract class Validator <T>(private val initialValue: T) {
    private val _proposedValue = MutableStateFlow(initialValue)
    protected var proposedValueHasBeenChanged = false
        private set
    val proposedValue get() = _proposedValue.value

    fun setProposedValue(value: T) {
        _proposedValue.value = value
        proposedValueHasBeenChanged = true
    }

    fun clear() {
        _proposedValue.value = initialValue
        proposedValueHasBeenChanged = false
    }

    protected abstract suspend fun messageFor(proposedValue: T): StringResource?

    val message = _proposedValue.map(::messageFor)

    suspend fun validate(): Boolean {
        val message = messageFor(proposedValue)
        val isValid = message == null
        if (isValid) clear()
        return isValid
    }
}

abstract class ListItemNameValidator<T: ListItem>(
    private val itemDao: ItemDao
): Validator<Pair<String, String>>("" to "") {

    fun setProposedName(name: String) =
        setProposedValue(name to proposedValue.second)
    fun setProposedExtraInfo(extraInfo: String) =
        setProposedValue(proposedValue.first to extraInfo)

    protected suspend fun nameAlreadyUsedInShoppingList(value: Pair<String, String>) =
        itemDao.nameAlreadyUsedInShoppingList(value.first, value.second)
    protected suspend fun nameAlreadyUsedInInventory(value: Pair<String, String>) =
        itemDao.nameAlreadyUsedInInventory(value.first, value.second)

    @CallSuper override suspend fun messageFor(
        proposedValue: Pair<String, String>
    ): StringResource? = when {
        !proposedValueHasBeenChanged -> null
        proposedValue.first.isEmpty() ->
            StringResource(R.string.new_item_no_name_error)
        else -> null
    }
}

class ShoppingListItemNameValidator(itemDao: ItemDao) :
    ListItemNameValidator<ShoppingListItem>(itemDao)
{
    override suspend fun messageFor(
        proposedValue: Pair<String, String>
    ) = super.messageFor(proposedValue) ?: when {
        nameAlreadyUsedInShoppingList(proposedValue) ->
            StringResource(R.string.new_shopping_list_item_duplicate_name_warning)
        nameAlreadyUsedInInventory(proposedValue) ->
            StringResource(R.string.new_shopping_list_item_will_not_be_linked_warning)
        else -> null
    }
}

class InventoryItemNameValidator(itemDao: ItemDao) :
    ListItemNameValidator<InventoryItem>(itemDao)
{
    override suspend fun messageFor(
        proposedValue: Pair<String, String>
    ) = super.messageFor(proposedValue) ?: when {
        nameAlreadyUsedInShoppingList(proposedValue) ->
            StringResource(R.string.new_inventory_item_duplicate_name_warning)
        nameAlreadyUsedInInventory(proposedValue) ->
            StringResource(R.string.new_inventory_item_will_not_be_linked_warning)
        else -> null
    }
}