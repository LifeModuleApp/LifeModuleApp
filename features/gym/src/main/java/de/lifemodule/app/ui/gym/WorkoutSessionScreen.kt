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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.feature.gym.R
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor

/**
 * Workout session: shows exercises from a workout template (playlist)
 * with expandable exercise cards for inline set logging.
 *
 * Uses [GymSessionViewModel] (injected via Hilt + SavedStateHandle)
 * to track sets for this specific session.
 */
@Composable
fun WorkoutSessionScreen(
    navController: NavController,
    workoutId: String,
    sessionViewModel: GymSessionViewModel = hiltViewModel(),
    gymViewModel: GymViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val templates by gymViewModel.workoutTemplates.collectAsStateWithLifecycle()
    val sessionSets by sessionViewModel.sets.collectAsStateWithLifecycle()

    val workout = remember(templates, workoutId) {
        templates.find { it.workout.uuid == workoutId }
    }
    val exerciseNames = workout?.exerciseNamesInOrder ?: emptyList()

    // Exercise definitions for metadata (muscle group, category)
    val exerciseDefs by gymViewModel.exerciseDefinitions.collectAsStateWithLifecycle()
    val exerciseDefMap = remember(exerciseDefs) {
        exerciseDefs.associateBy { it.name }
    }

    // Group session sets by exercise
    val setsByExercise = remember(sessionSets) {
        sessionSets.groupBy { it.exerciseName }
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = workout?.workout?.name ?: stringResource(R.string.gym_gym_training),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            itemsIndexed(exerciseNames) { index, exerciseName ->
                val def = exerciseDefMap[exerciseName]
                val sets = setsByExercise[exerciseName] ?: emptyList()

                ExerciseLogCard(
                    exerciseName = exerciseName,
                    category = def?.category ?: "strength",
                    muscleGroup = def?.muscleGroup ?: "",
                    sets = sets,
                    accent = accent,
                    onLogSets = { entries ->
                        sessionViewModel.logSets(exerciseName, index, entries)
                    },
                    onLogCardio = { seconds ->
                        sessionViewModel.logCardioSet(exerciseName, index, seconds)
                    },
                    onDeleteSet = { set -> sessionViewModel.deleteSet(set) },
                    onUpdateSet = { set -> sessionViewModel.updateSet(set) },
                    imagePath = def?.imagePath,
                    initiallyExpanded = true,
                    isImported = def?.importSource == de.lifemodule.app.data.ImportSource.COMMUNITY_HUB
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(
                        stringResource(R.string.gym_gym_fertig),
                        color = Black,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
