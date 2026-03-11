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

package de.lifemodule.app.ui.scanner

import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Result of OCR text recognition on a receipt image.
 *
 * Fields are best-effort extracted from the raw text. The user
 * can correct any OCR mistakes before saving.
 */
data class OcrResult(
    val rawText: String,
    val vendor: String?,
    val totalAmount: Double?,
    val vatAmount: Double?,
    val receiptDate: String?  // Raw date string as detected
)

/**
 * On-device text recognition using ML Kit (offline, no network needed).
 *
 * Processes a captured receipt image and attempts to extract
 * vendor name, total amount, VAT, and date.
 */
object ReceiptOcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    /**
     * Run OCR on the given image file.
     *
     * @return [OcrResult] with extracted fields, or `null` on failure.
     */
    suspend fun recognizeReceipt(context: Context, imageFile: File): OcrResult? {
        return try {
            val image = InputImage.fromFilePath(context, android.net.Uri.fromFile(imageFile))
            val text = recognizeText(image)

            if (text.isNullOrBlank()) {
                Timber.w("[Scanner] OCR returned empty text")
                return null
            }

            Timber.i("[Scanner] OCR raw text (%d chars): %s", text.length, text.take(200))

            OcrResult(
                rawText = text,
                vendor = extractVendor(text),
                totalAmount = extractTotalAmount(text),
                vatAmount = extractVatAmount(text),
                receiptDate = extractDate(text)
            )
        } catch (e: Exception) {
            Timber.e(e, "[Scanner] ML Kit text recognition failed")
            null
        }
    }

    private suspend fun recognizeText(image: InputImage): String? = suspendCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { result ->
                cont.resume(result.text)
            }
            .addOnFailureListener { e ->
                Timber.e(e, "[Scanner] ML Kit processing failed")
                cont.resume(null)
            }
    }

    // ── Heuristic field extraction ──────────────────────────────────────────

    /**
     * Vendor: usually the first non-empty line of the receipt.
     */
    private fun extractVendor(text: String): String? {
        return text.lines()
            .map { it.trim() }
            .firstOrNull { it.length >= 3 && !it.all { c -> c.isDigit() || c == '.' || c == ',' || c == ' ' } }
    }

    /**
     * Total amount: look for patterns like "SUMME", "TOTAL", "GESAMT", "ZU ZAHLEN"
     * followed by a decimal number.
     */
    private fun extractTotalAmount(text: String): Double? {
        val totalPatterns = listOf(
            Regex("""(?i)(?:summe|total|gesamt|zu\s*zahlen|betrag|netto\+mwst)[:\s]*(\d+[.,]\d{2})"""),
            Regex("""(?i)EUR\s*(\d+[.,]\d{2})"""),
            Regex("""(\d+[.,]\d{2})\s*€""")
        )

        for (pattern in totalPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val value = match.groupValues[1].replace(",", ".").toDoubleOrNull()
                if (value != null && value > 0) return value
            }
        }
        return null
    }

    /**
     * VAT amount: look for patterns like "MwSt", "USt", "VAT".
     */
    private fun extractVatAmount(text: String): Double? {
        val vatPatterns = listOf(
            Regex("""(?i)(?:mwst|ust|vat|mehrwertsteuer)[:\s]*(\d+[.,]\d{2})"""),
            Regex("""(?i)(\d+[.,]\d{2})\s*(?:mwst|ust|vat)""")
        )

        for (pattern in vatPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val value = match.groupValues[1].replace(",", ".").toDoubleOrNull()
                if (value != null && value > 0) return value
            }
        }
        return null
    }

    /**
     * Date: look for common date patterns (DD.MM.YYYY, DD/MM/YYYY, YYYY-MM-DD).
     */
    private fun extractDate(text: String): String? {
        val datePatterns = listOf(
            Regex("""\b(\d{2}[./]\d{2}[./]\d{4})\b"""),
            Regex("""\b(\d{4}-\d{2}-\d{2})\b"""),
            Regex("""\b(\d{2}[./]\d{2}[./]\d{2})\b""")
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1]
        }
        return null
    }
}
