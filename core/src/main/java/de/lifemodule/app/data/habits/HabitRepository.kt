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

package de.lifemodule.app.data.habits

import de.lifemodule.app.util.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val timeProvider: TimeProvider
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getActiveHabits(): Flow<List<HabitEntity>> = habitDao.getActiveHabits()
    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    suspend fun insertHabit(habit: HabitEntity): Long = habitDao.insertHabit(habit)
    suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)
    suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)

    fun getLogsWithHabitForDate(date: String): Flow<List<LogWithHabit>> =
        habitDao.getLogsWithHabitForDate(date)

    suspend fun getLogForHabitAndDate(habitUuid: String, date: String): HabitLogEntity? =
        habitDao.getLogForHabitAndDate(habitUuid, date)

    suspend fun getLogsForDateRange(startDate: String, endDate: String): List<HabitLogEntity> =
        habitDao.getLogsForDateRange(startDate, endDate)

    suspend fun insertLog(log: HabitLogEntity): Long = habitDao.insertLog(log)
    suspend fun updateLog(log: HabitLogEntity) = habitDao.updateLog(log)

    /**
     * Calculate current streak for a habit.
     * Counts consecutive days with a completed log going back from today.
     * If today has no log yet, starts counting from yesterday.
     */
    suspend fun calculateStreak(habitUuid: String): Int {
        val completedLogs = habitDao.getCompletedLogsForHabit(habitUuid)
        if (completedLogs.isEmpty()) return 0

        val completedDates = completedLogs
            .mapNotNull { runCatching { LocalDate.parse(it.date, dateFormatter) }.getOrNull() }
            .toSortedSet(compareByDescending { it })

        var streak = 0
        var checkDate = timeProvider.today()

        // If today is not yet completed, start checking from yesterday
        if (!completedDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        while (completedDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }
}
