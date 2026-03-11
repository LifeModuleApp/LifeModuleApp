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

package de.lifemodule.app.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * SHA-256 hash chain utility for GoBD-compliant modules (Logbook, Scanner).
 *
 * Every record in a hash chain contains:
 * - `previousHash` - the `entryHash` of the preceding record, or `"GENESIS"` for the first.
 * - `entryHash`    - `SHA-256(previousHash + field1 + field2 + ...)`
 *
 * This creates a tamper-evident chain: altering any record or re-ordering records
 * will invalidate all subsequent hashes.
 */
object HashChainUtil {

    /** Sentinel value for the very first record in a chain. */
    const val GENESIS = "GENESIS"

    /**
     * Compute SHA-256 of concatenated [parts].
     *
     * Each part is converted to string, concatenated with a pipe separator `|`,
     * and then hashed.
     *
     * @param parts Ordered field values. `null` values become the literal string `"null"`.
     * @return Lowercase hex-encoded SHA-256 digest.
     */
    fun computeHash(vararg parts: Any?): String {
        val input = parts.joinToString("|") { it?.toString() ?: "null" }
        return sha256(input.toByteArray(Charsets.UTF_8))
    }

    /**
     * Compute SHA-256 of a byte array (e.g. raw file bytes for image hashing).
     *
     * @return Lowercase hex-encoded SHA-256 digest.
     */
    fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute SHA-256 of a file (streaming, memory-efficient).
     *
     * @return Lowercase hex-encoded SHA-256 digest.
     */
    fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute SHA-256 of an [InputStream] (streaming, memory-efficient).
     * The stream is NOT closed by this function.
     *
     * @return Lowercase hex-encoded SHA-256 digest.
     */
    fun sha256Stream(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Build a logbook entry hash from its fields.
     *
     * @param previousHash Hash of the preceding entry, or [GENESIS].
     * @return Lowercase hex-encoded SHA-256 digest.
     */
    fun computeLogbookEntryHash(
        previousHash: String,
        uuid: String,
        journeyDate: Long,
        startLocation: String,
        endLocation: String,
        distanceKm: Double,
        purposeCode: String,
        vehicleId: String,
        notes: String?,
        correctionOfUuid: String?,
        createdAt: Long,
    ): String = computeHash(
        previousHash,
        uuid,
        journeyDate,
        startLocation,
        endLocation,
        distanceKm,
        purposeCode,
        vehicleId,
        notes,
        correctionOfUuid,
        createdAt
    )

    /**
     * Build a receipt finalization hash from its fields.
     *
     * @return Lowercase hex-encoded SHA-256 digest.
     */
    fun computeReceiptFinalizationHash(
        uuid: String,
        capturedAt: Long,
        vendor: String,
        receiptDate: Long,
        totalAmount: Double,
        vatAmount: Double?,
        currency: String,
        category: String,
        notes: String?,
        imageSha256: String,
        createdAt: Long,
    ): String = computeHash(
        uuid,
        capturedAt,
        vendor,
        receiptDate,
        totalAmount,
        vatAmount,
        currency,
        category,
        notes,
        imageSha256,
        createdAt
    )

    /**
     * Validate a chain of hashes. Returns `true` if the chain is intact.
     *
     * @param hashes Ordered list of (previousHash, entryHash) pairs.
     *               The first entry must have previousHash == [GENESIS].
     */
    fun validateChain(hashes: List<Pair<String, String>>): Boolean {
        if (hashes.isEmpty()) return true
        if (hashes.first().first != GENESIS) return false

        for (i in 1 until hashes.size) {
            if (hashes[i].first != hashes[i - 1].second) return false
        }
        return true
    }
}
