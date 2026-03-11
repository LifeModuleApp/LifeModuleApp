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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import timber.log.Timber
import androidx.core.app.NotificationCompat
import de.lifemodule.app.core.R

/**
 * Notification channels & receiver for all app notifications.
 */
class NotificationHelper {
    companion object {
        // ── Channels ──
        const val CHANNEL_CALENDAR = "calendar_events"
        const val CHANNEL_SCHEDULE = "schedule_reminders"
        const val CHANNEL_SUPPLEMENTS = "supplement_reminders"
        const val CHANNEL_HABITS = "habit_reminders"

        /** LifeModule accent green (#A2FF00) used for notification icon tint. */
        const val ACCENT_COLOR = 0xFFA2FF00.toInt()

        /** Notification group key to bundle multiple notifications. */
        const val GROUP_KEY = "de.lifemodule.app.NOTIFICATIONS"

        fun createAllChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)

            listOf(
                NotificationChannel(
                    CHANNEL_CALENDAR, context.getString(R.string.core_notification_channel_calendar),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.core_notification_channel_calendar_desc)
                    enableLights(true)
                    lightColor = ACCENT_COLOR
                },

                NotificationChannel(
                    CHANNEL_SCHEDULE, context.getString(R.string.core_notification_channel_schedule),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.core_notification_channel_schedule_desc)
                    enableLights(true)
                    lightColor = ACCENT_COLOR
                },

                NotificationChannel(
                    CHANNEL_SUPPLEMENTS, context.getString(R.string.core_notification_channel_supplements),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.core_notification_channel_supplements_desc)
                    enableLights(true)
                    lightColor = ACCENT_COLOR
                },

                NotificationChannel(
                    CHANNEL_HABITS, context.getString(R.string.core_notification_channel_habits),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.core_notification_channel_habits_desc)
                    enableLights(true)
                    lightColor = ACCENT_COLOR
                }
            ).forEach { manager.createNotificationChannel(it) }
        }
    }
}

/**
 * Generic broadcast receiver for all scheduled notifications.
 * Differentiates by extras: "channel", "title", "body".
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createAllChannels(context)

        val channel = intent.getStringExtra("channel") ?: NotificationHelper.CHANNEL_CALENDAR
        val title = intent.getStringExtra("title") ?: context.getString(R.string.core_notification_fallback_title)
        val body = intent.getStringExtra("body") ?: ""
        val notifId = intent.getIntExtra("notif_id", title.hashCode())

        val emoji = when (channel) {
            NotificationHelper.CHANNEL_CALENDAR -> "📅"
            NotificationHelper.CHANNEL_SCHEDULE -> "🎓"
            NotificationHelper.CHANNEL_SUPPLEMENTS -> "💊"
            NotificationHelper.CHANNEL_HABITS -> "✅"
            else -> "🔔"
        }

        // Tap notification -> open main activity
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context, notifId, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Large icon: app launcher icon as bitmap
        val largeIcon = BitmapFactory.decodeResource(
            context.resources,
            context.applicationInfo.icon
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setColor(NotificationHelper.ACCENT_COLOR)
            .setContentTitle("$emoji $title")
            .setContentText(body)
            .setSubText("LifeModule")
            .setGroup(NotificationHelper.GROUP_KEY)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notifId, notification)

        // ── Re-schedule if this is a daily repeating alarm ──
        if (intent.getBooleanExtra("is_repeating", false)) {
            rescheduleNextDay(context, intent, notifId)
        }
    }

    /**
     * Re-schedules the same alarm for the next day using exact alarms.
     * This replaces [AlarmManager.setRepeating] which is inexact on API 19+.
     */
    private fun rescheduleNextDay(context: Context, intent: Intent, requestCode: Int) {
        val nextTrigger = System.currentTimeMillis() + AlarmManager.INTERVAL_DAY

        val newIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("channel", intent.getStringExtra("channel"))
            putExtra("title", intent.getStringExtra("title"))
            putExtra("body", intent.getStringExtra("body"))
            putExtra("notif_id", requestCode)
            putExtra("is_repeating", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            }
        } catch (e: SecurityException) {
            Timber.w(e, "Exact alarm fallback for repeating")
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
        }
    }
}
