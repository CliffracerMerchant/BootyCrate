/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ItemGroupSelectorViewModel @Inject constructor(
    private val itemGroupDao: ItemGroupDao,
    private val settingsDao: SettingsDao,
): ViewModel() {

    val itemGroups = itemGroupDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), emptyList())

    val multiSelectGroups get() = settingsDao.getMultiSelectGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly,
                 runBlocking { settingsDao.getMultiSelectGroupsNow() })

    fun onMultiSelectCheckboxClick() {
        viewModelScope.launch { settingsDao.toggleMultiSelectGroups() }
    }
    fun onConfirmAddNewItemGroupDialog(name: String) {
        viewModelScope.launch { itemGroupDao.add(name) }
    }
    fun onConfirmDeleteItemGroupDialog(id: Long) {
        viewModelScope.launch { itemGroupDao.delete(id) }
    }
    fun onConfirmItemGroupRenameDialog(id: Long, name: String) {
        viewModelScope.launch { itemGroupDao.updateName(id, name) }
    }
    fun onItemGroupClick(id: Long) {
        viewModelScope.launch { itemGroupDao.updateIsSelected(id) }
    }
    fun onSelectAllGroupsClick() {
        viewModelScope.launch { itemGroupDao.selectAll() }
    }
}