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

package de.lifemodule.app.data.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import de.lifemodule.app.data.error.ErrorLogger
import kotlinx.coroutines.runBlocking

/** Status of the Health Connect SDK on this device. */
enum class HcSdkStatus {
    /** Health Connect is installed and ready. */
    AVAILABLE,
    /** Device doesn't support Health Connect (Android < 9 or no provider). */
    NOT_SUPPORTED,
    /** Provider app is outdated - user must update the Health Connect app. */
    NEEDS_UPDATE
}

/**
 * Singleton wrapper around [HealthConnectClient].
 *
 * Provides:
 * - SDK availability check (never crashes on unsupported devices).
 * - Lazy client instantiation - only when the SDK is actually available.
 * - The permission set this app requires.
 * - A helper to check whether all permissions have already been granted.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorLogger: ErrorLogger
) {
    // ── SDK status ────────────────────────────────────────────────────────────

    val sdkStatus: HcSdkStatus by lazy {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE                          -> HcSdkStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HcSdkStatus.NEEDS_UPDATE
            else                                                       -> HcSdkStatus.NOT_SUPPORTED
        }
    }

    // ── Client (null when SDK is not available) ───────────────────────────────

    val client: HealthConnectClient? by lazy {
        if (sdkStatus == HcSdkStatus.AVAILABLE) {
            try {
                HealthConnectClient.getOrCreate(context)
            } catch (e: Exception) {
                runBlocking {
                    errorLogger.logError("HealthConnectManager", "Failed to create HealthConnectClient", e, "ERROR")
                }
                null
            }
        } else null
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    /**
     * All permissions this module needs.
     * New data types can be added here - ViewModel and Repository already use this set.
     */
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    /**
     * Returns `true` if every permission in [permissions] has been granted.
     * Returns `false` if the SDK is unavailable or a client error occurs.
     */
    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return try {
            c.permissionController.getGrantedPermissions().containsAll(permissions)
        } catch (e: Exception) {
            runBlocking {
                errorLogger.logError("HealthConnectManager", "Could not check permissions", e, "WARNING")
            }
            false
        }
    }

    /**
     * Returns the [ActivityResultContract] to launch the HC permission dialog,
     * or a no-op contract when the SDK is unavailable (prevents crashes).
     *
     * Call this from a Composable via `remember { ... }` and pass it to
     * `rememberLauncherForActivityResult`.
     */
    fun createPermissionContract(): ActivityResultContract<Set<String>, Set<String>> {
        // PermissionController.createRequestPermissionResultContract() is a static
        // companion method - safe to call even before the client is created,
        // but we still gate on SDK availability so the system doesn’t throw.
        return if (sdkStatus == HcSdkStatus.AVAILABLE) {
            PermissionController.createRequestPermissionResultContract()
        } else {
            NoOpPermissionContract()
        }
    }
}
