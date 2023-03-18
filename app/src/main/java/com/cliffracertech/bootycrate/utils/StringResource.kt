/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/** A holder of a string resource that can be resolved to a [String]
 * by calling the method [resolve] with a [Context] instance. */
class StringResource(
    private val string: String?,
    @StringRes val stringResId: Int = 0,
    private val args: ArrayList<Any>?
) {
    data class Id(@StringRes val id: Int)

    constructor(string: String): this(string, 0, null)
    constructor(@StringRes stringResId: Int): this(null, stringResId, null)
    constructor(@StringRes stringResId: Int, stringVar: String):
            this(null, stringResId, arrayListOf(stringVar))
    constructor(@StringRes stringResId: Int, intVar: Int):
            this(null, stringResId, arrayListOf(intVar))
    constructor(@StringRes stringResId: Int, stringVarId: Id):
            this(null, stringResId, arrayListOf(stringVarId))

    fun resolve(context: Context?) = string ?: when {
        context == null -> ""
        args == null -> context.getString(stringResId)
        else -> {
            for (i in args.indices) {
                val it = args[i]
                if (it is Id)
                    args[i] = context.getString(it.id)
            }
            context.getString(stringResId, *args.toArray())
        }
    }
}

/** Acts the same as a [Text], except that the text is
 * described as a [StringResource] instead of a [String]. */
@Composable fun Text(
    text: StringResource,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
): Unit = Text(
    text.resolve(LocalContext.current), modifier,
    color, fontSize, fontStyle, fontWeight, fontFamily,
    letterSpacing, textDecoration, textAlign, lineHeight,
    overflow, softWrap, maxLines, onTextLayout, style)