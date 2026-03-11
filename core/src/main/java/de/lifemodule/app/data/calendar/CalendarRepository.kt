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

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalendarRepository @Inject constructor(
    private val calendarDao: CalendarDao
) {
    fun getEventsForDate(date: String): Flow<List<CalendarEventEntity>> = calendarDao.getEventsForDate(date)
    fun getUpcomingEvents(today: String, limit: Int = 50): Flow<List<CalendarEventEntity>> = calendarDao.getUpcomingEvents(today, limit)
    fun getEventsInRange(startDate: String, endDate: String): Flow<List<CalendarEventEntity>> = calendarDao.getEventsInRange(startDate, endDate)
    fun getDatesWithEvents(startDate: String, endDate: String): Flow<List<String>> = calendarDao.getDatesWithEvents(startDate, endDate)
    fun getRecurringEventsForMonthDay(monthDay: String): Flow<List<CalendarEventEntity>> = calendarDao.getRecurringEventsForMonthDay(monthDay)
    suspend fun insert(event: CalendarEventEntity): Long = calendarDao.insert(event)
    suspend fun delete(event: CalendarEventEntity) = calendarDao.delete(event)
}
