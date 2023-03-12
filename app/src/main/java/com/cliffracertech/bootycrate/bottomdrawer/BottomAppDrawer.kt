/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottomdrawer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.bottombar.BootyCrateBottomAppBar
import com.cliffracertech.bootycrate.bottomdrawer.DrawerState.*
import com.cliffracertech.bootycrate.itemgroupselector.ItemGroupSelector
import com.cliffracertech.bootycrate.ui.ConfirmDialog
import com.cliffracertech.bootycrate.ui.ConfirmatoryDialogState
import com.cliffracertech.bootycrate.ui.NameDialog
import com.cliffracertech.bootycrate.ui.NameDialogState
import com.cliffracertech.bootycrate.utils.toPx

/** [DrawerState]'s values [Hidden], [Collapsed], and [Expanded]
 * describe the possible states for a collapsible, hideable, drawer. */
enum class DrawerState { Hidden, Collapsed, Expanded;
    val isHidden get() = this == Hidden
    val isCollapsed get() = this == Collapsed
    val isExpanded get() = this == Expanded
}

/**
 * A state holder that contains parameters for a bottom-aligned drawer. The
 * properties [expandedHeight] and [peekHeight] describe the height of the
 * drawer when it is fully expanded and collapsed, respectively. The method
 * [swipeableState] can be invoked in a [Composable] context to obtain a
 * [SwipeableState]`<DrawerState>`. This can be used alongside the drawer's
 * anchor points provided through the [anchors] property to create a
 * swipeable bottom drawer through the [Modifier.swipeable] method.
 */
class BottomDrawerState(
    density: Density,
    val expandedHeight: Dp,
    val peekHeight: Dp,
) {
    val expandedHeightPx = expandedHeight.toPx(density)
    val peekHeightPx = peekHeight.toPx(density)

    @Composable fun swipeableState(
        initialState: DrawerState = Collapsed
    ) = rememberSwipeableState(initialState)

    val anchors = mapOf(
        peekHeightPx                       to Hidden,
        0f                                 to Collapsed,
        -(expandedHeightPx - peekHeightPx) to Expanded)
}

/**
 * A swipeable bottom app drawer that can be hidden, collapsed, or expanded.
 *
 * @param state The [BottomDrawerState] that describes the drawer
 * @param modifier The [Modifier] to use for the root layout
 * @param initialDrawerState The [DrawerState] value that defines
 *     the initial hidden/collapsed/expanded state of the drawer
 * @param content The content of the drawer. To help you define what
 *     content to show, a getter that returns the expansion progress
 *     (a value in the range of 0f to indicate the collapsed/hidden
 *     state to 1f to indicate the expanded state) when invoked as
 *     well as the current target [DrawerState] value are provided.
 */
@Composable fun BottomAppDrawer(
    state: BottomDrawerState,
    modifier: Modifier = Modifier,
    initialDrawerState: DrawerState = Collapsed,
    content: @Composable BoxScope.(
            expansionProgressProvider: () -> Float,
            targetState: DrawerState
        ) -> Unit
) {
    val swipeableState = state.swipeableState(initialDrawerState)

    Box(modifier = modifier
        .height(state.expandedHeight)
        .fillMaxWidth()
        .offset(y = state.expandedHeight - state.peekHeight)
        .swipeable(
            state = swipeableState,
            anchors = state.anchors,
            orientation = Orientation.Vertical)
        .graphicsLayer { translationY = swipeableState.offset.value }
    ) {
        val expansionProgressProvider = remember {{
            (-swipeableState.offset.value / state.expandedHeightPx).coerceIn(0f, 1f)
        }}
        content(expansionProgressProvider, swipeableState.targetValue)
    }
}

/**
 * A BottomAppDrawer with state provided by an instance of [BottomAppDrawerViewModel].
 *
 * @param modifier The [Modifier] that will apply to the entire drawer
 * @param additionalPeekHeight A [Dp] value that will be added to the default
 *     56.dp peek height of the drawer. This value can be used to adjust for
 *     the height of the bottom navigation bar, if any.
 */
@Composable fun BootyCrateBottomAppDrawer(
    modifier: Modifier = Modifier,
    additionalPeekHeight: Dp = 0.dp) {
    val vm: BottomAppDrawerViewModel = viewModel()
    val density = LocalDensity.current
    val drawerState = remember(density, additionalPeekHeight) {
        BottomDrawerState(
            density = density,
            expandedHeight = 456.dp,
            peekHeight = 56.dp + additionalPeekHeight)
    }
    BottomAppDrawer(
        state = drawerState,
        modifier = modifier,
    ) { expansionProgressProvider, targetState ->
        val expansionProgress = expansionProgressProvider()
        if (expansionProgress < 1f)
            BootyCrateBottomAppBar(
                interpolationProvider = remember {{ 1f - expansionProgressProvider() }},
                contentModifier = Modifier.height(drawerState.peekHeight))

        if (expansionProgress > 0f)
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
                modifier = Modifier.graphicsLayer {
                    if (targetState != Hidden)
                        alpha = expansionProgressProvider()
                }, otherTopBarContent = {
                    IconButton(vm::onSettingsButtonClick) {
                        Icon(Icons.Default.Settings, null)
                    }
                })
    }

    if (vm.renameItemGroupDialogState is NameDialogState.Showing)
        NameDialog(vm.renameItemGroupDialogState)
    if (vm.deleteItemGroupDialogState is ConfirmatoryDialogState.Showing)
        ConfirmDialog(vm.deleteItemGroupDialogState)
    if (vm.newItemGroupDialogState is NameDialogState.Showing)
        NameDialog(vm.newItemGroupDialogState)
}