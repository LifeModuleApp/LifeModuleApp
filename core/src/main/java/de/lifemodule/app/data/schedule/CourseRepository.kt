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

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CourseRepository @Inject constructor(private val dao: CourseDao) {
    fun getActiveCourses(): Flow<List<CourseEntity>> = dao.getActiveCourses()
    fun getCoursesForSemester(semester: String): Flow<List<CourseEntity>> = dao.getCoursesForSemester(semester)
    fun getCoursesForDay(day: Int): Flow<List<CourseEntity>> = dao.getCoursesForDay(day)
    fun getCoursesForDayAndSemester(day: Int, semester: String): Flow<List<CourseEntity>> = dao.getCoursesForDayAndSemester(day, semester)
    fun getAllSemesters(): Flow<List<String>> = dao.getAllSemesters()
    suspend fun insert(course: CourseEntity): Long = dao.insert(course)
    suspend fun delete(course: CourseEntity) = dao.delete(course)
}
