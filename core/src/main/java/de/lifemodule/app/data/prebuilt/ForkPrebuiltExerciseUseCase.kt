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

package de.lifemodule.app.data.prebuilt

import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.ImportSource
import de.lifemodule.app.data.gym.ExerciseDefinitionEntity
import de.lifemodule.app.data.gym.GymRepository
import de.lifemodule.app.util.time.TimeProvider
import timber.log.Timber
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Clones a [PrebuiltExerciseEntity] from the read-only prebuilt database into
 * the user's personal [de.lifemodule.app.data.LifeModuleDatabase].
 *
 * The clone receives a fresh UUID v4 and `importSource = USER`.
 * The prebuilt original is never modified.
 */
class ForkPrebuiltExerciseUseCase @Inject constructor(
    private val gymRepository: GymRepository,
    private val timeProvider: TimeProvider
) {
    /**
     * Forks [prebuilt] into the user's exercise library.
     *
     * @return the UUID of the newly created user exercise.
     */
    suspend operator fun invoke(prebuilt: PrebuiltExerciseEntity): String {
        val now = timeProvider.now().toInstant(ZoneOffset.UTC).toEpochMilli()
        val forked = ExerciseDefinitionEntity(
            uuid = BaseEntity.generateUuid(),
            createdAt = now,
            updatedAt = now,
            importSource = ImportSource.USER,
            importedFromPackageId = null,
            name = prebuilt.name,
            category = prebuilt.category,
            muscleGroup = prebuilt.muscleGroup,
            notes = prebuilt.notes
        )
        gymRepository.insertExerciseDefinition(forked)
        Timber.d("Forked prebuilt exercise '%s' -> user UUID %s", prebuilt.name, forked.uuid)
        return forked.uuid
    }
}
