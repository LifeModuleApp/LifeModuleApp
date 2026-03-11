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

import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.ImportSource
import de.lifemodule.app.data.nutrition.FoodItemEntity
import de.lifemodule.app.data.nutrition.NutritionRepository
import de.lifemodule.app.util.time.TimeProvider
import timber.log.Timber
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Clones a [PrebuiltFoodEntity] from the read-only prebuilt database into
 * the user's personal [de.lifemodule.app.data.LifeModuleDatabase].
 *
 * The clone receives a fresh UUID v4 and `importSource = USER`.
 * The prebuilt original is never modified.
 */
class ForkPrebuiltFoodUseCase @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val timeProvider: TimeProvider
) {
    /**
     * Forks [prebuilt] into the user's food database.
     *
     * @return the UUID of the newly created user food item.
     */
    suspend operator fun invoke(prebuilt: PrebuiltFoodEntity): String {
        val now = timeProvider.now().toInstant(ZoneOffset.UTC).toEpochMilli()
        val forked = FoodItemEntity(
            uuid = BaseEntity.generateUuid(),
            createdAt = now,
            updatedAt = now,
            importSource = ImportSource.USER,
            importedFromPackageId = null,
            name = prebuilt.name,
            kcalPer100g = prebuilt.kcalPer100g,
            proteinPer100g = prebuilt.proteinPer100g,
            carbsPer100g = prebuilt.carbsPer100g,
            fatPer100g = prebuilt.fatPer100g,
            sugarPer100g = prebuilt.sugarPer100g,
            barcode = prebuilt.barcode,
            isActive = true
        )
        nutritionRepository.insertFoodItem(forked)
        Timber.d("Forked prebuilt food '%s' -> user UUID %s", prebuilt.name, forked.uuid)
        return forked.uuid
    }
}
