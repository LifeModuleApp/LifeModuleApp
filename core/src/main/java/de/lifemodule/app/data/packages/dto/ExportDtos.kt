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

package de.lifemodule.app.data.packages.dto

import de.lifemodule.app.data.gym.ExerciseDefinitionEntity
import de.lifemodule.app.data.gym.WorkoutEntity
import de.lifemodule.app.data.gym.WorkoutTemplateExercise
import de.lifemodule.app.data.habits.HabitEntity
import de.lifemodule.app.data.nutrition.FoodItemEntity
import de.lifemodule.app.data.supplements.SupplementEntity
import de.lifemodule.app.data.supplements.SupplementIngredientEntity
import kotlinx.serialization.Serializable

/*
 * Transport DTOs for export/import ZIP packages.
 *
 * Every DTO strips personal & provenance fields that the roadmap
 * marks as @ExportExclude:
 *   - createdAt / updatedAt  - receiver sets fresh timestamps
 *   - importedFromPackageId  - regenerated on import
 *   - imagePath              - local file paths are device-specific
 *
 * uuid is **preserved** so the merge rule (IF NOT EXISTS uuid THEN INSERT)
 * works correctly on the receiver side.
 */

// ── Gym ──────────────────────────────────────────────────────────────────

@Serializable
data class ExportExerciseDefinition(
    val uuid: String,
    val name: String,
    val category: String,
    val muscleGroup: String,
    val notes: String
)

fun ExerciseDefinitionEntity.toExportDto() = ExportExerciseDefinition(
    uuid = uuid, name = name, category = category,
    muscleGroup = muscleGroup, notes = notes
)

@Serializable
data class ExportWorkoutTemplate(
    val uuid: String,
    val name: String,
    val date: String,
    val durationMinutes: Int?,
    val notes: String
)

fun WorkoutEntity.toExportDto() = ExportWorkoutTemplate(
    uuid = uuid, name = name, date = date,
    durationMinutes = durationMinutes, notes = notes
)

@Serializable
data class ExportTemplateExercise(
    val uuid: String,
    val workoutId: String,
    val exerciseName: String,
    val orderIndex: Int
)

fun WorkoutTemplateExercise.toExportDto() = ExportTemplateExercise(
    uuid = uuid, workoutId = workoutId,
    exerciseName = exerciseName, orderIndex = orderIndex
)

// ── Nutrition ────────────────────────────────────────────────────────────

@Serializable
data class ExportFoodItem(
    val uuid: String,
    val name: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val sugarPer100g: Double,
    val barcode: String?
)

fun FoodItemEntity.toExportDto() = ExportFoodItem(
    uuid = uuid, name = name,
    kcalPer100g = kcalPer100g, proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g, fatPer100g = fatPer100g,
    sugarPer100g = sugarPer100g, barcode = barcode
)

// ── Supplements ──────────────────────────────────────────────────────────

@Serializable
data class ExportSupplement(
    val uuid: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val timesPerDay: Int,
    val timeOfDay: String,
    val durationDays: Int?,
    val notes: String?
)

fun SupplementEntity.toExportDto() = ExportSupplement(
    uuid = uuid, name = name, dosage = dosage,
    frequency = frequency, timesPerDay = timesPerDay,
    timeOfDay = timeOfDay, durationDays = durationDays, notes = notes
)

@Serializable
data class ExportSupplementIngredient(
    val uuid: String,
    val supplementId: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val rvsPct: Double?
)

fun SupplementIngredientEntity.toExportDto() = ExportSupplementIngredient(
    uuid = uuid, supplementId = supplementId,
    name = name, amount = amount, unit = unit, rvsPct = rvsPct
)

// ── Habits ───────────────────────────────────────────────────────────────

@Serializable
data class ExportHabit(
    val uuid: String,
    val name: String,
    val emoji: String,
    val frequency: String,
    val repeatIntervalDays: Int,
    val timeOfDay: String,
    val isPositive: Boolean
)

fun HabitEntity.toExportDto() = ExportHabit(
    uuid = uuid, name = name, emoji = emoji,
    frequency = frequency, repeatIntervalDays = repeatIntervalDays,
    timeOfDay = timeOfDay, isPositive = isPositive
)

// ── Recipes ──────────────────────────────────────────────────────────────

@Serializable
data class ExportRecipe(
    val uuid: String,
    val title: String,
    val instructions: String,
    val category: String,
    val servings: Int,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
    val notes: String?
)

fun de.lifemodule.app.data.recipes.RecipeEntity.toExportDto() = ExportRecipe(
    uuid = uuid, title = title, instructions = instructions,
    category = category.name, servings = servings,
    prepTimeMinutes = prepTimeMinutes, cookTimeMinutes = cookTimeMinutes,
    notes = notes
)

@Serializable
data class ExportRecipeIngredient(
    val uuid: String,
    val recipeId: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val orderIndex: Int,
    val foodItemUuid: String?
)

fun de.lifemodule.app.data.recipes.RecipeIngredientEntity.toExportDto() = ExportRecipeIngredient(
    uuid = uuid, recipeId = recipeId,
    name = name, quantity = quantity, unit = unit,
    orderIndex = orderIndex, foodItemUuid = foodItemUuid
)
