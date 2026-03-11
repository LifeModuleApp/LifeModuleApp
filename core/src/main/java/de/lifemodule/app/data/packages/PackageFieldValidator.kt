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
import android.net.Uri
import timber.log.Timber

/**
 * Defence-in-depth field validator for imported community packages.
 *
 * Even though the server validates uploads, the app must independently verify
 * every field before inserting into Room - neither layer trusts the other.
 *
 * Validation failures throw [IllegalArgumentException] which the import pipeline
 * catches, logs with [Timber.w], and increments [ImportResult.failed].
 */
object PackageFieldValidator {

    /** UUID v4 format: 8-4-4-4-12 hex digits, version nibble = 4, variant = [89ab]. */
    private val UUID_REGEX = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    )

    /** URLs are never permitted inside package data. */
    private val LINK_REGEX = Regex("https?://|ftp://", RegexOption.IGNORE_CASE)

    /** Simple HTML tag detector. */
    private val HTML_TAG_REGEX = Regex("<[^>]+>")

    /** Maximum length of any single string field. */
    private const val MAX_STRING_LENGTH = 2000

    /** Maximum number of entities across all lists in a package. */
    const val MAX_TOTAL_ENTRIES = 5000

    /** Maximum compressed ZIP file size (10 MB). */
    const val MAX_ZIP_SIZE_BYTES = 10L * 1024 * 1024

    /**
     * Validate and return a UUID string.
     *
     * @throws IllegalArgumentException if format is invalid.
     */
    fun validateUuid(value: String): String {
        require(UUID_REGEX.matches(value)) { "Invalid UUID format: $value" }
        return value
    }

    /**
     * Validate a text field: check length, strip HTML, reject URLs.
     *
     * @param value     The raw string value from the deserialized package.
     * @param fieldName Human-readable name for error messages.
     * @return Sanitised string (HTML tags stripped, trimmed).
     * @throws IllegalArgumentException if validation fails.
     */
    fun validateString(value: String, fieldName: String): String {
        require(value.length <= MAX_STRING_LENGTH) {
            "Field '$fieldName' exceeds max length ($MAX_STRING_LENGTH)."
        }
        require(!LINK_REGEX.containsMatchIn(value)) {
            "Field '$fieldName' contains a URL, which is not permitted in package data."
        }
        // Strip HTML as a safety net (should never be present after server validation)
        return value.replace(HTML_TAG_REGEX, "").trim()
    }

    /**
     * Validate a numeric double field is within an expected range.
     *
     * @throws IllegalArgumentException if out of range.
     */
    fun validateDouble(value: Double, fieldName: String, min: Double, max: Double): Double {
        require(value in min..max) {
            "Field '$fieldName' value $value is out of range [$min, $max]."
        }
        return value
    }

    /**
     * Validate a numeric int field is within an expected range.
     *
     * @throws IllegalArgumentException if out of range.
     */
    fun validateInt(value: Int, fieldName: String, min: Int, max: Int): Int {
        require(value in min..max) {
            "Field '$fieldName' value $value is out of range [$min, $max]."
        }
        return value
    }

    /**
     * Check that the ZIP file size does not exceed the hard limit.
     *
     * @throws IllegalArgumentException if file exceeds [MAX_ZIP_SIZE_BYTES].
     */
    fun checkZipSize(uri: Uri, contentResolver: ContentResolver) {
        val size = contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        require(size <= MAX_ZIP_SIZE_BYTES) {
            "Package file exceeds maximum size of ${MAX_ZIP_SIZE_BYTES / 1024 / 1024} MB (actual: ${size / 1024 / 1024} MB)."
        }
    }

    /** ISO date format: yyyy-MM-dd. */
    private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    /** Maximum years into the future a date is considered valid. */
    private const val MAX_FUTURE_YEARS = 1L

    /**
     * Validate a date string is well-formed ISO-8601 (yyyy-MM-dd) and not
     * absurdly far in the future.
     *
     * @throws IllegalArgumentException if format is invalid or date is out of range.
     */
    fun validateDateString(value: String, fieldName: String): String {
        require(DATE_REGEX.matches(value)) {
            "Field '$fieldName' is not a valid date (expected yyyy-MM-dd): $value"
        }
        try {
            val date = java.time.LocalDate.parse(value)
            val maxFuture = java.time.LocalDate.now().plusYears(MAX_FUTURE_YEARS)
            require(!date.isAfter(maxFuture)) {
                "Field '$fieldName' date $value is too far in the future (max: $maxFuture)"
            }
        } catch (e: java.time.format.DateTimeParseException) {
            throw IllegalArgumentException("Field '$fieldName' contains invalid date: $value", e)
        }
        return value
    }

    /**
     * Validate that the total entry count across all lists is within limits.
     *
     * @throws IllegalArgumentException if count exceeds [MAX_TOTAL_ENTRIES].
     */
    fun checkEntryCount(entries: PackageEntries) {
        val total = listOfNotNull(
            entries.exerciseDefinitions,
            entries.workoutTemplates,
            entries.templateExercises,
            entries.foodItems,
            entries.supplements,
            entries.supplementIngredients,
            entries.habits,
            entries.recipes,
            entries.recipeIngredients
        ).sumOf { it.size }

        require(total <= MAX_TOTAL_ENTRIES) {
            "Package contains $total entries, exceeding the maximum of $MAX_TOTAL_ENTRIES."
        }
    }
}
