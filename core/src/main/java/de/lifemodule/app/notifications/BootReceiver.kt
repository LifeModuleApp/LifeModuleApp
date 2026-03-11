/*
 * LifeModule — A modular, privacy-focused life tracking app for Android.
 * Copyright (C) 2026 Paul Bernhard Colin Witzke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.lifemodule.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Re-schedules all repeating alarms after device reboot.
 * AlarmManager alarms are lost on reboot, so this receiver
 * reads saved preferences and re-schedules supplement + habit reminders.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        // Hilt handles injection automatically for @AndroidEntryPoint receivers.
        // No need to call super.onReceive as it is an abstract method.

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("Device rebooted - rescheduling notifications")

        val prefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)

        // Supplement daily reminder
        val suppEnabled = prefs.getBoolean("supplement_enabled", false)
        val suppTime = prefs.getString("supplement_time", null)
        if (suppEnabled && suppTime != null) {
            scheduler.scheduleSupplementReminder(suppTime)
            Timber.d("Re-scheduled supplement reminder at %s", suppTime)
        }

        // Habit daily reminder
        val habitEnabled = prefs.getBoolean("habit_enabled", false)
        val habitTime = prefs.getString("habit_time", null)
        if (habitEnabled && habitTime != null) {
            scheduler.scheduleHabitReminder(habitTime)
            Timber.d("Re-scheduled habit reminder at %s", habitTime)
        }
    }
}
