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

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.packages.dto.toExportDto
import de.lifemodule.app.util.time.TimeProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Produces a ZIP archive in the app cache directory containing:
 *   `manifest.json` - package metadata
 *   `entries.json`  - exported entity data (personal fields stripped)
 *
 * The ZIP is returned as a [Uri] suitable for a Share intent.
 *
 * Personal identifiers (e.g. imagePath, timestamps, importedFromPackageId)
 * are stripped before serialisation - see the `toExportDto()` extension
 * functions on each entity.
 */
class ExportPackageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * Builds a shareable ZIP package.
     *
     * @param contentType  What kind of data this package represents.
     * @param selection    The entity lists to include.
     * @return A content [Uri] (via FileProvider) pointing to the ZIP in the cache dir.
     */
    suspend fun invoke(
        contentType: PackageContentType,
        selection: ExportSelection
    ): Uri {
        val packageId = BaseEntity.generateUuid()
        val now = timeProvider.now()
        val isoTimestamp = now.atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        // ── 1. Build manifest ────────────────────────────────────────────
        val manifest = PackageManifest(
            packageId = packageId,
            schemaVersion = ImportMigrationManager.CURRENT_PACKAGE_SCHEMA_VERSION,
            contentType = contentType,
            authorCommunityId = null,  // local export - no community profile
            createdAt = isoTimestamp,
            appVersionCreatedWith = APP_VERSION,
            entryCount = selection.totalEntryCount
        )

        // ── 2. Convert entities to export DTOs (strips personal fields) ──
        val entries = PackageEntries(
            exerciseDefinitions = selection.exerciseDefinitions?.map { it.toExportDto() },
            workoutTemplates = selection.workoutTemplates?.map { it.toExportDto() },
            templateExercises = selection.templateExercises?.map { it.toExportDto() },
            foodItems = selection.foodItems?.map { it.toExportDto() },
            supplements = selection.supplements?.map { it.toExportDto() },
            supplementIngredients = selection.supplementIngredients?.map { it.toExportDto() },
            habits = selection.habits?.map { it.toExportDto() },
            recipes = selection.recipes?.map { it.toExportDto() },
            recipeIngredients = selection.recipeIngredients?.map { it.toExportDto() }
        )

        // ── 3. Write ZIP ─────────────────────────────────────────────────
        val cacheDir = File(context.cacheDir, "packages").apply { mkdirs() }
        val zipFile = File(cacheDir, "lifemodule_${contentType.name.lowercase()}_$packageId.zip")

        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            // manifest.json
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // entries.json
            zip.putNextEntry(ZipEntry("entries.json"))
            zip.write(json.encodeToString(entries).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        Timber.i(
            "[Export] Created %s package %s (%d entries, %d bytes)",
            contentType.name, packageId, selection.totalEntryCount, zipFile.length()
        )

        // ── 4. Return FileProvider URI ───────────────────────────────────
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
    }

    companion object {
        /**
         * Current app version embedded in every manifest.
         * Update this when the app version changes.
         */
        internal const val APP_VERSION = "1.2.0"
    }
}
