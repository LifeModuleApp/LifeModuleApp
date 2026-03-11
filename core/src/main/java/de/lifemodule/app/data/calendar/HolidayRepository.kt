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

package de.lifemodule.app.data.calendar

import android.content.Context
import timber.log.Timber
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

import de.lifemodule.app.data.error.ErrorLogger
import kotlinx.coroutines.runBlocking

/**
 * 100 % offline holiday repository.
 *
 * Reads pre-generated JSON files from `assets/holidays/holidays_{CC}.json`.
 * Each file contains holidays for years 2026-2044 including
 * nationwide and subdivision-specific entries.
 *
 * The country code is derived from `Locale.getDefault().country` (ISO 3166-1 alpha-2).
 * Results are cached in memory for the lifetime of the singleton.
 */
@Singleton
class HolidayRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorLogger: ErrorLogger
) {
    /** In-memory cache: countryCode -> (year -> list of holidays). */
    private val cache = ConcurrentHashMap<String, ConcurrentHashMap<Int, List<HolidayEntry>>>()

    /**
     * Returns holiday [CalendarEventEntity] items for the given [year].
     *
     * @param subdivision e.g. "DE-NW" - when non-empty the subdivision part
     *   ("NW") is matched against each holiday's subdivision list. Nationwide
     *   holidays are always included.
     * @param overrideCountry optional ISO 3166-1 country code (e.g. "US").
     *   When empty, Locale.getDefault().country is used.
     */
    fun getHolidays(year: Int, subdivision: String = "", overrideCountry: String = ""): List<CalendarEventEntity> {
        val countryCode = if (overrideCountry.isNotBlank()) {
            overrideCountry.uppercase()
        } else {
            Locale.getDefault().country.uppercase()
        }
        val subCode = if (subdivision.contains("-")) {
            subdivision.substringAfter("-")   // "DE-NW" -> "NW"
        } else {
            subdivision
        }

        val entries = loadYear(countryCode, year)
        return entries
            .filter { entry ->
                when {
                    entry.subdivisions.isEmpty() -> true              // nationwide
                    subCode.isNotEmpty()         -> subCode in entry.subdivisions
                    else                         -> false             // regional, no sub selected
                }
            }
            .map { it.toCalendarEventEntity(year) }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private fun loadYear(countryCode: String, year: Int): List<HolidayEntry> {
        val countryCache = cache.getOrPut(countryCode) { ConcurrentHashMap() }
        countryCache[year]?.let { return it }

        // Parse from asset JSON
        val json = loadCountryFile(countryCode) ?: return emptyList()
        val yearsObj = json.optJSONObject("years") ?: return emptyList()
        val yearArr = yearsObj.optJSONArray(year.toString()) ?: return emptyList()

        val result = mutableListOf<HolidayEntry>()
        for (i in 0 until yearArr.length()) {
            val obj = yearArr.getJSONObject(i)
            val subs = mutableListOf<String>()
            val subsArr = obj.optJSONArray("subdivisions")
            if (subsArr != null) {
                for (j in 0 until subsArr.length()) subs.add(subsArr.getString(j))
            }
            result.add(
                HolidayEntry(
                    date = obj.getString("date"),
                    name = obj.getString("name"),
                    subdivisions = subs
                )
            )
        }
        countryCache[year] = result
        Timber.d("Loaded %d holidays for %s/%d from assets", result.size, countryCode, year)
        return result
    }

    /** JSON file cache - avoids re-reading the same asset file for different years. */
    private val fileCache = ConcurrentHashMap<String, JSONObject>()
    private val missingCountries = ConcurrentHashMap.newKeySet<String>()

    private fun loadCountryFile(countryCode: String): JSONObject? {
        if (countryCode in missingCountries) return null
        fileCache[countryCode]?.let { return it }
        return try {
            val stream = context.assets.open("holidays/holidays_$countryCode.json")
            val text = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            fileCache[countryCode] = json
            json
        } catch (e: Exception) {
            runBlocking {
                errorLogger.logError("HolidayRepo", "No holiday file for $countryCode: ${e.message}", e, "WARNING")
            }
            missingCountries.add(countryCode)
            null
        }
    }

    // ── data model ───────────────────────────────────────────────────────────

    private data class HolidayEntry(
        val date: String,             // ISO "2026-01-01"
        val name: String,             // Localized holiday name
        val subdivisions: List<String> // e.g. ["NW","BY"] or empty for nationwide
    ) {
        fun toCalendarEventEntity(year: Int): CalendarEventEntity = CalendarEventEntity(
            uuid = "holiday_${date}_${name.hashCode().toUInt()}",
            title = name,
            description = if (subdivisions.isNotEmpty()) "Regional" else "",
            date = date,
            category = "holiday",
            color = "#FF453A",
            isHoliday = true
        )
    }
}
