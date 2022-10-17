/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem

/** A checkmark that animates between its unchecked and checked states. */
@Composable fun AnimatedCheckmark(checked: Boolean) {
    val uncheckedToChecked = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_to_checked_checkmark)
    val uncheckedToCheckedPainter = rememberAnimatedVectorPainter(uncheckedToChecked, checked)
    val checkedToUnchecked = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_checked_to_unchecked_checkmark)
    val checkedToUncheckedPainter = rememberAnimatedVectorPainter(checkedToUnchecked, !checked)
    Icon(painter = if (checked) uncheckedToCheckedPainter
                   else         checkedToUncheckedPainter,
         contentDescription = null,
         modifier = Modifier.offset(1.dp, 2.dp))
}

/**
 * A combination tinted checkbox and color indicator.
 *
 * CheckboxAndColorIndicator will appear as a tinted checkbox when
 * [showingCheckbox] is true, or as a tinted circle otherwise. The parameters
 * [checkboxClickLabel] and [colorIndicatorClickLabel] define the click labels
 * that will be used for each state. [checked] indicates the current checked
 * state of the checkbox when [showingCheckbox] is true. The parameters
 * [onCheckboxClick] and [onColorIndicatorClick] define the callbacks that will
 * be invoked when [showingCheckbox] is true or false, respectively.
 */
@Composable fun CheckboxAndColorIndicator(
    showingCheckbox: Boolean,
    tint: Color,
    checked: Boolean,
    checkboxClickLabel: String,
    onCheckboxClick: () -> Unit,
    colorIndicatorClickLabel: String,
    onColorIndicatorClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(modifier
    .minTouchTargetSize()
    .padding(10.dp)
    .clickable(
        role = if (showingCheckbox) Role.Checkbox
               else                 Role.Button,
        onClickLabel = if (!showingCheckbox) colorIndicatorClickLabel
                       else                  checkboxClickLabel,
        onClick = if (showingCheckbox) onCheckboxClick
                  else                 onColorIndicatorClick)
) {
    val uncheckedToCheckedBg = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_to_checked_background)
    val uncheckedToCircle = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_background_to_circle)
    val checkedToCircle = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_checked_background_to_circle)
    val uncheckedToCheckedBgPainter = rememberAnimatedVectorPainter(
        uncheckedToCheckedBg, checked)
    val uncheckedToCirclePainter = rememberAnimatedVectorPainter(
        uncheckedToCircle, !showingCheckbox)
    val checkedToCirclePainter = rememberAnimatedVectorPainter(
        checkedToCircle, !showingCheckbox)
    Icon(contentDescription = null,
         tint = tint,
         painter = when {
             showingCheckbox -> uncheckedToCheckedBgPainter
             checked ->         checkedToCirclePainter
             else ->            uncheckedToCirclePainter
         })
    AnimatedCheckmark(checked && showingCheckbox)
}

/** An interface containing callbacks for ShoppingListItem related interactions. */
interface ShoppingListItemCallback : ListItemCallback {
    fun onCheckboxClick()
}

/** Return a [ShoppingListItemCallback] implementation using the provided
* lambdas as the implementations for the [ShoppingListItemCallback] methods
* of the same name. */
fun shoppingListItemCallback(
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onColorChangeRequest: (ListItem.Color) -> Unit = {},
    onRenameRequest: (String) -> Unit = {},
    onExtraInfoChangeRequest: (String) -> Unit = {},
    onAmountChangeRequest: (Int) -> Unit = {},
    onEditButtonClick: () -> Unit = {},
    onCheckboxClick: () -> Unit = {}
) = object: ShoppingListItemCallback {
    override fun onClick() = onClick()
    override fun onLongClick() = onLongClick()
    override fun onColorChangeRequest(color: ListItem.Color) = onColorChangeRequest(color)
    override fun onRenameRequest(newName: String) = onRenameRequest(newName)
    override fun onExtraInfoChangeRequest(newExtraInfo: String) = onExtraInfoChangeRequest(newExtraInfo)
    override fun onAmountChangeRequest(newAmount: Int) = onAmountChangeRequest(newAmount)
    override fun onEditButtonClick() = onEditButtonClick()
    override fun onCheckboxClick() = onCheckboxClick()
}

/**
* A visual display of an [ShoppingListItem] that also allows user
* interactions to e.g. change the [ShoppingListItem]'s state.
*
* @param item The [ShoppingListItem] instance whose data is being displayed
* @param isEditable Whether or not the view will display itself in its expanded
*     state intended for editing the [ShoppingListItem]'s state. When
*     [isEditable] is true, the name, extra info, and amount of the item will
*     expand if necessary to meet minimum touch target sizes, and the checkbox
*     will morph to a color indicator that opens the color picker when clicked.
*     The [ShoppingListItem]'s amount's decrease / increase buttons will still
*     invoke their callbacks even when isEditable is false.
* @param callback The ShoppingListItemCallback whose method implementations
*     will be used as the callbacks for user interactions
* @param modifier The [Modifier] that will be used for the root layout
*/
@Composable fun ShoppingListItemView(
    item: ShoppingListItem,
    isEditable: Boolean,
    callback: ShoppingListItemCallback,
    modifier: Modifier = Modifier
) = ListItemView (
    item, isEditable, callback,
    colorIndicator = { showColorPicker ->
        val colors = ListItem.Color.asComposeColors()
        CheckboxAndColorIndicator(
            showingCheckbox = !isEditable,
            tint = colors[item.color],
            checked = item.isChecked,
            checkboxClickLabel = stringResource(
                R.string.item_checkbox_description, item.name),
            onCheckboxClick = callback::onCheckboxClick,
            colorIndicatorClickLabel = stringResource(
                R.string.edit_item_color_description, item.name),
            onColorIndicatorClick = showColorPicker,
            modifier = modifier)
    }, modifier,
) {}

@Preview @Composable
fun ShoppingListItemViewPreview() = BootyCrateTheme {
    var name by remember { mutableStateOf("Test item") }
    var extraInfo by remember { mutableStateOf("Test extra info") }
    var colorIndex by remember { mutableStateOf(ListItem.Color.Orange.ordinal) }
    var amount by remember { mutableStateOf(5) }
    var isEditable by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }
    val item by derivedStateOf {
        ShoppingListItem(1, name, extraInfo, colorIndex, amount, isChecked = isChecked)
    }
    val callback = remember { shoppingListItemCallback(
        onColorChangeRequest = { colorIndex = it.ordinal },
        onRenameRequest = { name = it },
        onExtraInfoChangeRequest = { extraInfo = it },
        onAmountChangeRequest = { amount = it },
        onEditButtonClick = { isEditable = !isEditable },
        onCheckboxClick = { isChecked = !isChecked })
    }
    ShoppingListItemView(item, isEditable, callback)
}