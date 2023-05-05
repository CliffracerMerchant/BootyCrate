/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.itemlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.defaultSpring
import com.cliffracertech.bootycrate.model.database.ListItem
import com.cliffracertech.bootycrate.tweenDuration
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme
import com.cliffracertech.bootycrate.utils.SimpleIconButton
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class SwipeToDeleteState { Centered, SwipedLeft, SwipedRight;
    companion object {
        fun anchors(fullWidth: Float) = mapOf (
            -fullWidth to SwipedLeft,
            0f         to Centered,
            fullWidth  to SwipedRight)
    }
}

/**
 * Adds a swipeable background.
 *
 * horizontalSwipeToDeleteSurface adds a horizontally swipeable background
 * matching [backgroundShape] and [backgroundColor]. A delete icon and another
 * background with the same shape, but with a color matching the [MaterialTheme]
 * error color, will show underneath the primary background as it is being
 * swiped. Any desired horizontal padding should be added using the
 * [horizontalContentPadding] parameter, rather than with a normal padding
 * modifier. This ensures that the delete background can appear up to the edges
 * of the container. The swipeableState anchors must also be provided through
 * the [anchors] parameter. After an item is swiped and following a short fade
 * out, the [onSwipe] callback will be executed.
 */
fun Modifier.horizontalSwipeToDeleteSurface(
    backgroundShape: CornerBasedShape,
    backgroundColor: Color,
    horizontalContentPadding: Dp,
    anchors: Map<Float, SwipeToDeleteState>,
    onSwipe: () -> Unit,
) = composed {
    val context = LocalContext.current
    val deleteBackgroundColor = MaterialTheme.colors.error
    val iconColor = LocalContentColor.current
    val deleteIcon = remember(iconColor) {
        ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp)
            ?.also { it.setTint(iconColor.toArgb())}
    }

    var swiped by remember { mutableStateOf(false) }
    val swipeableState = rememberSwipeableState(
        initialValue = SwipeToDeleteState.Centered,
        animationSpec = defaultSpring(),
        confirmStateChange = {
            if (it != SwipeToDeleteState.Centered)
                swiped = true
            true
        })
    val alpha by animateFloatAsState(
        targetValue = if (swiped) 0f else 1f,
        animationSpec = tween(tweenDuration, 50),
        label = "swiped item fade out",
        finishedListener = {
            if (it == 0f) onSwipe()
        })

    fillMaxWidth()
    .drawBehind {
        val swipingRight = if (swipeableState.offset.value == 0f)
                               return@drawBehind
                           else swipeableState.offset.value > 0f

        val cornerRadiusPx = backgroundShape.topStart.toPx(size, this)
        val backgroundMargin = horizontalContentPadding.roundToPx()
        val width = abs(swipeableState.offset.value) + 2 * cornerRadiusPx
        val startX = if (swipingRight) backgroundMargin.toFloat()
                     else size.width - backgroundMargin - width
        drawRoundRect(
            color = deleteBackgroundColor,
            topLeft= Offset(startX, 0f),
            size = Size(width, size.height),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            alpha = alpha)

        deleteIcon?.apply {
            bounds.top = ((size.height - intrinsicHeight) / 2f).roundToInt()
            bounds.bottom = bounds.top + intrinsicHeight
            val iconMargin = backgroundMargin + 12.dp.roundToPx()

            val canvasWidth = size.width.roundToInt()
            bounds.left = if (swipingRight) iconMargin
                          else canvasWidth - iconMargin - intrinsicWidth
            bounds.right = bounds.left + intrinsicWidth
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            drawIntoCanvas { draw(it.nativeCanvas) }
        }
    }.graphicsLayer {
        translationX = swipeableState.offset.value
        this.alpha = alpha
    }.padding(horizontal = horizontalContentPadding)
    .background(backgroundColor, backgroundShape)
    .swipeable(swipeableState, anchors, Orientation.Horizontal,
        thresholds = { _, _ -> FractionalThreshold(0.33f) },
        velocityThreshold = Float.POSITIVE_INFINITY.dp)
    .clip(backgroundShape)
}

/**
 * A text field that toggles between an unconstrained size when [editable]
 * is false, and a minimum touch target size when [editable] is true.
 *
 * @param text The text that will be displayed
 * @param onTextChange The callback that will be invoked when the user attempts
 *     to change the [text] value through input
 * @param modifier The [Modifier] that will be used for the text field
 * @param tint The tint that will be used for the text cursor
 * @param editable Whether or not the text field will allow keyboard
 *     focus of the text and enforce its minimum touch target size
 * @param editableTransitionProgressGetter A lambda that returns the
 *     progress of a transition between values of isEditable. The returned
 *     value should be in the range of [0f, 1f], with 0f indicating that
 *     isEditable is false, and 1f indicating that isEditable is true.
 * @param textStyle The [TextStyle] that will be used for the text
 */
