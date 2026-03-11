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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All Room schema migrations.
 *
 * Add a new object here every time [LifeModuleDatabase.version] increments.
 * Register each migration in [de.lifemodule.app.di.DatabaseModule].
 */

/**
 * v12 -> v13: Gym redesign.
 * Adds five new tables:
 *   workouts                  - workout templates
 *   workout_template_exercises - exercise order within a template
 *   exercise_definitions      - exercise library (name + image + muscle group)
 *   gym_sessions              - performed training sessions
 *   session_sets              - individual sets logged during a session
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // ── workout templates ─────────────────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workouts` (
                `id`               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name`             TEXT    NOT NULL,
                `date`             TEXT    NOT NULL,
                `durationMinutes`  INTEGER,
                `notes`            TEXT    NOT NULL DEFAULT '',
                `createdAtMillis`  INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // ── exercises within a template (order only) ──────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workout_template_exercises` (
                `id`            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `workoutId`     INTEGER NOT NULL,
                `exerciseName`  TEXT    NOT NULL,
                `orderIndex`    INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (`workoutId`) REFERENCES `workouts`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_workout_template_exercises_workoutId` " +
                "ON `workout_template_exercises` (`workoutId`)"
        )

        // ── exercise library ──────────────────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_definitions` (
                `id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name`        TEXT    NOT NULL,
                `category`    TEXT    NOT NULL DEFAULT 'strength',
                `muscleGroup` TEXT    NOT NULL DEFAULT '',
                `imagePath`   TEXT,
                `notes`       TEXT    NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )

        // ── actual training sessions ──────────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_sessions` (
                `id`                 INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date`               TEXT    NOT NULL,
                `name`               TEXT    NOT NULL,
                `type`               TEXT    NOT NULL DEFAULT 'strength',
                `durationMinutes`    INTEGER,
                `workoutTemplateId`  INTEGER,
                `notes`              TEXT    NOT NULL DEFAULT '',
                `createdAtMillis`    INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // ── sets performed within a session ───────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session_sets` (
                `id`                 INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId`          INTEGER NOT NULL,
                `exerciseName`       TEXT    NOT NULL,
                `exerciseOrderIndex` INTEGER NOT NULL DEFAULT 0,
                `setNumber`          INTEGER NOT NULL DEFAULT 1,
                `reps`               INTEGER,
                `weightKg`           REAL,
                `durationSeconds`    INTEGER,
                `createdAtMillis`    INTEGER NOT NULL,
                FOREIGN KEY (`sessionId`) REFERENCES `gym_sessions`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_session_sets_sessionId` " +
                "ON `session_sets` (`sessionId`)"
        )
    }
}

/**
 * v13 -> v14: NagerDate holiday Room cache.
 * Adds `holiday_cache` table so holidays are available offline.
 * Composite PK (year, date) - one row per holiday per year.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `holiday_cache` (
                `year`            INTEGER NOT NULL,
                `date`            TEXT    NOT NULL,
                `localName`       TEXT    NOT NULL,
                `isGlobal`        INTEGER NOT NULL,
                `counties`        TEXT    NOT NULL,
                `fetchedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`year`, `date`)
            )
            """.trimIndent()
        )
    }
}

/**
 * v14 -> v15: Add `isRecurring` column to calendar_events.
 * Enables yearly recurring events (e.g. birthdays).
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE calendar_events ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * v15 -> v16: Drop holiday_cache table.
 * Holidays are now loaded from bundled JSON assets - no more Room cache needed.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `holiday_cache`")
    }
}

/**
 * v16 -> v17: Add missing indices for query performance.
 * Adds date/column indices across all major tables.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // activity_log
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_log_date` ON `activity_log` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_log_module` ON `activity_log` (`module`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_log_timestamp` ON `activity_log` (`timestamp`)")

        // calendar_events
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_date` ON `calendar_events` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_isRecurring` ON `calendar_events` (`isRecurring`)")

        // mood_entries
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mood_entries_date` ON `mood_entries` (`date`)")

        // gym_sessions
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_sessions_date` ON `gym_sessions` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_sessions_workoutTemplateId` ON `gym_sessions` (`workoutTemplateId`)")

        // weight_entries
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_weight_entries_date` ON `weight_entries` (`date`)")

        // work_time_entries
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_time_entries_date` ON `work_time_entries` (`date`)")

        // courses
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_dayOfWeek` ON `courses` (`dayOfWeek`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_semester` ON `courses` (`semester`)")

        // food_items
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_barcode` ON `food_items` (`barcode`)")

        // daily_food_entries (date index - foodItemId index already exists)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_food_entries_date` ON `daily_food_entries` (`date`)")

        // shopping_items
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_checked` ON `shopping_items` (`checked`)")

        // exercise_definitions
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_definitions_category` ON `exercise_definitions` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_definitions_name` ON `exercise_definitions` (`name`)")
    }
}

/**
 * v17 -> v18: Add `isActive` column to `food_items`.
 * Enables soft-deletion (archiving) of food items so historical macros are preserved in daily logs.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE food_items ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1"
        )
    }
}

/**
 * v18 -> v19: Add error_logs table.
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `error_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `module` TEXT NOT NULL,
                `message` TEXT NOT NULL,
                `stackTrace` TEXT
            )
            """.trimIndent()
        )
    }
}

/**
 * v19 -> v20: Add severity column to error_logs.
 */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE error_logs ADD COLUMN severity TEXT NOT NULL DEFAULT 'ERROR'"
        )
    }
}

