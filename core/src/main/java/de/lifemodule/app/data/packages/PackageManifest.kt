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

import kotlinx.serialization.Serializable

/**
 * Manifest metadata for every export/import ZIP package.
 *
 * Serialised as `manifest.json` at the root of the archive.
 * All fields are validated by [ManifestValidator] before any entity data
 * is touched during import.
 *
 * @property packageId         Unique UUID v4 for this package.
 * @property schemaVersion     Package schema version at creation time.
 *                             Used by [ImportMigrationManager] to apply transforms.
 * @property contentType       Discriminator - tells the import pipeline which entity
 *                             types to expect inside [entries].
 * @property authorCommunityId Community-Hub user ID of the package author, or `null`
 *                             for locally exported packages.
 * @property createdAt         ISO-8601 timestamp of export (e.g. "2026-03-04T10:00:00Z").
 * @property appVersionCreatedWith Semantic version of the LifeModule build that created
 *                                 this package (e.g. "1.2.0").
 * @property entryCount        Total number of entities across all entry lists.
 */
@Serializable
data class PackageManifest(
    val packageId: String,
    val schemaVersion: Int,
    val contentType: PackageContentType,
    val authorCommunityId: String? = null,
    val createdAt: String,
    val appVersionCreatedWith: String,
    val entryCount: Int
)
