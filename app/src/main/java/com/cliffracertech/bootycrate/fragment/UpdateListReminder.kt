/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import ca.antonious.materialdaypicker.MaterialDayPicker
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.MainActivity
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.databinding.UpdateListReminderSettingsFragmentBinding
import com.cliffracertech.bootycrate.utils.alarmManager
import com.cliffracertech.bootycrate.utils.dpToPixels
import com.cliffracertech.bootycrate.utils.notificationManager
import com.google.android.material.timepicker.MaterialTimePicker
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters.next
import java.time.temporal.TemporalAdjusters.nextOrSame
import java.util.*

@SuppressLint("UnspecifiedImmutableFlag")
/** An object containing functions and classes relating the the update list reminder. */
object UpdateListReminder {

    /** A data class that contains members indicating the user's settings for the update list reminder. */
    data class Settings(
        /** Whether or not the update list reminder is enabled */
        var enabled: Boolean = false,
        /** The time of the day that the reminder notification will be sent (only hours and minutes are used) */
        var time: LocalTime = LocalTime.now(),
        /** Whether the reminder notifications will repeat every given day at the specified time */
        var repeat: Boolean = false,
        /** The days that a reminder notification will be sent, if repeat is enabled */
        var repeatDays: List<MaterialDayPicker.Weekday> = emptyList()
    ) {
        companion object {
            const val enabledKey = "UpdateListReminder_enabled"
            const val hourKey = "UpdateListReminder_hour"
            const val minuteKey = "UpdateListReminder_minute"
            const val repeatKey = "UpdateListReminder_repeat"
            const val repeatDaysKey = "UpdateListReminder_repeatDays"

            /** Return a UpdateListReminder.Settings object with values obtained from the application's shared preferences. */
            fun fromSharedPreferences(prefs: SharedPreferences) = Settings(
                enabled = prefs.getBoolean(enabledKey, false),
                time = LocalTime.of(prefs.getInt(hourKey, 0),
                                    prefs.getInt(minuteKey, 0)),
                repeat = prefs.getBoolean(repeatKey, false),
                repeatDays = weekDaysList(prefs.getString(repeatDaysKey, "") ?: ""))
        }
    }

    /** Schedule reminder notification(s) given the parameters provided in settings. */
    fun scheduleNotifications(context: Context, settings: Settings) {
        val alarmManager = alarmManager(context) ?: return
        val intent = Intent(context, SendNotificationReceiver::class.java)

        val now = ZonedDateTime.now()
        val alarmDays = when { !settings.enabled -> emptyList<String>()
                               !settings.repeat -> listOf(now.dayOfWeek)
                               else -> settings.repeatDays.map { it.toJavaDayOfWeek() } }
        for (day in DayOfWeek.values()) {
            val pendingIntent = PendingIntent.getBroadcast(context, day.ordinal, intent, intentFlags)
            alarmManager.cancel(pendingIntent)
            if (!alarmDays.contains(day)) continue

            val targetDate = LocalDate.now().with(nextOrSame(day))
            val targetDateTime = ZonedDateTime.of(targetDate, settings.time, now.zone)
                .let { if (it.isAfter(now)) it
                       else it.with(next(day)) }
            val targetInstant = targetDateTime.toInstant().toEpochMilli()
            alarmManager.set(AlarmManager.RTC, targetInstant, pendingIntent)
        }
    }

    /** A fragment to display and alter UpdateListReminder.Settings parameters
     *
     * SettingsFragment assumes that the context it is running in is an instance
     * of FragmentActivity, and will not work properly if this is not the case.
     */
    class SettingsFragment : Fragment(), MainActivity.MainActivityFragment {
        private lateinit var ui: UpdateListReminderSettingsFragmentBinding
        private lateinit var currentSettings: Settings
        private lateinit var sharedPreferences: SharedPreferences

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = UpdateListReminderSettingsFragmentBinding.inflate(inflater, container, false)
            .also { ui = it }.root

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            currentSettings = Settings.fromSharedPreferences(sharedPreferences)
            ui.reminderSwitch.isChecked = currentSettings.enabled
            ui.reminderRepeatCheckBox.isChecked = currentSettings.repeat
            enableReminderSettings(currentSettings.enabled)
            ui.reminderRepeatDayPicker.setSelectedDays(currentSettings.repeatDays)
            ui.reminderTimeView.text = dateDisplayFormatter().format(currentSettings.time)

