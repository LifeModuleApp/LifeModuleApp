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

package de.lifemodule.app.ui.mentalhealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.mentalhealth.MoodEntryEntity
import de.lifemodule.app.data.mentalhealth.MoodRepository
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MoodViewModel @Inject constructor(
    private val repository: MoodRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val today = timeProvider.today().format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** All mood entries recorded today (multiple entries allowed). */
    val todayEntries: StateFlow<List<MoodEntryEntity>> = repository.getAllEntriesForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Today's aggregated mood (average of all entries today).
     * This prevents the first entry from "dominating" - every entry has equal weight.
     */
    val todayEntry: StateFlow<MoodEntryEntity?> = todayEntries
        .map { entries ->
            if (entries.isEmpty()) null
            else if (entries.size == 1) entries.first()
            else {
                // Average all numeric values across today's entries
                entries.first().copy(
                    moodLevel = entries.map { it.moodLevel }.average().toInt(),
                    energyLevel = entries.map { it.energyLevel }.average().toInt(),
                    stressLevel = entries.map { it.stressLevel }.average().toInt(),
                    sleepQuality = entries.map { it.sleepQuality }.average().toInt(),
                    positiveNotes = entries.mapNotNull { it.positiveNotes.takeIf { n -> n.isNotBlank() } }
                        .joinToString(" · "),
                    negativeNotes = entries.mapNotNull { it.negativeNotes.takeIf { n -> n.isNotBlank() } }
                        .joinToString(" · ")
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentEntries: StateFlow<List<MoodEntryEntity>> = repository.getRecentEntries(14)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveMoodEntry(
        moodLevel: Int,
        energyLevel: Int,
        stressLevel: Int,
        sleepQuality: Int,
        positiveNotes: String,
        negativeNotes: String
    ) {
        viewModelScope.launch {
            try {
                repository.insert(
                    MoodEntryEntity(
                        date = today,
                        moodLevel = moodLevel,
                        energyLevel = energyLevel,
                        stressLevel = stressLevel,
                        sleepQuality = sleepQuality,
                        positiveNotes = positiveNotes,
                        negativeNotes = negativeNotes
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[MentalHealth] Failed to save mood entry")
            }
        }
    }

    fun updateEntry(entry: MoodEntryEntity) {
        viewModelScope.launch {
            try { repository.update(entry) }
            catch (e: Exception) { Timber.e(e, "[MentalHealth] Failed to update entry %s", entry.uuid) }
        }
    }

    fun deleteEntry(entry: MoodEntryEntity) {
        viewModelScope.launch {
            try { repository.delete(entry) }
            catch (e: Exception) { Timber.e(e, "[MentalHealth] Failed to delete entry %s", entry.uuid) }
        }
    }
}
