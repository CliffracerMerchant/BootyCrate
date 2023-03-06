/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cliffracertech.bootycrate.model.database.Validator
import com.cliffracertech.bootycrate.utils.StringResource
import kotlinx.collections.immutable.ImmutableList

/** Create a view that displays an icon appropriate for the
 * type of [Validator.Message] alongside its text. */
@Composable fun ValidatorMessageView(
    message: Validator.Message,
    modifier: Modifier = Modifier
) = Row(
    modifier.fillMaxWidth().height(48.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    val vector = when {
        message.isInformational -> Icons.Default.Info
        message.isWarning ->       Icons.Default.Warning
        else ->/*message.isError*/ Icons.Default.Error
    }
    val tint = when {
        message.isInformational -> Color.Blue
        message.isWarning ->       Color.Yellow
        else ->/*message.isError*/ MaterialTheme.colors.error
    }
    Icon(vector, null, tint = tint)

    Text(message.stringResource.resolve(LocalContext.current))
}

/** A display of a single nullable [Validator.Message], with appearance and/or
 * disappearance animations for when the message changes or becomes null. */
@Composable fun SingleValidatorMessage(
    message: Validator.Message?,
    modifier: Modifier = Modifier
) {
    var lastMessage by remember { mutableStateOf<Validator.Message>(
        Validator.Message.Error(StringResource(""))
    )}
    AnimatedVisibility(message != null, modifier) {
        Crossfade(message ?: lastMessage) {
            ValidatorMessageView(it)
        }
    }
    message?.let { lastMessage = it }
}

/** A display of a list of [Validator.Message]s that animates
 * size changes as new messages appear or disappear. */
@Composable fun ValidatorMessageList(
    messages: ImmutableList<Validator.Message>,
    modifier: Modifier = Modifier
) = LazyColumn(modifier.animateContentSize()) {
    items(messages) {
        ValidatorMessageView(it)
    }
}