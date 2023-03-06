/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottomdrawer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.bottombar.BootyCrateBottomAppBar
import com.cliffracertech.bootycrate.itemgroupselector.ItemGroupSelector
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.model.database.ItemGroup
import com.cliffracertech.bootycrate.model.database.ItemGroupDao
import com.cliffracertech.bootycrate.model.database.ItemGroupNameValidator
import com.cliffracertech.bootycrate.model.database.SettingsDao
import com.cliffracertech.bootycrate.ui.ConfirmDialog
import com.cliffracertech.bootycrate.ui.NameDialog
import com.cliffracertech.bootycrate.ui.NameDialogState
import com.cliffracertech.bootycrate.utils.collectAsState
import com.cliffracertech.bootycrate.utils.toPx
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
    private val itemGroupNameDialogMessage by
        itemGroupNameValidator.message.collectAsState(null, scope)

    private var renameDialogTargetId by mutableStateOf<Long?>(null)
    private fun hideRenameDialog() {
        renameDialogState = NameDialogState.NotShowing
        itemGroupNameValidator.clear()
    }
    private val renameDialogShowingState = NameDialogState.Showing(
        currentNameProvider = itemGroupNameValidator::value::get,
        messageProvider = { itemGroupNameDialogMessage },
        onNameChange = itemGroupNameValidator::value::set,
        onCancel = ::hideRenameDialog,
        onConfirm = {
            scope.launch {
                val validatedName = itemGroupNameValidator.validate()
                val id = renameDialogTargetId
                if (validatedName == null || id == null)
                    return@launch
                itemGroupDao.updateName(id, validatedName)
                hideRenameDialog()
            }
        })

    var renameDialogState by mutableStateOf<NameDialogState>(NameDialogState.NotShowing)
        private set

    fun onItemGroupRenameClick(itemGroup: ItemGroup) {
        renameDialogState = renameDialogShowingState
        renameDialogTargetId = itemGroup.id
    }


    private var confirmDeleteDialogTarget by mutableStateOf<ItemGroup?>(null)

    val showingConfirmDeleteDialog by derivedStateOf {
        confirmDeleteDialogTarget != null
    }

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


    private fun hideNewItemGroupDialog() {
        newItemGroupDialogState = NameDialogState.NotShowing
        itemGroupNameValidator.clear()
    }
    private val newItemGroupDialogShowingState = NameDialogState.Showing(
        currentNameProvider = itemGroupNameValidator::value::get,
        messageProvider = { itemGroupNameDialogMessage },
        onNameChange = itemGroupNameValidator::value::set,
        onCancel = ::hideNewItemGroupDialog,
        onConfirm = {
            scope.launch {
                val validatedName = itemGroupNameValidator.validate()
                    ?: return@launch
                itemGroupDao.add(validatedName)
                hideNewItemGroupDialog()
            }
        })

    var newItemGroupDialogState by mutableStateOf<NameDialogState>(NameDialogState.NotShowing)
        private set

    fun onAddButtonClick() {
        newItemGroupDialogState = newItemGroupDialogShowingState
    }
}

enum class DrawerState { Hidden, Collapsed, Expanded;
    val isHidden get() = this == Hidden
    val isCollapsed get() = this == Collapsed
    val isExpanded get() = this == Expanded
}

@Composable fun BootyCrateBottomNavDrawer(
    modifier: Modifier = Modifier,
) {
    val vm: BottomAppDrawerViewModel = viewModel()
    val swipeableState = rememberSwipeableState(DrawerState.Collapsed)
    val density = LocalDensity.current
    val peekHeight = remember(density) { 56.dp.toPx(density) }
    val expandedHeight = remember(density) { 456.dp.toPx(density) }
    val anchors = remember { mapOf(
        -expandedHeight to DrawerState.Expanded,
        0f              to DrawerState.Collapsed,
        peekHeight      to DrawerState.Hidden,
    )}

    Column(modifier = modifier
        .height(456.dp)
        .fillMaxWidth()
        .offset(y = 400.dp)
        .swipeable(
            state = swipeableState,
            anchors = anchors,
            orientation = Orientation.Vertical)
        .graphicsLayer { translationY = swipeableState.offset.value }
    ) {
        val expansionProgressProvider = remember {{
            (-swipeableState.offset.value / expandedHeight).coerceIn(0f, 1f)
        }}

        Box(Modifier.height(56.dp).fillMaxWidth()) {
            BootyCrateBottomAppBar(
                interpolationProvider = remember {{ 1f - expansionProgressProvider() }},
                modifier = Modifier.graphicsLayer { alpha = 1f - expansionProgressProvider() })
            ItemGroupSelector(
                title = stringResource(R.string.app_name),
                onSelectAllClick = vm::onSelectAllClick,
                multiSelectGroups = vm.multiSelectItemGroups,
                onMultiSelectClick = vm::onMultiSelectItemGroupsCheckboxClick,
                itemGroups = vm.itemGroups,
                onItemGroupClick = vm::onItemGroupClick,
                onItemGroupRenameClick = vm::onItemGroupRenameClick,
                onItemGroupDeleteClick = vm::onItemGroupDeleteClick,
                onAddButtonClick = vm::onAddButtonClick,
                otherTopBarContent = {
                    IconButton(vm::onSettingsButtonClick) {
                        Icon(Icons.Default.Settings, null)
                    }
                })
        }
    }

    RenameItemGroupDialog(vm.renameDialogState)
    NewItemGroupDialog(vm.newItemGroupDialogState)

    if (vm.showingConfirmDeleteDialog)
        ConfirmDialog(
            modifier = modifier,
            message = stringResource(
                R.string.confirm_delete_item_group_message),
            onDismissRequest = vm::onConfirmDeleteDialogCancelClick,
            onConfirm = vm::onConfirmDeleteDialogConfirmClick)
}

@Composable private fun RenameItemGroupDialog(
    state: NameDialogState,
    modifier: Modifier = Modifier,
) {
    if (state !is NameDialogState.Showing) return

    val title = stringResource(
        R.string.rename_item_group_dialog_title,
        state.currentNameProvider())
    NameDialog(title, state, modifier)
}

@Composable private fun NewItemGroupDialog(
    state: NameDialogState,
    modifier: Modifier = Modifier,
) {
    if (state !is NameDialogState.Showing)
        return
    val title = stringResource(R.string.add_item_group_dialog_title)
    NameDialog(title, state, modifier)
}