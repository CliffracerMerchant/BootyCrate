/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.Role.Companion.Checkbox
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.model.database.ShoppingListItem
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme

fun Modifier.minTouchTargetSize() =
    this.sizeIn(minWidth = 48.dp, minHeight = 48.dp)

@Composable fun AnimatedCheckmark(checked: Boolean) {
    val uncheckedToChecked = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_to_checked_checkmark)
    val uncheckedToCheckedPainter = rememberAnimatedVectorPainter(uncheckedToChecked, checked)
    val checkedToUnchecked = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_checked_to_unchecked_checkmark)
    val checkedToUncheckedPainter = rememberAnimatedVectorPainter(checkedToUnchecked, !checked)
    Icon(painter = if (checked) uncheckedToCheckedPainter
                   else         checkedToUncheckedPainter,
         contentDescription = null)
}

@Composable fun AnimatedCheckboxBackground(checked: Boolean, tint: Color) {
    val vector = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_checkbox_unchecked_to_checked_background)
    Icon(painter = rememberAnimatedVectorPainter(vector, checked),
         contentDescription = null, tint = tint)
}

@Composable fun AnimatedCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
    itemName: String,
    tint: Color,
    modifier: Modifier = Modifier,
) = Box(modifier
    .minTouchTargetSize()
    .padding(12.dp)
    .clickable(role = Checkbox, onClick = onClick, onClickLabel =
        stringResource(R.string.item_checkbox_description, itemName))
) {
    AnimatedCheckboxBackground(checked, tint)
    AnimatedCheckmark(checked)
}

@Composable fun TextFieldEdit(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    tint: Color = LocalContentColor.current,
    readOnly: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
) = MeasureUnconstrainedViewSize(viewToMeasure = {
    BasicTextField(
        value = text,
        onValueChange = {},
        modifier = modifier,
        readOnly = readOnly,
        textStyle = textStyle,
        singleLine = true)
}) { _, minHeight ->
    val height = maxOf(minHeight, (if (readOnly) 0 else 48).dp)
    Box(modifier.animateContentSize().height(height)) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.align(Alignment.CenterStart),
            readOnly = readOnly,
            textStyle = textStyle,//.copy(textAlign = TextAlign.Center),
            singleLine = true,
            cursorBrush = remember(tint) { SolidColor(tint) },
        ) { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                innerTextField()
                AnimatedVisibility(
                    visible = !readOnly,
                    modifier = Modifier.align(Alignment.BottomStart),
                    enter = fadeIn(), exit = fadeOut()
                ) { Divider(Modifier, LocalContentColor.current, 1.dp) }
            }
        }
    }
}

@Composable fun MeasureUnconstrainedViewSize(
    viewToMeasure: @Composable () -> Unit,
    content: @Composable (Dp, Dp) -> Unit,
) = SubcomposeLayout { constraints ->
    val (measuredWidth, measuredHeight) = run {
        val size = subcompose("viewToMeasure", viewToMeasure)[0].measure(Constraints())
        size.width.toDp() to size.height.toDp()
    }
    val contentPlaceable = subcompose("content") {
        content(measuredWidth, measuredHeight)
    }[0].measure(constraints)
    layout(contentPlaceable.width, contentPlaceable.height) {
        contentPlaceable.place(0, 0)
    }
}

@Composable fun AmountEdit(
    amount: Int,
    isEditableByKeyboard: Boolean,
    tint: Color,
    amountDescription: String,
    onAmountChangeRequest: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onResize: (width: Dp, height: Dp) -> Unit = { _, _ -> },
) = MeasureUnconstrainedViewSize(viewToMeasure = {
    BasicTextField(
        value = amount.toString(),
        onValueChange = {},
        modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Max),
        textStyle = MaterialTheme.typography.h5.copy(textAlign = TextAlign.Center),
        singleLine = true)
}) { valueWidth, valueHeight ->
    val valueEditMinWidth = (if (isEditableByKeyboard) 58 else 10).dp
    val width = maxOf(valueWidth, valueEditMinWidth) + 96.dp
    val height = maxOf(valueHeight, 48.dp)
    LaunchedEffect(width, height) { onResize(width, height) }

    Box(modifier.animateContentSize().size(width, height)) {
        val decreaseDesc = stringResource(R.string.item_amount_decrease_description, amountDescription)
        IconButton(
            onClick = { onAmountChangeRequest(amount - 1) },
            modifier = Modifier.align(Alignment.CenterStart),
        ) { Icon(painterResource(R.drawable.minus_icon), decreaseDesc) }

        val increaseDesc = stringResource(R.string.item_amount_increase_description, amountDescription)
        IconButton(
            onClick = { onAmountChangeRequest(amount + 1) },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) { Icon(painterResource(R.drawable.plus_icon), increaseDesc) }

        BasicTextField(
            value = amount.toString(),
            onValueChange = { onAmountChangeRequest(it.toInt()) },
            modifier = Modifier.align(Alignment.Center),
            readOnly = !isEditableByKeyboard,
            textStyle = MaterialTheme.typography.h5.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = remember(tint) { SolidColor(tint) },
            decorationBox = { value ->
                Box(Modifier.animateContentSize(), contentAlignment = Alignment.Center) {
                    value()
                    AnimatedVisibility(
                        visible = isEditableByKeyboard,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(valueEditMinWidth),
                        enter = fadeIn(), exit = fadeOut()
                    ) { Divider(Modifier, LocalContentColor.current, 1.dp) }
                }
            })
    }
}

