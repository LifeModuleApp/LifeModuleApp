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

import androidx.room.withTransaction
import de.lifemodule.app.data.LifeModuleDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GymRepository @Inject constructor(
    private val gymDao: GymDao,
    private val db: LifeModuleDatabase
) {

    // ── Workout Templates ──────────────────────────────────────────────────

    fun getAllWorkoutTemplates(): Flow<List<WorkoutWithTemplateExercises>> =
        gymDao.getAllWorkoutTemplates()

    suspend fun getWorkoutByUuid(uuid: String): WorkoutEntity? =
        gymDao.getWorkoutByUuid(uuid)

    /**
     * Inserts a workout template with its ordered exercises (names only, no sets/reps).
     * Replaces all existing template exercises for this workout on re-save.
     */
    suspend fun insertWorkoutTemplate(
        workout: WorkoutEntity,
        exerciseNames: List<String>
    ): String = db.withTransaction {
        gymDao.insertWorkout(workout)
        val workoutUuid = workout.uuid
        gymDao.deleteTemplateExercisesForWorkout(workoutUuid)
        val templateExercises = exerciseNames.mapIndexed { index, name ->
            WorkoutTemplateExercise(workoutId = workoutUuid, exerciseName = name, orderIndex = index)
        }
        gymDao.insertTemplateExercises(templateExercises)
        workoutUuid
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) =
        gymDao.deleteWorkout(workout)

    // ── Exercise Definition Library ───────────────────────────────────────

    fun getAllExerciseDefinitions(): Flow<List<ExerciseDefinitionEntity>> =
        gymDao.getAllExerciseDefinitions()

    fun getExerciseDefinitionsByCategory(category: String): Flow<List<ExerciseDefinitionEntity>> =
        gymDao.getExerciseDefinitionsByCategory(category)

    fun searchExerciseDefinitions(query: String): Flow<List<ExerciseDefinitionEntity>> =
        gymDao.searchExerciseDefinitions(query)

    suspend fun insertExerciseDefinition(def: ExerciseDefinitionEntity): Long =
        gymDao.insertExerciseDefinition(def)

    suspend fun updateExerciseDefinition(def: ExerciseDefinitionEntity) =
        gymDao.updateExerciseDefinition(def)

    suspend fun deleteExerciseDefinition(def: ExerciseDefinitionEntity) =
        gymDao.deleteExerciseDefinition(def)

    // ── Sessions ──────────────────────────────────────────────────────────

    fun getAllSessionsWithSets(): Flow<List<GymSessionWithSets>> =
        gymDao.getAllSessionsWithSets()

    fun getRecentSessionsWithSets(limit: Int = 20): Flow<List<GymSessionWithSets>> =
        gymDao.getRecentSessionsWithSets(limit)

    fun getSetsForSession(sessionUuid: String): Flow<List<SessionSetEntity>> =
        gymDao.getSetsForSession(sessionUuid)

    suspend fun getSessionByUuid(uuid: String): GymSessionEntity? =
        gymDao.getSessionByUuid(uuid)

    suspend fun insertSession(session: GymSessionEntity): String {
        gymDao.insertSession(session)
        return session.uuid
    }

    suspend fun updateSession(session: GymSessionEntity) =
        gymDao.updateSession(session)

    suspend fun deleteSession(session: GymSessionEntity) =
        gymDao.deleteSession(session)

    // ── Session Sets ──────────────────────────────────────────────────────

    suspend fun insertSet(set: SessionSetEntity): Long =
        gymDao.insertSet(set)

    suspend fun updateSet(set: SessionSetEntity) =
        gymDao.updateSet(set)

    suspend fun deleteSet(set: SessionSetEntity) =
        gymDao.deleteSet(set)

    // ── Analytics ─────────────────────────────────────────────────────────

    fun getSetHistoryByExercise(name: String): Flow<List<SessionSetEntity>> =
        gymDao.getSetHistoryByExercise(name)

    fun getDistinctExerciseNames(): Flow<List<String>> =
        gymDao.getDistinctExerciseNames()

    fun getTotalSessionCount(): Flow<Int> =
        gymDao.getTotalSessionCount()

    fun getTotalTrainingMinutes(): Flow<Int?> =
        gymDao.getTotalTrainingMinutes()

    // ── Quick-Log helpers ──────────────────────────────────────────────────

    fun getSetsForDate(date: String): Flow<List<SessionSetEntity>> =
        gymDao.getSetsForDate(date)

    suspend fun getMaxSetNumber(sessionUuid: String, exerciseName: String): Int? =
        gymDao.getMaxSetNumber(sessionUuid, exerciseName)

    suspend fun getOrCreateQuickLogSession(date: String, name: String): String {
        val existing = gymDao.getQuickLogSessionByDate(date)
        if (existing != null) return existing.uuid
        val session = GymSessionEntity(date = date, name = name)
        gymDao.insertSession(session)
        return session.uuid
    }
}
