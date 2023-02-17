/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.NavigationState

@Composable fun NavBarItem(
    title: String,
    iconPainter: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = Column(
    modifier = modifier.clickable(
        enabled = true,
        onClickLabel = title,
        role = Role.Button,
        onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Icon(iconPainter, title)
    Text(title)
}

/** An instance of [BottomAppBarWithCutout] with state provided by an instance
 * of [BottomAppBarViewModel]. */
@Composable fun BootyCrateBottomAppBar(
    interpolationProvider: () -> Float,
    modifier: Modifier = Modifier
) = BoxWithConstraints {
    val viewModel: BottomAppBarViewModel = viewModel()
    val density = LocalDensity.current
    val gradientStartColor = MaterialTheme.colors.primaryVariant
    val gradientEndColor = MaterialTheme.colors.secondaryVariant
    val gradientColors = remember { listOf(gradientStartColor, gradientEndColor) }

    var cutoutLayoutCoordinates by remember { mutableStateOf(Rect.Zero) }
    val backgroundBrush = remember {
        Brush.horizontalGradient(gradientColors)
    }

    val cutoutWidth by animateDpAsState(
        targetValue = cutoutContentWidth(
            showingCheckoutButton = viewModel.checkoutButtonIsVisible))

    val topEdge = remember {
        TopEdgeWithCutout(
            density = density,
            cutout = TopCutout(
                density = density,
                depth = 54f.dp,
                contentHeight = 56.dp,
                topCornerRadius = 25.dp,
                bottomCornerRadius = 33.dp,
                contentMargin = 6.dp,
                widthProvider = { cutoutWidth },
                interpolationProvider = interpolationProvider,
                contents = { CutoutContent(
                    modifier = Modifier.onPlaced {
                        cutoutLayoutCoordinates = Rect(
                            offset = it.positionInParent(),
                            size = it.size.toSize())
                    }, backgroundGradientWidth = this@BoxWithConstraints.maxWidth,
                    checkoutButtonIsVisible = viewModel.checkoutButtonIsVisible,
                    checkoutButtonIsEnabled = viewModel.checkoutButtonIsEnabled,
                    onCheckoutConfirm = viewModel::onCheckoutButtonConfirm,
                    onAddButtonClick = viewModel::onAddButtonClick,
                    interpolationProvider = interpolationProvider
                )}),
            indicator = TopEdgeWithCutout.Indicator(
                density = density,
                width = 48.dp,
                thickness = 8.dp,
                color = Color.Gray),
            topOuterCornerRadius = 25.dp)
    }

    BottomAppBarWithCutout(
        modifier = modifier,
        backgroundBrush = backgroundBrush,
        indicatorTarget = viewModel.selectedRootScreen,
        topEdge = topEdge,
    ) { _, onLayout ->
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            val shoppingList = NavigationState.RootScreen.ShoppingList
            NavBarItem(
                title = stringResource(R.string.shopping_list_description),
                iconPainter = painterResource(R.drawable.shopping_cart_icon),
                modifier = Modifier.onPlaced { onLayout(shoppingList, it) }
            ) { viewModel.onNavBarItemClick(shoppingList)}

            val inventory = NavigationState.RootScreen.Inventory
            NavBarItem(
                title = stringResource(R.string.inventory_description),
                iconPainter = painterResource(R.drawable.inventory_icon),
                modifier = Modifier.onPlaced { onLayout(inventory, it) }
            ) { viewModel.onNavBarItemClick(inventory)}
        }
    }

    if (viewModel.newShoppingListItemDialogIsVisible) {
        val newShoppingListItemDialogVM: NewShoppingListItemDialogViewModel by viewModel()
        NewShoppingListItemDialog(
            onDismissRequest = newShoppingListItemDialogVM::onDismissRequest,
            onAddAnotherClick = newShoppingListItemDialogVM::onAddAnotherClick,
            onOkClick = newShoppingListItemDialogVM::onOkClick,
            messages = newShoppingListItemDialogVM.messages)
    }

    if (viewModel.newInventoryItemDialogIsVisible) {
        val newInventoryItemDialogVM: NewInventoryItemDialogViewModel by viewModel()
        NewInventoryItemDialog(
            onDismissRequest = newInventoryItemDialogVM::onDismissRequest,
            onAddAnotherClick = newInventoryItemDialogVM::onAddAnotherClick,
            onOkClick = newInventoryItemDialogVM::onOkClick,
            messages = newInventoryItemDialogVM.messages)
    }
}

