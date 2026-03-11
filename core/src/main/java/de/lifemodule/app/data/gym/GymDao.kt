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

package de.lifemodule.app.data.gym

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {

    // ── Workout Templates ──────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY name ASC")
    fun getAllWorkoutTemplates(): Flow<List<WorkoutWithTemplateExercises>>

    @Query("SELECT * FROM workouts WHERE uuid = :uuid")
    suspend fun getWorkoutByUuid(uuid: String): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    // ── Template Exercises (order-only, no sets/reps) ─────────────────────

    @Query("SELECT * FROM workout_template_exercises WHERE workoutId = :workoutUuid ORDER BY orderIndex")
    fun getTemplateExercises(workoutUuid: String): Flow<List<WorkoutTemplateExercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercise(exercise: WorkoutTemplateExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExercise>)

    @Query("DELETE FROM workout_template_exercises WHERE workoutId = :workoutUuid")
    suspend fun deleteTemplateExercisesForWorkout(workoutUuid: String)

    // ── Exercise Definition Library ───────────────────────────────────────

    @Query("SELECT * FROM exercise_definitions ORDER BY category ASC, name ASC")
    fun getAllExerciseDefinitions(): Flow<List<ExerciseDefinitionEntity>>

    @Query("SELECT * FROM exercise_definitions WHERE category = :category ORDER BY name ASC")
    fun getExerciseDefinitionsByCategory(category: String): Flow<List<ExerciseDefinitionEntity>>

    @Query("SELECT * FROM exercise_definitions WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExerciseDefinitions(query: String): Flow<List<ExerciseDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseDefinition(def: ExerciseDefinitionEntity): Long

    /** Import-safe: returns -1 if UUID already exists (never overwrites). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseDefinitionIgnore(def: ExerciseDefinitionEntity): Long

    /** Import-safe: returns -1 per skipped entry. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkoutIgnore(workout: WorkoutEntity): Long

    /** Import-safe: returns -1 per skipped entry. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplateExerciseIgnore(exercise: WorkoutTemplateExercise): Long

    @Update
    suspend fun updateExerciseDefinition(def: ExerciseDefinitionEntity)

    @Delete
    suspend fun deleteExerciseDefinition(def: ExerciseDefinitionEntity)

    // ── Sessions ──────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM gym_sessions ORDER BY date DESC, created_at DESC")
    fun getAllSessionsWithSets(): Flow<List<GymSessionWithSets>>

    @Transaction
    @Query("SELECT * FROM gym_sessions ORDER BY date DESC LIMIT :limit")
    fun getRecentSessionsWithSets(limit: Int = 20): Flow<List<GymSessionWithSets>>

    @Query("SELECT * FROM gym_sessions WHERE uuid = :uuid")
    suspend fun getSessionByUuid(uuid: String): GymSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GymSessionEntity): Long

    @Update
    suspend fun updateSession(session: GymSessionEntity)

    @Delete
    suspend fun deleteSession(session: GymSessionEntity)

    // ── Session Sets ──────────────────────────────────────────────────────

    @Query("SELECT * FROM session_sets WHERE sessionId = :sessionUuid ORDER BY exerciseOrderIndex, setNumber")
    fun getSetsForSession(sessionUuid: String): Flow<List<SessionSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SessionSetEntity): Long

    @Update
    suspend fun updateSet(set: SessionSetEntity)

    @Delete
    suspend fun deleteSet(set: SessionSetEntity)

    @Query("DELETE FROM session_sets WHERE sessionId = :sessionUuid")
    suspend fun deleteSetsForSession(sessionUuid: String)

    // ── Analytics ─────────────────────────────────────────────────────────

    /** Full history of a specific exercise for progressive overload charts */
    @Query("""
        SELECT s.* FROM session_sets s
        INNER JOIN gym_sessions gs ON s.sessionId = gs.uuid
        WHERE s.exerciseName = :name
        ORDER BY gs.date ASC, s.setNumber ASC
    """)
    fun getSetHistoryByExercise(name: String): Flow<List<SessionSetEntity>>

    @Query("SELECT DISTINCT exerciseName FROM session_sets ORDER BY exerciseName ASC")
    fun getDistinctExerciseNames(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM gym_sessions")
    fun getTotalSessionCount(): Flow<Int>

    @Query("SELECT SUM(durationMinutes) FROM gym_sessions WHERE durationMinutes IS NOT NULL")
    fun getTotalTrainingMinutes(): Flow<Int?>

    // ── Quick-Log helpers ──────────────────────────────────────────────────

    @Query("SELECT * FROM gym_sessions WHERE date = :date AND workoutTemplateId IS NULL ORDER BY created_at ASC LIMIT 1")
    suspend fun getQuickLogSessionByDate(date: String): GymSessionEntity?

    @Query("""
        SELECT s.* FROM session_sets s
        INNER JOIN gym_sessions gs ON s.sessionId = gs.uuid
        WHERE gs.date = :date
        ORDER BY s.exerciseName ASC, s.setNumber ASC
    """)
    fun getSetsForDate(date: String): Flow<List<SessionSetEntity>>

    @Query("SELECT MAX(setNumber) FROM session_sets WHERE sessionId = :sessionUuid AND exerciseName = :exerciseName")
    suspend fun getMaxSetNumber(sessionUuid: String, exerciseName: String): Int?
}
