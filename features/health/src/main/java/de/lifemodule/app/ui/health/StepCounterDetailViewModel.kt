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

package de.lifemodule.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lifemodule.app.data.health.DailySteps
import de.lifemodule.app.data.health.HealthRepository
import de.lifemodule.app.util.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import java.time.temporal.ChronoUnit

enum class StepTimeScale {
    WEEK, MONTH, YEAR
}

@HiltViewModel
class StepCounterDetailViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _timeScale = MutableStateFlow(StepTimeScale.WEEK)
    val timeScale: StateFlow<StepTimeScale> = _timeScale

    private val _timeOffset = MutableStateFlow(0)
    val timeOffset: StateFlow<Int> = _timeOffset

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _dailySteps = MutableStateFlow<List<DailySteps>>(emptyList())
    val dailySteps: StateFlow<List<DailySteps>> = _dailySteps

    val currentRangeLabel: StateFlow<String> = combine(_timeScale, _timeOffset) { scale, offset ->
        val today = timeProvider.today()
        when (scale) {
            StepTimeScale.WEEK -> {
                if (offset == 0) "Letzte 7 Tage"
                else {
                    val start = today.plusWeeks(offset.toLong()).minusDays(6)
                    val end = today.plusWeeks(offset.toLong())
                    "${start.format(DateTimeFormatter.ofPattern("dd.MM."))} - ${end.format(DateTimeFormatter.ofPattern("dd.MM."))}"
                }
            }
            StepTimeScale.MONTH -> {
                val targetMonth = today.plusMonths(offset.toLong())
                if (offset == 0) "Letzte 30 Tage"
                else targetMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + targetMonth.year
            }
            StepTimeScale.YEAR -> {
                val targetYear = today.plusYears(offset.toLong())
                targetYear.year.toString()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        loadData()
    }

    fun setTimeScale(scale: StepTimeScale) {
        if (_timeScale.value != scale) {
            _timeScale.value = scale
            _timeOffset.value = 0
            loadData()
        }
    }

    fun shiftOffset(delta: Int) {
        _timeOffset.value += delta
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val today = timeProvider.today()
                val scale = _timeScale.value
                val offset = _timeOffset.value.toLong()

                val (start, end) = when (scale) {
                    StepTimeScale.WEEK -> {
                        if (offset == 0L) {
                            today.minusDays(6) to today
                        } else {
                            val endDay = today.plusWeeks(offset)
                            endDay.minusDays(6) to endDay
                        }
                    }
                    StepTimeScale.MONTH -> {
                        if (offset == 0L) {
                            today.minusDays(29) to today
                        } else {
                            val targetMonth = today.plusMonths(offset)
                            val startDay = targetMonth.withDayOfMonth(1)
                            val endDay = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth())
                            startDay to endDay
                        }
                    }
                    StepTimeScale.YEAR -> {
                        if (offset == 0L) {
                            today.minusDays(364) to today
                        } else {
                            val targetYear = today.plusYears(offset)
                            val startDay = targetYear.withDayOfYear(1)
                            val endDay = targetYear.withDayOfYear(targetYear.lengthOfYear())
                            startDay to endDay
                        }
                    }
                }

                _dailySteps.value = repository.fetchStepsByDateRange(start, end).sortedBy { it.date }
            } catch (e: Exception) {
                Timber.e(e, "[Health] Failed to load step data")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
