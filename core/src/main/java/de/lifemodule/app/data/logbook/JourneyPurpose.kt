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

package de.lifemodule.app.data.logbook

/**
 * Purpose of a logged vehicle journey.
 *
 * Used by the GoBD-compliant Logbook module to classify trips.
 * Stored as its [name] string in Room via [de.lifemodule.app.data.RoomConverters].
 */
enum class JourneyPurpose {
    /** Business travel - tax-deductible. */
    BUSINESS,

    /** Daily commute to/from workplace. */
    COMMUTE,

    /** Private trip - not tax-relevant. */
    PRIVATE
}
