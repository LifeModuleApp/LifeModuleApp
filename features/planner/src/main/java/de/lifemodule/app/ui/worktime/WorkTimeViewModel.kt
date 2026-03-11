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

package de.lifemodule.app.ui.worktime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.worktime.WorkTimeEntryEntity
import de.lifemodule.app.data.worktime.WorkTimeRepository
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class WorkTimeViewModel @Inject constructor(
    private val repository: WorkTimeRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val activeEntry = repository.getActiveEntry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentMonthEntries = repository.getEntriesForMonth(
        YearMonth.now().toString().substring(0, 7)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Method 1: Live timer ──────────────────────────────────────────────────

    fun clockIn() {
        viewModelScope.launch {
            try {
                repository.insert(
                    WorkTimeEntryEntity(
                        date = timeProvider.today().toString(),
                        clockInMillis = timeProvider.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[WorkTime] Failed to clock in")
            }
        }
    }

    fun clockOut(entry: WorkTimeEntryEntity) {
        viewModelScope.launch {
            try { repository.update(entry.copy(clockOutMillis = timeProvider.currentTimeMillis())) }
            catch (e: Exception) { Timber.e(e, "[WorkTime] Failed to clock out for %s", entry.uuid) }
        }
    }

    // ── Method 2: Manual start/stop (today, no live counter) ──────────────────

    fun insertManualEntry(startTime: LocalTime, endTime: LocalTime, breakMin: Int) {
        viewModelScope.launch {
            try {
                val today = timeProvider.today()
                val zone = ZoneId.systemDefault()
                val clockIn = today.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
                val clockOut = today.atTime(endTime).atZone(zone).toInstant().toEpochMilli()
                repository.insert(
                    WorkTimeEntryEntity(
                        date = today.toString(),
                        clockInMillis = clockIn,
                        clockOutMillis = clockOut,
                        breakMinutes = breakMin
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[WorkTime] Failed to insert manual entry")
            }
        }
    }

    // ── Method 3: Retroactive entry (any date) ───────────────────────────────

    fun insertRetroactiveEntry(
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        breakMin: Int,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val zone = ZoneId.systemDefault()
                val clockIn = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
                val clockOut = date.atTime(endTime).atZone(zone).toInstant().toEpochMilli()
                repository.insert(
                    WorkTimeEntryEntity(
                        date = date.toString(),
                        clockInMillis = clockIn,
                        clockOutMillis = clockOut,
                        breakMinutes = breakMin,
                        notes = notes
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[WorkTime] Failed to insert retroactive entry for %s", date)
            }
        }
    }

    fun deleteEntry(entry: WorkTimeEntryEntity) {
        viewModelScope.launch {
            try { repository.delete(entry) }
            catch (e: Exception) { Timber.e(e, "[WorkTime] Failed to delete entry %s", entry.uuid) }
        }
    }

    fun today(): LocalDate = timeProvider.today()
}
