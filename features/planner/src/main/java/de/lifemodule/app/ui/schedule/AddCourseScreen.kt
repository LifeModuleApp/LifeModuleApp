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

package de.lifemodule.app.ui.schedule

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.feature.planner.R
import androidx.compose.ui.res.stringResource

private val dayLabels = listOf("Mo", "Di", "Mi", "Do", "Fr")

@Composable
private fun courseTypeLabels() = listOf(
    "vorlesung" to stringResource(R.string.planner_schedule_vorlesung),
    "uebung" to stringResource(R.string.planner_schedule_uebung),
    "seminar" to stringResource(R.string.planner_schedule_seminar),
    "praktikum" to stringResource(R.string.planner_schedule_praktikum),
    "tutorium" to stringResource(R.string.planner_schedule_tutorium)
)

@Composable
private fun weekTypeLabels() = listOf(
    "every" to stringResource(R.string.planner_schedule_jede_woche),
    "a" to stringResource(R.string.planner_schedule_wochentyp_a),
    "b" to stringResource(R.string.planner_schedule_wochentyp_b)
)

private val colorPalette = listOf(
    "#30D158", "#0A84FF", "#FF453A", "#FF9F0A",
    "#BF5AF2", "#64D2FF", "#FFD60A", "#FF375F",
    "#AC8E68", "#48484A"
)

@Composable
fun AddCourseScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var professor by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var selectedDay by remember { mutableIntStateOf(0) }
    var startHour by remember { mutableIntStateOf(8) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(9) }
    var endMinute by remember { mutableIntStateOf(30) }
    var semester by remember { mutableStateOf("") }
    var weekType by remember { mutableStateOf("every") }
    var courseType by remember { mutableStateOf("vorlesung") }
    var selectedColor by remember { mutableStateOf("#30D158") }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.planner_schedule_kurs_hinzufuegen),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LMInput(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.planner_schedule_kursname),
                modifier = Modifier.fillMaxWidth()
            )
            LMInput(
                value = professor,
                onValueChange = { professor = it },
                label = stringResource(R.string.planner_schedule_professor),
                modifier = Modifier.fillMaxWidth()
            )
            LMInput(
                value = room,
                onValueChange = { room = it },
                label = stringResource(R.string.planner_schedule_raum),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Semester ──
            LMInput(
                value = semester,
                onValueChange = { semester = it },
                label = stringResource(R.string.planner_schedule_semester_zb_sose_2026),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Course Type ──
            Text(
                text = stringResource(R.string.planner_schedule_kurstyp),
                style = MaterialTheme.typography.bodyMedium,
                color = Secondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                courseTypeLabels().forEach { (type, label) ->
                    FilterChip(
                        selected = courseType == type,
                        onClick = { courseType = type },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Black
                        )
                    )
                }
            }

            // ── Day selector ──
            Text(
                text = stringResource(R.string.planner_schedule_wochentag),
                style = MaterialTheme.typography.bodyMedium,
                color = Secondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedDay == index,
                        onClick = { selectedDay = index },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Black
                        )
                    )
                }
            }

            // ── Week Type (A/B) ──
            Text(
                text = stringResource(R.string.planner_schedule_wochenrhythmus),
                style = MaterialTheme.typography.bodyMedium,
                color = Secondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekTypeLabels().forEach { (type, label) ->
                    FilterChip(
                        selected = weekType == type,
                        onClick = { weekType = type },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Black
                        )
                    )
                }
            }

            // ── Time Pickers (native) ──
            Text(stringResource(R.string.planner_schedule_uhrzeit), style = MaterialTheme.typography.bodyMedium, color = Secondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                startHour = hour
                                startMinute = minute
                            },
                            startHour,
                            startMinute,
                            true
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = accent)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = String.format("%02d:%02d", startHour, startMinute),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                endHour = hour
                                endMinute = minute
                            },
                            endHour,
                            endMinute,
                            true
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = accent)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = String.format("%02d:%02d", endHour, endMinute),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Color Picker ──
            Text(
                text = stringResource(R.string.planner_schedule_farbe),
                style = MaterialTheme.typography.bodyMedium,
                color = Secondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorPalette.forEach { hex ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        Timber.w(e, "[AddCourseScreen] Failed to parse color: %s", hex)
                        accent
                    }
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    Color.White,
                                    CircleShape
                                ) else Modifier
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save ──
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addCourse(
                            name = name,
                            professor = professor,
                            room = room,
                            dayOfWeek = selectedDay,
                            startTime = String.format("%02d:%02d", startHour, startMinute),
                            endTime = String.format("%02d:%02d", endHour, endMinute),
                            color = selectedColor,
                            semester = semester,
                            weekType = weekType,
                            courseType = courseType
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.planner_schedule_speichern), color = Black)
            }
        }
    }
}
