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

import android.graphics.Color as AndroidColor
import timber.log.Timber
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.data.schedule.CourseEntity
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import java.time.LocalTime
import de.lifemodule.app.feature.planner.R
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@Composable
private fun dayNames() = listOf(
    stringResource(R.string.planner_schedule_mo), stringResource(R.string.planner_schedule_di),
    stringResource(R.string.planner_schedule_mi), stringResource(R.string.planner_schedule_do),
    stringResource(R.string.planner_schedule_fr)
)
private val HOUR_HEIGHT = 60.dp
// START_HOUR and END_HOUR are now configurable via settings (see readScheduleHours below)
private const val DEFAULT_START_HOUR = 8
private const val DEFAULT_END_HOUR = 20

@Composable
fun ScheduleScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val context = LocalContext.current
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val courses by viewModel.coursesForSemester.collectAsStateWithLifecycle()
    val coursesForDay by viewModel.coursesForDay.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()

    // Read configurable schedule hours from shared preferences
    val prefs = remember { context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE) }
    val START_HOUR = remember { prefs.getInt("schedule_start_hour", DEFAULT_START_HOUR) }
    val END_HOUR = remember { prefs.getInt("schedule_end_hour", DEFAULT_END_HOUR) }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.planner_schedule_stundenplan),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { navController.navigate(AppRoute.UniScheduleAdd) })
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Controls Row: Semester + View Toggle ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Semester dropdown
                SemesterDropdown(
                    semesters = semesters,
                    selected = selectedSemester,
                    onSelect = { viewModel.setSemester(it) }
                )

                // View toggle
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = viewMode == "week",
                        onClick = { viewModel.setViewMode("week") },
                        label = { Text(stringResource(R.string.planner_schedule_woche)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Black
                        )
                    )
                    FilterChip(
                        selected = viewMode == "day",
                        onClick = { viewModel.setViewMode("day") },
                        label = { Text(stringResource(R.string.planner_schedule_tag_ansicht)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Black
                        )
                    )
                }
            }

            if (viewMode == "week") {
                WeekGridView(
                    courses = courses,
                    today = viewModel.today(),
                    currentTime = viewModel.currentTime(),
                    startHour = START_HOUR,
                    endHour = END_HOUR,
                    onDayClick = { day ->
                        viewModel.selectDay(day)
                        viewModel.setViewMode("day")
                    }
                )
            } else {
                DayDetailView(
                    selectedDay = selectedDay,
                    courses = coursesForDay,
                    onDaySelect = { viewModel.selectDay(it) },
                    onDelete = { viewModel.deleteCourse(it) }
                )
            }
        }
    }
}

// ── Semester Dropdown ──

@Composable
private fun SemesterDropdown(
    semesters: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = if (selected.isBlank()) stringResource(R.string.planner_schedule_alle_semester) else "🎓 $selected",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.planner_schedule_alle_semester_1)) },
                onClick = { onSelect(""); expanded = false }
            )
            semesters.forEach { sem ->
                DropdownMenuItem(
                    text = { Text(sem) },
                    onClick = { onSelect(sem); expanded = false }
                )
            }
        }
    }
}

// ── Week Grid View (Untis-Style) ──

