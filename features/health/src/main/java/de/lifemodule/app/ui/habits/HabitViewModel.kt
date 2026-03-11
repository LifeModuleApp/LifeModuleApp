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

package de.lifemodule.app.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.habits.HabitEntity
import de.lifemodule.app.data.habits.HabitLogEntity
import de.lifemodule.app.data.habits.HabitRepository
import de.lifemodule.app.data.habits.LogWithHabit
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HabitWithStreak(
    val habit: HabitEntity,
    val streak: Int
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(timeProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val activeHabits: StateFlow<List<HabitEntity>> = repository.getActiveHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs: StateFlow<List<LogWithHabit>> = _selectedDate
        .flatMapLatest { date ->
            repository.getLogsWithHabitForDate(date.format(dateFormatter))
                .map { logs ->
                    logs.sortedWith(
                        compareBy<LogWithHabit> {
                            when (it.habit.timeOfDay) {
                                "morning" -> 1
                                "noon"    -> 2
                                "evening" -> 3
                                else      -> 4
                            }
                        }.thenBy { it.habit.name }
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _streaks = MutableStateFlow<Map<String, Int>>(emptyMap())
    val streaks: StateFlow<Map<String, Int>> = _streaks.asStateFlow()

    fun refreshStreaks() {
        viewModelScope.launch {
            try {
                val active = activeHabits.value
                val streakMap = mutableMapOf<String, Int>()
                for (habit in active) {
                    streakMap[habit.uuid] = repository.calculateStreak(habit.uuid)
                }
                _streaks.value = streakMap
            } catch (e: Exception) {
                Timber.e(e, "[Habits] Failed to refresh streaks")
            }
        }
    }

    /**
     * Create today's logs for habits that are due today.
     *
     * Waits for the first non-empty emission from activeHabits so we don't
     * silently skip log creation when called before Room has loaded data.
     * Falls back immediately if there truly are no habits (empty after first emit).
     *
     * DB-level unique index on (habitId, date) + IGNORE strategy is the final safety net.
     */
    fun ensureTodayLogs() {
        viewModelScope.launch {
            try {
                val today = _selectedDate.value
                val todayStr = today.format(dateFormatter)

                val habits = activeHabits.value
                for (habit in habits) {
                    if (isHabitDueToday(habit, today)) {
                        repository.insertLog(
                            HabitLogEntity(
                                habitId = habit.uuid,
                                date = todayStr,
                                completed = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "[Habits] Failed to ensure today's logs")
            }
        }
    }

    private fun isHabitDueToday(habit: HabitEntity, today: LocalDate): Boolean {
        if (habit.repeatIntervalDays <= 1) return true // daily
        val createdDate = LocalDate.ofEpochDay(habit.createdAt / 86_400_000)
        val daysSinceCreation = ChronoUnit.DAYS.between(createdDate, today)
        return daysSinceCreation % habit.repeatIntervalDays == 0L
    }

    fun toggleCompleted(log: HabitLogEntity) {
        viewModelScope.launch {
            try {
                val updated = log.copy(
                    completed = !log.completed,
                    completedAtMillis = if (!log.completed) timeProvider.currentTimeMillis() else null
                )
                repository.updateLog(updated)
                refreshStreaks()
            } catch (e: Exception) {
                Timber.e(e, "[Habits] Failed to toggle completed for log %s", log.uuid)
            }
        }
    }

    fun addHabit(
        name: String,
        emoji: String,
        frequency: String,
        repeatIntervalDays: Int,
        timeOfDay: String,
        isPositive: Boolean,
        imagePath: String?
    ) {
        viewModelScope.launch {
            try {
                repository.insertHabit(
                    HabitEntity(
                        name = name,
                        emoji = emoji,
                        frequency = frequency,
                        repeatIntervalDays = repeatIntervalDays,
                        timeOfDay = timeOfDay,
                        isPositive = isPositive,
                        imagePath = imagePath
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Habits] Failed to add habit '%s'", name)
            }
        }
    }

    fun archiveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            try { repository.updateHabit(habit.copy(isActive = false)) }
            catch (e: Exception) { Timber.e(e, "[Habits] Failed to archive '%s'", habit.name) }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            try { repository.deleteHabit(habit) }
            catch (e: Exception) { Timber.e(e, "[Habits] Failed to delete '%s'", habit.name) }
        }
    }
}
