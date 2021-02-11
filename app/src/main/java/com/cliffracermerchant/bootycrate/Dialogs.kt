/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.share_dialog.*

/** An object that contains functions to open dialogs.
 *
 *  Dialog contains functions to open various dialogs. An instance of an activity must
 *  be supplied to Dialog via the init function. If any of the dialogs are called
 *  before init, an exception will occur. */
object Dialog {
    private lateinit var context: Context
    private lateinit var fragmentManager: FragmentManager
    private lateinit var snackBarParent: CoordinatorLayout
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    fun init(activity: MainActivity, snackBarParent: CoordinatorLayout) {
        context = activity
        fragmentManager = activity.supportFragmentManager
        this.shoppingListViewModel = activity.shoppingListViewModel
        this.inventoryViewModel = activity.inventoryViewModel
        this.snackBarParent = snackBarParent
    }

    /** Display a color picker dialog to choose from one of ViewModelItem's twelve colors.
     *
     *  Note that the initial color parameter and the return value are the
     *  indices of the chosen color, not the Android color value for the color. */
    fun colorPicker(initialColorIndex: Int = 0, callback: (Int) -> Unit) {
        val index = initialColorIndex.coerceIn(ViewModelItem.Colors.indices)
        val initialColor = ViewModelItem.Colors[index]
        val colorPicker = ColorSheet().colorPicker(ViewModelItem.Colors, initialColor,
                                                   noColorOption = false) { color ->
            val colorIndex = ViewModelItem.Colors.indexOf(color)
            callback(if (colorIndex != -1) colorIndex else 0)
        }
        colorPicker.show(fragmentManager)
    }

    private enum class ShareOption { TextMessage, Email }
    /** Display a dialog to provide options to share the list of items */
    fun <Entity: ViewModelItem>shareList(items: List<Entity>) {
        themedAlertBuilder().setView(R.layout.share_dialog).create().apply {
            setOnShowListener {
                shareTextMessageOption.setOnClickListener {
                    shareList(items, ShareOption.TextMessage)
                    dismiss()
                }
                shareEmailOption.setOnClickListener {
                    shareList(items, ShareOption.Email)
                    dismiss()
                }
            }
        }.show()
    }

    /** Export the list of items via the selected share option. */
    private fun <Entity: ViewModelItem>shareList(items: List<Entity>, shareOption: ShareOption) {
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
                val subject = context.getString(R.string.shopping_list_navigation_item_name)
                intent.putExtra(Intent.EXTRA_SUBJECT, subject)
                intent.putExtra(Intent.EXTRA_TEXT, message)
            }
        }
        if (intent.resolveActivity(context.packageManager) != null)
            context.startActivity(intent)
        else Snackbar.make(snackBarParent, R.string.share_error_message, Snackbar.LENGTH_SHORT).
                      setAnchorView(snackBarParent).show()
    }

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