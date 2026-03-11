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

package de.lifemodule.app.ui.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.logbook.JourneyPurpose
import de.lifemodule.app.data.logbook.LogbookEntryEntity
import de.lifemodule.app.data.logbook.VehicleEntity
import de.lifemodule.app.util.HashChainUtil
import de.lifemodule.app.util.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class LogbookViewModel @Inject constructor(
    private val repository: LogbookRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val vehicles: StateFlow<List<VehicleEntity>> =
        repository.getAllVehicles()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entries: StateFlow<List<LogbookEntryEntity>> =
        repository.getAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess.asStateFlow()

    fun clearSaveState() { _saveSuccess.value = null }

    fun addVehicle(name: String, licensePlate: String?) {
        viewModelScope.launch {
            try {
                val now = timeProvider.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                repository.insertVehicle(
                    VehicleEntity(
                        createdAt = now,
                        updatedAt = now,
                        name = name,
                        licensePlate = licensePlate
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Logbook] Failed to insert vehicle")
            }
        }
    }

    fun addEntry(
        startLocation: String,
        endLocation: String,
        distanceKm: Double,
        purpose: JourneyPurpose,
        vehicleId: String,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                val now = timeProvider.now()
                val epochMillis = now.toInstant(ZoneOffset.UTC).toEpochMilli()
                val uuid = BaseEntity.generateUuid()

                // Hash chain: get previous entry's hash (or GENESIS)
                val lastEntry = repository.getLastEntry()
                val previousHash = lastEntry?.entryHash ?: HashChainUtil.GENESIS

                val entryHash = HashChainUtil.computeLogbookEntryHash(
                    previousHash = previousHash,
                    uuid = uuid,
                    journeyDate = epochMillis,
                    startLocation = startLocation,
                    endLocation = endLocation,
                    distanceKm = distanceKm,
                    purposeCode = purpose.name,
                    vehicleId = vehicleId,
                    notes = notes,
                    correctionOfUuid = null,
                    createdAt = epochMillis
                )

                repository.insertEntry(
                    LogbookEntryEntity(
                        uuid = uuid,
                        createdAt = epochMillis,
                        updatedAt = epochMillis,
                        journeyDate = epochMillis,
                        startLocation = startLocation,
                        endLocation = endLocation,
                        distanceKm = distanceKm,
                        purposeCode = purpose,
                        vehicleId = vehicleId,
                        notes = notes,
                        isFinalized = true,
                        entryHash = entryHash,
                        previousHash = previousHash
                    )
                )
                _saveSuccess.value = true
                Timber.i("[Logbook] Entry %s added (%.1f km, %s)", uuid, distanceKm, purpose)
            } catch (e: Exception) {
                Timber.e(e, "[Logbook] Failed to insert logbook entry")
                _saveSuccess.value = false
            }
        }
    }

    /**
     * Create a correction entry for an existing record (GoBD-compliant correction).
     */
    fun addCorrection(
        originalUuid: String,
        startLocation: String,
        endLocation: String,
        distanceKm: Double,
        purpose: JourneyPurpose,
        vehicleId: String,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                val now = timeProvider.now()
                val epochMillis = now.toInstant(ZoneOffset.UTC).toEpochMilli()
                val uuid = BaseEntity.generateUuid()

                val lastEntry = repository.getLastEntry()
                val previousHash = lastEntry?.entryHash ?: HashChainUtil.GENESIS

                val entryHash = HashChainUtil.computeLogbookEntryHash(
                    previousHash = previousHash,
                    uuid = uuid,
                    journeyDate = epochMillis,
                    startLocation = startLocation,
                    endLocation = endLocation,
                    distanceKm = distanceKm,
                    purposeCode = purpose.name,
                    vehicleId = vehicleId,
                    notes = notes,
                    correctionOfUuid = originalUuid,
                    createdAt = epochMillis
                )

                repository.insertEntry(
                    LogbookEntryEntity(
                        uuid = uuid,
                        createdAt = epochMillis,
                        updatedAt = epochMillis,
                        journeyDate = epochMillis,
                        startLocation = startLocation,
                        endLocation = endLocation,
                        distanceKm = distanceKm,
                        purposeCode = purpose,
                        vehicleId = vehicleId,
                        notes = notes,
                        isFinalized = true,
                        correctionOfUuid = originalUuid,
                        entryHash = entryHash,
                        previousHash = previousHash
                    )
                )
                _saveSuccess.value = true
                Timber.i("[Logbook] Correction for %s added as %s", originalUuid, uuid)
            } catch (e: Exception) {
                Timber.e(e, "[Logbook] Failed to insert correction entry")
                _saveSuccess.value = false
            }
        }
    }
}
