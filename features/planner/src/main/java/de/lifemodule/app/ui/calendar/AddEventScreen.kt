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

package de.lifemodule.app.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import timber.log.Timber
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import de.lifemodule.app.feature.planner.R
import androidx.compose.ui.res.stringResource

@Composable
private fun categories() = listOf(
    "exam" to stringResource(R.string.planner_calendar_klausur),
    "deadline" to stringResource(R.string.planner_calendar_deadline),
    "personal" to stringResource(R.string.planner_calendar_privat),
    "birthday" to stringResource(R.string.planner_calendar_geburtstag),
    "other" to stringResource(R.string.planner_calendar_sonstig)
)

@Composable
private fun reminderOptions() = listOf(
    0 to stringResource(R.string.planner_calendar_zur_startzeit),
    15 to stringResource(R.string.planner_calendar_15_min_vorher),
    30 to stringResource(R.string.planner_calendar_30_min_vorher),
    60 to stringResource(R.string.planner_calendar_1_std_vorher),
    1440 to stringResource(R.string.planner_calendar_1_tag_vorher)
)

private val colorPalette = listOf(
    "#30D158", "#0A84FF", "#FF453A", "#FF9F0A",
    "#BF5AF2", "#64D2FF", "#FFD60A", "#FF375F",
    "#AC8E68", "#48484A"
)

@Composable
fun AddEventScreen(
    navController: NavController,
    preselectedDate: String? = null,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(
            if (preselectedDate != null) {
                runCatching { LocalDate.parse(preselectedDate, DateTimeFormatter.ISO_LOCAL_DATE) }
                    .getOrDefault(viewModel.today())
            } else viewModel.today()
        )
    }
    var startHour by remember { mutableStateOf<Int?>(null) }
    var startMinute by remember { mutableStateOf<Int?>(null) }
    var endHour by remember { mutableStateOf<Int?>(null) }
    var endMinute by remember { mutableStateOf<Int?>(null) }
    var selectedCategory by remember { mutableStateOf("other") }
    var selectedColor by remember { mutableStateOf("#30D158") }
    var notifyEnabled by remember { mutableStateOf(false) }
    var reminderMinutes by remember { mutableStateOf(15) }

    // Birthday-specific fields
    var birthdayPerson by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.planner_calendar_event_hinzufuegen),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Category ──
            Text(stringResource(R.string.planner_calendar_kategorie), style = MaterialTheme.typography.bodyMedium, color = Secondary)
            val categoryList = categories()
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = categoryList) { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Black
                        )
                    )
                }
            }

            // ── Title / Birthday person ──
            if (selectedCategory == "birthday") {
                LMInput(
                    value = birthdayPerson,
                    onValueChange = { birthdayPerson = it },
                    label = stringResource(R.string.planner_calendar_name_der_person),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LMInput(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.planner_calendar_titel),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LMInput(
                value = description,
                onValueChange = { description = it },
                label = if (selectedCategory == "birthday") stringResource(R.string.planner_calendar_notiz_optional) else stringResource(R.string.planner_calendar_beschreibung),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Date Picker ──
            Text(stringResource(R.string.planner_calendar_datum), style = MaterialTheme.typography.bodyMedium, color = Secondary)
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            selectedDate = LocalDate.of(year, month + 1, day)
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accent)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Time Pickers (not for birthday) ──
            if (selectedCategory != "birthday") {
                Text(stringResource(R.string.planner_calendar_uhrzeit), style = MaterialTheme.typography.bodyMedium, color = Secondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start time
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    startHour = hour
                                    startMinute = minute
                                },
                                startHour ?: 9,
                                startMinute ?: 0,
                                true // 24h format
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = accent)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = if (startHour != null) String.format("%02d:%02d", startHour, startMinute) else stringResource(R.string.planner_calendar_start),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // End time
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    endHour = hour
                                    endMinute = minute
                                },
                                endHour ?: (startHour?.plus(1) ?: 10),
                                endMinute ?: 0,
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = accent)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = if (endHour != null) String.format("%02d:%02d", endHour, endMinute) else stringResource(R.string.planner_calendar_ende),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Notification Toggle ──
            LMCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = accent)
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.planner_calendar_erinnerung), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        if (notifyEnabled) {
                            Text(
                                reminderOptions().first { it.first == reminderMinutes }.second,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                    Switch(
                        checked = notifyEnabled,
                        onCheckedChange = { notifyEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = accent)
                    )
                }
            }

            // Reminder options
            val reminderList = reminderOptions()
            if (notifyEnabled) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = reminderList) { (minutes, label) ->
                        FilterChip(
                            selected = reminderMinutes == minutes,
                            onClick = { reminderMinutes = minutes },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent,
                                selectedLabelColor = Black
                            )
                        )
                    }
                }
            }

            // ── Color picker ──
            Text(stringResource(R.string.planner_calendar_farbe), style = MaterialTheme.typography.bodyMedium, color = Secondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorPalette.forEach { hex ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) { Timber.w(e, "[AddEventScreen] Failed to parse color: %s", hex); accent }
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save Button ──
            val eventTitle = if (selectedCategory == "birthday") stringResource(R.string.planner_calendar_geburtstag_titel, birthdayPerson) else title
            val isValid = if (selectedCategory == "birthday") birthdayPerson.isNotBlank() else title.isNotBlank()

            Button(
                onClick = {
                    if (isValid) {
                        val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val timeStr = if (startHour != null) String.format("%02d:%02d", startHour, startMinute) else null
                        val endTimeStr = if (endHour != null) String.format("%02d:%02d", endHour, endMinute) else null

                        viewModel.addEvent(
                            title = eventTitle,
                            description = description,
                            date = dateStr,
                            time = timeStr,
                            endTime = endTimeStr,
                            category = selectedCategory,
                            color = if (selectedCategory == "birthday") "#FF375F" else selectedColor,
                            isRecurring = selectedCategory == "birthday"
                        )

                        // Schedule notification if enabled
                        if (notifyEnabled) {
                            viewModel.scheduleNotification(
                                context = context,
                                title = eventTitle,
                                date = dateStr,
                                time = timeStr,
                                reminderMinutes = reminderMinutes
                            )
                        }

                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.planner_calendar_speichern), color = Black)
            }
        }
    }
}
