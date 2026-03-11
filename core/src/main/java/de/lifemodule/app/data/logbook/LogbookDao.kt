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

package de.lifemodule.app.data.logbook

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the GoBD-compliant Logbook.
 *
 * ### GoBD Rule: **Only `@Insert` and `@Query("SELECT ...")` are permitted.**
 *
 * - No `@Update`, no `@Delete` - records are write-once.
 * - Corrections are new rows with `correction_of_uuid` referencing the original.
 * - The hash chain (`entry_hash` / `previous_hash`) ensures tamper evidence.
 */
@Dao
interface LogbookDao {

    // ── Vehicle CRUD ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Query("SELECT * FROM logbook_vehicles ORDER BY name ASC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM logbook_vehicles WHERE uuid = :uuid")
    suspend fun getVehicleByUuid(uuid: String): VehicleEntity?

    // ── Logbook entries (write-once) ───────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: LogbookEntryEntity): Long

    /** Ignore-variant for import pipeline (UUID collision -> skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryIgnore(entry: LogbookEntryEntity): Long

    @Query("SELECT * FROM logbook_entries ORDER BY journey_date DESC, created_at DESC")
    fun getAllEntries(): Flow<List<LogbookEntryEntity>>

    @Query("SELECT * FROM logbook_entries WHERE vehicle_id = :vehicleId ORDER BY journey_date DESC")
    fun getEntriesForVehicle(vehicleId: String): Flow<List<LogbookEntryEntity>>

    @Query("SELECT * FROM logbook_entries WHERE journey_date BETWEEN :startMillis AND :endMillis ORDER BY journey_date ASC")
    fun getEntriesInRange(startMillis: Long, endMillis: Long): Flow<List<LogbookEntryEntity>>

    @Query("SELECT * FROM logbook_entries WHERE uuid = :uuid")
    suspend fun getEntryByUuid(uuid: String): LogbookEntryEntity?

    /** Get the most recent entry (needed for hash chain - previous_hash lookup). */
    @Query("SELECT * FROM logbook_entries ORDER BY created_at DESC LIMIT 1")
    suspend fun getLastEntry(): LogbookEntryEntity?

    /** All corrections for a specific original entry. */
    @Query("SELECT * FROM logbook_entries WHERE correction_of_uuid = :originalUuid ORDER BY created_at ASC")
    fun getCorrectionsFor(originalUuid: String): Flow<List<LogbookEntryEntity>>

    /** Summary: total km by purpose for a date range (analytics). */
    @Query("""
        SELECT purpose_code, SUM(distance_km) as total_km
        FROM logbook_entries
        WHERE journey_date BETWEEN :startMillis AND :endMillis
          AND correction_of_uuid IS NULL
        GROUP BY purpose_code
    """)
    suspend fun getKmByPurpose(startMillis: Long, endMillis: Long): List<KmByPurposeSummary>
}

/**
 * Read-only projection for [LogbookDao.getKmByPurpose].
 */
data class KmByPurposeSummary(
    val purpose_code: String,
    val total_km: Double
)
