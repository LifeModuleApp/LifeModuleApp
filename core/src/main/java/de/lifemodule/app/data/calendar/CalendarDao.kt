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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    @Query("SELECT * FROM calendar_events WHERE date = :date ORDER BY time")
    fun getEventsForDate(date: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE date >= :today ORDER BY date, time LIMIT :limit")
    fun getUpcomingEvents(today: String, limit: Int = 50): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE date >= :startDate AND date <= :endDate ORDER BY date, time")
    fun getEventsInRange(startDate: String, endDate: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT DISTINCT date FROM calendar_events WHERE date >= :startDate AND date <= :endDate")
    fun getDatesWithEvents(startDate: String, endDate: String): Flow<List<String>>

    /**
     * Get recurring events that match a specific month-day (MM-DD suffix) regardless of year.
     * Used for yearly birthdays and other recurring events.
     */
    @Query("SELECT * FROM calendar_events WHERE isRecurring = 1 AND substr(date, 6) = :monthDay")
    fun getRecurringEventsForMonthDay(monthDay: String): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CalendarEventEntity): Long

    @Delete
    suspend fun delete(event: CalendarEventEntity)
}
