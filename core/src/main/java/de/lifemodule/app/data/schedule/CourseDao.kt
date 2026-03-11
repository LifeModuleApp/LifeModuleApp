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

package de.lifemodule.app.data.schedule

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses WHERE isActive = 1 ORDER BY dayOfWeek, startTime")
    fun getActiveCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE isActive = 1 AND semester = :semester ORDER BY dayOfWeek, startTime")
    fun getCoursesForSemester(semester: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :day AND isActive = 1 ORDER BY startTime")
    fun getCoursesForDay(day: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :day AND isActive = 1 AND semester = :semester ORDER BY startTime")
    fun getCoursesForDayAndSemester(day: Int, semester: String): Flow<List<CourseEntity>>

    @Query("SELECT DISTINCT semester FROM courses WHERE isActive = 1 AND semester != '' ORDER BY semester DESC")
    fun getAllSemesters(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity): Long

    @Delete
    suspend fun delete(course: CourseEntity)
}