            ui.reminderRepeatCheckBox.buttonTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.disableable_accent_tint)
            setupOnClickListeners()
            updateDayPickerContentDescriptions()
        }

        private fun setupOnClickListeners() { ui.apply {
            reminderSwitch.setOnCheckedChangeListener { _, checked ->
                reminderTimeView.isEnabled = reminderSwitch.isChecked
                enableReminderSettings(checked)
                sharedPreferences.edit().putBoolean(Settings.enabledKey, checked).apply()
                currentSettings.enabled = checked
                context?.let { scheduleNotifications(it, currentSettings) }
            }
            reminderTimeView.setOnClickListener {
                val activity = context as? FragmentActivity ?:
                    return@setOnClickListener
                MaterialTimePicker.Builder()
                    .setHour(currentSettings.time.hour)
                    .setMinute(currentSettings.time.minute)
                    .build().apply {
                        addOnPositiveButtonClickListener {
                            currentSettings.time = LocalTime.of(hour, minute)
                            reminderTimeView.text = dateDisplayFormatter().format(currentSettings.time)
                            sharedPreferences.edit()
                                .putInt(Settings.hourKey, hour)
                                .putInt(Settings.minuteKey, minute).apply()
                            scheduleNotifications(activity, currentSettings)
                        }
                    }.show(activity.supportFragmentManager, null)
            }

            reminderRepeatCheckBox.setOnCheckedChangeListener { _, checked ->
                enableRepeatDays(checked && reminderSwitch.isChecked)
                currentSettings.repeat = checked
                context?.let { scheduleNotifications(it, currentSettings) }
                sharedPreferences.edit().putBoolean(Settings.repeatKey, checked).apply()
            }
            reminderRepeatDayPicker.setDaySelectionChangedListener { selectedDays ->
                currentSettings.repeatDays = selectedDays
                sharedPreferences.edit().putString(Settings.repeatDaysKey, selectedDays.serialize()).apply()
                context?.let { scheduleNotifications(it, currentSettings) }
            }
        }}

        private fun updateDayPickerContentDescriptions() =
            (ui.reminderRepeatDayPicker.getChildAt(0) as? LinearLayout)?.apply {
                val padding = resources.dpToPixels(4f).toInt()
                val firstDayOfWeek = MaterialDayPicker.Weekday.getFirstDayOfWeekFor(Locale.getDefault())
                val weekDays = MaterialDayPicker.Weekday.getOrderedDaysOfWeek(firstDayOfWeek)
                val weekDayStrings = weekDays.map { it.toJavaDayOfWeek().getDisplayName(
                                                        TextStyle.FULL, Locale.getDefault()) }
                for (i in 0 until childCount) {
                    if (i % 2 == 1) continue
                    val child = getChildAt(i)
                    child.setPadding(padding, padding, padding, padding)
                    child.contentDescription = weekDayStrings[(i / 2)]
                }
            }

        private fun enableReminderSettings(enabled: Boolean = true) {
            ui.reminderTimeView.isEnabled = ui.reminderSwitch.isChecked
            ui.reminderRepeatCheckBox.isEnabled = ui.reminderSwitch.isChecked
            enableRepeatDays(enabled && ui.reminderRepeatCheckBox.isChecked)
        }

        private fun enableRepeatDays(enabled: Boolean = true) =
            if (enabled) ui.reminderRepeatDayPicker.enableAllDays()
            else         ui.reminderRepeatDayPicker.disableAllDays()

        private fun dateDisplayFormatter(): DateTimeFormatter {
            val sys24HourClock = DateFormat.is24HourFormat(context)
            val formatStr = if (sys24HourClock) "kk:mm" else "hh:mm a"
            return DateTimeFormatter.ofPattern(formatStr, Locale.getDefault())
        }

        override fun onResume() {
            super.onResume()
            // If a reminder is sent while repeat is off, the reminder will set
            // the reminder enabled shared preference to false so that another
            // reminder is not erroneously sent. The shared preference therefore
            // needs to be checked during onResume to prevent the switch from not
            // matching the sharedPreference value if this fragment is still active
            // when this occurs.
            currentSettings.enabled = sharedPreferences.getBoolean(Settings.enabledKey, false)
            if (ui.reminderSwitch .isChecked != currentSettings.enabled)
                ui.reminderSwitch.isChecked = currentSettings.enabled
        }

        override fun showsBottomAppBar() = false
        override fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) {
            if (isActive) activityUi.actionBar.transition(
                backButtonVisible = true,
                searchButtonVisible = false,
                changeSortButtonVisible = false,
                menuButtonVisible = false)
        }
    }

    /** A BroadcastReceiver that receives intents for when the device is restarted or when
     * the app is updated. Both of these events necessitate a reset of all set alarms. */
    class AlarmsNeedResetReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!(intent.action == "android.intent.action.BOOT_COMPLETED" ||
                  intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
                  intent.action == "android.intent.action.MY_PACKAGE_REPLACED")) return
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val settings = Settings.fromSharedPreferences(prefs)
            scheduleNotifications(context, settings)
        }
    }

    /** A BroadcastReceiver that sends a notification to the user to remind them to update their
     * shopping list or inventory, and schedules another one next week if Settings.repeat is true. */
    class SendNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notificationManager = notificationManager(context) ?: return
            val notificationIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val channelId = context.getString(R.string.update_list_notification_channel_id)
            val alarmIntent = PendingIntent.getActivity(context, 0, notificationIntent, intentFlags)
            val notification = NotificationCompat.Builder(context, "reminder")
                .setSmallIcon(R.drawable.shopping_cart_icon)
                .setContentTitle(context.getString(R.string.update_list_reminder_title))
                .setContentText(context.getString(R.string.update_list_reminder_message))
                .setContentIntent(alarmIntent)
                .setChannelId(channelId).build()
            notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                notificationManager.getNotificationChannel(channelId) == null
            ) {
                val title = context.getString(R.string.update_list_notification_channel_name)
                val description = context.getString(R.string.update_list_notification_channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, title, importance)
                channel.description = description
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(0, notification)

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val settings = Settings.fromSharedPreferences(prefs)
            if (settings.repeat) {
                val now = ZonedDateTime.now()
                val targetDate = LocalDate.now().with(next(now.dayOfWeek))
                val targetDateTime = ZonedDateTime.of(targetDate, settings.time, now.zone)
                val targetInstant = targetDateTime.toInstant().toEpochMilli()
                val alarmManager = alarmManager(context) ?: return
                val pendingIntent = PendingIntent.getBroadcast(context, targetDate.dayOfWeek.ordinal,
                                                               intent, intentFlags)
                alarmManager.set(AlarmManager.RTC, targetInstant, pendingIntent)
            }
            else prefs.edit().putBoolean(Settings.enabledKey, false).apply()
        }
    }
}

private val intentFlags get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) 0
                                else PendingIntent.FLAG_IMMUTABLE

fun List<MaterialDayPicker.Weekday>.serialize() = StringBuilder().also {
    for (day in this) it.append(day.ordinal)
}.toString()

fun weekDaysList(string: String) =
    try { string.map(Character::getNumericValue).map(MaterialDayPicker.Weekday::get) }
    catch(e: IndexOutOfBoundsException) { emptyList() }

fun MaterialDayPicker.Weekday.toJavaDayOfWeek() =
    if (this == MaterialDayPicker.Weekday.SUNDAY)
        DayOfWeek.SUNDAY
    else DayOfWeek.values()[this.ordinal - 1]
