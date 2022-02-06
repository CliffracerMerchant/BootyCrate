/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.view.View
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.utils.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * An manager of messages to be displayed to the user, e.g. through a SnackBar.
 *
 * New messages can be posted using the postMessage function. MessageHandler
 * users can listen to the SharedFlow member messages for new messages. The
 * function postItemsDeletedMessage is provided for convenience for the common
 * use case of showing an X item(s) deleted message after items are deleted
 * from a list, along with an undo action.
 */
@ActivityRetainedScoped
class MessageHandler @Inject constructor() {
    /**
     * A message to be displayed to the user.
     * @param stringResource A StringResource that, when resolved, will be the text of the message.
     * @param actionStringResource A nullable StringResource that, when resolved, will be the text
     * of the message action, if any.
     * @param onActionClick The callback that will be invoked if the message action is clicked.
     * @param onDismiss The callback that will be invoked when the message is dismissed. The
     * int parameter will be equal to a value of BaseTransientBottomBar.BaseCallback.DismissEvent.
     */
    data class Message(
        val stringResource: StringResource,
        val actionStringResource: StringResource? = null,
        val onActionClick: (() -> Unit)? = null,
        val onDismiss: ((Int) -> Unit)? = null)

    private val _messages = MutableSharedFlow<Message>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages = _messages.asSharedFlow()

    /** Post the message described by the parameters to the message queue. */
    fun postMessage(
        stringResource: StringResource,
        actionStringResource: StringResource? = null,
        onActionClick: (() -> Unit)? = null,
        onDismiss: ((Int) -> Unit)? = null
    ) = _messages.tryEmit(Message(stringResource, actionStringResource, onActionClick, onDismiss))

    private var totalDeletedItemCount: Int = 0

    /**
     * Post a message for the common use case of deleting items in a list while
     * providing an undo action. If postItemsDeletedMessage is called repeatedly
     * before the message is dismissed, the count of deleted items will continue
     * to accumulate.
     * @param count The number of items that were deleted in the deletion that
     * led to this call of postItemsDeletedMessage. Callers that make successive
     * calls to postItemsDeletedMessage do not need to accumulate this number
     * manually, as this is done automatically if a new call is made before the
     * previous message is dismissed.
     * @param onUndo The callback that will be invoked if the undo action is performed.
     * @param onDismiss The callback that will be invoked when the message is dismissed.
     */
    fun postItemsDeletedMessage(
        count: Int,
        onUndo: () -> Unit,
        onDismiss: ((Int) -> Unit)? = null
    ) {
        totalDeletedItemCount += count
        val stringResource = StringResource(R.string.delete_snackbar_text, totalDeletedItemCount)
        val actionText = StringResource(R.string.undo_description)
        val onDismissPrivate = { dismissCode: Int ->
            if (dismissCode != DISMISS_EVENT_CONSECUTIVE)
                totalDeletedItemCount = 0
            onDismiss?.invoke(dismissCode)
            Unit
        }
        postMessage(stringResource, actionText, onUndo, onDismissPrivate)
    }
}

/** Display the Flow<MessageHandler.Message>'s emitted values using SnackBars
 * anchored to the provided view. */
fun Flow<MessageHandler.Message>.displayWithSnackBarAnchoredTo(anchor: View) {
    val lifecycleOwner = anchor.findViewTreeLifecycleOwner()
    lifecycleOwner?.recollectWhenStarted(this) { message ->
        val text = message.stringResource.resolve(anchor.context)
        val actionText = message.actionStringResource?.resolve(anchor.context)
        val snackBar = Snackbar.make(anchor, text, Snackbar.LENGTH_LONG)
            .setAnchorView(anchor)
            .setAction(actionText) { message.onActionClick?.invoke() }
            .setActionTextColor(anchor.context.theme.resolveIntAttribute(R.attr.colorAccent))
            .addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    message.onDismiss?.invoke(event)
                }
            })
        val textView = snackBar.view.findViewById<TextView>(R.id.snackbar_text)
        val startPadding = anchor.resources.dpToPixels(10f).toInt()
        (textView.parent as? View)?.setPadding(start = startPadding)
        snackBar.show()
        SoftKeyboard.hide(anchor)
    }
}