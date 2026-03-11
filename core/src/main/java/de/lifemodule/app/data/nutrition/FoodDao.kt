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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    // ── Food Items ──
    @Query("SELECT * FROM food_items WHERE isActive = 1 ORDER BY name ASC")
    fun getAllFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE uuid = :uuid")
    suspend fun getFoodItemByUuid(uuid: String): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodItemByBarcode(barcode: String): FoodItemEntity?

    @Insert
    suspend fun insertFoodItem(item: FoodItemEntity): Long

    /** Import-safe: returns -1 if UUID already exists (never overwrites). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoodItemIgnore(item: FoodItemEntity): Long

    @Update
    suspend fun updateFoodItem(item: FoodItemEntity)

    @Delete
    suspend fun deleteFoodItem(item: FoodItemEntity)

    // ── Daily Entries ──
    @Transaction
    @Query("SELECT * FROM daily_food_entries WHERE date = :date ORDER BY uuid DESC")
    fun getEntriesWithFoodForDate(date: String): Flow<List<DailyEntryWithFood>>

    @Insert
    suspend fun insertDailyEntry(entry: DailyFoodEntryEntity): Long

    @Delete
    suspend fun deleteDailyEntry(entry: DailyFoodEntryEntity)

    // ── Analytics ──
    @Transaction
    @Query("SELECT * FROM daily_food_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getEntriesForDateRange(startDate: String, endDate: String): Flow<List<DailyEntryWithFood>>
}
