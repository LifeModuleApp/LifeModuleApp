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

package de.lifemodule.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.nutrition.DailyEntryWithFood
import de.lifemodule.app.data.nutrition.DailyFoodEntryEntity
import de.lifemodule.app.data.nutrition.FoodItemEntity
import de.lifemodule.app.data.nutrition.NutritionRepository
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repository: NutritionRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(timeProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyEntries: StateFlow<List<DailyEntryWithFood>> = _selectedDate
        .flatMapLatest { date ->
            repository.getEntriesWithFoodForDate(date.format(dateFormatter))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoodItems: StateFlow<List<FoodItemEntity>> = repository.getAllFoodItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFoodEntry(foodItemId: String, grams: Double) {
        viewModelScope.launch {
            try {
                repository.insertDailyEntry(
                    DailyFoodEntryEntity(
                        foodItemId = foodItemId,
                        date = _selectedDate.value.format(dateFormatter),
                        gramsConsumed = grams
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Nutrition] Failed to add food entry for item %s", foodItemId)
            }
        }
    }

    fun deleteFoodEntry(entry: DailyFoodEntryEntity) {
        viewModelScope.launch {
            try { repository.deleteDailyEntry(entry) }
            catch (e: Exception) { Timber.e(e, "[Nutrition] Failed to delete food entry %s", entry.uuid) }
        }
    }

    fun addFoodItem(
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        sugar: Double,
        barcode: String?,
        imagePath: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.insertFoodItem(
                    FoodItemEntity(
                        name = name,
                        kcalPer100g = kcal,
                        proteinPer100g = protein,
                        carbsPer100g = carbs,
                        fatPer100g = fat,
                        sugarPer100g = sugar,
                        barcode = barcode?.ifBlank { null },
                        imagePath = imagePath
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Nutrition] Failed to add food item '%s'", name)
            }
        }
    }

    fun archiveFoodItem(item: FoodItemEntity) {
        viewModelScope.launch {
            try { repository.updateFoodItem(item.copy(isActive = false)) }
            catch (e: Exception) { Timber.e(e, "[Nutrition] Failed to archive food item '%s'", item.name) }
        }
    }

    fun deleteFoodItem(item: FoodItemEntity) {
        viewModelScope.launch {
            try { repository.deleteFoodItem(item) }
            catch (e: Exception) { Timber.e(e, "[Nutrition] Failed to delete food item '%s'", item.name) }
        }
    }

    /** Lookup a food by barcode - returns null if not found */
    suspend fun findByBarcode(barcode: String): FoodItemEntity? {
        return repository.getFoodItemByBarcode(barcode)
    }
}
