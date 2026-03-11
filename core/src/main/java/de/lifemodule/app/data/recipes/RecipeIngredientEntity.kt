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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.ImportSource

/**
 * An ingredient within a recipe.
 *
 * References [RecipeEntity] via [recipeId].
 * May optionally reference a food item from `:features:nutrition` via [foodItemUuid]
 * (by UUID, not foreign key - modules stay isolated).
 */
@Entity(
    tableName = "recipe_ingredients",
    indices = [
        Index("uuid"),
        Index("recipe_id"),
        Index("food_item_uuid")
    ]
)
data class RecipeIngredientEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    override val uuid: String = BaseEntity.generateUuid(),

    @ColumnInfo(name = "created_at")
    override val createdAt: Long = 0L,

    @ColumnInfo(name = "updated_at")
    override val updatedAt: Long = 0L,

    @ColumnInfo(name = "import_source")
    override val importSource: ImportSource = ImportSource.USER,

    @ColumnInfo(name = "imported_from_package_id")
    override val importedFromPackageId: String? = null,

    /** UUID of the parent [RecipeEntity]. */
    @ColumnInfo(name = "recipe_id")
    val recipeId: String,

    /** Display name of the ingredient (e.g. "Butter"). */
    @ColumnInfo(name = "name")
    val name: String,

    /** Quantity (e.g. 200.0). */
    @ColumnInfo(name = "quantity")
    val quantity: Double,

    /** Unit of measurement (e.g. "g", "ml", "Stück", "EL"). */
    @ColumnInfo(name = "unit")
    val unit: String,

    /** Order index for display. */
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,

    /**
     * Optional UUID reference to a [de.lifemodule.app.data.nutrition.FoodItemEntity].
     * Used for nutritional calculation but NOT a Room foreign key
     * (modules stay isolated).
     */
    @ColumnInfo(name = "food_item_uuid")
    val foodItemUuid: String? = null,

) : BaseEntity(uuid, createdAt, updatedAt, importSource, importedFromPackageId)
