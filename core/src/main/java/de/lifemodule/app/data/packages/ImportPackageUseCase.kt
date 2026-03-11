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

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.ImportSource
import de.lifemodule.app.data.LifeModuleDatabase
import de.lifemodule.app.data.gym.ExerciseDefinitionEntity
import de.lifemodule.app.data.gym.GymDao
import de.lifemodule.app.data.gym.WorkoutEntity
import de.lifemodule.app.data.gym.WorkoutTemplateExercise
import de.lifemodule.app.data.habits.HabitDao
import de.lifemodule.app.data.habits.HabitEntity
import de.lifemodule.app.data.nutrition.FoodDao
import de.lifemodule.app.data.nutrition.FoodItemEntity
import de.lifemodule.app.data.packages.dto.ExportExerciseDefinition
import de.lifemodule.app.data.packages.dto.ExportFoodItem
import de.lifemodule.app.data.packages.dto.ExportHabit
import de.lifemodule.app.data.packages.dto.ExportRecipe
import de.lifemodule.app.data.packages.dto.ExportRecipeIngredient
import de.lifemodule.app.data.packages.dto.ExportSupplement
import de.lifemodule.app.data.packages.dto.ExportSupplementIngredient
import de.lifemodule.app.data.packages.dto.ExportTemplateExercise
import de.lifemodule.app.data.packages.dto.ExportWorkoutTemplate
import de.lifemodule.app.data.recipes.RecipeCategory
import de.lifemodule.app.data.recipes.RecipeDao
import de.lifemodule.app.data.recipes.RecipeEntity
import de.lifemodule.app.data.recipes.RecipeIngredientEntity
import de.lifemodule.app.data.supplements.SupplementDao
import de.lifemodule.app.data.supplements.SupplementEntity
import de.lifemodule.app.data.supplements.SupplementIngredientEntity
import de.lifemodule.app.util.time.TimeProvider
import kotlinx.serialization.json.Json
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives a ZIP package URI, validates it, migrates it if needed, and merges
 * its contents into the user's Room database without UUID collisions.
 *
 * Merge policy (non-negotiable):
 *   - UUID exists -> SKIP (never overwrite personal data)
 *   - UUID absent  -> INSERT with importSource = COMMUNITY_HUB
 *
 * @see ZipPackageParser
 * @see ManifestValidator
 * @see ImportMigrationManager
 */
class ImportPackageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: LifeModuleDatabase,
    private val parser: ZipPackageParser,
    private val gymDao: GymDao,
    private val foodDao: FoodDao,
    private val supplementDao: SupplementDao,
    private val habitDao: HabitDao,
    private val recipeDao: RecipeDao,
    private val timeProvider: TimeProvider
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Import a community package from the given [uri].
     *
     * This is a long-running suspending function suitable for
     * `viewModelScope.launch(Dispatchers.IO)`.
     */
    suspend fun invoke(uri: Uri): ImportResult {
        // ── 0. ZIP size gate ─────────────────────────────────────────────
        try {
            PackageFieldValidator.checkZipSize(uri, context.contentResolver)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "[Import] ZIP size check failed")
            return errorResult(e.message ?: "ZIP too large")
        }

        // ── 1. Extract ZIP ───────────────────────────────────────────────
        val parsed = parser.extract(uri).getOrElse { error ->
            Timber.e("[Import] Extraction failed: %s", error.message)
            return ImportResult(
                packageId = "",
                inserted = 0,
                skipped = 0,
                failed = 0,
                errors = listOf(error.message ?: "Extraction failed")
            )
        }

        try {
            // ── 2. Parse & validate manifest ────────────────────────────
            val manifest: PackageManifest = try {
                json.decodeFromString<PackageManifest>(parsed.manifest)
            } catch (e: Exception) {
                Timber.e(e, "[Import] Manifest deserialization failed")
                return errorResult("Invalid manifest.json: ${e.message}")
            }

            when (val validation = ManifestValidator.validate(manifest)) {
                is ManifestValidator.Result.Valid -> { /* OK */ }
                is ManifestValidator.Result.Invalid -> {
                    Timber.w("[Import] Manifest invalid: %s", validation.reason)
                    return errorResult("Manifest invalid: ${validation.reason}")
                }
            }

            // ── 3. Schema migration ─────────────────────────────────────
            val rawEntries = JSONObject(parsed.entries)
            val migratedEntries = ImportMigrationManager.migrate(
                rawEntries, manifest.schemaVersion
            )
            val entriesJson = migratedEntries.toString()

            // ── 4. Deserialize entries ──────────────────────────────────
            val entries: PackageEntries = try {
                json.decodeFromString<PackageEntries>(entriesJson)
            } catch (e: Exception) {
                Timber.e(e, "[Import] Entries deserialization failed")
                return errorResult("Invalid entries.json: ${e.message}")
            }

            // ── 4b. Entry count gate ───────────────────────────────────
            try {
                PackageFieldValidator.checkEntryCount(entries)
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "[Import] Entry count check failed")
                return errorResult(e.message ?: "Too many entries")
            }

            // ══════════════════════════════════════════════════════════════
            // Phase 1 - STAGING: validate every entity in RAM.
            //           Any single validation failure -> reject ENTIRE package.
            //           No database writes happen in this phase.
            // ══════════════════════════════════════════════════════════════
            val now = timeProvider.now()
            val epochMillis = now.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            val packageId = manifest.packageId

            val staged = try {
                stageEntities(entries, epochMillis, packageId)
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "[Import] Staging failed - package %s rejected", manifest.packageId)
                return errorResult("Rejected (validation): ${e.message}")
            }

            Timber.d(
                "[Import] Staging passed for package %s (%d entities validated)",
                packageId, staged.totalCount
            )

            // ══════════════════════════════════════════════════════════════
            // Phase 2 - EXECUTION: atomic transaction write.
            //           All inserts run inside a single Room transaction.
            //           If any DAO call throws, Room rolls back everything.
            // ══════════════════════════════════════════════════════════════
            var inserted = 0
            var skipped = 0

            try {
                db.withTransaction {
                    fun count(result: Long) { if (result != -1L) inserted++ else skipped++ }

                    staged.exerciseDefinitions.forEach { count(gymDao.insertExerciseDefinitionIgnore(it)) }
                    staged.workoutTemplates.forEach { count(gymDao.insertWorkoutIgnore(it)) }
                    staged.templateExercises.forEach { count(gymDao.insertTemplateExerciseIgnore(it)) }
                    staged.foodItems.forEach { count(foodDao.insertFoodItemIgnore(it)) }
                    staged.supplements.forEach { count(supplementDao.insertSupplementIgnore(it)) }
                    staged.supplementIngredients.forEach { count(supplementDao.insertIngredientIgnore(it)) }
                    staged.habits.forEach { count(habitDao.insertHabitIgnore(it)) }
                    staged.recipes.forEach { count(recipeDao.insertRecipeIgnore(it)) }
                    staged.recipeIngredients.forEach { count(recipeDao.insertIngredientIgnore(it)) }
                }
            } catch (e: Exception) {
                // ══════════════════════════════════════════════════════════
                // Phase 3 - RECOVERY: Room already rolled back the transaction.
                //           No partial state can survive this point.
                // ══════════════════════════════════════════════════════════
                Timber.e(e, "[Import] Transaction failed - all changes rolled back for package %s", packageId)
                return errorResult("Import failed (rolled back): ${e.message}")
            }

            Timber.i(
                "[Import] Package %s complete: %d inserted, %d skipped (staged %d entities)",
                packageId, inserted, skipped, staged.totalCount
            )

            return ImportResult(
                packageId = packageId,
                inserted = inserted,
                skipped = skipped,
                failed = 0,
                errors = emptyList()
            )

        } finally {
            // ── 6. Cleanup temp dir ─────────────────────────────────────
            parser.cleanup(parsed.tempDir)
        }
    }

    private fun errorResult(message: String) = ImportResult(
        packageId = "",
        inserted = 0,
        skipped = 0,
        failed = 0,
        errors = listOf(message)
    )

    // ═══════════════════════════════════════════════════════════════════════
    // Phase 1 helper - validate all DTOs into Room entities in RAM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Converts every DTO in [entries] to its Room entity via the [toEntity]
     * extension functions (which call [PackageFieldValidator]).
     *
     * If **any** entity fails validation, the resulting [IllegalArgumentException]
     * propagates upward and aborts the entire import before any DB write.
     */
    private fun stageEntities(
        entries: PackageEntries,
        epochMillis: Long,
        packageId: String
    ): StagedImport = StagedImport(
        exerciseDefinitions = entries.exerciseDefinitions?.map { it.toEntity(epochMillis, packageId) } ?: emptyList(),
        workoutTemplates = entries.workoutTemplates?.map { it.toEntity(epochMillis, packageId) } ?: emptyList(),
        templateExercises = entries.templateExercises?.map { it.toEntity(epochMillis, packageId) } ?: emptyList(),
        foodItems = entries.foodItems?.map { it.toEntity(epochMillis, packageId) } ?: emptyList(),
        supplements = entries.supplements?.map { it.toEntity(epochMillis, packageId) } ?: emptyList(),
        supplementIngredients = entries.supplementIngredients?.map { it.toEntity(epochMillis, packageId) } ?: emptyList(),
        habits = entries.habits?.map { it.toEntity(epochMillis, packageId) } ?: emptyList(),
        recipes = entries.recipes?.map { it.toEntity(epochMillis, packageId) } ?: emptyList(),
        recipeIngredients = entries.recipeIngredients?.map { it.toEntity(epochMillis, packageId) } ?: emptyList()
    )

    /**
     * All entities that passed validation, ready for atomic transaction write.
     * Created during Phase 1 (staging), consumed during Phase 2 (execution).
     */
    private data class StagedImport(
        val exerciseDefinitions: List<ExerciseDefinitionEntity>,
        val workoutTemplates: List<WorkoutEntity>,
        val templateExercises: List<WorkoutTemplateExercise>,
        val foodItems: List<FoodItemEntity>,
        val supplements: List<SupplementEntity>,
        val supplementIngredients: List<SupplementIngredientEntity>,
        val habits: List<HabitEntity>,
        val recipes: List<RecipeEntity>,
        val recipeIngredients: List<RecipeIngredientEntity>
    ) {
        val totalCount: Int get() =
            exerciseDefinitions.size + workoutTemplates.size + templateExercises.size +
            foodItems.size + supplements.size + supplementIngredients.size +
            habits.size + recipes.size + recipeIngredients.size
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DTO -> Entity conversion (reverse of toExportDto)
// ═══════════════════════════════════════════════════════════════════════════

private fun ExportExerciseDefinition.toEntity(
    epochMillis: Long, packageId: String
) = ExerciseDefinitionEntity(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    name = PackageFieldValidator.validateString(name, "name"),
    category = PackageFieldValidator.validateString(category, "category"),
    muscleGroup = PackageFieldValidator.validateString(muscleGroup, "muscleGroup"),
    notes = PackageFieldValidator.validateString(notes, "notes")
)

private fun ExportWorkoutTemplate.toEntity(
    epochMillis: Long, packageId: String
) = WorkoutEntity(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    name = PackageFieldValidator.validateString(name, "name"),
    date = PackageFieldValidator.validateString(date, "date"),
    durationMinutes = durationMinutes?.let { PackageFieldValidator.validateInt(it, "durationMinutes", 0, 1440) },
    notes = PackageFieldValidator.validateString(notes, "notes")
)

private fun ExportTemplateExercise.toEntity(
    epochMillis: Long, packageId: String
) = WorkoutTemplateExercise(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    workoutId = PackageFieldValidator.validateUuid(workoutId),
    exerciseName = PackageFieldValidator.validateString(exerciseName, "exerciseName"),
    orderIndex = PackageFieldValidator.validateInt(orderIndex, "orderIndex", 0, 999)
)

private fun ExportFoodItem.toEntity(
    epochMillis: Long, packageId: String
) = FoodItemEntity(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    name = PackageFieldValidator.validateString(name, "name"),
    kcalPer100g = PackageFieldValidator.validateDouble(kcalPer100g, "kcalPer100g", 0.0, 10_000.0),
    proteinPer100g = PackageFieldValidator.validateDouble(proteinPer100g, "proteinPer100g", 0.0, 1_000.0),
    carbsPer100g = PackageFieldValidator.validateDouble(carbsPer100g, "carbsPer100g", 0.0, 1_000.0),
    fatPer100g = PackageFieldValidator.validateDouble(fatPer100g, "fatPer100g", 0.0, 1_000.0),
    sugarPer100g = PackageFieldValidator.validateDouble(sugarPer100g, "sugarPer100g", 0.0, 1_000.0),
    barcode = barcode?.let { PackageFieldValidator.validateString(it, "barcode") }
)

private fun ExportSupplement.toEntity(
    epochMillis: Long, packageId: String
) = SupplementEntity(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    name = PackageFieldValidator.validateString(name, "name"),
    dosage = PackageFieldValidator.validateString(dosage, "dosage"),
    frequency = PackageFieldValidator.validateString(frequency, "frequency"),
    timesPerDay = PackageFieldValidator.validateInt(timesPerDay, "timesPerDay", 0, 100),
    timeOfDay = PackageFieldValidator.validateString(timeOfDay, "timeOfDay"),
    durationDays = durationDays?.let { PackageFieldValidator.validateInt(it, "durationDays", 0, 3650) },
    notes = notes?.let { PackageFieldValidator.validateString(it, "notes") }
)

private fun ExportSupplementIngredient.toEntity(
    epochMillis: Long, packageId: String
) = SupplementIngredientEntity(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    supplementId = PackageFieldValidator.validateUuid(supplementId),
    name = PackageFieldValidator.validateString(name, "name"),
    amount = PackageFieldValidator.validateDouble(amount, "amount", 0.0, 1_000_000.0),
    unit = PackageFieldValidator.validateString(unit, "unit"),
    rvsPct = rvsPct?.let { PackageFieldValidator.validateDouble(it, "rvsPct", 0.0, 100_000.0) }
)

private fun ExportHabit.toEntity(
    epochMillis: Long, packageId: String
) = HabitEntity(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    name = PackageFieldValidator.validateString(name, "name"),
    emoji = PackageFieldValidator.validateString(emoji, "emoji"),
    frequency = PackageFieldValidator.validateString(frequency, "frequency"),
    repeatIntervalDays = PackageFieldValidator.validateInt(repeatIntervalDays, "repeatIntervalDays", 0, 3650),
    timeOfDay = PackageFieldValidator.validateString(timeOfDay, "timeOfDay"),
    isPositive = isPositive
)

private fun ExportRecipe.toEntity(
    epochMillis: Long, packageId: String
) = RecipeEntity(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    title = PackageFieldValidator.validateString(title, "title"),
    instructions = PackageFieldValidator.validateString(instructions, "instructions"),
    category = try { RecipeCategory.valueOf(PackageFieldValidator.validateString(category, "category")) } catch (e: Exception) { Timber.w(e, "[ImportPackageUseCase] Unknown RecipeCategory '%s', defaulting to OTHER", category); RecipeCategory.OTHER },
    servings = PackageFieldValidator.validateInt(servings, "servings", 0, 1_000),
    prepTimeMinutes = prepTimeMinutes?.let { PackageFieldValidator.validateInt(it, "prepTimeMinutes", 0, 1440) },
    cookTimeMinutes = cookTimeMinutes?.let { PackageFieldValidator.validateInt(it, "cookTimeMinutes", 0, 1440) },
    notes = notes?.let { PackageFieldValidator.validateString(it, "notes") }
)

private fun ExportRecipeIngredient.toEntity(
    epochMillis: Long, packageId: String
) = RecipeIngredientEntity(
    uuid = PackageFieldValidator.validateUuid(uuid),
    createdAt = epochMillis,
    updatedAt = epochMillis,
    importSource = ImportSource.COMMUNITY_HUB,
    importedFromPackageId = packageId,
    recipeId = PackageFieldValidator.validateUuid(recipeId),
    name = PackageFieldValidator.validateString(name, "name"),
    quantity = PackageFieldValidator.validateDouble(quantity, "quantity", 0.0, 1_000_000.0),
    unit = PackageFieldValidator.validateString(unit, "unit"),
    orderIndex = PackageFieldValidator.validateInt(orderIndex, "orderIndex", 0, 999),
    foodItemUuid = foodItemUuid?.let { PackageFieldValidator.validateUuid(it) }
)