@Composable
private fun WeekGridView(
    courses: List<CourseEntity>,
    today: java.time.LocalDate,
    currentTime: java.time.LocalTime,
    startHour: Int,
    endHour: Int,
    onDayClick: (Int) -> Unit
) {
    val accent = LocalAccentColor.current
    val scrollState = rememberScrollState()
    val columnWidth = 64.dp
    val timeColumnWidth = 40.dp
    val now = currentTime
    val todayIndex = today.dayOfWeek.value - 1 // 0=Mon

    Column {
        // Day headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = timeColumnWidth + 4.dp)
        ) {
            dayNames().forEachIndexed { index, name ->
                val isToday = index == todayIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isToday) accent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onDayClick(index) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) accent else MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Grid body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            val totalHours = endHour - startHour

            Row(modifier = Modifier.fillMaxWidth()) {
                // Time labels column
                Column(
                    modifier = Modifier.width(timeColumnWidth)
                ) {
                    for (h in startHour..endHour) {
                        Box(
                            modifier = Modifier.height(HOUR_HEIGHT),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = "${h}:00",
                                style = MaterialTheme.typography.labelSmall,
                                color = Secondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }

                // Grid columns for each day
                Row(modifier = Modifier.weight(1f)) {
                    for (dayIndex in 0..4) {
                        val dayCourses = courses.filter { it.dayOfWeek == dayIndex }
                        val isToday = dayIndex == todayIndex

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(HOUR_HEIGHT * (totalHours + 1))
                        ) {
                            // Grid lines
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val hourH = size.height / (totalHours + 1)
                                // Horizontal hour lines
                                for (h in 0..totalHours) {
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.06f),
                                        start = Offset(0f, h * hourH),
                                        end = Offset(size.width, h * hourH),
                                        strokeWidth = 1f
                                    )
                                }
                                // Left border
                                drawLine(
                                    color = Color.White.copy(alpha = 0.06f),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1f
                                )
                            }

                            // Course blocks
                            dayCourses.forEach { course ->
                                val startMinutes = timeToMinutes(course.startTime) - startHour * 60
                                val endMinutes = timeToMinutes(course.endTime) - startHour * 60
                                val durationMinutes = endMinutes - startMinutes

                                if (startMinutes >= 0 && durationMinutes > 0) {
                                    val topOffset = (startMinutes.toFloat() / 60f) * HOUR_HEIGHT.value
                                    val blockHeight = (durationMinutes.toFloat() / 60f) * HOUR_HEIGHT.value

                                    val courseColor = try {
                                        Color(AndroidColor.parseColor(course.color))
                                    } catch (e: Exception) {
                                        Timber.w(e, "[ScheduleScreen] Failed to parse course color: %s", course.color)
                                        accent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset(y = topOffset.dp)
                                            .fillMaxWidth()
                                            .height(blockHeight.dp)
                                            .padding(horizontal = 1.dp, vertical = 1.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(courseColor.copy(alpha = 0.85f))
                                            .padding(2.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = course.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 10.sp
                                            )
                                            if (course.room.isNotBlank() && blockHeight > 30f) {
                                                Text(
                                                    text = course.room,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 8.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Current time indicator (red line)
                            if (isToday) {
                                val nowMinutes = now.hour * 60 + now.minute - startHour * 60
                                if (nowMinutes in 0..(totalHours * 60)) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val yPx = (nowMinutes.toFloat() / 60f) * HOUR_HEIGHT.toPx()
                                        drawCircle(
                                            color = Color.Red,
                                            radius = 4f,
                                            center = Offset(0f, yPx)
                                        )
                                        drawLine(
                                            color = Color.Red,
                                            start = Offset(0f, yPx),
                                            end = Offset(size.width, yPx),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Day Detail View ──

@Composable
private fun DayDetailView(
    selectedDay: Int,
    courses: List<CourseEntity>,
    onDaySelect: (Int) -> Unit,
    onDelete: (CourseEntity) -> Unit
) {
    val accent = LocalAccentColor.current
    Column {
        // Day tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayNames().forEachIndexed { index, name ->
                val isSelected = selectedDay == index
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accent else Color.Transparent)
                        .clickable { onDaySelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Black else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (courses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.planner_schedule_keine_kurse_am_tag, dayNames().getOrElse(selectedDay) { "" }),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = courses) { course ->
                    CourseDetailCard(course = course, onDelete = { onDelete(course) })
                }
            }
        }
    }
}

@Composable
private fun CourseDetailCard(course: CourseEntity, onDelete: () -> Unit) {
    val accent = LocalAccentColor.current
    val courseColor = try {
        Color(AndroidColor.parseColor(course.color))
    } catch (e: Exception) {
        Timber.w(e, "[ScheduleScreen] Failed to parse course color: %s", course.color)
        accent
    }

    val typeEmoji = when (course.courseType) {
        "vorlesung" -> "📖"
        "uebung" -> "✏️"
        "seminar" -> "💬"
        "praktikum" -> "🔬"
        "tutorium" -> "👨‍🏫"
        else -> "📚"
    }

    val weekLabel = when (course.weekType) {
        "a" -> stringResource(R.string.planner_schedule_woche_a)
        "b" -> stringResource(R.string.planner_schedule_woche_b)
        else -> ""
    }

    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.planner_schedule_kurs_loeschen)) },
            text = { Text(stringResource(R.string.planner_schedule_kurs_loeschen_bestaetigung, course.name)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); confirmDelete = false }) {
                    Text(stringResource(R.string.planner_schedule_loeschen), color = Color(0xFFFF453A))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.planner_schedule_abbrechen)) }
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
            // Color bar
            Box(
                modifier = Modifier
                    .size(6.dp, 52.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(courseColor)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = typeEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (course.professor.isNotBlank()) {
                    Text(
                        text = course.professor,
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }
                Row {
                    if (course.room.isNotBlank()) {
                        Text(
                            text = "📍 ${course.room}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (course.semester.isNotBlank()) {
                        Text(
                            text = "🎓 ${course.semester}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                    }
                }
                if (weekLabel.isNotBlank()) {
                    Text(
                        text = "🔄$weekLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = course.startTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = courseColor
                )
                Text(
                    text = course.endTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = Secondary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.planner_schedule_loeschen),
                    tint = Color(0xFFFF453A).copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun timeToMinutes(time: String): Int {
    return try {
        val parts = time.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) {
        Timber.w(e, "[ScheduleScreen] Failed to parse time: %s", time)
        0
    }
}
