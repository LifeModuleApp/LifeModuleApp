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

package de.lifemodule.app.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.weight.WeightEntryEntity
import de.lifemodule.app.data.weight.WeightRepository
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val allEntries = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestEntry = repository.getLatestEntry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addEntry(entry: WeightEntryEntity) {
        viewModelScope.launch {
            try { repository.insert(entry) }
            catch (e: Exception) { Timber.e(e, "[Weight] Failed to add weight entry") }
        }
    }

    fun today(): LocalDate = timeProvider.today()

    fun deleteEntry(entry: WeightEntryEntity) {
        viewModelScope.launch {
            try { repository.delete(entry) }
            catch (e: Exception) { Timber.e(e, "[Weight] Failed to delete weight entry %s", entry.uuid) }
        }
    }
}
