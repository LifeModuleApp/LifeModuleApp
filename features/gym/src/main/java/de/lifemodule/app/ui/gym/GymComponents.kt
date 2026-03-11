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
import de.lifemodule.app.ui.components.CommunityBadge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.lifemodule.app.data.gym.SessionSetEntity
import de.lifemodule.app.feature.gym.R
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Border
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Secondary

/**
 * Parses note-style set input like "7x30, 3x50" into list of (reps, weight?).
 */
fun parseNoteInput(input: String): List<Pair<Int, Double?>> {
    if (input.isBlank()) return emptyList()
    return input.split(",", ";", " ").mapNotNull { part ->
        val trimmed = part.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        val xIndex = trimmed.indexOfFirst { it == 'x' || it == 'X' || it == '×' }
        if (xIndex > 0) {
            val reps = trimmed.substring(0, xIndex).trim().toIntOrNull() ?: return@mapNotNull null
            val weight = trimmed.substring(xIndex + 1).trim().toDoubleOrNull()
            reps to weight
        } else {
            val reps = trimmed.toIntOrNull() ?: return@mapNotNull null
            reps to null
        }
    }
}

/**
 * Parses cardio duration input:
 *   "H:MM:SS" or "HH:MM:SS" -> total seconds
 *   "MM:SS"                  -> total seconds
 *   "30"                     -> 30 min = 1800 s
 */
fun parseCardioDuration(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    if (':' in trimmed) {
        val parts = trimmed.split(':')
        return when (parts.size) {
            3 -> {
                val h = parts[0].trim().toIntOrNull() ?: return null
                val m = parts[1].trim().toIntOrNull() ?: return null
                val s = parts[2].trim().toIntOrNull() ?: 0
                h * 3600 + m * 60 + s
            }
            2 -> {
                val m = parts[0].trim().toIntOrNull() ?: return null
                val s = parts[1].trim().toIntOrNull() ?: 0
                m * 60 + s
            }
            else -> null
        }
    }
    return trimmed.toIntOrNull()?.let { it * 60 }
}

/**
 * Formats seconds into "H:MM:SS" (or "M:SS" when < 1 hour).
 */
fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}

/**
 * Mobile-friendly cardio duration picker dialog (h / min / sec fields).
 * Shows a live preview of the formatted duration as you type.
 */
