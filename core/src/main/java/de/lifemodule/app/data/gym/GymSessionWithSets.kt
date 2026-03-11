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

package de.lifemodule.app.data.gym

import androidx.room.Embedded
import androidx.room.Relation

data class GymSessionWithSets(
    @Embedded val session: GymSessionEntity,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "sessionId"
    )
    val sets: List<SessionSetEntity>
) {
    /** Distinct exercise names logged in this session, in order */
    val exerciseNames: List<String>
        get() = sets
            .sortedBy { it.exerciseOrderIndex }
            .map { it.exerciseName }
            .distinct()

    /** Total volume in kg (sum of reps × weight over all sets) */
    val totalVolumeKg: Double
        get() = sets.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) }
}
