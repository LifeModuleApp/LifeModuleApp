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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.gym.GymRepository
import de.lifemodule.app.data.gym.GymSessionEntity
import de.lifemodule.app.data.gym.SessionSetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the active training session screen.
 * Receives [sessionId] via [SavedStateHandle] from the nav argument.
 */
@HiltViewModel
class GymSessionViewModel @Inject constructor(
    private val repository: GymRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    /** Current session metadata */
    private val _session = MutableStateFlow<GymSessionEntity?>(null)
    val session: StateFlow<GymSessionEntity?> = _session.asStateFlow()

    /** All sets logged in this session, ordered by exercise then set number */
    val sets: StateFlow<List<SessionSetEntity>> =
        repository.getSetsForSession(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Sets grouped by exercise name, preserving original exercise order.
     * Map is ordered: first exercise added comes first.
     */
    val setsByExercise: StateFlow<Map<String, List<SessionSetEntity>>> =
        sets.map { allSets ->
            allSets
                .groupBy { it.exerciseName }
                .entries
                .sortedBy { entry -> entry.value.minOfOrNull { it.exerciseOrderIndex } ?: 0 }
                .associate { it.key to it.value }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            try { _session.value = repository.getSessionByUuid(sessionId) }
            catch (e: Exception) { Timber.e(e, "[GymSession] Failed to load session %s", sessionId) }
        }
    }

    // ── Set logging ───────────────────────────────────────────────────────

    /**
     * Logs one set for [exerciseName].
     * [exerciseOrderIndex] is the position of the exercise within this session.
     */
    fun logSet(
        exerciseName: String,
        exerciseOrderIndex: Int,
        reps: Int?,
        weightKg: Double?
    ) {
        viewModelScope.launch {
            try {
                val existingSets = sets.value.filter { it.exerciseName == exerciseName }
                val nextSetNumber = existingSets.size + 1
                repository.insertSet(
                    SessionSetEntity(
                        sessionId = sessionId,
                        exerciseName = exerciseName,
                        exerciseOrderIndex = exerciseOrderIndex,
                        setNumber = nextSetNumber,
                        reps = reps,
                        weightKg = weightKg
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[GymSession] Failed to log set for '%s'", exerciseName)
            }
        }
    }

    /**
     * Logs multiple sets at once from note-style input (e.g. "7x30, 3x50").
     * Each entry is a Pair(reps, weightKg).
     */
    fun logSets(
        exerciseName: String,
        exerciseOrderIndex: Int,
        entries: List<Pair<Int, Double?>>
    ) {
        viewModelScope.launch {
            try {
                val existingSets = sets.value.filter { it.exerciseName == exerciseName }
                var nextSetNumber = existingSets.size + 1
                for ((reps, weight) in entries) {
                    repository.insertSet(
                        SessionSetEntity(
                            sessionId = sessionId,
                            exerciseName = exerciseName,
                            exerciseOrderIndex = exerciseOrderIndex,
                            setNumber = nextSetNumber++,
                            reps = reps,
                            weightKg = weight
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "[GymSession] Failed to log sets for '%s'", exerciseName)
            }
        }
    }

    /**
     * Logs a cardio set with duration in seconds.
     */
    fun logCardioSet(
        exerciseName: String,
        exerciseOrderIndex: Int,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            try {
                val existingSets = sets.value.filter { it.exerciseName == exerciseName }
                val nextSetNumber = existingSets.size + 1
                repository.insertSet(
                    SessionSetEntity(
                        sessionId = sessionId,
                        exerciseName = exerciseName,
                        exerciseOrderIndex = exerciseOrderIndex,
                        setNumber = nextSetNumber,
                        durationSeconds = durationSeconds
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[GymSession] Failed to log cardio for '%s'", exerciseName)
            }
        }
    }

    fun deleteSet(set: SessionSetEntity) {
        viewModelScope.launch {
            try { repository.deleteSet(set) }
            catch (e: Exception) { Timber.e(e, "[GymSession] Failed to delete set %s", set.uuid) }
        }
    }

    fun updateSet(set: SessionSetEntity) {
        viewModelScope.launch {
            try { repository.updateSet(set) }
            catch (e: Exception) { Timber.e(e, "[GymSession] Failed to update set %s", set.uuid) }
        }
    }

    // ── Session finish ────────────────────────────────────────────────────

    /**
     * Saves the duration and optional notes when the session is
     * finished by the user.
     */
    fun finishSession(durationMinutes: Int?, notes: String = "") {
        viewModelScope.launch {
            try {
                val current = repository.getSessionByUuid(sessionId) ?: return@launch
                repository.updateSession(
                    current.copy(
                        durationMinutes = durationMinutes,
                        notes = notes.ifBlank { current.notes }
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[GymSession] Failed to finish session %s", sessionId)
            }
        }
    }
}
