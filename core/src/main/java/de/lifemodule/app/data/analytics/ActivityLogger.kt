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

package de.lifemodule.app.data.analytics

import de.lifemodule.app.util.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.LocalDate

/**
 * Central service that logs every user action across all modules.
 * Call this from ViewModels whenever a user action occurs.
 */
@Singleton
class ActivityLogger @Inject constructor(
    private val dao: ActivityLogDao,
    private val timeProvider: TimeProvider
) {
    private val timestampFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun log(
        module: String,
        action: String,
        itemTitle: String,
        details: String = "",
        date: String = timeProvider.today().format(dateFormatter)
    ) {
        dao.insert(
            ActivityLogEntity(
                timestamp = timeProvider.now().format(timestampFormatter),
                module = module,
                action = action,
                itemTitle = itemTitle,
                details = details,
                date = date
            )
        )
    }

    fun getAll() = dao.getAll()
    fun getByModule(module: String) = dao.getByModule(module)
    fun getByDateRange(start: String, end: String) = dao.getByDateRange(start, end)
    fun getRecent(limit: Int = 50) = dao.getRecent(limit)
    fun getTotalCount() = dao.getTotalCount()
}
