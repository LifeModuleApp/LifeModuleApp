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

package de.lifemodule.app.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor

/**
 * Custom BackupAgent that only participates in Google Auto-Backup
 * when the user has explicitly opted in via Settings.
 *
 * Default: backup is disabled (privacy-first).
 */
class ConditionalBackupAgent : BackupAgent() {

    private fun isBackupEnabled(): Boolean {
        val prefs = getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("google_backup_enabled", false)
    }

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (isBackupEnabled()) {
            super.onFullBackup(data)
        }
        // If disabled -> no data is written to the backup transport
    }

    override fun onRestoreFile(
        data: ParcelFileDescriptor,
        size: Long,
        destination: java.io.File,
        type: Int,
        mode: Long,
        mtime: Long
    ) {
        // Always allow restore - the data was backed up when the user allowed it
        super.onRestoreFile(data, size, destination, type, mode, mtime)
    }

    // Required overrides for key-value backup (unused with Auto-Backup)
    override fun onBackup(
        oldState: ParcelFileDescriptor,
        data: BackupDataOutput,
        newState: ParcelFileDescriptor
    ) { /* no-op */ }

    override fun onRestore(
        data: BackupDataInput,
        appVersionCode: Int,
        newState: ParcelFileDescriptor
    ) { /* no-op */ }
}
