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

package de.lifemodule.app.data.calendar

import de.lifemodule.app.util.time.TimeProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates RFC 5545 iCalendar (.ics) content from [CalendarEventEntity] items.
 *
 * 100 % local - no network, no third-party library.
 */
@Singleton
class IcsGenerator @Inject constructor(
    private val timeProvider: TimeProvider
) {
    companion object {
        private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Generates a complete .ics file as a [String] from the given [events].
     *
     * @param calendarName Optional calendar name (X-WR-CALNAME).
     */
    fun generate(
        events: List<CalendarEventEntity>,
        calendarName: String = "LifeModule"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//LifeModule//Calendar//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("X-WR-CALNAME:$calendarName")

        for (event in events) {
            sb.append(generateVEvent(event))
        }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    private fun generateVEvent(event: CalendarEventEntity): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VEVENT")

        // UID - deterministic from event id + date
        sb.appendLine("UID:${event.uuid}-${event.date}@lifemodule.app")

        // DTSTAMP - current time per RFC 5545
        val now = timeProvider.now()
        sb.appendLine("DTSTAMP:${formatDateTime(now)}")

        // DTSTART & DTEND
        if (event.time != null) {
            // Timed event
            val startDt = parseLocalDateTime(event.date, event.time)
            sb.appendLine("DTSTART:${formatDateTime(startDt)}")

            if (event.endTime != null) {
                val endDt = parseLocalDateTime(event.endDate ?: event.date, event.endTime)
                sb.appendLine("DTEND:${formatDateTime(endDt)}")
            } else {
                // Default 1-hour duration
                sb.appendLine("DTEND:${formatDateTime(startDt.plusHours(1))}")
            }
        } else {
            // All-day event
            sb.appendLine("DTSTART;VALUE=DATE:${formatDate(event.date)}")
            if (event.endDate != null) {
                // ICS all-day DTEND is exclusive - add one day
                val endDate = LocalDate.parse(event.endDate, ISO_DATE).plusDays(1)
                sb.appendLine("DTEND;VALUE=DATE:${formatDate(endDate.toString())}")
            } else {
                val nextDay = LocalDate.parse(event.date, ISO_DATE).plusDays(1)
                sb.appendLine("DTEND;VALUE=DATE:${formatDate(nextDay.toString())}")
            }
        }

        // SUMMARY
        sb.appendLine("SUMMARY:${escapeIcs(event.title)}")

        // DESCRIPTION
        if (event.description.isNotBlank()) {
            sb.appendLine("DESCRIPTION:${escapeIcs(event.description)}")
        }

        // CATEGORIES
        val icsCategory = when (event.category) {
            "exam"     -> "EXAM"
            "deadline" -> "DEADLINE"
            "personal" -> "PERSONAL"
            "holiday"  -> "HOLIDAY"
            else       -> "OTHER"
        }
        sb.appendLine("CATEGORIES:$icsCategory")

        // Recurring marker (RRULE for yearly recurring events like birthdays)
        if (event.isRecurring) {
            sb.appendLine("RRULE:FREQ=YEARLY")
        }

        sb.appendLine("END:VEVENT")
        return sb.toString()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun formatDateTime(ldt: java.time.LocalDateTime): String {
        return "%04d%02d%02dT%02d%02d%02d".format(
            ldt.year, ldt.monthValue, ldt.dayOfMonth,
            ldt.hour, ldt.minute, ldt.second
        )
    }

    private fun formatDate(isoDate: String): String {
        // "2026-01-15" -> "20260115"
        return isoDate.replace("-", "")
    }

    private fun parseLocalDateTime(date: String, time: String): java.time.LocalDateTime {
        val ld = LocalDate.parse(date, ISO_DATE)
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return ld.atTime(hour, min)
    }

    /** Escape text values per RFC 5545 §3.3.11. */
    private fun escapeIcs(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")
}
