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

package de.lifemodule.app.data.supplements

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {

    // ── Supplements ──
    @Query("SELECT * FROM supplements WHERE isActive = 1 ORDER BY timeOfDay, name")
    fun getActiveSupplements(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements ORDER BY isActive DESC, name")
    fun getAllSupplements(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE uuid = :uuid")
    suspend fun getSupplementByUuid(uuid: String): SupplementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplement(supplement: SupplementEntity): Long

    /** Import-safe: returns -1 if UUID already exists (never overwrites). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSupplementIgnore(supplement: SupplementEntity): Long

    /** Import-safe: returns -1 if UUID already exists (never overwrites). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIngredientIgnore(ingredient: SupplementIngredientEntity): Long

    @Update
    suspend fun updateSupplement(supplement: SupplementEntity)

    @Delete
    suspend fun deleteSupplement(supplement: SupplementEntity)

    // ── Supplements with Ingredients ──
    @Transaction
    @Query("SELECT * FROM supplements WHERE uuid = :uuid")
    fun getSupplementWithIngredients(uuid: String): Flow<SupplementWithIngredients?>

    @Transaction
    @Query("SELECT * FROM supplements WHERE isActive = 1 ORDER BY timeOfDay, name")
    fun getActiveSupplementsWithIngredients(): Flow<List<SupplementWithIngredients>>

    // ── Ingredients ──
    @Query("SELECT * FROM supplement_ingredients ORDER BY supplementId, name")
    fun getAllIngredients(): Flow<List<SupplementIngredientEntity>>

    @Query("SELECT * FROM supplement_ingredients WHERE supplementId = :supplementUuid ORDER BY name")
    fun getIngredientsForSupplement(supplementUuid: String): Flow<List<SupplementIngredientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: SupplementIngredientEntity): Long

    @Update
    suspend fun updateIngredient(ingredient: SupplementIngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: SupplementIngredientEntity)

    @Query("DELETE FROM supplement_ingredients WHERE supplementId = :supplementUuid")
    suspend fun deleteAllIngredientsForSupplement(supplementUuid: String)

    // ── Logs ──
    @Transaction
    @Query("SELECT * FROM supplement_logs WHERE date = :date ORDER BY supplementId")
    fun getLogsWithSupplementForDate(date: String): Flow<List<LogWithSupplement>>

    @Query("SELECT * FROM supplement_logs WHERE supplementId = :supplementUuid AND date = :date LIMIT 1")
    suspend fun getLogForSupplementAndDate(supplementUuid: String, date: String): SupplementLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: SupplementLogEntity): Long

    @Update
    suspend fun updateLog(log: SupplementLogEntity)

    @Query("DELETE FROM supplement_logs WHERE supplementId = :supplementUuid AND date = :date")
    suspend fun deleteLog(supplementUuid: String, date: String)

    // ── Analytics ──
    @Query("SELECT DISTINCT date FROM supplement_logs WHERE taken = 1 ORDER BY date DESC")
    fun getAllTakenDates(): Flow<List<String>>

    @Query("SELECT * FROM supplement_logs WHERE taken = 1 ORDER BY date DESC")
    fun getAllTakenLogs(): Flow<List<SupplementLogEntity>>
}
