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

package de.lifemodule.app.data.scanner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the Receipt / Invoice Scanner module.
 *
 * Unlike the Logbook DAO, `@Update` is allowed **before** finalization
 * (the review window lets users correct OCR mistakes). After finalization,
 * only correction rows may be inserted.
 */
@Dao
interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: ReceiptRecordEntity): Long

    /** Ignore-variant for import pipeline (UUID collision -> skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecordIgnore(record: ReceiptRecordEntity): Long

    /**
     * Update allowed only for non-finalized records (UI enforces this constraint).
     * After finalization, a correction row must be created instead.
     */
    @Update
    suspend fun updateRecord(record: ReceiptRecordEntity)

    @Query("SELECT * FROM receipt_records ORDER BY receipt_date DESC, created_at DESC")
    fun getAllRecords(): Flow<List<ReceiptRecordEntity>>

    @Query("SELECT * FROM receipt_records WHERE receipt_date BETWEEN :startMillis AND :endMillis ORDER BY receipt_date ASC")
    fun getRecordsInRange(startMillis: Long, endMillis: Long): Flow<List<ReceiptRecordEntity>>

    @Query("SELECT * FROM receipt_records WHERE uuid = :uuid")
    suspend fun getRecordByUuid(uuid: String): ReceiptRecordEntity?

    @Query("SELECT * FROM receipt_records WHERE category = :category ORDER BY receipt_date DESC")
    fun getRecordsByCategory(category: String): Flow<List<ReceiptRecordEntity>>

    @Query("SELECT * FROM receipt_records WHERE is_finalized = 0 ORDER BY created_at ASC")
    fun getUnfinalizedRecords(): Flow<List<ReceiptRecordEntity>>

    /** All corrections for a specific original record. */
    @Query("SELECT * FROM receipt_records WHERE correction_of_uuid = :originalUuid ORDER BY created_at ASC")
    fun getCorrectionsFor(originalUuid: String): Flow<List<ReceiptRecordEntity>>

    /** Summary: total spending by category for a date range. */
    @Query("""
        SELECT category, SUM(total_amount) as total
        FROM receipt_records
        WHERE receipt_date BETWEEN :startMillis AND :endMillis
          AND correction_of_uuid IS NULL
        GROUP BY category
    """)
    suspend fun getSpendingByCategory(startMillis: Long, endMillis: Long): List<SpendingByCategorySummary>
}

/**
 * Read-only projection for [ReceiptDao.getSpendingByCategory].
 */
data class SpendingByCategorySummary(
    val category: String,
    val total: Double
)
