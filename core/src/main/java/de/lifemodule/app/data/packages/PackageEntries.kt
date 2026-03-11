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

package de.lifemodule.app.data.packages

import kotlinx.serialization.Serializable

/**
 * Top-level JSON wrapper written as the `entries` section inside the ZIP.
 *
 * Every list is optional - only the lists relevant to the chosen
 * [PackageContentType] are populated.  Unknown keys are ignored
 * by the receiver for forward-compatibility.
 */
@Serializable
data class PackageEntries(
    val exerciseDefinitions: List<de.lifemodule.app.data.packages.dto.ExportExerciseDefinition>? = null,
    val workoutTemplates: List<de.lifemodule.app.data.packages.dto.ExportWorkoutTemplate>? = null,
    val templateExercises: List<de.lifemodule.app.data.packages.dto.ExportTemplateExercise>? = null,
    val foodItems: List<de.lifemodule.app.data.packages.dto.ExportFoodItem>? = null,
    val supplements: List<de.lifemodule.app.data.packages.dto.ExportSupplement>? = null,
    val supplementIngredients: List<de.lifemodule.app.data.packages.dto.ExportSupplementIngredient>? = null,
    val habits: List<de.lifemodule.app.data.packages.dto.ExportHabit>? = null,
    val recipes: List<de.lifemodule.app.data.packages.dto.ExportRecipe>? = null,
    val recipeIngredients: List<de.lifemodule.app.data.packages.dto.ExportRecipeIngredient>? = null
)
