/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.springStiffness
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme
import kotlin.math.ceil

/** Compose the contents of [content] while being provided with the
* unconstrained [Dp] dimensions of the content provided in [measurable]. */
@Composable fun MeasureUnconstrainedViewSize(
    measurable: @Composable () -> Unit,
    content: @Composable (Dp, Dp) -> Unit,
) = SubcomposeLayout { constraints ->
    val (measuredWidth, measuredHeight) = run {
        val size = subcompose("measurable", measurable)[0]
            .measure(Constraints())
        size.width.toDp() to size.height.toDp()
    }
    val contentPlaceable = subcompose("content") {
        content(measuredWidth, measuredHeight)
    }[0].measure(constraints)
    layout(contentPlaceable.width, contentPlaceable.height) {
        contentPlaceable.place(0, 0)
    }
}

/**
 * A text field that toggles between an unconstrained size when [readOnly] is
 * true, and a minimum touch target size when [readOnly] is false.
 *
 * @param text The text that will be displayed
 * @param onTextChange The callback that will be invoked when the user attempts
 *     to change the [text] value through input
 * @param modifier The [Modifier] that will be used for the text field
 * @param tint The tint that will be used for the text cursor
 * @param readOnly Whether or not the text field will prevent editing of the
 *     text and allow its size to fall below minimum touch target sizes
 * @param animationProgressProvider A lambda that returns the progress of a
 *     transition between values of readOnly. The returned value should be
 *     in the range of [0f, 1f], with 0f indicating that readOnly is false,
 *     and 1f indicating that readOnly is true.
 * @param textStyle The [TextStyle] that will be used for the text
*/
@Composable fun TextFieldEdit(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    readOnly: Boolean = true,
    animationProgressProvider: () -> Float = { if (readOnly) 0f else 1f },
    textStyle: TextStyle = LocalTextStyle.current,
) = Box(modifier, Alignment.CenterStart) {

    val color = LocalContentColor.current
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.drawBehind {
            val animationProgress = animationProgressProvider()
            if (animationProgress == 0f)
                return@drawBehind
            drawLine(
                color = color,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx(),
                alpha = animationProgressProvider())
        }, readOnly = readOnly,
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = remember(tint) { SolidColor(tint) })
}

/**
* An editor for an Int [amount] that displays decrease and increase buttons on
* either side of the amount, and allows direct keyboard editing of the value
* when [isEditableByKeyboard] is true. [decreaseDescription] and [increaseDescription]
* will be used as the content descriptions for the decrease and increase buttons,
* respectively, while [tint] will be used to tint the text cursor when the amount
* is being edited via the keyboard. An attempt to change the amount either by
* keyboard or the buttons will cause [onAmountChangeRequest] to be invoked.
*/
@Composable fun AmountEdit(
    amount: Int,
    isEditableByKeyboard: Boolean,
    tint: Color,
    decreaseDescription: String,
    increaseDescription: String,
    onAmountChangeRequest: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = MeasureUnconstrainedViewSize(measurable = {
    BasicTextField(
        value = amount.toString(),
        onValueChange = {},
        modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Max),
        textStyle = MaterialTheme.typography.h5.copy(textAlign = TextAlign.Center),
        singleLine = true)
}) { valueWidth, valueHeight ->
    val valueEditMinWidth = (if (isEditableByKeyboard) 48 else 0).dp
    val width = maxOf(valueWidth, valueEditMinWidth) + 96.dp
    val height = maxOf(valueHeight, 48.dp)

    Box(modifier = modifier
        .animateContentSize(spring(stiffness = springStiffness))
        .size(width, height)
    ) {
        IconButton(
            onClick = { onAmountChangeRequest(amount - 1) },
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(painterResource(R.drawable.minus_icon), decreaseDescription)
        }
        IconButton(
            onClick = { onAmountChangeRequest(amount + 1) },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(painterResource(R.drawable.plus_icon), increaseDescription)
        }
        BasicTextField(
            value = amount.toString(),
            onValueChange = { onAmountChangeRequest(it.toInt()) },
            modifier = Modifier.align(Alignment.Center),
            readOnly = !isEditableByKeyboard,
            textStyle = MaterialTheme.typography.h5.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = remember(tint) { SolidColor(tint) },
            decorationBox = { valueDisplay ->
                Box(Modifier.animateContentSize(), Alignment.Center) {
                    valueDisplay()
                    AnimatedVisibility(
                        visible = isEditableByKeyboard,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(valueEditMinWidth),
                        enter = fadeIn(), exit = fadeOut(),
                    ) {
                        Divider(Modifier, LocalContentColor.current, 1.dp)
                    }
                }
            })
    }
}

