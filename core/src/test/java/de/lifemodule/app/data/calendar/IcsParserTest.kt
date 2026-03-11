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

import de.lifemodule.app.data.error.ErrorLogDao
import de.lifemodule.app.data.error.ErrorLogEntity
import de.lifemodule.app.data.error.ErrorLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IcsParserTest {

    private val parser = IcsParser(ErrorLogger(FakeErrorLogDao()))

    @Test
    fun parse_singleEvent_returnsMappedEntity() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            SUMMARY:Workout Session
            DESCRIPTION:Leg day
            DTSTART:20260310T183000
            DTEND:20260310T193000
            CATEGORIES:exam
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("Workout Session", event.title)
        assertEquals("Leg day", event.description)
        assertEquals("2026-03-10", event.date)
        assertEquals("18:30", event.time)
        assertEquals("19:30", event.endTime)
        assertEquals("exam", event.category)
    }

    @Test
    fun parse_allDayEvent_hasDateWithoutTime() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            SUMMARY:Holiday
            DTSTART;VALUE=DATE:20261224
            CATEGORIES:holiday
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("2026-12-24", event.date)
        assertNull(event.time)
        assertEquals("holiday", event.category)
    }

    private class FakeErrorLogDao : ErrorLogDao {
        override fun getAllErrorLogs(): Flow<List<ErrorLogEntity>> = emptyFlow()
        override suspend fun insertErrorLog(log: ErrorLogEntity) = Unit
        override suspend fun deleteOldLogs(cutoffMillis: Long) = Unit
        override suspend fun deleteAllErrorLogs() = Unit
    }
}
