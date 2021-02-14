/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.cliffracermerchant.bootycrate.Dialog.themedAlertBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.thebluealliance.spectrum.SpectrumDialog
import dagger.hilt.android.AndroidEntryPoint
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.share_dialog.*
import javax.inject.Inject

/** Display a color picker dialog to choose from one of ViewModelItem's twelve colors,
 *  then invoke @param callback with the chosen color index.
 *
 *  Note that the initial color parameter and the return value are the
 *  indices of the chosen color, not the Android color value for the color. */
fun showColorPickerDialog(
    context: Context,
    fragmentManager: FragmentManager,
    initialColorIndex: Int,
    callback: (Int) -> Unit,
) {
    SpectrumDialog.Builder(context)
        .setColors(ViewModelItem.Colors)
        .setSelectedColor(initialColorIndex)
        .setOnColorSelectedListener { _, color -> callback(color) }
        .build().show(fragmentManager, null)
}

private enum class ShareOption { TextMessage, Email }

@AndroidEntryPoint
class ShareDialog<Entity: ViewModelItem>(
    context: Context,
    items: List<Any>,
    snackBarParent: View
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        themedAlertBuilder(context)
        .setView(R.layout.share_dialog)
        .create().apply {
            setOnShowListener {
                shareTextMessageOption.setOnClickListener {
                    shareList(ShareOption.TextMessage)
                    dismiss()
                }
                shareEmailOption.setOnClickListener {
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
                val subject =  context?.getString(R.string.shopping_list_navigation_item_name)
                intent.putExtra(Intent.EXTRA_SUBJECT, subject)
                intent.putExtra(Intent.EXTRA_TEXT, message)
            }
        }
        if (intent.resolveActivity(context.packageManager) != null)
            context?.startActivity(intent)
        else Snackbar.make(snackBarParent, R.string.share_error_message, Snackbar.LENGTH_SHORT).
        setAnchorView(snackBarParent).show()
    }
}

class AboutAppDialog(context: Context) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        themedAlertBuilder(context).setView(R.layout.about_app_dialog).create()
}

fun themedAlertBuilder(context: Context): MaterialAlertDialogBuilder {
    // AlertDialog seems to ignore the theme's alertDialogTheme value, making it
    // necessary to pass it's value in manually to the AlertDialog.builder constructor.
    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.materialAlertDialogTheme, typedValue, true)
    return MaterialAlertDialogBuilder(context, typedValue.data)
}


object Dialog {

    fun aboutApp() { themedAlertBuilder().setView(R.layout.about_app_dialog).show() }

    /** Open a dialog to ask the user to the type of database import they want (merge
     *  existing or overwrite, and recreate the given activity if the import requires it. */
    fun importDatabaseFromUri(uri: Uri, activity: FragmentActivity?)  {
        themedAlertBuilder().
            setMessage(R.string.import_database_question_message).
            setNeutralButton(android.R.string.cancel) { _, _ -> }.
            setNegativeButton(R.string.import_database_question_merge_option) { _, _ ->
                BootyCrateDatabase.mergeWithBackup(context, uri)
            }.setPositiveButton(R.string.import_database_question_overwrite_option) { _, _ ->
                themedAlertBuilder().
                    setMessage(R.string.import_database_overwrite_confirmation_message).
                    setNegativeButton(android.R.string.no) { _, _ -> }.
                    setPositiveButton(android.R.string.yes) { _, _ ->
                        BootyCrateDatabase.replaceWithBackup(context, uri)
                        // The pref pref_viewmodels_need_cleared needs to be set to true so that
                        // when the MainActivity is recreated, it will clear its ViewModelStore
                        // and use the DAOs of the new database instead of the old one.
                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                        val editor = prefs.edit()
                        editor.putBoolean(context.getString(R.string.pref_viewmodels_need_cleared), true)
                        editor.apply()
                        activity?.recreate()
                    }.show()
            }.show()
    }

    /** Return an AlertDialog.Builder that uses the current theme's alertDialogTheme. */
    fun themedAlertBuilder(): MaterialAlertDialogBuilder {
        // AlertDialog seems to ignore the theme's alertDialogTheme value, making it
        // necessary to pass it's value in manually to the AlertDialog.builder constructor.
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.materialAlertDialogTheme, typedValue, true)
        return MaterialAlertDialogBuilder(context, typedValue.data)
    }
}