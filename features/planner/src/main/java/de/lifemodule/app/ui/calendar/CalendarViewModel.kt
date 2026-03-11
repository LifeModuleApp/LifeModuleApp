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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.calendar.CalendarEventEntity
import de.lifemodule.app.data.calendar.CalendarRepository
import timber.log.Timber
import de.lifemodule.app.data.calendar.HolidayRepository
import de.lifemodule.app.data.calendar.IcsGenerator
import de.lifemodule.app.data.calendar.IcsParser
import de.lifemodule.app.notifications.NotificationScheduler
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val holidayRepository: HolidayRepository,
    private val notificationScheduler: NotificationScheduler,
    private val timeProvider: TimeProvider,
    private val icsParser: IcsParser,
    private val icsGenerator: IcsGenerator,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Currently displayed month
    private val _currentMonth = MutableStateFlow(YearMonth.from(timeProvider.today()))
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    // Selected date
    private val _selectedDate = MutableStateFlow(timeProvider.today().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate

    // Holidays loaded from bundled JSON assets
    private val _holidays = MutableStateFlow<List<CalendarEventEntity>>(emptyList())
    val holidays: StateFlow<List<CalendarEventEntity>> = _holidays

    // Events for selected date (includes recurring events like birthdays)
    @OptIn(ExperimentalCoroutinesApi::class)
    val eventsForDate: StateFlow<List<CalendarEventEntity>> = _selectedDate
        .flatMapLatest { date ->
            val monthDay = date.substring(5) // "MM-DD"
            combine(
                repository.getEventsForDate(date),
                repository.getRecurringEventsForMonthDay(monthDay)
            ) { regular, recurring ->
                // Merge: regular events + recurring events not from today's year
                val regularIds = regular.map { it.uuid }.toSet()
                val filteredRecurring = recurring.filter { it.uuid !in regularIds }
                regular + filteredRecurring
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dates with events in current month (for dot indicators)
    @OptIn(ExperimentalCoroutinesApi::class)
    val datesWithEvents: StateFlow<List<String>> = _currentMonth
        .flatMapLatest { month ->
            val start = month.atDay(1).format(dateFormatter)
            val end = month.atEndOfMonth().format(dateFormatter)
            repository.getDatesWithEvents(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Upcoming events sorted by urgency (closest first)
    val upcomingEvents: StateFlow<List<CalendarEventEntity>> = repository
        .getUpcomingEvents(timeProvider.today().format(dateFormatter))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load holidays for the current year on startup
        loadHolidays(YearMonth.from(timeProvider.today()).year)
    }

    private fun loadHolidays(year: Int) {
        viewModelScope.launch {
            try {
                val prefs = appContext.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
                val region = prefs.getString("selected_region", "") ?: ""
                val country = prefs.getString("selected_holiday_country", "") ?: ""
                val loaded = holidayRepository.getHolidays(year, subdivision = region, overrideCountry = country)
                _holidays.value = loaded
            } catch (e: Exception) {
                Timber.e(e, "[Calendar] Failed to load holidays for year %d", year)
            }
        }
    }

    fun selectDate(date: String) { _selectedDate.value = date }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        val newYear = _currentMonth.value.year
        if (_holidays.value.none { it.date.startsWith("$newYear-") }) {
            loadHolidays(newYear)
        }
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        val newYear = _currentMonth.value.year
        if (_holidays.value.none { it.date.startsWith("$newYear-") }) {
            loadHolidays(newYear)
        }
    }

    fun goToToday() {
        _currentMonth.value = YearMonth.from(timeProvider.today())
        _selectedDate.value = timeProvider.today().format(dateFormatter)
    }

    /** Expose today for Composables that need it without direct TimeProvider access */
    fun today(): LocalDate = timeProvider.today()

    fun getHolidaysForMonth(month: YearMonth): List<CalendarEventEntity> {
        return _holidays.value.filter { holiday ->
            try {
                val date = LocalDate.parse(holiday.date, dateFormatter)
                date.year == month.year && date.monthValue == month.monthValue
            } catch (e: Exception) { Timber.w(e, "[CalendarViewModel] Failed to parse holiday date: %s", holiday.date); false }
        }
    }

    fun addEvent(
        title: String, description: String, date: String,
        time: String?, endTime: String?, category: String, color: String,
        isRecurring: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                repository.insert(
                    CalendarEventEntity(
                        title = title, description = description,
                        date = date, time = time, endTime = endTime,
                        category = category, color = color,
                        isRecurring = isRecurring
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "[Calendar] Failed to add event '%s'", title)
            }
        }
    }

    fun deleteEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            try { repository.delete(event) }
            catch (e: Exception) { Timber.e(e, "[Calendar] Failed to delete event '%s'", event.title) }
        }
    }

    fun scheduleNotification(
        context: android.content.Context,
        title: String,
        date: String,
        time: String?,
        reminderMinutes: Int
    ) {
        notificationScheduler.scheduleCalendarReminder(
            title = title,
            date = date,
            time = time,
            minutesBefore = reminderMinutes
        )
    }

    // ── ICS import / export ──────────────────────────────────────────────────

    /** Result of an ICS import operation. */
    private val _icsImportResult = MutableStateFlow<IcsImportResult?>(null)
    val icsImportResult: StateFlow<IcsImportResult?> = _icsImportResult

    /**
     * Imports events from an .ics file URI.
     * All events are inserted into the local database - 100 % offline.
     */
    fun importIcs(context: android.content.Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _icsImportResult.value = IcsImportResult(0, error = true)
                    return@launch
                }
                val events = inputStream.use { stream ->
                    icsParser.parse(stream)
                }
                var imported = 0
                for (event in events) {
                    repository.insert(event)
                    imported++
                }
                _icsImportResult.value = IcsImportResult(imported)
            } catch (e: Exception) {
                Timber.e(e, "[CalendarViewModel] ICS import failed")
                _icsImportResult.value = IcsImportResult(0, error = true)
            }
        }
    }

    /**
     * Generates ICS content for the given events (or all visible events if null).
     * Returns the raw .ics string to be shared or saved by the UI layer.
     */
    fun generateIcs(events: List<CalendarEventEntity>): String {
        return icsGenerator.generate(events)
    }

    /** Dismisses the ICS import result snackbar. */
    fun clearIcsImportResult() {
        _icsImportResult.value = null
    }
}

data class IcsImportResult(
    val count: Int,
    val error: Boolean = false
)
