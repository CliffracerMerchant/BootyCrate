/*
 * Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracertech.bootycrate.viewmodel

import android.app.Application
import android.view.MenuItem
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.dataStore
import com.cliffracertech.bootycrate.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.database.ListItem
import com.cliffracertech.bootycrate.utils.mutableEnumPreferenceFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivityViewModel(app: Application): AndroidViewModel(app) {
    private val itemGroupDao = BootyCrateDatabase.get(app).itemGroupDao()
    private val settingsDao = BootyCrateDatabase.get(app).settingsDao()

    val itemGroups = itemGroupDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val multiSelectGroups get() = runBlocking { settingsDao.getMultiSelectGroupsNow() }

    private val sortFlow = app.dataStore.mutableEnumPreferenceFlow(
        intPreferencesKey("item_sort"), viewModelScope, ListItem.Sort.Color)
    val sort = sortFlow.asStateFlow()

    fun onSortOptionSelected(menuItem: MenuItem): Boolean {
        sortFlow.value = when (menuItem.itemId) {
            R.id.color_option ->             ListItem.Sort.Color
            R.id.name_ascending_option ->    ListItem.Sort.NameAsc
            R.id.name_descending_option ->   ListItem.Sort.NameDesc
            R.id.amount_ascending_option ->  ListItem.Sort.AmountAsc
            R.id.amount_descending_option -> ListItem.Sort.AmountDesc
            else -> ListItem.Sort.Color
        }
        return true
    }

    fun onMultiSelectCheckboxClick() { viewModelScope.launch {
        settingsDao.toggleMultiSelectGroups()
    }}
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