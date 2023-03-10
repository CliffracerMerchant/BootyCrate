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
import com.cliffracertech.bootycrate.utils.StringResource
import com.cliffracertech.bootycrate.utils.Text

/** A dialog button row with a cancel and an ok button at its end. */
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

/** ConfirmatoryDialogState's subclasses represent the possible
 * states for a confirmatory dialog: [NotShowing] and [Showing]. */
sealed class ConfirmatoryDialogState {
    /** The dialog is not being shown. */
    object NotShowing: ConfirmatoryDialogState()

    /** The dialog is being shown. [onCancel] and [onConfirm] describe
     * the callbacks that should be invoked when the cancel and confirm
     * buttons of the confirmatory dialog are clicked. [message] and the
     * optional [title] are [StringResource]s that become the body text
     * and title of the dialog when resolved. */
    class Showing(
        val message: StringResource,
        val onCancel: () -> Unit,
        val onConfirm: () -> Unit,
        val title: StringResource? = null,
    ) : ConfirmatoryDialogState()
}

/**
 * A simple confirmatory dialog with cancel and ok buttons.
 *
 * @param state The [ConfirmatoryDialogState.Showing] instance
 *     that contains the dialog's state and callbacks
 * @param modifier The [Modifier] to use for the root layout
 */
@Composable fun ConfirmDialog(
    state: ConfirmatoryDialogState.Showing,
    modifier: Modifier = Modifier,
) = AlertDialog(
    modifier = modifier,
    onDismissRequest = state.onCancel,
    title = state.title?.let {{ Text(it) }},
    text = { Text(state.message) },
    buttons = {
        CancelOkButtonRow(
            onCancelClick = state.onCancel,
            onOkClick = state.onConfirm)
    })

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
     * @param title A [StringResource] that becomes the title of the
     *     dialog, if any, when resolved
     */
    class Showing(
        val currentNameProvider: () -> String,
        val messageProvider: () -> Validator.Message?,
        val onNameChange: (String) -> Unit,
        val onCancel: () -> Unit,
        val onConfirm: () -> Unit,
        val title: StringResource? = null,
    ): NameDialogState()
}

/**
 * A dialog with a text field to name items.
 *
 * @param state A [NameDialogState.Showing] instance that contains
 *     state providers and callbacks for dialog interactions
 * @param modifier The [Modifier] to use for the root layout
 */
@Composable fun NameDialog(
    state: NameDialogState.Showing,
    modifier: Modifier = Modifier,
) {
    val name = state.currentNameProvider()
    val message = state.messageProvider()

    AlertDialog(
        modifier = modifier,
        onDismissRequest = state.onCancel,
        title = state.title?.let {{ Text(it) }},
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