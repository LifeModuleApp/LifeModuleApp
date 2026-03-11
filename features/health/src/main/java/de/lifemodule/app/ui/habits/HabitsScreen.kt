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

package de.lifemodule.app.ui.habits

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.CommunityBadge
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.data.ImportSource
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Secondary
import java.time.format.DateTimeFormatter
import java.util.Locale
import de.lifemodule.app.feature.health.R
import androidx.compose.ui.res.stringResource

@Composable
fun HabitsScreen(
    navController: NavController,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
    val activeHabits by viewModel.activeHabits.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val streaks by viewModel.streaks.collectAsStateWithLifecycle()

    LaunchedEffect(activeHabits) {
        if (activeHabits.isNotEmpty()) {
            viewModel.ensureTodayLogs()
        }
        viewModel.refreshStreaks()
    }

    val completedCount = todayLogs.count { it.log.completed }
    val totalCount = todayLogs.size

    val dateText = selectedDate.format(
        DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(Locale.getDefault())
    )

    // Delete or Archive confirmation
    var pendingActionHabit by remember { mutableStateOf<de.lifemodule.app.data.habits.HabitEntity?>(null) }
    pendingActionHabit?.let { habit ->
        AlertDialog(
            onDismissRequest = { pendingActionHabit = null },
            title = { Text(stringResource(R.string.health_habits_gewohnheit_loeschen)) },
            text = { Text(stringResource(R.string.health_habits_gewohnheit_loeschen_text)) },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.archiveHabit(habit)
                    pendingActionHabit = null 
                }) {
                    Text(stringResource(R.string.health_habits_archivieren), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { pendingActionHabit = null }) {
                        Text(stringResource(R.string.health_habits_abbrechen), color = Secondary)
                    }
                    TextButton(onClick = { 
                        viewModel.deleteHabit(habit)
                        pendingActionHabit = null 
                    }) {
                        Text(stringResource(R.string.health_habits_loeschen_total), color = Destructive)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.health_habits_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { navController.navigate(AppRoute.HabitsAdd) })
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
            item {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Progress
            item {
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.health_habits_erledigt_format, completedCount, totalCount),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (completedCount == totalCount && totalCount > 0) accent
                            else MaterialTheme.colorScheme.primary
                        )
                        if (totalCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (completedCount == totalCount) stringResource(R.string.health_habits_perfekter_tag)
                                else stringResource(R.string.health_habits_noch_offen, totalCount - completedCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                        }
                    }
                }
            }

            if (todayLogs.isEmpty() && activeHabits.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.health_habits_noch_keine_habits_eingerichtet_tippe),
                        color = Secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }

            items(todayLogs, key = { it.log.uuid }) { logWithHabit ->
                val log = logWithHabit.log
                val habit = logWithHabit.habit
                val streak = streaks[habit.uuid] ?: 0

                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle
                        IconButton(onClick = { viewModel.toggleCompleted(log) }) {
                            Icon(
                                imageVector = if (log.completed) Icons.Filled.CheckCircle
                                else Icons.Outlined.Circle,
                                contentDescription = if (log.completed) stringResource(R.string.health_habits_erledigt) else stringResource(R.string.health_habits_offen),
                                tint = if (log.completed) accent else Secondary
                            )
                        }

                        // Thumbnail
                        if (!habit.imagePath.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(habit.imagePath),
                                contentDescription = habit.name,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = habit.emoji,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (log.completed) Secondary else MaterialTheme.colorScheme.onSurface
                                )
                                if (habit.importSource == ImportSource.COMMUNITY_HUB) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CommunityBadge()
                                }
                            }
                            Row {
                                if (!habit.isPositive) {
                                    Text(
                                        text = stringResource(R.string.health_habits_vermeiden),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Destructive
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = timeOfDayLabel(habit.timeOfDay),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                                if (habit.repeatIntervalDays > 1) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.health_habits_intervall_tage, habit.repeatIntervalDays),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Secondary
                                    )
                                }
                            }
                        }

                        // Streak badge
                        if (streak > 0) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text("🔥", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "$streak",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent
                                )
                            }
                        }
                        // Delete / Archive button at the end
                        IconButton(onClick = { pendingActionHabit = habit }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.health_habits_loeschen),
                                tint = Destructive
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun timeOfDayLabel(timeOfDay: String): String = when (timeOfDay) {
    "morning" -> stringResource(R.string.health_habits_morgens_1)
    "noon" -> stringResource(R.string.health_habits_mittags_1)
    "evening" -> stringResource(R.string.health_habits_abends_1)
    else -> stringResource(R.string.health_habits_jederzeit_1)
}
