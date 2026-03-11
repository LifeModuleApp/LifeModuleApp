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

import de.lifemodule.app.data.gym.ExerciseDefinitionEntity
import de.lifemodule.app.data.gym.WorkoutEntity
import de.lifemodule.app.data.gym.WorkoutTemplateExercise
import de.lifemodule.app.data.habits.HabitEntity
import de.lifemodule.app.data.nutrition.FoodItemEntity
import de.lifemodule.app.data.recipes.RecipeEntity
import de.lifemodule.app.data.recipes.RecipeIngredientEntity
import de.lifemodule.app.data.supplements.SupplementEntity
import de.lifemodule.app.data.supplements.SupplementIngredientEntity

/**
 * Holds the entities selected for export, grouped by content type.
 *
 * Each list is nullable - only the lists relevant to the chosen
 * [PackageContentType] need to be populated. The export pipeline
 * serialises every non-null list into the output ZIP.
 */
data class ExportSelection(
    val workoutTemplates: List<WorkoutEntity>? = null,
    val templateExercises: List<WorkoutTemplateExercise>? = null,
    val exerciseDefinitions: List<ExerciseDefinitionEntity>? = null,
    val foodItems: List<FoodItemEntity>? = null,
    val supplements: List<SupplementEntity>? = null,
    val supplementIngredients: List<SupplementIngredientEntity>? = null,
    val habits: List<HabitEntity>? = null,
    val recipes: List<RecipeEntity>? = null,
    val recipeIngredients: List<RecipeIngredientEntity>? = null
) {
    /** Total number of entities across all non-null lists. */
    val totalEntryCount: Int
        get() = listOfNotNull(
            workoutTemplates,
            templateExercises,
            exerciseDefinitions,
            foodItems,
            supplements,
            supplementIngredients,
            habits,
            recipes,
            recipeIngredients
        ).sumOf { it.size }
}
