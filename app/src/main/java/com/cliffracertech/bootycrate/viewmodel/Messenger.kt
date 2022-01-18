/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import android.content.Context
import com.cliffracertech.bootycrate.R
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * An manager of messages to be displayed to the user, e.g. through a SnackBar.
 *
 * New messages can be posted using the postMessage function. Messager users
 * can listen to the SharedFlow member messages for new messages. The function
 * postItemsDeletedMessage is provided for convenience for the common use case
 * of showing an X item(s) deleted message after items are deleted from a list,
 * along with an undo action.
 */
@Module @InstallIn(ActivityRetainedComponent::class)
class Messenger @Inject constructor(
    @ActivityContext private val context: Context
) {
    /**
     * A message to be displayed to the user.
     * @param text The text of the message.
     * @param actionText The text of the message action, if any.
     * @param onActionClick The callback that will be invoked if the message action is clicked.
     * @param onDismiss The callback that will be invoked when the message is dismissed. The
     * int parameter will be equal to a value of BaseTransientBottomBar.BaseCallback.DismissEvent.
     */
    data class Message(
        val text: String,
        val actionText: String? = null,
        val onActionClick: (() -> Unit)? = null,
        val onDismiss: ((Int) -> Unit)? = null)

    private val _messages = MutableSharedFlow<Message>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages = _messages.asSharedFlow()

    /** Post the message to the message queue. */
    fun postMessage(message: Message) =  _messages.tryEmit(message)

    /** Post the message described by the parameters to the message queue. */
    fun postMessage(
        text: String,
        actionText: String? = null,
        onActionClick: (() -> Unit)? = null,
        onDismiss: ((Int) -> Unit)? = null
    ) = postMessage(Message(text, actionText, onActionClick, onDismiss))

    private var totalDeletedItemCount: Int = 0

    data class DeletedItemsMessage(
        val count: Int,
        val onUndo: () -> Unit,
        val onDismiss: ((Int) -> Unit)? = null)

    /**
     * Post a message for the common use case of deleting items in a list while
     * providing an undo action. If postItemsDeletedMessage is called repeatedly
     * before the message is dismissed, the count of deleted items will continue
     * to accumulate.
     * @param count The number of items that were deleted in the deletion that
     * led to this call of postItemsDeletedMessage. Callers that make successive
     * calls to postItemsDeletedMessage do not need to accumulate this number
     * manually, as this is done automatically if a new call is made before the
     * message is dismissed.
     * @param onUndo The callback that will be invoked if the undo action is performed.
     * @param onDismiss The callback that will be invoked when the message is dismissed.
     */
    fun postItemsDeletedMessage(
        count: Int,
        onUndo: () -> Unit,
        onDismiss: ((Int) -> Unit)? = null
    ) {
        totalDeletedItemCount += count
        val text = context.getString(R.string.delete_snackbar_text, totalDeletedItemCount)
        val actionText = context.getString(R.string.undo_description)
        val onDismissPrivate = { dismissCode: Int ->
            if (dismissCode != DISMISS_EVENT_CONSECUTIVE)
                totalDeletedItemCount = 0
            onDismiss?.invoke(dismissCode)
            Unit
        }
        postMessage(text, actionText, onUndo, onDismissPrivate)
    }

    fun postItemsDeletedMessage(message: DeletedItemsMessage) =
        postItemsDeletedMessage(message.count, message.onUndo, message.onDismiss)
}