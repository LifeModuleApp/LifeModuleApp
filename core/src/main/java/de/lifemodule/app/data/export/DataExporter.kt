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

package de.lifemodule.app.data.export

import android.content.Context
import android.net.Uri
import de.lifemodule.app.core.R
import de.lifemodule.app.data.LifeModuleDatabase
import de.lifemodule.app.data.error.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import de.lifemodule.app.util.time.TimeProvider
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports all app data as a ZIP of individual CSV files (one per module).
 * Each CSV uses semicolons as delimiter for German Excel compatibility.
 */
@Singleton
class DataExporter @Inject constructor(
    private val db: LifeModuleDatabase,
    private val timeProvider: TimeProvider,
    private val errorLogger: ErrorLogger
) {
    /**
     * Exports all app data as a ZIP of individual CSV files to the provided Uri (SAF).
     */
    suspend fun exportAllAsCsv(context: Context, targetUri: Uri): Int = withContext(Dispatchers.IO) {
        val today = timeProvider.today().format(DateTimeFormatter.ISO_LOCAL_DATE)

        try {
            var allSuccess = true
            var anySuccess = false
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    fun runExport(name: String, block: (java.io.BufferedWriter) -> Unit) {
                        val success = streamCsv(zip, name, block)
                        allSuccess = allSuccess && success
                        anySuccess = anySuccess || success
                    }
                    // Note: If you add new tables to the Room DB (e.g. Table 21),
                    // you must manually add them to this export script to ensure
                    // completeness guarantees for user data portability.

                    // 1. Activity Log (streamed - potentially large table)
                    runExport( "activity_log.csv") { w ->
                        w.appendLine("ID;Timestamp;Module;Action;Title;Details;Date")
                        db.openHelper.readableDatabase.query("SELECT * FROM activity_log ORDER BY timestamp DESC").use { cursor ->
                            val idIdx = cursor.getColumnIndexOrThrow("id")
                            val tsIdx = cursor.getColumnIndexOrThrow("timestamp")
                            val modIdx = cursor.getColumnIndexOrThrow("module")
                            val actIdx = cursor.getColumnIndexOrThrow("action")
                            val titleIdx = cursor.getColumnIndexOrThrow("itemTitle")
                            val detIdx = cursor.getColumnIndexOrThrow("details")
                            val dateIdx = cursor.getColumnIndexOrThrow("date")

                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idIdx)
                                val ts = cursor.getLong(tsIdx)
                                val mod = cursor.getString(modIdx)
                                val act = cursor.getString(actIdx)
                                val title = cursor.getString(titleIdx)
                                val det = cursor.getString(detIdx)
                                val date = cursor.getString(dateIdx)
                                w.appendLine("$id;$ts;$mod;$act;${esc(title)};${esc(det)};$date")
                            }
                        }
                    }

                    // 2. Calendar
                    val events = db.calendarDao().getEventsInRange("0000-01-01", "9999-12-31").first()
                    if (events.isNotEmpty()) {
                        runExport( "calendar.csv") { w ->
                            w.appendLine("ID;Title;Description;Date;Start Time;End Time;Category;Color;Is Holiday")
                            events.forEach { r ->
                                w.appendLine("${r.uuid};${esc(r.title)};${esc(r.description)};${r.date};${r.time ?: ""};${r.endTime ?: ""};${r.category};${r.color};${r.isHoliday}")
                            }
                        }
                    }

                    // 3. Mood / Mental Health
                    val moods = db.moodDao().getAllEntries().first()
                    if (moods.isNotEmpty()) {
                        runExport( "mood.csv") { w ->
                            w.appendLine("ID;Date;Mood Level;Energy Level;Stress Level;Sleep Quality;Positive Notes;Negative Notes")
                            moods.forEach { r ->
                                w.appendLine("${r.uuid};${r.date};${r.moodLevel};${r.energyLevel};${r.stressLevel};${r.sleepQuality};${esc(r.positiveNotes)};${esc(r.negativeNotes)}")
                            }
                        }
                    }

                    // 4. Habits
                    val habits = db.habitDao().getAllHabits().first()
                    if (habits.isNotEmpty()) {
                        runExport( "habits.csv") { w ->
                            w.appendLine("ID;Name;Emoji;Frequency;Repeat Interval (Days);Active;Created (ms)")
                            habits.forEach { r ->
                                w.appendLine("${r.uuid};${esc(r.name)};${r.emoji};${r.frequency};${r.repeatIntervalDays};${r.isActive};${r.createdAt}")
                            }
                        }
                    }

                    // 5. Habit Completion Logs (streamed - potentially large table)
                    runExport( "habit_completions.csv") { w ->
                        w.appendLine("ID;Habit ID;Date;Completed;Completed At (ms)")
                        db.openHelper.readableDatabase.query("SELECT * FROM habit_logs").use { cursor ->
                            val idIdx = cursor.getColumnIndexOrThrow("uuid")
                            val habitIdIdx = cursor.getColumnIndexOrThrow("habitId")
                            val dateIdx = cursor.getColumnIndexOrThrow("date")
                            val completedIdx = cursor.getColumnIndexOrThrow("completed")
                            val completedAtIdx = cursor.getColumnIndexOrThrow("completedAtMillis")

                            while (cursor.moveToNext()) {
                                val id = cursor.getString(idIdx)
                                val habitId = cursor.getString(habitIdIdx)
                                val date = cursor.getString(dateIdx)
                                val completed = cursor.getInt(completedIdx) == 1
                                val completedAt = if (cursor.isNull(completedAtIdx)) "" else cursor.getLong(completedAtIdx).toString()
                                w.appendLine("$id;$habitId;$date;$completed;$completedAt")
                            }
                        }
                    }

                    // 6. Supplements
                    val supps = db.supplementDao().getAllSupplements().first()
                    if (supps.isNotEmpty()) {
                        runExport( "supplements.csv") { w ->
                            w.appendLine("ID;Name;Dosage;Frequency;Time of Day;Times per Day;Active;Notes")
                            supps.forEach { r ->
                                w.appendLine("${r.uuid};${esc(r.name)};${esc(r.dosage)};${r.frequency};${r.timeOfDay};${r.timesPerDay};${r.isActive};${esc(r.notes)}")
                            }
                        }
                    }

                    // 7. Supplement Ingredients
                    val ingredients = db.supplementDao().getAllIngredients().first()
                    if (ingredients.isNotEmpty()) {
                        runExport( "supplement_ingredients.csv") { w ->
                            w.appendLine("ID;Supplement ID;Ingredient;Amount;Unit;RVS %")
                            ingredients.forEach { r ->
                                w.appendLine("${r.uuid};${r.supplementId};${esc(r.name)};${fmt(r.amount)};${r.unit};${r.rvsPct ?: ""}")
                            }
                        }
                    }

                    // 8. Supplement Intake Logs
                    val intakes = db.supplementDao().getAllTakenLogs().first()
                    if (intakes.isNotEmpty()) {
                        runExport( "supplement_intake.csv") { w ->
                            w.appendLine("ID;Supplement ID;Date;Taken;Taken At (ms)")
                            intakes.forEach { r ->
                                w.appendLine("${r.uuid};${r.supplementId};${r.date};${r.taken};${r.takenAtMillis ?: ""}")
                            }
                        }
                    }

                    // 9. Schedule / Courses
                    val courses = db.courseDao().getActiveCourses().first()
                    if (courses.isNotEmpty()) {
                        runExport( "schedule.csv") { w ->
                            w.appendLine("ID;Name;Professor;Room;Day of Week;Start Time;End Time;Semester;Type;Color")
                            courses.forEach { r ->
                                w.appendLine("${r.uuid};${esc(r.name)};${esc(r.professor)};${esc(r.room)};${r.dayOfWeek};${r.startTime};${r.endTime};${esc(r.semester)};${r.courseType};${r.color}")
                            }
                        }
                    }

                    // 10. Exercise Definitions
                    val exercises = db.gymDao().getAllExerciseDefinitions().first()
                    if (exercises.isNotEmpty()) {
                        runExport( "exercises.csv") { w ->
                            w.appendLine("ID;Name;Category;Muscle Group;Notes")
                            exercises.forEach { r ->
                                w.appendLine("${r.uuid};${esc(r.name)};${esc(r.category)};${esc(r.muscleGroup)};${esc(r.notes)}")
                            }
                        }
                    }

                    // 11. Workout Templates + Template Exercises
                    val templates = db.gymDao().getAllWorkoutTemplates().first()
                    if (templates.isNotEmpty()) {
                        runExport( "workout_templates.csv") { w ->
                            w.appendLine("ID;Name;Date;Duration (min);Notes;Created (ms)")
                            templates.forEach { temp ->
                                w.appendLine("${temp.workout.uuid};${esc(temp.workout.name)};${temp.workout.date};${temp.workout.durationMinutes ?: ""};${esc(temp.workout.notes)};${temp.workout.createdAt}")
                            }
                        }

                        runExport( "workout_template_exercises.csv") { w ->
                            w.appendLine("ID;Workout ID;Exercise Name;Order Index")
                            templates.forEach { temp ->
                                temp.exercises.forEach { e ->
                                    w.appendLine("${e.uuid};${e.workoutId};${esc(e.exerciseName)};${e.orderIndex}")
                                }
                            }
                        }
                    }

                    // 12. Gym Sessions + Sets
                    val sessions = db.gymDao().getAllSessionsWithSets().first()
                    if (sessions.isNotEmpty()) {
                        runExport( "gym_sessions.csv") { w ->
                            w.appendLine("ID;Date;Name;Type;Duration (min);Template ID;Notes;Created (ms)")
                            sessions.forEach { s ->
                                w.appendLine("${s.session.uuid};${s.session.date};${esc(s.session.name)};${s.session.type};${s.session.durationMinutes ?: ""};${s.session.workoutTemplateId ?: ""};${esc(s.session.notes)};${s.session.createdAt}")
                            }
                        }

                        runExport( "gym_sets.csv") { w ->
                            w.appendLine("Session ID;Exercise;Set Number;Reps;Weight (kg);Duration (s)")
                            sessions.forEach { s ->
                                s.sets.forEach { set ->
                                    w.appendLine("${set.sessionId};${esc(set.exerciseName)};${set.setNumber};${set.reps ?: ""};${fmt(set.weightKg)};${set.durationSeconds ?: ""}")
                                }
                            }
                        }
                    }

                    // 13. Nutrition - Food Database
                    val foods = db.foodDao().getAllFoodItems().first()
                    if (foods.isNotEmpty()) {
                        runExport( "food_database.csv") { w ->
                            w.appendLine("ID;Name;kcal/100g;Protein/100g;Carbs/100g;Fat/100g;Sugar/100g;Barcode")
                            foods.forEach { r ->
                                w.appendLine("${r.uuid};${esc(r.name)};${fmt(r.kcalPer100g)};${fmt(r.proteinPer100g)};${fmt(r.carbsPer100g)};${fmt(r.fatPer100g)};${fmt(r.sugarPer100g)};${r.barcode ?: ""}")
                            }
                        }
                    }

                    // 14. Nutrition - Daily Entries (streamed - potentially large table)
                    runExport( "nutrition_log.csv") { w ->
                        w.appendLine("ID;Food Item ID;Food Name;Date;Grams")
                        val query = """
                            SELECT d.uuid, d.foodItemId, f.name, d.date, d.gramsConsumed
                            FROM daily_food_entries d
                            INNER JOIN food_items f ON d.foodItemId = f.uuid
                            ORDER BY d.date ASC
                        """.trimIndent()
                        db.openHelper.readableDatabase.query(query).use { cursor ->
                            val idIdx = cursor.getColumnIndexOrThrow("uuid")
                            val fIdIdx = cursor.getColumnIndexOrThrow("foodItemId")
                            val nameIdx = cursor.getColumnIndexOrThrow("name")
                            val dateIdx = cursor.getColumnIndexOrThrow("date")
                            val gramsIdx = cursor.getColumnIndexOrThrow("gramsConsumed")

                            while (cursor.moveToNext()) {
                                val id = cursor.getString(idIdx)
                                val fId = cursor.getString(fIdIdx)
                                val fName = cursor.getString(nameIdx)
                                val date = cursor.getString(dateIdx)
                                val grams = cursor.getFloat(gramsIdx)
                                w.appendLine("$id;$fId;${esc(fName)};$date;${fmt(grams)}")
                            }
                        }
                    }

                    // 15. Work Time
                    val workLogs = db.workTimeDao().getAllEntries().first()
                    if (workLogs.isNotEmpty()) {
                        runExport( "work_time.csv") { w ->
                            w.appendLine("ID;Date;Clock In (ms);Clock Out (ms);Break (min);Project;Notes")
                            workLogs.forEach { r ->
                                w.appendLine("${r.uuid};${r.date};${r.clockInMillis};${r.clockOutMillis ?: ""};${r.breakMinutes};${esc(r.projectName)};${esc(r.notes)}")
                            }
                        }
                    }

                    // 16. Weight
                    val weights = db.weightDao().getAllEntries().first()
                    if (weights.isNotEmpty()) {
                        runExport( "weight.csv") { w ->
                            w.appendLine("ID;Date;Weight (kg);Time of Day;After Waking;Notes")
                            weights.forEach { r ->
                                w.appendLine("${r.uuid};${r.date};${fmt(r.weightKg)};${r.timeOfDay};${r.afterWaking};${esc(r.notes)}")
                            }
                        }
                    }

                    // 17. Shopping List
                    val shoppingItems = db.shoppingDao().getAllItems().first()
                    if (shoppingItems.isNotEmpty()) {
                        runExport( "shopping.csv") { w ->
                            w.appendLine("ID;Name;Quantity;Category;Checked;Created (ms)")
                            shoppingItems.forEach { r ->
                                w.appendLine("${r.uuid};${esc(r.name)};${esc(r.quantity)};${esc(r.category)};${r.checked};${r.createdAt}")
                            }
                        }
                    }
                }
            }
            if (allSuccess) 2 else if (anySuccess) 1 else 0
        } catch (e: Exception) {
            errorLogger.logError("DataExporter", "Export failed", e)
            0
        }
    }

    /** Streams a CSV file directly into the ZIP entry. Safe against internal table crashes. */
    private inline fun streamCsv(zip: ZipOutputStream, name: String, block: (BufferedWriter) -> Unit): Boolean {
        return try {
            zip.putNextEntry(ZipEntry(name))
            val writer = BufferedWriter(OutputStreamWriter(zip, Charsets.UTF_8))
            // Write UTF-8 BOM (Byte Order Mark) so Excel recognizes encoding
            writer.write("\uFEFF")
            block(writer)
            writer.flush()
            zip.closeEntry()
            true
        } catch (e: Exception) {
            timber.log.Timber.e(e, "DataExporter: Failed to stream table into CSV: %s", name)
            // If the entry fails midway, attempt to close it so the ZIP file as a whole remains valid
            try { zip.closeEntry() } catch (ignored: Exception) {}
            false
        }
    }

    /** Escapes CSV values containing semicolons, quotes, or newlines (RFC 4180 compliant). */
    private fun esc(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        val containsSpecial = value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        if (!containsSpecial) return value
        
        // RFC 4180: Fields containing line breaks (CRLF), double quotes, and commas should be enclosed in double-quotes
        // Normalize all newlines to CRLF as expected by Excel
        val norm = value.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n")
        return "\"${norm.replace("\"", "\"\"")}\""
    }

    /** Formats a float to 1 decimal place using a fixed Locale (dot instead of comma). */
    private fun fmt(value: Float?): String {
        if (value == null) return ""
        return String.format(java.util.Locale.US, "%.1f", value)
    }
    
    /** Formats a double to 1 decimal place using a fixed Locale. */
    private fun fmt(value: Double?): String {
        if (value == null) return ""
        return String.format(java.util.Locale.US, "%.1f", value)
    }

    /**
     * Returns the total number of user data rows across all primary tables.
     * Used to show a meaningful count in the export subtitle instead of the
     * internal activity log count.
     */
    fun getTotalDataRowCount() = kotlinx.coroutines.flow.combine(
        db.foodDao().getEntriesForDateRange("2000-01-01", "2099-12-31"),
        db.habitDao().getAllHabits(),
        db.gymDao().getAllSessionsWithSets(),
        db.moodDao().getAllEntries(),
        db.weightDao().getAllEntries()
    ) { nutrition, habits, sessions, moods, weights ->
        nutrition.size + habits.size + sessions.sumOf { 1 + it.sets.size } + moods.size + weights.size
    }
}
