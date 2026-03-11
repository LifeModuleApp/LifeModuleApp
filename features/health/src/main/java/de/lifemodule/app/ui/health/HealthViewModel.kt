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

package de.lifemodule.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.health.DailySteps
import de.lifemodule.app.data.health.HcSdkStatus
import de.lifemodule.app.data.health.HealthConnectManager
import de.lifemodule.app.data.health.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lifemodule.app.data.weight.WeightRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    val manager: HealthConnectManager,       // exposed so Screen can call createPermissionContract()
    private val repository: HealthRepository,
    private val weightRepository: WeightRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)

    // ── SDK / permissions ─────────────────────────────────────────────────────

    /** Status of the Health Connect SDK on this device. */
    val sdkStatus: HcSdkStatus = manager.sdkStatus

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    // ── Loading ───────────────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Health data ───────────────────────────────────────────────────────────

    private val _todaySteps = MutableStateFlow(0L)
    val todaySteps: StateFlow<Long> = _todaySteps.asStateFlow()

    /** Last 7 days, oldest entry first (index 0 = 6 days ago, index 6 = today). */
    private val _weeklySteps = MutableStateFlow<List<DailySteps>>(emptyList())
    val weeklySteps: StateFlow<List<DailySteps>> = _weeklySteps.asStateFlow()

    private val _todayDistanceKm = MutableStateFlow(0.0)
    val todayDistanceKm: StateFlow<Double> = _todayDistanceKm.asStateFlow()

    private val _avgHeartRate = MutableStateFlow(0)
    val avgHeartRate: StateFlow<Int> = _avgHeartRate.asStateFlow()

    /** Last night's total sleep as a [Duration]. */
    private val _lastNightSleep = MutableStateFlow(Duration.ZERO)
    val lastNightSleep: StateFlow<Duration> = _lastNightSleep.asStateFlow()

    // ── User Settings (for BMR) ───────────────────────────────────────────────
    private val _userAge = MutableStateFlow(prefs.getInt("user_age", 30))
    private val _userHeightCm = MutableStateFlow(prefs.getInt("user_height_cm", 175))

    // ── Computed Values ───────────────────────────────────────────────────────
    
    val activeCalories: StateFlow<Int> = _todaySteps.map { steps ->
        (steps * 0.04).toInt() // Rough estimate: ~40 kcal per 1000 steps
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bmr: StateFlow<Int> = combine(
        _userAge,
        _userHeightCm,
        weightRepository.getLatestEntry()
    ) { age, height, entry ->
        val weight = entry?.weightKg ?: 75.0
        // Simplified Mifflin-St Jeor (averaging male/female offset = -78)
        val calculatedBmr = (10 * weight) + (6.25 * height) - (5 * age) - 78
        calculatedBmr.coerceAtLeast(0.0).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Initialisation ────────────────────────────────────────────────────────

    init {
        if (sdkStatus == HcSdkStatus.AVAILABLE) {
            checkPermissionsAndLoad()
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Called after the user interacts with the Health Connect permission dialog.
     * [granted] is the set of permission strings that were accepted.
     */
    fun onPermissionsResult(granted: Set<String>) {
        if (granted.containsAll(manager.permissions)) {
            _permissionsGranted.value = true
            loadAllData()
        } else {
            // Even partial grants warrant a re-check
            checkPermissionsAndLoad()
        }
    }

    /** Re-check permission state and reload data (e.g. pull-to-refresh). */
    fun refresh() {
        _userAge.value = prefs.getInt("user_age", 30)
        _userHeightCm.value = prefs.getInt("user_height_cm", 175)
        checkPermissionsAndLoad()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun checkPermissionsAndLoad() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allGranted = manager.hasAllPermissions()
                _permissionsGranted.value = allGranted
                if (allGranted) loadAllData()
            } catch (e: Exception) {
                Timber.e(e, "[Health] Failed to check permissions")
            }
        }
    }

    private fun loadAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                coroutineScope {
                    launch { _todaySteps.value      = repository.fetchDailySteps() }
                    launch { _weeklySteps.value     = repository.fetchWeeklySteps() }
                    launch { _todayDistanceKm.value = repository.fetchDailyDistanceKm() }
                    launch { _avgHeartRate.value    = repository.fetchTodayAvgHeartRate() }
                    launch { _lastNightSleep.value  = repository.fetchLastNightSleep() }
                }
            } catch (e: Exception) {
                Timber.e(e, "[Health] Failed to load Health Connect data")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
