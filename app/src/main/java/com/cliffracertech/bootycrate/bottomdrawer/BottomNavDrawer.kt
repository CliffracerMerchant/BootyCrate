/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottomdrawer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import com.cliffracertech.bootycrate.model.database.Validator
import com.cliffracertech.bootycrate.ui.SingleValidatorMessage
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
}

@Composable fun CancelOkButtonRow(
    modifier: Modifier = Modifier,
    okButtonEnabled: Boolean = true,
    onCancelClick: () -> Unit,
    onOkClick: () -> Unit,
) = Row(modifier) {
    Spacer(Modifier.weight(1f))
    TextButton(onClick = onCancelClick) {
        Text(stringResource(android.R.string.cancel))
    }
    TextButton(
        onClick = onOkClick,
        enabled = okButtonEnabled
    ) {
        Text(stringResource(android.R.string.ok))
    }
}

/**
 * A naming dialog for an [ItemGroup].
 *
 * @param modifier The [Modifier] to use for the root layout
 * @param currentName The current name of the [ItemGroup] whose name
 *     is being set. The title of the dialog will change according to
 *     whether this value starts as a blank string.
 * @param onCurrentNameChange The callback that will be invoked when
 *     the current name is changed
 * @param message A nullable [Validator.Message] that provides information about
 *     the current value of [currentName] (e.g. that it is invalid) if necessary
 * @param onDismissRequest The callback that will be invoked when the user tries
 *     to dismiss the dialog via the cancel button, the back button/gesture, or
 *     a click outside the dialog
 * @param onConfirm The callback that will be invoked when the user clicks the ok button
 */
@Composable private fun ItemGroupNameDialog(
    modifier: Modifier = Modifier,
    currentName: String,
    onCurrentNameChange: (String) -> Unit,
    message: Validator.Message?,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) = AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = {
        val context = LocalContext.current
        val title = remember {
            if (currentName.isBlank())
                context.getString(R.string.add_item_group_dialog_title)
            else context.getString(R.string.rename_item_group_dialog_title, currentName)
        }
        Text(title)
    }, text = {
        Column {
            TextField(
                onValueChange = onCurrentNameChange,
                value = currentName,
                modifier = Modifier.fillMaxWidth(),
//                    .padding(horizontal = 16.dp),
                isError = message is Validator.Message.Error,
                singleLine = true)
            SingleValidatorMessage(message)
        }
    }, buttons = {
        CancelOkButtonRow(
            okButtonEnabled = message is Validator.Message.Error,
            onCancelClick = onDismissRequest,
            onOkClick = onConfirm)
    },
)