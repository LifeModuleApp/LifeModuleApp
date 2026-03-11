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

import org.json.JSONObject
import timber.log.Timber

/**
 * A single schema migration step that transforms package JSON from one version
 * to the next.  Implementations must be deterministic and side-effect-free.
 */
interface ImportMigration {
    /** The schema version this migration upgrades **from**. */
    val fromVersion: Int

    /**
     * Transforms [data] written at [fromVersion] so it conforms to
     * `fromVersion + 1`.
     */
    fun migrate(data: JSONObject): JSONObject
}

/**
 * Version-aware transform pipeline for imported package data.
 *
 * When a ZIP archive is opened the [PackageManifest.schemaVersion] is compared
 * to [CURRENT_PACKAGE_SCHEMA_VERSION].  If the manifest version is older, each
 * registered [ImportMigration] is applied sequentially until the data matches
 * the current schema.
 *
 * **Forward-compatibility:** If the manifest version is *higher* than the app
 * knows about, the data is accepted as-is.  Unknown JSON keys are silently
 * ignored by kotlinx-serialization's default decoder settings.
 *
 * ### Adding a new migration
 * 1. Bump [CURRENT_PACKAGE_SCHEMA_VERSION].
 * 2. Create a class implementing [ImportMigration] with `fromVersion = oldVersion`.
 * 3. Append the instance to [registry].
 */
object ImportMigrationManager {

    /**
     * Current package schema version.
     *
     * Increment every time the export JSON structure changes (new field,
     * renamed key, etc.).  This is the *package* format version - it is
     * independent of the Room database version number.
     */
    const val CURRENT_PACKAGE_SCHEMA_VERSION: Int = 1

    // ── Migration registry ──────────────────────────────────────────────
    // Add migration instances here when bumping CURRENT_PACKAGE_SCHEMA_VERSION.
    // Example:
    //   object Migration_1_to_2 : ImportMigration {
    //       override val fromVersion = 1
    //       override fun migrate(data: JSONObject): JSONObject { ... }
    //   }
    private val registry: List<ImportMigration> = listOf(
        // intentionally empty - schema version 1 is the initial format
    )

    /**
     * Migrates [data] from [fromVersion] up to [CURRENT_PACKAGE_SCHEMA_VERSION].
     *
     * @param data         The raw `entries` JSON object from the ZIP package.
     * @param fromVersion  The `schemaVersion` declared in the package manifest.
     * @return             The transformed JSON object ready for deserialization.
     */
    fun migrate(data: JSONObject, fromVersion: Int): JSONObject {
        if (fromVersion >= CURRENT_PACKAGE_SCHEMA_VERSION) {
            // Package is current or from a newer app version - accept as-is.
            return data
        }

        Timber.i(
            "[Import] Migrating package data from schema v%d -> v%d",
            fromVersion, CURRENT_PACKAGE_SCHEMA_VERSION
        )

        var result = data
        for (migration in registry) {
            if (migration.fromVersion in fromVersion until CURRENT_PACKAGE_SCHEMA_VERSION) {
                Timber.d("[Import] Applying migration v%d -> v%d", migration.fromVersion, migration.fromVersion + 1)
                result = migration.migrate(result)
            }
        }
        return result
    }
}
