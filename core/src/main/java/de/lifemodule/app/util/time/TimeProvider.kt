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

package de.lifemodule.app.util.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Abstraction over system clock.
 * Production: delegates to [LocalDateTime.now] etc.
 * Debug: allows an offset so the entire app "time-travels".
 */
interface TimeProvider {
    fun now(): LocalDateTime
    fun today(): LocalDate
    fun currentTime(): LocalTime

    /**
     * Returns epoch millis consistent with [now].
     * Use this instead of [System.currentTimeMillis].
     */
    fun currentTimeMillis(): Long =
        now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
