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

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Logs every user action across the app for analytics and data export.
 * This is the foundation for the "ultimate life tracker" data export.
 */
@Entity(
    tableName = "activity_log",
    indices = [Index("date"), Index("module"), Index("timestamp")]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: String,       // ISO 8601: "2026-02-17T17:00:00"
    val module: String,          // "nutrition", "supplements", "habits", "calendar", "gym", "mood", "schedule"
    val action: String,          // "add", "complete", "delete", "update", "take"
    val itemTitle: String,       // e.g., "Protein Shake", "Push Day", "Vitamin D"
    val details: String = "",    // Extra JSON-like info, e.g. "{\"calories\": 500}"
    val date: String = ""        // Related date for the action
)
