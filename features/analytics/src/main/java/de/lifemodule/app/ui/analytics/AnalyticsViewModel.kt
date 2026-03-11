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

package de.lifemodule.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.analytics.ActivityLogger
import de.lifemodule.app.data.calendar.CalendarRepository
import de.lifemodule.app.data.gym.GymRepository
import de.lifemodule.app.data.habits.HabitRepository
import de.lifemodule.app.data.mentalhealth.MoodRepository
import de.lifemodule.app.data.nutrition.NutritionRepository
import de.lifemodule.app.data.supplements.SupplementRepository
import de.lifemodule.app.data.health.HealthRepository
import de.lifemodule.app.data.weight.WeightEntryEntity
import de.lifemodule.app.data.weight.WeightRepository
import de.lifemodule.app.data.worktime.WorkTimeEntryEntity
import de.lifemodule.app.data.worktime.WorkTimeRepository
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class NutritionDayStats(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)

data class ExerciseProgress(
    val date: String,
    val maxWeight: Double
)

data class HabitStreakInfo(
    val name: String,
    val emoji: String,
    val streak: Int
)

data class SupplementStreakInfo(
    val name: String,
    val streak: Int,
    val lastTaken: String?
)

/** 7-day completion grid for one habit */
data class HabitWeekInfo(
    val name: String,
    val emoji: String,
    val days: List<Boolean> // last 7 days, index 0 = oldest
)

/** Weight data point for trend chart */
data class WeightPoint(
    val date: String,
    val kg: Double
)

/** Weight forecast point (future projection) */
data class WeightForecastPoint(
    val dayOffset: Int,
    val kg: Double
)

