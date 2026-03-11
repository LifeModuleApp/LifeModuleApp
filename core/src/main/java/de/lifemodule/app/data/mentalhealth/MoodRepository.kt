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

package de.lifemodule.app.data.mentalhealth

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MoodRepository @Inject constructor(
    private val moodDao: MoodDao
) {
    fun getEntryForDate(date: String): Flow<MoodEntryEntity?> = moodDao.getEntryForDate(date)
    fun getAllEntriesForDate(date: String): Flow<List<MoodEntryEntity>> = moodDao.getAllEntriesForDate(date)
    fun getAllEntries(): Flow<List<MoodEntryEntity>> = moodDao.getAllEntries()
    fun getRecentEntries(limit: Int = 7): Flow<List<MoodEntryEntity>> = moodDao.getRecentEntries(limit)
    suspend fun insert(entry: MoodEntryEntity): Long = moodDao.insert(entry)
    suspend fun update(entry: MoodEntryEntity) = moodDao.update(entry)
    suspend fun delete(entry: MoodEntryEntity) = moodDao.delete(entry)
}
