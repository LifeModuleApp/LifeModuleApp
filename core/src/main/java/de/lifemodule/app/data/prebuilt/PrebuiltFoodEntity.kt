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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Read-only food item shipped with the app in `prebuilt.db`.
 *
 * UUIDs use a deterministic `prebuilt-` prefix (UUID v5 namespace) so they
 * can never collide with user-generated v4 UUIDs.
 *
 * This entity is **never written to at runtime** - the prebuilt DB is opened
 * read-only via `createFromAsset()`.
 */
@Entity(
    tableName = "prebuilt_foods",
    indices = [Index("barcode")]
)
data class PrebuiltFoodEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    val name: String,

    @ColumnInfo(name = "kcal_per_100g")
    val kcalPer100g: Double,

    @ColumnInfo(name = "protein_per_100g")
    val proteinPer100g: Double,

    @ColumnInfo(name = "carbs_per_100g")
    val carbsPer100g: Double,

    @ColumnInfo(name = "fat_per_100g")
    val fatPer100g: Double,

    @ColumnInfo(name = "sugar_per_100g")
    val sugarPer100g: Double,

    val barcode: String? = null
)
