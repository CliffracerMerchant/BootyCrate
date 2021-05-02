/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.fragment

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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import ca.antonious.materialdaypicker.MaterialDayPicker
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.GradientStyledMainActivity
import com.cliffracertech.bootycrate.databinding.UpdateListReminderSettingsFragmentBinding
import com.cliffracertech.bootycrate.utils.alarmManager
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import com.cliffracertech.bootycrate.utils.notificationManager
import com.google.android.material.timepicker.MaterialTimePicker
import java.text.SimpleDateFormat
import java.util.*

/** An object containing functions and classes relating the the update list reminder. */
object UpdateListReminder {

    /** A data class that contains members indicating the user's settings for the update list reminder. */
    data class Settings(/** Whether or not the update list reminder is enabled */
        var enabled: Boolean = false,
        /** The time of the day that the reminder notification will be sent (only hours and minutes are used) */
        var time: Calendar = Calendar.getInstance(),
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

            /** Return a UpdateListReminder.Settings object with values obtained from the applications share preferences. */
            fun fromSharedPreferences(prefs: SharedPreferences) = Settings(
                enabled = prefs.getBoolean(enabledKey, false),
                time = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, prefs.getInt(hourKey, 0))
                    set(Calendar.MINUTE, prefs.getInt(minuteKey, 0))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0) },
                repeat = prefs.getBoolean(repeatKey, false),
                repeatDays = weekDaysList(prefs.getString(repeatDaysKey, "") ?: ""))
        }
    }

    /** Schedule reminder notification(s) given the parameters provided in settings. */
    fun scheduleNotifications(context: Context, settings: Settings) {
        val intent = Intent(context, SendNotificationReceiver::class.java)
        val alarmManager = alarmManager(context) ?: return
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
        alarmManager.cancel(pendingIntent)
        if (!settings.enabled || (settings.repeat && settings.repeatDays.isEmpty()))
            return

        val now = Calendar.getInstance()
        val days = if (settings.repeat) settings.repeatDays.map { it.ordinal + 1 } // Since Calendar.SUNDAY == 1
                   else                 listOf(now.get(Calendar.DAY_OF_WEEK))
        for (day in days) {
            settings.time.set(Calendar.WEEK_OF_YEAR, now.get(Calendar.WEEK_OF_YEAR))
            settings.time.set(Calendar.DAY_OF_WEEK, day)
            if (settings.time < now)
                settings.time.add(Calendar.DAY_OF_WEEK, if (settings.repeat) 7 else 1)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, settings.time.timeInMillis, pendingIntent)
        }
    }

    /** A fragment to display and alter UpdateListReminder.Settings parameters. */
    class SettingsFragment : Fragment() {
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

            val context = requireContext()
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            currentSettings = Settings.fromSharedPreferences(sharedPreferences)
            ui.reminderRepeatCheckBox.buttonTintList =
                ContextCompat.getColorStateList(context, R.color.disableable_accent_tint)
            ui.reminderSwitch.isChecked = currentSettings.enabled
            ui.reminderRepeatCheckBox.isChecked = currentSettings.repeat
            enableReminderSettings(currentSettings.enabled)
            ui.reminderRepeatDayPicker.setSelectedDays(currentSettings.repeatDays)
            ui.reminderTimeView.text = dateDisplayFormat().format(currentSettings.time.time)
            setOnClickListeners()
        }

        private fun setOnClickListeners() {
            ui.reminderSwitch.setOnCheckedChangeListener { _, checked ->
                ui.reminderTimeView.isEnabled = ui.reminderSwitch.isChecked
                enableReminderSettings(checked)
                sharedPreferences.edit().putBoolean(Settings.enabledKey, checked).apply()
                currentSettings.enabled = checked
                context?.let { scheduleNotifications(it, currentSettings) }
            }
            ui.reminderTimeView.setOnClickListener {
                val context = this.context ?: return@setOnClickListener
                MaterialTimePicker.Builder()
                    .setHour(currentSettings.time.get(Calendar.HOUR_OF_DAY))
                    .setMinute(currentSettings.time.get(Calendar.MINUTE))
                    .build().apply {
                        addOnPositiveButtonClickListener {
                            currentSettings.time.set(Calendar.HOUR_OF_DAY, hour)
                            currentSettings.time.set(Calendar.MINUTE, minute)
                            ui.reminderTimeView.text = dateDisplayFormat().format(currentSettings.time.time)
                            sharedPreferences.edit()
                                .putInt(Settings.hourKey, hour)
                                .putInt(Settings.minuteKey, minute).apply()
                            scheduleNotifications(context, currentSettings)
                        }
                    }.show(context.asFragmentActivity().supportFragmentManager, "")
            }
            ui.reminderRepeatCheckBox.setOnCheckedChangeListener { _, checked ->
                enableRepeatDays(checked && ui.reminderSwitch.isChecked)
                currentSettings.repeat = checked
                context?.let { scheduleNotifications(it, currentSettings) }
                sharedPreferences.edit().putBoolean(Settings.repeatKey, checked).apply()
            }
            ui.reminderRepeatDayPicker.setDaySelectionChangedListener { selectedDays ->
                currentSettings.repeatDays = selectedDays
                sharedPreferences.edit().putString(Settings.repeatDaysKey, selectedDays.serialize()).apply()
                context?.let { scheduleNotifications(it, currentSettings) }
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

        private fun dateDisplayFormat(): SimpleDateFormat {
            val sys24HourClock = DateFormat.is24HourFormat(context)
            val formatStr = if (sys24HourClock) "kk:mm" else "hh:mm a"
            return SimpleDateFormat(formatStr, Locale.getDefault())
        }
    }

    /** A BroadcastReceiver that receives intents with the action 'android.intent.action.BOOT_COMPLETED'
     * in order to reschedule any reminder notifications that were lost when the device restarted. */
    class DeviceRestartReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != "android.intent.action.BOOT_COMPLETED") return
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val settings = Settings.fromSharedPreferences(prefs)
            scheduleNotifications(context, settings)
        }
    }

    /** A BroadcastReceiver that sends a notification to the user to remind them to update their shopping list or inventory,
     * and schedules another one next week if Settings.repeat is true. */
    class SendNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notificationManager = notificationManager(context) ?: return
            val notificationIntent = Intent(context, GradientStyledMainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val channelId = context.getString(R.string.update_list_notification_channel_id)
            val notification = NotificationCompat.Builder(context, "reminder")
                .setSmallIcon(R.drawable.shopping_cart_icon)
                .setContentTitle(context.getString(R.string.update_list_reminder_title))
                .setContentText(context.getString(R.string.update_list_reminder_message))
                .setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, 0))
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
                val targetDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 7)
                    set(Calendar.HOUR_OF_DAY, settings.time.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, settings.time.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val alarmManager = alarmManager(context) ?: return
                val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetDate.timeInMillis, pendingIntent)
            }
            else prefs.edit().putBoolean(Settings.enabledKey, false).apply()
        }
    }
}

fun List<MaterialDayPicker.Weekday>.serialize() = StringBuilder().also {
    for (day in this) it.append(day.ordinal)
}.toString()

fun weekDaysList(string: String) =
    try { string.map(Character::getNumericValue).map(MaterialDayPicker.Weekday::get) }
    catch(e: IndexOutOfBoundsException) { emptyList() }
