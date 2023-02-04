/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.model.database.InventoryItem
import com.cliffracertech.bootycrate.model.database.ItemDao
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.utils.collectAsState
import com.cliffracertech.bootycrate.utils.getValue
import com.cliffracertech.bootycrate.utils.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * An abstract [ViewModel] to provide data and callbacks for a dialog to add
 * new [ListItem] subclasses.
 *
 * If the properties [newItemName] and [newItemExtraInfo] are updated with
 * the proposed name and extra info for a new item, the property
 * [newItemNameIsAlreadyUsed] describes if the name and extra info combination
 * is already in use by another item on the same list, is in use by an item in
 * a neighboring collection, or if it is not in use by either, as indicated by
 * the values of the enum [NameIsAlreadyUsed].
 *
 * Subclasses will need to override [newItemNameIsAlreadyUsed] so that its
 * values are the correct [NameIsAlreadyUsed] value given the current values
 * of the StateFlow properties [nameIsAlreadyUsedInShoppingList] and
 * [nameIsAlreadyUsedInInventory].
 *
 * When a suitable name is found, the method [onAddItemRequest] can be called
 * to attempt to create a new item using the most recent values of [newItemName]
 * and [newItemExtraInfo].
 */
abstract class NewItemDialogViewModel<T: ListItem>(
    private val itemDao: ItemDao,
    private val itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?
) : ViewModel() {
    protected val scope = coroutineScope ?: viewModelScope
    val selectedItemGroups get() = runBlocking { itemGroupDao.getSelectedGroupsNow() }

    fun onAddItemRequest(item: T, groupId: Long) {
        scope.launch { itemDao.add(item.toDbListItem(groupId)) }
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

    abstract val newItemNameIsAlreadyUsed: NameIsAlreadyUsed
}

/** A view model to provide data for a dialog to add new [ShoppingListItem]s. */
@HiltViewModel
class NewShoppingListItemDialogViewModel(
    itemDao: ItemDao,
    itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?
) : NewItemDialogViewModel<ShoppingListItem>(
    itemDao, itemGroupDao, coroutineScope
) {
    @Inject constructor(
        itemDao: ItemDao,
        itemGroupDao: ItemGroupDao,
    ) : this(itemDao, itemGroupDao, null)

    override val newItemNameIsAlreadyUsed by combine(
        nameIsAlreadyUsedInInventory,
        nameIsAlreadyUsedInShoppingList,
    ) { existsInInventory, existsInShoppingList -> when {
        existsInShoppingList -> NameIsAlreadyUsed.TrueForCurrentList
        existsInInventory ->    NameIsAlreadyUsed.TrueForSiblingList
        else ->                 NameIsAlreadyUsed.False
    }}.collectAsState(NameIsAlreadyUsed.False, scope)
}

/** A view model to provide data for a dialog to add new [InventoryItem]s. */
@HiltViewModel
class NewInventoryItemDialogViewModel(
    itemDao: ItemDao,
    itemGroupDao: ItemGroupDao,
    coroutineScope: CoroutineScope?
) : NewItemDialogViewModel<InventoryItem>(
    itemDao, itemGroupDao, coroutineScope
) {
    @Inject constructor(
        itemDao: ItemDao,
        itemGroupDao: ItemGroupDao,
    ) : this(itemDao, itemGroupDao, null)

    override val newItemNameIsAlreadyUsed by combine(
        nameIsAlreadyUsedInInventory,
        nameIsAlreadyUsedInShoppingList
    ) { existsInInventory, existsInShoppingList -> when {
        existsInInventory ->    NameIsAlreadyUsed.TrueForCurrentList
        existsInShoppingList -> NameIsAlreadyUsed.TrueForSiblingList
        else ->                 NameIsAlreadyUsed.False
    }}.collectAsState(NameIsAlreadyUsed.False, scope)
}