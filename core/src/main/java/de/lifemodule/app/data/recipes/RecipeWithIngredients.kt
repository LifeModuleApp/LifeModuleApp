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

package de.lifemodule.app.data.recipes

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A [RecipeEntity] with all its [RecipeIngredientEntity] items.
 *
 * Loaded via `@Transaction` queries in [RecipeDao].
 */
data class RecipeWithIngredients(
    @Embedded
    val recipe: RecipeEntity,

    @Relation(
        parentColumn = "uuid",
        entityColumn = "recipe_id"
    )
    val ingredients: List<RecipeIngredientEntity>
)
