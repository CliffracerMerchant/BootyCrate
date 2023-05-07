/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.StringResource
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A manager of messages to be displayed to the user, e.g. through a SnackBar.
 *
 * New messages can be posted using the postMessage function. MessageHandler
 * users can listen to the [SharedFlow] member [messages] for new messages. The
 * function [postItemsDeletedMessage] is provided for convenience for the common
 * use case of showing an X item(s) deleted message after items are deleted
 * from a list, along with an undo action.
 */
@ActivityRetainedScoped
class MessageHandler @Inject constructor() {
    /**
     * A message to be displayed to the user.
     *
     * @param duration A [SnackbarDuration] describing how long the snackbar should be shown for
     * @param stringResource A StringResource that, when resolved, will be the text of the message
     * @param actionStringResource A nullable StringResource that, when resolved, will be the text
     * of the message action, if any
     * @param onActionClick The callback that will be invoked if the message action is clicked
     * @param onTimeout The callback that will be invoked when the message times out
     */
    class Message(
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val stringResource: StringResource,
        val actionStringResource: StringResource? = null,
        val onActionClick: (() -> Unit)? = null,
        val onTimeout: (() -> Unit)? = null)

    private val _messages = MutableSharedFlow<Message>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages = _messages.asSharedFlow()

    /** Post the message described by the parameters to the message queue. */
    fun postMessage(
        stringResource: StringResource,
        actionStringResource: StringResource? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onActionClick: (() -> Unit)? = null,
        onTimeout: (() -> Unit)? = null
    ) = _messages.tryEmit(Message(
            duration, stringResource,
            actionStringResource,
            onActionClick, onTimeout))

    private var totalDeletedItemCount: Int = 0
    /**
     * Post a message for the common use case of deleting items in a list while
     * providing an undo action. If postItemsDeletedMessage is called repeatedly
     * before the message is dismissed, the count of deleted items will continue
     * to accumulate.
     *
     * @param count The number of items that were deleted in the deletion that
     * led to this call of postItemsDeletedMessage. Callers that make successive
     * calls to postItemsDeletedMessage do not need to accumulate this number
     * manually, as this is done automatically if a new call is made before the
     * previous message is dismissed.
     * @param onUndo The callback that will be invoked if the undo action is performed
     * @param onTimeout The callback that will be invoked when the message times out
     */
    fun postItemsDeletedMessage(
        count: Int,
        onUndo: () -> Unit,
        onTimeout: (() -> Unit)? = null
    ) {
        totalDeletedItemCount += count
        postMessage(
            stringResource = StringResource(
                R.string.delete_snackbar_text, totalDeletedItemCount),
            actionStringResource = StringResource(R.string.undo_description),
            duration = SnackbarDuration.Short,
            onActionClick = {
                totalDeletedItemCount = 0
                onUndo()
            }, onTimeout = {
                totalDeletedItemCount = 0
                onTimeout?.invoke()
            })
    }
}

/** Show snack bars to display the [MessageHandler.Message]s emitted from [messages]. */
@Composable fun SnackbarHostState.ShowSnackBarsFor(
    messages: Flow<MessageHandler.Message>
) {
    val context = LocalContext.current
    // To be able to distinguish between snackbar timeouts and a new
    // snackbar replacing the old one, we cancel the snackbar result
    // job instead of calling currentSnackbarData?.dismiss()
    var snackbarResultJob: Job? = remember { null }
    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarResultJob?.cancel()
            snackbarResultJob = launch {
                val result = showSnackbar(
                    message = message.stringResource.resolve(context),
                    actionLabel = message.actionStringResource?.resolve(context)
                        ?: context.getString(R.string.dismiss_description).uppercase(),
                    duration = message.duration)
                when (result) {
                    SnackbarResult.ActionPerformed -> message.onActionClick?.invoke()
                    SnackbarResult.Dismissed ->       message.onTimeout?.invoke()
                }
            }
        }
    }
}