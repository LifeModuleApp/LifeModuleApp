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

package de.lifemodule.app.data.nutrition

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NutritionRepository @Inject constructor(
    private val foodDao: FoodDao
) {
    fun getAllFoodItems(): Flow<List<FoodItemEntity>> = foodDao.getAllFoodItems()

    fun getEntriesWithFoodForDate(date: String): Flow<List<DailyEntryWithFood>> =
        foodDao.getEntriesWithFoodForDate(date)

    suspend fun getFoodItemByUuid(uuid: String): FoodItemEntity? = foodDao.getFoodItemByUuid(uuid)

    suspend fun getFoodItemByBarcode(barcode: String): FoodItemEntity? =
        foodDao.getFoodItemByBarcode(barcode)

    suspend fun insertFoodItem(item: FoodItemEntity): Long = foodDao.insertFoodItem(item)

    suspend fun updateFoodItem(item: FoodItemEntity) = foodDao.updateFoodItem(item)

    suspend fun deleteFoodItem(item: FoodItemEntity) = foodDao.deleteFoodItem(item)

    suspend fun insertDailyEntry(entry: DailyFoodEntryEntity): Long =
        foodDao.insertDailyEntry(entry)

    suspend fun deleteDailyEntry(entry: DailyFoodEntryEntity) = foodDao.deleteDailyEntry(entry)

    // Analytics
    fun getEntriesForDateRange(startDate: String, endDate: String) =
        foodDao.getEntriesForDateRange(startDate, endDate)
}
