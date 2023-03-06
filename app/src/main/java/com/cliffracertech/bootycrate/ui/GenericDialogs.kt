/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cliffracertech.bootycrate.model.database.Validator

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
 * A simple confirmatory dialog with cancel and ok buttons.
 *
 * @param modifier The [Modifier] to use for the root layout
 * @param title A nullable [String] that will be used as the title of
 *     the dialog. The title will not be displayed if this is null.
 * @param message The [String] message to be displayed
 * @param onDismissRequest The callback that will be invoked if the
 *     user tries to dismiss the dialog via the cancel button, the
 *     back button/gesture, or by clicking outside the dialog
 */
@Composable fun ConfirmDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) = AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = title?.let {{ Text(it) }},
    text = { Text(message) },
    buttons = {
        CancelOkButtonRow(
            onCancelClick = onDismissRequest,
            onOkClick = onConfirm)
    }
)

/**
 * The state for a naming/renaming dialog, which will be either [NotShowing]
 * or [Showing]. [Showing] contains getters for the visible state (e.g. the
 * current name) and callbacks for events (e.g. a cancel button being clicked).
 */
sealed class NameDialogState {
    /** The rename dialog is not showing. */
    object NotShowing : NameDialogState()

    /**
     * The rename dialog is showing.
     *
     * @param currentNameProvider A getter that will return the currently
     *     proposed name when invoked
     * @param messageProvider A getter that will return a [Validator.Message]
     *     regarding the current name, if one is required. If the returned
     *     value message is a [Validator.Message.Error], the ok button of
     *     the dialog should be disabled.
     * @param onNameChange The callback that will be invoked when the user
     *     tries to change the name to a new value
     * @param onCancel The callback that will be invoked when the user
     *     tries to dismiss the rename dialog
     * @param onConfirm The callback that will be invoked when the user
     *     tries to confirm the currently input name
     */
    class Showing(
        val currentNameProvider: () -> String,
        val messageProvider: () -> Validator.Message?,
        val onNameChange: (String) -> Unit,
        val onCancel: () -> Unit,
        val onConfirm: () -> Unit,
    ): NameDialogState()
}

/**
 * A dialog with a text field to name items.
 *
 * @param title The dialog's title
 * @param state A [NameDialogState.Showing] instance that contains
 *     state providers and callbacks for dialog interactions
 * @param modifier The [Modifier] to use for the root layout
 */
@Composable fun NameDialog(
    title: String,
    state: NameDialogState.Showing,
    modifier: Modifier = Modifier,
) {
    val name = state.currentNameProvider()
    val message = state.messageProvider()

    AlertDialog(
        modifier = modifier,
        onDismissRequest = state.onCancel,
        title = { Text(title) },
        text = { Column {
            TextField(
                onValueChange = state.onNameChange,
                value = name,
                modifier = Modifier.fillMaxWidth(),
//                    .padding(horizontal = 16.dp),
                isError = message is Validator.Message.Error,
                singleLine = true)
            AnimatedValidatorMessage(message)
        }}, buttons = {
            CancelOkButtonRow(
                okButtonEnabled = message is Validator.Message.Error,
                onCancelClick = state.onCancel,
                onOkClick = state.onConfirm)
        })
}