/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.dialog

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.model.database.BootyCrateDatabase
import com.cliffracertech.bootycrate.utils.SoftKeyboard
import com.cliffracertech.bootycrate.utils.dpToPixels
import com.cliffracertech.bootycrate.utils.resolveIntAttribute
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Open a dialog to display an about app screen. */
class AboutAppDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        themedAlertDialogBuilder(requireContext())
            .setView(R.layout.about_app_dialog)
            .setPositiveButton(android.R.string.ok, null).create()
}

class PrivacyPolicyDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        themedAlertDialogBuilder(requireContext())
            .setView(R.layout.privacy_policy_dialog)
            .setPositiveButton(android.R.string.ok, null).create()
}

/** Return a MaterialAlertDialogBuilder with the context theme's materialAlertDialogTheme style applied. */
fun themedAlertDialogBuilder(context: Context) = MaterialAlertDialogBuilder(
    context, context.theme.resolveIntAttribute(R.attr.materialAlertDialogTheme))
        .setBackground(ContextCompat.getDrawable(context, R.drawable.alert_dialog))
        .setBackgroundInsetStart(0)
        .setBackgroundInsetEnd(0)

/** Show a dialog to rename an item group, with the name initially set
 * to initialName, with a hint equal to hint, and which invokes onFinish
 * if the user taps the ok button. */
fun itemGroupNameDialog(
    context: Context,
    initialName: String? = null,
    onFinish: ((String) -> Unit)
) {
    val editText = EditText(context).apply {
        setText(initialName)
        setHint(R.string.item_group_name_hint)
        setSingleLine()
    }
    themedAlertDialogBuilder(context)
        .setTitle(R.string.rename_item_group_popup_title)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onFinish(editText.text.toString())
        }.setNegativeButton(android.R.string.cancel, null)
        .create().apply {
            val spacing = context.dpToPixels(16f).toInt()
            setView(editText, spacing, 0, spacing, 0)
            setOnShowListener {
                val okButton = getButton(AlertDialog.BUTTON_POSITIVE)
                okButton.isEnabled = !initialName.isNullOrBlank()
                SoftKeyboard.showWithDelay(editText)
            }
            editText.doOnTextChanged { text, _, _, _ ->
                val okButton = getButton(AlertDialog.BUTTON_POSITIVE)
                okButton.isEnabled = !text.isNullOrBlank()
            }
        }.show()
}

/** Show a dialog to confirm the deletion of an item group. */
fun deleteItemGroupDialog(context: Context, onConfirm: () -> Unit) =
    themedAlertDialogBuilder(context)
        .setMessage(R.string.delete_item_group_confirmation_message)
        .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
        .setNegativeButton(android.R.string.cancel, null)
        .create().show()

/** Open a dialog to ask the user to the type of database import they want (merge
 *  existing or overwrite, and recreate the given activity if the import requires it. */
fun importDatabaseFromUriDialog(
    uri: Uri,
    context: Context,
    database: BootyCrateDatabase
) {
    themedAlertDialogBuilder(context)
        .setMessage(R.string.import_database_question_message)
        .setNeutralButton(android.R.string.cancel) { _, _ -> }
        .setNegativeButton(R.string.import_database_question_merge_option) { _, _ ->
            database.importBackup(context, uri, overwriteExistingDb = false)
        }.setPositiveButton(R.string.import_database_question_overwrite_option) { _, _ ->
            themedAlertDialogBuilder(context)
                .setMessage(R.string.import_database_overwrite_confirmation_message)
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    database.importBackup(context, uri, overwriteExistingDb = true)
                }.show()
        }.show()
}