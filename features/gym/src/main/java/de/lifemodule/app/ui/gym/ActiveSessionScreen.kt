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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.data.gym.SessionSetEntity
import de.lifemodule.app.feature.gym.R
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Border
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import kotlinx.coroutines.delay

@Composable
fun ActiveSessionScreen(
    navController: NavController,
    viewModel: GymSessionViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val setsByExercise by viewModel.setsByExercise.collectAsStateWithLifecycle()
    val allSets by viewModel.sets.collectAsStateWithLifecycle()
    val isCardio = session?.type == "cardio"

    // ── Session Timer ─────────────────────────────────────────────────────
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }
    val timerText = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    // ── New exercise input state ───────────────────────────────────────────
    var newExerciseName by remember { mutableStateOf("") }

    // ── Per-exercise note-style input (strength) ──────────────────────────
    val noteInputState = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }

    // ── Finish dialog ─────────────────────────────────────────────────────
    var showFinishDialog by remember { mutableStateOf(false) }
    if (showFinishDialog) {
        FinishSessionDialog(
            onConfirm = {
                viewModel.finishSession(durationMinutes = elapsedSeconds / 60)
                navController.popBackStack()
            },
            onDismiss = { showFinishDialog = false }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = session?.name ?: stringResource(R.string.gym_gym_training),
                onBackClick = { showFinishDialog = true }
            )
        },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header: timer + type ──────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isCardio) stringResource(R.string.gym_gym_cardio)
                                       else stringResource(R.string.gym_gym_kraft),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                            Text(
                                text = stringResource(R.string.gym_gym_saetze_gesamt, allSets.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                        }
                        Text(
                            text = "⏱ $timerText",
                            style = MaterialTheme.typography.headlineMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Exercise blocks ───────────────────────────────────────
            val exerciseList = setsByExercise.entries.toList()
            itemsIndexed(exerciseList, key = { _, entry -> entry.key }) { _, entry ->
                val exerciseName = entry.key
                val sets = entry.value
                val orderIndex = sets.minOfOrNull { it.exerciseOrderIndex } ?: 0

                if (isCardio) {
                    CardioExerciseBlock(
                        exerciseName = exerciseName,
                        sets = sets,
                        accent = accent,
                        onSaveDuration = { seconds ->
                            viewModel.logCardioSet(exerciseName, orderIndex, seconds)
                        },
                        onDeleteSet = { viewModel.deleteSet(it) }
                    )
                } else {
                    val noteInput = noteInputState.getOrDefault(exerciseName, "")
                    StrengthExerciseBlock(
                        exerciseName = exerciseName,
                        sets = sets,
                        noteInput = noteInput,
                        accent = accent,
                        onNoteChange = { noteInputState[exerciseName] = it },
                        onAddSets = { entries ->
                            viewModel.logSets(exerciseName, orderIndex, entries)
                            noteInputState[exerciseName] = ""
                        },
                        onDeleteSet = { viewModel.deleteSet(it) }
                    )
                }
            }

            // ── Add new exercise ──────────────────────────────────────
            item {
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LMInput(
                            value = newExerciseName,
                            onValueChange = { newExerciseName = it },
                            label = if (isCardio) stringResource(R.string.gym_gym_geraet_hinzufuegen)
                                    else stringResource(R.string.gym_gym_neue_uebung_hinzufuegen),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newExerciseName.isNotBlank()) {
                                    val orderIndex = setsByExercise.size
                                    noteInputState[newExerciseName.trim()] = ""
                                    viewModel.logSet(
                                        exerciseName = newExerciseName.trim(),
                                        exerciseOrderIndex = orderIndex,
                                        reps = null,
                                        weightKg = null
                                    )
                                    newExerciseName = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, stringResource(R.string.gym_gym_uebung_hinzufuegen), tint = accent)
                        }
                    }
                }
            }

            // ── Finish button ─────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showFinishDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(stringResource(R.string.gym_gym_training_beenden), color = Black, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ── Strength Exercise Block (Note-style input) ───────────────────────────────

