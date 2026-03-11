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

package de.lifemodule.app.ui.worktime

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.feature.planner.R
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMEmptyState
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun WorkTimeScreen(
    navController: NavController,
    viewModel: WorkTimeViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val activeEntry by viewModel.activeEntry.collectAsStateWithLifecycle()
    val monthEntries by viewModel.currentMonthEntries.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dtf = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Live timer state
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeEntry) {
        if (activeEntry != null) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    // Delete confirmation
    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    pendingDelete?.let { onConfirm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.planner_worktime_eintrag_loeschen)) },
            text = { Text(stringResource(R.string.planner_worktime_eintrag_loeschen_bestaetigung)) },
            confirmButton = {
                TextButton(onClick = { onConfirm(); pendingDelete = null }) {
                    Text(stringResource(R.string.planner_worktime_loeschen), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.planner_worktime_abbrechen))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.planner_worktime_titel),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Tab selector ──────────────────────────────────────────────
            item {
                val tabs = listOf(
                    Triple(Icons.Default.Timer, stringResource(R.string.planner_worktime_live_timer), 0),
                    Triple(Icons.Default.Edit, stringResource(R.string.planner_worktime_manuell), 1),
                    Triple(Icons.Default.History, stringResource(R.string.planner_worktime_nachtragen), 2)
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = accent
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEach { (icon, label, index) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, null, modifier = Modifier.padding(end = 4.dp))
                                    Text(label)
                                }
                            },
                            selectedContentColor = accent,
                            unselectedContentColor = Secondary
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // ── Method 1: Live Timer ──────────────────────────────────────
            when (selectedTab) {
                0 -> item {
                    LiveTimerCard(
                        activeEntry = activeEntry,
                        now = now,
                        timeFormat = timeFormat,
                        accent = accent,
                        onClockIn = { viewModel.clockIn() },
                        onClockOut = { activeEntry?.let { viewModel.clockOut(it) } }
                    )
                }

                // ── Method 2: Manual ──────────────────────────────────────
                1 -> item {
                    ManualEntryCard(
                        accent = accent,
                        dtf = dtf,
                        onSave = { start, end, breakMin ->
                            viewModel.insertManualEntry(start, end, breakMin)
                            selectedTab = 0
                        }
                    )
                }

                // ── Method 3: Retroactive ─────────────────────────────────
                2 -> item {
                    RetroactiveEntryCard(
                        accent = accent,
                        dtf = dtf,
                        today = viewModel.today(),
                        onSave = { date, start, end, breakMin, notes ->
                            viewModel.insertRetroactiveEntry(date, start, end, breakMin, notes)
                            selectedTab = 0
                        }
                    )
                }
            }

            // ── Monthly summary ───────────────────────────────────────────
            item {
                val totalMinutes = monthEntries
                    .filter { it.clockOutMillis != null }
                    .sumOf { entry ->
                        TimeUnit.MILLISECONDS.toMinutes(
                            (entry.clockOutMillis ?: 0) - entry.clockInMillis
                        ) - entry.breakMinutes
                    }
                val totalH = totalMinutes / 60
                val totalM = totalMinutes % 60

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.planner_worktime_dieser_monat, totalH, totalM),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── History ───────────────────────────────────────────────────
            val completedEntries = monthEntries.filter { it.clockOutMillis != null }
            if (completedEntries.isEmpty()) {
                item {
                    LMEmptyState(
                        emoji = "⏰",
                        title = stringResource(R.string.planner_worktime_leer_titel),
                        subtitle = stringResource(R.string.planner_worktime_leer_subtitel)
                    )
                }
            }
            items(completedEntries) { entry ->
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val durationMin = TimeUnit.MILLISECONDS.toMinutes(
                                (entry.clockOutMillis ?: 0) - entry.clockInMillis
                            ) - entry.breakMinutes
                            Text(
                                text = stringResource(R.string.planner_worktime_dauer_format, durationMin / 60, durationMin % 60),
                                style = MaterialTheme.typography.titleMedium,
                                color = accent
                            )
                            Text(
                                text = "${entry.date} · ${timeFormat.format(Date(entry.clockInMillis))} - ${timeFormat.format(Date(entry.clockOutMillis!!))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                            if (entry.breakMinutes > 0) {
                                Text(
                                    text = stringResource(R.string.planner_worktime_pause_min, entry.breakMinutes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Secondary.copy(alpha = 0.7f)
                                )
                            }
                            if (entry.notes.isNotBlank()) {
                                Text(
                                    text = entry.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Secondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(onClick = { pendingDelete = { viewModel.deleteEntry(entry) } }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.planner_worktime_loeschen), tint = Secondary)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ── Live Timer Card ───────────────────────────────────────────────────────────

@Composable
private fun LiveTimerCard(
    activeEntry: de.lifemodule.app.data.worktime.WorkTimeEntryEntity?,
    now: Long,
    timeFormat: SimpleDateFormat,
    accent: Color,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    LMCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeEntry != null) {
                val elapsed = now - activeEntry.clockInMillis
                val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60

                Text(
                    text = stringResource(
                        R.string.planner_worktime_laeuft_seit,
                        timeFormat.format(Date(activeEntry.clockInMillis))
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = Secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                    fontSize = 48.sp,
                    color = accent
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClockOut,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Stop, null)
                    Text(
                        stringResource(R.string.planner_worktime_ausstempeln),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.planner_worktime_nicht_eingestempelt),
                    style = MaterialTheme.typography.titleSmall,
                    color = Secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.planner_worktime_timer_null),
                    fontSize = 48.sp,
                    color = Secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClockIn,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text(
                        stringResource(R.string.planner_worktime_einstempeln),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

// ── Manual Entry Card (today, pick start & end) ───────────────────────────────

@Composable
private fun ManualEntryCard(
    accent: Color,
    dtf: DateTimeFormatter,
    onSave: (LocalTime, LocalTime, Int) -> Unit
) {
    var startTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var breakMin by remember { mutableStateOf("0") }

    LMCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.planner_worktime_manuell_beschreibung),
                style = MaterialTheme.typography.bodySmall,
                color = Secondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimePickerField(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.planner_worktime_start),
                    time = startTime,
                    dtf = dtf,
                    onTimeChange = { startTime = it }
                )
                TimePickerField(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.planner_worktime_ende),
                    time = endTime,
                    dtf = dtf,
                    onTimeChange = { endTime = it }
                )
            }

            OutlinedTextField(
                value = breakMin,
                onValueChange = { breakMin = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.planner_worktime_pause_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = Secondary,
                    focusedBorderColor = accent,
                    unfocusedLabelColor = Secondary,
                    focusedLabelColor = accent
                )
            )

            Button(
                onClick = {
                    val bm = breakMin.toIntOrNull() ?: 0
                    if (endTime.isAfter(startTime)) {
                        onSave(startTime, endTime, bm)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Text(
                    stringResource(R.string.planner_worktime_speichern),
                    modifier = Modifier.padding(start = 4.dp),
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
            }
        }
    }
}

// ── Retroactive Entry Card (any date) ─────────────────────────────────────────

@Composable
private fun RetroactiveEntryCard(
    accent: Color,
    dtf: DateTimeFormatter,
    today: LocalDate,
    onSave: (LocalDate, LocalTime, LocalTime, Int, String) -> Unit
) {
    var dateStr by remember { mutableStateOf(today.toString()) }
    var startTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var breakMin by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    LMCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.planner_worktime_nachtragen_beschreibung),
                style = MaterialTheme.typography.bodySmall,
                color = Secondary
            )

            OutlinedTextField(
                value = dateStr,
                onValueChange = { dateStr = it },
                label = { Text(stringResource(R.string.planner_worktime_datum)) },
                placeholder = { Text(stringResource(R.string.planner_worktime_datum_placeholder), color = Secondary.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = Secondary,
                    focusedBorderColor = accent,
                    unfocusedLabelColor = Secondary,
                    focusedLabelColor = accent
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimePickerField(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.planner_worktime_start),
                    time = startTime,
                    dtf = dtf,
                    onTimeChange = { startTime = it }
                )
                TimePickerField(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.planner_worktime_ende),
                    time = endTime,
                    dtf = dtf,
                    onTimeChange = { endTime = it }
                )
            }

            OutlinedTextField(
                value = breakMin,
                onValueChange = { breakMin = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.planner_worktime_pause_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = Secondary,
                    focusedBorderColor = accent,
                    unfocusedLabelColor = Secondary,
                    focusedLabelColor = accent
                )
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.planner_worktime_notizen)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = Secondary,
                    focusedBorderColor = accent,
                    unfocusedLabelColor = Secondary,
                    focusedLabelColor = accent
                )
            )

            Button(
                onClick = {
                    val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@Button
                    val bm = breakMin.toIntOrNull() ?: 0
                    if (endTime.isAfter(startTime)) {
                        onSave(date, startTime, endTime, bm, notes)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Text(
                    stringResource(R.string.planner_worktime_speichern),
                    modifier = Modifier.padding(start = 4.dp),
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
            }
        }
    }
}

// ── Time Picker Field (tap to cycle hours/minutes) ────────────────────────────

@Composable
private fun TimePickerField(
    modifier: Modifier,
    label: String,
    time: LocalTime,
    dtf: DateTimeFormatter,
    onTimeChange: (LocalTime) -> Unit
) {
    OutlinedTextField(
        value = time.format(dtf),
        onValueChange = { raw ->
            // Parse "HH:mm" or "HHmm" input
            val cleaned = raw.replace(":", "").take(4)
            if (cleaned.length == 4) {
                val h = cleaned.substring(0, 2).toIntOrNull() ?: return@OutlinedTextField
                val m = cleaned.substring(2, 4).toIntOrNull() ?: return@OutlinedTextField
                if (h in 0..23 && m in 0..59) {
                    onTimeChange(LocalTime.of(h, m))
                }
            }
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = Color.White,
            focusedTextColor = Color.White,
            unfocusedBorderColor = Secondary,
            focusedBorderColor = LocalAccentColor.current,
            unfocusedLabelColor = Secondary,
            focusedLabelColor = LocalAccentColor.current
        )
    )
}
