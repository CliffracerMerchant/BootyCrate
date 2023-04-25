/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottomdrawer

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.ViewModel
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.model.database.ItemGroup
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ItemGroupNameValidator
import com.cliffracertech.bootycrate.model.database.SettingsDao
import com.cliffracertech.bootycrate.ui.ConfirmatoryDialogState
import com.cliffracertech.bootycrate.ui.NameDialogState
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.collectAsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

interface RenameItemGroupButtonHandler {
    val renameItemGroupDialogState: NameDialogState
    fun onItemGroupRenameClick(itemGroup: ItemGroup)
}

class DefaultRenameItemGroupButtonHandler(
    private val dao: ItemGroupDao,
    private val coroutineScope: CoroutineScope,
) : RenameItemGroupButtonHandler {
    private val validator = ItemGroupNameValidator(dao)
    private val validatorMessage by validator.message.collectAsState(null, coroutineScope)

    private fun hideRenameDialog() {
        renameItemGroupDialogState = NameDialogState.NotShowing
        validator.clear()
    }

    private fun dialogShowingState(
        originalName: String,
    ) = NameDialogState.Showing(
        currentNameProvider = validator::value::get,
        messageProvider = ::validatorMessage,
        onNameChange = validator::value::set,
        onCancel = ::hideRenameDialog,
        onConfirm = {
            coroutineScope.launch {
                val validatedName = validator.validate()
                    ?: return@launch
                dao.setName(originalName, validatedName)
                hideRenameDialog()
            }
        }, title = StringResource(
            R.string.rename_item_group_dialog_title, originalName))

    override var renameItemGroupDialogState by
            mutableStateOf<NameDialogState>(NameDialogState.NotShowing)
        private set

    override fun onItemGroupRenameClick(itemGroup: ItemGroup) {
        renameItemGroupDialogState = dialogShowingState(originalName = itemGroup.name)
    }
}

interface DeleteItemGroupButtonHandler {
    val deleteItemGroupDialogState: ConfirmatoryDialogState
    fun onItemGroupDeleteClick(itemGroup: ItemGroup)
}

class DefaultDeleteItemGroupButtonHandler(
    private val dao: ItemGroupDao,
    coroutineScope: CoroutineScope,
) : DeleteItemGroupButtonHandler {
    private var target by mutableStateOf<ItemGroup?>(null)

    private fun hideDialog() {
        deleteItemGroupDialogState = ConfirmatoryDialogState.NotShowing
        target = null
    }

    private val dialogShowingState = ConfirmatoryDialogState.Showing(
        message = StringResource(R.string.confirm_delete_item_group_message),
        onCancel = ::hideDialog,
        onConfirm = {
            target?.name?.let {
                coroutineScope.launch { dao.delete(it) }
                hideDialog()
            }
        })

    override var deleteItemGroupDialogState by
            mutableStateOf<ConfirmatoryDialogState>(ConfirmatoryDialogState.NotShowing)
        private set

    override fun onItemGroupDeleteClick(itemGroup: ItemGroup) {
        target = itemGroup
        deleteItemGroupDialogState = dialogShowingState
    }
}

interface AddItemGroupButtonHandler {
    val newItemGroupDialogState: NameDialogState
    fun onAddButtonClick()
}

class DefaultAddItemGroupButtonHandler(
    private val dao: ItemGroupDao,
    coroutineScope: CoroutineScope,
) : AddItemGroupButtonHandler {
    private val validator = ItemGroupNameValidator(dao)
    private val validatorMessage by validator.message.collectAsState(null, coroutineScope)

    private fun hideNewItemGroupDialog() {
        newItemGroupDialogState = NameDialogState.NotShowing
        validator.clear()
    }
    private val newItemGroupDialogShowingState = NameDialogState.Showing(
        currentNameProvider = validator::value::get,
        messageProvider = ::validatorMessage,
        onNameChange = validator::value::set,
        onCancel = ::hideNewItemGroupDialog,
        onConfirm = {
            coroutineScope.launch {
                val validatedName = validator.validate()
                    ?: return@launch
                dao.add(validatedName)
                hideNewItemGroupDialog()
            }
        }, title = StringResource(R.string.add_item_group_dialog_title))

    override var newItemGroupDialogState by
            mutableStateOf<NameDialogState>(NameDialogState.NotShowing)
        private set

    override fun onAddButtonClick() {
        newItemGroupDialogState = newItemGroupDialogShowingState
    }
}

@HiltViewModel class BottomAppDrawerViewModel(
    private val navState: NavigationState,
    private val itemGroupDao: ItemGroupDao,
    private val settingsDao: SettingsDao,
    coroutineScope: CoroutineScope
) : ViewModel(coroutineScope),
    RenameItemGroupButtonHandler by DefaultRenameItemGroupButtonHandler(itemGroupDao, coroutineScope),
    DeleteItemGroupButtonHandler by DefaultDeleteItemGroupButtonHandler(itemGroupDao, coroutineScope),
    AddItemGroupButtonHandler by DefaultAddItemGroupButtonHandler(itemGroupDao, coroutineScope)
{
    @Inject constructor(
        navState: NavigationState,
        itemGroupDao: ItemGroupDao,
        settingsDao: SettingsDao,
    ) : this(navState, itemGroupDao, settingsDao, viewModelScope())

    val isHidden by derivedStateOf {
        !navState.visibleScreen.isRootScreen
    }

    fun onSettingsButtonClick() {
        navState.addToStack(NavigationState.AdditionalScreenType.AppSettings)
    }

    val multiSelectItemGroups by settingsDao
        .getMultiSelectGroups()
        .collectAsState(false, coroutineScope)

    fun onSelectAllClick() {
        coroutineScope.launch {
            settingsDao.updateMultiSelectGroups(true)
            itemGroupDao.selectAll()
        }
    }

    fun onMultiSelectItemGroupsCheckboxClick() {
        coroutineScope.launch {
            settingsDao.toggleMultiSelectGroups()
        }
    }

    val itemGroups by itemGroupDao.getAll()
        .map(List<ItemGroup>::toImmutableList)
        .collectAsState(emptyList<ItemGroup>().toImmutableList(), coroutineScope)

    fun onItemGroupClick(itemGroup: ItemGroup) {
        coroutineScope.launch {
            itemGroupDao.toggleIsSelected(itemGroup.name)
        }
    }
}