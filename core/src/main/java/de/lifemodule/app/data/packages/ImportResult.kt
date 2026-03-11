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

package de.lifemodule.app.data.packages

/**
 * Result of a package import operation.
 *
 * @property packageId  The UUID from the package manifest (empty on extraction failure).
 * @property inserted   Number of entities successfully inserted into the local DB.
 * @property skipped    Number of entities skipped because their UUID already existed.
 * @property failed     Number of entities that caused a parse or insertion error.
 * @property errors     Human-readable error messages for diagnostics.
 */
data class ImportResult(
    val packageId: String,
    val inserted: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<String>
) {
    /** True if the import ran without extraction/manifest errors. */
    val isSuccess: Boolean
        get() = packageId.isNotBlank()

    /** Total number of entities processed (inserted + skipped + failed). */
    val totalProcessed: Int
        get() = inserted + skipped + failed
}
