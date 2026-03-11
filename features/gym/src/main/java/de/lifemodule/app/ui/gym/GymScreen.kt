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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.data.ImportSource
import de.lifemodule.app.data.gym.ExerciseDefinitionEntity
import de.lifemodule.app.data.gym.SessionSetEntity
import de.lifemodule.app.data.gym.WorkoutWithTemplateExercises
import de.lifemodule.app.feature.gym.R
import de.lifemodule.app.ui.components.CommunityBadge
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Border
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun GymScreen(
    navController: NavController,
    viewModel: GymViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val exerciseDefs by viewModel.exerciseDefinitions.collectAsStateWithLifecycle()
    val templates by viewModel.workoutTemplates.collectAsStateWithLifecycle()
    val todaySets by viewModel.todaySets.collectAsStateWithLifecycle()

    // Group today's sets by exercise name
    val todaySetsByExercise = remember(todaySets) {
        todaySets.groupBy { it.exerciseName }
    }

    // Coroutine scope for workout session creation
    val scope = rememberCoroutineScope()
    val fallbackName = stringResource(R.string.gym_gym_fallback_kraft)

    // ── Add Exercise dialog ──
    var showAddExercise by remember { mutableStateOf(false) }
    var newExName by remember { mutableStateOf("") }
    var newExCategory by remember { mutableStateOf("strength") }
    var newExMuscleGroup by remember { mutableStateOf("") }
    var newExImageUri by remember { mutableStateOf<Uri?>(null) }

    // ── Edit Exercise dialog ──
    var editExercise by remember { mutableStateOf<ExerciseDefinitionEntity?>(null) }
    var editExName by remember { mutableStateOf("") }
    var editExCategory by remember { mutableStateOf("strength") }
    var editExMuscleGroup by remember { mutableStateOf("") }
    var editExImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> newExImageUri = uri }

    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> editExImageUri = uri }

    if (showAddExercise) {
        AlertDialog(
            onDismissRequest = { showAddExercise = false },
            title = { Text(stringResource(R.string.gym_gym_uebung_erstellen)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LMInput(
                        value = newExName,
                        onValueChange = { newExName = it },
                        label = stringResource(R.string.gym_gym_uebung_name)
                    )
                    Text(
                        stringResource(R.string.gym_gym_kategorie),
                        style = MaterialTheme.typography.labelMedium,
                        color = Secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { newExCategory = "strength" },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (newExCategory == "strength") accent.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                        ) {
                            Text(
                                stringResource(R.string.gym_gym_kraft_1),
                                color = if (newExCategory == "strength") accent else Secondary
                            )
                        }
                        OutlinedButton(
                            onClick = { newExCategory = "cardio" },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (newExCategory == "cardio") accent.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                        ) {
                            Text(
                                stringResource(R.string.gym_gym_cardio_1),
                                color = if (newExCategory == "cardio") accent else Secondary
                            )
                        }
                    }
                    LMInput(
                        value = newExMuscleGroup,
                        onValueChange = { newExMuscleGroup = it },
                        label = stringResource(R.string.gym_gym_muskelgruppe)
                    )
                    // Photo picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = accent)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.gym_gym_foto_hinzufuegen),
                                color = accent
                            )
                        }
                        if (newExImageUri != null) {
                            AsyncImage(
                                model = newExImageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addExerciseDefinition(
                            name = newExName.trim(),
                            category = newExCategory,
                            muscleGroup = newExMuscleGroup.trim(),
                            imagePath = newExImageUri?.toString()
                        )
                        newExName = ""; newExCategory = "strength"; newExMuscleGroup = ""
                        newExImageUri = null
                        showAddExercise = false
                    },
                    enabled = newExName.isNotBlank()
                ) {
                    Text(stringResource(R.string.gym_gym_speichern), color = accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExercise = false }) {
                    Text(stringResource(R.string.gym_gym_abbrechen))
                }
            }
        )
    }

    // ── Edit Exercise AlertDialog ──
    if (editExercise != null) {
        AlertDialog(
            onDismissRequest = { editExercise = null },
            title = { Text(stringResource(R.string.gym_gym_uebung_bearbeiten)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LMInput(
                        value = editExName,
                        onValueChange = { editExName = it },
                        label = stringResource(R.string.gym_gym_uebung_name)
                    )
                    Text(
                        stringResource(R.string.gym_gym_kategorie),
                        style = MaterialTheme.typography.labelMedium,
                        color = Secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { editExCategory = "strength" },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (editExCategory == "strength") accent.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                        ) {
                            Text(
                                stringResource(R.string.gym_gym_kraft_1),
                                color = if (editExCategory == "strength") accent else Secondary
                            )
                        }
                        OutlinedButton(
                            onClick = { editExCategory = "cardio" },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (editExCategory == "cardio") accent.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                        ) {
                            Text(
                                stringResource(R.string.gym_gym_cardio_1),
                                color = if (editExCategory == "cardio") accent else Secondary
                            )
                        }
                    }
                    LMInput(
                        value = editExMuscleGroup,
                        onValueChange = { editExMuscleGroup = it },
                        label = stringResource(R.string.gym_gym_muskelgruppe)
                    )
                    // Photo picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editImagePickerLauncher.launch("image/*") }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = accent)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.gym_gym_foto_hinzufuegen),
                                color = accent
                            )
                        }
                        val displayUri = editExImageUri ?: editExercise?.imagePath?.let { Uri.parse(it) }
                        if (displayUri != null) {
                            AsyncImage(
                                model = displayUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editExercise?.let { def ->
                            viewModel.updateExerciseDefinition(
                                def.copy(
                                    name = editExName.trim(),
                                    category = editExCategory,
                                    muscleGroup = editExMuscleGroup.trim(),
                                    imagePath = editExImageUri?.toString() ?: def.imagePath
                                )
                            )
                        }
                        editExercise = null
                        editExImageUri = null
                    },
                    enabled = editExName.isNotBlank()
                ) {
                    Text(stringResource(R.string.gym_gym_speichern), color = accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { editExercise = null; editExImageUri = null }) {
                    Text(stringResource(R.string.gym_gym_abbrechen))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.gym_gym_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(
                onClick = {
                    if (selectedTab == 0) showAddExercise = true
                    else navController.navigate(AppRoute.GymTemplateAdd)
                }
            )
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Black,
                contentColor = accent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            stringResource(R.string.gym_gym_tab_quick_log),
                            color = if (selectedTab == 0) accent else Secondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            stringResource(R.string.gym_gym_tab_workouts),
                            color = if (selectedTab == 1) accent else Secondary
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> QuickLogTab(
                    exercises = exerciseDefs,
                    todaySetsByExercise = todaySetsByExercise,
                    accent = accent,
                    fallbackSessionName = fallbackName,
                    viewModel = viewModel,
                    onEditExercise = { exercise ->
                        editExName = exercise.name
                        editExCategory = exercise.category
                        editExMuscleGroup = exercise.muscleGroup
                        editExImageUri = null
                        editExercise = exercise
                    }
                )
                1 -> WorkoutsTab(
                    templates = templates,
                    accent = accent,
                    scope = scope,
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}

// ── Tab 1: Quick Log (exercise catalog with inline logging) ───────────────────

@Composable
private fun QuickLogTab(
    exercises: List<ExerciseDefinitionEntity>,
    todaySetsByExercise: Map<String, List<SessionSetEntity>>,
    accent: Color,
    fallbackSessionName: String,
    viewModel: GymViewModel,
    onEditExercise: (ExerciseDefinitionEntity) -> Unit
) {
    if (exercises.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.gym_gym_keine_uebungen),
                color = Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        items(exercises, key = { it.uuid }) { exercise ->
            val exerciseSets = todaySetsByExercise[exercise.name] ?: emptyList()
            ExerciseLogCard(
                exerciseName = exercise.name,
                category = exercise.category,
                muscleGroup = exercise.muscleGroup,
                sets = exerciseSets,
                accent = accent,
                onLogSets = { entries ->
                    viewModel.logSetsForExercise(exercise.name, entries, fallbackSessionName)
                },
                onLogCardio = { seconds ->
                    viewModel.logCardioForExercise(exercise.name, seconds, fallbackSessionName)
                },
                onDeleteSet = { set -> viewModel.deleteSet(set) },
                onUpdateSet = { set -> viewModel.updateSet(set) },
                onDeleteExercise = { viewModel.deleteExerciseDefinition(exercise) },
                onEditExercise = { onEditExercise(exercise) },
                imagePath = exercise.imagePath,
                isImported = exercise.importSource == ImportSource.COMMUNITY_HUB
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Tab 2: Workouts (playlists) ───────────────────────────────────────────────

@Composable
private fun WorkoutsTab(
    templates: List<WorkoutWithTemplateExercises>,
    accent: Color,
    scope: CoroutineScope,
    navController: NavController,
    viewModel: GymViewModel
) {
    if (templates.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.gym_gym_keine_workouts),
                color = Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        items(templates, key = { it.workout.uuid }) { template ->
            WorkoutCard(
                template = template,
                accent = accent,
                onStart = {
                    scope.launch {
                        val sessionId = viewModel.createWorkoutSession(
                            workoutName = template.workout.name,
                            workoutTemplateId = template.workout.uuid
                        )
                        navController.navigate(
                            AppRoute.GymWorkoutSession(
                                sessionId = sessionId,
                                workoutId = template.workout.uuid
                            )
                        )
                    }
                },
                onDelete = { viewModel.deleteWorkoutTemplate(template.workout) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun WorkoutCard(
    template: WorkoutWithTemplateExercises,
    accent: Color,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.gym_gym_workout_loeschen)) },
            text = { Text(stringResource(R.string.gym_gym_workout_loeschen_text, template.workout.name)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); confirmDelete = false }) {
                    Text(stringResource(R.string.gym_gym_loeschen), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.gym_gym_abbrechen))
                }
            }
        )
    }

    LMCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = template.workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (template.workout.importSource == ImportSource.COMMUNITY_HUB) {
                    Spacer(Modifier.width(6.dp))
                    CommunityBadge()
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onStart) {
                    Icon(
                        Icons.Default.PlayArrow,
                        stringResource(R.string.gym_gym_training_starten),
                        tint = accent
                    )
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Default.Delete,
                        stringResource(R.string.gym_gym_loeschen),
                        tint = Destructive.copy(alpha = 0.7f)
                    )
                }
            }

            if (template.exerciseNamesInOrder.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = template.exerciseNamesInOrder.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Secondary
                )
            }
        }
    }
}

