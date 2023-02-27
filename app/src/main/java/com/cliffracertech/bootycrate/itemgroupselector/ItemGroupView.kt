/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemgroupselector

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ItemGroup
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme

@Composable fun AmountIndicator(
    painter: Painter,
    amount: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) = Row(modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically) {
    Icon(painter, contentDescription,
         Modifier.size(18.dp))
    Text(text = amount.toString())
}

@Composable fun AmountIndicator(
    icon: ImageVector,
    amount: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) = AmountIndicator(rememberVectorPainter(icon),
                    amount, contentDescription, modifier)

@Composable fun ItemGroupView(
    isSelected: Boolean,
    selectionBrush: Brush,
    itemGroup: ItemGroup,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier.height(48.dp).fillMaxWidth(),
    shape = MaterialTheme.shapes.large
) {
    val selectionBackgroundAlpha by
        animateFloatAsState(if (isSelected) 0.5f else 0f)
    Box(Modifier
        .fillMaxSize()
        .alpha(selectionBackgroundAlpha)
        .background(selectionBrush, MaterialTheme.shapes.large))

    // TODO: Add string resources
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = itemGroup.name,
             Modifier.padding(start = 16.dp).weight(1f))
        AmountIndicator(
            icon = Icons.Default.ShoppingCart,
            amount = itemGroup.shoppingListItemCount,
            contentDescription = "shopping list item count")
        Spacer(Modifier.width(8.dp))
        AmountIndicator(
            painter = painterResource(R.drawable.inventory_icon),
            amount = itemGroup.shoppingListItemCount,
            contentDescription = "shopping list item count")

        var showingOptionsMenu by remember { mutableStateOf(false) }
        IconButton(onClick = { showingOptionsMenu = true }) {
            Icon(Icons.Default.MoreVert, "more options for item group ${itemGroup.name}")

            DropdownMenu(
                expanded = showingOptionsMenu,
                onDismissRequest = { showingOptionsMenu = false },
            ) {
                DropdownMenuItem(onClick = onDeleteRequest) {
                    Text(stringResource(R.string.delete_description))
                }
            }
        }
    }
}

@Composable fun ItemGroupViewPreview(darkTheme: Boolean) =
    BootyCrateTheme(darkTheme) {
        var isSelected by remember { mutableStateOf(false) }
        val color1 = MaterialTheme.colors.primary
        val color2 = MaterialTheme.colors.secondary
        val brush = remember { Brush.horizontalGradient(listOf(color1, color2)) }
        val itemGroup = remember {
            ItemGroup(name = "Item group 1",
                      shoppingListItemCount = 3,
                      inventoryItemCount = 12)
        }
        ItemGroupView(
            isSelected = isSelected,
            selectionBrush = brush,
            itemGroup = itemGroup,
            onDeleteRequest = {},
            modifier = Modifier.clickable { isSelected = !isSelected })
    }

@Preview @Composable fun LightItemGroupViewPreview() = ItemGroupViewPreview(false)
@Preview @Composable fun DarkItemGroupViewPreview() = ItemGroupViewPreview(true)