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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.ImportSource

/**
 * A single journey record in the GoBD-compliant logbook.
 *
 * ### GoBD Compliance (§14 + EU Accounting Directives)
 *
 * - Records are **write-once**. No `@Update` or `@Delete` DAO methods exist.
 * - Corrections create a new row with [correctionOfUuid] referencing the original.
 * - Each record stores a [previousHash] linking it to the prior record (SHA-256 chain).
 * - The chain root has `previousHash = "GENESIS"`.
 * - [entryHash] = SHA-256(previousHash + all fields) - tamper-evident.
 *
 * @see de.lifemodule.app.util.HashChainUtil
 */
@Entity(
    tableName = "logbook_entries",
    indices = [
        Index("uuid"),
        Index("vehicle_id"),
        Index("journey_date"),
        Index("correction_of_uuid")
    ]
)
data class LogbookEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    override val uuid: String = BaseEntity.generateUuid(),

    @ColumnInfo(name = "created_at")
    override val createdAt: Long = 0L,

    @ColumnInfo(name = "updated_at")
    override val updatedAt: Long = 0L,

    @ColumnInfo(name = "import_source")
    override val importSource: ImportSource = ImportSource.USER,

    @ColumnInfo(name = "imported_from_package_id")
    override val importedFromPackageId: String? = null,

    /** Epoch-millis of the journey date (from TimeProvider). */
    @ColumnInfo(name = "journey_date")
    val journeyDate: Long,

    /** Starting location (address or description). */
    @ColumnInfo(name = "start_location")
    val startLocation: String,

    /** Destination (address or description). */
    @ColumnInfo(name = "end_location")
    val endLocation: String,

    /** Distance driven in kilometres. */
    @ColumnInfo(name = "distance_km")
    val distanceKm: Double,

    /** Classification of the trip. */
    @ColumnInfo(name = "purpose_code")
    val purposeCode: JourneyPurpose,

    /** UUID reference to [VehicleEntity]. */
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,

    /** Free-text notes (optional). */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

    /** Once true, the entry is sealed and may not be altered (only corrected). */
    @ColumnInfo(name = "is_finalized")
    val isFinalized: Boolean = false,

    /** If this row corrects a prior entry, stores that entry's UUID. */
    @ColumnInfo(name = "correction_of_uuid")
    val correctionOfUuid: String? = null,

    /** SHA-256(previousHash + all fields) - tamper-evident seal. */
    @ColumnInfo(name = "entry_hash")
    val entryHash: String,

    /** SHA-256 of the previous entry's hash, or `"GENESIS"` for the first record. */
    @ColumnInfo(name = "previous_hash")
    val previousHash: String,

) : BaseEntity(uuid, createdAt, updatedAt, importSource, importedFromPackageId)