@Preview @Composable
fun AmountEditPreview() = BootyCrateTheme {
    Row {
        var isEditable by remember { mutableStateOf(false) }
        var amount by remember { mutableStateOf(22) }
        AmountEdit(
            amount = amount,
            tint = Color.Red,
            isEditableByKeyboard = isEditable,
            amountDescription = "",
            onAmountChangeRequest = { amount = it },
            modifier = Modifier.background(MaterialTheme.colors.surface,
                                           MaterialTheme.shapes.small))
        IconButton({ isEditable = !isEditable }) {
            Icon(Icons.Default.Edit, "")
        }
    }
}

/** An interface containing callbacks for ListItem related interactions. */
interface ListItemCallback {
    /** The callback that will be invoked when the item is clicked. */
    fun onClick()
    /** The callback that will be invoked when the item is long clicked. */
    fun onLongClick()
    /** The callback that will be invoked when the item is swiped. */
    fun onSwipe()
    /** The callback that will be invoked when the item's
     * color has been requested to be changed to [color]. */
    fun onColorChangeRequest(color: ListItem.Color)
    /** The callback that will be invoked when the item's
     * name has been requested to be changed to [newName]*/
    fun onRenameRequest(newName: String)
    /** The callback that will be invoked when the item's extraInfo
     * has been requested to be changed to [newExtraInfo]*/
    fun onExtraInfoChangeRequest(newExtraInfo: String)
    /**  The callback that will be invoked when the item's amount
     * has been requested to be changed to [newAmount]*/
    fun onAmountChangeRequest(newAmount: Int)
    /** The callback that will be invoked when the item's edit is clicked. */
    fun onEditButtonClick()
}

fun listItemCallback(
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onSwipe: () -> Unit = {},
    onColorChangeRequest: (ListItem.Color) -> Unit = {},
    onRenameRequest: (String) -> Unit = {},
    onExtraInfoChangeRequest: (String) -> Unit = {},
    onAmountChangeRequest: (Int) -> Unit = {},
    onEditButtonClick: () -> Unit = {}
) = object : ListItemCallback {
    override fun onClick() = onClick()
    override fun onLongClick() = onLongClick()
    override fun onSwipe() = onSwipe()
    override fun onColorChangeRequest(color: ListItem.Color) = onColorChangeRequest(color)
    override fun onRenameRequest(newName: String) = onRenameRequest(newName)
    override fun onExtraInfoChangeRequest(newExtraInfo: String) = onExtraInfoChangeRequest(newExtraInfo)
    override fun onAmountChangeRequest(newAmount: Int) = onAmountChangeRequest(newAmount)
    override fun onEditButtonClick() = onEditButtonClick()
}

