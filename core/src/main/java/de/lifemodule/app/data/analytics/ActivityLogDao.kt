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

package de.lifemodule.app.data.analytics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ActivityLogEntity)

    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_log WHERE module = :module ORDER BY timestamp DESC")
    fun getByModule(module: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_log WHERE date >= :startDate AND date <= :endDate ORDER BY timestamp DESC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT COUNT(*) FROM activity_log")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ActivityLogEntity>>
}
