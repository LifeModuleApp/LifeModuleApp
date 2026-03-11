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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // ── Habits ──
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY timeOfDay, name")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY isActive DESC, name")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    /** Import-safe: returns -1 if UUID already exists (never overwrites). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHabitIgnore(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    // ── Logs ──
    @Transaction
    @Query("SELECT * FROM habit_logs WHERE date = :date ORDER BY habitId")
    fun getLogsWithHabitForDate(date: String): Flow<List<LogWithHabit>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitUuid AND date = :date LIMIT 1")
    suspend fun getLogForHabitAndDate(habitUuid: String, date: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE date >= :startDate AND date <= :endDate")
    suspend fun getLogsForDateRange(startDate: String, endDate: String): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: HabitLogEntity): Long

    @Update
    suspend fun updateLog(log: HabitLogEntity)

    // ── Streak calculation ──
    @Query("""
        SELECT * FROM habit_logs 
        WHERE habitId = :habitUuid AND completed = 1 
        ORDER BY date DESC
    """)
    suspend fun getCompletedLogsForHabit(habitUuid: String): List<HabitLogEntity>

    // ── Analytics ──
    @Query("SELECT * FROM habit_logs WHERE completed = 1 ORDER BY date DESC")
    fun getAllCompletedLogs(): Flow<List<HabitLogEntity>>
}
