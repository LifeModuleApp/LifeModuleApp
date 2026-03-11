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

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Read-only Room database shipped as an asset (`databases/prebuilt.db`).
 *
 * Contains reference data (exercise library, base food database) that the user
 * can browse and "fork" into their personal [de.lifemodule.app.data.LifeModuleDatabase].
 *
 * This DB is opened via `createFromAsset()` with `fallbackToDestructiveMigration()`
 * so that app updates can ship a new version without migration overhead - it's
 * purely reference data, not user content.
 */
@Database(
    entities = [
        PrebuiltExerciseEntity::class,
        PrebuiltFoodEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PrebuiltDatabase : RoomDatabase() {
    abstract fun exerciseDao(): PrebuiltExerciseDao
    abstract fun foodDao(): PrebuiltFoodDao
}
