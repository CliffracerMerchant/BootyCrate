/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottomdrawer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.model.database.ItemGroup
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ItemGroupNameValidator
import com.cliffracertech.bootycrate.model.database.SettingsDao
import com.cliffracertech.bootycrate.utils.collectAsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class BottomAppDrawerViewModel(
    private val navState: NavigationState,
    private val itemGroupDao: ItemGroupDao,
    private val settingsDao: SettingsDao,
    coroutineScope: CoroutineScope?
) : ViewModel() {
    @Inject constructor(
        navState: NavigationState,
        itemGroupDao: ItemGroupDao,
        dbSettingsDao: SettingsDao
    ) : this(navState, itemGroupDao, dbSettingsDao, null)

    val scope = coroutineScope ?: viewModelScope

    fun onSettingsButtonClick() {
        navState.addToStack(NavigationState.AdditionalScreen.AppSettings)
    }

    val multiSelectItemGroups by settingsDao
        .getMultiSelectGroups()
        .collectAsState(false, scope)

    fun onSelectAllClick() {
        scope.launch {
            settingsDao.updateMultiSelectGroups(true)
            itemGroupDao.selectAll()
        }
    }

    fun onMultiSelectItemGroupsCheckboxClick() {
        scope.launch { settingsDao.toggleMultiSelectGroups() }
    }

    val itemGroups by itemGroupDao.getAll()
        .map(List<ItemGroup>::toImmutableList)
        .collectAsState(emptyList<ItemGroup>().toImmutableList(), scope)

    fun onItemGroupClick(itemGroup: ItemGroup) {
        scope.launch { itemGroupDao.updateIsSelected(itemGroup.id) }
    }


    private val itemGroupNameValidator = ItemGroupNameValidator(itemGroupDao)

    val itemGroupNameDialogMessage by
        itemGroupNameValidator.message.collectAsState(null, scope)

    var itemGroupNameDialogTarget by mutableStateOf<ItemGroup?>(null)
        private set

    fun onItemGroupRenameClick(itemGroup: ItemGroup) {
        itemGroupNameDialogTarget = itemGroup
    }

    fun onItemGroupNameDialogCancel() {
        itemGroupNameDialogTarget = null
        itemGroupNameValidator.clear()
    }

    fun onItemGroupNameDialogChange(newName: String) {
        itemGroupNameValidator.value = newName
    }

    fun onItemGroupRenameDialogConfirm() {
        scope.launch {
            val validatedName = itemGroupNameValidator.validate()
            val id = itemGroupNameDialogTarget?.id
            if (validatedName == null || id == null)
                return@launch
            itemGroupDao.updateName(id, validatedName)
            onItemGroupNameDialogCancel()
        }
    }


    var confirmDeleteDialogTarget by mutableStateOf<ItemGroup?>(null)
        private set

    fun onItemGroupDeleteClick(itemGroup: ItemGroup) {
        confirmDeleteDialogTarget = itemGroup
    }

    fun onConfirmDeleteDialogCancelClick() {
        confirmDeleteDialogTarget = null
    }

    fun onConfirmDeleteDialogConfirmClick() {
        val id = confirmDeleteDialogTarget?.id ?: return
        scope.launch { itemGroupDao.delete(id) }
        onConfirmDeleteDialogCancelClick()
    }


    var showingNewItemGroupDialog by mutableStateOf(false)
        private set

    fun onAddButtonClick() {
        showingNewItemGroupDialog = true
    }

    fun onNewItemGroupDialogNameChange(newName: String) {
        itemGroupNameValidator.value = newName
    }

    fun onNewItemGroupDialogCancel() {
        showingNewItemGroupDialog = false
    }

    fun onNewItemGroupDialogConfirm() {
        scope.launch {
            val validatedName = itemGroupNameValidator.validate()
                ?: return@launch
            itemGroupDao.add(validatedName)
            onNewItemGroupDialogCancel()
        }
    }
}