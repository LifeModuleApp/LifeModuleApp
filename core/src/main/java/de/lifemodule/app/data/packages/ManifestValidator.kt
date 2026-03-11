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

import timber.log.Timber
import java.util.Locale

/**
 * Validates a [PackageManifest] before the import pipeline processes any entity data.
 *
 * Validation is deliberately strict: a malformed or tampered manifest causes
 * the entire import to be rejected rather than silently producing corrupt data.
 */
object ManifestValidator {

    /** Result of a validation attempt. */
    sealed interface Result {
        data object Valid : Result
        data class Invalid(val reason: String) : Result
    }

    /**
     * Validates the given [manifest].
     *
     * Checks:
     * 1. `packageId` is a non-blank string (UUID format is checked but not enforced
     *    to allow future ID schemes).
     * 2. `schemaVersion` is ≥ 1.
     * 3. `contentType` is a known enum value (guaranteed by kotlinx-serialization;
     *    unknown values throw during deserialization).
     * 4. `createdAt` is a non-blank ISO-8601 string.
     * 5. `appVersionCreatedWith` is non-blank.
     * 6. `entryCount` is ≥ 0.
     */
    fun validate(manifest: PackageManifest): Result {
        if (manifest.packageId.isBlank()) {
            return reject("packageId is blank")
        }
        if (manifest.schemaVersion < 1) {
            return reject("schemaVersion must be ≥ 1, was ${manifest.schemaVersion}")
        }
        if (manifest.createdAt.isBlank()) {
            return reject("createdAt is blank")
        }
        if (manifest.appVersionCreatedWith.isBlank()) {
            return reject("appVersionCreatedWith is blank")
        }
        if (manifest.entryCount < 0) {
            return reject("entryCount must be ≥ 0, was ${manifest.entryCount}")
        }

        // ── Version gating: reject packages from incompatible app versions ──
        val packageSemver = parseSemver(manifest.appVersionCreatedWith)
        val currentSemver = parseSemver(ExportPackageUseCase.APP_VERSION)
        if (packageSemver != null && currentSemver != null) {
            if (packageSemver.first > currentSemver.first) {
                return reject(
                    "Package created with newer major version " +
                    "(${manifest.appVersionCreatedWith} > ${ExportPackageUseCase.APP_VERSION})"
                )
            }
        }

        return Result.Valid
    }

    /**
     * Parse a semver string into (major, minor, patch).
     * Accepts "1.2.0", "1.2", "1". Returns null on parse failure.
     */
    private fun parseSemver(version: String): Triple<Int, Int, Int>? {
        val parts = version.trim().split(".")
        return try {
            Triple(
                parts.getOrNull(0)?.toInt() ?: return null,
                parts.getOrNull(1)?.toInt() ?: 0,
                parts.getOrNull(2)?.toInt() ?: 0
            )
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun reject(reason: String): Result.Invalid {
        Timber.w("[ManifestValidator] Rejected: %s", reason)
        return Result.Invalid(reason)
    }
}
