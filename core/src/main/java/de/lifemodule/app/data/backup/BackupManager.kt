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

package de.lifemodule.app.data.backup

import android.content.Context
import android.net.Uri
import de.lifemodule.app.data.DatabaseKeyManager
import de.lifemodule.app.data.LifeModuleDatabase
import de.lifemodule.app.data.error.ErrorLogger
import de.lifemodule.app.data.packages.ExportPackageUseCase
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles full database backup (export) and restore (import).
 *
 * Backup format: `.lmbackup` file (ZIP containing):
 *   - `lifemodule.db`   - Room database file
 *   - `metadata.json`   - version info, timestamp
 */
@Singleton
class BackupManager @Inject constructor(
    private val db: LifeModuleDatabase,
    private val timeProvider: TimeProvider,
    private val errorLogger: ErrorLogger,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val DB_NAME = "lifemodule_database"
    }

    /**
     * Creates a backup of the Room database to the given Uri (SAF).
     * The database is decrypted to a portable plaintext copy so backups
     * can be restored on any device regardless of encryption key.
     * Returns true on success, false on failure.
     */
    suspend fun createBackup(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Checkpoint WAL to flush all pending writes
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }

            // 2. Get database file
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@withContext false

            // 3. Decrypt database to a portable plaintext temp file
            val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val plaintextTemp = File(backupDir, "backup_plain_temp.db")
            
            // Pre-emptive cleanup: if the app hard-crashed during a previous backup,
            // this unencrypted file might still exist. Erase it before continuing.
            if (plaintextTemp.exists()) plaintextTemp.delete()

            try {
                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)

                DatabaseKeyManager.withHexPassphraseString(context) { hexKey ->
                    val encDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                        dbFile.absolutePath, hexKey, null,
                        net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE or net.sqlcipher.database.SQLiteDatabase.CREATE_IF_NECESSARY
                    )
                    encDb.rawExecSQL("ATTACH DATABASE '${plaintextTemp.absolutePath}' AS plaintext KEY ''")
                    encDb.rawQuery("SELECT sqlcipher_export('plaintext')", null).use { it.moveToFirst() }
                    encDb.rawExecSQL("DETACH DATABASE plaintext")
                    encDb.close()

                    // 4. Create backup ZIP directly into the target Uri
                    val timestamp = timeProvider.today().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    
                    context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                        ZipOutputStream(outputStream).use { zip ->
                            // Add decrypted database file
                            zip.putNextEntry(ZipEntry("lifemodule.db"))
                            plaintextTemp.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()

                            // Add metadata
                            zip.putNextEntry(ZipEntry("metadata.json"))
                            val metadata = JSONObject().apply {
                                put("dbVersion", db.openHelper.readableDatabase.version)
                                put("appVersion", ExportPackageUseCase.APP_VERSION)
                                put("timestamp", timeProvider.currentTimeMillis())
                                put("date", timestamp)
                            }
                            zip.write(metadata.toString().toByteArray(Charsets.UTF_8))
                            zip.closeEntry()
                        }
                    } ?: return@withHexPassphraseString false

                    true
                } ?: false
            } finally {
                // Always clean up plaintext temp even if process is killed mid-operation
                plaintextTemp.delete()
            }
        } catch (e: Exception) {
            errorLogger.logError("BackupManager", "Backup creation failed", e)
            false
        }
    }

    /**
     * Restores a backup from the given URI.
     *
     * ### Safety architecture (3-phase):
     * 1. **Staging** - extract, validate metadata, PRAGMA integrity_check
     * 2. **Execution** - encrypt, rename live DB to `.bak`, move new DB into place
     * 3. **Recovery** - if anything fails after the rename, roll back from `.bak`
     *
     * Returns true on success, false on failure.
     *
     * IMPORTANT: After restore, the activity must be recreated to re-open the database.
     */
    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DB_NAME)
        val bakFile = File(dbFile.path + ".bak")

        try {
            // ── 1. Copy input to temp file ──────────────────────────────
            val tempDir = File(context.cacheDir, "restore_temp").apply { mkdirs() }
            val tempZip = File(tempDir, "restore.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempZip.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            // ── 2. Validate backup structure ────────────────────────────
            ZipFile(tempZip).use { zipFile ->
                val dbEntry = zipFile.getEntry("lifemodule.db")
                if (dbEntry == null) {
                    Timber.e("[Restore] ZIP missing lifemodule.db entry")
                    tempZip.delete()
                    return@withContext false
                }

                // ── 2b. Strict version gating ───────────────────────────
                val metaEntry = zipFile.getEntry("metadata.json")
                if (metaEntry != null) {
                    val metaJson = zipFile.getInputStream(metaEntry).bufferedReader().readText()
                    val meta = JSONObject(metaJson)
                    val backupDbVersion = meta.optInt("dbVersion", -1)
                    val currentDbVersion = db.openHelper.readableDatabase.version
                    if (backupDbVersion > currentDbVersion) {
                        Timber.e(
                            "[Restore] HARD REJECT - backup DB v%d > app DB v%d",
                            backupDbVersion, currentDbVersion
                        )
                        errorLogger.logError(
                            "BackupManager",
                            "Backup DB v$backupDbVersion > app DB v$currentDbVersion - cannot restore",
                            null
                        )
                        tempZip.delete()
                        return@withContext false
                    }
                    // Check appVersion if present (backups from newer major versions are rejected)
                    val backupAppVersion = meta.optString("appVersion", "")
                    if (backupAppVersion.isNotBlank()) {
                        val backupMajor = backupAppVersion.split(".").firstOrNull()?.toIntOrNull()
                        val currentMajor = ExportPackageUseCase.APP_VERSION.split(".").firstOrNull()?.toIntOrNull()
                        if (backupMajor != null && currentMajor != null && backupMajor > currentMajor) {
                            Timber.e(
                                "[Restore] HARD REJECT - backup app v%s > current v%s",
                                backupAppVersion, ExportPackageUseCase.APP_VERSION
                            )
                            tempZip.delete()
                            return@withContext false
                        }
                    }
                } else {
                    Timber.w("[Restore] No metadata.json in backup - skipping version check")
                }

                // ── 3. Close the database ───────────────────────────────
                db.close()

                // ── 4. Extract plaintext database to temp ───────────────
                val plaintextTemp = File(dbFile.parentFile, "restore_plain_temp.db")
                val encryptedTemp = File(dbFile.parentFile, "restore_encrypted_temp.db")

                try {
                    zipFile.getInputStream(dbEntry).use { input ->
                        plaintextTemp.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // ── 5. PRAGMA integrity_check on restored DB ────────
                    val restoredDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                        plaintextTemp.path,
                        null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                    )
                    val integrityOk = restoredDb.use { sdb ->
                        val integrity = sdb.rawQuery("PRAGMA integrity_check", null)
                        val ok = integrity.use { cursor ->
                            cursor.moveToFirst() && cursor.getString(0) == "ok"
                        }
                        if (!ok) {
                            Timber.e("[Restore] PRAGMA integrity_check FAILED - backup is corrupt")
                            return@withContext false
                        }

                        // 5b. Validate required tables exist
                        val requiredTables = listOf(
                            "food_items", "daily_food_entries", "habits",
                            "mood_entries", "courses", "calendar_events", "gym_sessions"
                        )
                        requiredTables.all { table ->
                            sdb.rawQuery(
                                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                                arrayOf(table)
                            ).use { it.moveToFirst() }
                        }
                    }
                    if (!integrityOk) {
                        Timber.e("[Restore] Restored DB missing required tables")
                        return@withContext false
                    }

                    Timber.d("[Restore] Staging passed - integrity OK, tables validated")

                    // ── 6. Encrypt the plaintext DB ─────────────────────
                    net.sqlcipher.database.SQLiteDatabase.loadLibs(context)

                    DatabaseKeyManager.withHexPassphraseString(context) { hexKey ->
                        val plainDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                            plaintextTemp.absolutePath, "", null,
                            net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE
                        )
                        plainDb.rawExecSQL("ATTACH DATABASE '${encryptedTemp.absolutePath}' AS encrypted KEY '$hexKey'")
                        plainDb.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { it.moveToFirst() }
                        plainDb.rawExecSQL("DETACH DATABASE encrypted")
                        plainDb.close()
                    }

                    // ── 7. Safety swap: .bak rename instead of delete ───
                    //    If anything fails after this point, we can
                    //    roll back from the .bak file.
                    val hadExistingDb = dbFile.exists()
                    if (bakFile.exists()) bakFile.delete() // cleanup from previous failed restore
                    if (hadExistingDb) {
                        if (!dbFile.renameTo(bakFile)) {
                            Timber.e("[Restore] Failed to rename live DB to .bak")
                            return@withContext false
                        }
                    }
                    File(dbFile.path + "-wal").delete()
                    File(dbFile.path + "-shm").delete()

                    if (!encryptedTemp.renameTo(dbFile)) {
                        // ROLLBACK: restore from .bak
                        Timber.e("[Restore] Failed to move encrypted DB into place - rolling back")
                        if (hadExistingDb) bakFile.renameTo(dbFile)
                        return@withContext false
                    }

                    // ── 8. Success -> delete .bak ────────────────────────
                    bakFile.delete()
                    Timber.i("[Restore] Database restored successfully")
                } finally {
                    // Always clean up temp files even on failure
                    plaintextTemp.delete()
                    encryptedTemp.delete()
                }
            }

            tempZip.delete()
            true
        } catch (e: Exception) {
            // Emergency rollback: if .bak exists and live DB is gone, restore it
            if (bakFile.exists() && !dbFile.exists()) {
                bakFile.renameTo(dbFile)
                Timber.w("[Restore] Emergency rollback from .bak after exception")
            }
            errorLogger.logError("BackupManager", "Restore failed", e)
            false
        }
    }
}
