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

import de.lifemodule.app.data.logbook.KmByPurposeSummary
import de.lifemodule.app.data.logbook.LogbookDao
import de.lifemodule.app.data.logbook.LogbookEntryEntity
import de.lifemodule.app.data.logbook.VehicleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogbookRepository @Inject constructor(
    private val logbookDao: LogbookDao
) {
    fun getAllVehicles(): Flow<List<VehicleEntity>> = logbookDao.getAllVehicles()

    suspend fun getVehicleByUuid(uuid: String): VehicleEntity? = logbookDao.getVehicleByUuid(uuid)

    suspend fun insertVehicle(vehicle: VehicleEntity): Long = logbookDao.insertVehicle(vehicle)

    fun getAllEntries(): Flow<List<LogbookEntryEntity>> = logbookDao.getAllEntries()

    fun getEntriesForVehicle(vehicleId: String): Flow<List<LogbookEntryEntity>> =
        logbookDao.getEntriesForVehicle(vehicleId)

    fun getEntriesInRange(startMillis: Long, endMillis: Long): Flow<List<LogbookEntryEntity>> =
        logbookDao.getEntriesInRange(startMillis, endMillis)

    suspend fun getLastEntry(): LogbookEntryEntity? = logbookDao.getLastEntry()

    suspend fun insertEntry(entry: LogbookEntryEntity): Long = logbookDao.insertEntry(entry)

    suspend fun getKmByPurpose(startMillis: Long, endMillis: Long): List<KmByPurposeSummary> =
        logbookDao.getKmByPurpose(startMillis, endMillis)
}
