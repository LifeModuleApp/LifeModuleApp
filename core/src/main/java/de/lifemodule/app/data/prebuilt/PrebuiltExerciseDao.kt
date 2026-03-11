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

package de.lifemodule.app.data.prebuilt

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Read-only DAO for the prebuilt exercise library. */
@Dao
interface PrebuiltExerciseDao {

    @Query("SELECT * FROM prebuilt_exercises ORDER BY name ASC")
    fun getAll(): Flow<List<PrebuiltExerciseEntity>>

    @Query("SELECT * FROM prebuilt_exercises WHERE category = :category ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<PrebuiltExerciseEntity>>

    @Query("SELECT * FROM prebuilt_exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<PrebuiltExerciseEntity>>

    @Query("SELECT * FROM prebuilt_exercises WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): PrebuiltExerciseEntity?

    @Query("SELECT DISTINCT category FROM prebuilt_exercises ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
}