/** Work-time daily hours for the current week */
data class WorkDayHours(
    val dayLabel: String,  // e.g. "Mo"
    val hours: Double
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    nutritionRepository: NutritionRepository,
    private val habitRepository: HabitRepository,
    private val moodRepository: MoodRepository,
    private val gymRepository: GymRepository,
    calendarRepository: CalendarRepository,
    supplementRepository: SupplementRepository,
    activityLogger: ActivityLogger,
    weightRepository: WeightRepository,
    workTimeRepository: WorkTimeRepository,
    private val healthRepository: HealthRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val today = timeProvider.today()
    private val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    // ── Mood ──
    val todayMood = moodRepository.getEntryForDate(todayStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val moodRange = MutableStateFlow(7) // 7 or 30
    val recentMoods = MutableStateFlow<List<de.lifemodule.app.data.mentalhealth.MoodEntryEntity>>(emptyList())

    // ── Nutrition today ──
    val todayNutrition: StateFlow<NutritionDayStats> = nutritionRepository
        .getEntriesForDateRange(todayStr, todayStr)
        .map { entries ->
            var kcal = 0.0; var protein = 0.0; var carbs = 0.0; var fat = 0.0
            entries.forEach { ewf ->
                val grams = ewf.entry.gramsConsumed
                val factor = grams / 100.0
                kcal += ewf.foodItem.kcalPer100g * factor
                protein += ewf.foodItem.proteinPer100g * factor
                carbs += ewf.foodItem.carbsPer100g * factor
                fat += ewf.foodItem.fatPer100g * factor
            }
            NutritionDayStats(kcal, protein, carbs, fat)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionDayStats())

    // ── Habits streaks ──
    val habitStreaks = MutableStateFlow<List<HabitStreakInfo>>(emptyList())

    // ── Habit weekly completion grid ──
    val habitWeekGrid = MutableStateFlow<List<HabitWeekInfo>>(emptyList())

    // ── Supplement streaks ──
    val supplementStreaks: StateFlow<List<SupplementStreakInfo>> = combine(
        supplementRepository.getAllSupplements(),
        supplementRepository.getAllTakenLogs()
    ) { supplements, logs ->
        supplements.filter { it.isActive }.map { supp ->
            val suppLogs = logs.filter { it.supplementId == supp.uuid }
                .map { it.date }
                .distinct()
                .sorted()
                .reversed()
            val streak = calculateConsecutiveDays(suppLogs)
            SupplementStreakInfo(
                name = supp.name,
                streak = streak,
                lastTaken = suppLogs.firstOrNull()
            )
        }.sortedByDescending { it.streak }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Gym ──
    val exerciseNames = gymRepository.getDistinctExerciseNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedExercise = MutableStateFlow<String?>(null)
    val exerciseProgress = MutableStateFlow<List<ExerciseProgress>>(emptyList())

    val totalWorkouts = gymRepository.getTotalSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentWorkouts = gymRepository.getRecentSessionsWithSets(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Activity Log ──
    val totalActions = activityLogger.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Upcoming Events ──
    val upcomingEvents = calendarRepository.getUpcomingEvents(todayStr, 5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Health / Activity ──
    val todaySteps = MutableStateFlow(0L)
    val todayCalories = MutableStateFlow(0.0)

    // ── Weight trend (range: 7 = week, 30 = month, 365 = year) ──
    val weightRange = MutableStateFlow(30) // default = month
    val weightTrend: StateFlow<List<WeightPoint>> = combine(
        weightRepository.getAllEntries(),
        weightRange
    ) { entries, range ->
        val cutoff = today.minusDays(range.toLong())
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        entries.filter { it.date >= cutoff }
            .sortedBy { it.date }
            .map { e -> WeightPoint(date = e.date, kg = e.weightKg) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Linear-regression forecast: projects trend forward by ~30 % of range */
    val weightForecast: StateFlow<List<WeightForecastPoint>> = weightTrend.map { points ->
        if (points.size < 2) return@map emptyList()
        // Simple linear regression: y = slope * x + intercept
        val n = points.size
        val xs = (0 until n).map { it.toDouble() }
        val ys = points.map { it.kg }
        val xMean = xs.average()
        val yMean = ys.average()
        val numerator = xs.zip(ys).sumOf { (x, y) -> (x - xMean) * (y - yMean) }
        val denominator = xs.sumOf { x -> (x - xMean) * (x - xMean) }
        if (denominator == 0.0) return@map emptyList()
        val slope = numerator / denominator
        val intercept = yMean - slope * xMean
        // Forecast forward: 30 % of data length (min 3, max 30 points)
        val forecastLen = (n * 0.3).toInt().coerceIn(3, 30)
        (0..forecastLen).map { i ->
            val x = n - 1 + i // continues from the last data point
            WeightForecastPoint(dayOffset = i, kg = slope * x + intercept)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setWeightRange(days: Int) {
        weightRange.value = days
    }

    // ── Work-time weekly bar chart ──
    val workWeekHours: StateFlow<List<WorkDayHours>> = run {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val yearMonth = weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val shortDay = DateTimeFormatter.ofPattern("EE") // locale-aware: "Mo"/"Mon"
        workTimeRepository.getEntriesForMonth(yearMonth).map { entries ->
            (0..6).map { offset ->
                val date = weekStart.plusDays(offset.toLong())
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val dayEntries = entries.filter { it.date == dateStr && it.clockOutMillis != null }
                val totalMs = dayEntries.sumOf { (it.clockOutMillis!! - it.clockInMillis) - (it.breakMinutes * 60_000L) }
                val hours = totalMs / 3_600_000.0
                WorkDayHours(dayLabel = date.format(shortDay), hours = hours.coerceAtLeast(0.0))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHabitStreaks()
        loadMoods()
        loadHabitWeekGrid()
        loadHealthData()
    }

    private fun loadHealthData() {
        viewModelScope.launch {
            try {
                val steps = healthRepository.fetchDailySteps()
                todaySteps.value = steps
                todayCalories.value = steps * 0.04
            } catch (e: Exception) {
                Timber.e(e, "[Analytics] Failed to load health data")
            }
        }
    }

    /** Switch mood chart between 7-day and 30-day range */
    fun setMoodRange(days: Int) {
        moodRange.value = days
        loadMoods()
    }

    private fun loadMoods() {
        viewModelScope.launch {
            try {
                moodRepository.getRecentEntries(moodRange.value).collect { moods ->
                    recentMoods.value = moods
                }
            } catch (e: Exception) {
                Timber.e(e, "[Analytics] Failed to load moods")
            }
        }
    }

    private fun loadHabitStreaks() {
        viewModelScope.launch {
            try {
                habitRepository.getActiveHabits().collect { activeHabits ->
                    val streaks = activeHabits.map { habit ->
                        val streak = habitRepository.calculateStreak(habit.uuid)
                        HabitStreakInfo(name = habit.name, emoji = habit.emoji, streak = streak)
                    }.sortedByDescending { it.streak }
                    habitStreaks.value = streaks
                }
            } catch (e: Exception) {
                Timber.e(e, "[Analytics] Failed to load habit streaks")
            }
        }
    }

    private fun loadHabitWeekGrid() {
        viewModelScope.launch {
            try {
                val last7Dates = (6 downTo 0).map {
                    today.minusDays(it.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                }
                habitRepository.getActiveHabits().collect { activeHabits ->
                    // Batch query: fetch all logs for the date range in one query
                    val allLogs = habitRepository.getLogsForDateRange(
                        startDate = last7Dates.last(),
                        endDate = last7Dates.first()
                    )
                    val logLookup = allLogs
                        .filter { it.completed }
                        .map { it.habitId to it.date }
                        .toSet()

                    val grid = activeHabits.map { habit ->
                        val days = last7Dates.map { date ->
                            (habit.uuid to date) in logLookup
                        }
                        HabitWeekInfo(name = habit.name, emoji = habit.emoji, days = days)
                    }
                    habitWeekGrid.value = grid
                }
            } catch (e: Exception) {
                Timber.e(e, "[Analytics] Failed to load habit week grid")
            }
        }
    }

    fun selectExercise(name: String) {
        selectedExercise.value = name
        viewModelScope.launch {
            try {
                gymRepository.getSetHistoryByExercise(name).collect { sets ->
                    val progression = sets
                        .groupBy { it.sessionId }
                        .entries
                        .mapIndexed { index, (_, sessionSets) ->
                            ExerciseProgress(
                                date = "#${index + 1}",
                                maxWeight = sessionSets.mapNotNull { it.weightKg }.maxOrNull() ?: 0.0
                            )
                        }
                    exerciseProgress.value = progression
                }
            } catch (e: Exception) {
                Timber.e(e, "[Analytics] Failed to load exercise progress for '%s'", name)
            }
        }
    }

    private fun calculateConsecutiveDays(dateStrings: List<String>): Int {
        if (dateStrings.isEmpty()) return 0
        val dates = dateStrings.mapNotNull {
            runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        }.sortedDescending()
        if (dates.isEmpty()) return 0

        var streak = 0
        var checkDate = timeProvider.today()

        if (!dates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        val dateSet = dates.toSet()
        while (dateSet.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }
}
