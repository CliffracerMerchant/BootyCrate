/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottomdrawer

import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.SwipeableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.bottombar.BootyCrateBottomAppBar
import com.cliffracertech.bootycrate.bottomdrawer.DrawerState.*
import com.cliffracertech.bootycrate.itemgroupselector.ItemGroupSelector
import com.cliffracertech.bootycrate.springStiffness
import com.cliffracertech.bootycrate.ui.ConfirmDialog
import com.cliffracertech.bootycrate.ui.ConfirmatoryDialogState
import com.cliffracertech.bootycrate.ui.NameDialog
import com.cliffracertech.bootycrate.ui.NameDialogState
import com.cliffracertech.bootycrate.utils.toPx
import kotlin.math.abs

/** [DrawerState]'s values [Hidden], [Collapsed], and [Expanded]
 * describe the possible states for a collapsible, hideable, drawer. */
enum class DrawerState { Hidden, Collapsed, Expanded;
    val isHidden get() = this == Hidden
    val isCollapsed get() = this == Collapsed
    val isExpanded get() = this == Expanded
}

/**
 * An immutable state holder that contains parameters for a top or bottom-
 * aligned drawer. The properties [expandedHeight] and [peekHeight] describe
 * the height of the drawer when it is fully expanded and collapsed,
 * respectively. The [anchors] property can be used to create a swipeable
 * bottom drawer through the [Modifier.swipeable] method.
 */
class DrawerSizes(
    density: Density,
    val expandedHeight: Dp,
    val peekHeight: Dp,
) {
    val expandedHeightPx = expandedHeight.toPx(density)
    val peekHeightPx = peekHeight.toPx(density)
    val heightChangePx = expandedHeightPx - peekHeightPx

    val anchors = mapOf(
        0f               to Expanded,
        heightChangePx   to Collapsed,
        expandedHeightPx to Hidden)
}

/**
 * A swipeable bottom app drawer that can be hidden, collapsed, or expanded.
 *
 * @param sizes The [DrawerSizes] that describes the drawer
 * @param allowDragToHide Whether or not hiding the drawer via swiping
 *     down should be allowed. If false, downward drag gestures will be
 *     blocked when the drawer is already in its collapsed state. While
 *     this effect can also be achieved via a SwipeableState.confirmStateChange,
 *     setting allowDragToHide to false will also prevent the drawer
 *     from moving toward the hidden state (similar to an infinite
 *     resistance), and will not prevent programmatic changes to the
 *     hidden state (e.g. using swipeableState.animateTo(Hidden).
 * @param modifier The [Modifier] to use for the root layout
 * @param swipeableState A [SwipeableState]`<DrawerState>` instance
 * @param content The content of the drawer. To help you define what
 *     content to show, a getter that returns the expansion progress
 *     (a value in the range of 0f to indicate the collapsed/hidden
 *     state to 1f to indicate the expanded state) when invoked as
 *     well as the current target [DrawerState] value are provided.
 */
@Composable fun BottomAppDrawer(
    sizes: DrawerSizes,
    allowDragToHide: Boolean,
    modifier: Modifier = Modifier,
    swipeableState: SwipeableState<DrawerState> =
        rememberSwipeableState(Collapsed),
    content: @Composable BoxScope.(expansionProgressProvider: () -> Float) -> Unit,
) = Box(modifier = modifier
    .height(sizes.expandedHeight)
    .fillMaxWidth()
    .graphicsLayer {
        translationY = swipeableState.offset.value
    }.swipeable(
        state = swipeableState,
        anchors = sizes.anchors,
        orientation = Orientation.Vertical,
        resistance = ResistanceConfig(
            basis = sizes.heightChangePx / 2f,
            factorAtMin = Float.MAX_VALUE,
            factorAtMax = Float.MAX_VALUE)
    ).pointerInput(allowDragToHide) {
        if (!allowDragToHide) forEachGesture {
            awaitPointerEventScope {
                while(true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.fastForEach {
                        val yPosChange = it.positionChange().y
                        val offset = swipeableState.offset.value
                        if (yPosChange > 0f && offset > sizes.heightChangePx)
                            it.consume()
                    }
                }
            }
        }
    }
) {
    content { // expansionProgressProvider = {
        if (swipeableState.offset.value <= sizes.heightChangePx)
            (1f - swipeableState.offset.value / sizes.heightChangePx)
        else ((sizes.heightChangePx - swipeableState.offset.value) /
                sizes.peekHeightPx).coerceAtLeast(-1f)
    }
}

/**
 * A BottomAppDrawer with state provided by an instance of [BottomAppDrawerViewModel].
 *
 * @param modifier The [Modifier] that will apply to the entire drawer
 * @param additionalPeekHeight A [Dp] value that will be added to the
 *     default 56.dp peek height of the drawer as well as its expanded
 *     height. This value can be used to adjust for the height of the
 *     bottom navigation bar, if any.
 */
@Composable fun BootyCrateBottomAppDrawer(
    modifier: Modifier = Modifier,
    additionalPeekHeight: Dp = 0.dp) {
    val vm: BottomAppDrawerViewModel = viewModel()
    val density = LocalDensity.current
    val drawerSizes = remember(density, additionalPeekHeight) {
        DrawerSizes(
            density = density,
            expandedHeight = 456.dp + additionalPeekHeight,
            peekHeight = 56.dp + additionalPeekHeight)
    }
    val swipeableState = rememberSwipeableState(
        initialValue = Collapsed,
        animationSpec = spring(stiffness = springStiffness))

    LaunchedEffect(vm.drawerIsHidden) {
        swipeableState.animateTo(
            if (vm.drawerIsHidden) Hidden
            else                   Collapsed)
    }

    BottomAppDrawer(
        sizes = drawerSizes,
        allowDragToHide = false,
        modifier = modifier,
        swipeableState = swipeableState,
    ) { expansionProgressProvider ->
        BootyCrateBottomAppBar(interpolationProvider = {
            if (vm.drawerIsHidden) 0f
            else 1f - abs(expansionProgressProvider())
        })

        val itemGroupSelectorVisible by remember { derivedStateOf {
            expansionProgressProvider() > 0f
        }}
        if (itemGroupSelectorVisible)
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
                    if (swipeableState.targetValue != Hidden)
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