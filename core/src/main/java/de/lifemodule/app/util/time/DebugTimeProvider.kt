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

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug implementation - adds a user-configurable offset to the real clock.
 * Used via the Debug Menu to "time-travel" the entire app.
 *
 * Offset starts at ZERO (= real time). Calling [setTargetTime] computes the
 * delta between the real clock and the chosen target, then applies it to every
 * subsequent call.
 */
@Singleton
class DebugTimeProvider @Inject constructor() : TimeProvider {

    @Volatile
    private var offset: Duration = Duration.ZERO

    override fun now(): LocalDateTime = LocalDateTime.now().plus(offset)
    override fun today(): LocalDate = now().toLocalDate()
    override fun currentTime(): LocalTime = now().toLocalTime()

    /** Set the app clock so that [now] returns [target] at this instant. */
    fun setTargetTime(target: LocalDateTime) {
        offset = Duration.between(LocalDateTime.now(), target)
    }

    /** Reset to real system time. */
    fun resetToSystemTime() {
        offset = Duration.ZERO
    }

    /** Current offset for display in the Debug Menu. */
    fun getOffset(): Duration = offset
}
