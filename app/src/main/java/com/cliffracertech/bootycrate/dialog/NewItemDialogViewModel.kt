/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracertech.bootycrate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.model.database.*
import com.cliffracertech.bootycrate.utils.getValue
import com.cliffracertech.bootycrate.utils.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * An abstract AndroidViewModel to provide data and callbacks for a dialog to add new ListItem subclasses
 * .
 * If the properties newItemName and newItemExtraInfo are updated with the
 * proposed name and extra info for a new item, the StateFlow property
 * newItemNameIsAlreadyUsed can be collected to tell if the name and extra info
 * combination is already in use by another item on the same list, is in use by
 * an item in a neighboring collection, or if it is not in use by either, as
 * indicated by the values of the enum class NameIsAlreadyUsed.
 *
 * Subclasses will need to override newItemNameIsAlreadyUsed with a Flow whose
 * emitted values are the correct NameIsAlreadyUsed value given the current
 * values of the StateFlow properties nameIsAlreadyUsedInShoppingList and
 * nameIsAlreadyUsedInInventory.
 */
abstract class NewItemDialogViewModel<T: ListItem>(
    private val itemDao: ItemDao,
    private val itemGroupDao: ItemGroupDao
) : ViewModel() {

    val selectedItemGroups get() = runBlocking { itemGroupDao.getSelectedGroupsNow() }

    fun onAddItemRequest(item: T, groupId: Long) {
        viewModelScope.launch { itemDao.add(item.toDbListItem(groupId)) }
    }

    /** An enum whose values represent whether a given name for a new
     * item is already in use by another item in a given list. */
    enum class NameIsAlreadyUsed {
        /** The name is already taken by an item in the list whose contents are being checked. */
        TrueForCurrentList,
        /** The name is already taken by an item in a sibling list of items, but not the current one. */
        TrueForSiblingList,
        /** The name is not in use. */
        False
    }

    private val _newItemName = MutableStateFlow("")
    var newItemName by _newItemName

    private val _newItemExtraInfo = MutableStateFlow("")
    var newItemExtraInfo by _newItemExtraInfo

    protected val nameIsAlreadyUsedInShoppingList =
        _newItemName.combine(_newItemExtraInfo, itemDao::nameAlreadyUsedInShoppingList)
    protected val nameIsAlreadyUsedInInventory =
        _newItemName.combine(_newItemExtraInfo, itemDao::nameAlreadyUsedInInventory)

    abstract val newItemNameIsAlreadyUsed: StateFlow<NameIsAlreadyUsed>
}

/** A view model to provide data for a dialog to add new ShoppingListItems. */
@HiltViewModel
class NewShoppingListItemDialogViewModel @Inject constructor(
    itemDao: ItemDao,
    itemGroupDao: ItemGroupDao
) : NewItemDialogViewModel<ShoppingListItem>(itemDao, itemGroupDao) {

    override val newItemNameIsAlreadyUsed = combine(
        nameIsAlreadyUsedInShoppingList,
        nameIsAlreadyUsedInInventory
    ) { existsInShoppingList, existsInInventory -> when {
        existsInShoppingList -> NameIsAlreadyUsed.TrueForCurrentList
        existsInInventory ->    NameIsAlreadyUsed.TrueForSiblingList
        else ->                 NameIsAlreadyUsed.False
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), NameIsAlreadyUsed.False)
}

/** A view model to provide data for a dialog to add new InventoryItems. */
@HiltViewModel
class NewInventoryItemDialogViewModel @Inject constructor(
    itemDao: ItemDao,
    itemGroupDao: ItemGroupDao
) : NewItemDialogViewModel<InventoryItem>(itemDao, itemGroupDao) {

    override val newItemNameIsAlreadyUsed = combine(
        nameIsAlreadyUsedInInventory,
        nameIsAlreadyUsedInShoppingList
    ) { existsInInventory, existsInShoppingList -> when {
        existsInInventory ->    NameIsAlreadyUsed.TrueForCurrentList
        existsInShoppingList -> NameIsAlreadyUsed.TrueForSiblingList
        else ->                 NameIsAlreadyUsed.False
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), NameIsAlreadyUsed.False)
}