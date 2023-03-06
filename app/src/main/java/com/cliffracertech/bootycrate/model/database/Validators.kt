/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model.database

import androidx.annotation.CallSuper
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.StringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * An abstract value validator.
 *
 * Validator can be used to validate generic non-null values and return
 * messages explaining why the value is not valid if necessary. The initial
 * value of the mutable property [value] will be equal to [initialValue]. The
 * property [valueHasBeenChanged] is visible to sub-classes, and can be used
 * in case different error messages are desired before or after [value] has
 * been changed at least once. For example, a [String] property would mostly
 * likely be initialized to a blank [String]. If blank [String]s are invalid,
 * [valueHasBeenChanged] can be utilized to only show a 'value must not be blank'
 * error message after it has been changed at least once. This would prevent
 * the message from being immediately displayed before the user has had a
 * chance to change the value.
 *
 * The suspend function [messageFor] must be overridden in subclasses to return
 * a [StringResource] that becomes the message explaining why the current value
 * is not valid when resolved, or null if the name is valid. The Flow<[Message]?>
 * property [message] can be collected to obtain the current message given the
 * current value of [value].
 *
 * Because [message] may not have updated after a recent change to [value], and
 * because [value] might change in another thread after being validated, the
 * suspend function [validate] should always be called to ensure that a given
 * value is valid. The current [value] will be validated, and then either the
 * validated value or null if the value was invalid will be returned. [validate]
 * also resets [value] to [initialValue] if it does not return null so that the
 * validator can be reused.
 */
abstract class Validator <T>(private val initialValue: T) {
    private val valueFlow = MutableStateFlow(initialValue)
    protected var valueHasBeenChanged = false
        private set

    var value
        get() = valueFlow.value
        set(value) {
            valueFlow.value = value
            valueHasBeenChanged = true
        }

    fun clear() {
        value = initialValue
        valueHasBeenChanged = false
    }

    /** Message's subclasses [Information], [Warning], and [Error] provide
     * information about a proposed value for a value being validated. */
    sealed class Message(val stringResource: StringResource) {
        /** An informational message that does not indicate
         * that there is a problem with the proposed value. */
        class Information(stringResource: StringResource): Message(stringResource)
        /** A message that warns about a potential problem with the
         * proposed value. It is left up to the user to heed or ignore. */
        class Warning(stringResource: StringResource): Message(stringResource)
        /** A message that describes a critical error with the proposed
         * value that requires the value to be changed before proceeding. */
        class Error(stringResource: StringResource): Message(stringResource)

        val isInformational get() = this is Information
        val isWarning get() = this is Warning
        val isError get() = this is Error
    }

    protected abstract suspend fun messageFor(value: T): Message?

    val message = valueFlow.map(::messageFor)

    suspend fun validate(): T? {
        val value = this.value
        // Although value might not have been changed yet, we set
        // valueHasBeenChanged to true so that subclasses that don't
        // provide an error message for an invalid initial value do
        // provide an error message here
        valueHasBeenChanged = true
        val isValid = messageFor(value)?.isError != true
        return if (isValid) value else null
    }
}

/**
 * A [Validator] that validates the name and extra info combination for a new
 * ListItem subclass. For ease of use, consider using the properties [name] and
 * [extraInfo] should be used instead of [Validator.value]. The inner values of
 * the Pair<String, String> returned by validate represents the validated name
 * and extraInfo, respectively, for the [ListItem].
 */
abstract class ListItemNameValidator<T: ListItem>(
    private val dao: ItemDao
): Validator<Pair<String, String>>("" to "") {

    var name
        get() = value.first
        set(newName) { value = newName to extraInfo }
    var extraInfo
        get() = value.second
        set(newExtraInfo) { value = name to newExtraInfo }

    protected suspend fun nameAlreadyUsedInShoppingList(value: Pair<String, String>) =
        dao.nameAlreadyUsedInShoppingList(value.first, value.second)
    protected suspend fun nameAlreadyUsedInInventory(value: Pair<String, String>) =
        dao.nameAlreadyUsedInInventory(value.first, value.second)

    private val itemHasNoNameErrorMessage = Message.Error(
        StringResource(R.string.new_item_no_name_error))

    @CallSuper override suspend fun messageFor(
        value: Pair<String, String>,
    ): Message? = when {
        !valueHasBeenChanged ->  null
        value.first.isEmpty() -> itemHasNoNameErrorMessage
        else ->                  null
    }
}

class ShoppingListItemNameValidator(dao: ItemDao) :
    ListItemNameValidator<ShoppingListItem>(dao)
{
    private val nameAlreadyUsedMessage = Message.Warning(
        StringResource(R.string.new_shopping_list_item_duplicate_name_warning))
    private val nameAlreadyUsedInOtherListMessage = Message.Warning(
        StringResource(R.string.new_shopping_list_item_duplicate_name_warning))

    override suspend fun messageFor(
        value: Pair<String, String>
    ) = super.messageFor(value) ?: when {
        nameAlreadyUsedInShoppingList(value) -> nameAlreadyUsedMessage
        nameAlreadyUsedInInventory(value) ->    nameAlreadyUsedInOtherListMessage
        else ->                                 null
    }
}

class InventoryItemNameValidator(dao: ItemDao) :
    ListItemNameValidator<InventoryItem>(dao)
{
    private val nameAlreadyUsedMessage = Message.Warning(
        StringResource(R.string.new_inventory_item_duplicate_name_warning))
    private val nameAlreadyUsedInOtherListMessage = Message.Warning(
        StringResource(R.string.new_inventory_item_duplicate_name_warning))

    override suspend fun messageFor(
        value: Pair<String, String>
    ) = super.messageFor(value) ?: when {
        nameAlreadyUsedInInventory(value) ->    nameAlreadyUsedMessage
        nameAlreadyUsedInShoppingList(value) -> nameAlreadyUsedInOtherListMessage
        else ->                                 null
    }
}

/**
 * A [Validator] that checks that the proposed groupId for a new [ListItem]
 * instance matches an existing and selected [ItemGroup]. If there is only one
 * selected [ItemGroup] when a ListItemGroupValidator is instantiated, then its
 * id will automatically be set as the [value].
 */
class ListItemGroupValidator(
    private val dao: ItemGroupDao,
    coroutineScope: CoroutineScope
) : Validator<Long>(-1L) {
    private fun List<ItemGroup>.contains(id: Long) =
        find { it.id == id } != null

    init {
        coroutineScope.launch {
            val first = dao.getSelectedGroups().first()
            if (value == -1L && first.size == 1)
                value = first.first().id
        }
    }

    private val noItemGroupErrorMessage = Message.Error(
        StringResource(R.string.new_item_has_no_item_group_error_message))

    override suspend fun messageFor(value: Long): Message? = when {
        !valueHasBeenChanged -> null
        !dao.getSelectedGroups().first().contains(value) ->
            noItemGroupErrorMessage
        else -> null
    }
}

class ItemGroupNameValidator(private val dao: ItemGroupDao) : Validator<String>("") {

    private val blankNameErrorMessage = Message.Error(
        StringResource(R.string.item_group_blank_name_error_message))
    private val nameAlreadyUsedErrorMessage = Message.Error(
        StringResource(R.string.item_group_already_exists_error_message))

    override suspend fun messageFor(value: String): Message? = when {
        !valueHasBeenChanged -> null
        value.isBlank() ->            blankNameErrorMessage
        dao.nameAlreadyUsed(value) -> nameAlreadyUsedErrorMessage
        else ->                       null
    }
}