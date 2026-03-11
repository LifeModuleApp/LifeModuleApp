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

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SupplementRepository @Inject constructor(
    private val supplementDao: SupplementDao
) {
    // ── Supplements ──
    fun getActiveSupplements(): Flow<List<SupplementEntity>> =
        supplementDao.getActiveSupplements()

    fun getAllSupplements(): Flow<List<SupplementEntity>> =
        supplementDao.getAllSupplements()

    suspend fun getSupplementByUuid(uuid: String): SupplementEntity? =
        supplementDao.getSupplementByUuid(uuid)

    suspend fun insertSupplement(supplement: SupplementEntity): Long =
        supplementDao.insertSupplement(supplement)

    suspend fun updateSupplement(supplement: SupplementEntity) =
        supplementDao.updateSupplement(supplement)

    suspend fun deleteSupplement(supplement: SupplementEntity) =
        supplementDao.deleteSupplement(supplement)

    // ── Supplements with Ingredients ──
    fun getSupplementWithIngredients(uuid: String): Flow<SupplementWithIngredients?> =
        supplementDao.getSupplementWithIngredients(uuid)

    fun getActiveSupplementsWithIngredients(): Flow<List<SupplementWithIngredients>> =
        supplementDao.getActiveSupplementsWithIngredients()

    // ── Ingredients ──
    fun getIngredientsForSupplement(supplementUuid: String): Flow<List<SupplementIngredientEntity>> =
        supplementDao.getIngredientsForSupplement(supplementUuid)

    suspend fun insertIngredient(ingredient: SupplementIngredientEntity): Long =
        supplementDao.insertIngredient(ingredient)

    suspend fun updateIngredient(ingredient: SupplementIngredientEntity) =
        supplementDao.updateIngredient(ingredient)

    suspend fun deleteIngredient(ingredient: SupplementIngredientEntity) =
        supplementDao.deleteIngredient(ingredient)

    // ── Logs ──
    fun getLogsWithSupplementForDate(date: String): Flow<List<LogWithSupplement>> =
        supplementDao.getLogsWithSupplementForDate(date)

    suspend fun getLogForSupplementAndDate(supplementUuid: String, date: String): SupplementLogEntity? =
        supplementDao.getLogForSupplementAndDate(supplementUuid, date)

    suspend fun insertLog(log: SupplementLogEntity): Long =
        supplementDao.insertLog(log)

    suspend fun updateLog(log: SupplementLogEntity) =
        supplementDao.updateLog(log)

    suspend fun deleteLog(supplementUuid: String, date: String) =
        supplementDao.deleteLog(supplementUuid, date)

    // ── Analytics ──
    fun getAllTakenDates() = supplementDao.getAllTakenDates()
    fun getAllTakenLogs() = supplementDao.getAllTakenLogs()
}