@Composable
fun CardioDurationDialog(
    initialSeconds: Int = 0,
    accent: Color,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hoursText   by remember { mutableStateOf(if (initialSeconds >= 3600) (initialSeconds / 3600).toString() else "") }
    var minutesText by remember {
        val m = (initialSeconds % 3600) / 60
        mutableStateOf(if (m > 0 || initialSeconds >= 60) m.toString() else "")
    }
    var secondsText by remember {
        val s = initialSeconds % 60
        mutableStateOf(if (s > 0) s.toString() else "")
    }

    val totalSeconds = (hoursText.toIntOrNull() ?: 0) * 3600 +
                       (minutesText.toIntOrNull() ?: 0) * 60 +
                       (secondsText.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timer, null, tint = accent) },
        title = { Text(stringResource(R.string.gym_gym_dauer_eingabe)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Live preview
                if (totalSeconds > 0) {
                    Text(
                        text = formatDuration(totalSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { v ->
                            if (v.length <= 2 && (v.isEmpty() || v.toIntOrNull() != null)) hoursText = v
                        },
                        label = { Text("h", color = Secondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = Border,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", color = Secondary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { v ->
                            val n = v.toIntOrNull()
                            if (v.isEmpty() || (n != null && n < 60)) minutesText = v
                        },
                        label = { Text("min", color = Secondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = Border,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", color = Secondary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { v ->
                            val n = v.toIntOrNull()
                            if (v.isEmpty() || (n != null && n < 60)) secondsText = v
                        },
                        label = { Text("sek", color = Secondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = Border,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (totalSeconds > 0) onConfirm(totalSeconds) },
                enabled = totalSeconds > 0,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text(stringResource(R.string.gym_gym_speichern), color = Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.gym_gym_abbrechen)) }
        }
    )
}

/**
 * Expandable exercise card with inline set logging.
 * Supports both strength (reps×weight) and cardio (duration) modes.
 * Features: edit-mode (tap set to edit), photo display.
 * Cardio uses the CardioDurationDialog (h/min/sec) for user-friendly input.
 */
@Composable
fun ExerciseLogCard(
    exerciseName: String,
    category: String,
    muscleGroup: String,
    sets: List<SessionSetEntity>,
    accent: Color,
    onLogSets: (List<Pair<Int, Double?>>) -> Unit,
    onLogCardio: ((Int) -> Unit)? = null,
    onDeleteSet: (SessionSetEntity) -> Unit,
    onUpdateSet: ((SessionSetEntity) -> Unit)? = null,
    onDeleteExercise: (() -> Unit)? = null,
    onEditExercise: (() -> Unit)? = null,
    imagePath: String? = null,
    initiallyExpanded: Boolean = false,
    isImported: Boolean = false
) {
    val isCardio = category.equals("cardio", ignoreCase = true)
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var noteInput by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    /** Edit-mode: currently-editing set, null = adding new */
    var editingSet by remember { mutableStateOf<SessionSetEntity?>(null) }

    /** Cardio picker dialog state */
    var showCardioPicker by remember { mutableStateOf(false) }
    var cardioEditInitialSec by remember { mutableStateOf(0) }

    if (confirmDelete && onDeleteExercise != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.gym_gym_uebung_loeschen)) },
            text = { Text(stringResource(R.string.gym_gym_uebung_loeschen_text, exerciseName)) },
            confirmButton = {
                TextButton(onClick = { onDeleteExercise(); confirmDelete = false }) {
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

    // ── Cardio duration picker ──
    if (showCardioPicker) {
        CardioDurationDialog(
            initialSeconds = cardioEditInitialSec,
            accent = accent,
            onConfirm = { totalSec ->
                if (editingSet != null) {
                    onUpdateSet?.invoke(editingSet!!.copy(durationSeconds = totalSec))
                    editingSet = null
                } else {
                    onLogCardio?.invoke(totalSec)
                }
                showCardioPicker = false
                cardioEditInitialSec = 0
            },
            onDismiss = {
                showCardioPicker = false
                cardioEditInitialSec = 0
                editingSet = null
            }
        )
    }

    LMCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(14.dp)
        ) {
            // ── Header row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!imagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(imagePath),
                        contentDescription = exerciseName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isImported) {
                            Spacer(Modifier.width(6.dp))
                            CommunityBadge()
                        }
                    }
                    if (muscleGroup.isNotBlank()) {
                        Text(
                            text = muscleGroup,
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                    }
                }
                if (sets.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.gym_gym_heute_sets, sets.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.gym_gym_keine_sets_heute),
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Secondary
                )
            }

            // ── Expanded: sets + input ──
            if (expanded) {
                Spacer(Modifier.height(10.dp))

                if (isCardio) {
                    // Cardio sets: show H:MM:SS, tap to edit
                    sets.filter { it.durationSeconds != null }.forEachIndexed { idx, set ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (onUpdateSet != null) {
                                        editingSet = set
                                        cardioEditInitialSec = set.durationSeconds ?: 0
                                        showCardioPicker = true
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.gym_gym_satz_nummer, idx + 1),
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                            Text(
                                text = formatDuration(set.durationSeconds ?: 0),
                                style = MaterialTheme.typography.bodyMedium,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { onDeleteSet(set) },
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    stringResource(R.string.gym_gym_satz_loeschen),
                                    tint = Destructive.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                } else {
                    // Strength sets: show reps × weight
                    sets.filter { it.reps != null }.forEachIndexed { idx, set ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (onUpdateSet != null) {
                                        editingSet = set
                                        noteInput = "${set.reps}x${set.weightKg?.let {
                                            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                                        } ?: ""}"
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.gym_gym_satz_nummer, idx + 1),
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                            Text(
                                text = stringResource(
                                    R.string.gym_gym_satz_format,
                                    set.reps.toString(),
                                    set.weightKg?.let { stringResource(R.string.gym_gym_gewicht_kg, it.toString()) }
                                        ?: stringResource(R.string.gym_gym_gewicht_unbekannt)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (editingSet?.uuid == set.uuid) accent.copy(alpha = 0.6f) else accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.weight(1f))
                            if (editingSet?.uuid == set.uuid) {
                                Icon(Icons.Default.Edit, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            IconButton(
                                onClick = { onDeleteSet(set) },
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    stringResource(R.string.gym_gym_satz_loeschen),
                                    tint = Destructive.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Input ──
                if (isCardio) {
                    // Cardio: button opens the h/min/sec dialog
                    Button(
                        onClick = {
                            cardioEditInitialSec = 0
                            editingSet = null
                            showCardioPicker = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(Icons.Default.Timer, null, tint = Black)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.gym_gym_dauer_eingabe), color = Black)
                    }
                } else {
                    // Strength: text input (note-style: "7x30, 3x50")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = {
                                Text(
                                    if (editingSet != null) stringResource(R.string.gym_gym_satz_bearbeiten)
                                    else stringResource(R.string.gym_gym_saetze_eingabe),
                                    color = Secondary
                                )
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.gym_gym_saetze_placeholder),
                                    color = Secondary.copy(alpha = 0.4f)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (editingSet != null) accent.copy(alpha = 0.7f) else accent,
                                unfocusedBorderColor = Border,
                                focusedTextColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Secondary,
                                focusedLabelColor = accent
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        if (editingSet != null) {
                            Button(
                                onClick = {
                                    val setToEdit = editingSet ?: return@Button
                                    val entries = parseNoteInput(noteInput)
                                    if (entries.isNotEmpty()) {
                                        val (reps, weight) = entries.first()
                                        onUpdateSet?.invoke(setToEdit.copy(reps = reps, weightKg = weight))
                                    }
                                    editingSet = null
                                    noteInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Default.Edit, stringResource(R.string.gym_gym_satz_aktualisieren), tint = Black)
                            }
                            IconButton(onClick = { editingSet = null; noteInput = "" }) {
                                Text("X", color = Secondary)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val entries = parseNoteInput(noteInput)
                                    if (entries.isNotEmpty()) {
                                        onLogSets(entries)
                                        noteInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Icon(Icons.Default.Add, stringResource(R.string.gym_gym_satz_hinzufuegen), tint = Black)
                            }
                        }
                    }
                }

                // Edit / Delete exercise buttons
                if (onEditExercise != null || onDeleteExercise != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (onEditExercise != null) {
                            TextButton(onClick = onEditExercise) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = accent)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.gym_gym_bearbeiten), color = accent)
                            }
                        }
                        if (onDeleteExercise != null) {
                            TextButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Destructive.copy(alpha = 0.7f))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.gym_gym_entfernen), color = Destructive.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}
