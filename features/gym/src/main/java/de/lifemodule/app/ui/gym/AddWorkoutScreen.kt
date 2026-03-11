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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.feature.gym.R
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary

/**
 * Screen for creating a new workout (playlist of exercises).
 * User picks exercises from the global exercise catalog.
 */
@Composable
fun AddWorkoutScreen(
    navController: NavController,
    viewModel: GymViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    var workoutName by remember { mutableStateOf("") }
    val allExercises by viewModel.exerciseDefinitions.collectAsStateWithLifecycle()
    val selectedNames = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.gym_gym_workout_erstellen),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ── Workout name ──
            LMInput(
                value = workoutName,
                onValueChange = { workoutName = it },
                label = stringResource(R.string.gym_gym_workout_name_hint)
            )

            Spacer(Modifier.height(16.dp))

            // ── Selected exercises preview ──
            if (selectedNames.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.gym_gym_uebungsreihenfolge),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                selectedNames.forEachIndexed { idx, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${idx + 1}.",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedNames.remove(name) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.gym_gym_entfernen),
                                tint = Destructive
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Exercise catalog ──
            Text(
                text = stringResource(R.string.gym_gym_uebungen_auswaehlen),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            if (allExercises.isEmpty()) {
                Text(
                    stringResource(R.string.gym_gym_keine_uebungen),
                    color = Secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allExercises, key = { it.uuid }) { exercise ->
                        val isSelected = exercise.name in selectedNames
                        LMCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedNames.remove(exercise.name)
                                        else selectedNames.add(exercise.name)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedNames.remove(exercise.name)
                                        else selectedNames.add(exercise.name)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accent,
                                        uncheckedColor = Secondary
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        exercise.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (exercise.muscleGroup.isNotBlank()) {
                                        Text(
                                            exercise.muscleGroup,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Save button ──
            Button(
                onClick = {
                    if (workoutName.isNotBlank() && selectedNames.isNotEmpty()) {
                        viewModel.addWorkoutTemplate(workoutName.trim(), selectedNames.toList())
                        navController.popBackStack()
                    }
                },
                enabled = workoutName.isNotBlank() && selectedNames.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.gym_gym_workout_speichern), color = Black)
            }
        }
    }
}