@Composable fun ListItemView(
    item: ListItem,
    isEditable: Boolean,
    callback: ListItemCallback,
    modifier: Modifier = Modifier,
    colorIndicator: @Composable (showColorPicker: () -> Unit) -> Unit,
) = Surface(modifier.animateContentSize(), MaterialTheme.shapes.large) {
    val colors = ListItem.Color.asComposeColors()
    val color = remember(item.color) {
        colors.getOrElse(item.color) { Color.Red }
    }
    var showColorPicker by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showColorPicker,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
        transitionSpec = { scaleIn(initialScale = 0.9f) + fadeIn() with
                           scaleOut(targetScale = 0.9f) + fadeOut() }
    ) { showingColorPicker ->
        if (showingColorPicker) ColorPicker(
            currentColor = color,
            colors = colors,
            colorDescriptions = ListItem.Color.descriptions(),
            onColorClick = { index, _ ->
                val listItemColor = ListItem.Color.values().getOrElse(index) { ListItem.Color.Red }
                callback.onColorChangeRequest(listItemColor)
                showColorPicker = false
            })
        else Row(verticalAlignment = Alignment.CenterVertically) {
            val expansionTransition = updateTransition(isEditable, "item expand/collapse")
            colorIndicator { showColorPicker = true }
            Column(Modifier.weight(1f)) {
                TextFieldEdit(
                    text = item.name,
                    onTextChange = callback::onRenameRequest,
                    tint = color,
                    readOnly = !isEditable,
                    textStyle = MaterialTheme.typography.body1)
                TextFieldEdit(
                    text = item.extraInfo,
                    onTextChange = callback::onExtraInfoChangeRequest,
                    tint = color,
                    readOnly = !isEditable,
                    textStyle = MaterialTheme.typography.subtitle1)
            }
            Box(Modifier.animateContentSize()) {
                val amountEditEndPadding by
                expansionTransition.animateDp(label = "amountEditSlideAnim") { isEditable ->
                    if (isEditable) 0.dp else 48.dp
                }
                val editButtonTopPadding by
                expansionTransition.animateDp(label = "editButtonSlideAnim") { isEditable ->
                    if (isEditable) 48.dp else 0.dp
                }
                AmountEdit(
                    amount = item.amount,
                    isEditableByKeyboard = isEditable,
                    tint = color,
                    onAmountChangeRequest = callback::onAmountChangeRequest,
                    amountDescription = item.name,
                    modifier = Modifier.padding(end = amountEditEndPadding))
                IconButton(
                    onClick = callback::onEditButtonClick,
                    modifier = Modifier.padding(top = editButtonTopPadding)
                                       .align(Alignment.TopEnd)
                ) {
                    val vector = AnimatedImageVector.animatedVectorResource(
                            R.drawable.animated_edit_to_collapse)
                    val painter = rememberAnimatedVectorPainter(vector, isEditable)
                    val desc = stringResource(
                        if (isEditable) R.string.collapse_item_description
                        else            R.string.edit_item_description, item.name)
                    Icon(painter, desc)
                }
            }
        }
    }
}

@Preview @Composable fun ListItemViewPreview() = BootyCrateTheme {
    var name by remember { mutableStateOf("Test item") }
    var extraInfo by remember { mutableStateOf("Test extra info") }
    var colorIndex by remember { mutableStateOf(ListItem.Color.Orange.ordinal) }
    var amount by remember { mutableStateOf(5) }
    var isEditable by remember { mutableStateOf(false) }
    val item by derivedStateOf { ShoppingListItem(1, name, extraInfo, colorIndex, amount) }
    val callback = remember { listItemCallback(
        onColorChangeRequest = { colorIndex = it.ordinal },
        onRenameRequest = { name = it },
        onExtraInfoChangeRequest = { extraInfo = it },
        onAmountChangeRequest = { amount = it },
        onEditButtonClick = { isEditable = !isEditable })
    }
    ListItemView(item, isEditable, callback) { showColorPicker ->
        Box(Modifier
            .size(48.dp).padding(10.dp)
            .clickable(onClick = showColorPicker)
            .background(ListItem.Color.values()[item.color].toComposeColor(), CircleShape))
    }
}

@Composable fun ColorIndicator(
    currentColor: Color,
    objectDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(modifier
    .minTouchTargetSize()
    .padding(10.dp)
    .background(currentColor, CircleShape)
    .clickable(
        role = Role.Button,
        onClick = onClick,
        onClickLabel = stringResource(R.string.edit_item_color_description, objectDescription)))

@Composable fun ColorPicker(
    currentColor: Color,
    colors: List<Color>,
    colorDescriptions: List<String>,
    modifier: Modifier = Modifier,
    onColorClick: (Int, Color) -> Unit,
) = LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 48.dp),
    modifier = modifier,//.circularReveal(visible),
    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
    verticalArrangement = Arrangement.SpaceEvenly,
    horizontalArrangement = Arrangement.SpaceEvenly,
) {
    itemsIndexed(items = colors, contentType = { _, _ -> true }) { index, color ->
        Box(Modifier
            .minTouchTargetSize()
            .padding(10.dp)
            .background(color, CircleShape)
            .clickable(
                role = Role.Button,
                onClick = { onColorClick(index, color) },
                onClickLabel = stringResource(R.string.edit_item_color_description, colorDescriptions[index]))
        ) {
            if (color == currentColor)
                Icon(Icons.Default.Check, null, Modifier.offset(x = 1.dp, y = 2.dp))
        }
    }
}