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

package de.lifemodule.app.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.LifeModuleDatabase
import de.lifemodule.app.data.analytics.ActivityLogger
import de.lifemodule.app.data.backup.BackupManager
import de.lifemodule.app.data.export.DataExporter
import de.lifemodule.app.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val activityLogger: ActivityLogger,
    private val dataExporter: DataExporter,
    private val backupManager: BackupManager,
    private val notificationScheduler: NotificationScheduler,
    private val db: LifeModuleDatabase,
    @Named("theme") private val themeDataStore: DataStore<Preferences>,
    @Named("modules") private val modulesDataStore: DataStore<Preferences>,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)

    // ── Region ──
    private val _selectedRegion = MutableStateFlow(prefs.getString("selected_region", "") ?: "")
    val selectedRegion: StateFlow<String> = _selectedRegion

    private val _selectedHolidayCountry = MutableStateFlow(prefs.getString("selected_holiday_country", "") ?: "")
    val selectedHolidayCountry: StateFlow<String> = _selectedHolidayCountry

    // ── Personal Data ──
    private val _userAge = MutableStateFlow(prefs.getInt("user_age", 30))
    val userAge: StateFlow<Int> = _userAge

    private val _userHeightCm = MutableStateFlow(prefs.getInt("user_height_cm", 175))
    val userHeightCm: StateFlow<Int> = _userHeightCm

    // ── Master toggle ──
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    // ── Calendar ──
    private val _calendarReminder1h = MutableStateFlow(prefs.getBoolean("calendar_1h", true))
    val calendarReminder1h: StateFlow<Boolean> = _calendarReminder1h

    private val _calendarReminder1d = MutableStateFlow(prefs.getBoolean("calendar_1d", false))
    val calendarReminder1d: StateFlow<Boolean> = _calendarReminder1d

    private val _calendarReminder1w = MutableStateFlow(prefs.getBoolean("calendar_1w", false))
    val calendarReminder1w: StateFlow<Boolean> = _calendarReminder1w

    // ── Stundenplan ──
    private val _scheduleEnabled = MutableStateFlow(prefs.getBoolean("schedule_enabled", true))
    val scheduleEnabled: StateFlow<Boolean> = _scheduleEnabled

    private val _scheduleMinutesBefore = MutableStateFlow(prefs.getInt("schedule_minutes", 20))
    val scheduleMinutesBefore: StateFlow<Int> = _scheduleMinutesBefore

    private val _scheduleStartHour = MutableStateFlow(prefs.getInt("schedule_start_hour", 8))
    val scheduleStartHour: StateFlow<Int> = _scheduleStartHour

    private val _scheduleEndHour = MutableStateFlow(prefs.getInt("schedule_end_hour", 20))
    val scheduleEndHour: StateFlow<Int> = _scheduleEndHour

    // ── Supplements ──
    private val _supplementEnabled = MutableStateFlow(prefs.getBoolean("supplement_enabled", false))
    val supplementEnabled: StateFlow<Boolean> = _supplementEnabled

    private val _supplementTime = MutableStateFlow(prefs.getString("supplement_time", "09:00") ?: "09:00")
    val supplementTime: StateFlow<String> = _supplementTime

    // ── Habits ──
    private val _habitEnabled = MutableStateFlow(prefs.getBoolean("habit_enabled", false))
    val habitEnabled: StateFlow<Boolean> = _habitEnabled

    private val _habitTime = MutableStateFlow(prefs.getString("habit_time", "20:00") ?: "20:00")
    val habitTime: StateFlow<String> = _habitTime

    // ── Google Backup ──
    private val _googleBackupEnabled = MutableStateFlow(prefs.getBoolean("google_backup_enabled", false))
    val googleBackupEnabled: StateFlow<Boolean> = _googleBackupEnabled

    // ── Screenshot Protection ──
    private val _screenshotProtectionEnabled = MutableStateFlow(prefs.getBoolean("screenshot_protection_enabled", true))
    val screenshotProtectionEnabled: StateFlow<Boolean> = _screenshotProtectionEnabled

    // ── Data stats for export subtitle ──
    val totalDataRows: StateFlow<Int> = dataExporter.getTotalDataRowCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Export/Backup Lock ──
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    // ── Region ──
    fun setRegion(regionCode: String) {
        _selectedRegion.value = regionCode
        prefs.edit().putString("selected_region", regionCode).apply()
    }

    fun setHolidayCountry(countryCode: String) {
        _selectedHolidayCountry.value = countryCode
        prefs.edit().putString("selected_holiday_country", countryCode).apply()
        // Reset subdivision when country changes
        _selectedRegion.value = ""
        prefs.edit().putString("selected_region", "").apply()
    }

    // ── Personal Data ──
    fun setUserAge(age: Int) {
        _userAge.value = age
        prefs.edit().putInt("user_age", age).apply()
    }

    fun setUserHeightCm(height: Int) {
        _userHeightCm.value = height
        prefs.edit().putInt("user_height_cm", height).apply()
    }

    // ── Master toggle ──
    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        if (!enabled) {
            // Cancel all scheduled notifications
            notificationScheduler.cancelSupplementReminder()
            notificationScheduler.cancelHabitReminder()
        }
    }

    // ── Calendar toggles ──
    fun setCalendar1h(enabled: Boolean) {
        _calendarReminder1h.value = enabled
        prefs.edit().putBoolean("calendar_1h", enabled).apply()
    }

    fun setCalendar1d(enabled: Boolean) {
        _calendarReminder1d.value = enabled
        prefs.edit().putBoolean("calendar_1d", enabled).apply()
    }

    fun setCalendar1w(enabled: Boolean) {
        _calendarReminder1w.value = enabled
        prefs.edit().putBoolean("calendar_1w", enabled).apply()
    }

    // ── Stundenplan ──
    fun setScheduleEnabled(enabled: Boolean) {
        _scheduleEnabled.value = enabled
        prefs.edit().putBoolean("schedule_enabled", enabled).apply()
    }

    fun setScheduleMinutes(minutes: Int) {
        _scheduleMinutesBefore.value = minutes
        prefs.edit().putInt("schedule_minutes", minutes).apply()
    }

    fun setScheduleStartHour(hour: Int) {
        _scheduleStartHour.value = hour
        prefs.edit().putInt("schedule_start_hour", hour).apply()
    }

    fun setScheduleEndHour(hour: Int) {
        _scheduleEndHour.value = hour
        prefs.edit().putInt("schedule_end_hour", hour).apply()
    }

    // ── Supplements ──
    fun setSupplementEnabled(enabled: Boolean) {
        _supplementEnabled.value = enabled
        prefs.edit().putBoolean("supplement_enabled", enabled).apply()
        if (enabled) {
            notificationScheduler.scheduleSupplementReminder(_supplementTime.value)
        } else {
            notificationScheduler.cancelSupplementReminder()
        }
    }

    fun setSupplementTime(time: String) {
        _supplementTime.value = time
        prefs.edit().putString("supplement_time", time).apply()
        if (_supplementEnabled.value) {
            notificationScheduler.scheduleSupplementReminder(time)
        }
    }

    // ── Habits ──
    fun setHabitEnabled(enabled: Boolean) {
        _habitEnabled.value = enabled
        prefs.edit().putBoolean("habit_enabled", enabled).apply()
        if (enabled) {
            notificationScheduler.scheduleHabitReminder(_habitTime.value)
        } else {
            notificationScheduler.cancelHabitReminder()
        }
    }

    fun setHabitTime(time: String) {
        _habitTime.value = time
        prefs.edit().putString("habit_time", time).apply()
        if (_habitEnabled.value) {
            notificationScheduler.scheduleHabitReminder(time)
        }
    }

    // ── Export ──
    // Returns: 2 = Success, 1 = Partial Success, 0 = Failure
    suspend fun exportData(context: Context, targetUri: Uri): Int {
        if (_isExporting.value) return 0
        _isExporting.value = true
        return try {
            dataExporter.exportAllAsCsv(context, targetUri)
        } finally {
            _isExporting.value = false
        }
    }

    // ── Google Backup ──
    fun setGoogleBackupEnabled(enabled: Boolean) {
        _googleBackupEnabled.value = enabled
        prefs.edit().putBoolean("google_backup_enabled", enabled).apply()
    }

    // ── Screenshot Protection ──
    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        _screenshotProtectionEnabled.value = enabled
        prefs.edit().putBoolean("screenshot_protection_enabled", enabled).apply()
    }

    // ── Backup ──
    suspend fun createBackup(targetUri: Uri): Boolean {
        if (_isExporting.value) return false
        _isExporting.value = true
        return try {
            val success = backupManager.createBackup(targetUri)
            if (success) {
                // Record last backup time so the UI can show a reminder after 30+ days
                prefs.edit().putLong("last_backup_millis", System.currentTimeMillis()).apply()
            }
            success
        } finally {
            _isExporting.value = false
        }
    }

    suspend fun restoreBackup(uri: Uri): Boolean {
        val success = backupManager.restoreBackup(uri)
        if (success) {
            // Force app restart safely (avoids IllegalStateException from Hilt container death)
            val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            if (intent != null) {
                val componentName = intent.component
                val restartIntent = Intent.makeRestartActivityTask(componentName)
                
                // Close the DB connections properly before the process is killed
                db.close()
                
                // Use AlarmManager to trigger restart to avoid OEM "App Crashed" dialogs
                val pendingIntent = android.app.PendingIntent.getActivity(
                    appContext, 0, restartIntent,
                    android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 2000, pendingIntent)
            }
            
            // Kill the current process so Hilt Singletons (like Room instance) are fully destroyed
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        return success
    }

    // ── Dev: Reset all data ──
    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        // 1. Clear Room database
        db.clearAllTables()

        // 2. Clear SharedPreferences
        appContext.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
            .edit().clear().apply()
        appContext.getSharedPreferences("legal", Context.MODE_PRIVATE)
            .edit().clear().apply()

        // 3. Clear DataStore
        themeDataStore.edit { it.clear() }
        modulesDataStore.edit { it.clear() }

        // 4. Cancel scheduled notifications
        notificationScheduler.cancelSupplementReminder()
        notificationScheduler.cancelHabitReminder()
    }
}
