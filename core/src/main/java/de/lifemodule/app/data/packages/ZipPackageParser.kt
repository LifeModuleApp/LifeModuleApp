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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * Extracts a ZIP package from a content [Uri] into a temporary directory.
 *
 * After extraction the caller can read `manifest.json` and `entries.json`
 * from the returned [ParsedPackage]. The temp dir is cleaned up automatically
 * via [cleanup] or when the app's cache is cleared by the system.
 *
 * Security:
 * - Path traversal: entries containing ".." are rejected.
 * - ZIP bomb: total extracted size is capped at [MAX_EXTRACTED_SIZE_BYTES].
 * - Entry count: maximum [MAX_ENTRIES] files inside the ZIP.
 * - Only `.json` files at the root level are written.
 */
class ZipPackageParser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Result of a successful extraction.
     *
     * @property tempDir   The directory containing the extracted files.
     * @property manifest  The raw `manifest.json` content as a UTF-8 string.
     * @property entries   The raw `entries.json` content as a UTF-8 string.
     */
    data class ParsedPackage(
        val tempDir: File,
        val manifest: String,
        val entries: String
    )

    /**
     * Extraction error - all are non-recoverable.
     */
    sealed class ParseError(message: String) : Throwable(message) {
        class IoFailure(cause: String) : ParseError(cause)
        class PathTraversal(entry: String) : ParseError("Path traversal detected: $entry")
        class TooLarge(bytes: Long) : ParseError("Extracted size ($bytes bytes) exceeds limit")
        class TooManyEntries(count: Int) : ParseError("ZIP contains $count entries (max $MAX_ENTRIES)")
        class ManifestMissing : ParseError("manifest.json not found in ZIP")
        class EntriesMissing : ParseError("entries.json not found in ZIP")
    }

    /**
     * Extracts the package ZIP from [uri] into a temp directory.
     *
     * @return [Result.success] with [ParsedPackage] or [Result.failure] with a [ParseError].
     */
    fun extract(uri: Uri): Result<ParsedPackage> {
        val tempDir = File(context.cacheDir, "import_${System.nanoTime()}").apply { mkdirs() }

        return try {
            val contentResolver: ContentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(ParseError.IoFailure("Cannot open URI: $uri"))

            var totalSize = 0L
            var entryCount = 0

            ZipInputStream(inputStream.buffered()).use { zip ->
                var zipEntry = zip.nextEntry
                while (zipEntry != null) {
                    entryCount++
                    if (entryCount > MAX_ENTRIES) {
                        cleanup(tempDir)
                        return Result.failure(ParseError.TooManyEntries(entryCount))
                    }

                    val name = zipEntry.name

                    // ── Security: path traversal guard ───────────────────────
                    if (name.contains("..") || name.startsWith("/")) {
                        cleanup(tempDir)
                        return Result.failure(ParseError.PathTraversal(name))
                    }

                    // Only process root-level .json files
                    if (!name.contains("/") && name.endsWith(".json", ignoreCase = true)) {
                        val outFile = File(tempDir, name)
                        outFile.outputStream().buffered().use { out ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zip.read(buffer).also { len = it } > 0) {
                                totalSize += len
                                if (totalSize > MAX_EXTRACTED_SIZE_BYTES) {
                                    cleanup(tempDir)
                                    return Result.failure(ParseError.TooLarge(totalSize))
                                }
                                out.write(buffer, 0, len)
                            }
                        }
                    }

                    zip.closeEntry()
                    zipEntry = zip.nextEntry
                }
            }

            // ── Verify required files present ────────────────────────────
            val manifestFile = File(tempDir, "manifest.json")
            val entriesFile = File(tempDir, "entries.json")

            if (!manifestFile.exists()) {
                cleanup(tempDir)
                return Result.failure(ParseError.ManifestMissing())
            }
            if (!entriesFile.exists()) {
                cleanup(tempDir)
                return Result.failure(ParseError.EntriesMissing())
            }

            Timber.i(
                "[Import] Extracted package: %d files, %d bytes total",
                entryCount, totalSize
            )

            Result.success(
                ParsedPackage(
                    tempDir = tempDir,
                    manifest = manifestFile.readText(Charsets.UTF_8),
                    entries = entriesFile.readText(Charsets.UTF_8)
                )
            )

        } catch (e: Exception) {
            Timber.e(e, "[Import] ZIP extraction failed")
            cleanup(tempDir)
            Result.failure(ParseError.IoFailure(e.message ?: "Unknown extraction error"))
        }
    }

    /** Deletes the temp directory and all its contents. */
    fun cleanup(tempDir: File) {
        try {
            tempDir.deleteRecursively()
        } catch (e: Exception) {
            Timber.w(e, "[Import] Failed to cleanup temp dir: %s", tempDir.absolutePath)
        }
    }

    companion object {
        /** Maximum total extracted size: 50 MB. */
        const val MAX_EXTRACTED_SIZE_BYTES = 50L * 1024 * 1024

        /** Maximum number of entries inside one ZIP. */
        const val MAX_ENTRIES = 500
    }
}
