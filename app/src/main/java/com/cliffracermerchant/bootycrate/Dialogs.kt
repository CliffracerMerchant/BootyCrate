/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.cliffracermerchant.bootycrate.databinding.ShareDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dev.sasikanth.colorsheet.ColorSheet

/** Display a color picker dialog to choose from one of ViewModelItem's twelve colors,
 * then invoke @param callback with the chosen color index.
 *
 * Note that the initial color parameter and the return value are the
 * indices of the chosen color, not the Android color value for the color.
 */
fun showViewModelItemColorPickerDialog(
    fragmentManager: FragmentManager,
    initialColorIndex: Int,
    callback: (Int) -> Unit,
) = ColorSheet().colorPicker(
    colors = ViewModelItem.Colors.toIntArray(),
    selectedColor = ViewModelItem.Colors[initialColorIndex],
    listener = { color: Int ->
        val colorIndex = ViewModelItem.Colors.indexOf(color)
        callback(if (colorIndex != -1) colorIndex else 0)
    }).show(fragmentManager)

private enum class ShareOption { TextMessage, Email }

class ShareDialog<Entity: ViewModelItem>(
    private val subject: String,
    private val items: List<Entity>,
    private val snackBarAnchor: View
) : DialogFragment() {
    lateinit var ui: ShareDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ShareDialogBinding.inflate(LayoutInflater.from(context))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        themedAlertDialogBuilder(requireContext())
            .setView(ui.root).create().apply {
                setOnShowListener {
                    ui.shareTextMessageOption.setOnClickListener {
                        shareList(ShareOption.TextMessage)
                        dismiss()
                    }
                    ui.shareEmailOption.setOnClickListener {
                        shareList(ShareOption.Email)
                        dismiss()
                    }
                }
            }

    /** Export the list of items via the selected share option. */
    private fun shareList(shareOption: ShareOption) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.type = "HTTP.PLAIN_TEXT_TYPE"

        var message = ""
        for (i in 0 until items.size - 1)
            message += items[i].toString() + "\n"
        if (items.isNotEmpty())
            message += items.last().toString()

        when (shareOption) {
            ShareOption.TextMessage -> {
                intent.putExtra("sms_body", message)
                intent.data = Uri.parse("smsto:")
            } ShareOption.Email -> {
                intent.data = Uri.parse("mailto:")
                intent.putExtra(Intent.EXTRA_SUBJECT, subject)
                intent.putExtra(Intent.EXTRA_TEXT, message)
            }
        }
        try { requireContext().startActivity(intent) }
        catch (e: ActivityNotFoundException) {
            Snackbar.make(
                snackBarAnchor,
                R.string.share_error_message,
                Snackbar.LENGTH_SHORT
            ).setAnchorView(snackBarAnchor).show()
        }
    }
}

class AboutAppDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        themedAlertDialogBuilder(requireContext()).setView(R.layout.about_app_dialog).create()
}

fun themedAlertDialogBuilder(context: Context) = MaterialAlertDialogBuilder(
    context, context.theme.resolveIntAttribute(R.attr.materialAlertDialogTheme))