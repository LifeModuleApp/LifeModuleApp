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

package de.lifemodule.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.schedule.CourseEntity
import de.lifemodule.app.data.schedule.CourseRepository
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    // Current view mode
    val viewMode = MutableStateFlow("week") // "week" or "day"

    // Current selected day (for day view)
    val selectedDay = MutableStateFlow(timeProvider.today().dayOfWeek.value - 1) // 0=Mon

    // Current semester filter
    val selectedSemester = MutableStateFlow("")

    // All available semesters
    val semesters: StateFlow<List<String>> = repository.getAllSemesters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All courses for the selected semester (for week grid)
    @OptIn(ExperimentalCoroutinesApi::class)
    val coursesForSemester: StateFlow<List<CourseEntity>> = selectedSemester
        .flatMapLatest { semester ->
            if (semester.isBlank()) repository.getActiveCourses()
            else repository.getCoursesForSemester(semester)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Courses for the selected day (for day view)
    @OptIn(ExperimentalCoroutinesApi::class)
    val coursesForDay: StateFlow<List<CourseEntity>> = selectedDay
        .flatMapLatest { day -> repository.getCoursesForDay(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDay(day: Int) { selectedDay.value = day }
    fun setViewMode(mode: String) { viewMode.value = mode }
    fun setSemester(semester: String) { selectedSemester.value = semester }
    fun today(): LocalDate = timeProvider.today()
    fun currentTime(): LocalTime = timeProvider.currentTime()

    fun addCourse(
        name: String, professor: String, room: String,
        dayOfWeek: Int, startTime: String, endTime: String,
        color: String, semester: String, weekType: String, courseType: String
    ) {
        viewModelScope.launch {
            try {
                repository.insert(
                    CourseEntity(
                        name = name, professor = professor, room = room,
                        dayOfWeek = dayOfWeek, startTime = startTime,
                        endTime = endTime, color = color,
                        semester = semester, weekType = weekType, courseType = courseType
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Schedule] Failed to add course '%s'", name)
            }
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            try { repository.delete(course) }
            catch (e: Exception) { Timber.e(e, "[Schedule] Failed to delete course '%s'", course.name) }
        }
    }
}
