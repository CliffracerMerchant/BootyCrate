/*
 * Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory.
 */

package com.cliffracertech.bootycrate.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import ca.antonious.materialdaypicker.MaterialDayPicker
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.UpdateListReminderFragmentBinding
import com.cliffracertech.bootycrate.utils.asFragmentActivity
import com.google.android.material.timepicker.MaterialTimePicker
import java.text.SimpleDateFormat
import java.util.*

class UpdateListReminderFragment : Fragment() {

    private val ui get() = _ui
    private lateinit var _ui: UpdateListReminderFragmentBinding

    private lateinit var enabledKey: String
    private lateinit var timeKey: String
    private lateinit var repeatKey: String
    private lateinit var repeatDaysKey: String
    private lateinit var sharedPreferences: SharedPreferences

    data class ReminderSettings(
        var enabled: Boolean = false,
        var time: Date = Date(),
        var hour: Int = 0,
        var minute: Int = 0,
        var repeat: Boolean = false,
        var repeatDays: List<MaterialDayPicker.Weekday> = emptyList())
    private var currentSettings = ReminderSettings()
    var onSettingsChangedListener: ((ReminderSettings) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = UpdateListReminderFragmentBinding.inflate(inflater, container, false)
        .apply { _ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        enabledKey = context.getString(R.string.pref_update_list_reminder_enabled_key)
        timeKey = context.getString(R.string.pref_update_list_reminder_time_key)
        repeatKey = context.getString(R.string.pref_update_list_reminder_repeat_enabled_key)
        repeatDaysKey = context.getString(R.string.pref_update_list_reminder_repeat_days_key)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        ui.reminderRepeatCheckBox.buttonTintList = ContextCompat.getColorStateList(context, R.color.disableable_accent_tint)
        initValues()
        setOnClickListeners()
    }

    private fun initValues() {
        ui.reminderSwitch.isChecked = sharedPreferences.getBoolean(enabledKey, false)
        ui.reminderRepeatCheckBox.isChecked = sharedPreferences.getBoolean(repeatKey, false)
        enableReminderSettings(ui.reminderSwitch.isChecked)
        val selectedDays = weekDaysFromStr(sharedPreferences.getString(repeatDaysKey, "") ?: "")
        ui.reminderRepeatDayPicker.setSelectedDays(selectedDays)

        val timeStr = sharedPreferences.getString(timeKey, Date(0).toString())
        ui.reminderTimeView.text = timeText(Date(timeStr))
    }

    private fun setOnClickListeners() {
        ui.reminderSwitch.setOnCheckedChangeListener { _, checked ->
            ui.reminderTimeView.isEnabled = ui.reminderSwitch.isChecked
            enableReminderSettings(checked)
            sharedPreferences.edit().putBoolean(enabledKey, checked).apply()
            currentSettings.enabled = checked
            onSettingsChangedListener?.invoke(currentSettings)
        }
        ui.reminderTimeView.setOnClickListener {
            val context = this.context ?: return@setOnClickListener
            MaterialTimePicker.Builder()
                .setHour(currentSettings.hour)
                .setMinute(currentSettings.minute)
                .build().apply {
                    addOnPositiveButtonClickListener {
                        currentSettings.time = Date(0, 0, 0, hour, minute)
                        currentSettings.hour = hour
                        currentSettings.minute = minute
                        ui.reminderTimeView.text = timeText(currentSettings.time)
                        onSettingsChangedListener?.invoke(currentSettings)
                        sharedPreferences.edit().putString(timeKey, currentSettings.time.toString())
                            .apply()
                    }
                }.show(context.asFragmentActivity().supportFragmentManager, "")
        }
        ui.reminderRepeatCheckBox.setOnCheckedChangeListener { _, checked ->
            enableRepeatDays(checked && ui.reminderSwitch.isChecked)
            currentSettings.repeat = checked
            onSettingsChangedListener?.invoke(currentSettings)
            sharedPreferences.edit().putBoolean(repeatKey, checked).apply()
        }
        ui.reminderRepeatDayPicker.setDaySelectionChangedListener { selectedDays ->
            currentSettings.repeatDays = selectedDays
            sharedPreferences.edit().putString(repeatDaysKey, selectedDays.serialize()).apply()
            onSettingsChangedListener?.invoke(currentSettings)
        }
    }

    fun List<MaterialDayPicker.Weekday>.serialize() = StringBuilder().also {
        for (day in this) it.append(day.ordinal)
    }.toString()

    private fun weekDaysFromStr(string: String) =
        try { string.map(Character::getNumericValue).map(MaterialDayPicker.Weekday::get) }
        catch(e: IndexOutOfBoundsException) { emptyList<MaterialDayPicker.Weekday>() }

    private fun enableReminderSettings(enabled: Boolean = true) {
        ui.reminderTimeView.isEnabled = ui.reminderSwitch.isChecked
        ui.reminderRepeatCheckBox.isEnabled = ui.reminderSwitch.isChecked
        enableRepeatDays(enabled && ui.reminderRepeatCheckBox.isChecked)
    }

    private fun enableRepeatDays(enabled: Boolean = true) {
        if (enabled) ui.reminderRepeatDayPicker.enableAllDays()
        else         ui.reminderRepeatDayPicker.disableAllDays()
    }

    private fun timeText(time: Date): String {
        val sys24HourClock = DateFormat.is24HourFormat(context)
        val formatStr = if (sys24HourClock) "kk:mm" else "hh:mm a"
        val format = SimpleDateFormat(formatStr, Locale.getDefault())
        return format.format(time)
    }
}
