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

package de.lifemodule.app.data.worktime

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkTimeEntryEntity)

    @Update
    suspend fun update(entry: WorkTimeEntryEntity)

    @Delete
    suspend fun delete(entry: WorkTimeEntryEntity)

    @Query("SELECT * FROM work_time_entries WHERE clockOutMillis IS NULL LIMIT 1")
    fun getActiveEntry(): Flow<WorkTimeEntryEntity?>

    @Query("SELECT * FROM work_time_entries WHERE date LIKE :yearMonth || '%' ORDER BY clockInMillis DESC")
    fun getEntriesForMonth(yearMonth: String): Flow<List<WorkTimeEntryEntity>>

    @Query("SELECT * FROM work_time_entries ORDER BY clockInMillis DESC")
    fun getAllEntries(): Flow<List<WorkTimeEntryEntity>>
}