/**
 * v20 -> v21: UUID migration - all 18 exportable entities get TEXT primary keys and
 * BaseEntity columns (created_at, updated_at, import_source, imported_from_package_id).
 *
 * Strategy: each table is renamed to _bak, recreated with the new schema, then
 * repopulated with deterministic UUIDs: '00000000-0000-4000-8000-' || printf('%012d', old_id)
 * This keeps FK integrity without any JOINs.
 *
 * Entities NOT migrated (keep Long autoincrement): activity_logs, error_logs.
 */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = OFF")

        // ─── 1. food_items ────────────────────────────────────────────────────
        db.execSQL("ALTER TABLE food_items RENAME TO food_items_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `food_items` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `name` TEXT NOT NULL,
            `kcalPer100g` REAL NOT NULL,
            `proteinPer100g` REAL NOT NULL,
            `carbsPer100g` REAL NOT NULL,
            `fatPer100g` REAL NOT NULL,
            `sugarPer100g` REAL NOT NULL,
            `barcode` TEXT,
            `imagePath` TEXT,
            `isActive` INTEGER NOT NULL DEFAULT 1
        )""".trimIndent())
        db.execSQL("""INSERT INTO food_items
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             name,kcalPer100g,proteinPer100g,carbsPer100g,fatPer100g,sugarPer100g,
             barcode,imagePath,isActive)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              name,kcalPer100g,proteinPer100g,carbsPer100g,fatPer100g,sugarPer100g,
              barcode,imagePath,isActive
            FROM food_items_bak""".trimIndent())
        db.execSQL("DROP TABLE food_items_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_barcode` ON `food_items` (`barcode`)")

        // ─── 2. daily_food_entries (child of food_items) ─────────────────────
        db.execSQL("ALTER TABLE daily_food_entries RENAME TO daily_food_entries_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `daily_food_entries` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `foodItemId` TEXT NOT NULL,
            `date` TEXT NOT NULL,
            `gramsConsumed` REAL NOT NULL,
            FOREIGN KEY(`foodItemId`) REFERENCES `food_items`(`uuid`) ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("""INSERT INTO daily_food_entries
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             foodItemId,date,gramsConsumed)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              '00000000-0000-4000-8000-'||printf('%012d',foodItemId),
              date,gramsConsumed
            FROM daily_food_entries_bak""".trimIndent())
        db.execSQL("DROP TABLE daily_food_entries_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_food_entries_foodItemId` ON `daily_food_entries` (`foodItemId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_food_entries_date` ON `daily_food_entries` (`date`)")

        // ─── 3. supplements ──────────────────────────────────────────────────
        db.execSQL("ALTER TABLE supplements RENAME TO supplements_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `supplements` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `name` TEXT NOT NULL,
            `dosage` TEXT NOT NULL,
            `frequency` TEXT NOT NULL DEFAULT 'daily',
            `timesPerDay` INTEGER NOT NULL DEFAULT 1,
            `timeOfDay` TEXT NOT NULL DEFAULT 'morning',
            `durationDays` INTEGER,
            `startDate` TEXT,
            `notes` TEXT,
            `imagePath` TEXT,
            `isActive` INTEGER NOT NULL DEFAULT 1
        )""".trimIndent())
        db.execSQL("""INSERT INTO supplements
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             name,dosage,frequency,timesPerDay,timeOfDay,durationDays,startDate,notes,imagePath,isActive)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              name,dosage,frequency,timesPerDay,timeOfDay,durationDays,startDate,notes,imagePath,isActive
            FROM supplements_bak""".trimIndent())
        db.execSQL("DROP TABLE supplements_bak")

        // ─── 4. supplement_logs (child of supplements) ───────────────────────
        db.execSQL("ALTER TABLE supplement_logs RENAME TO supplement_logs_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `supplement_logs` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `supplementId` TEXT NOT NULL,
            `date` TEXT NOT NULL,
            `taken` INTEGER NOT NULL DEFAULT 0,
            `takenAtMillis` INTEGER,
            FOREIGN KEY(`supplementId`) REFERENCES `supplements`(`uuid`) ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("""INSERT INTO supplement_logs
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             supplementId,date,taken,takenAtMillis)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              '00000000-0000-4000-8000-'||printf('%012d',supplementId),
              date,taken,takenAtMillis
            FROM supplement_logs_bak""".trimIndent())
        db.execSQL("DROP TABLE supplement_logs_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplement_logs_supplementId` ON `supplement_logs` (`supplementId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_supplement_logs_supplementId_date` ON `supplement_logs` (`supplementId`,`date`)")

        // ─── 5. supplement_ingredients (child of supplements) ────────────────
        db.execSQL("ALTER TABLE supplement_ingredients RENAME TO supplement_ingredients_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `supplement_ingredients` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `supplementId` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `amount` REAL NOT NULL,
            `unit` TEXT NOT NULL,
            `rvsPct` REAL,
            FOREIGN KEY(`supplementId`) REFERENCES `supplements`(`uuid`) ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("""INSERT INTO supplement_ingredients
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             supplementId,name,amount,unit,rvsPct)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              '00000000-0000-4000-8000-'||printf('%012d',supplementId),
              name,amount,unit,rvsPct
            FROM supplement_ingredients_bak""".trimIndent())
        db.execSQL("DROP TABLE supplement_ingredients_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplement_ingredients_supplementId` ON `supplement_ingredients` (`supplementId`)")

        // ─── 6. habits (had createdAtMillis) ─────────────────────────────────
        db.execSQL("ALTER TABLE habits RENAME TO habits_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `habits` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `name` TEXT NOT NULL,
            `emoji` TEXT NOT NULL DEFAULT '✅',
            `frequency` TEXT NOT NULL DEFAULT 'daily',
            `repeatIntervalDays` INTEGER NOT NULL DEFAULT 1,
            `timeOfDay` TEXT NOT NULL DEFAULT 'any',
            `isPositive` INTEGER NOT NULL DEFAULT 1,
            `imagePath` TEXT,
            `isActive` INTEGER NOT NULL DEFAULT 1
        )""".trimIndent())
        db.execSQL("""INSERT INTO habits
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             name,emoji,frequency,repeatIntervalDays,timeOfDay,isPositive,imagePath,isActive)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              COALESCE(createdAtMillis,0),COALESCE(createdAtMillis,0),'USER',NULL,
              name,emoji,frequency,repeatIntervalDays,timeOfDay,isPositive,imagePath,isActive
            FROM habits_bak""".trimIndent())
        db.execSQL("DROP TABLE habits_bak")

        // ─── 7. habit_logs (child of habits) ─────────────────────────────────
        db.execSQL("ALTER TABLE habit_logs RENAME TO habit_logs_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `habit_logs` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `habitId` TEXT NOT NULL,
            `date` TEXT NOT NULL,
            `completed` INTEGER NOT NULL DEFAULT 0,
            `completedAtMillis` INTEGER,
            FOREIGN KEY(`habitId`) REFERENCES `habits`(`uuid`) ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("""INSERT INTO habit_logs
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             habitId,date,completed,completedAtMillis)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              COALESCE(completedAtMillis,0),COALESCE(completedAtMillis,0),'USER',NULL,
              '00000000-0000-4000-8000-'||printf('%012d',habitId),
              date,completed,completedAtMillis
            FROM habit_logs_bak""".trimIndent())
        db.execSQL("DROP TABLE habit_logs_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_logs_habitId` ON `habit_logs` (`habitId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitId_date` ON `habit_logs` (`habitId`,`date`)")

        // ─── 8. mood_entries (had createdAtMillis) ───────────────────────────
        db.execSQL("ALTER TABLE mood_entries RENAME TO mood_entries_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `mood_entries` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `date` TEXT NOT NULL,
            `moodLevel` INTEGER NOT NULL DEFAULT 5,
            `energyLevel` INTEGER NOT NULL DEFAULT 5,
            `stressLevel` INTEGER NOT NULL DEFAULT 5,
            `sleepQuality` INTEGER NOT NULL DEFAULT 5,
            `positiveNotes` TEXT NOT NULL DEFAULT '',
            `negativeNotes` TEXT NOT NULL DEFAULT ''
        )""".trimIndent())
        db.execSQL("""INSERT INTO mood_entries
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             date,moodLevel,energyLevel,stressLevel,sleepQuality,positiveNotes,negativeNotes)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              COALESCE(createdAtMillis,0),COALESCE(createdAtMillis,0),'USER',NULL,
              date,moodLevel,energyLevel,stressLevel,sleepQuality,positiveNotes,negativeNotes
            FROM mood_entries_bak""".trimIndent())
        db.execSQL("DROP TABLE mood_entries_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mood_entries_date` ON `mood_entries` (`date`)")

        // ─── 9. courses ───────────────────────────────────────────────────────
        db.execSQL("ALTER TABLE courses RENAME TO courses_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `courses` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `name` TEXT NOT NULL,
            `professor` TEXT NOT NULL DEFAULT '',
            `room` TEXT NOT NULL DEFAULT '',
            `dayOfWeek` INTEGER NOT NULL DEFAULT 0,
            `startTime` TEXT NOT NULL DEFAULT '08:00',
            `endTime` TEXT NOT NULL DEFAULT '09:30',
            `color` TEXT NOT NULL DEFAULT '#30D158',
            `semester` TEXT NOT NULL DEFAULT '',
            `weekType` TEXT NOT NULL DEFAULT 'every',
            `courseType` TEXT NOT NULL DEFAULT 'vorlesung',
            `isActive` INTEGER NOT NULL DEFAULT 1
        )""".trimIndent())
        db.execSQL("""INSERT INTO courses
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             name,professor,room,dayOfWeek,startTime,endTime,color,semester,weekType,courseType,isActive)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              name,professor,room,dayOfWeek,startTime,endTime,color,semester,weekType,courseType,isActive
            FROM courses_bak""".trimIndent())
        db.execSQL("DROP TABLE courses_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_dayOfWeek` ON `courses` (`dayOfWeek`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_semester` ON `courses` (`semester`)")

        // ─── 10. calendar_events (had createdAtMillis) ───────────────────────
        db.execSQL("ALTER TABLE calendar_events RENAME TO calendar_events_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `calendar_events` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `title` TEXT NOT NULL,
            `description` TEXT NOT NULL DEFAULT '',
            `date` TEXT NOT NULL,
            `endDate` TEXT,
            `time` TEXT,
            `endTime` TEXT,
            `category` TEXT NOT NULL DEFAULT 'other',
            `color` TEXT NOT NULL DEFAULT '#30D158',
            `isHoliday` INTEGER NOT NULL DEFAULT 0,
            `isRecurring` INTEGER NOT NULL DEFAULT 0
        )""".trimIndent())
        db.execSQL("""INSERT INTO calendar_events
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             title,description,date,endDate,time,endTime,category,color,isHoliday,isRecurring)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              COALESCE(createdAtMillis,0),COALESCE(createdAtMillis,0),'USER',NULL,
              title,description,date,endDate,time,endTime,category,color,isHoliday,isRecurring
            FROM calendar_events_bak""".trimIndent())
        db.execSQL("DROP TABLE calendar_events_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_date` ON `calendar_events` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_isRecurring` ON `calendar_events` (`isRecurring`)")

        // ─── 11. workouts (had createdAtMillis) ──────────────────────────────
        db.execSQL("ALTER TABLE workouts RENAME TO workouts_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `workouts` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `name` TEXT NOT NULL,
            `date` TEXT NOT NULL,
            `durationMinutes` INTEGER,
            `notes` TEXT NOT NULL DEFAULT ''
        )""".trimIndent())
        db.execSQL("""INSERT INTO workouts
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             name,date,durationMinutes,notes)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              COALESCE(createdAtMillis,0),COALESCE(createdAtMillis,0),'USER',NULL,
              name,date,durationMinutes,notes
            FROM workouts_bak""".trimIndent())
        db.execSQL("DROP TABLE workouts_bak")

        // ─── 12. workout_template_exercises (child of workouts) ──────────────
        db.execSQL("ALTER TABLE workout_template_exercises RENAME TO workout_template_exercises_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `workout_template_exercises` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `workoutId` TEXT NOT NULL,
            `exerciseName` TEXT NOT NULL,
            `orderIndex` INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`uuid`) ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("""INSERT INTO workout_template_exercises
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             workoutId,exerciseName,orderIndex)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              '00000000-0000-4000-8000-'||printf('%012d',workoutId),
              exerciseName,orderIndex
            FROM workout_template_exercises_bak""".trimIndent())
        db.execSQL("DROP TABLE workout_template_exercises_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercises_workoutId` ON `workout_template_exercises` (`workoutId`)")

        // ─── 13. exercise_definitions ─────────────────────────────────────────
        db.execSQL("ALTER TABLE exercise_definitions RENAME TO exercise_definitions_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `exercise_definitions` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `name` TEXT NOT NULL,
            `category` TEXT NOT NULL DEFAULT 'strength',
            `muscleGroup` TEXT NOT NULL DEFAULT '',
            `imagePath` TEXT,
            `notes` TEXT NOT NULL DEFAULT ''
        )""".trimIndent())
        db.execSQL("""INSERT INTO exercise_definitions
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             name,category,muscleGroup,imagePath,notes)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              name,category,muscleGroup,imagePath,notes
            FROM exercise_definitions_bak""".trimIndent())
        db.execSQL("DROP TABLE exercise_definitions_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_definitions_category` ON `exercise_definitions` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_definitions_name` ON `exercise_definitions` (`name`)")

        // ─── 14. gym_sessions (had createdAtMillis; workoutTemplateId nullable) ─
        db.execSQL("ALTER TABLE gym_sessions RENAME TO gym_sessions_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `gym_sessions` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `date` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `type` TEXT NOT NULL DEFAULT 'strength',
            `durationMinutes` INTEGER,
            `workoutTemplateId` TEXT,
            `notes` TEXT NOT NULL DEFAULT ''
        )""".trimIndent())
        db.execSQL("""INSERT INTO gym_sessions
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             date,name,type,durationMinutes,workoutTemplateId,notes)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              COALESCE(createdAtMillis,0),COALESCE(createdAtMillis,0),'USER',NULL,
              date,name,type,durationMinutes,
              CASE WHEN workoutTemplateId IS NULL THEN NULL
                   ELSE '00000000-0000-4000-8000-'||printf('%012d',workoutTemplateId) END,
              notes
            FROM gym_sessions_bak""".trimIndent())
        db.execSQL("DROP TABLE gym_sessions_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_sessions_date` ON `gym_sessions` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_sessions_workoutTemplateId` ON `gym_sessions` (`workoutTemplateId`)")

        // ─── 15. session_sets (child of gym_sessions; had createdAtMillis) ───
        db.execSQL("ALTER TABLE session_sets RENAME TO session_sets_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `session_sets` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `sessionId` TEXT NOT NULL,
            `exerciseName` TEXT NOT NULL,
            `exerciseOrderIndex` INTEGER NOT NULL DEFAULT 0,
            `setNumber` INTEGER NOT NULL DEFAULT 1,
            `reps` INTEGER,
            `weightKg` REAL,
            `durationSeconds` INTEGER,
            FOREIGN KEY(`sessionId`) REFERENCES `gym_sessions`(`uuid`) ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("""INSERT INTO session_sets
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             sessionId,exerciseName,exerciseOrderIndex,setNumber,reps,weightKg,durationSeconds)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              COALESCE(createdAtMillis,0),COALESCE(createdAtMillis,0),'USER',NULL,
              '00000000-0000-4000-8000-'||printf('%012d',sessionId),
              exerciseName,exerciseOrderIndex,setNumber,reps,weightKg,durationSeconds
            FROM session_sets_bak""".trimIndent())
        db.execSQL("DROP TABLE session_sets_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_sets_sessionId` ON `session_sets` (`sessionId`)")

        // ─── 16. weight_entries ───────────────────────────────────────────────
        db.execSQL("ALTER TABLE weight_entries RENAME TO weight_entries_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `weight_entries` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `date` TEXT NOT NULL,
            `weightKg` REAL NOT NULL,
            `timeOfDay` TEXT NOT NULL DEFAULT 'MORNING',
            `afterWaking` INTEGER NOT NULL DEFAULT 0,
            `notes` TEXT NOT NULL DEFAULT ''
        )""".trimIndent())
        db.execSQL("""INSERT INTO weight_entries
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             date,weightKg,timeOfDay,afterWaking,notes)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              date,weightKg,timeOfDay,afterWaking,notes
            FROM weight_entries_bak""".trimIndent())
        db.execSQL("DROP TABLE weight_entries_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_weight_entries_date` ON `weight_entries` (`date`)")

        // ─── 17. shopping_items (had createdAtMillis) ────────────────────────
        db.execSQL("ALTER TABLE shopping_items RENAME TO shopping_items_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `shopping_items` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `name` TEXT NOT NULL,
            `quantity` TEXT NOT NULL DEFAULT '',
            `category` TEXT NOT NULL DEFAULT 'Sonstiges',
            `checked` INTEGER NOT NULL DEFAULT 0
        )""".trimIndent())
        db.execSQL("""INSERT INTO shopping_items
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             name,quantity,category,checked)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              COALESCE(createdAtMillis,0),COALESCE(createdAtMillis,0),'USER',NULL,
              name,quantity,category,checked
            FROM shopping_items_bak""".trimIndent())
        db.execSQL("DROP TABLE shopping_items_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_checked` ON `shopping_items` (`checked`)")

        // ─── 18. work_time_entries (clockInMillis is domain field, keep it) ──
        db.execSQL("ALTER TABLE work_time_entries RENAME TO work_time_entries_bak")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `work_time_entries` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `date` TEXT NOT NULL,
            `clockInMillis` INTEGER NOT NULL,
            `clockOutMillis` INTEGER,
            `breakMinutes` INTEGER NOT NULL DEFAULT 0,
            `notes` TEXT NOT NULL DEFAULT '',
            `projectName` TEXT NOT NULL DEFAULT ''
        )""".trimIndent())
        db.execSQL("""INSERT INTO work_time_entries
            (uuid,created_at,updated_at,import_source,imported_from_package_id,
             date,clockInMillis,clockOutMillis,breakMinutes,notes,projectName)
            SELECT '00000000-0000-4000-8000-'||printf('%012d',id),
              0,0,'USER',NULL,
              date,clockInMillis,clockOutMillis,breakMinutes,notes,projectName
            FROM work_time_entries_bak""".trimIndent())
        db.execSQL("DROP TABLE work_time_entries_bak")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_time_entries_date` ON `work_time_entries` (`date`)")

        db.execSQL("PRAGMA foreign_keys = ON")
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// v22: Logbook (GoBD), Receipt Scanner (GoBD), Recipes
// ═══════════════════════════════════════════════════════════════════════════

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // ─── 1. logbook_vehicles ──────────────────────────────────────────────
        db.execSQL("""CREATE TABLE IF NOT EXISTS `logbook_vehicles` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `name` TEXT NOT NULL,
            `license_plate` TEXT,
            `initial_odometer_km` REAL NOT NULL DEFAULT 0.0
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_logbook_vehicles_uuid` ON `logbook_vehicles` (`uuid`)")

        // ─── 2. logbook_entries (GoBD write-once, hash chain) ─────────────────
        db.execSQL("""CREATE TABLE IF NOT EXISTS `logbook_entries` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `journey_date` INTEGER NOT NULL,
            `start_location` TEXT NOT NULL,
            `end_location` TEXT NOT NULL,
            `distance_km` REAL NOT NULL,
            `purpose_code` TEXT NOT NULL,
            `vehicle_id` TEXT NOT NULL,
            `notes` TEXT,
            `is_finalized` INTEGER NOT NULL DEFAULT 0,
            `correction_of_uuid` TEXT,
            `entry_hash` TEXT NOT NULL,
            `previous_hash` TEXT NOT NULL
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_logbook_entries_uuid` ON `logbook_entries` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_logbook_entries_vehicle_id` ON `logbook_entries` (`vehicle_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_logbook_entries_journey_date` ON `logbook_entries` (`journey_date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_logbook_entries_correction_of_uuid` ON `logbook_entries` (`correction_of_uuid`)")

        // ─── 3. receipt_records (GoBD, finalization hash) ─────────────────────
        db.execSQL("""CREATE TABLE IF NOT EXISTS `receipt_records` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `captured_at` INTEGER NOT NULL,
            `vendor` TEXT NOT NULL,
            `receipt_date` INTEGER NOT NULL,
            `total_amount` REAL NOT NULL,
            `vat_amount` REAL,
            `currency` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `notes` TEXT,
            `image_path` TEXT NOT NULL,
            `image_sha256` TEXT NOT NULL,
            `is_finalized` INTEGER NOT NULL DEFAULT 0,
            `finalization_hash` TEXT,
            `correction_of_uuid` TEXT
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_records_uuid` ON `receipt_records` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_records_receipt_date` ON `receipt_records` (`receipt_date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_records_category` ON `receipt_records` (`category`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_records_correction_of_uuid` ON `receipt_records` (`correction_of_uuid`)")

        // ─── 4. recipes ──────────────────────────────────────────────────────
        db.execSQL("""CREATE TABLE IF NOT EXISTS `recipes` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `title` TEXT NOT NULL,
            `instructions` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `servings` INTEGER NOT NULL DEFAULT 1,
            `prep_time_minutes` INTEGER,
            `cook_time_minutes` INTEGER,
            `image_path` TEXT,
            `notes` TEXT
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipes_uuid` ON `recipes` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipes_category` ON `recipes` (`category`)")

        // ─── 5. recipe_ingredients ───────────────────────────────────────────
        db.execSQL("""CREATE TABLE IF NOT EXISTS `recipe_ingredients` (
            `uuid` TEXT NOT NULL PRIMARY KEY,
            `created_at` INTEGER NOT NULL DEFAULT 0,
            `updated_at` INTEGER NOT NULL DEFAULT 0,
            `import_source` TEXT NOT NULL DEFAULT 'USER',
            `imported_from_package_id` TEXT,
            `recipe_id` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `quantity` REAL NOT NULL,
            `unit` TEXT NOT NULL,
            `order_index` INTEGER NOT NULL DEFAULT 0,
            `food_item_uuid` TEXT
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_uuid` ON `recipe_ingredients` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipe_id` ON `recipe_ingredients` (`recipe_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_food_item_uuid` ON `recipe_ingredients` (`food_item_uuid`)")
    }
}
