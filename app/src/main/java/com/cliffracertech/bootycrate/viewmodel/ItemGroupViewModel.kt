/*
 * Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracertech.bootycrate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** An AndroidViewModel to expose the contents of the database's itemGroup table.
 *
 * A Flow of the list of all item groups in the database can be accessed
 * through the public property inventories. The property selectedInventories
 * exposes a Flow of the list of all inventories whose property isSelected is
 * true. The property selectedInventoryName is a StateFlow<String> whose
 * current value will be equal to the name of the selected inventory if only
 * one is selected, or a string to express that multiple inventories are
 * selected otherwise.
 *
 * The functions add, delete, and updateName can be used to add new
 * item groups, delete existing ones, or update the name of an existing
 * item group, respectively.
 *
 * The StateFlow multiSelect's current value represents the database's current
 * allowance for multi-selecting item groups. If false, selecting an item group
 * other than the current one will deselect the previous selected inventory.
 * multiSelect's value can be toggled with the function toggleMultiSelect. The
 * isSelected state of individual inventories is updated by calling updateIsSelected
 * with the inventory's id. When in multiselect mode, this will toggle the
 * inventory's isSelected state. It will single select it otherwise. The
 * function selectAll is provided for convenience, but will not work if the
 * database is not in multiselect inventory mode.
 */
class ItemGroupViewModel(app: Application): AndroidViewModel(app) {
    private val itemGroupDao = BootyCrateDatabase.get(app).itemGroupDao()
    private val settingsDao = BootyCrateDatabase.get(app).settingsDao()
    private val nameForMultiSelection = app.getString(R.string.multiple_selected_item_groups_description)

    val itemGroups = itemGroupDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val selectedItemGroups get() = runBlocking { itemGroupDao.getSelectedGroupsNow() }

    val selectedItemGroupName = itemGroupDao.getSelectedGroups().map {
        if (it.size == 1) it.first().name
        else nameForMultiSelection
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    val multiSelect get() = runBlocking { settingsDao.getMultiSelectGroupsNow() }

    fun toggleMultiSelect() { viewModelScope.launch {
        settingsDao.toggleMultiSelectGroups()
    }}
    fun add(name: String) {
        viewModelScope.launch { itemGroupDao.add(name) }
    }
    fun delete(id: Long) {
        viewModelScope.launch { itemGroupDao.delete(id) }
    }
    fun updateName(id: Long, name: String) {
        viewModelScope.launch { itemGroupDao.updateName(id, name) }
    }

    /** Update the selection state of the inventory whose id is equal to the
     * parameter id. If the database is in single select inventory mode, this
     * will select the given inventory; it will otherwise toggle the selection. */
    fun updateIsSelected(id: Long) {
        viewModelScope.launch { itemGroupDao.updateIsSelected(id) }
    }
    fun selectAll() {
        viewModelScope.launch { itemGroupDao.selectAll() }
    }
}

/** An AndroidViewModel for an InventorySelector.
 *
 * A Flow of the list of all inventories in the database can be accessed
 * through the public property inventories. The property selectedInventories
 * exposes a Flow of the list of all inventories whose property isSelected is
 * true. The property selectedInventoryName is a StateFlow<String> whose
 * current value will be equal to the name of the selected inventory if only
 * one is selected, or a string to express that multiple inventories are
 * selected otherwise.
 *
 * The functions add, delete, and updateName can be used to add new
 * inventories, delete existing ones, or update the name of an existing
 * inventory, respectively.
 *
 * The StateFlow multiSelect's current value represents the database's current
 * allowance for multi-selecting inventories. If false, selecting an inventory
 * other than the current one will deselect the previous selected inventory.
 * multiSelect's value can be toggled with the function toggleMultiSelect. The
 * isSelected state of individual inventories is updated by calling updateIsSelected
 * with the inventory's id. When in multiselect mode, this will toggle the
 * inventory's isSelected state. It will single select it otherwise. The
 * function selectAll is provided for convenience, but will not work if the
 * database is not in multiselect inventory mode.
 */
class ItemGroupSelectorViewModel(app: Application): AndroidViewModel(app) {
    private val itemGroupDao = BootyCrateDatabase.get(app).itemGroupDao()
    private val settingsDao = BootyCrateDatabase.get(app).settingsDao()

    val itemGroups = itemGroupDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val multiSelectGroups = settingsDao.getMultiSelectGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(),
            runBlocking { settingsDao.getMultiSelectGroupsNow() })

    fun toggleMultiSelect() { viewModelScope.launch {
        settingsDao.toggleMultiSelectGroups()
    }}
    fun onAddNewItemGroupRequest(name: String) {
        viewModelScope.launch { itemGroupDao.add(name) }
    }
    fun onDeleteItemGroupRequest(id: Long) {
        viewModelScope.launch { itemGroupDao.delete(id) }
    }
    fun onUpdateItemGroupNameRequest(id: Long, name: String) {
        viewModelScope.launch { itemGroupDao.updateName(id, name) }
    }

    fun onItemGroupClick(id: Long) {
        viewModelScope.launch { itemGroupDao.updateIsSelected(id) }
    }
    fun onSelectAllGroupsRequest() {
        viewModelScope.launch { itemGroupDao.selectAll() }
    }
}