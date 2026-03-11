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

import android.content.Intent
import android.graphics.Color as AndroidColor
import timber.log.Timber
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.data.calendar.CalendarEventEntity
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import de.lifemodule.app.feature.planner.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@Composable
private fun germanMonths() = listOf(
    stringResource(R.string.planner_calendar_januar), stringResource(R.string.planner_calendar_februar), stringResource(R.string.planner_calendar_maerz), stringResource(R.string.planner_calendar_april), stringResource(R.string.planner_calendar_mai), stringResource(R.string.planner_calendar_juni),
    stringResource(R.string.planner_calendar_juli), stringResource(R.string.planner_calendar_august), stringResource(R.string.planner_calendar_september), stringResource(R.string.planner_calendar_oktober), stringResource(R.string.planner_calendar_november), stringResource(R.string.planner_calendar_dezember)
)

@Composable
private fun weekDayHeaders() = listOf(
    stringResource(R.string.planner_calendar_mo), stringResource(R.string.planner_calendar_di),
    stringResource(R.string.planner_calendar_mi), stringResource(R.string.planner_calendar_do),
    stringResource(R.string.planner_calendar_fr), stringResource(R.string.planner_calendar_sa),
    stringResource(R.string.planner_calendar_so)
)

@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val datesWithEvents by viewModel.datesWithEvents.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    val eventsForDate by viewModel.eventsForDate.collectAsStateWithLifecycle()

    // ICS import
    val icsImportResult by viewModel.icsImportResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val icsImportedMsg = stringResource(R.string.planner_calendar_ics_imported, icsImportResult?.count ?: 0)
    val icsImportErrorMsg = stringResource(R.string.planner_calendar_ics_import_error)
    val icsNoEventsMsg = stringResource(R.string.planner_calendar_ics_no_events)

    LaunchedEffect(icsImportResult) {
        icsImportResult?.let { result ->
            val msg = if (result.error) icsImportErrorMsg else icsImportedMsg
            snackbarHostState.showSnackbar(msg)
            viewModel.clearIcsImportResult()
        }
    }

    val icsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importIcs(context, it) }
    }

    // Holidays from bundled JSON assets
    val allHolidays by viewModel.holidays.collectAsStateWithLifecycle()
    val holidays = remember(allHolidays, currentMonth) {
        allHolidays.filter { h ->
            try {
                val d = LocalDate.parse(h.date, DateTimeFormatter.ISO_LOCAL_DATE)
                d.year == currentMonth.year && d.monthValue == currentMonth.monthValue
            } catch (e: Exception) { Timber.w(e, "[CalendarScreen] Failed to parse holiday date: %s", h.date); false }
        }
    }
    val holidayDates = remember(allHolidays) {
        allHolidays.map { it.date }.toSet()
    }

    val today = viewModel.today().format(DateTimeFormatter.ISO_LOCAL_DATE)

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.planner_calendar_kalender),
                onBackClick = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = {
                        icsLauncher.launch(arrayOf("text/calendar"))
                    }) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = stringResource(R.string.planner_calendar_ics_import),
                            tint = Secondary
                        )
                    }
                    IconButton(onClick = {
                        val userEvents = upcomingEvents.filter { !it.isHoliday }
                        if (userEvents.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(icsNoEventsMsg)
                            }
                        } else {
                            val icsContent = viewModel.generateIcs(userEvents)
                            val file = File(context.cacheDir, "lifemodule_calendar.ics")
                            file.writeText(icsContent)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/calendar"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.planner_calendar_ics_share_title)))
                        }
                    }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.planner_calendar_ics_export),
                            tint = Secondary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { navController.navigate(AppRoute.CalendarAdd) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // ── Month Header with Navigation ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.planner_calendar_vorheriger_monat),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = germanMonths()[currentMonth.monthValue - 1],
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentMonth.year}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                    }

                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.planner_calendar_naechster_monat),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── "Heute" button ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.goToToday() }) {
                        Text(stringResource(R.string.planner_calendar_heute), color = accent)
                    }
                }
            }

            // ── Weekday Headers ──
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // CW column
                    Box(
                        modifier = Modifier.weight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.planner_calendar_kw),
                            style = MaterialTheme.typography.labelSmall,
                            color = Secondary,
                            fontSize = 10.sp
                        )
                    }
                    val weekDays = weekDayHeaders()
                    weekDays.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (day == weekDays[5] || day == weekDays[6]) accent.copy(alpha = 0.6f) else Secondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Calendar Grid ──
            item {
                val firstDayOfMonth = currentMonth.atDay(1)
                val lastDayOfMonth = currentMonth.atEndOfMonth()
                val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Monday

                // Calculate grid: rows of 7 days
                val totalDays = currentMonth.lengthOfMonth()
                val leadingEmptyDays = startDayOfWeek - 1
                val totalCells = leadingEmptyDays + totalDays
                val rows = (totalCells + 6) / 7

                Column {
                    for (row in 0 until rows) {
                        // Compute CW for this row
                        val firstDayInRow = row * 7 - leadingEmptyDays + 1
                        val representativeDay = firstDayInRow.coerceIn(1, totalDays)
                        val cwDate = currentMonth.atDay(representativeDay)
                        val weekFields = java.time.temporal.WeekFields.of(Locale.getDefault())
                        val cw = cwDate.get(weekFields.weekOfWeekBasedYear())

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            // CW label
                            Box(
                                modifier = Modifier.weight(0.6f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$cw",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Secondary.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )
                            }

                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - leadingEmptyDays + 1

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNum in 1..totalDays) {
                                        val dateStr = currentMonth.atDay(dayNum).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        val isToday = dateStr == today
                                        val isSelected = dateStr == selectedDate
                                        val hasEvent = datesWithEvents.contains(dateStr)
                                        val isHoliday = holidayDates.contains(dateStr)
                                        val isWeekend = col >= 5

                                        Column(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isToday -> accent
                                                        isSelected -> Surface
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable { viewModel.selectDate(dateStr) },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = when {
                                                    isToday -> Black
                                                    isHoliday -> Color(0xFFFF453A)
                                                    isWeekend -> Secondary.copy(alpha = 0.6f)
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            // Event dot
                                            if (hasEvent || isHoliday) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isToday) Black.copy(alpha = 0.6f)
                                                            else if (isHoliday) Color(0xFFFF453A)
                                                            else accent
                                                        )
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

            // ── Selected Date: Timeline View ──
            val selectedHolidays = holidays.filter { it.date == selectedDate }
            val allSelectedEvents = selectedHolidays + eventsForDate

            if (allSelectedEvents.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📌 ${formatDateGerman(selectedDate)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            navController.navigate(AppRoute.CalendarAddForDate(selectedDate))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.planner_calendar_add_event_cd), tint = accent)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sort events: timed events first (by time), then all-day
                val sortedSelected = allSelectedEvents.sortedWith(
                    compareBy<CalendarEventEntity> { it.time == null }
                        .thenBy { it.time ?: "" }
                )

                items(items = sortedSelected) { event ->
                    TimelineEventCard(event = event)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                // No events for selected date - show add-button hint
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📌 ${formatDateGerman(selectedDate)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            navController.navigate(AppRoute.CalendarAddForDate(selectedDate))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.planner_calendar_add_event_cd), tint = accent)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LMCard {
                        Box(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.planner_calendar_keine_termine_am_tag),
                                color = Secondary
                            )
                        }
                    }
                }
            }

            // ── Upcoming Events (sorted by urgency) ──
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.planner_calendar_anstehende_termine),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (upcomingEvents.isEmpty()) {
                item {
                    LMCard {
                        Box(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.planner_calendar_keine_anstehenden_termine_tippe_um),
                                color = Secondary
                            )
                        }
                    }
                }
            } else {
                items(items = upcomingEvents) { event ->
                    EventCard(event = event, showDate = true, today = viewModel.today())
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── Upcoming Holidays ──
            val upcomingHolidays = allHolidays.filter { it.date >= today }.take(5)
            if (upcomingHolidays.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.planner_calendar_feiertage),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(items = upcomingHolidays) { holiday ->
                    EventCard(event = holiday, showDate = true, today = viewModel.today())
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TimelineEventCard(event: CalendarEventEntity) {
    val accent = LocalAccentColor.current
    val eventColor = try {
        Color(AndroidColor.parseColor(event.color))
    } catch (e: Exception) {
        Timber.w(e, "[CalendarScreen] Failed to parse event color: %s", event.color)
        accent
    }

    val categoryEmoji = when (event.category) {
        "exam" -> "📝"
        "deadline" -> "⏰"
        "personal" -> "👤"
        "birthday" -> "🎂"
        "holiday" -> "🇩🇪"
        else -> "📌"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // ── Time column (left rail) ──
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.End
        ) {
            val eventTime = event.time
            val eventEndTime = event.endTime
            if (eventTime != null) {
                Text(
                    text = eventTime,
                    style = MaterialTheme.typography.labelMedium,
                    color = eventColor,
                    fontWeight = FontWeight.Bold
                )
                if (eventEndTime != null) {
                    Text(
                        text = eventEndTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.planner_calendar_ganztaegig_all),
                    style = MaterialTheme.typography.labelSmall,
                    color = Secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.planner_calendar_ganztaegig_day),
                    style = MaterialTheme.typography.labelSmall,
                    color = Secondary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ── Timeline rail (dot + line) ──
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(eventColor)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(eventColor.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // ── Content card ──
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(eventColor.copy(alpha = 0.12f))
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = categoryEmoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary,
                        maxLines = 2
                    )
                }
                if (event.time != null && event.endTime != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${event.time} - ${event.endTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = eventColor
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: CalendarEventEntity, showDate: Boolean, today: LocalDate) {
    val accent = LocalAccentColor.current
    val eventColor = try {
        Color(AndroidColor.parseColor(event.color))
    } catch (e: Exception) {
        Timber.w(e, "[CalendarScreen] Failed to parse event color: %s", event.color)
        accent
    }

    val categoryEmoji = when (event.category) {
        "exam" -> "📝"
        "deadline" -> "⏰"
        "personal" -> "👤"
        "birthday" -> "🎂"
        "holiday" -> "🇩🇪"
        else -> "📌"
    }

    val daysUntil = try {
        val eventDate = LocalDate.parse(event.date, DateTimeFormatter.ISO_LOCAL_DATE)
        ChronoUnit.DAYS.between(today, eventDate)
    } catch (e: Exception) {
        Timber.w(e, "[CalendarScreen] Failed to parse event date: %s", event.date)
        0L
    }

    val urgencyText = when {
        daysUntil == 0L -> stringResource(R.string.planner_calendar_heute)
        daysUntil == 1L -> stringResource(R.string.planner_calendar_morgen)
        else -> stringResource(R.string.planner_calendar_in_x_tagen, daysUntil)
    }

    val urgencyColor = when {
        daysUntil == 0L -> Color(0xFFFF453A)
        daysUntil == 1L -> Color(0xFFFF9F0A)
        daysUntil < 7 -> Color(0xFFFFD60A)
        else -> Secondary
    }

    LMCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color bar
            Box(
                modifier = Modifier
                    .size(4.dp, 44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(eventColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = categoryEmoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary,
                        maxLines = 1
                    )
                }
                Row {
                    if (showDate) {
                        Text(
                            text = formatDateGerman(event.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = Secondary
                        )
                    }
                    if (event.time != null) {
                        if (showDate) Text(" - ", color = Secondary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = event.time + (event.endTime?.let { " - $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = Secondary
                        )
                    }
                }
            }
            if (!event.isHoliday) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = urgencyText,
                        style = MaterialTheme.typography.labelSmall,
                        color = urgencyColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun formatDateGerman(dateStr: String): String {
    val months = germanMonths()
    return try {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        val dayName = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            .replaceFirstChar { it.uppercase() }
        "$dayName, ${date.dayOfMonth}. ${months[date.monthValue - 1]} ${date.year}"
    } catch (e: Exception) {
        Timber.w(e, "[CalendarScreen] Failed to format date: %s", dateStr)
        dateStr
    }
}
