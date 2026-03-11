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

package de.lifemodule.app.data.health

import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import de.lifemodule.app.data.error.ErrorLogger
import de.lifemodule.app.util.time.TimeProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All Health Connect data access lives here.
 *
 * Every function returns a safe default (0 / empty list) when:
 *  - Health Connect is not installed.
 *  - Permissions are not granted.
 *  - The device has no data for the requested period.
 *  - Any unexpected exception occurs (logged, never rethrown).
 */
@Singleton
class HealthRepository @Inject constructor(
    private val manager: HealthConnectManager,
    private val timeProvider: TimeProvider,
    private val errorLogger: ErrorLogger
) {

    // ── Steps ─────────────────────────────────────────────────────────────────

    /**
     * Returns the total step count for [date] (defaults to today).
     * Uses the `StepsRecord.COUNT_TOTAL` aggregate - efficient single call.
     */
    suspend fun fetchDailySteps(date: LocalDate = timeProvider.today()): Long {
        val client = manager.client ?: return 0L
        return try {
            val (start, end) = date.dayRange()
            val result: AggregationResult = client.aggregate(
                AggregateRequest(
                    metrics       = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            result[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            errorLogger.logError("HealthRepo", "fetchDailySteps failed for $date: ${e.message}", e, "WARNING")
            0L
        }
    }

    /**
     * Returns steps for each of the last [days] days (oldest first).
     * e.g. days=7 -> indices 0...6 = today-6 ... today.
     */
    suspend fun fetchWeeklySteps(days: Int = 7): List<DailySteps> {
        val today = timeProvider.today()
        return (days - 1 downTo 0).map { daysAgo ->
            val day = today.minusDays(daysAgo.toLong())
            DailySteps(day, fetchDailySteps(day))
        }
    }

    /**
     * Efficient parallel fetching of step data for an arbitrary period.
     */
    suspend fun fetchStepsByDateRange(start: LocalDate, end: LocalDate): List<DailySteps> = coroutineScope {
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
        val deferredList = (0..daysBetween).map { offset ->
            async {
                val day = start.plusDays(offset.toLong())
                DailySteps(day, fetchDailySteps(day))
            }
        }
        deferredList.awaitAll()
    }

    // ── Distance ──────────────────────────────────────────────────────────────

    /**
     * Returns today's total distance in **kilometres** (double, rounded to 2 decimals).
     */
    suspend fun fetchDailyDistanceKm(date: LocalDate = timeProvider.today()): Double {
        val client = manager.client ?: return 0.0
        return try {
            val (start, end) = date.dayRange()
            val result = client.aggregate(
                AggregateRequest(
                    metrics       = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val distanceMeters = result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            (distanceMeters / 1000.0).roundTo(2)
        } catch (e: Exception) {
            errorLogger.logError("HealthRepo", "fetchDailyDistance failed for $date: ${e.message}", e, "WARNING")
            0.0
        }
    }

    // ── Heart Rate ────────────────────────────────────────────────────────────

    /**
     * Returns the average heart rate (bpm) measured today, or 0 if no data.
     */
    suspend fun fetchTodayAvgHeartRate(): Int {
        val client = manager.client ?: return 0
        return try {
            val (start, end) = timeProvider.today().dayRange()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType      = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val allSamples = response.records.flatMap { it.samples }
            if (allSamples.isEmpty()) 0
            else (allSamples.sumOf { it.beatsPerMinute } / allSamples.size).toInt()
        } catch (e: Exception) {
            errorLogger.logError("HealthRepo", "fetchAvgHeartRate failed: ${e.message}", e, "WARNING")
            0
        }
    }

    // ── Sleep ─────────────────────────────────────────────────────────────────

    /**
     * Returns total sleep duration for the *last night* (18:00 yesterday -> 12:00 today).
     * Returns [Duration.ZERO] if no sleep records are found.
     */
    suspend fun fetchLastNightSleep(): Duration {
        val client = manager.client ?: return Duration.ZERO
        return try {
            val zone  = ZoneId.systemDefault()
            val today = timeProvider.today()
            val start = today.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
            val end   = today.atTime(12, 0).atZone(zone).toInstant()

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType      = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.fold(Duration.ZERO) { acc, session ->
                acc + Duration.between(session.startTime, session.endTime)
            }
        } catch (e: Exception) {
            errorLogger.logError("HealthRepo", "fetchLastNightSleep failed: ${e.message}", e, "WARNING")
            Duration.ZERO
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Start and end [Instant] for the given calendar day (local timezone). */
    private fun LocalDate.dayRange(): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val start = atStartOfDay(zone).toInstant()
        val end   = plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }

    private fun Double.roundTo(decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return Math.round(this * factor) / factor
    }
}

/** One day's aggregated step count. */
data class DailySteps(val date: LocalDate, val steps: Long)
