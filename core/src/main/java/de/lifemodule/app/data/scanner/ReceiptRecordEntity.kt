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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.ImportSource

/**
 * A scanned receipt or invoice record.
 *
 * ### GoBD Compliance
 *
 * - After the configurable review window (default: 72 hours), the record is **sealed**.
 *   [finalizationHash] is computed and stored. Form fields become read-only.
 * - Corrections follow the same pattern as Logbook: a new row with [correctionOfUuid].
 * - [imageSha256] hashes the original image for tamper detection.
 *
 * ### Workflow
 * CameraX capture -> write JPEG to app-internal storage -> compute [imageSha256] ->
 * ML Kit text recognition (on-device, offline) -> pre-fill fields -> user confirms ->
 * record finalised.
 */
@Entity(
    tableName = "receipt_records",
    indices = [
        Index("uuid"),
        Index("receipt_date"),
        Index("category"),
        Index("correction_of_uuid")
    ]
)
data class ReceiptRecordEntity(
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

    /** Epoch-millis when the image was captured. */
    @ColumnInfo(name = "captured_at")
    val capturedAt: Long,

    /** Vendor / shop name (from OCR or manual entry). */
    @ColumnInfo(name = "vendor")
    val vendor: String,

    /** Date on the receipt (epoch-millis). */
    @ColumnInfo(name = "receipt_date")
    val receiptDate: Long,

    /** Total amount in [currency]. */
    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,

    /** VAT amount, if detectable. */
    @ColumnInfo(name = "vat_amount")
    val vatAmount: Double? = null,

    /** ISO 4217 currency code (e.g. "EUR"), from Locale.getDefault(). */
    @ColumnInfo(name = "currency")
    val currency: String,

    /** Expense category. */
    @ColumnInfo(name = "category")
    val category: ReceiptCategory,

    /** Free-text notes. */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

    /** Internal storage path to the captured image (never exported raw). */
    @ColumnInfo(name = "image_path")
    val imagePath: String,

    /** SHA-256 hash of the original image for tamper detection. */
    @ColumnInfo(name = "image_sha256")
    val imageSha256: String,

    /** Whether the record has been sealed (after review window). */
    @ColumnInfo(name = "is_finalized")
    val isFinalized: Boolean = false,

    /** SHA-256(all fields + imageSha256) computed at finalization. Null until sealed. */
    @ColumnInfo(name = "finalization_hash")
    val finalizationHash: String? = null,

    /** If this row corrects a prior record, stores that record's UUID. */
    @ColumnInfo(name = "correction_of_uuid")
    val correctionOfUuid: String? = null,

) : BaseEntity(uuid, createdAt, updatedAt, importSource, importedFromPackageId)