// parseNoteInput is shared from GymComponents.kt

@Composable
private fun StrengthExerciseBlock(
    exerciseName: String,
    sets: List<SessionSetEntity>,
    noteInput: String,
    accent: Color,
    onNoteChange: (String) -> Unit,
    onAddSets: (List<Pair<Int, Double?>>) -> Unit,
    onDeleteSet: (SessionSetEntity) -> Unit
) {
    val realSets = sets.filter { it.reps != null || it.weightKg != null }

    LMCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (realSets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                realSets.forEach { set ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.gym_gym_satz_nummer, set.setNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Text(
                            text = stringResource(
                                R.string.gym_gym_satz_format,
                                set.reps?.toString() ?: stringResource(R.string.gym_gym_reps_unbekannt),
                                set.weightKg?.let { stringResource(R.string.gym_gym_gewicht_kg, it.toString()) }
                                    ?: stringResource(R.string.gym_gym_gewicht_unbekannt)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onDeleteSet(set) }, modifier = Modifier.padding(0.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.gym_gym_satz_loeschen),
                                tint = Destructive.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Note-style input: "7x30, 3x50"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.gym_gym_saetze_eingabe), color = Secondary) },
                    placeholder = { Text(stringResource(R.string.gym_gym_saetze_placeholder), color = Secondary.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Border,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Secondary,
                        focusedLabelColor = accent
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val entries = parseNoteInput(noteInput)
                        if (entries.isNotEmpty()) onAddSets(entries)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.gym_gym_satz_hinzufuegen), tint = Black)
                }
            }
        }
    }
}

// ── Cardio Exercise Block (Built-in Timer) ───────────────────────────────────

@Composable
private fun CardioExerciseBlock(
    exerciseName: String,
    sets: List<SessionSetEntity>,
    accent: Color,
    onSaveDuration: (Int) -> Unit,
    onDeleteSet: (SessionSetEntity) -> Unit
) {
    val realSets = sets.filter { it.durationSeconds != null }

    // Built-in timer state
    var isRunning by remember { mutableStateOf(false) }
    var timerStartMs by remember { mutableLongStateOf(0L) }
    var elapsedSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            timerStartMs = System.currentTimeMillis() - (elapsedSec * 1000L)
            while (isRunning) {
                elapsedSec = ((System.currentTimeMillis() - timerStartMs) / 1000).toInt()
                delay(500L)
            }
        }
    }

    LMCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            // Already logged durations
            if (realSets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                realSets.forEach { set ->
                    val sec = set.durationSeconds ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.gym_gym_satz_nummer, set.setNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Text(
                            text = formatElapsedTime(sec),
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onDeleteSet(set) }, modifier = Modifier.padding(0.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.gym_gym_satz_loeschen),
                                tint = Destructive.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Timer display
            Text(
                text = formatElapsedTime(elapsedSec),
                fontSize = 36.sp,
                color = if (isRunning) accent else Secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))

            // Timer controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Destructive else accent
                    )
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = if (isRunning) Color.White else Black
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isRunning) stringResource(R.string.gym_gym_stopp)
                        else stringResource(R.string.gym_gym_start_timer),
                        color = if (isRunning) Color.White else Black
                    )
                }

                if (!isRunning && elapsedSec > 0) {
                    Button(
                        onClick = {
                            onSaveDuration(elapsedSec)
                            elapsedSec = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(Icons.Default.Save, null, tint = Black)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.gym_gym_speichern), color = Black)
                    }
                }
            }
        }
    }
}

private fun formatElapsedTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

// ── Finish Dialog ─────────────────────────────────────────────────────────────

@Composable
private fun FinishSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalAccentColor.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gym_gym_training_beenden_1)) },
        text = { Text(stringResource(R.string.gym_gym_dein_training_wird_mit_der)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.gym_gym_speichern), color = Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.gym_gym_weiter_trainieren))
            }
        }
    )
}
