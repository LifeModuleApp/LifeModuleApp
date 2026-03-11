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

package de.lifemodule.app.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.gym.ExerciseDefinitionEntity
import de.lifemodule.app.data.gym.GymRepository
import de.lifemodule.app.data.gym.GymSessionEntity
import de.lifemodule.app.data.gym.GymSessionWithSets
import de.lifemodule.app.data.gym.SessionSetEntity
import de.lifemodule.app.data.gym.WorkoutEntity
import de.lifemodule.app.data.gym.WorkoutWithTemplateExercises
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class GymViewModel @Inject constructor(
    private val repository: GymRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val todayDate: String =
        timeProvider.today().format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** Exercise catalog (global pool) */
    val exerciseDefinitions: StateFlow<List<ExerciseDefinitionEntity>> =
        repository.getAllExerciseDefinitions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All saved workout templates (playlists) */
    val workoutTemplates: StateFlow<List<WorkoutWithTemplateExercises>> =
        repository.getAllWorkoutTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All sets logged today across all sessions (reactive) */
    val todaySets: StateFlow<List<SessionSetEntity>> =
        repository.getSetsForDate(todayDate)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Recent training sessions (for analytics / history) */
    val recentSessions: StateFlow<List<GymSessionWithSets>> =
        repository.getRecentSessionsWithSets(30)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Quick-Log (Tab 1) ─────────────────────────────────────────────────

    /** Cached quick-log session UUID for today (lazily created) */
    private var todaySessionId: String? = null
    private val sessionMutex = Mutex()

    private suspend fun ensureTodaySession(fallbackName: String): String = sessionMutex.withLock {
        todaySessionId?.let { return@withLock it }
        val id = repository.getOrCreateQuickLogSession(todayDate, fallbackName)
        todaySessionId = id
        id
    }

    /**
     * Logs sets for an exercise from the Quick Log tab.
     * Automatically creates / reuses today's session.
     */
    fun logSetsForExercise(
        exerciseName: String,
        entries: List<Pair<Int, Double?>>,
        fallbackSessionName: String
    ) {
        viewModelScope.launch {
            try {
                val sessionId = ensureTodaySession(fallbackSessionName)
                val maxSetNumber = repository.getMaxSetNumber(sessionId, exerciseName) ?: 0
                var nextSetNumber = maxSetNumber + 1
                for ((reps, weight) in entries) {
                    repository.insertSet(
                        SessionSetEntity(
                            sessionId = sessionId,
                            exerciseName = exerciseName,
                            exerciseOrderIndex = 0,
                            setNumber = nextSetNumber++,
                            reps = reps,
                            weightKg = weight
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "[Gym] Failed to log sets for exercise '%s'", exerciseName)
            }
        }
    }

    fun deleteSet(set: SessionSetEntity) {
        viewModelScope.launch {
            try { repository.deleteSet(set) }
            catch (e: Exception) { Timber.e(e, "[Gym] Failed to delete set %s", set.uuid) }
        }
    }

    fun updateSet(set: SessionSetEntity) {
        viewModelScope.launch {
            try { repository.updateSet(set) }
            catch (e: Exception) { Timber.e(e, "[Gym] Failed to update set %s", set.uuid) }
        }
    }

    /**
     * Logs a cardio entry (duration in seconds) for an exercise.
     */
    fun logCardioForExercise(
        exerciseName: String,
        durationSeconds: Int,
        fallbackSessionName: String
    ) {
        viewModelScope.launch {
            try {
                val sessionId = ensureTodaySession(fallbackSessionName)
                val maxSetNumber = repository.getMaxSetNumber(sessionId, exerciseName) ?: 0
                repository.insertSet(
                    SessionSetEntity(
                        sessionId = sessionId,
                        exerciseName = exerciseName,
                        exerciseOrderIndex = 0,
                        setNumber = maxSetNumber + 1,
                        durationSeconds = durationSeconds
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Gym] Failed to log cardio for '%s'", exerciseName)
            }
        }
    }

    /**
     * Update exercise definition image path.
     */
    fun updateExerciseImage(def: ExerciseDefinitionEntity, imagePath: String?) {
        viewModelScope.launch {
            try { repository.updateExerciseDefinition(def.copy(imagePath = imagePath)) }
            catch (e: Exception) { Timber.e(e, "[Gym] Failed to update exercise image for '%s'", def.name) }
        }
    }

    /**
     * Full update for an exercise definition (name, category, muscleGroup, image).
     */
    fun updateExerciseDefinition(def: ExerciseDefinitionEntity) {
        viewModelScope.launch {
            try { repository.updateExerciseDefinition(def) }
            catch (e: Exception) { Timber.e(e, "[Gym] Failed to update exercise definition '%s'", def.name) }
        }
    }

    // ── Workout Sessions (Tab 2) ──────────────────────────────────────────

    /**
     * Creates a new session linked to a workout template.
     * Called when the user taps "Start" on a workout card.
     */
    suspend fun createWorkoutSession(
        workoutName: String,
        workoutTemplateId: String
    ): String = repository.insertSession(
        GymSessionEntity(
            date = todayDate,
            name = workoutName,
            type = "strength",
            workoutTemplateId = workoutTemplateId
        )
    )

    // ── Workout Templates (playlists) ─────────────────────────────────────

    fun addWorkoutTemplate(name: String, exerciseNames: List<String>) {
        viewModelScope.launch {
            try {
                if (name.isBlank() || exerciseNames.all { it.isBlank() }) return@launch
                repository.insertWorkoutTemplate(
                    WorkoutEntity(name = name, date = todayDate),
                    exerciseNames.filter { it.isNotBlank() }
                )
            } catch (e: Exception) {
                Timber.e(e, "[Gym] Failed to add workout template '%s'", name)
            }
        }
    }

    fun deleteWorkoutTemplate(workout: WorkoutEntity) {
        viewModelScope.launch {
            try { repository.deleteWorkout(workout) }
            catch (e: Exception) { Timber.e(e, "[Gym] Failed to delete workout '%s'", workout.name) }
        }
    }

    // ── Exercise Library ──────────────────────────────────────────────────

    fun addExerciseDefinition(
        name: String,
        category: String = "strength",
        muscleGroup: String = "",
        notes: String = "",
        imagePath: String? = null
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) return@launch
                repository.insertExerciseDefinition(
                    ExerciseDefinitionEntity(
                        name = name,
                        category = category,
                        muscleGroup = muscleGroup,
                        notes = notes,
                        imagePath = imagePath
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Gym] Failed to add exercise definition '%s'", name)
            }
        }
    }

    fun deleteExerciseDefinition(def: ExerciseDefinitionEntity) {
        viewModelScope.launch {
            try { repository.deleteExerciseDefinition(def) }
            catch (e: Exception) { Timber.e(e, "[Gym] Failed to delete exercise '%s'", def.name) }
        }
    }

    // ── Session Management ────────────────────────────────────────────────

    fun deleteSession(session: GymSessionEntity) {
        viewModelScope.launch {
            try { repository.deleteSession(session) }
            catch (e: Exception) { Timber.e(e, "[Gym] Failed to delete session %s", session.uuid) }
        }
    }
}

