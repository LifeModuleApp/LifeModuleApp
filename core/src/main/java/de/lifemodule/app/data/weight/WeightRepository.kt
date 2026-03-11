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

package de.lifemodule.app.data.weight

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(private val dao: WeightDao) {
    fun getAllEntries() = dao.getAllEntries()
    fun getLatestEntry() = dao.getLatestEntry()
    fun getEntriesForDateRange(start: String, end: String) = dao.getEntriesForDateRange(start, end)
    suspend fun insert(entry: WeightEntryEntity) = dao.insert(entry)
    suspend fun delete(entry: WeightEntryEntity) = dao.delete(entry)
}
