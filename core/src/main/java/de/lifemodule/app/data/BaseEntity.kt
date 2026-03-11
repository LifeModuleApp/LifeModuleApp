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

package de.lifemodule.app.data

import androidx.room.ColumnInfo
import java.util.UUID

/**
 * Abstract base for every Room entity whose records can be exported, imported, or
 * shipped as prebuilt data.
 *
 * Merge rule: on UUID collision the existing record always wins.
 * USER > COMMUNITY_HUB > PREBUILT in terms of write priority.
 */
abstract class BaseEntity(

    /** Stable, globally-unique identifier. Never changes after creation. */
    @ColumnInfo(name = "uuid")
    open val uuid: String,

    /** Unix epoch millis - set once at creation, never updated. */
    @ColumnInfo(name = "created_at")
    open val createdAt: Long,

    /** Unix epoch millis - updated on every local mutation. Immutable for PREBUILT records. */
    @ColumnInfo(name = "updated_at")
    open val updatedAt: Long,

    /** Origin of this record. Drives merge-priority and read-only enforcement. */
    @ColumnInfo(name = "import_source")
    open val importSource: ImportSource,

    /** UUID of the Community Hub package this record was imported from, or null. */
    @ColumnInfo(name = "imported_from_package_id")
    open val importedFromPackageId: String?,
) {
    companion object {
        /** Generate a new random UUID v4 string. */
        fun generateUuid(): String = UUID.randomUUID().toString()
    }
}
