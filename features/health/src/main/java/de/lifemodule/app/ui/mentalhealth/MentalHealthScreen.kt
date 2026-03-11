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

package de.lifemodule.app.ui.mentalhealth

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.lifemodule.app.data.mentalhealth.MoodEntryEntity
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.feature.health.R
import androidx.compose.ui.res.stringResource

@Composable
fun MentalHealthScreen(
    navController: NavController,
    viewModel: MoodViewModel = hiltViewModel()
) {
    val todayEntry by viewModel.todayEntry.collectAsStateWithLifecycle()
    val recentEntries by viewModel.recentEntries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.health_mentalhealth_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { navController.navigate(AppRoute.MentalHealthAdd) })
        },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today's mood card
            item {
                Text(
                    text = stringResource(R.string.health_mentalhealth_heute),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                if (todayEntry != null) {
                    TodayMoodCard(entry = todayEntry!!)
                } else {
                    LMCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.health_mentalhealth_noch_kein_eintrag_heute),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Secondary
                            )
                            Text(
                                text = stringResource(R.string.health_mentalhealth_tippe_um_deinen_mood_zu),
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                        }
                    }
                }
            }

            // Recent history
            if (recentEntries.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.health_mentalhealth_letzte_eintraege),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(items = recentEntries) { entry ->
                    MoodHistoryCard(
                        entry = entry,
                        onEdit = { /* TODO: Navigate to edit screen or show edit dialog */ },
                        onDelete = { viewModel.deleteEntry(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayMoodCard(entry: MoodEntryEntity) {
    LMCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = moodEmoji(entry.moodLevel),
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = moodLabel(entry.moodLevel),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.health_mentalhealth_mood_format, entry.moodLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip("⚡", stringResource(R.string.health_mentalhealth_energie), entry.energyLevel)
                StatChip("😤", stringResource(R.string.health_mentalhealth_stress), entry.stressLevel)
                StatChip("😴", stringResource(R.string.health_mentalhealth_schlaf), entry.sleepQuality)
            }
            if (entry.positiveNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✅ ${entry.positiveNotes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary
                )
            }
            if (entry.negativeNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "❌ ${entry.negativeNotes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary
                )
            }
        }
    }
}

@Composable
private fun MoodHistoryCard(
    entry: MoodEntryEntity,
    onEdit: (MoodEntryEntity) -> Unit = {},
    onDelete: (MoodEntryEntity) -> Unit = {}
) {
    val accent = LocalAccentColor.current
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.health_mentalhealth_eintrag_loeschen)) },
            text = { Text(stringResource(R.string.health_mentalhealth_eintrag_loeschen_bestaetigung)) },
            confirmButton = {
                TextButton(onClick = { onDelete(entry); confirmDelete = false }) {
                    Text(stringResource(R.string.health_mentalhealth_loeschen), color = Color(0xFFFF453A))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.health_mentalhealth_abbrechen))
                }
            }
        )
    }

    LMCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = moodEmoji(entry.moodLevel), fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Secondary
                )
                Text(
                    text = moodLabel(entry.moodLevel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.health_mentalhealth_score_format, entry.moodLevel),
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            IconButton(onClick = { onEdit(entry) }) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.health_mentalhealth_bearbeiten), tint = Secondary)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.health_mentalhealth_loeschen), tint = Color(0xFFFF453A).copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun StatChip(emoji: String, label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = stringResource(R.string.health_mentalhealth_score_format, value),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Secondary
        )
    }
}

private fun moodEmoji(level: Int): String = when (level) {
    1 -> "😭"
    2 -> "😢"
    3 -> "😞"
    4 -> "😕"
    5 -> "😐"
    6 -> "🙂"
    7 -> "😊"
    8 -> "😄"
    9 -> "🤩"
    10 -> "🔥"
    else -> "😐"
}

@Composable
private fun moodLabel(level: Int): String = when (level) {
    in 1..2 -> stringResource(R.string.health_mentalhealth_mood_sehr_schlecht)
    in 3..4 -> stringResource(R.string.health_mentalhealth_mood_nicht_so_gut)
    5 -> stringResource(R.string.health_mentalhealth_mood_neutral)
    in 6..7 -> stringResource(R.string.health_mentalhealth_mood_gut)
    in 8..9 -> stringResource(R.string.health_mentalhealth_mood_sehr_gut)
    10 -> stringResource(R.string.health_mentalhealth_mood_fantastisch)
    else -> stringResource(R.string.health_mentalhealth_mood_neutral)
}
