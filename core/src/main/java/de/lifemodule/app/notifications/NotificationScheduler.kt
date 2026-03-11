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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import de.lifemodule.app.core.R
import de.lifemodule.app.data.error.ErrorLogger
import kotlinx.coroutines.runBlocking

/**
 * Central notification scheduling service.
 * Uses AlarmManager to schedule exact alarms for all notification types.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider,
    private val errorLogger: ErrorLogger
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Returns true if the app is allowed to schedule exact alarms.
     * On Android 12+ (API 31) this requires the SCHEDULE_EXACT_ALARM permission and
     * the user granting it from System Settings -> Apps -> Special App Access.
     * Below Android 12 exact alarms are always permitted.
     */
    private fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Timber.w("Exact alarms not permitted. Direct the user to Settings > Apps > Special App Access > Alarms & Reminders.")
                return false
            }
        }
        return true
    }

    companion object {
        private const val TAG = "NotificationScheduler"
    }

    // ════════════════════════════════════════════
    // CALENDAR
    // ════════════════════════════════════════════

    /**
     * Schedule a calendar event reminder.
     * @param title Event title
     * @param date ISO date string "2026-02-18"
     * @param time HH:mm or null for all-day
     * @param minutesBefore How many minutes before to remind (60, 1440, 10080)
     */
    fun scheduleCalendarReminder(
        title: String,
        date: String,
        time: String?,
        minutesBefore: Int
    ) {
        val eventDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        val eventTime = if (time != null) {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            LocalTime.of(9, 0)
        }

        val triggerAt = LocalDateTime.of(eventDate, eventTime)
            .minusMinutes(minutesBefore.toLong())

        val label = when (minutesBefore) {
            60 -> context.getString(R.string.core_notification_in_1_stunde)
            1440 -> context.getString(R.string.core_notification_morgen)
            10080 -> context.getString(R.string.core_notification_in_einer_woche)
            else -> context.getString(R.string.core_notification_in_x_min, minutesBefore)
        }

        scheduleAlarm(
            channel = NotificationHelper.CHANNEL_CALENDAR,
            title = title,
            body = context.getString(R.string.core_notification_calendar_body, label, time ?: context.getString(R.string.core_notification_ganztaegig), date),
            triggerAt = triggerAt,
            requestCode = "cal_${title}_${date}_$minutesBefore".hashCode()
        )
    }

    // ════════════════════════════════════════════
    // STUNDENPLAN
    // ════════════════════════════════════════════

    /**
     * Schedule a course reminder for each day.
     * @param courseName e.g. "Mathe"
     * @param room e.g. "H3"
     * @param dayOfWeek 1=Monday..7=Sunday
     * @param startTime "08:00"
     * @param minutesBefore How many minutes before the course
     */
    fun scheduleClassReminder(
        courseName: String,
        room: String,
        dayOfWeek: Int,
        startTime: String,
        minutesBefore: Int
    ) {
        val today = timeProvider.today()
        val time = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))

        // Find next occurrence of this day of week
        var nextOccurrence = today
        while (nextOccurrence.dayOfWeek.value != dayOfWeek) {
            nextOccurrence = nextOccurrence.plusDays(1)
        }

        val triggerAt = LocalDateTime.of(nextOccurrence, time)
            .minusMinutes(minutesBefore.toLong())

        scheduleAlarm(
            channel = NotificationHelper.CHANNEL_SCHEDULE,
            title = courseName,
            body = context.getString(R.string.core_notification_class_body, minutesBefore, room),
            triggerAt = triggerAt,
            requestCode = "class_${courseName}_${dayOfWeek}_$startTime".hashCode()
        )
    }

    // ════════════════════════════════════════════
    // SUPPLEMENTS - Daily repeating
    // ════════════════════════════════════════════

    /**
     * Schedule a daily supplement reminder at a specific time.
     * @param reminderTime "09:00" format
     */
    fun scheduleSupplementReminder(reminderTime: String) {
        val time = LocalTime.parse(reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
        val now = timeProvider.now()
        var triggerAt = LocalDateTime.of(now.toLocalDate(), time)

        // If already past today, schedule for tomorrow
        if (triggerAt.isBefore(now)) {
            triggerAt = triggerAt.plusDays(1)
        }

        scheduleRepeatingAlarm(
            channel = NotificationHelper.CHANNEL_SUPPLEMENTS,
            title = context.getString(R.string.core_notification_supplement_title),
            body = context.getString(R.string.core_notification_supplement_body),
            triggerAt = triggerAt,
            requestCode = "supplement_daily".hashCode()
        )
    }

    // ════════════════════════════════════════════
    // HABITS - Daily repeating
    // ════════════════════════════════════════════

    /**
     * Schedule a daily habit reminder at a specific time.
     * @param reminderTime "20:00" format
     */
    fun scheduleHabitReminder(reminderTime: String) {
        val time = LocalTime.parse(reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
        val now = timeProvider.now()
        var triggerAt = LocalDateTime.of(now.toLocalDate(), time)

        if (triggerAt.isBefore(now)) {
            triggerAt = triggerAt.plusDays(1)
        }

        scheduleRepeatingAlarm(
            channel = NotificationHelper.CHANNEL_HABITS,
            title = context.getString(R.string.core_notification_habit_title),
            body = context.getString(R.string.core_notification_habit_body),
            triggerAt = triggerAt,
            requestCode = "habit_daily".hashCode()
        )
    }

    // ════════════════════════════════════════════
    // CANCEL
    // ════════════════════════════════════════════

    fun cancelCalendarReminders(title: String, date: String) {
        listOf(60, 1440, 10080).forEach { minutes ->
            cancelAlarm("cal_${title}_${date}_$minutes".hashCode())
        }
    }

    fun cancelSupplementReminder() = cancelAlarm("supplement_daily".hashCode())
    fun cancelHabitReminder() = cancelAlarm("habit_daily".hashCode())

    // ════════════════════════════════════════════
    // INTERNAL
    // ════════════════════════════════════════════

    private fun scheduleAlarm(
        channel: String,
        title: String,
        body: String,
        triggerAt: LocalDateTime,
        requestCode: Int
    ) {
        val triggerMillis = triggerAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= timeProvider.currentTimeMillis()) return

        val intent = createIntent(channel, title, body, requestCode)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canScheduleExact()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // Fallback: inexact alarm - may fire late but won't crash
                runBlocking {
                    errorLogger.logError(TAG, "setExactAndAllowWhileIdle denied, falling back to inexact alarm", e, "WARNING")
                }
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else {
            // Permission not granted: use inexact alarm as best-effort fallback
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun scheduleRepeatingAlarm(
        channel: String,
        title: String,
        body: String,
        triggerAt: LocalDateTime,
        requestCode: Int
    ) {
        val triggerMillis = triggerAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = createIntent(channel, title, body, requestCode).apply {
            putExtra("is_repeating", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canScheduleExact()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                runBlocking {
                    errorLogger.logError(TAG, "setExactAndAllowWhileIdle denied for repeating alarm, falling back", e, "WARNING")
                }
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun createIntent(channel: String, title: String, body: String, notifId: Int): Intent {
        return Intent(context, ReminderReceiver::class.java).apply {
            putExtra("channel", channel)
            putExtra("title", title)
            putExtra("body", body)
            putExtra("notif_id", notifId)
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
