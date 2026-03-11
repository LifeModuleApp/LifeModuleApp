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

package de.lifemodule.app.ui.debug

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.util.time.DebugTimeProvider
import de.lifemodule.app.util.time.TimeProvider
import de.lifemodule.app.R
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import de.lifemodule.app.notifications.NotificationHelper
import de.lifemodule.app.notifications.ReminderReceiver

/**
 * Debug screen that allows time-travelling via [DebugTimeProvider].
 *
 * Only reachable in debug builds - the Settings entry is hidden behind
 * a [BuildConfig.DEBUG] check.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    navController: NavController,
    timeProvider: TimeProvider
) {
    val accent = LocalAccentColor.current
    val isDebugProvider = timeProvider is DebugTimeProvider
    val debugProvider = timeProvider as? DebugTimeProvider

    // Current virtual time (re-read on recomposition)
    var virtualNow by remember { mutableStateOf(timeProvider.now()) }

    val dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss")

    // Date picker - initialise to current virtual date
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = virtualNow.toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )

    // Time picker - initialise to current virtual time
    val timePickerState = rememberTimePickerState(
        initialHour = virtualNow.hour,
        initialMinute = virtualNow.minute,
        is24Hour = true
    )

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.app_debug_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Status Card ───────────────────────────────────────────────
            LMCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.app_debug_status_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    StatusRow(stringResource(R.string.app_debug_provider), if (isDebugProvider) stringResource(R.string.app_debug_debug_provider) else stringResource(R.string.app_debug_system_provider))
                    StatusRow(stringResource(R.string.app_debug_virtuelle_zeit), virtualNow.format(dtf))
                    StatusRow(stringResource(R.string.app_debug_system_zeit), LocalDateTime.now().format(dtf))
                    if (debugProvider != null) {
                        val offset = debugProvider.getOffset()
                        val sign = if (offset.isNegative) "-" else "+"
                        val abs = offset.abs()
                        val days = abs.toDays()
                        val hours = abs.toHours() % 24
                        val minutes = abs.toMinutes() % 60
                        StatusRow(stringResource(R.string.app_debug_offset), "${sign}${days}d ${hours}h ${minutes}m")
                    }
                }
            }

            if (!isDebugProvider) {
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.app_debug_warning),
                        modifier = Modifier.padding(16.dp),
                        color = Destructive,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                return@Scaffold
            }

            // ── Date Picker ───────────────────────────────────────────────
            LMCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_debug_ziel_datum),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        title = null,
                        headline = null,
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            selectedDayContainerColor = accent,
                            selectedDayContentColor = Black,
                            todayDateBorderColor = accent,
                            todayContentColor = accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Time Picker ───────────────────────────────────────────────
            LMCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.app_debug_ziel_uhrzeit),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor = accent,
                            clockDialSelectedContentColor = Black,
                            timeSelectorSelectedContainerColor = accent.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = accent
                        )
                    )
                }
            }

            // ── Action Buttons ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Apply
                Button(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        val selectedDate = if (selectedMillis != null) {
                            Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        } else {
                            LocalDate.now()
                        }
                        val selectedTime = LocalTime.of(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        val target = LocalDateTime.of(selectedDate, selectedTime)
                        debugProvider!!.setTargetTime(target)
                        virtualNow = timeProvider.now()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Black
                    )
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.app_debug_anwenden), fontWeight = FontWeight.Bold)
                }

                // Reset
                OutlinedButton(
                    onClick = {
                        debugProvider!!.resetToSystemTime()
                        virtualNow = timeProvider.now()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Destructive,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.app_debug_zuruecksetzen), color = Destructive, fontWeight = FontWeight.Bold)
                }
            }

            // ── Test Notifications ────────────────────────────────────────
            LMCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Test Notifications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val ctx = LocalContext.current
                    
                    // Test Supplement Notification
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(ctx, ReminderReceiver::class.java).apply {
                                putExtra("channel", NotificationHelper.CHANNEL_SUPPLEMENTS)
                                putExtra("title", "Supplement Erinnerung")
                                putExtra("body", "Es ist Zeit für deine 💊 Omega 3 (Test)")
                                putExtra("notif_id", 991)
                            }
                            ctx.sendBroadcast(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Test Supplement Notification", color = accent)
                    }
                    
                    // Test Habit Notification
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(ctx, ReminderReceiver::class.java).apply {
                                putExtra("channel", NotificationHelper.CHANNEL_HABITS)
                                putExtra("title", "Habit Erinnerung")
                                putExtra("body", "Vergiss nicht: 📖 Lesen (Test)")
                                putExtra("notif_id", 992)
                            }
                            ctx.sendBroadcast(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Test Habit Notification", color = accent)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
