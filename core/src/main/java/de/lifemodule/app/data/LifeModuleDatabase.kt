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

package de.lifemodule.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.lifemodule.app.data.analytics.ActivityLogDao
import de.lifemodule.app.data.analytics.ActivityLogEntity
import de.lifemodule.app.data.error.ErrorLogDao
import de.lifemodule.app.data.error.ErrorLogEntity
import de.lifemodule.app.data.calendar.CalendarDao
import de.lifemodule.app.data.logbook.LogbookDao
import de.lifemodule.app.data.logbook.LogbookEntryEntity
import de.lifemodule.app.data.logbook.VehicleEntity
import de.lifemodule.app.data.scanner.ReceiptDao
import de.lifemodule.app.data.scanner.ReceiptRecordEntity
import de.lifemodule.app.data.recipes.RecipeDao
import de.lifemodule.app.data.recipes.RecipeEntity
import de.lifemodule.app.data.recipes.RecipeIngredientEntity
import de.lifemodule.app.data.calendar.CalendarEventEntity
import de.lifemodule.app.data.gym.ExerciseDefinitionEntity
import de.lifemodule.app.data.gym.GymDao
import de.lifemodule.app.data.gym.GymSessionEntity
import de.lifemodule.app.data.gym.SessionSetEntity
import de.lifemodule.app.data.gym.WorkoutEntity
import de.lifemodule.app.data.gym.WorkoutTemplateExercise
import de.lifemodule.app.data.habits.HabitDao
import de.lifemodule.app.data.habits.HabitEntity
import de.lifemodule.app.data.habits.HabitLogEntity
import de.lifemodule.app.data.mentalhealth.MoodDao
import de.lifemodule.app.data.mentalhealth.MoodEntryEntity
import de.lifemodule.app.data.nutrition.DailyFoodEntryEntity
import de.lifemodule.app.data.nutrition.FoodDao
import de.lifemodule.app.data.nutrition.FoodItemEntity
import de.lifemodule.app.data.schedule.CourseDao
import de.lifemodule.app.data.schedule.CourseEntity
import de.lifemodule.app.data.shopping.ShoppingDao
import de.lifemodule.app.data.shopping.ShoppingItemEntity
import de.lifemodule.app.data.supplements.SupplementDao
import de.lifemodule.app.data.supplements.SupplementEntity
import de.lifemodule.app.data.supplements.SupplementIngredientEntity
import de.lifemodule.app.data.supplements.SupplementLogEntity
import de.lifemodule.app.data.weight.WeightDao
import de.lifemodule.app.data.weight.WeightEntryEntity
import de.lifemodule.app.data.worktime.WorkTimeDao
import de.lifemodule.app.data.worktime.WorkTimeEntryEntity

@TypeConverters(RoomConverters::class)
/**
 * Central Room database for all user data.
 *
 * ### Migration Discipline (v1.x lifecycle)
 *
 * **ALLOWED:**
 * - `ALTER TABLE <name> ADD COLUMN <col> <type> DEFAULT <safe_default>`
 *   (nullable or with a sensible default)
 * - `CREATE TABLE IF NOT EXISTS <name> (...)`
 * - `CREATE INDEX IF NOT EXISTS ...`
 *
 * **FORBIDDEN:**
 * - `ALTER TABLE ... DROP COLUMN`
 * - `ALTER TABLE ... RENAME COLUMN`
 * - `DROP TABLE`
 * - Any statement that loses or corrupts existing user data.
 *
 * All migrations are tested via exported schema JSON in `app/schemas/`.
 * The export-format version used by ZIP packages is tracked separately
 * in [de.lifemodule.app.data.packages.ImportMigrationManager.CURRENT_PACKAGE_SCHEMA_VERSION].
 */
@Database(
    entities = [
        FoodItemEntity::class,
        DailyFoodEntryEntity::class,
        SupplementEntity::class,
        SupplementLogEntity::class,
        SupplementIngredientEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        MoodEntryEntity::class,
        CourseEntity::class,
        CalendarEventEntity::class,
        // ── Gym (v13 redesign) ──────────────────────────────────────────
        WorkoutEntity::class,                // workout templates
        WorkoutTemplateExercise::class,      // exercises within a template (order only)
        ExerciseDefinitionEntity::class,     // exercise library with images
        GymSessionEntity::class,             // actual training sessions
        SessionSetEntity::class,             // performed sets within a session
        // ── Other modules ───────────────────────────────────────────────
        ActivityLogEntity::class,
        WeightEntryEntity::class,
        ShoppingItemEntity::class,
        WorkTimeEntryEntity::class,
        ErrorLogEntity::class,
        // ── Logbook (GoBD, v22) ─────────────────────────────────────────
        LogbookEntryEntity::class,
        VehicleEntity::class,
        // ── Scanner (GoBD, v22) ─────────────────────────────────────────
        ReceiptRecordEntity::class,
        // ── Recipes (v22) ───────────────────────────────────────────────
        RecipeEntity::class,
        RecipeIngredientEntity::class,
    ],
    version = 22,
    exportSchema = true   // schema JSON written to app/schemas/ for migration auditing
)
abstract class LifeModuleDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun supplementDao(): SupplementDao
    abstract fun habitDao(): HabitDao
    abstract fun moodDao(): MoodDao
    abstract fun courseDao(): CourseDao
    abstract fun calendarDao(): CalendarDao
    abstract fun gymDao(): GymDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun weightDao(): WeightDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun workTimeDao(): WorkTimeDao
    abstract fun errorLogDao(): ErrorLogDao
    abstract fun logbookDao(): LogbookDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun recipeDao(): RecipeDao
}
