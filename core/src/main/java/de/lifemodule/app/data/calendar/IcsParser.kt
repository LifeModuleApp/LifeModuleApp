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

import timber.log.Timber
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import de.lifemodule.app.data.error.ErrorLogger
import kotlinx.coroutines.runBlocking

/**
 * Parses iCalendar (.ics / RFC 5545) streams into [CalendarEventEntity] items.
 *
 * Supports:
 *  - VEVENT components with DTSTART, DTEND, SUMMARY, DESCRIPTION, CATEGORIES
 *  - DATE and DATE-TIME value types (with and without VALUE=DATE parameter)
 *  - Line unfolding (RFC 5545 §3.1)
 *
 * 100 % local - no network, no third-party library.
 */
@Singleton
class IcsParser @Inject constructor(
    private val errorLogger: ErrorLogger
) {

    companion object {
        private const val TAG = "IcsParser"
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5 MB
        private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    /**
     * Parses the given [inputStream] and returns a list of [CalendarEventEntity].
     * Invalid or unsupported entries are silently skipped.
     */
    fun parse(inputStream: InputStream): List<CalendarEventEntity> {
        val maxChars = MAX_FILE_SIZE
        val builder = StringBuilder(minOf(64 * 1024, maxChars))
        var truncated = false

        inputStream.bufferedReader().use { reader ->
            val buffer = CharArray(8192)
            while (true) {
                val read = reader.read(buffer)
                if (read <= 0) break

                val remaining = maxChars - builder.length
                if (remaining <= 0) {
                    truncated = true
                    break
                }

                val toAppend = minOf(read, remaining)
                builder.append(buffer, 0, toAppend)

                if (toAppend < read) {
                    truncated = true
                    break
                }
            }
        }

        if (truncated) {
            runBlocking {
                errorLogger.logError(TAG, "ICS file exceeds ${MAX_FILE_SIZE / 1024 / 1024} MB limit, truncating", null, "WARNING")
            }
        }

        return parse(builder.toString())
    }

    /**
     * Parses raw ICS text and returns a list of [CalendarEventEntity].
     */
    fun parse(icsText: String): List<CalendarEventEntity> {
        // Unfold lines: lines that start with a space or tab are continuations
        val unfolded = icsText
            .replace("\r\n ", "")
            .replace("\r\n\t", "")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val events = mutableListOf<CalendarEventEntity>()
        var inEvent = false
        var summary = ""
        var description = ""
        var dtStart = ""
        var dtEnd = ""
        var categories = ""

        for (line in unfolded.lines()) {
            val trimmed = line.trim()

            when {
                trimmed.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true
                    summary = ""
                    description = ""
                    dtStart = ""
                    dtEnd = ""
                    categories = ""
                }

                trimmed.equals("END:VEVENT", ignoreCase = true) -> {
                    if (inEvent && summary.isNotBlank() && dtStart.isNotBlank()) {
                        try {
                            val event = buildEvent(summary, description, dtStart, dtEnd, categories)
                            if (event != null) events.add(event)
                        } catch (e: Exception) {
                            runBlocking {
                                errorLogger.logError(TAG, "Skipping malformed VEVENT: ${e.message}", e, "WARNING")
                            }
                        }
                    }
                    inEvent = false
                }

                inEvent -> {
                    val (key, value) = parseProperty(trimmed) ?: continue
                    when {
                        key.startsWith("SUMMARY")     -> summary = unescapeIcs(value)
                        key.startsWith("DESCRIPTION") -> description = unescapeIcs(value)
                        key.startsWith("DTSTART")     -> dtStart = value
                        key.startsWith("DTEND")       -> dtEnd = value
                        key.startsWith("CATEGORIES")  -> categories = value.lowercase()
                    }
                }
            }
        }

        Timber.d("Parsed %d events from ICS", events.size)
        return events
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun parseProperty(line: String): Pair<String, String>? {
        val colonIndex = line.indexOf(':')
        if (colonIndex < 0) return null
        return line.substring(0, colonIndex).uppercase() to line.substring(colonIndex + 1)
    }

    private fun buildEvent(
        summary: String,
        description: String,
        dtStart: String,
        dtEnd: String,
        categories: String
    ): CalendarEventEntity? {
        val (startDate, startTime) = parseDtValue(dtStart)
        val (endDate, endTime) = if (dtEnd.isNotBlank()) parseDtValue(dtEnd) else (null to null)

        if (startDate == null) return null

        val category = mapCategory(categories)
        val color = categoryColor(category)

        return CalendarEventEntity(
            title = summary,
            description = description,
            date = startDate,
            endDate = endDate,
            time = startTime,
            endTime = endTime,
            category = category,
            color = color,
            isHoliday = false,
            isRecurring = false
        )
    }

    /**
     * Parses a DTSTART/DTEND value, handling:
     *  - "20260115T140000"      -> ("2026-01-15", "14:00")   - local time
     *  - "20260115T140000Z"     -> converted from UTC to device local time
     *  - "20260115"             -> ("2026-01-15", null)
     */
    private fun parseDtValue(raw: String): Pair<String?, String?> {
        val isUtc = raw.endsWith('Z', ignoreCase = true)
        val cleaned = raw.trimEnd('Z', 'z')

        return if (cleaned.contains('T')) {
            try {
                val ldt = LocalDateTime.parse(cleaned, DATE_TIME_FORMAT)
                val localDt = if (isUtc) {
                    // Convert UTC -> device local time zone
                    ldt.toInstant(ZoneOffset.UTC)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                } else {
                    ldt
                }
                localDt.toLocalDate().toString() to "%02d:%02d".format(localDt.hour, localDt.minute)
            } catch (e: Exception) {
                Timber.w(e, "[IcsParser] Failed to parse datetime: %s", raw)
                null to null
            }
        } else {
            try {
                val ld = LocalDate.parse(cleaned, DATE_FORMAT)
                ld.toString() to null
            } catch (e: Exception) {
                Timber.w(e, "[IcsParser] Failed to parse date: %s", raw)
                null to null
            }
        }
    }

    /** Maps ICS CATEGORIES to the app's category system. */
    private fun mapCategory(categories: String): String = when {
        categories.contains("exam") || categories.contains("klausur")   -> "exam"
        categories.contains("deadline")                                  -> "deadline"
        categories.contains("birthday") || categories.contains("geburtstag") -> "personal"
        categories.contains("holiday") || categories.contains("feiertag")    -> "holiday"
        categories.contains("personal") || categories.contains("privat")     -> "personal"
        else -> "other"
    }

    /** Default color for a given category. */
    private fun categoryColor(category: String): String = when (category) {
        "exam"     -> "#FF9F0A"
        "deadline" -> "#FF453A"
        "holiday"  -> "#FF453A"
        "personal" -> "#30D158"
        else       -> "#0A84FF"
    }

    /** Unescape ICS text values (RFC 5545 §3.3.11). */
    private fun unescapeIcs(value: String): String = value
        .replace("\\n", "\n")
        .replace("\\N", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
}