@Preview @Composable
fun AmountEditPreview() = BootyCrateTheme {
    Row {
        var isEditable by remember { mutableStateOf(false) }
        var amount by remember { mutableStateOf(2) }
        AmountEdit(
            amount = amount,
            tint = Color.Red,
            isEditableByKeyboard = isEditable,
            decreaseDescription = "",
            increaseDescription = "",
            onAmountChangeRequest = { amount = it },
            modifier = Modifier.background(MaterialTheme.colors.surface,
                                           MaterialTheme.shapes.small))
        IconButton({ isEditable = !isEditable }) {
            Icon(Icons.Default.Edit, "")
        }
    }
}

/**
 * An icon button that animates between edit and collapse icons.
 *
 * @param onClick The callback that will be invoked when the button is clicked
 * @param isEditable A method that will return whether or not the item the
 *     button is being used for is editable. When isEditable returns true,
 *     the collapse icon will be shown. When is returns false, the edit icon
 *     will be shown.
 * @param modifier The [Modifier] to use for the button
 * @param itemName The name of the item whose editable state is manipulated
 *     by the button. This is used for the on click labels for the button
 *     for accessibility purposes.
 */
@Composable fun AnimatedEditToCloseButton(
    onClick: () -> Unit,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    itemName: String,
) = IconButton(onClick, modifier) {
    val vector = AnimatedImageVector.animatedVectorResource(
        R.drawable.animated_edit_to_collapse)
    val painter = rememberAnimatedVectorPainter(vector, isEditable)
    val desc = stringResource(
        if (isEditable) R.string.collapse_item_description
        else            R.string.edit_item_description, itemName)
    Icon(painter, desc)
}

/**
* A grid arrangement of color options to choose from.
 *
* @param modifier The [Modifier] that will be used for the picker
* @param currentColor The [Color] in [colors] that will be
*     identified as the currently picked color by a checkmark
* @param colors A [List] containing all of the [Color] options to display
* @param colorDescriptions A [List] the same size as [colors]
*     containing [String] descriptions for each of the color options.
* @param onColorClick The callback that will be invoked when a color
*     option is chosen. Both the index of and the [Color] value of
*     the clicked option are provided.
*/
@Composable fun ColorPicker(
    modifier: Modifier = Modifier,
    currentColor: Color? = null,
    colors: List<Color>,
    colorDescriptions: List<String>,
    onColorClick: (Int, Color) -> Unit,
) = BoxWithConstraints {
    val maxColorsPerRow = (maxWidth / 48.dp).toInt()
    val rows = ceil(colors.size.toFloat() / maxColorsPerRow).toInt()
    val columns = colors.size / rows

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.height(48.dp * rows),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly,
        userScrollEnabled = false
    ) {
        require(colors.size == colorDescriptions.size)
        itemsIndexed(colors) { index, color ->
            val label = stringResource(R.string.color_picker_option_description,
                                       colorDescriptions[index])
            Box(Modifier
                .requiredSize(48.dp)
                .clip(CircleShape)
                .clickable(
                    role = Role.Button,
                    onClick = { onColorClick(index, color) },
                    onClickLabel = label)
                .padding(10.dp)
                .background(color, CircleShape)
            ) {
                if (color == currentColor)
                    // The check mark's offset makes it appear more centered
                    Icon(Icons.Default.Check, null, Modifier.offset(1.dp, 2.dp))
            }
        }
    }
}

/** A [ColorPicker] geared towards picking a [ListItem.ColorGroup] for a [ListItem]. */
@Composable fun ListItemColorGroupPicker(
    modifier: Modifier = Modifier,
    currentColorGroup: ListItem.ColorGroup,
    onColorGroupClick: (ListItem.ColorGroup) -> Unit
) {
    val colors = ListItem.ColorGroup.colors()
    ColorPicker(
        modifier = modifier,
        currentColor = colors[currentColorGroup.ordinal],
        colors = colors,
        colorDescriptions = ListItem.ColorGroup.descriptions(),
        onColorClick = { index, _ ->
            val groups = ListItem.ColorGroup.values()
            val colorGroup = groups.getOrElse(index) { groups.first() }
            onColorGroupClick(colorGroup)
        })
}

@Preview @Composable
fun ColorPickerPreview() = BootyCrateTheme {
    val colors = ListItem.ColorGroup.colors()
    var currentColor by remember { mutableStateOf(colors.first()) }
    val descriptions = ListItem.ColorGroup.descriptions()
    Surface(shape = MaterialTheme.shapes.large) {
        ColorPicker(
            currentColor = currentColor,
            colors = colors,
            colorDescriptions = descriptions,
        ) { _, color -> currentColor = color }
    }
}