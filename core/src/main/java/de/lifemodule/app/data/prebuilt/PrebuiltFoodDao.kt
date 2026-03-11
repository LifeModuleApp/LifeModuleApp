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

/** Read-only DAO for the prebuilt food database. */
@Dao
interface PrebuiltFoodDao {

    @Query("SELECT * FROM prebuilt_foods ORDER BY name ASC")
    fun getAll(): Flow<List<PrebuiltFoodEntity>>

    @Query("SELECT * FROM prebuilt_foods WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<PrebuiltFoodEntity>>

    @Query("SELECT * FROM prebuilt_foods WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): PrebuiltFoodEntity?

    @Query("SELECT * FROM prebuilt_foods WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): PrebuiltFoodEntity?
}