@Composable fun ListItemTextField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    editable: Boolean = true,
    editableTransitionProgressGetter: () -> Float = { if (editable) 1f else 0f },
    textStyle: TextStyle = LocalTextStyle.current,
) = Box(modifier, Alignment.CenterStart) {

    val color = LocalContentColor.current
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val animationProgress = editableTransitionProgressGetter()
                if (animationProgress != 0f)
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                        alpha = animationProgress)
            },
        enabled = editable,
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = remember(tint) { SolidColor(tint) })
}

/**
 * An editor for an Int [amount] that displays decrease and increase buttons on
 * either side of the amount, and allows direct keyboard editing of the value
 * when [focusable] is true. [decreaseDescription] and [increaseDescription]
 * will be used as the content descriptions for the decrease and increase buttons,
 * respectively, while [tint] will be used to tint the text cursor when the amount
 * is being edited via the keyboard. An attempt to change the amount either by
 * keyboard or the buttons will cause [onAmountChangeRequest] to be invoked.
 *
 * The optional [focusableTransitionProgressGetter] should return the progress
 * of a transition between [focusable] states in the range of [0f, 1f]
 * (corresponding to the unfocusable and focusable states, respectively). If
 * provided, this will allow the AmountEdit to smoothly animate between the
 * two states.
 */
@Composable fun AmountEdit(
    sizes: AmountEditSizes,
    amount: Int,
    focusable: Boolean,
    tint: Color,
    decreaseDescription: String,
    increaseDescription: String,
    onAmountChangeRequest: (Int) -> Unit,
    modifier: Modifier = Modifier,
    focusableTransitionProgressGetter: () -> Float =
        { if (focusable) 1f else 0f },
) {
    val color = LocalContentColor.current

    Box(modifier = modifier.size(sizes.width(focusable), sizes.height)) {
        BasicTextField(
            value = amount.toString(),
            onValueChange = { onAmountChangeRequest(it.toInt()) },
            modifier = Modifier
                .align(Alignment.Center)
                .width(sizes.valueWidth(focusable))
                .graphicsLayer {
                    val interp = focusableTransitionProgressGetter()
                    translationX = sizes.valueXOffset(focusable, interp).toPx()
                }.drawBehind {
                    val interp = focusableTransitionProgressGetter()
                    if (interp == 0f)
                        return@drawBehind
                    val width = (24 + 24 * interp).dp.toPx()
                    val startX = (size.width - width) / 2f
                    val endX = startX + width
                    drawLine(
                        color = color,
                        start = Offset(startX, size.height),
                        end = Offset(endX, size.height),
                        strokeWidth = 1.dp.toPx(),
                        alpha = interp)
                },
            enabled = focusable,
            textStyle = sizes.textStyle,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = remember(tint) { SolidColor(tint) })

        SimpleIconButton(
            onClick = { onAmountChangeRequest(amount - 1) },
            modifier = Modifier.align(Alignment.CenterStart),
            imageVector = Icons.Default.RemoveCircleOutline,
            description = decreaseDescription)

        SimpleIconButton(
            onClick = { onAmountChangeRequest(amount + 1) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    val interp = focusableTransitionProgressGetter()
                    translationX = sizes.increaseButtonXOffset(focusable, interp).toPx()
                },
            imageVector = Icons.Default.AddCircleOutline,
            description = increaseDescription)
    }
}

@Preview @Composable
fun AmountEditPreview() = BootyCrateTheme {
    Row {
        val density = LocalDensity.current
        val fontFamilyResolver = LocalFontFamilyResolver.current
        val textStyle = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center)
        val sizes = remember {
            AmountEditSizes(textStyle, fontFamilyResolver, density)
        }
        var amount by remember { mutableStateOf(2) }
        var focusable by remember { mutableStateOf(false) }
        val focusableTransitionProgress by animateFloatAsState(
            targetValue = if (focusable) 1f else 0f,
            animationSpec = defaultSpring())

        AmountEdit(
            sizes = sizes,
            amount = amount,
            focusable = focusable,
            focusableTransitionProgressGetter = { focusableTransitionProgress },
            tint = Color.Red,
            decreaseDescription = "",
            increaseDescription = "",
            onAmountChangeRequest = { amount = it },
            modifier = Modifier.background(
                MaterialTheme.colors.surface,
                MaterialTheme.shapes.small))
        SimpleIconButton(
            onClick = { focusable = !focusable },
            imageVector = Icons.Default.Edit,
            description = null)
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
            Box(modifier = Modifier
                    .requiredSize(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = { onColorClick(index, color) },
                        onClickLabel = label)
                    .padding(10.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (color == currentColor)
                    Icon(Icons.Default.Check, null)
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
            modifier = Modifier.padding(vertical = 8.dp),
            currentColor = currentColor,
            colors = colors,
            colorDescriptions = descriptions,
        ) { _, color -> currentColor = color }
    }
}