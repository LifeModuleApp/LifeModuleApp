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

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the SQLCipher database passphrase securely using the Android Keystore.
 *
 * Flow:
 *  1. On first launch, a random 32-byte passphrase is generated.
 *  2. The passphrase is encrypted with an AES-256 key stored in Android Keystore.
 *  3. The encrypted passphrase is stored in SharedPreferences.
 *  4. On subsequent launches, the passphrase is decrypted using the Keystore key.
 */
object DatabaseKeyManager {

    @PublishedApi internal val HEX_CHARS = "0123456789abcdef".toCharArray()

    /** Android Keystore alias for the wrapping key. */
    private const val KEYSTORE_ALIAS = "lifemodule_db_key"

    /** SharedPreferences file for the encrypted passphrase. */
    private const val PREFS_NAME = "db_encryption"
    private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
    private const val KEY_IV = "encryption_iv"

    /** AES-GCM tag length in bits. */
    private const val GCM_TAG_LENGTH = 128

    /**
     * Returns the SQLCipher passphrase as a ByteArray (raw 32 random bytes).
     * Generates and persists one on first call.
     *
     * If the stored passphrase cannot be decrypted (e.g. after a device transfer
     * where the Android Keystore key is lost), the orphaned credentials are cleared
     * and a fresh passphrase is generated. Any existing encrypted DB will be
     * inaccessible and should be deleted by the caller.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(KEY_IV, null)

        return if (encryptedBase64 != null && ivBase64 != null) {
            try {
                // Decrypt existing passphrase
                val encryptedBytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP)
                val iv = android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP)
                decryptPassphrase(encryptedBytes, iv)
            } catch (e: Exception) {
                // Keystore key is gone (device transfer / factory reset / key invalidated).
                // Clear orphaned credentials and generate a fresh passphrase.
                Timber.w(e, "Cannot decrypt stored passphrase - Keystore key likely lost. Generating new key.")
                prefs.edit().clear().apply()
                val passphrase = generateRandomPassphrase()
                val (encrypted, iv) = encryptPassphrase(passphrase)
                prefs.edit()
                    .putString(KEY_ENCRYPTED_PASSPHRASE, android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
                    .putString(KEY_IV, android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
                    .apply()
                Timber.d("Generated replacement database encryption key after Keystore failure")
                passphrase
            }
        } else {
            // First launch: generate, encrypt, persist
            val passphrase = generateRandomPassphrase()
            val (encrypted, iv) = encryptPassphrase(passphrase)
            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
                .putString(KEY_IV, android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
                .apply()
            Timber.d("Generated and stored new database encryption key")
            passphrase
        }
    }

    /**
     * Returns the passphrase as a 64-char hex string encoded to UTF-8 bytes.
     * This is what SupportFactory and all native SQLCipher operations must use
     * so that PBKDF2 key derivation is consistent.
     */
    fun getHexPassphrase(context: Context): ByteArray {
        val raw = getOrCreatePassphrase(context)
        val hexBytes = raw.joinToString("") { "%02x".format(it) }.toByteArray(Charsets.UTF_8)
        raw.fill(0)
        return hexBytes
    }

    /**
     * Executes [block] with the hex passphrase as a temporary String.
     * The underlying byte/char buffers are wiped immediately after use.
     */
    inline fun <T> withHexPassphraseString(context: Context, block: (String) -> T): T {
        val raw = getOrCreatePassphrase(context)
        val hexChars = CharArray(raw.size * 2)
        var outIdx = 0
        for (byte in raw) {
            val intVal = byte.toInt() and 0xFF
            hexChars[outIdx++] = HEX_CHARS[intVal ushr 4]
            hexChars[outIdx++] = HEX_CHARS[intVal and 0x0F]
        }
        raw.fill(0)

        val hexString = String(hexChars)
        return try {
            block(hexString)
        } finally {
            hexChars.fill('\u0000')
        }
    }

    /**
     * Returns true if an existing (unencrypted) database file exists on disk
     * but no passphrase has been generated yet - meaning we need to migrate.
     */
    fun needsMigrationFromUnencrypted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasKey = prefs.contains(KEY_ENCRYPTED_PASSPHRASE)
        val dbFile = context.getDatabasePath("lifemodule_database")
        return !hasKey && dbFile.exists()
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun generateRandomPassphrase(): ByteArray {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGen.generateKey()
    }

    private fun encryptPassphrase(passphrase: ByteArray): Pair<ByteArray, ByteArray> {
        val key = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(passphrase)
        return encrypted to cipher.iv
    }

    private fun decryptPassphrase(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val key = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }
}
