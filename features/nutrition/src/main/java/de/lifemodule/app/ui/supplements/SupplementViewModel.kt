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

package de.lifemodule.app.ui.supplements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.supplements.LogWithSupplement
import de.lifemodule.app.data.supplements.SupplementEntity
import de.lifemodule.app.data.supplements.SupplementIngredientEntity
import de.lifemodule.app.data.supplements.SupplementLogEntity
import de.lifemodule.app.data.supplements.SupplementRepository
import de.lifemodule.app.data.supplements.SupplementWithIngredients
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class DailyIngredientTotal(
    val name: String,
    val totalAmount: Double,
    val unit: String,
    val maxRvsPct: Double?
)

@HiltViewModel
class SupplementViewModel @Inject constructor(
    private val repository: SupplementRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(timeProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val allSupplements: StateFlow<List<SupplementEntity>> = repository.getAllSupplements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs: StateFlow<List<LogWithSupplement>> = _selectedDate
        .flatMapLatest { date ->
            repository.getLogsWithSupplementForDate(date.format(dateFormatter))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSupplements: StateFlow<List<SupplementEntity>> = repository.getActiveSupplements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSupplementsWithIngredients: StateFlow<List<SupplementWithIngredients>> =
        repository.getActiveSupplementsWithIngredients()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyIngredientTotals: StateFlow<List<DailyIngredientTotal>> =
        repository.getActiveSupplementsWithIngredients()
            .map { supplementsWithIngredients ->
                val grouped = mutableMapOf<String, MutableList<SupplementIngredientEntity>>()
                for (swi in supplementsWithIngredients) {
                    for (ing in swi.ingredients) {
                        val key = "${ing.name}|${ing.unit}"
                        grouped.getOrPut(key) { mutableListOf() }.add(ing)
                    }
                }
                grouped.map { (key, ingredients) ->
                    val parts = key.split("|")
                    DailyIngredientTotal(
                        name = parts[0],
                        totalAmount = ingredients.sumOf { it.amount },
                        unit = parts[1],
                        maxRvsPct = ingredients.mapNotNull { it.rvsPct }.maxOrNull()
                    )
                }.sortedBy { it.name }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Create today's logs respecting durationDays and frequency.
     * Uses the already-collected StateFlow value to avoid creating a second DB subscription,
     * which was the root cause of duplicate log entries (TOCTOU race condition).
     * Duplicates are additionally prevented at DB level via a unique index on (supplementId, date)
     * combined with OnConflictStrategy.IGNORE.
     */
    fun ensureTodayLogs() {
        viewModelScope.launch {
            try {
                val today = _selectedDate.value
                val todayStr = today.format(dateFormatter)
                // Read current value from the already-active StateFlow - no new subscription
                val supplements = activeSupplements.value
                for (supp in supplements) {
                    // Check duration limit - auto-deactivate expired supplements
                    val durationDays = supp.durationDays
                    if (durationDays != null && supp.startDate != null) {
                        val start = runCatching { LocalDate.parse(supp.startDate, dateFormatter) }.getOrNull()
                        if (start != null) {
                            val daysSinceStart = ChronoUnit.DAYS.between(start, today)
                            if (daysSinceStart >= durationDays) {
                                repository.updateSupplement(supp.copy(isActive = false))
                                continue
                            }
                        }
                    }

                    // Check frequency for weekly supplements
                    if (supp.frequency == "weekly") {
                        val start = supp.startDate?.let {
                            runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull()
                        } ?: timeProvider.today()
                        val daysSince = ChronoUnit.DAYS.between(start, today)
                        if (daysSince % 7 != 0L) continue
                    }

                    // IGNORE strategy + unique DB index handles concurrency - no manual existence check needed
                    repository.insertLog(
                        SupplementLogEntity(
                            supplementId = supp.uuid,
                            date = todayStr,
                            taken = false
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "[Supplements] Failed to ensure today's logs")
            }
        }
    }

    fun toggleTaken(log: SupplementLogEntity) {
        viewModelScope.launch {
            try {
                val updated = log.copy(
                    taken = !log.taken,
                    takenAtMillis = if (!log.taken) timeProvider.currentTimeMillis() else null
                )
                repository.updateLog(updated)
            } catch (e: Exception) {
                Timber.e(e, "[Supplements] Failed to toggle taken for log %s", log.uuid)
            }
        }
    }

    fun addSupplement(
        name: String,
        dosage: String,
        frequency: String,
        timesPerDay: Int,
        timeOfDay: String,
        durationDays: Int?,
        notes: String?,
        imagePath: String?
    ) {
        viewModelScope.launch {
            try {
                repository.insertSupplement(
                    SupplementEntity(
                        name = name,
                        dosage = dosage,
                        frequency = frequency,
                        timesPerDay = timesPerDay,
                        timeOfDay = timeOfDay,
                        durationDays = durationDays,
                        startDate = timeProvider.today().format(dateFormatter),
                        notes = notes?.ifBlank { null },
                        imagePath = imagePath
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Supplements] Failed to add supplement '%s'", name)
            }
        }
    }

    fun archiveSupplement(supplement: SupplementEntity) {
        viewModelScope.launch {
            try { repository.updateSupplement(supplement.copy(isActive = false)) }
            catch (e: Exception) { Timber.e(e, "[Supplements] Failed to archive '%s'", supplement.name) }
        }
    }

    fun deleteSupplement(supplement: SupplementEntity) {
        viewModelScope.launch {
            try { repository.deleteSupplement(supplement) }
            catch (e: Exception) { Timber.e(e, "[Supplements] Failed to delete '%s'", supplement.name) }
        }
    }

    fun toggleActive(supplement: SupplementEntity) {
        viewModelScope.launch {
            try { repository.updateSupplement(supplement.copy(isActive = !supplement.isActive)) }
            catch (e: Exception) { Timber.e(e, "[Supplements] Failed to toggle active for '%s'", supplement.name) }
        }
    }

    private val _ingredientFlowCache = mutableMapOf<String, StateFlow<List<SupplementIngredientEntity>>>()

    fun getIngredientsForSupplement(supplementId: String): StateFlow<List<SupplementIngredientEntity>> {
        return _ingredientFlowCache.getOrPut(supplementId) {
            repository.getIngredientsForSupplement(supplementId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun addIngredient(
        supplementId: String,
        name: String,
        amount: Double,
        unit: String,
        rvsPct: Double?
    ) {
        viewModelScope.launch {
            try {
                repository.insertIngredient(
                    SupplementIngredientEntity(
                        supplementId = supplementId,
                        name = name,
                        amount = amount,
                        unit = unit,
                        rvsPct = rvsPct
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Supplements] Failed to add ingredient '%s'", name)
            }
        }
    }

    fun deleteIngredient(ingredient: SupplementIngredientEntity) {
        viewModelScope.launch {
            try { repository.deleteIngredient(ingredient) }
            catch (e: Exception) { Timber.e(e, "[Supplements] Failed to delete ingredient '%s'", ingredient.name) }
        }
    }
}
